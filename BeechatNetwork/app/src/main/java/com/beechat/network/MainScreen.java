package com.beechat.network;

import android.content.Context;
import android.content.res.Resources;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import com.digi.xbee.api.listeners.IDataReceiveListener;
import com.digi.xbee.api.models.XBeeMessage;
import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.exceptions.XBeeException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.Objects;
import java.util.List;
import java.io.*;



/***
 *  --- MainScreen ---
 *  The class that is responsible for the main application window.
 ***/
public class MainScreen extends AppCompatActivity {
    Context context;
    Resources resources;
    ViewPager viewPager;
    TabLayout tabLayout;
    ArrayList<Fragment> fragments;
    static FileOutputStream outputStream;
    static DatabaseHandler db;
    DataReceiveListener listener = new DataReceiveListener();
    static ArrayList<String> contactNames = new ArrayList<>();
    static ArrayList<String> contactXbeeAddress = new ArrayList<>();
    static ArrayList<String> contactUserIds = new ArrayList<>();
    static boolean replyReceived = false;
    List<Contact> contactsFromDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);

        db = new DatabaseHandler(this);

        contactNames.clear();
        contactXbeeAddress.clear();
        contactUserIds.clear();
        contactsFromDb = db.getAllContacts(Blake3.toString(SplashScreen.myGeneratedUserId));
        for (Contact cn : contactsFromDb) {
            contactNames.add(cn.getName());
            contactXbeeAddress.add(cn.getXbeeDeviceNumber());
            contactUserIds.add(cn.getUserId());
        }


        context = LocaleHelper.setLocale(MainScreen.this, WelcomeScreen.language);
        resources = context.getResources();

        viewPager = findViewById(R.id.pager);
        tabLayout = findViewById(R.id.tabLayout);

        fragments = new ArrayList<>();

        fragments.add(new NearbyDevicesScreen());
        fragments.add(new ContactsScreen());
        fragments.add(new BroadcastScreen());
        fragments.add(new SettingsScreen());

        FragmentAdapter pagerAdapter = new FragmentAdapter(getSupportFragmentManager(), getApplicationContext(), fragments);
        viewPager.setAdapter(pagerAdapter);

        tabLayout.setupWithViewPager(viewPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.nearby_black);
        tabLayout.getTabAt(1).setIcon(R.drawable.chat_black);
        tabLayout.getTabAt(2).setIcon(R.drawable.broadcast_black);
        tabLayout.getTabAt(3).setIcon(R.drawable.settings_black);

        try {
            // Channel check events added to the device.
            SplashScreen.myXbeeDevice.addDataListener(listener);
            byte[] toSend = new Packet(
                Packet.Type.INFO
              , (short)0
              , (short)1
              , (new String("ACK" + Base58.encode(SplashScreen.myGeneratedUserId))).getBytes()
              , SplashScreen.hasher
            ).getData();
            SplashScreen.myXbeeDevice.sendBroadcastData(toSend);
        } catch (final XBeeException e) {
            e.printStackTrace();
            Objects.requireNonNull(this).runOnUiThread(() -> {
                new AlertDialog.Builder(this).setTitle(resources.getString(R.string.error_connecting_title))
                        .setMessage(resources.getString(R.string.error_connecting_description, e.getMessage()))
                        .setPositiveButton(android.R.string.ok, null).show();
            });
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            return;
        }
        NearbyDevicesScreen.devicesAN.set(data.getIntExtra("id", 0), data.getStringExtra("name"));
        contactUserIds.add(data.getStringExtra("userid"));
        contactXbeeAddress.add(data.getStringExtra("addr"));
        contactNames.add(data.getStringExtra("newname"));
        NearbyDevicesScreen.contactsFromDb.add(data.getStringExtra("userid") + " (" + data.getStringExtra("addr") + ")");
    }

    /***
     *  --- DataReceiveListener ---
     *  The class that is responsible for listening to the message channel.
     ***/
    private static class DataReceiveListener implements IDataReceiveListener {
        static Packet temp;
        static Packet.Type currentType = Packet.Type.MESSAGE_DATA;
        static short currentPart = -1;
        static short currentTotal = 1;
        String reply = new String("REPLY");
        static Message message = new Message();

        public DataReceiveListener() {
            super();
            try {
                temp = new Packet(new Blake3());
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        @Override
        public void dataReceived(XBeeMessage xbeeMessage) {
            temp.setRaw(xbeeMessage.getData());
            if (!temp.isCorrect()) {
                return;
            }
            if (currentType != temp.getType() || currentPart != temp.getPartNumber() - 1) {
                currentPart = temp.getPartNumber();
                if (currentPart != 0) {
                    currentPart = -1;
                    currentTotal = 1;
                    currentType = Packet.Type.NONE;
                    message.clear();
                    return;
                }
                currentType = temp.getType();
                currentTotal = temp.getTotalNumber();
                message.clear();
            }
            if (currentType == Packet.Type.FILE_DATA) {
                byte [] decrypted = null;
                try {
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(
                        Cipher.DECRYPT_MODE
                      , getUserSk(xbeeMessage.getDevice().get64BitAddress())
                    );
                    decrypted = cipher.doFinal(temp.getData());
                } catch (Exception e){
                    e.printStackTrace();
                }
                try {
                    outputStream.write(decrypted);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (temp.getPartNumber() == currentTotal - 1) {
                    try {
                        outputStream.flush();
                        outputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return;
            }
            if (currentType == Packet.Type.INFO) {
                if ((new String(temp.getData())).equals("REPLY")) {
                    replyReceived = true;
                    return;
                }
            }
            if (message.add(temp)) {
                currentPart++;
                if (currentType == Packet.Type.MESSAGE_DATA) {
                    byte[] toSend = new Packet(
                        Packet.Type.INFO
                      , (short)0
                      , (short)1
                      , reply.getBytes()
                      , SplashScreen.hasher
                    ).getData();
                    try {
                        SplashScreen.myXbeeDevice.sendData(
                            new RemoteXBeeDevice(
                                SplashScreen.myXbeeDevice
                              , xbeeMessage.getDevice().get64BitAddress()
                            )
                          , toSend
                        );
                    } catch (final XBeeException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!message.isReady()) {
                return;
            }

            switch (message.getType()) {
                case MESSAGE_DATA: {
                    String newMessageAddr = xbeeMessage.getDevice().get64BitAddress().toString();
                    db.insertMessage(
                        new TextMessage(
                            Blake3.toString(SplashScreen.myGeneratedUserId)
                          , SplashScreen.addressMyXbeeDevice
                          , contactUserIds.get(contactXbeeAddress.indexOf(newMessageAddr))
                          , newMessageAddr
                          , new String(message.getData()) + "\nS"
                        )
                    );
                    ChatScreen.getMessages().add(new String(message.getData()) + "\nS");
                    ChatScreen.setNotification();
                    message.clear();
                    break;
                }
                case DP_KEY: {
                    break;
                }
                case KP_KEY: {
                    break;
                }
                case KP_SHKEY: {
                    break;
                }
                case FILE_NAME_DATA: {
                    try {
                        String dir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        ).getAbsolutePath();
                        outputStream = new FileOutputStream(new File(dir + "/" + new String(temp.getData()).split(" ")[0]));
                        ChatScreen.getMessages().add(new String(message.getData()) + "\nS");
                        ChatScreen.setNotification();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case INFO: {
                    if (!(new String(temp.getData())).substring(0, 3).equals("ACK")) {
                        return;
                    }
                    String save = Blake3.toString(Base58.decode((new String(message.getData())).substring(3)));
                    int i = contactUserIds.indexOf(save);
                    if (contactUserIds.contains(save)) {
                        NearbyDevicesScreen.devicesAN.add(
                            contactNames.get(i)
                          + " ("
                          + xbeeMessage.getDevice().get64BitAddress().toString()
                          + ")"
                        );
                    } else {
                        NearbyDevicesScreen.devicesAN.add(
                            save
                          + " ("
                          + xbeeMessage.getDevice().get64BitAddress().toString()
                          + ")"
                        );
                    }
                    NearbyDevicesScreen.devicesAddress.add(
                        xbeeMessage.getDevice().get64BitAddress().toString()
                    );
                    NearbyDevicesScreen.devicesNodeIds.add(save);
                    break;
                }
            }
        }
    }
}
