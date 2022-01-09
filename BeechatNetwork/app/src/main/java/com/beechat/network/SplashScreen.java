package com.beechat.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.digi.xbee.api.android.XBeeDevice;
import com.digi.xbee.api.exceptions.XBeeException;

/***
 *  --- SplashScreen ---
 *  The class that is responsible for the initialisation of XBEE device.
 ***/
public class SplashScreen extends AppCompatActivity {
    Context context;
    Resources resources;

    // Constants.
    public static int BAUD_RATE = 57600;

    // Variables.
    DatabaseHandler db;
    public static String addressMyXbeeDevice;
    public static XBeeDevice myXbeeDevice;
    public static byte[] myGeneratedUserId;
    private User neo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        context = LocaleHelper.setLocale(SplashScreen.this, WelcomeScreen.language);
        resources = context.getResources();

        Bundle extras = getIntent().getExtras();
        db = new DatabaseHandler(this);

        final ProgressDialog dialog = ProgressDialog.show(this, resources.getString(R.string.startup_device_title),
                resources.getString(R.string.startup_device), true);

        neo = db.getUser(extras.getString("key_usernameId"));
        myGeneratedUserId = Blake3.fromString(extras.getString("key_usernameId"));
        myXbeeDevice = new XBeeDevice(this, BAUD_RATE);

        new Thread(() -> {
            try {
                myXbeeDevice.open();
                try {
                    myXbeeDevice.setNodeID(Base58.encode(myGeneratedUserId));
                } catch (XBeeException e) {
                    e.printStackTrace();
                }
                SplashScreen.this.runOnUiThread(() -> {
                    dialog.dismiss();
                    if (myXbeeDevice != null) {
                        addressMyXbeeDevice = myXbeeDevice.get64BitAddress().toString();
                        Toast.makeText(SplashScreen.this, "User:" + Blake3.toString(myGeneratedUserId) + ",XBEE:" + addressMyXbeeDevice, Toast.LENGTH_SHORT).show();
                    }

                    Intent intent = new Intent(SplashScreen.this, MainScreen.class);
                    startActivity(intent);
                });

            } catch (XBeeException e) {
                e.printStackTrace();
                myXbeeDevice.close();
            } finally {
                myXbeeDevice.close();
            }
        }).start();
    }
}


