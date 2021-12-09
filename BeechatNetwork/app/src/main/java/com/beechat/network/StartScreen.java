package com.beechat.network;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;


public class StartScreen extends AppCompatActivity {

    DatabaseHandler DB;
    String idSalt = null;
    private Context mContext;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_screen);

        DB = new DatabaseHandler(this);
        idSalt = getSaltString();

       if (DB.checkCountUsers()) {
           Intent intent = new Intent(getApplicationContext(), LogInScreen.class);
           startActivity(intent);
        } else {
           Intent intent = new Intent(getApplicationContext(), WelcomeScreen.class);
           intent.putExtra("key_user_id", idSalt);
           startActivity(intent);
        }
    }

    protected String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 12) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return salt.toString();
    }
}

