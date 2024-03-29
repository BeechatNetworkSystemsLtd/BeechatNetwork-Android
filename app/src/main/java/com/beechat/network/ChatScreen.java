package com.beechat.network;

import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.content.FileProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.exceptions.XBeeException;
import com.digi.xbee.api.models.XBee64BitAddress;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/***
 *  --- ChatScreen ---
 *  The class that is responsible for the chat window.
 ***/
public class ChatScreen extends AppCompatActivity {

    // Variables.
    Context context;
    Resources resources;
    ImageButton sendButton;
    ImageButton backButton, attachButton;
    ImageButton fileStopButton, filePauseButton;
    ListView chatListView;
    EditText inputField;
    static TextView textViewAttachment;
    KeyguardManager myKM;
    static ChatDeviceAdapter chatDeviceAdapter;
    RemoteXBeeDevice remote;
    public static AppCompatActivity linkAct = null;
    String message, filename, sizeFile, fileString;
    Boolean fileFlag = false;
    int numberOfPackage;
    int remainOfPackage;
    boolean filePause = false;
    boolean fileStop = false;
    static int transmitProgressValue = 0;
    static int transmitPercentageValue = 0;
    static ProgressBar transmitProgress = null;
    byte[] array;

    static DatabaseHandler db;

    static void setFileDelivery(float val) {
        linkAct.runOnUiThread(() -> {
            if (transmitProgress != null) {
                transmitProgress.setProgress((int)(val * transmitProgress.getMax()));
            }
        });
    }

    static void setFileText(String text, int per) {
        linkAct.runOnUiThread(() -> {
            if (textViewAttachment != null) {
                if (per != -1) {
                    textViewAttachment.setText(text + " " + Integer.toString(per) + "%");
                } else {
                    textViewAttachment.setText("");
                }
            }
        });
    }

    static ArrayList<String> messages = new ArrayList<>();
    public static List<String> getMessages() {
        return messages;
    }

    static boolean flagNotification = false;
    public static void setNotification() {
        linkAct.runOnUiThread(() -> {
            chatDeviceAdapter.notifyDataSetChanged();
        });
        flagNotification = true;
    }

    static String myUserId, myXbeeAddress, selectedName, selectedUserId, selectedXbeeAddress;
    Cipher cipher;
    static TextView nameTextView;

    // Constants
    int FILE_SELECT_CODE = 101;
    int sizeOfPackage = Packet.getMaxLen();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_screen);

        myKM = (KeyguardManager) ChatScreen.this.getSystemService(Context.KEYGUARD_SERVICE);
        context = LocaleHelper.setLocale(ChatScreen.this, WelcomeScreen.language);
        resources = context.getResources();

        db = new DatabaseHandler(this);
        Bundle extras = getIntent().getExtras();

        textViewAttachment = findViewById(R.id.textViewAttachment);

        transmitProgress = findViewById(R.id.progressBar);
        sendButton = findViewById(R.id.sendButton);
        nameTextView = findViewById(R.id.nameTextView);
        backButton = findViewById(R.id.backButton);
        attachButton = findViewById(R.id.attachButton);
        fileStopButton = findViewById(R.id.fileStopButton);
        filePauseButton = findViewById(R.id.filePauseButton);
        fileStopButton.setVisibility(View.INVISIBLE);
        filePauseButton.setVisibility(View.INVISIBLE);

        inputField = findViewById(R.id.inputField);

        chatListView = findViewById(R.id.chatListView);
        chatListView.setDivider(null);

        myUserId = extras.getString("key_myUserId");
        myXbeeAddress = extras.getString("key_myXbeeAddress");
        selectedName = extras.getString("key_selectedName");
        selectedUserId = extras.getString("key_selectedUserId");
        selectedXbeeAddress = extras.getString("key_selectedXbeeAddress");
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(db.getKey(selectedXbeeAddress), "AES"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //if (messages.size() == 0) {
        if (true) {
            messages.clear();
            List<TextMessage> messagesDB = db.getAllMessages(myUserId, myXbeeAddress, selectedUserId, selectedXbeeAddress);
            for (TextMessage mg : messagesDB) {
                messages.add(mg.getContent());
            }
        }

        chatDeviceAdapter = new ChatDeviceAdapter(this, messages);
        chatListView.setAdapter(chatDeviceAdapter);

        chatListView.setOnItemClickListener(
            new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View view,
                        int position, long id) {
                    Object o = chatListView.getItemAtPosition(position);
                    String pen = o.toString();
                    if (pen.endsWith("P") || pen.endsWith("F")) {
                        requestToOpenFile(removeLastChar(removeLastChar(pen)));
                    }
                }
            }
        );

        linkAct = this;
        nameTextView.setText(selectedName);

        nameTextView.setOnClickListener(v -> {
            Intent intent = new Intent(ChatScreen.this, EditContactScreen.class);
            intent.putExtra("key_selectedXbeeAddress", selectedXbeeAddress);
            intent.putExtra("key_selectedUserId", selectedUserId);
            startActivity(intent);
        });

        // Handling the event of returning to the main window.
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chatDeviceAdapter.clear();
                finish();
            }
        });

        // Handling the event of attaching files.
        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                browseClick();
            }
        });

        // Handling the message sending event.
        fileStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileStop = true;
            }
        });

        // Handling the message sending event.
        filePauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                filePause = !filePause;
                Drawable drawable = null;
                if (filePause) {
                    drawable = getResources().getDrawable(android.R.drawable.ic_media_play);
                } else {
                    drawable = getResources().getDrawable(android.R.drawable.ic_media_pause);
                }
                filePauseButton.setImageDrawable(drawable);
            }
        });

        // Handling the message sending event.
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                ProgressBar transmitProgress = findViewById(R.id.progressBar);
                message = inputField.getText().toString();
                if (message.length() == 0 && !fileFlag) {
                    return;
                }
                String pureMes = message;
                message = message + "\n" + currentTime;
                try {
                    if (!fileFlag) {
                        message = new String(cipher.doFinal(message.getBytes()));

                        Message m = new Message(
                            Packet.Type.MESSAGE_DATA
                          , message.getBytes()
                        );
                        messages.add(message + "\n");
                        m.send(SplashScreen.myXbeeDevice, remote, SplashScreen.hasher);
                        for (ContactsScreen.ContactInfo ccn: ContactsScreen.contactInfos) {
                            if (ccn.name.equals(selectedName)) {
                                ccn.mes = pureMes;
                                ContactsScreen.onRefresh();
                                break;
                            }
                        }
                        inputField.setText("");
                    } else {
                        new Thread(() -> {
                            try {
                                String dir = Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS
                                ).getAbsolutePath() + File.separator + "Beechat";

                                (new File(dir)).mkdirs();

                                FileOutputStream outputStream = new FileOutputStream(
                                    new File(
                                        dir + File.separator + fileString
                                    )
                                );

                                Message m = new Message(
                                    Packet.Type.FILE_NAME_DATA
                                  , cipher.doFinal(fileString.getBytes())
                                );
                                m.send(SplashScreen.myXbeeDevice, remote, SplashScreen.hasher);

                                ByteBuffer bb = ByteBuffer.wrap(array);
                                byte[] packageFile = new byte[sizeOfPackage];
                                int tcount = remainOfPackage != 0 ? numberOfPackage + 1 : numberOfPackage;
                                Packet sender = new Packet(SplashScreen.hasher);

                                ChatScreen.this.runOnUiThread(() -> {
                                    fileStopButton.setVisibility(View.VISIBLE);
                                    filePauseButton.setVisibility(View.VISIBLE);
                                });

                                for (int i = 0; i < (int)numberOfPackage; i++) {
                                    bb.get(packageFile, 0, packageFile.length);
                                    outputStream.write(packageFile);
                                    sender.setData(
                                        Packet.Type.FILE_DATA
                                      , (int)i
                                      , (int)tcount
                                      , packageFile
                                      , SplashScreen.hasher
                                    );
                                    SplashScreen.myXbeeDevice.sendData(
                                        remote
                                      , sender.getData()
                                    );
                                    transmitProgressValue = (int)(((float)i / (float)numberOfPackage) * (float)transmitProgress.getMax());
                                    transmitPercentageValue = (int)(((float)i / (float)numberOfPackage) * 100);
                                    ChatScreen.this.runOnUiThread(() -> {
                                        transmitProgress.setProgress(transmitProgressValue);
                                        textViewAttachment.setText(fileString + "  " + Integer.toString(transmitPercentageValue) + "%");
                                    });
                                    if (fileStop) {
                                        break;
                                    } else while (filePause) {
                                        Thread.sleep(500);
                                    }
                                }
                                if (remainOfPackage != 0 && !fileStop) {
                                    sender = new Packet(SplashScreen.hasher);
                                    packageFile = new byte[remainOfPackage];
                                    bb.get(packageFile, 0, packageFile.length);
                                    outputStream.write(packageFile);
                                    sender.setData(
                                        Packet.Type.FILE_DATA
                                      , (int)(tcount - 1)
                                      , (int)tcount
                                      , packageFile
                                      , SplashScreen.hasher
                                    );
                                    SplashScreen.myXbeeDevice.sendData(
                                        remote
                                      , sender.getData()
                                    );
                                }
                                outputStream.flush();
                                outputStream.close();
                                messages.add(fileString + "\n" + "P");
                                ChatScreen.this.runOnUiThread(() -> {
                                    fileStopButton.setVisibility(View.INVISIBLE);
                                    filePauseButton.setVisibility(View.INVISIBLE);
                                });
                                message = fileString + "\n" + "P";
                                fileStop = false;
                            } catch (Exception e) {
                                ChatScreen.this.runOnUiThread(() -> {
                                    messages.add("No rights to read this file\n");
                                });
                                e.printStackTrace();
                            }
                            db.insertMessage(new TextMessage(myUserId, myXbeeAddress, selectedUserId, selectedXbeeAddress, message));
                            ChatScreen.this.runOnUiThread(() -> {
                                textViewAttachment.setText("");
                                transmitProgress.setProgress(0);
                                chatDeviceAdapter.notifyDataSetChanged();
                            });
                        }).start();
                        inputField.setText("");
                        fileFlag = false;
                        return;
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    messages.add("Error transmitting message: " + e.getMessage());
                }
                db.insertMessage(new TextMessage(myUserId, myXbeeAddress, selectedUserId, selectedXbeeAddress, message));
                ChatScreen.this.runOnUiThread(() -> {
                    chatDeviceAdapter.notifyDataSetChanged();
                });
            }
        });

        String REMOTE_NODE_ID = selectedXbeeAddress;

        XBee64BitAddress RemoteAddr = new XBee64BitAddress(REMOTE_NODE_ID);

        if (SplashScreen.myXbeeDevice.get64BitAddress().equals(REMOTE_NODE_ID)) {
            messages.add("Error: the value of the REMOTE_NODE_ID must be "
                    + "the Node Identifier (NI) of the OTHER module.");
        } else {
            remote = new RemoteXBeeDevice(SplashScreen.myXbeeDevice, RemoteAddr);
            if (remote != null) {
            } else {
                messages.add("Could not find the module! " + REMOTE_NODE_ID + " in the network.");
            }
        }

        // Creation of a channel for processing notifications.
        createNotificationChannel();

        final Handler handler = new Handler();
        final int delay = 1000;

        // Checking the message channel and generating notifications.
        handler.postDelayed(new Runnable() {
            public void run() {
                //chatDeviceAdapter.notifyDataSetChanged();
                if (myKM.inKeyguardRestrictedInputMode() && flagNotification) {
                    String message = selectedName + ":\n" + messages.get(messages.size() - 1);
                    message = removeLastChar(message);
                    notifyThis("Beechat notification", message);
                    flagNotification = false;
                }
                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    /***
     *  --- updateName() ---
     *  The function of updating the current contact name in database.
     ***/
    public static void updateName() {
        List<Contact> contactsFromDb = db.getAllContacts(Blake3.toString(SplashScreen.myGeneratedUserId));

        ContactsScreen.contactNames.clear();
        ContactsScreen.contactXbeeAddress.clear();
        ContactsScreen.contactUserIds.clear();
        for (Contact cn : contactsFromDb) {
            ContactsScreen.contactNames.add(cn.getName());
            ContactsScreen.contactXbeeAddress.add(cn.getXbeeDeviceNumber());
            ContactsScreen.contactUserIds.add(cn.getUserId());
        }
    }

    // Additional functions--->
    public void browseClick() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (Exception ex) {
            System.out.println("browseClick :" + ex);
        }
    }

    public void requestToOpenFile(String name) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                             + File.separator + "Beechat" + File.separator + name);
        Uri path = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
        String mimeType = getContentResolver().getType(path);
        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        openIntent.setDataAndType(path, mimeType);
        try {
            startActivity(openIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String getPath(Context context, Uri uri) {

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                //final String id = DocumentsContract.getDocumentId(uri);
                //final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                //return getDataColumn(context, contentUri, null, null);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    final String id;
                    Cursor cursor = null;
                    try {
                        cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            String fileName = cursor.getString(0);
                            String path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                            if (!TextUtils.isEmpty(path)) {
                                return path;
                            }
                        }
                    }
                    finally {
                        if (cursor != null)
                            cursor.close();
                    }
                    id = DocumentsContract.getDocumentId(uri);
                    if (!TextUtils.isEmpty(id)) {
                        if (id.startsWith("raw:")) {
                            return id.replaceFirst("raw:", "");
                        }
                        String[] contentUriPrefixesToTry = new String[]{
                                "content://downloads/public_downloads",
                                "content://downloads/my_downloads"
                        };
                        for (String contentUriPrefix : contentUriPrefixesToTry) {
                            try {
                                final Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));


                                return getDataColumn(context, contentUri, null, null);
                            } catch (NumberFormatException e) {
                                //In Android 8 and Android P the id is not a number
                                return uri.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
                            }
                        }


                    }
                }
                else {
                    final String id = DocumentsContract.getDocumentId(uri);
                    Uri contentUri = null;

                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                    try {
                        contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    }
                    catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (contentUri != null) {

                        return getDataColumn(context, contentUri, null, null);
                    }
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    Uri uri = data.getData();
                    String path = getPath(this, uri);
                    /*if (filesize >= FILE_SIZE_LIMIT) {
                        Toast.makeText(this, "The selected file is too large. Select a new file with size less than 2mb", Toast.LENGTH_LONG).show();
                    } */
                    String mimeType = getContentResolver().getType(uri);
                    if (mimeType == null) {
                        File file = new File(path);
                        filename = file.getName();
                        if (filename.length() > 34) fileString = filename.substring(filename.length() - 20);
                        else fileString = filename;
                        /*if (path == null) {
                            filename = FilenameUtils.getName(uri.toString());
                        } else {
                            File file = new File(path);
                            filename = file.getName();
                        }*/
                    } else {
                        Uri returnUri = data.getData();
                        Cursor returnCursor = getContentResolver().query(returnUri, null, null, null, null);
                        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                        returnCursor.moveToFirst();
                        filename = returnCursor.getString(nameIndex);
                        if (filename.length() > 34) fileString = filename.substring(filename.length() - 20);
                        else fileString = filename;
                        sizeFile = Long.toString(returnCursor.getLong(sizeIndex));

                    }

                    textViewAttachment.setText(fileString + " (" + sizeFile + ")");
                    File fileSave = getExternalFilesDir(null);
                    String sourcePath = getExternalFilesDir(null).toString();
                    array = cipher.doFinal(method(new File(path)));
                    numberOfPackage = array.length/sizeOfPackage;
                    remainOfPackage = array.length % sizeOfPackage;
                    fileFlag = true;
                    /*try {
                        copyFileStream(new File(sourcePath + "/" + filename), uri, this);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static byte[] method(File file)
            throws IOException {

        // Creating an object of FileInputStream to
        // read from a file
        FileInputStream fl = new FileInputStream(file);

        // Now creating byte array of same length as file
        byte[] arr = new byte[(int) file.length()];

        // Reading file content to byte array
        // using standard read() method
        fl.read(arr);

        // lastly closing an instance of file input stream
        // to avoid memory leakage
        fl.close();

        // Returning above byte array
        return arr;
    }

    private void copyFileStream(File dest, Uri uri, Context context)
            throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;

            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            is.close();
            os.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateName();
        chatDeviceAdapter.notifyDataSetChanged();
    }


    /***
     *  --- ChatDeviceAdapter ---
     *  The class that is responsible for initializing the message list.
     ***/
    private class ChatDeviceAdapter extends ArrayAdapter<String> {

        private final Context context;

        ChatDeviceAdapter(@NonNull Context context, ArrayList<String> messages) {
            super(context, -1, messages);
            this.context = context;
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            String message = messages.get(position);
            if (message == null) {
                return new RelativeLayout(context);
            }

            Typeface typeface = ResourcesCompat.getFont(context, R.font.nunito_regular);

            if (message.endsWith("S")) {
                message = removeLastChar(message);
                message = removeLastChar(message);
                String[] mesData = message.split("\n");

                RelativeLayout layout = new RelativeLayout(context);
                //layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(40, 30, 40, 30);

                LinearLayout layout2 = new LinearLayout(context);
                layout2.setOrientation(LinearLayout.VERTICAL);
                Drawable drawable = getResources().getDrawable(R.drawable.server_message_box);
                layout2.setBackground(drawable);

                TextView nameText = new TextView(context);
                nameText.setText(message.split("\n")[0]);
                nameText.setTypeface(typeface);
                nameText.setTextSize(15);
                nameText.setPadding(20, 20, 40, 10);

                TextView timeText = null;
                if (mesData.length > 1) {
                    timeText = new TextView(context);
                    timeText.setText(message.split("\n")[1]);
                    timeText.setTypeface(nameText.getTypeface(), Typeface.BOLD);
                    timeText.setTextSize(11);
                    timeText.setPadding(20, 0, 20, 20);
                }

                layout2.addView(nameText);
                if (timeText != null) {
                    layout2.addView(timeText);
                }
                layout.addView(layout2);


                return layout;
            } else if (message.endsWith("P")) {
                message = removeLastChar(message);
                message = removeLastChar(message);

                int color = Color.parseColor("#010523");
                RelativeLayout layout = new RelativeLayout(context);
                //layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(40, 30, 40, 30);

                TextView nameText = new TextView(context);
                nameText.setGravity(Gravity.RIGHT);
                nameText.setText(message);
                nameText.setTypeface(typeface);
                nameText.setTextColor(color);
                nameText.setTextSize(15);
                nameText.setPadding(40, 20, 20, 15);
                Drawable drawable = getResources().getDrawable(R.drawable.client_message_box);
                //Drawable iconTick = getResources().getDrawable(android.R.drawable.sym_contact_card);
                //ImageView imageView = new ImageView(context);
                //imageView.setImageDrawable(iconTick);

                LinearLayout layout2 = new LinearLayout(context);
                layout2.setOrientation(LinearLayout.HORIZONTAL);
                layout2.setBackground(drawable);
                layout2.setGravity(Gravity.RIGHT);

                //layout2.addView(imageView);
                layout2.addView(nameText);
                layout.addView(layout2);
                layout.setGravity(Gravity.RIGHT);

                return layout;
            } else if (message.endsWith("F")) {
                message = removeLastChar(message);
                message = removeLastChar(message);

                int color = Color.parseColor("#010523");
                RelativeLayout layout = new RelativeLayout(context);
                //layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(40, 30, 40, 30);

                TextView nameText = new TextView(context);
                nameText.setGravity(Gravity.LEFT);
                nameText.setText(message);
                nameText.setTypeface(typeface);
                nameText.setTextColor(color);
                nameText.setTextSize(15);
                nameText.setPadding(40, 20, 20, 15);
                Drawable drawable = getResources().getDrawable(R.drawable.server_message_box);
                //Drawable iconTick = getResources().getDrawable(android.R.drawable.sym_contact_card);

                //ImageView imageView = new ImageView(context);
                //imageView.setImageDrawable(iconTick);

                LinearLayout layout2 = new LinearLayout(context);
                layout2.setOrientation(LinearLayout.HORIZONTAL);
                layout2.setBackground(drawable);
                layout2.setGravity(Gravity.LEFT);

                layout2.addView(nameText);
                //layout2.addView(imageView);
                layout.addView(layout2);
                layout.setGravity(Gravity.LEFT);

                return layout;
            } else {
                int color = Color.parseColor("#010523");
                String[] mesData = message.split("\n");
                RelativeLayout layout = new RelativeLayout(context);
                //layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(40, 30, 40, 30);

                TextView nameText = new TextView(context);
                nameText.setGravity(Gravity.RIGHT);
                nameText.setText(mesData[0]);
                nameText.setTypeface(typeface);
                nameText.setTextColor(color);
                nameText.setTextSize(15);
                nameText.setPadding(40, 20, 20, 15);
                Drawable drawable = getResources().getDrawable(R.drawable.client_message_box);

                LinearLayout layout2 = new LinearLayout(context);
                layout2.setOrientation(LinearLayout.VERTICAL);
                Drawable iconTick = getResources().getDrawable(R.drawable.sent_tick);
                layout2.setBackground(drawable);
                layout2.setGravity(Gravity.RIGHT);

                LinearLayout layout3 = null;
                TextView timeText = null;
                TextView imgView = null;
                if (mesData.length > 1) {
                    layout3 = new LinearLayout(context);
                    layout3.setGravity(Gravity.RIGHT);
                    layout3.setOrientation(LinearLayout.HORIZONTAL);
                    timeText = new TextView(context);
                    timeText.setGravity(Gravity.RIGHT);
                    timeText.setText(mesData[1]);
                    timeText.setTypeface(nameText.getTypeface());
                    timeText.setPadding(20, 0, 0, 10);
                    timeText.setTextSize(11);

                    imgView = new TextView(context);
                    imgView.setTypeface(nameText.getTypeface());
                    imgView.setTextSize(11);
                    imgView.setTextColor(color);
                    imgView.setPadding(10, 0, 15, 10);
                    imgView.setText("\u2713");
                }

                layout2.addView(nameText);
                if (timeText != null && layout3 != null && imgView != null) {
                    layout3.addView(timeText);
                    layout3.addView(imgView);
                    layout2.addView(layout3);
                }
                layout.addView(layout2);
                layout.setGravity(Gravity.RIGHT);

                return layout;
            }
        }
    }

    /***
     *  --- removeLastChar(String) ---
     *  The function to remove the last character in a string.
     *
     *  @param s Transmitted message.
     ***/
    public static String removeLastChar(String s) {
        return (s == null || s.length() == 0)
                ? null
                : (s.substring(0, s.length() - 1));
    }

    /***
     *  --- createNotificationChannel() ---
     *  The function of creating a notification channel.
     ***/
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "R.string.channel_name";
            String description = "R.string.channel_description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("CHANNEL_ID", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /***
     *  --- notifyThis(String, String) ---
     *  The function of sending a message to the notification channel.
     *
     *  @param title Notification header.
     *  @param message Notification text.
     ***/
    public void notifyThis(String title, String message) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "CHANNEL_ID")
                .setSmallIcon(R.drawable.digi_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(0, mBuilder.build());
    }
}
