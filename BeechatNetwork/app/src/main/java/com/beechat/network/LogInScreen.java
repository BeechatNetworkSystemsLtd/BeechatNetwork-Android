package com.beechat.network;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/***
 *  --- LogInScreen ---
 *  The class that is responsible for authenticating existing users.
 ***/
public class LogInScreen extends AppCompatActivity {

    // Variables
    EditText password;
    Button buttonLogin, buttonCreateAccount, buttonImportAccount;
    DatabaseHandler db;
    String usernameId;
    Spinner spinnerUsernames;
    ArrayList<String> listUsernames = new ArrayList<>();
    private SecretKeySpec secretKeySpec;

    // Constants
    String algorithm = "AES";
    String charsetNameISO = "ISO-8859-1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_in_screen);

        db = new DatabaseHandler(this);

        ArrayList<User> listUsers = (ArrayList<User>)db.getUsers();
        for (User u: listUsers) {
            if (u.getLogo().length() == 0) {
                listUsernames.add(u.getUsername());
                break;
            }
            listUsernames.add(u.getLogo());
        }
        password = findViewById(R.id.editTextPassword);

        buttonLogin = findViewById(R.id.buttonLogIn);
        buttonCreateAccount = findViewById(R.id.buttonCreateAccount);
        buttonImportAccount = findViewById(R.id.buttonImportAccount);

        spinnerUsernames = findViewById(R.id.accountSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, listUsernames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUsernames.setAdapter(adapter);

        AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                usernameId = listUsers.get(position).getUsername();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        spinnerUsernames.setOnItemSelectedListener(itemSelectedListener);
        User temp = listUsers.get(spinnerUsernames.getSelectedItemPosition());
        usernameId = temp.getUsername();
        buttonLogin.setOnClickListener(view -> logInChat());

        buttonCreateAccount.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), WelcomeScreen.class);
            startActivity(intent);
        });

        buttonImportAccount.setOnClickListener(view -> {
        });
    }

    /***
     *  --- logInChat() ---
     *  The function of connecting to chat with an existing account.
     ***/
    private void logInChat() {
        String pass = password.getText().toString();

        if (pass.equals(""))
            Toast.makeText(LogInScreen.this, "Please enter the password", Toast.LENGTH_SHORT).show();
        else {
            byte[] passHash = null;
            try {
                Blake3 hasher = new Blake3();
                hasher.update(pass.getBytes(), pass.length());
                passHash = hasher.finalize(User.PASS_HASH_SIZE);
            } catch (Exception ex) {
                System.out.println("Blake3 exception: " + ex.getMessage());
                ex.printStackTrace();
            }
            Boolean checkUserPass = db.checkUsernamePassword(usernameId, Blake3.toString(passHash));
            if (checkUserPass) {
                Toast.makeText(LogInScreen.this, "Sign in successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LogInScreen.this, SplashScreen.class);
                intent.putExtra("key_usernameId", usernameId);
                startActivity(intent);
            } else {
                Toast.makeText(LogInScreen.this, "Invalid Credentials!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
