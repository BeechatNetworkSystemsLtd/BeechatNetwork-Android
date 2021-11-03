package com.beechat.network;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

public class DataScreen extends AppCompatActivity {

    Button backButton, wipeDataButton, yes, no;
    PopupWindow popUp;
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_screen);

        backButton = (Button)findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        popUp = new PopupWindow(this);
        final LinearLayout layout = new LinearLayout(this);
        tv = new TextView(this);
        yes = new Button(this);
        yes.setText("Yes");
        no = new Button(this);
        no.setText("No");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        tv.setText("Are you sure?");
        layout.addView(tv, params);
        layout.addView(yes, params);
        layout.addView(no, params);
        popUp.setContentView(layout);

        yes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                WelcomeScreen.db.deleteDB();
                finishAffinity();
                System.exit(0);
                //popUp.dismiss();
            }
        });

        no.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popUp.dismiss();
            }
        });

        wipeDataButton = (Button)findViewById(R.id.wipeDataButton);
        wipeDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popUp.showAtLocation(layout, Gravity.BOTTOM, 10, 10);
            }
        });

    }
}
