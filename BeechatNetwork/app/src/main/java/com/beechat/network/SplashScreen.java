package com.beechat.network;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.digi.xbee.api.android.DigiMeshDevice;
import com.digi.xbee.api.android.connection.usb.AndroidUSBInputStream;
import com.digi.xbee.api.android.connection.usb.AndroidUSBOutputStream;
import com.digi.xbee.api.android.connection.usb.AndroidUSBPermissionListener;
import com.digi.xbee.api.exceptions.InvalidInterfaceException;
import com.digi.xbee.api.exceptions.PermissionDeniedException;
import com.digi.xbee.api.exceptions.XBeeException;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/***
 *  --- SplashScreen ----
 *  The class that is responsible for the start application window.
 ***/
public class SplashScreen extends AppCompatActivity {

    // Constants.
    private static final int BAUD_RATE = 57600;

    // Variables.
    // Variables.
    private UsbDevice usbDevice;

    private UsbDeviceConnection usbConnection;

    private UsbInterface usbInterface;

    private UsbEndpoint receiveEndPoint;
    private UsbEndpoint sendEndPoint;

    private PendingIntent mPermissionIntent;

    private UsbManager usbManager;

    private AndroidUSBInputStream inputStream;

    private AndroidUSBOutputStream outputStream;

    private boolean isConnected = false;
    private boolean permissionsReceived = false;
    private boolean permissionsGranted = false;

    private Context context;

    private int baudRate;

    private AndroidUSBPermissionListener permissionListener;

    private Logger logger;

    //private AndroidUSBPermissionListener permissionListener;
    private static DigiMeshDevice myDevice;
    public static DatabaseHandler db = null;
    public static List<String> xbee_devices = new ArrayList<>();
    public static String idDevice = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        db = new DatabaseHandler(this);

        final ProgressDialog dialog = ProgressDialog.show(this, getResources().getString(R.string.startup_device_title),
                getResources().getString(R.string.startup_device), true);

        myDevice = new DigiMeshDevice(this, BAUD_RATE, permissionListener);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    myDevice.open();
                    SplashScreen.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            if (myDevice != null) {
                                idDevice = myDevice.get64BitAddress().toString();
                            }
                            // Reading all devices
                            System.out.println("Reading: " + "Reading all devices..");
                            List<Device> devices = db.getAllDevices();

                            for (Device cn : devices) {
                                xbee_devices.add(cn.getXbeeDeviceNumber());
                            }

                            if (xbee_devices.isEmpty()) {
                                System.out.println("Device " + myDevice.get64BitAddress().toString()+ " not exist!");
                                db.addDevice(new Device(myDevice.get64BitAddress().toString()));
                                Intent intent = new Intent(SplashScreen.this, WelcomeScreen.class);
                                intent.putExtra("key_idDevice", idDevice);
                                startActivity(intent);
                            } else {
                                if (xbee_devices.contains(myDevice.get64BitAddress().toString())) {
                                    System.out.println("Device " + myDevice.get64BitAddress().toString() + " exist!");
                                    Intent intent = new Intent(SplashScreen.this, MainScreen.class);
                                    startActivity(intent);
                                } else {
                                    System.out.println("Device " + myDevice.get64BitAddress().toString() + " not exist!");
                                    db.addDevice(new Device(myDevice.get64BitAddress().toString()));
                                    Intent intent = new Intent(SplashScreen.this, WelcomeScreen.class);
                                    intent.putExtra("key_idDevice", idDevice);
                                    startActivity(intent);
                                }
                            }
                        }
                    });

                } catch (XBeeException e) {
                    e.printStackTrace();
                    myDevice.close();
                } finally {
                    myDevice.close();
                }
            }
        }).start();
    }
}


