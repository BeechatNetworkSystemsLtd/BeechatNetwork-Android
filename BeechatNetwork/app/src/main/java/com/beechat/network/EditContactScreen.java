package com.beechat.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.digi.xbee.api.DigiMeshNetwork;
import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.android.DigiMeshDevice;
import com.digi.xbee.api.android.connection.usb.AndroidUSBPermissionListener;
import com.digi.xbee.api.exceptions.XBeeException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
                finish();
            }
        });


        addContactButton = (Button)findViewById(R.id.addContactButton);
        addContactButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                NearbyDevicesScreen.flag = true;
                NearbyDevicesScreen.name = nameEditText.getText();
                finish();
            }
        });


    }
}
