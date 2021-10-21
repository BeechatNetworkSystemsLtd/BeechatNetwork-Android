package com.beechat.network;

import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.digi.xbee.api.DigiMeshDevice;
import com.digi.xbee.api.RemoteDigiMeshDevice;
import com.digi.xbee.api.exceptions.XBeeException;
import com.digi.xbee.api.listeners.IDataReceiveListener;
import com.digi.xbee.api.models.XBee64BitAddress;
import com.digi.xbee.api.models.XBeeMessage;

import java.util.ArrayList;


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
    private static boolean flagNotification = false;
    private KeyguardManager myKM= null;

    Button sendButton, backButton;
    TextView nameTextView;
    ListView chatListView;
    EditText inputField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myKM = (KeyguardManager) ChatScreen.this.getSystemService(Context.KEYGUARD_SERVICE);
        setContentView(R.layout.chat_screen);

        device = NearbyDevicesScreen.getDMDevice();

        sendButton = findViewById(R.id.sendButton);
        nameTextView = findViewById(R.id.nameTextView);
        backButton = findViewById(R.id.backButton);

        inputField = findViewById(R.id.inputField);

        chatListView = findViewById(R.id.chatListView);
        chatListView.setDivider(null);
        chatDeviceAdapter = new ChatDeviceAdapter(this, messages);
        chatListView.setAdapter(chatDeviceAdapter);

        nameTextView.setText("Chatting with " + NearbyDevicesScreen.name);

        // Handling the event of returning to the main window.
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chatDeviceAdapter.clear();
                device.close();
                finish();
                //onBackPressed();

            }
        });

        // Handling the message sending event.
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                message = inputField.getText().toString();
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
                chatDeviceAdapter.notifyDataSetChanged();
            }
        });

        // Channel check events added to the device.
        device.addDataListener(listener);

        String REMOTE_NODE_ID = NearbyDevicesScreen.getSelectedDevice();
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
                    String message = NearbyDevicesScreen.getSelectedDevice() + ":\n" + messages.get(messages.size() - 1);
                    message = removeLastChar(message);
                    notifyThis("Beechat notification", message);
                    flagNotification = false;
                }
                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                layout.addView(nameText);

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
            messages.add(new String(xbeeMessage.getData()) + "\nS");
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