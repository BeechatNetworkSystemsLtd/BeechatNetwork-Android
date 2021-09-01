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


public class ChatActivity extends AppCompatActivity {

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
        myKM = (KeyguardManager) ChatActivity.this.getSystemService(Context.KEYGUARD_SERVICE);
        MainActivity.logOnSd("ChatActivity, onCreate(Bundle savedInstanceState), 62 ");
        setContentView(R.layout.activity_chat);

        device = MainActivity.getDMDevice();

        sendButton = findViewById(R.id.sendButton);
        nameTextView = findViewById(R.id.nameTextView);
        backButton = findViewById(R.id.backButton);

        inputField = findViewById(R.id.inputField);

        chatListView = findViewById(R.id.chatListView);
        chatListView.setDivider(null);
        chatDeviceAdapter = new ChatDeviceAdapter(this, messages);
        chatListView.setAdapter(chatDeviceAdapter);

        nameTextView.setText("Chatting with " + MainActivity.getSelectedDevice());

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.logOnSd("ChatActivity, backButton.setOnClickListener(new View.OnClickListener(), 83 ");
                chatDeviceAdapter.clear();
                device.close();
                finish();
                //onBackPressed();

            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.logOnSd("ChatActivity, sendButton.setOnClickListener(new View.OnClickListener(), 94 ");
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

        device.addDataListener(listener);

        String REMOTE_NODE_ID = MainActivity.getSelectedDevice();
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

        createNotificationChannel();

        final Handler handler = new Handler();
        final int delay = 1000;

        handler.postDelayed(new Runnable() {
            public void run() {
                chatDeviceAdapter.notifyDataSetChanged();
                if( myKM.inKeyguardRestrictedInputMode() && flagNotification) {
                    String message = MainActivity.getSelectedDevice() + ":\n" + messages.get(messages.size() - 1);
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
        MainActivity.logOnSd("ChatActivity, onResume() , 148 ");
        chatDeviceAdapter.notifyDataSetChanged();
    }

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

    private static class DataReceiveListener implements IDataReceiveListener {
        @Override
        public void dataReceived(XBeeMessage xbeeMessage) {
            messages.add(new String(xbeeMessage.getData()) + "\nS");
            flagNotification = true;
        }
    }

    public static String removeLastChar(String s) {
        return (s == null || s.length() == 0)
                ? null
                : (s.substring(0, s.length() - 1));
    }

    private void createNotificationChannel() {
        MainActivity.logOnSd("ChatActivity, createNotificationChannel(), 219");
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
