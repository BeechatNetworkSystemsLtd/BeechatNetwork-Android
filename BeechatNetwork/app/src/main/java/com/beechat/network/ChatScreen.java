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
/*import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;*/
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.android.XBeeDevice;
import com.digi.xbee.api.exceptions.XBeeException;
import com.digi.xbee.api.listeners.IDataReceiveListener;
import com.digi.xbee.api.models.XBee64BitAddress;
import com.digi.xbee.api.models.XBeeMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/***
 *  --- ChatScreen ----
 *  The class that is responsible for the chat window.
 ***/
public class ChatScreen extends AppCompatActivity {

    // Constants.
    private static final String REMOTE_NODE_ID = "XBEE_B";

    // Variables.
    private static XBeeDevice device;
    private static DataReceiveListener listener = new DataReceiveListener();
    private static ChatDeviceAdapter chatDeviceAdapter;
    private static ArrayList<String> messages = new ArrayList<>();
    private static RemoteXBeeDevice remote = null;
    private static String message = null;
    private static String datetime = null;
    private static boolean flagNotification = false;
    private KeyguardManager myKM= null;

    public static String test = "";
    Button sendButton;
    ImageButton backButton, attachButton;
    public static TextView nameTextView;
    ListView chatListView;
    EditText inputField;
    Context context;
    Resources resources;

    TextView textViewAttachment;

    String filename;
    public static String tempTest;
    String sizeFile;
    private static final int FILE_SELECT_CODE = 101;

    byte[] array;
    boolean fileFlag = false;
    int numberOfPackage;
    int sizeOfPackage = 73;

    public static String chatSenderId = null;
    public static String chatReceiverId = null;
    public static String chatXbeeSender = null;
    public static String chatXbeeReceiver = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_screen);

        Bundle extras = getIntent().getExtras();
        chatSenderId = extras.getString("sender_id");
        chatReceiverId = extras.getString("receiver_id");
        chatXbeeSender = extras.getString("xbee_sender");
        chatXbeeReceiver = extras.getString("xbee_receiver");


        textViewAttachment = (TextView)findViewById(R.id.textViewAttachment);
        context = LocaleHelper.setLocale(ChatScreen.this, WelcomeScreen.language);
        resources = context.getResources();
        myKM = (KeyguardManager) ChatScreen.this.getSystemService(Context.KEYGUARD_SERVICE);

        List<Message> messagesDB = SplashScreen.db.getAllMessages(chatSenderId, chatXbeeSender, chatReceiverId, chatXbeeReceiver);
        for (Message mg : messagesDB)
        {
            messages.add(mg.getContent());
        }
        device = ContactsScreen.getDMContactDevice();

        sendButton = findViewById(R.id.sendButton);
        nameTextView = findViewById(R.id.nameTextView);
        backButton = findViewById(R.id.backButton);
        attachButton = findViewById(R.id.attachButton);

        inputField = findViewById(R.id.inputField);

        chatListView = findViewById(R.id.chatListView);
        chatListView.setDivider(null);
        chatDeviceAdapter = new ChatDeviceAdapter(this, messages);
        chatListView.setAdapter(chatDeviceAdapter);


        nameTextView.setText(extras.getString("key_name"));

        nameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatScreen.this, EditContactScreen.class);
                intent.putExtra("key_device", chatXbeeReceiver);
                intent.putExtra("key_user_id", chatReceiverId);
                startActivity(intent);
            }
        });

        // Handling the event of returning to the main window.
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chatDeviceAdapter.clear();
                device.close();
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
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                datetime = currentDate + " " + currentTime;
                message = inputField.getText().toString();
                message = message + "\n" + currentTime;
                if (message.isEmpty())
                {
                    message = "Empty input!";
                }
                try {
                    if (!fileFlag) {
                        device.sendData(remote, message.getBytes());
                        messages.add(message + "\n");
                        inputField.setText("");
                    } else {
                        device.sendData(remote, textViewAttachment.getText().toString().getBytes());
                        messages.add(message + "\n");
                        inputField.setText("");
                        textViewAttachment.setText("");
                        fileFlag = false;
                    }
                    /*ByteBuffer bb = ByteBuffer.wrap(array);
                    byte[] packageFile = new byte[sizeOfPackage];
                    bb.get(packageFile, 0, packageFile.length-1);
                    device.sendData(remote, packageFile);
                    messages.add(message + "\n");
                    inputField.setText("");*/


                } catch (XBeeException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    messages.add("Error transmitting message: " + e.getMessage());
                }
                SplashScreen.db.insertMessage(new Message(extras.getString("sender_id"), extras.getString("xbee_sender"), extras.getString("receiver_id"), extras.getString("xbee_receiver"),message));
                chatDeviceAdapter.notifyDataSetChanged();
            }
        });

        // Channel check events added to the device.
        device.addDataListener(listener);
        String REMOTE_NODE_ID = ContactsScreen.getSelectedDevice();

        XBee64BitAddress RemoteAddr = new XBee64BitAddress(REMOTE_NODE_ID);

        if (device.get64BitAddress().equals(REMOTE_NODE_ID)) {
            messages.add("Error: the value of the REMOTE_NODE_ID must be "
                    + "the Node Identifier (NI) of the OTHER module.");
        }
        else {
            //remote = new RemoteDigiMeshDevice(device,RemoteAddr);
            remote = new RemoteXBeeDevice(device,RemoteAddr);
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
                chatDeviceAdapter.notifyDataSetChanged();
                if( myKM.inKeyguardRestrictedInputMode() && flagNotification) {
                    String message = ContactsScreen.getSelectedDevice() + ":\n" + messages.get(messages.size() - 1);
                    /*if (!NearbyDevicesScreen.dmaDevices.isEmpty())
                    {
                        message = NearbyDevicesScreen.getSelectedDevice() + ":\n" + messages.get(messages.size() - 1);
                    } else */

                    message = removeLastChar(message);
                    notifyThis("Beechat notification", message);
                    flagNotification = false;
                }
                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    public static void updateName(){
        List<Contact> users = SplashScreen.db.getAllContacts();
        ContactsScreen.names.clear();
        ContactsScreen.xbee_user_ids.clear();
        ContactsScreen.xbee_names.clear();
        for (Contact cn : users) {
            ContactsScreen.xbee_names.add(cn.getXbeeDeviceNumber());
            ContactsScreen.xbee_user_ids.add(cn.getUserId());
            ContactsScreen.names.add(cn.getName());
        }
    }

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

    public static String getPath(Context context, Uri uri) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    return getDataColumn(context, contentUri, null, null);
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
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
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
                    /*if (filesize >= FILE_SIZE_LIMIT) {
                        Toast.makeText(this, "The selected file is too large. Select a new file with size less than 2mb", Toast.LENGTH_LONG).show();
                    } */
                    String mimeType = getContentResolver().getType(uri);
                    if (mimeType == null) {
                        String path = getPath(this, uri);
                        File file = new File(path);
                        filename = file.getName();
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
                        sizeFile = Long.toString(returnCursor.getLong(sizeIndex));

                    }

                    textViewAttachment.setText(filename + " ("+sizeFile+")");
                    tempTest = filename;
                    File fileSave = getExternalFilesDir(null);
                    String sourcePath = getExternalFilesDir(null).toString();
                    /*array = method(new File(sourcePath + "/" + filename));
                    numberOfPackage = array.length/sizeOfPackage;*/
                    fileFlag = true;
                    try {
                        copyFileStream(new File(sourcePath + "/" + filename), uri, this);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendFile() {

    }

    public static byte[] method(File file)
            throws IOException
    {

        // Creating an object of FileInputStream to
        // read from a file
        FileInputStream fl = new FileInputStream(file);

        // Now creating byte array of same length as file
        byte[] arr = new byte[(int)file.length()];

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
     *  --- ChatDeviceAdapter ----
     *  The class that is responsible for initializing the message list.
     ***/
    private class ChatDeviceAdapter extends ArrayAdapter<String> {

        private Context context;

        ChatDeviceAdapter(@NonNull Context context, ArrayList<String> messages) {
            super(context, -1, messages);
            this.context = context;
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            String message = messages.get(position);

            if (message.endsWith("S")){
                message = removeLastChar(message);

                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(40, 30, 40, 30);

                TextView nameText = new TextView(context);

                nameText.setText(message);
                nameText.setTypeface(nameText.getTypeface(), Typeface.BOLD);
                nameText.setTextSize(15);
                Drawable drawable = getResources().getDrawable(R.drawable.server_message_box);
                nameText.setBackground(drawable);
                layout.addView(nameText);


                return layout;
            }
            else {
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(40, 30, 40, 30);

                TextView nameText = new TextView(context);
                nameText.setGravity(Gravity.RIGHT);
                nameText.setText(message);
                nameText.setTypeface(nameText.getTypeface(), Typeface.BOLD);
                nameText.setTextSize(15);
                Drawable drawable = getResources().getDrawable(R.drawable.client_message_box);
                nameText.setBackground(drawable);

                LinearLayout layout2 = new LinearLayout(context);
                Drawable iconTick = getResources().getDrawable(R.drawable.sent_tick);

                ImageView imgView = new ImageView(context);
                imgView.setImageDrawable(iconTick);
                int color = Color.parseColor("#00FF00"); //The color u want
                imgView.setColorFilter(color);
                layout2.addView(imgView);
                layout2.setGravity(Gravity.RIGHT);

                layout.addView(nameText);
                layout.addView(layout2);

                return layout;
            }
        }
    }

    /***
     *  --- DataReceiveListener ----
     *  The class that is responsible for listening to the message channel.
     ***/
    private static class DataReceiveListener implements IDataReceiveListener {
        @Override
        public void dataReceived(XBeeMessage xbeeMessage) {
            String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            datetime = currentDate + " " + currentTime;
            messages.add(new String(xbeeMessage.getData()) + "\nS");

            SplashScreen.db.insertMessage(new Message(chatSenderId, chatXbeeSender,chatReceiverId,chatXbeeReceiver, new String(xbeeMessage.getData()) + "\nS"));
            flagNotification = true;
        }
    }

    /***
     *  --- removeLastChar(String) ----
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
     *  --- createNotificationChannel() ----
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
     *  --- notifyThis(String, String) ----
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