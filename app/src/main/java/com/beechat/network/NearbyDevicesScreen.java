package com.beechat.network;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.XBeeNetwork;
import com.digi.xbee.api.android.XBeeDevice;
import com.digi.xbee.api.android.connection.usb.AndroidUSBPermissionListener;
import com.digi.xbee.api.exceptions.XBeeException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/***
 *  --- NearbyDevicesScreen ---
 *  The class is responsible for the search and display ids of XBEE devices.
 ***/
public class NearbyDevicesScreen extends AppCompatActivity {
    Context context;
    Resources resources;
    CustomDeviceAdapter remoteXBeeDeviceAdapter;
    ListView devicesListView;
    ImageButton refreshButton;
    ImageButton pingButton;
    ImageButton backButton;
    TextView devicesLabel;
    DatabaseHandler db;
    String selectedXbeeDevice, selectedUserId;
    public static String cname, cselectedUserId, cselectedXbeeDevice;

    public static ArrayList<String> devicesAN = new ArrayList<>();
    public static ArrayList<String> devicesAddress = new ArrayList<>();
    public static ArrayList<String> devicesNodeIds = new ArrayList<>();

    List<Contact> contacts;
    public static ArrayList<String> contactsFromDb = new ArrayList<>();

    // Constants.
    private static final File root = new File(String.valueOf(Environment.getExternalStorageDirectory()));
    private static final String sFileName = "log.txt";
    private static final File gpxfile = new File(root, sFileName);
    private static final String separator = System.getProperty("line.separator");
    private static final File fdelete = new File(gpxfile.getPath());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nearby_devices_screen);
        context = LocaleHelper.setLocale(Objects.requireNonNull(this), WelcomeScreen.language);
        resources = context.getResources();

        db = new DatabaseHandler(Objects.requireNonNull(this));

        contactsFromDb.clear();

        contacts = db.getAllContacts(Blake3.toString(SplashScreen.myGeneratedUserId));

        for (Contact cn : contacts) {
            contactsFromDb.add(cn.getUserId() + " (" + cn.getXbeeDeviceNumber() + ")");
        }

        devicesLabel = findViewById(R.id.devicesLabelTextView);
        refreshButton = findViewById(R.id.refreshButton);
        pingButton = findViewById(R.id.pingButton);
        backButton = findViewById(R.id.backButton2);

        devicesListView = findViewById(R.id.devicesListView);
        remoteXBeeDeviceAdapter = new CustomDeviceAdapter(Objects.requireNonNull(Objects.requireNonNull(this)), devicesAN);
        devicesListView.setAdapter(remoteXBeeDeviceAdapter);

        // Handling an event on clicking an item from the list of available devices.
        devicesListView.setOnItemClickListener((adapterView, view, i, l) -> {

            selectedXbeeDevice = devicesAddress.get(i);
            selectedUserId = devicesNodeIds.get(i);

            if (!contactsFromDb.isEmpty()) {
                if (contactsFromDb.contains(selectedUserId + " (" + selectedXbeeDevice + ")")) {
                    Toast.makeText(Objects.requireNonNull(this), "The selected Beenode is already a contact, please navigate to Conversations window and click on the contact to begin chat.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Intent intent = new Intent(Objects.requireNonNull(this), AddContactScreen.class);
            intent.putExtra("key_selectedXbeeDevice", selectedXbeeDevice);
            intent.putExtra("key_selectedUserId", selectedUserId);
            intent.putExtra("num_selected", i);
            startActivityForResult(intent, 1);
        });

        // Handling and event  by clicking the "Refresh" button.
        refreshButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                remoteXBeeDeviceAdapter.clear();
                startScan();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        pingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    // Channel check events added to the device.
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
                }
            }
        });

        // Request for permission to access phone ports.
        requestPermission();

        // Checking the conditions for permission to create and write a file with a log to the phone.
        if (shouldAskPermissions()) {
            askPermissions();
        } else {
            if (fdelete.exists()) {
                if (fdelete.delete()) {
                    System.out.println("File log.txt deleted :" + gpxfile.getPath());
                } else {
                    System.out.println("file log.txt not deleted :" + gpxfile.getPath());
                }
            }
        }

        new Thread(() -> {
            try {
                while (true) {
                    Objects.requireNonNull(this).runOnUiThread(() -> {
                        remoteXBeeDeviceAdapter.notifyDataSetChanged();
                    });
                    Thread.sleep(500);
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        cname = data.getStringExtra("newname");
        cselectedUserId = data.getStringExtra("userid");
        cselectedXbeeDevice = data.getStringExtra("addr");
        devicesAN.set(data.getIntExtra("id", 0), data.getStringExtra("name"));
        contactsFromDb.add(data.getStringExtra("userid") + " (" + data.getStringExtra("addr") + ")");
        MainScreen.contactUserIds.add(cselectedUserId);
        MainScreen.contactXbeeAddress.add(cselectedXbeeDevice);
        MainScreen.contactNames.add(cname);
    }

    /***
     *  --- logOnSd(String) ---
     *  The function of writing information to a log file on the phone.
     *
     *  @param sBody Logged message text.
     ***/
    public static void logOnSd(String sBody) {
        try {
            if (!root.exists()) {
                root.mkdirs();
            }
            Date currentTime = Calendar.getInstance().getTime();
            FileWriter writer = new FileWriter(gpxfile.getAbsoluteFile(), true);
            writer.append(separator);
            writer.append(currentTime.toString() + " ," + sBody);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     *  --- shouldAskPermissions() ---
     *  The function of checking for permission to create and write information to the phone.
     ***/
    protected boolean shouldAskPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return false;
        }
        int permission = ContextCompat.checkSelfPermission(Objects.requireNonNull(this), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return (permission != PackageManager.PERMISSION_GRANTED);
    }

    /***
     *  --- askPermissions() ---
     *  The function of requesting permission to access the internal storage of the phone.
     ***/
    protected void askPermissions() {
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
        int permission = ActivityCompat.checkSelfPermission(Objects.requireNonNull(this), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    Objects.requireNonNull(this), permissions, 1);
        }
    }

    /***
     *  --- requestPermission() ---
     *  The function of requesting permission to access the ports of the phone.
     ***/
    private void requestPermission() {
        AndroidUSBPermissionListener permissionListener = permissionGranted -> {
            if (permissionGranted) {
                System.out.println("User granted USB permission.");
            } else
                System.out.println("User rejected USB permission.");
        };
    }

    /***
     *  --- startScan() ---
     *  The function of starting scanning for available Xbee devices.
     ***/
    private void startScan() {
        final ProgressDialog dialog = ProgressDialog.show(Objects.requireNonNull(this), resources.getString(R.string.scanning_device_title),
                resources.getString(R.string.scanning_devices), true);

        new Thread(() -> {
            try {
                listNodes(SplashScreen.myXbeeDevice);

                Objects.requireNonNull(this).runOnUiThread(() -> {
                    dialog.dismiss();
                    remoteXBeeDeviceAdapter.notifyDataSetChanged();
                });


            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        remoteXBeeDeviceAdapter.notifyDataSetChanged();
    }

    /***
     *  --- CustomDeviceAdapter ---
     *  The class that initializes the list of available devices.
     ***/
    class CustomDeviceAdapter extends ArrayAdapter<String> {
        private final Context context;

        CustomDeviceAdapter(@NonNull Context context, ArrayList<String> devices) {
            super(context, 0, devices);
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            String device = devicesAN.get(position);
            View listItem = convertView;
            if (listItem == null) {
                listItem = LayoutInflater.from(context).inflate(R.layout.nearby_layout, parent, false);
            }

            TextView nameText = (TextView)listItem.findViewById(R.id.nearby_name);
            nameText.setText(device);

            return listItem;
        }
    }

    /***
     *  --- listNodes(XBeeDevice) ---
     *  The function searches for available devices in network.
     *
     *  @param myDevice Current device.
     ***/
    private List<RemoteXBeeDevice> listNodes(XBeeDevice myDevice) {
        List<RemoteXBeeDevice> devices = null;

        XBeeNetwork network = myDevice.getNetwork();
        try {
            network.setDiscoveryTimeout(10000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        network.startDiscoveryProcess();

        while (network.isDiscoveryRunning()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        devices = network.getDevices();
        int i = 0;
        while (i < devices.size()) {
            Contact existContact = null;
            Boolean userExist = false;
            for (Contact cn : contacts) {
                if (cn.getUserId().equals(Blake3.toString(Base58.decode(devices.get(i).getNodeID())))) {
                    userExist = true;
                    existContact = cn;
                }
            }
            if (userExist) {
                devicesAN.add(existContact.getName() + " (" + devices.get(i).get64BitAddress().toString() + ")");
            } else {
                devicesAN.add(Blake3.toString(Base58.decode(devices.get(i).getNodeID())) + " (" + devices.get(i).get64BitAddress().toString() + ")");
            }
            devicesAddress.add(devices.get(i).get64BitAddress().toString());
            devicesNodeIds.add(Blake3.toString(Base58.decode(devices.get(i).getNodeID())));
            i = i + 1;
        }
        return devices;
    }


}
