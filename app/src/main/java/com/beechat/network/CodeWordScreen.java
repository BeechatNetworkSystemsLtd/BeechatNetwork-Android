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
 *  --- CodeWordScreen ---
 *  This class is responsible for adding new contacts.
 ***/
public class CodeWordScreen extends AppCompatActivity {

    // Variables.
    EditText codeWordEdit;
    Button CWOKButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.code_word_screen);

        codeWordEdit = findViewById(R.id.codeWordEdit);
        CWOKButton = findViewById(R.id.buttonCWOK);

        CWOKButton.setOnClickListener(v -> {
            String cw = codeWordEdit.getText().toString();

            Intent intent = new Intent();
            intent.putExtra("cw", cw);
            setResult(RESULT_OK, intent);
            finish();
        });
    }
}

