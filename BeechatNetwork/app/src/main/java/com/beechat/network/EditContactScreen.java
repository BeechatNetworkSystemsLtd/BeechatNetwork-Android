package com.beechat.network;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/***
 *  --- EditContactScreen ----
 *  The class is responsible for the adding and editing contacts.
 ***/
public class EditContactScreen extends AppCompatActivity {
    Context context;
    Resources resources;
    private static String selectedDevice = null;
    private static String selectedUserId = null;

    TextView addressTextView;
    EditText nameEditText;
    Button updateContactButton;
    ImageButton backButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_contact_screen);

        context = LocaleHelper.setLocale(EditContactScreen.this, WelcomeScreen.language);
        resources = context.getResources();

        addressTextView = (TextView)findViewById(R.id.addressTextView);
        nameEditText = (EditText) findViewById(R.id.nameEditText);

        Bundle extras = getIntent().getExtras();

        if(extras !=null) {
            selectedDevice = extras.getString("key_device");
            selectedUserId = extras.getString("key_user_id");
            addressTextView.setText("Address " + selectedDevice+":"+selectedUserId);
        }

        backButton = (ImageButton)findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        updateContactButton = (Button)findViewById(R.id.updateContactButton);
        updateContactButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SplashScreen.db.updateContact(new Contact(selectedUserId, selectedDevice, nameEditText.getText().toString()));
                ChatScreen.nameTextView.setText(nameEditText.getText().toString());
                ContactsScreen.onRefresh();
                finish();
            }
        });


    }
}
