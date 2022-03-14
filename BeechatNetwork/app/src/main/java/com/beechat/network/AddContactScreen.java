package com.beechat.network;

import android.content.Context;
import android.content.res.Resources;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.digi.xbee.api.models.XBeeMessage;
import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.exceptions.XBeeException;
import com.digi.xbee.api.models.XBee64BitAddress;

import androidx.appcompat.app.AppCompatActivity;

/***
 *  --- AddContactScreen ---
 *  This class is responsible for adding new contacts.
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
    int selectedNum;

    String selectedXbeeDevice, selectedUserId, name, ownerContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_contact_screen);

        db = new DatabaseHandler(this);

        context = LocaleHelper.setLocale(getApplicationContext(), WelcomeScreen.language);
        resources = context.getResources();

        Bundle extras = getIntent().getExtras();

        addressTextView = findViewById(R.id.addressTextView);
        nameEditText = findViewById(R.id.nameEditText);
        backButton = findViewById(R.id.backButton);
        addContactButton = findViewById(R.id.addContactButton);

        if (extras != null) {
            selectedXbeeDevice = extras.getString("key_selectedXbeeDevice");
            selectedUserId = extras.getString("key_selectedUserId");
            selectedNum = extras.getInt("num_selected");
            addressTextView.setText("Address " + selectedXbeeDevice + "(" + selectedUserId + ")");
        }

        ownerContact = Blake3.toString(SplashScreen.myGeneratedUserId);

        backButton.setOnClickListener(v -> finish());

        addContactButton.setOnClickListener(v -> {
            name = nameEditText.getText().toString();

            if (db.getKey(selectedXbeeDevice) != null) {
                db.addContact(
                    new Contact(
                        selectedUserId
                      , selectedXbeeDevice
                      , name
                      , ownerContact
                    )
                );
                ContactsScreen.contactNames.add(name);
                ContactsScreen.contactUserIds.add(selectedUserId);
                ContactsScreen.contactXbeeAddress.add(selectedXbeeDevice);
                ContactsScreen.onRefresh();
            } else {
                if (MainScreen.userAddLock) {
                    finish();
                }
                try {
                    SplashScreen.hasher.clear();
                    SplashScreen.hasher.update(SplashScreen.myGeneratedUserId, User.NODEID_SIZE);
                    SplashScreen.randomHash = SplashScreen.hasher.finalize(128);
                } catch (Exception ex) {
                    System.out.println("Exception: " + ex.getMessage());
                    ex.printStackTrace();
                }
                ContactsScreen.contactNames.add("Init ...");
                ContactsScreen.onRefresh();
                Message getDP = new Message(Packet.Type.DP_KEY, SplashScreen.randomHash);
                try {
                    getDP.send(
                        SplashScreen.myXbeeDevice
                      , new RemoteXBeeDevice(
                            SplashScreen.myXbeeDevice
                            , new XBee64BitAddress(selectedXbeeDevice)
                        )
                      , SplashScreen.hasher
                    );
                } catch (Exception ex) {
                    System.out.println("Exception: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            Intent intent = new Intent();
            intent.putExtra("newname", name);
            intent.putExtra("userid", selectedUserId);
            intent.putExtra("addr", selectedXbeeDevice);
            intent.putExtra("name", name + " (" + selectedXbeeDevice + ")");
            intent.putExtra("id", selectedNum);
            setResult(RESULT_OK, intent);
            finish();
        });


    }
}
