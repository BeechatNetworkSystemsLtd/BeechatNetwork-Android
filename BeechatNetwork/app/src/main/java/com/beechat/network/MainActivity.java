package com.beechat.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.digi.xbee.api.DigiMeshNetwork;
import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.android.DigiMeshDevice;
import com.digi.xbee.api.android.connection.usb.AndroidUSBPermissionListener;
import com.digi.xbee.api.exceptions.XBeeException;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    // Constants.
    private static final int BAUD_RATE = 57600;

    // Variables.
    private AndroidUSBPermissionListener permissionListener;
    private CustomDeviceAdapter remoteXBeeDeviceAdapter;
    private static String selectedDevice = null;

    private static DigiMeshDevice myDevice;
    private static ArrayList<String> dmaDevices = new ArrayList<>();

    ListView devicesListView;
    Button refreshButton;
    TextView listDevicesText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listDevicesText = (TextView) findViewById(R.id.textView);
        refreshButton = (Button)findViewById(R.id.refreshButton);

        devicesListView = (ListView)findViewById(R.id.devicesListView);
        remoteXBeeDeviceAdapter = new CustomDeviceAdapter(this, dmaDevices);
        devicesListView.setAdapter(remoteXBeeDeviceAdapter);

        devicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedDevice = remoteXBeeDeviceAdapter.getItem(i);
                connectToDevice(selectedDevice);
            }
        });
        refreshButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                remoteXBeeDeviceAdapter.clear();
                startScan();
            }
        });
        requestPermission();
    }

    private void requestPermission() {
        permissionListener = new AndroidUSBPermissionListener() {
            @Override
            public void permissionReceived(boolean permissionGranted) {
                if (permissionGranted)
                    System.out.println("User granted USB permission.");
                else
                    System.out.println("User rejected USB permission.");
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        remoteXBeeDeviceAdapter.clear();
        startScan();
    }

    private void startScan() {
        myDevice = new DigiMeshDevice(MainActivity.this, BAUD_RATE, permissionListener);
        listnodes(myDevice);
        remoteXBeeDeviceAdapter.notifyDataSetChanged();
    }

    public static DigiMeshDevice getDMDevice() {return myDevice;}

    public static String getSelectedDevice() {
        return selectedDevice;
    }

    private void connectToDevice(final String device) {
        final ProgressDialog dialog = ProgressDialog.show(this, getResources().getString(R.string.connecting_device_title),
                getResources().getString(R.string.connecting_device_description), true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    myDevice.open();

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                            startActivity(intent);
                        }
                    });
                } catch (final XBeeException e) {
                    e.printStackTrace();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            new AlertDialog.Builder(MainActivity.this).setTitle(getResources().getString(R.string.error_connecting_title))
                                    .setMessage(getResources().getString(R.string.error_connecting_description, e.getMessage()))
                                    .setPositiveButton(android.R.string.ok, null).show();
                        }
                    });
                }
            }
        }).start();
    }

    private class CustomDeviceAdapter extends ArrayAdapter<String> {

        private Context context;

        CustomDeviceAdapter(@NonNull Context context, ArrayList<String> devices) {
            super(context, -1, devices);
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            String device = dmaDevices.get(position);

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 30, 40, 30);

            TextView nameText = new TextView(context);
            nameText.setText(device);
            nameText.setTypeface(nameText.getTypeface(), Typeface.BOLD);
            nameText.setTextSize(18);
            layout.addView(nameText);

            return layout;
        }
    }

    public static List<RemoteXBeeDevice> listnodes(DigiMeshDevice myDevice) {

        List<RemoteXBeeDevice> devices = null;

        if (myDevice.isOpen() == true) {
            myDevice.close();
        }

        try {
            myDevice.open();

            DigiMeshNetwork network = (DigiMeshNetwork) myDevice.getNetwork();
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
            while (i<devices.size()) {
                dmaDevices.add(devices.get(i).get64BitAddress().toString());
                i=i+1;
            }
        } catch (XBeeException e) {
            e.printStackTrace();
            myDevice.close();
        } finally {
            myDevice.close();
        }
        return devices;
    }

}
