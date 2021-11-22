package com.beechat.network;

import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
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

import com.digi.xbee.api.DigiMeshDevice;
import com.digi.xbee.api.RemoteDigiMeshDevice;
import com.digi.xbee.api.exceptions.XBeeException;
import com.digi.xbee.api.listeners.IDataReceiveListener;
import com.digi.xbee.api.models.XBee64BitAddress;
import com.digi.xbee.api.models.XBeeMessage;

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
    private static DigiMeshDevice device;
    private static DataReceiveListener listener = new DataReceiveListener();
    private static ChatDeviceAdapter chatDeviceAdapter;
    private static ArrayList<String> messages = new ArrayList<>();
    private static RemoteDigiMeshDevice remote = null;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = LocaleHelper.setLocale(ChatScreen.this, SelectLanguageScreen.language);
        resources = context.getResources();
        myKM = (KeyguardManager) ChatScreen.this.getSystemService(Context.KEYGUARD_SERVICE);
        setContentView(R.layout.chat_screen);

        List<Message> messagesDB = SplashScreen.db.getAllMessages();
        for (Message mg : messagesDB)
        {
            messages.add(mg.getContent());
        }
        /*if (!NearbyDevicesScreen.dmaDevices.isEmpty()) {
            device = NearbyDevicesScreen.getDMDevice();
        } else*/ device = ContactsScreen.getDMContactDevice();

        sendButton = findViewById(R.id.sendButton);
        nameTextView = findViewById(R.id.nameTextView);
        backButton = findViewById(R.id.backButton);
        attachButton = findViewById(R.id.attachButton);

        inputField = findViewById(R.id.inputField);

        chatListView = findViewById(R.id.chatListView);
        chatListView.setDivider(null);
        chatDeviceAdapter = new ChatDeviceAdapter(this, messages);
        chatListView.setAdapter(chatDeviceAdapter);

        // Reading all users
        System.out.println("Reading: " + "Reading all users..");
        List<User> users = SplashScreen.db.getAllUsers();

        /*if (!NearbyDevicesScreen.dmaDevices.isEmpty()){
        if (NearbyDevicesScreen.name != null) {
            test = NearbyDevicesScreen.name.toString();
        }
        else  {
            test = NearbyDevicesScreen.names.get(0);
        }} else*/ test = ContactsScreen.contacts.get(0);
        nameTextView.setText(test);

        nameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatScreen.this, EditContactScreen.class);
                intent.putExtra("key", ContactsScreen.xbee_names.get(0));
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
                    device.sendData(remote, message.getBytes());
                    messages.add(message + "\n");
                    inputField.setText("");
                } catch (XBeeException e) {
                    messages.add("Error transmitting message: " + e.getMessage());
                }
                /*if (!NearbyDevicesScreen.dmaDevices.isEmpty()) {
                    SplashScreen.db.insertMessage(new Message(NearbyDevicesScreen.senderId, NearbyDevicesScreen.receiverId, message, datetime));
                } else*/ SplashScreen.db.insertMessage(new Message(ContactsScreen.senderId, ContactsScreen.receiverId, message, datetime));
                chatDeviceAdapter.notifyDataSetChanged();
            }
        });

        // Channel check events added to the device.
        device.addDataListener(listener);
        String REMOTE_NODE_ID = ContactsScreen.getSelectedDevice();
        /*if (!NearbyDevicesScreen.dmaDevices.isEmpty()) {
            REMOTE_NODE_ID = NearbyDevicesScreen.getSelectedDevice();
        } else {
            REMOTE_NODE_ID = ContactsScreen.getSelectedDevice();
        }*/

        XBee64BitAddress RemoteAddr = new XBee64BitAddress(REMOTE_NODE_ID);

        if (device.get64BitAddress().equals(REMOTE_NODE_ID)) {
            messages.add("Error: the value of the REMOTE_NODE_ID must be "
                    + "the Node Identifier (NI) of the OTHER module.");
        }
        else {
            remote = new RemoteDigiMeshDevice(device,RemoteAddr);
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
        List<User> users = SplashScreen.db.getAllUsers();
        ContactsScreen.names.clear();
        ContactsScreen.xbee_names.clear();
        for (User cn : users) {
            ContactsScreen.xbee_names.add(cn.getXbeeDeviceNumber());
            ContactsScreen.names.add(cn.getName());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateName();
        chatDeviceAdapter.notifyDataSetChanged();
    }

    /*public void onClick(View view) {
        Intent intent = new Intent(ChatScreen.this, AddContactScreen.class);
        intent.putExtra("key", ContactsScreen.xbee_names.get(0));
        startActivity(intent);
    }*/

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
            /*if (!NearbyDevicesScreen.dmaDevices.isEmpty()) {
                SplashScreen.db.insertMessage(new Message(NearbyDevicesScreen.senderId, NearbyDevicesScreen.receiverId, new String(xbeeMessage.getData()) + "\nS", datetime));
            } else*/ SplashScreen.db.insertMessage(new Message(ContactsScreen.senderId, ContactsScreen.receiverId, new String(xbeeMessage.getData()) + "\nS", datetime));
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