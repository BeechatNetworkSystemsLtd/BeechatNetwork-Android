package com.beechat.network;

import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Environment;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import androidx.appcompat.app.AppCompatActivity;

/***
 *  --- DataScreen ---
 *  The class that is responsible for the removing all data from database.
 ***/
public class DataScreen extends AppCompatActivity {
    // Variables
    Context context;
    Resources resources;
    Button wipeDataButton, yesButton, noButton, okButton, exportButton;
    ImageButton backButton;
    PopupWindow popUp, popUpSuccess;
    TextView tv, tvSuccess;
    DatabaseHandler db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_screen);

        context = LocaleHelper.setLocale(DataScreen.this, WelcomeScreen.language);
        resources = context.getResources();

        db = new DatabaseHandler(this);

        backButton = findViewById(R.id.backButton);
        exportButton = findViewById(R.id.exportButton);
        backButton.setOnClickListener(v -> finish());

        popUp = new PopupWindow(this);
        final LinearLayout layout = new LinearLayout(this);
        tv = new TextView(this);
        yesButton = new Button(this);
        yesButton.setText(R.string.yesButtonLabel);
        noButton = new Button(this);
        noButton.setText(R.string.noButtonLabel);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        tv.setText(R.string.areYouSureLabel);
        layout.addView(tv, params);
        layout.addView(yesButton, params);
        layout.addView(noButton, params);
        popUp.setContentView(layout);

        popUpSuccess = new PopupWindow(this);
        final LinearLayout layoutSuccess = new LinearLayout(this);
        tvSuccess = new TextView(this);
        okButton = new Button(this);
        okButton.setText(R.string.okButtonLabel);
        LinearLayout.LayoutParams paramsSuccess = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        tvSuccess.setText(R.string.successLabel);
        layoutSuccess.addView(tvSuccess, paramsSuccess);
        layoutSuccess.addView(okButton, paramsSuccess);
        popUpSuccess.setContentView(layoutSuccess);

        yesButton.setOnClickListener(v -> {
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            popUp.dismiss();
            popUpSuccess.showAtLocation(layoutSuccess, Gravity.CENTER, 10, 10);
        });

        exportButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, CodeWordScreen.class);
            startActivityForResult(intent, 14);
        });

        noButton.setOnClickListener(v -> popUp.dismiss());

        okButton.setOnClickListener(v -> {
            db.deleteDB();
            finishAffinity();
            System.exit(0);
        });

        wipeDataButton = findViewById(R.id.wipeDataButton);
        wipeDataButton.setOnClickListener(v -> popUp.showAtLocation(layout, Gravity.CENTER, 10, 10));

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 14) {
            String cw = data.getStringExtra("cw");
            SecretKey skey = MainScreen.getAESKeyFromPassword(cw, "AES");
            try {
                String dir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ).getAbsolutePath() + File.separator + "Beechat";

                (new File(dir)).mkdirs();

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
    }
}
