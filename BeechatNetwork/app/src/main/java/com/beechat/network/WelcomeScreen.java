package com.beechat.network;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.digi.xbee.api.android.DigiMeshDevice;
import com.digi.xbee.api.android.connection.usb.AndroidUSBPermissionListener;
import com.digi.xbee.api.exceptions.XBeeException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/***
 *  --- WelcomeScreen ----
 *  The class that is responsible for the start application window.
 ***/
public class WelcomeScreen extends AppCompatActivity {

    // Constants.
    private static final int BAUD_RATE = 57600;

    // Variables.
    private AndroidUSBPermissionListener permissionListener;
    private static DigiMeshDevice myDevice;
    public static DatabaseHandler db = null;
    public static List<String> xbee_devices = new ArrayList<>();

    Button finishButton;
    CheckBox agreementCheckBox;
    TextView eulaTextView, idTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_screen);

        db = new DatabaseHandler(this);
        eulaTextView = (TextView) findViewById(R.id.textViewEULA);
        String largeTextString = getStringFromRawRes(R.raw.eula);

        if(largeTextString != null) {
            //null check is optional
            eulaTextView.setText(largeTextString);
        } else {
            eulaTextView.setText("EULA is empty!");
        }

        eulaTextView.setMovementMethod(new ScrollingMovementMethod());

        idTextView = (TextView) findViewById(R.id.textViewMyID);

        finishButton = (Button)findViewById(R.id.finishButton);
        finishButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeScreen.this, MainScreen.class);
                startActivity(intent);
            }
        });

        agreementCheckBox = (CheckBox) findViewById(R.id.agreementCheckBox);
        agreementCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    finishButton.setEnabled(true);
                } else {
                    finishButton.setEnabled(false);
                }
            }
        });

        final ProgressDialog dialog = ProgressDialog.show(this, getResources().getString(R.string.startup_device_title),
                getResources().getString(R.string.startup_device), true);
        myDevice = new DigiMeshDevice(this, BAUD_RATE, permissionListener);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    myDevice.open();
                    WelcomeScreen.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            if (myDevice != null) {
                                idTextView.setText("My ID \n" + myDevice.get64BitAddress().toString());
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
                            } else {
                                if (xbee_devices.contains(myDevice.get64BitAddress().toString())) {
                                    System.out.println("Device " + myDevice.get64BitAddress().toString() + " exist!");
                                    eulaTextView.setVisibility(View.GONE);
                                    agreementCheckBox.setVisibility(View.GONE);
                                    finishButton.setVisibility(View.GONE);
                                    Intent intent = new Intent(WelcomeScreen.this, MainScreen.class);
                                    startActivity(intent);
                                } else {
                                    System.out.println("Device " + myDevice.get64BitAddress().toString() + " not exist!");
                                    db.addDevice(new Device(myDevice.get64BitAddress().toString()));
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

    /***
     *  --- getStringFromRawRes(String) ----
     *  The function of starting scanning for available Xbee devices.
     *
     *  @param rawRes The text of End User License Agreement.
     ***/
    @Nullable
    private String getStringFromRawRes(int rawRes) {
        InputStream inputStream;

        try {
            inputStream = getResources().openRawResource(rawRes);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            return null;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        try {
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                inputStream.close();
                byteArrayOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String resultString;
        try {
            resultString = byteArrayOutputStream.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        return resultString;
    }
}
