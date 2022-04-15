package com.beechat.network;

import android.Manifest;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;


import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.text.method.PasswordTransformationMethod;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.BufferedInputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import java.io.File;
import java.io.FileInputStream;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

/***
 *  --- WelcomeScreen ---
 *  The class that is responsible for the new user registration.
 ***/
public class WelcomeScreen extends AppCompatActivity {

    // Variables
    Context context;
    Resources resources;
    EditText password, logoName;
    ImageButton hidePassButton;
    ImageButton regenerateButton;
    Button finishButton, importButton;
    CheckBox agreementCheckBox;
    TextView eulaTextView, userIdTextView;
    String largeTextString;
    byte[] generatedUserId;
    byte[] generatedUserDPk;
    byte[] generatedUserDSk;
    byte[] generatedUserKPk;
    byte[] generatedUserKSk;
    DatabaseHandler DB;
    private SecretKeySpec secretKeySpec;
    int FILE_SELECT_CODE = 101;
    String path = null;
    boolean passHide = false;

    // Constants
    String algorithm = "AES";
    String charsetNameISO = "ISO-8859-1";
    String charsetNameUTF = "UTF-8";
    String[] languages = {"en", "es", "de", "fr", "ru"};
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
        importButton = findViewById(R.id.buttonImport);
        regenerateButton = findViewById(R.id.regenerateKey);
        hidePassButton = findViewById(R.id.hidePassButton);
        logoName = findViewById(R.id.logoName);
        password = findViewById(R.id.passwordOne);

        context = LocaleHelper.setLocale(WelcomeScreen.this, "en");
        resources = context.getResources();

        if (true) {
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

        userIdTextView.setText(Blake3.toString(generatedUserId));

        finishButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createAccount();
            }
        });

        askPermissions();

        // Handling the event of attaching files.
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
                } catch (Exception ex) {
                    System.out.println("browseClick :" + ex);
                }
            }
        });

        hidePassButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (passHide) {
                    password.setTransformationMethod(new PasswordTransformationMethod());
                } else {
                    password.setTransformationMethod(null);
                }
                passHide = !passHide;
            }
        });

        regenerateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                userIdTextView.setText(Blake3.toString(generatedUserId));
            }
        });


        agreementCheckBox = findViewById(R.id.agreementCheckBox);
        agreementCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> finishButton.setEnabled(isChecked));
    }

    /***
     *  --- askPermissions() ---
     *  The function of requesting permission to access the internal storage of the phone.
     ***/
    protected void askPermissions() {
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
        int permission = ActivityCompat.checkSelfPermission(Objects.requireNonNull(this), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, permissions, 1);
        }
    }


    /***
     *  --- createAccount() ---
     *  The function of creating a new chat account in the database.
     ***/
    private void createAccount() {
        String logo = logoName.getText().toString();
        String pass = password.getText().toString();
        byte[] passHash = null;

        if (pass.equals("")) {
            Toast.makeText(
                WelcomeScreen.this
              , "Please enter all the fields!"
              , Toast.LENGTH_SHORT
            ).show();
            return;
        }
        if (DB.checkUsername(Blake3.toString(generatedUserId))) {
            Toast.makeText(
                WelcomeScreen.this
              , "User already exists! Please Sign in!"
              , Toast.LENGTH_SHORT
            ).show();
            return;
        }

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

        if (DB.addUser(neo) == false) {
            Toast.makeText(
                WelcomeScreen.this
              , "Registration failed!"
              , Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Toast.makeText(
            WelcomeScreen.this
          , "Registered successfully!"
          , Toast.LENGTH_SHORT
        ).show();
        Intent intent = new Intent(WelcomeScreen.this, SplashScreen.class);
        intent.putExtra("key_usernameId", Blake3.toString(generatedUserId));
        startActivity(intent);
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    Uri uri = data.getData();
                    path = ChatScreen.getPath(this, uri);
                    Intent intent = new Intent(this, CodeWordScreen.class);
                    startActivityForResult(intent, 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                String cw = data.getStringExtra("cw");
                byte[] neo_raw = readRecovery(new File(path), cw);
                int seek = 0;
                if (neo_raw == null) {
                    Toast.makeText(this, "The code word is not accepted!", Toast.LENGTH_LONG).show();
                    return;
                }

                byte[] userId = new byte[User.NODEID_SIZE * 2];
                System.arraycopy(neo_raw, seek, userId, 0, User.NODEID_SIZE * 2);
                seek += User.NODEID_SIZE * 2;

                byte[] passh = new byte[User.PASS_HASH_SIZE * 2];
                System.arraycopy(neo_raw, seek, passh, 0, User.PASS_HASH_SIZE * 2);
                seek += User.PASS_HASH_SIZE * 2;

                byte[] dpk = new byte[Dilithium.CRYPTO_PUBLICKEYBYTES];
                System.arraycopy(neo_raw, seek, dpk, 0, Dilithium.CRYPTO_PUBLICKEYBYTES);
                seek += Dilithium.CRYPTO_PUBLICKEYBYTES;

                byte[] dsk = new byte[Dilithium.CRYPTO_SECRETKEYBYTES];
                System.arraycopy(neo_raw, seek, dsk, 0, Dilithium.CRYPTO_SECRETKEYBYTES);
                seek += Dilithium.CRYPTO_SECRETKEYBYTES;

                byte[] kpk = new byte[Kyber512.KYBER_PUBLICKEYBYTES];
                System.arraycopy(neo_raw, seek, kpk, 0, Kyber512.KYBER_PUBLICKEYBYTES);
                seek += Kyber512.KYBER_PUBLICKEYBYTES;

                byte[] ksk = new byte[Kyber512.KYBER_SECRETKEYBYTES];
                System.arraycopy(neo_raw, seek, ksk, 0, Kyber512.KYBER_SECRETKEYBYTES);
                seek += Kyber512.KYBER_SECRETKEYBYTES;

                byte[] logo = new byte[neo_raw.length - seek];
                System.arraycopy(neo_raw, seek, logo, 0, neo_raw.length - seek);

                User neo = new User(
                    new String(userId)
                  , new String(passh)
                  , dpk
                  , dsk
                  , kpk
                  , ksk
                  , new String(logo)
                );
                DB.addUser(neo);
                Toast.makeText(this, "The user is imported!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getApplicationContext(), LogInScreen.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "The code word is not accepted!", Toast.LENGTH_LONG).show();
            }
        }
    }

	public static byte[] readRecovery(File bFile, String cw){
		FileInputStream fin;
		try {
			fin = new FileInputStream(bFile);
			BufferedInputStream in = new BufferedInputStream(fin);
			try {
				byte[] data = new byte[in.available()];
                SecretKey skey = MainScreen.getAESKeyFromPassword(cw, "AES");
				in.read(data);
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, skey);
                byte [] output = cipher.doFinal(data);
				return output;
			} catch (Exception e) {
				in.close();
            } finally {
				in.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
        return null;
	}
}
