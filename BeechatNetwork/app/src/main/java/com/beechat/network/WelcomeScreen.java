package com.beechat.network;

import android.content.Context;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/***
 *  --- WelcomeScreen ----
 *  The class that is responsible for the EULA application window.
 ***/
public class WelcomeScreen extends AppCompatActivity {
    Context context;
    Resources resources;
    Button finishButton;
    CheckBox agreementCheckBox;
    TextView eulaTextView, idTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_screen);
        context = LocaleHelper.setLocale(WelcomeScreen.this, SelectLanguageScreen.language);
        resources = context.getResources();
        eulaTextView = (TextView) findViewById(R.id.textViewEULA);
        String largeTextString = null;
        if (SelectLanguageScreen.language == "en") {
            largeTextString=getStringFromRawRes(R.raw.eula_en);
        } else largeTextString=getStringFromRawRes(R.raw.eula_es);

        if(largeTextString != null) {
            eulaTextView.setText(largeTextString);
        } else {
            eulaTextView.setText("EULA is empty!");
        }

        eulaTextView.setMovementMethod(new ScrollingMovementMethod());
        //eulaTextView.setHorizontallyScrolling(true);
        eulaTextView.setVerticalFadingEdgeEnabled(true);
        eulaTextView.setVerticalScrollBarEnabled(true);

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

        Bundle extras = getIntent().getExtras();
        idTextView.setText("My ID \n" + extras.getString("key_idDevice"));
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
