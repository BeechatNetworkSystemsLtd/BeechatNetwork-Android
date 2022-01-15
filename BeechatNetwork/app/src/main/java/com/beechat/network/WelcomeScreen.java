package com.beechat.network;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/***
 *  --- WelcomeScreen ---
 *  The class that is responsible for the new user registration.
 ***/
public class WelcomeScreen extends AppCompatActivity {

    // Variables
    Context context;
    Resources resources;
    EditText password, passwordConfirm, logoName;
    Button finishButton;
    CheckBox agreementCheckBox;
    TextView eulaTextView, userIdTextView;
    Spinner languagesSpinner;
    String largeTextString;
    byte[] generatedUserId;
    byte[] generatedUserDPk;
    byte[] generatedUserDSk;
    byte[] generatedUserKPk;
    byte[] generatedUserKSk;
    DatabaseHandler DB;
    private SecretKeySpec secretKeySpec;

    // Constants
    String algorithm = "AES";
    String charsetNameISO = "ISO-8859-1";
    String charsetNameUTF = "UTF-8";
    String[] languages = {"en", "es"};
    public static String language = "en";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_screen);

        DB = new DatabaseHandler(this);

        eulaTextView = findViewById(R.id.textViewEULA);
        eulaTextView.setMovementMethod(new ScrollingMovementMethod());
        eulaTextView.setVerticalFadingEdgeEnabled(true);
        eulaTextView.setVerticalScrollBarEnabled(true);

        userIdTextView = findViewById(R.id.textViewMyID);
        finishButton = findViewById(R.id.finishButton);
        logoName = findViewById(R.id.logoName);
        password = findViewById(R.id.passwordOne);
        passwordConfirm = findViewById(R.id.passwordTwo);
        languagesSpinner = findViewById(R.id.languageSpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        languagesSpinner.setAdapter(adapter);

        AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                language = (String) parent.getItemAtPosition(position);
                if (language.equals("en")) {
                    largeTextString = getStringFromRawRes(R.raw.eula_en);
                } else largeTextString = getStringFromRawRes(R.raw.eula_es);

                if (largeTextString != null) {
                    eulaTextView.setText(largeTextString);
                } else {
                    eulaTextView.setText(R.string.eulaEmpty);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        languagesSpinner.setOnItemSelectedListener(itemSelectedListener);

        language = languagesSpinner.getSelectedItem().toString();
        context = LocaleHelper.setLocale(WelcomeScreen.this, language);
        resources = context.getResources();

        if (language.equals("en")) {
            largeTextString = getStringFromRawRes(R.raw.eula_en);
        } else largeTextString = getStringFromRawRes(R.raw.eula_es);

        if (largeTextString != null) {
            eulaTextView.setText(largeTextString);
        } else {
            eulaTextView.setText(R.string.eulaEmpty);
        }

        generatedUserKPk = new byte[Kyber512.KYBER_PUBLICKEYBYTES];
        generatedUserKSk = new byte[Kyber512.KYBER_SECRETKEYBYTES];
        generatedUserDPk = new byte[Dilithium.CRYPTO_PUBLICKEYBYTES];
        generatedUserDSk = new byte[Dilithium.CRYPTO_SECRETKEYBYTES];

        try {
            Kyber512.crypto_kem_keypair(generatedUserKPk, generatedUserKSk);
            Dilithium.crypto_sign_keypair(generatedUserDPk, generatedUserDSk);
            Blake3 hasher = new Blake3();
            hasher.update(generatedUserDPk, Dilithium.CRYPTO_PUBLICKEYBYTES);
            generatedUserId = hasher.finalize(User.NODEID_SIZE);
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }

        userIdTextView.setText("Your Beechat address\n" + Blake3.toString(generatedUserId));

        finishButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createAccount();
            }
        });

        agreementCheckBox = findViewById(R.id.agreementCheckBox);
        agreementCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> finishButton.setEnabled(isChecked));
    }

    /***
     *  --- createAccount() ---
     *  The function of creating a new chat account in the database.
     ***/
    private void createAccount() {
        String logo = logoName.getText().toString();
        String pass = password.getText().toString();
        String passConfirm = passwordConfirm.getText().toString();

        if (passConfirm.equals("") || pass.equals(""))
            Toast.makeText(WelcomeScreen.this, "Please enter all the fields!", Toast.LENGTH_SHORT).show();
        else {
            if (pass.equals(passConfirm)) {
                Boolean checkUser = DB.checkUsername(Blake3.toString(generatedUserId));
                if (!checkUser) {
                    byte[] passHash = null;
                    try {
                        Blake3 hasher = new Blake3();
                        hasher.update(pass.getBytes(), pass.length());
                        passHash = hasher.finalize(User.PASS_HASH_SIZE);
                    } catch (Exception ex) {
                        System.out.println("Exception: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                    User neo = new User(
                        Blake3.toString(generatedUserId)
                      , Blake3.toString(passHash)
                      , generatedUserDPk
                      , generatedUserDSk
                      , generatedUserKPk
                      , generatedUserKSk
                      , logo
                    );
                    Boolean insert = DB.addUser(neo);
                    if (insert) {
                        Toast.makeText(WelcomeScreen.this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(WelcomeScreen.this, SplashScreen.class);
                        intent.putExtra("key_usernameId", Blake3.toString(generatedUserId));
                        startActivity(intent);

                    } else {
                        Toast.makeText(WelcomeScreen.this, "Registration failed!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(WelcomeScreen.this, "User already exists! Please Sign in!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(WelcomeScreen.this, "Passwords not matching", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /***
     *  --- getStringFromRawRes(int) ---
     *  The function to extract a string from an raw of converted file.
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
            resultString = byteArrayOutputStream.toString(charsetNameUTF);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        return resultString;
    }
}
