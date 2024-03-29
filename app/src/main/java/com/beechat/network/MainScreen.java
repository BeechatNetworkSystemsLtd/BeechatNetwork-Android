package com.beechat.network;

import android.content.Context;
import android.content.res.Resources;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.net.Uri;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.viewpager.widget.ViewPager;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import com.digi.xbee.api.listeners.IDataReceiveListener;
import com.digi.xbee.api.models.XBeeMessage;
import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.exceptions.XBeeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.List;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.KeySpec;
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
    static String fileString;
    static FileOutputStream outputStream;
    static DatabaseHandler db;
    static Fragment linkAct;
    public static boolean userAddLock = false;
    DataReceiveListener listener = new DataReceiveListener();
    public static ArrayList<String> contactNames = new ArrayList<>();
    public static ArrayList<String> contactXbeeAddress = new ArrayList<>();
    public static ArrayList<String> contactUserIds = new ArrayList<>();
    static boolean replyReceived = false;
    int FILE_SELECT_CODE = 101;
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

        //fragments.add(new NearbyDevicesScreen());
        linkAct = new ContactsScreen();
        fragments.add(linkAct);
        //fragments.add(new BroadcastScreen());
        fragments.add(new SettingsScreen());

        FragmentAdapter pagerAdapter = new FragmentAdapter(getSupportFragmentManager(), getApplicationContext(), fragments);
        viewPager.setAdapter(pagerAdapter);

        tabLayout.setupWithViewPager(viewPager);

        //tabLayout.getTabAt(0).setIcon(R.drawable.nearby_black);
        tabLayout.getTabAt(0).setIcon(R.drawable.chat_black);
        //tabLayout.getTabAt(2).setIcon(R.drawable.broadcast_black);
        tabLayout.getTabAt(1).setIcon(R.drawable.settings_black);

        if (SplashScreen.myXbeeDevice.isOpen() == false) {
            return;
        }
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

    // AES key derived from a password
    public static SecretKey getAESKeyFromPassword(String password, String salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            // iterationCount = 65536
            // keyLength = 256
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
            SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            return secret;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
    }

    /***
     *  --- DataReceiveListener ---
     *  The class that is responsible for listening to the message channel.
     ***/
    private static class DataReceiveListener implements IDataReceiveListener {
        static Packet temp;
        static Blake3 tempBlake;
        static Packet.Type currentType = Packet.Type.NONE;
        static int currentPart = -1;
        static int currentTotal = 1;
        static boolean shortMesFlag = true;
        static Message message = new Message();

        public DataReceiveListener() {
            super();
            try {
                tempBlake = new Blake3();
                temp = new Packet(tempBlake);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        @Override
        public void dataReceived(XBeeMessage xbeeMessage) {
            temp = new Packet(tempBlake);
            temp.setRaw(xbeeMessage.getData());
            if (shortMesFlag) {
                message.Clear();
                shortMesFlag = false;
            }
            if (temp.getType() == Packet.Type.MESSAGE_DATA) {
                if (temp.getTotalNumber() == 1) {
                    shortMesFlag = true;
                }
            }
            if (!temp.isCorrect()) {
                return;
            }
            if (currentType != temp.getType()) {
                currentPart = temp.getPartNumber();
                if (currentPart != 0) {
                    currentPart = -1;
                    currentTotal = 1;
                    currentType = Packet.Type.NONE;
                    message.Clear();
                    return;
                }
                currentType = temp.getType();
                currentTotal = temp.getTotalNumber();
                message.Clear();
            }
            if (currentType == Packet.Type.FILE_DATA) {
                try {
                    outputStream.write(temp.getData());
                    ChatScreen.setFileDelivery((float)temp.getPartNumber() / (float)currentTotal);
                    ChatScreen.setFileText(fileString, (int)(100 * ((float)temp.getPartNumber() / (float)currentTotal)));
                    if (temp.getPartNumber() == currentTotal - 1) {
                        outputStream.flush();
                        outputStream.close();
                        ChatScreen.setFileDelivery(0);
                        ChatScreen.setFileText("", -1);
                        ChatScreen.getMessages().add(
                            new String(fileString + "\nF")
                        );
                        ChatScreen.setNotification();
                        currentType = Packet.Type.NONE;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            if (message.Add(temp)) {
                currentPart++;
            }
            if (message.isReady()) {
                processMessage(xbeeMessage);
                currentType = Packet.Type.NONE;
                message.Clear();
            }
        }


        private void processMessage(XBeeMessage xbeeMessage) {
            switch (message.getType()) {
                case MESSAGE_DATA: {
                    addNewMessage(xbeeMessage);
                    break;
                }
                case DP_KEY: {
                    signRandomHash(xbeeMessage);
                    break;
                }
                case SIGNED_HASH: {
                    verifyRandomHash(xbeeMessage);
                    break;
                }
                case KP_KEY: {
                    getKyberPublic(xbeeMessage);
                    break;
                }
                case CHIPHER: {
                    acceptChipher(xbeeMessage);
                    break;
                }
                case FILE_NAME_DATA: {
                    createNewFile(xbeeMessage);
                    break;
                }
                case INFO: {
                    infoHandler(xbeeMessage);
                    break;
                }
            }
        }


        private void signRandomHash(XBeeMessage xbeeMessage) {
            userAddLock = true;
            byte[] sm = new byte[Dilithium.CRYPTO_BYTES + message.getData().length];

            Dilithium.crypto_sign(sm, message.getData(), message.getData().length, SplashScreen.myDSKey);

            byte[] smKey = new byte[sm.length + Dilithium.CRYPTO_PUBLICKEYBYTES];
            System.arraycopy(SplashScreen.myDPKey, 0, smKey, 0, Dilithium.CRYPTO_PUBLICKEYBYTES);
            System.arraycopy(sm, 0, smKey, Dilithium.CRYPTO_PUBLICKEYBYTES, sm.length);

            Message getDP = new Message(Packet.Type.SIGNED_HASH, smKey);
            try {
                getDP.send(
                    SplashScreen.myXbeeDevice
                  , new RemoteXBeeDevice(
                        SplashScreen.myXbeeDevice
                        , xbeeMessage.getDevice().get64BitAddress()
                    )
                  , SplashScreen.hasher
                );
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }

        }


        private void verifyRandomHash(XBeeMessage xbeeMessage) {
            byte[] userKey = new byte[Dilithium.CRYPTO_PUBLICKEYBYTES];
            System.arraycopy(message.getData(), 0, userKey, 0, Dilithium.CRYPTO_PUBLICKEYBYTES);
            byte[] signedHash = new byte[message.getData().length - Dilithium.CRYPTO_PUBLICKEYBYTES];
            System.arraycopy(message.getData(), Dilithium.CRYPTO_PUBLICKEYBYTES, signedHash, 0, message.getData().length - Dilithium.CRYPTO_PUBLICKEYBYTES);
            byte[] m = new byte[signedHash.length - Dilithium.CRYPTO_BYTES];

            Dilithium.crypto_sign_open(m, signedHash, signedHash.length, userKey);

            if (!Arrays.equals(m, SplashScreen.randomHash)) {
                return;
            }

            Message getDP = new Message(Packet.Type.KP_KEY, SplashScreen.myKPKey);
            try {
                getDP.send(
                    SplashScreen.myXbeeDevice
                  , new RemoteXBeeDevice(
                        SplashScreen.myXbeeDevice
                        , xbeeMessage.getDevice().get64BitAddress()
                    )
                  , SplashScreen.hasher
                );
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
            ContactsScreen.contactInfos.remove(ContactsScreen.contactInfos.size() - 1);
            ContactsScreen.ContactInfo ci = new ContactsScreen.ContactInfo();
            ci.name = "Verify ...";
            ci.label = NearbyDevicesScreen.cname.substring(0, 1);
            ci.mes = "";
            ci.date = "";
            ContactsScreen.contactInfos.add(ci);
            linkAct.getActivity().runOnUiThread(() -> {
                ContactsScreen.onRefresh();
            });
        }


        private void getKyberPublic(XBeeMessage xbeeMessage) {
            byte[] bob_skey = new byte[Kyber512.KYBER_SSBYTES];
            byte[] ct = new byte[Kyber512.KYBER_CIPHERTEXTBYTES];

            Kyber512.crypto_kem_enc(ct, bob_skey, message.getData());

            try {
                SplashScreen.hasher.clear();
                SplashScreen.hasher.update(bob_skey, Kyber512.KYBER_SSBYTES);
                byte[] secret = SplashScreen.hasher.finalize(256);
                db.addKey(xbeeMessage.getDevice().get64BitAddress().toString(), secret);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }

            Message getDP = new Message(Packet.Type.CHIPHER, ct);
            try {
                getDP.send(
                    SplashScreen.myXbeeDevice
                  , new RemoteXBeeDevice(
                        SplashScreen.myXbeeDevice
                        , xbeeMessage.getDevice().get64BitAddress()
                    )
                  , SplashScreen.hasher
                );
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
            userAddLock = false;
        }


        private void acceptChipher(XBeeMessage xbeeMessage) {
            byte[] alice_skey = new byte[Kyber512.KYBER_SSBYTES];
            Kyber512.crypto_kem_dec(alice_skey, message.getData(), SplashScreen.myKSKey);

            try {
                SplashScreen.hasher.clear();
                SplashScreen.hasher.update(alice_skey, Kyber512.KYBER_SSBYTES);
                byte[] secret = SplashScreen.hasher.finalize(256);
                db.addKey(xbeeMessage.getDevice().get64BitAddress().toString(), secret);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }

            db.addContact(
                new Contact(
                    NearbyDevicesScreen.cselectedUserId
                  , NearbyDevicesScreen.cselectedXbeeDevice
                  , NearbyDevicesScreen.cname
                  , Blake3.toString(SplashScreen.myGeneratedUserId)
                )
            );
            ContactsScreen.contactInfos.remove(ContactsScreen.contactInfos.size() - 1);
            ContactsScreen.ContactInfo ci = new ContactsScreen.ContactInfo();
            ci.name = NearbyDevicesScreen.cname;
            ci.label = NearbyDevicesScreen.cname.substring(0, 1);
            ci.mes = "";
            ci.date = "";
            ContactsScreen.contactInfos.add(ci);
            ContactsScreen.contactNames.add(NearbyDevicesScreen.cname);
            ContactsScreen.contactUserIds.add(NearbyDevicesScreen.cselectedUserId);
            ContactsScreen.contactXbeeAddress.add(NearbyDevicesScreen.cselectedXbeeDevice);
            linkAct.getActivity().runOnUiThread(() -> {
                ContactsScreen.onRefresh();
            });
        }


        private void addNewMessage(XBeeMessage xbeeMessage) {
            String decrypt = "", newMessageAddr = "";
            try {
                Cipher cipher = Cipher.getInstance("AES");
                newMessageAddr = xbeeMessage.getDevice().get64BitAddress().toString();
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(db.getKey(newMessageAddr), "AES"));
                decrypt = new String(cipher.doFinal(message.getData()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            db.insertMessage(
                new TextMessage(
                    Blake3.toString(SplashScreen.myGeneratedUserId)
                  , SplashScreen.addressMyXbeeDevice
                  , contactUserIds.get(contactXbeeAddress.indexOf(newMessageAddr))
                  , newMessageAddr
                  , decrypt + "\nS"
                )
            );
            ChatScreen.getMessages().add(decrypt + "\nS");
            ChatScreen.setNotification();
            int i = contactXbeeAddress.indexOf(newMessageAddr);
            String searchedName = contactNames.get(i);
            for (ContactsScreen.ContactInfo cn: ContactsScreen.contactInfos) {
                if (cn.name.equals(searchedName)) {
                    String[] mesData = decrypt.split("\n");
                    cn.mes = mesData[0];
                    ContactsScreen.onRefresh();
                    break;
                }
            }
        }


        private void createNewFile(XBeeMessage xbeeMessage) {
            try {
                Cipher cipher = Cipher.getInstance("AES");
                String newMessageAddr =
                    xbeeMessage.getDevice().get64BitAddress().toString();
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(db.getKey(newMessageAddr), "AES"));
                String decrypt = new String(cipher.doFinal(message.getData()));
                String dir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ).getAbsolutePath() + File.separator + "Beechat";

                (new File(dir)).mkdirs();

                outputStream = new FileOutputStream(
                    new File(
                        dir + File.separator + decrypt.split(" ")[0]
                    )
                );
                fileString = decrypt;
                if (fileString.length() > 34) fileString = fileString.substring(fileString.length() - 20);

                db.insertMessage(
                    new TextMessage(
                        Blake3.toString(SplashScreen.myGeneratedUserId)
                      , SplashScreen.addressMyXbeeDevice
                      , contactUserIds.get(contactXbeeAddress.indexOf(newMessageAddr))
                      , newMessageAddr
                      , new String(fileString + "\nF")
                    )
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        private void infoHandler(XBeeMessage xbeeMessage) {
            if (!(new String(temp.getData())).substring(0, 3).equals("ACK")) {
                return;
            }
            String save = Blake3.toString(
                Base58.decode((new String(message.getData())).substring(3))
            );
            int i = contactUserIds.indexOf(save);
            if (NearbyDevicesScreen.devicesNodeIds.contains(save)) {
                return;
            }

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
        }
    }
}
