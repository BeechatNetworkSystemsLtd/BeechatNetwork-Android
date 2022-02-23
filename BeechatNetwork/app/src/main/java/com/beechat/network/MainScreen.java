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
    DataReceiveListener listener = new DataReceiveListener();
    static ArrayList<String> contactNames = new ArrayList<>();
    static ArrayList<String> contactXbeeAddress = new ArrayList<>();
    static ArrayList<String> contactUserIds = new ArrayList<>();
    static String cname, cselectedUserId, cselectedXbeeDevice;
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

        fragments.add(new NearbyDevicesScreen());
        linkAct = new ContactsScreen();
        fragments.add(linkAct);
        fragments.add(new BroadcastScreen());
        fragments.add(new SettingsScreen());

        FragmentAdapter pagerAdapter = new FragmentAdapter(getSupportFragmentManager(), getApplicationContext(), fragments);
        viewPager.setAdapter(pagerAdapter);

        tabLayout.setupWithViewPager(viewPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.nearby_black);
        tabLayout.getTabAt(1).setIcon(R.drawable.chat_black);
        tabLayout.getTabAt(2).setIcon(R.drawable.broadcast_black);
        tabLayout.getTabAt(3).setIcon(R.drawable.settings_black);

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
        if (requestCode == 65536 + 14) {
            String cw = data.getStringExtra("cw");
            SecretKey skey = getAESKeyFromPassword(cw, "AES");
            try {
                String dir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ).getAbsolutePath();
                FileOutputStream outputStream = new FileOutputStream(
                    new File(
                        dir + "/" + SplashScreen.neo.getLogo() + ".user"
                    )
                );
                int seek = 0;
                byte[] towrite = new byte[
                    User.NODEID_SIZE * 2
                  + User.PASS_HASH_SIZE * 2
                  + Dilithium.CRYPTO_PUBLICKEYBYTES
                  + Dilithium.CRYPTO_SECRETKEYBYTES
                  + Kyber512.KYBER_PUBLICKEYBYTES
                  + Kyber512.KYBER_SECRETKEYBYTES
                  + SplashScreen.neo.getLogo().length()
                ];

                System.arraycopy(SplashScreen.neo.getUsername().getBytes(), 0, towrite, seek, User.NODEID_SIZE * 2);
                seek += User.NODEID_SIZE * 2;

                System.arraycopy(SplashScreen.neo.getPassword().getBytes(), 0, towrite, seek, User.PASS_HASH_SIZE * 2);
                seek += User.PASS_HASH_SIZE * 2;

                System.arraycopy(SplashScreen.neo.getDPubKey(), 0, towrite, seek, Dilithium.CRYPTO_PUBLICKEYBYTES);
                seek += Dilithium.CRYPTO_PUBLICKEYBYTES;

                System.arraycopy(SplashScreen.neo.getDPrivKey(), 0, towrite, seek, Dilithium.CRYPTO_SECRETKEYBYTES);
                seek += Dilithium.CRYPTO_SECRETKEYBYTES;

                System.arraycopy(SplashScreen.neo.getKPubKey(), 0, towrite, seek, Kyber512.KYBER_PUBLICKEYBYTES);
                seek += Kyber512.KYBER_PUBLICKEYBYTES;

                System.arraycopy(SplashScreen.neo.getKPrivKey(), 0, towrite, seek, Kyber512.KYBER_SECRETKEYBYTES);
                seek += Kyber512.KYBER_SECRETKEYBYTES;

                System.arraycopy(SplashScreen.neo.getLogo().getBytes(), 0, towrite, seek, SplashScreen.neo.getLogo().length());

                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, skey);
                outputStream.write(cipher.doFinal(towrite));
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Export error!", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Current user was exported!", Toast.LENGTH_SHORT).show();
            return;
        }
        cname = data.getStringExtra("newname");
        cselectedUserId = data.getStringExtra("userid");
        cselectedXbeeDevice = data.getStringExtra("addr");
        NearbyDevicesScreen.devicesAN.set(data.getIntExtra("id", 0), data.getStringExtra("name"));
        contactUserIds.add(cselectedUserId);
        contactXbeeAddress.add(cselectedXbeeDevice);
        contactNames.add(cname);
        NearbyDevicesScreen.contactsFromDb.add(data.getStringExtra("userid") + " (" + data.getStringExtra("addr") + ")");
    }

    /***
     *  --- DataReceiveListener ---
     *  The class that is responsible for listening to the message channel.
     ***/
    private static class DataReceiveListener implements IDataReceiveListener {
        static Packet temp;
        static Blake3 tempBlake;
        static Packet.Type currentType = Packet.Type.MESSAGE_DATA;
        static int currentPart = -1;
        static int currentTotal = 1;
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
                    ChatScreen.setFileDelivery((float)temp.getPartNumber() / currentTotal);
                    ChatScreen.setFileText(fileString, (int)(100 * (float)temp.getPartNumber() / currentTotal));
                    if (temp.getPartNumber() == currentTotal - 1) {
                        outputStream.flush();
                        outputStream.close();
                        ChatScreen.setFileDelivery(0);
                        ChatScreen.setFileText("", 0);
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
                    cselectedUserId
                  , cselectedXbeeDevice
                  , cname
                  , Blake3.toString(SplashScreen.myGeneratedUserId)
                )
            );
            ContactsScreen.contactNames.add(cname);
            ContactsScreen.contactUserIds.add(cselectedUserId);
            ContactsScreen.contactXbeeAddress.add(cselectedXbeeDevice);
            linkAct.getActivity().runOnUiThread(() -> {
                ContactsScreen.onRefresh();
            });
        }


        private void addNewMessage(XBeeMessage xbeeMessage) {
            String newMessageAddr =
                xbeeMessage.getDevice().get64BitAddress().toString();
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
            message.Clear();
        }


        private void createNewFile(XBeeMessage xbeeMessage) {
            try {
                String dir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ).getAbsolutePath();
                outputStream = new FileOutputStream(
                    new File(
                        dir + "/" + new String(message.getData()).split(" ")[0]
                    )
                );
                fileString = new String(message.getData());
                if (fileString.length() > 34) fileString = fileString.substring(fileString.length() - 20);
                ChatScreen.getMessages().add(
                    new String(message.getData()) + "\nS"
                );
                ChatScreen.setNotification();
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
