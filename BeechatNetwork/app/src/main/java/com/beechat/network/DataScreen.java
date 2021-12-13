package com.beechat.network;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/***
 *  --- DataScreen ----
 *  The class that is responsible for the removing all data from database.
 ***/
public class DataScreen extends AppCompatActivity {
    // Variables
    Context context;
    Resources resources;
    Button wipeDataButton, yesButton, noButton, okButton;
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

        noButton.setOnClickListener(v -> popUp.dismiss());

        okButton.setOnClickListener(v -> {
            db.deleteDB();
            finishAffinity();
            System.exit(0);
        });

        wipeDataButton = findViewById(R.id.wipeDataButton);
        wipeDataButton.setOnClickListener(v -> popUp.showAtLocation(layout, Gravity.CENTER, 10, 10));

    }
}
