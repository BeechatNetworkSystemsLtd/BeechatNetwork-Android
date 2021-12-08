package com.beechat.network;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

/***
 *  --- AddContactScreen ----
 *  The class is responsible for the adding and editing contacts.
 ***/
public class AddContactScreen extends AppCompatActivity {

    private static String selectedDevice = null;
    private static String selectedUserId = null;

    TextView addressTextView;
    EditText nameEditText;
    Button addContactButton;
    ImageButton backButton;
    Context context;
    Resources resources;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_contact_screen);
        context = LocaleHelper.setLocale(getApplicationContext(), WelcomeScreen.language);
        resources = context.getResources();

        addressTextView = (TextView)findViewById(R.id.addressTextView);
        nameEditText = (EditText) findViewById(R.id.nameEditText);

        Bundle extras = getIntent().getExtras();
        if(extras !=null) {
            selectedDevice = extras.getString("key_xbee_id");
            selectedUserId = extras.getString("key_user_id");
            addressTextView.setText("Address " + selectedDevice+":"+selectedUserId);
        }

        backButton = (ImageButton)findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        addContactButton = (Button)findViewById(R.id.addContactButton);
        addContactButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                NearbyDevicesScreen.name = nameEditText.getText();
                SplashScreen.db.addContact(new Contact(selectedUserId, selectedDevice, nameEditText.getText().toString()));
                ContactsScreen.remoteXBeeDeviceAdapterName.notifyDataSetChanged();
                finish();
            }
        });


    }
}
