package com.beechat.network;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.digi.xbee.api.android.DigiMeshDevice;
import com.digi.xbee.api.android.XBeeDevice;
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
    Context context;
    Resources resources;

    // Constants.
    private static final int BAUD_RATE = 57600;

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

    private int baudRate;

    private AndroidUSBPermissionListener permissionListener;

    private Logger logger;

    private static XBeeDevice myDevice;
    public static DatabaseHandler db = null;
    public static List<String> xbee_devices = new ArrayList<>();
    public static String idDevice = "";
    public static String splashScreenUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        Bundle extras = getIntent().getExtras();
        context = LocaleHelper.setLocale(SplashScreen.this, WelcomeScreen.language);
        resources = context.getResources();

        db = new DatabaseHandler(this);

        final ProgressDialog dialog = ProgressDialog.show(this, resources.getString(R.string.startup_device_title),
                resources.getString(R.string.startup_device), true);

        myDevice = new XBeeDevice(this, BAUD_RATE, permissionListener);
        splashScreenUserId = extras.getString("key_user_id");


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    myDevice.open();
                    try {
                        myDevice.setNodeID(splashScreenUserId);
                    } catch (XBeeException e) {
                        e.printStackTrace();
                    }
                    SplashScreen.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            if (myDevice != null) {
                                idDevice = myDevice.get64BitAddress().toString();
                                Toast.makeText(SplashScreen.this, "64BitAddress: " + idDevice, Toast.LENGTH_SHORT).show();
                            }

                            Intent intent = new Intent(SplashScreen.this, MainScreen.class);
                            intent.putExtra("key_xbee_id", idDevice);
                            intent.putExtra("key_user_id", splashScreenUserId);
                            startActivity(intent);
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


