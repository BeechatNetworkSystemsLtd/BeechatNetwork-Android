package com.beechat.network;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import com.digi.xbee.api.listeners.IDataReceiveListener;
import com.digi.xbee.api.models.XBeeMessage;
import com.digi.xbee.api.exceptions.XBeeException;

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

    /***
     *  --- DataReceiveListener ---
     *  The class that is responsible for listening to the message channel.
     ***/
    private static class DataReceiveListener implements IDataReceiveListener {
        static Packet temp;
        static Packet.Type currentType = Packet.Type.MESSAGE_DATA;
        static short currentPart = 0;
        static short currentTotal = 1;

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
            if (temp.isCorrect()) {
                ChatScreen.getMessages().add(new String(temp.getData()) + "\nS");
            }
            if (currentType != temp.getType()) {
                currentTotal = 0;
                currentPart = 0;
                currentType = temp.getType();
                currentTotal = temp.getTotalNumber();
            }
            if (currentType == Packet.Type.FILE_DATA) {
                try {
                    outputStream.write(temp.getData());
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
            if (temp.getPartNumber() == currentTotal - 1) {
                if (currentType == Packet.Type.MESSAGE_DATA) {
                    int i = contactUserIds.indexOf(xbeeMessage.getDevice().getNodeID());
                    db.insertMessage(
                        new Message(
                            Blake3.toString(SplashScreen.myGeneratedUserId)
                          , SplashScreen.addressMyXbeeDevice
                          , contactNames.get(i)
                          , Blake3.toString(Base58.decode(xbeeMessage.getDevice().getNodeID()))
                          , new String(temp.getData()) + "\nS"
                        )
                    );
                    ChatScreen.setNotification();
                }
                if (currentType == Packet.Type.DP_KEY) {
                }
                if (currentType == Packet.Type.KP_KEY) {
                }
                if (currentType == Packet.Type.FILE_NAME_DATA) {
                    try {
                        String dir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        ).getAbsolutePath();
                        outputStream = new FileOutputStream(new File(dir + "/" + new String(temp.getData())));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (currentType == Packet.Type.INFO) {
                    //int i = contactXbeeAddress.indexOf(xbeeMessage.getDevice().get64BitAddress().toString());
                    if ((new String(temp.getData())).substring(0, 3).equals("ACK")) {
                        String save = Blake3.toString(Base58.decode((new String(temp.getData())).substring(3)));
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
                    }
                }
            }
        }
    }
}
