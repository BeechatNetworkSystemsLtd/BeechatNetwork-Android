package com.beechat.network;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.digi.xbee.api.android.XBeeDevice;
import com.digi.xbee.api.exceptions.XBeeException;

import androidx.fragment.app.Fragment;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.nio.ByteBuffer;


/***
 *  --- SettingScreen ---
 *  The class that is responsible for the application settings.
 ***/
public class SettingsScreen extends Fragment {

    // Variables
    Context context;
    Resources resources;
    View view;
    SeekBar baudRateSeekBar;
    TextView labelBaudRateTextView;
    TextView settingsRadioAddr;
    TextView settingsBeechatAddr;
    Button savedDataButton, applyButton, logoutButton, reconnectButton;
    static DatabaseHandler db;
    int FILE_SELECT_CODE = 101;

    // Constants
    List<Integer> listValues = Arrays.asList(1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200, 230400);
    long valueBaudRate = 3;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.settings_screen, container, false);

        context = LocaleHelper.setLocale(getActivity(), WelcomeScreen.language);
        resources = context.getResources();

        db = new DatabaseHandler(context);

        baudRateSeekBar = view.findViewById(R.id.baudRateSeekBar);
        savedDataButton = view.findViewById(R.id.savedDataButton);
        reconnectButton = view.findViewById(R.id.buttonReconnect);
        settingsRadioAddr = view.findViewById(R.id.settingsRadioAddr);
        settingsBeechatAddr = view.findViewById(R.id.settingsBeechatAddr);
        applyButton = view.findViewById(R.id.buttonApply);
        logoutButton = view.findViewById(R.id.buttonLogOut);
        labelBaudRateTextView = view.findViewById(R.id.labelBaudRateTextView);

        int baud = db.getBaud();

        labelBaudRateTextView.setText(Integer.toString(baud / 8) + " B/s\n" + Integer.toString(baud) + " baud");
        for (int i = 0; i < listValues.size(); i++) {
            if (listValues.get(i) == baud) {
                baudRateSeekBar.setProgress(i);
            }
        }

        if (SplashScreen.addressMyXbeeDevice != null) {
            if (SplashScreen.addressMyXbeeDevice.length() > 0) {
                settingsRadioAddr.setText(SplashScreen.addressMyXbeeDevice);
            }
        }
        settingsBeechatAddr.setText(Blake3.toString(SplashScreen.myGeneratedUserId));

        baudRateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueBaudRate = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int speed = listValues.get((int)valueBaudRate) / 8;
                labelBaudRateTextView.setText(Integer.toString(speed) + " B/s\n" + Integer.toString(listValues.get((int)valueBaudRate)) + " baud");
                SplashScreen.BAUD_RATE = listValues.get((int)valueBaudRate);
            }
        });

        // Handling the event of attaching files.
        reconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    SplashScreen.myXbeeDevice.close();
                } catch (Exception e) {
                }
                SplashScreen.myXbeeDevice = new XBeeDevice(getActivity(), listValues.get((int)valueBaudRate));
                new Thread(() -> {
                    try {
                        SplashScreen.myXbeeDevice.open();
                        SplashScreen.myXbeeDevice.setNodeID(Base58.encode(SplashScreen.myGeneratedUserId));
                    } catch (Exception e) {
                    }
                }).start();
            }
        });

        savedDataButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DataScreen.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            MainScreen.contactNames.clear();
            MainScreen.contactXbeeAddress.clear();
            MainScreen.contactUserIds.clear();
            Intent intent = new Intent(getActivity(), StartScreen.class);
            startActivity(intent);
            Objects.requireNonNull(getActivity()).finish();
        });

        applyButton.setOnClickListener(v -> {
            db.setBaud(listValues.get((int)valueBaudRate));
            try {
                SplashScreen.myXbeeDevice.setParameter("BD", ByteBuffer.allocate(8).putLong(valueBaudRate).array());
                SplashScreen.myXbeeDevice.applyChanges();
                SplashScreen.myXbeeDevice.writeChanges();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Toast.makeText(getContext(), "The baud rate was changed to " + SplashScreen.BAUD_RATE, Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}
