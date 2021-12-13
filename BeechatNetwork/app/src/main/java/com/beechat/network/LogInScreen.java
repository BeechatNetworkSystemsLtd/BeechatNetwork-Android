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
    DatabaseHandler DB;
    String usernameId;
    Spinner spinnerUsernames;
    ArrayList<String> listUsernames = new ArrayList<>();
    private Cipher cipher, decipher;
    private SecretKeySpec secretKeySpec;

    // Constants
    String algorithm = "AES";
    String charsetNameISO = "ISO-8859-1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_in_screen);

        DB = new DatabaseHandler(this);

        listUsernames = (ArrayList<String>) DB.getNames();
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
                usernameId = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        spinnerUsernames.setOnItemSelectedListener(itemSelectedListener);
        usernameId = spinnerUsernames.getSelectedItem().toString();

        try {
            cipher = Cipher.getInstance(algorithm);
            decipher = Cipher.getInstance(algorithm);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        byte[] bytes = usernameId.getBytes();
        secretKeySpec = new SecretKeySpec(bytes, algorithm);

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
            Toast.makeText(LogInScreen.this, "Please enter password!", Toast.LENGTH_SHORT).show();
        else {
            Boolean checkUserPass = DB.checkUsernamePassword(usernameId, AESEncryptionMethod(pass));
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

    /***
     *  --- AESEncryptionMethod(String) ---
     *  The password encryption function.
     *
     *  @param string The user password.
     ***/
    private String AESEncryptionMethod(String string) {

        byte[] stringByte = string.getBytes();
        byte[] encryptedByte = new byte[stringByte.length];

        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            encryptedByte = cipher.doFinal(stringByte);
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        String returnString = null;
        try {
            returnString = new String(encryptedByte, charsetNameISO);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return returnString;
    }

    /***
     *  --- AESDecryptionMethod(String) ---
     *  The password decryption function.
     *
     *  @param string The user password.
     ***/
    private String AESDecryptionMethod(String string) throws UnsupportedEncodingException {
        byte[] EncryptedByte = string.getBytes(charsetNameISO);
        String decryptedString = string;

        byte[] decryption;

        try {
            decipher.init(cipher.DECRYPT_MODE, secretKeySpec);
            decryption = decipher.doFinal(EncryptedByte);
            decryptedString = new String(decryption);
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return decryptedString;
    }
}
