package com.beechat.network;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/***
 *  --- EditContactScreen ----
 *  The class is responsible for the adding and editing contacts.
 ***/
public class EditContactScreen extends AppCompatActivity {

    private static String selectedDevice = null;

    TextView addressTextView;
    EditText nameEditText;
    Button backButton, addContactButton, okButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_contact_screen);

        addressTextView = (TextView)findViewById(R.id.addressTextView);
        nameEditText = (EditText) findViewById(R.id.nameEditText);

        Bundle extras = getIntent().getExtras();
        if(extras !=null) {
            selectedDevice = extras.getString("key");
            addressTextView.setText("Address " + selectedDevice);
        }

        backButton = (Button)findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        okButton = (Button)findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });


        addContactButton = (Button)findViewById(R.id.addContactButton);
        addContactButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                NearbyDevicesScreen.name = nameEditText.getText();
                SplashScreen.db.addUser(new User(selectedDevice, nameEditText.getText().toString()));
                ContactsScreen.remoteXBeeDeviceAdapterName.notifyDataSetChanged();
                finish();
            }
        });


    }
}
