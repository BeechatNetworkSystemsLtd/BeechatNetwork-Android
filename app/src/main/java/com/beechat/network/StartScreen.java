package com.beechat.network;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/***
 *  --- StartScreen ---
 *  The class that is responsible for the entry point to the application.
 ***/
public class StartScreen extends AppCompatActivity {

    // Variables
    static DatabaseHandler DB;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_screen);

        DB = new DatabaseHandler(this);

        new Thread(() -> {
            if (DB.checkCountUsers()) {
                Intent intent = new Intent(getApplicationContext(), LogInScreen.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(getApplicationContext(), WelcomeScreen.class);
                startActivity(intent);
            }
        }).start();
    }
}

