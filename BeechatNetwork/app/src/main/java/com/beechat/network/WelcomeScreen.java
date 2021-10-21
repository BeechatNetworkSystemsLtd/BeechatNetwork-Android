package com.beechat.network;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

/***
 *  --- WelcomeScreen ----
 *  The class that is responsible for the start application window.
 ***/
public class WelcomeScreen extends AppCompatActivity {

    Button finishButton;
    CheckBox agreementCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_screen);

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
    }
}
