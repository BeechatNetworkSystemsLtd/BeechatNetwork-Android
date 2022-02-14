package com.beechat.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;

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
    //public static int BAUD_RATE = 115200;
    public static int BAUD_RATE = 9600;

    // Variables.
    DatabaseHandler db;
    public static String addressMyXbeeDevice;
    public static XBeeDevice myXbeeDevice;
    public static byte[] myGeneratedUserId;
    public static byte[] myDPKey;
    public static byte[] myDSKey;
    public static byte[] myKPKey;
    public static byte[] myKSKey;
    public static byte[] randomHash;
    public static User neo;
    public static Blake3 hasher;
    int dotCounter = 0;
    boolean isEnter = false;
    TextView skipView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        context = LocaleHelper.setLocale(SplashScreen.this, WelcomeScreen.language);
        resources = context.getResources();

        try {
            hasher = new Blake3();
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }

        Bundle extras = getIntent().getExtras();
        db = new DatabaseHandler(this);
        Button skipButton = findViewById(R.id.buttonSkip);
        Button reqButton = findViewById(R.id.buttonSkip2);
        TextView skipView = findViewById(R.id.textView2);
        //skipView.setText("Connecting to device " + Integer.toString(BAUD_RATE) + " bps");

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(() -> {
                    isEnter = true;
                    Intent intent = new Intent(SplashScreen.this, MainScreen.class);
                    startActivity(intent);
                }).start();
            }
        });

        reqButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(() -> {
                    try {
                        myXbeeDevice.open();
                        myXbeeDevice.setNodeID(Base58.encode(myGeneratedUserId));

                        isEnter = true;
                        SplashScreen.this.runOnUiThread(() -> {
                            //dialog.dismiss();
                            if (myXbeeDevice != null) {
                                addressMyXbeeDevice = myXbeeDevice.get64BitAddress().toString();
                                Toast.makeText(SplashScreen.this, "User:" + Blake3.toString(myGeneratedUserId) + ", XBee:" + addressMyXbeeDevice, Toast.LENGTH_SHORT).show();
                            }

                            Intent intent = new Intent(SplashScreen.this, MainScreen.class);
                            startActivity(intent);
                        });
                    } catch (XBeeException e) {
                        e.printStackTrace();
                        myXbeeDevice.close();
                    }
                }).start();

                new Thread(() -> {
                    while (!isEnter) {
                        if (dotCounter == 5) dotCounter = 0;
                        SplashScreen.this.runOnUiThread(() -> {
                            skipView.setText("Connecting to device " + Integer.toString(BAUD_RATE) + " bps ");
                        });
                        for (int i = 0; i < dotCounter; i++) {
                            SplashScreen.this.runOnUiThread(() -> {
                                skipView.setText(skipView.getText() + ".");
                            });
                            try {
                                Thread.sleep(500);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        dotCounter++;
                    }
                }).start();
            }
        });

        neo = db.getUser(extras.getString("key_usernameId"));
        myGeneratedUserId = Blake3.fromString(extras.getString("key_usernameId"));
        myXbeeDevice = new XBeeDevice(this, BAUD_RATE);
        myDPKey = neo.getDPubKey();
        myDSKey = neo.getDPrivKey();
        myKPKey = neo.getKPubKey();
        myKSKey = neo.getKPrivKey();
    }
}


