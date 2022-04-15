package com.beechat.network;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/***
 *  --- EditContactScreen ---
 *  The class is responsible for the editing contacts.
 ***/
public class EditContactScreen extends AppCompatActivity {
    // Variables
    Context context;
    Resources resources;

    TextView beechatAddressTextView;
    TextView radioAddressTextView;
    EditText nameEditText;
    Button updateContactButton;
    ImageButton backButton;
    DatabaseHandler db;

    String selectedXbeeAddress, selectedUserId, name, ownerContact;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_contact_screen);

        context = LocaleHelper.setLocale(EditContactScreen.this, WelcomeScreen.language);
        resources = context.getResources();
        db = new DatabaseHandler(this);
        Bundle extras = getIntent().getExtras();

        radioAddressTextView = findViewById(R.id.ecRadioAddrTextView);
        beechatAddressTextView = findViewById(R.id.ecBeechatAddrTextView);
        nameEditText = findViewById(R.id.nameEditText);
        backButton = findViewById(R.id.backButton);
        updateContactButton = findViewById(R.id.updateContactButton);

        if (extras != null) {
            selectedXbeeAddress = extras.getString("key_selectedXbeeAddress");
            selectedUserId = extras.getString("key_selectedUserId");
            radioAddressTextView.setText(selectedXbeeAddress);
            beechatAddressTextView.setText(selectedUserId);
        }

        ownerContact = Blake3.toString(SplashScreen.myGeneratedUserId);

        backButton.setOnClickListener(v -> finish());
        updateContactButton.setOnClickListener(v -> {
            name = nameEditText.getText().toString();
            db.updateContact(new Contact(selectedUserId, selectedXbeeAddress, name, ownerContact));
            ChatScreen.nameTextView.setText(name);
            ContactsScreen.onRefresh();
            finish();
        });


    }
}
