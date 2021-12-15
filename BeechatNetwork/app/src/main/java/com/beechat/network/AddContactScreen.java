package com.beechat.network;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/***
 *  --- AddContactScreen ---
 *  The class is responsible for adding new contacts.
 ***/
public class AddContactScreen extends AppCompatActivity {

    // Variables.
    TextView addressTextView;
    EditText nameEditText;
    Button addContactButton;
    ImageButton backButton;
    Context context;
    Resources resources;
    DatabaseHandler db;

    String selectedXbeeDevice, selectedUserId, name = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_contact_screen);

        context = LocaleHelper.setLocale(getApplicationContext(), WelcomeScreen.language);
        resources = context.getResources();

        Bundle extras = getIntent().getExtras();

        db = new DatabaseHandler(this);

        addressTextView = findViewById(R.id.addressTextView);
        nameEditText = findViewById(R.id.nameEditText);
        backButton = findViewById(R.id.backButton);
        addContactButton = findViewById(R.id.addContactButton);

        if (extras != null) {
            selectedXbeeDevice = extras.getString("key_selectedXbeeDevice");
            selectedUserId = extras.getString("key_selectedUserId");
            addressTextView.setText("Address " + selectedXbeeDevice + "(" + selectedUserId + ")");
        }

        backButton.setOnClickListener(v -> finish());

        addContactButton.setOnClickListener(v -> {
            name = nameEditText.getText().toString();
            db.addContact(new Contact(selectedUserId, selectedXbeeDevice, name));
            ContactsScreen.contactNames.add(name);
            ContactsScreen.contactUserIds.add(selectedUserId);
            ContactsScreen.contactXbeeAddress.add(selectedXbeeDevice);
            ContactsScreen.onRefresh();
            finish();
        });


    }
}
