package com.beechat.network;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

public class DataScreen extends AppCompatActivity {
    Context context;
    Resources resources;
    Button wipeDataButton, yesButton, noButton, okButton;
    ImageButton backButton;
    PopupWindow popUp, popUpSuccess;
    TextView tv, tvSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = LocaleHelper.setLocale(DataScreen.this, SelectLanguageScreen.language);
        resources = context.getResources();
        setContentView(R.layout.data_screen);

        backButton = (ImageButton)findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        popUp = new PopupWindow(this);
        final LinearLayout layout = new LinearLayout(this);
        tv = new TextView(this);
        yesButton = new Button(this);
        yesButton.setText("Yes");
        noButton = new Button(this);
        noButton.setText("No");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        tv.setText("Are you sure?");
        layout.addView(tv, params);
        layout.addView(yesButton, params);
        layout.addView(noButton, params);
        popUp.setContentView(layout);

        popUpSuccess = new PopupWindow(this);
        final LinearLayout layoutSuccess = new LinearLayout(this);
        tvSuccess = new TextView(this);
        okButton = new Button(this);
        okButton.setText("Ok");
        LinearLayout.LayoutParams paramsSuccess = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        tvSuccess.setText("Success");
        layoutSuccess.addView(tvSuccess, paramsSuccess);
        layoutSuccess.addView(okButton, paramsSuccess);
        popUpSuccess.setContentView(layoutSuccess);

        yesButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                popUp.dismiss();
                popUpSuccess.showAtLocation(layoutSuccess, Gravity.CENTER, 10, 10);
            }
        });

        noButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popUp.dismiss();
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SplashScreen.db.deleteDB();
                finishAffinity();
                System.exit(0);
            }
        });

        wipeDataButton = (Button)findViewById(R.id.wipeDataButton);
        wipeDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popUp.showAtLocation(layout, Gravity.CENTER, 10, 10);
            }
        });

    }
}
