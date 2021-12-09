package com.beechat.network;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
//import android.support.annotation.Nullable;
//import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/***
 *  --- WelcomeScreen ----
 *  The class that is responsible for the EULA application window.
 ***/
public class WelcomeScreen extends AppCompatActivity {
    Context context;
    Resources resources;
    EditText passwordOne, passwordTwo;
    Button finishButton;
    CheckBox agreementCheckBox;
    TextView eulaTextView, idTextView;
    Spinner languagesList;
    public static String language = "en";
    String[] languages = {"en", "es"};
    String largeTextString = null;
    DatabaseHandler DB;
    private Cipher cipher, decipher;
    private SecretKeySpec secretKeySpec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_screen);

        DB = new DatabaseHandler(this);
        eulaTextView = (TextView) findViewById(R.id.textViewEULA);
        languagesList = findViewById(R.id.languageSpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languagesList.setAdapter(adapter);

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
                    eulaTextView.setText("EULA is empty!");
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        languagesList.setOnItemSelectedListener(itemSelectedListener);
        language = languagesList.getSelectedItem().toString();

        context = LocaleHelper.setLocale(WelcomeScreen.this, language);
        resources = context.getResources();

        if (language.equals("en")) {
            largeTextString = getStringFromRawRes(R.raw.eula_en);
        } else largeTextString = getStringFromRawRes(R.raw.eula_es);

        if (largeTextString != null) {
            eulaTextView.setText(largeTextString);
        } else {
            eulaTextView.setText("EULA is empty!");
        }

        eulaTextView.setMovementMethod(new ScrollingMovementMethod());
        eulaTextView.setVerticalFadingEdgeEnabled(true);
        eulaTextView.setVerticalScrollBarEnabled(true);

        idTextView = (TextView) findViewById(R.id.textViewMyID);
        finishButton = (Button) findViewById(R.id.finishButton);

        passwordOne = (EditText) findViewById(R.id.passwordOne);
        passwordTwo = (EditText) findViewById(R.id.passwordTwo);

        Bundle extras = getIntent().getExtras();

        try {
            cipher = Cipher.getInstance("AES");
            decipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        byte[] bytes = extras.getString("key_user_id").getBytes();
        secretKeySpec = new SecretKeySpec(bytes, "AES");

        finishButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String user = extras.getString("key_user_id");
                String pass = passwordOne.getText().toString();
                String passConfirm = passwordTwo.getText().toString();
                if (user.equals("") || pass.equals(""))
                    Toast.makeText(WelcomeScreen.this, "Please enter all the fields", Toast.LENGTH_SHORT).show();
                else {
                    if (pass.equals(passConfirm)) {
                        Boolean checkuser = DB.checkUsername(user);
                        if (!checkuser) {
                            Boolean insert = null;
                            insert = DB.addUser(user, AESEncryptionMethod(pass));
                            if (insert) {
                                Toast.makeText(WelcomeScreen.this, "Registered successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(WelcomeScreen.this, SplashScreen.class);
                                intent.putExtra("key_user_id", user);
                                startActivity(intent);

                            } else {
                                Toast.makeText(WelcomeScreen.this, "Registration failed", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(WelcomeScreen.this, "User already exists! Please Sign in", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(WelcomeScreen.this, "Passwords not matching", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        agreementCheckBox = (CheckBox) findViewById(R.id.agreementCheckBox);
        agreementCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                finishButton.setEnabled(isChecked);
            }
        });

        idTextView.setText("My ID \n" + extras.getString("key_user_id"));
    }

    private String AESEncryptionMethod(String string){

        byte[] stringByte = string.getBytes();
        byte[] encryptedByte = new byte[stringByte.length];

        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            encryptedByte = cipher.doFinal(stringByte);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        String returnString = null;

        try {
            returnString = new String(encryptedByte, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return returnString;
    }

    private String AESDecryptionMethod(String string) throws UnsupportedEncodingException {
        byte[] EncryptedByte = string.getBytes("ISO-8859-1");
        String decryptedString = string;

        byte[] decryption;

        try {
            decipher.init(cipher.DECRYPT_MODE, secretKeySpec);
            decryption = decipher.doFinal(EncryptedByte);
            decryptedString = new String(decryption);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return decryptedString;
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
