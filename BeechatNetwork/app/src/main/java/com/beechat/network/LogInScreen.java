package com.beechat.network;

import android.content.Intent;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Random;

public class LogInScreen extends AppCompatActivity {

    EditText password;
    Button buttonLogin, buttonCreateAccount, buttonImportAccount;
    DatabaseHandler DB;
    String username_id;
    Spinner listUsernames;
    ArrayList<String> usernames = new ArrayList<>();
    String idSaltCreate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_in_screen);

        idSaltCreate = getSaltString();

        DB = new DatabaseHandler(this);

        usernames = (ArrayList<String>) DB.getNames();
        password = (EditText) findViewById(R.id.editTextPassword);

        buttonLogin = (Button) findViewById(R.id.buttonLogIn);
        buttonCreateAccount = findViewById(R.id.buttonCreateAccount);
        buttonImportAccount = findViewById(R.id.buttonImportAccount);

        listUsernames = findViewById(R.id.accountSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, usernames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        listUsernames.setAdapter(adapter);

        AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                username_id = (String)parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        listUsernames.setOnItemSelectedListener(itemSelectedListener);
        username_id = listUsernames.getSelectedItem().toString();

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String pass = password.getText().toString();

                if(pass.equals(""))
                    Toast.makeText(LogInScreen.this, "Please enter password", Toast.LENGTH_SHORT).show();
                else{
                    Boolean checkuserpass = DB.checkUsernamePassword(username_id, pass);
                    if(checkuserpass){
                        Toast.makeText(LogInScreen.this, "Sign in successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LogInScreen.this, SplashScreen.class);
                        intent.putExtra("key_user_id", username_id);
                        startActivity(intent);
                    }else{
                        Toast.makeText(LogInScreen.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        buttonCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getApplicationContext(), WelcomeScreen.class);
                intent.putExtra("key_user_id", idSaltCreate);
                startActivity(intent);
            }
        });

        buttonImportAccount.setOnClickListener(view -> {
        });
    }

    protected String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 12) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return salt.toString();
    }
}
