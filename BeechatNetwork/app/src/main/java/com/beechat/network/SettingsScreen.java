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
    Spinner languageSpinner;
    SeekBar baudRateSeekBar;
    TextView labelBaudRateTextView;
    Button savedDataButton, applyButton, logoutButton, exportButton;
    static DatabaseHandler db;
    int FILE_SELECT_CODE = 101;

    // Constants
    List<Integer> listValues = Arrays.asList(2400, 4800, 9600, 19200, 39400, 57600, 115200);
    long valueBaudRate = 2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.settings_screen, container, false);

        context = LocaleHelper.setLocale(getActivity(), WelcomeScreen.language);
        resources = context.getResources();

        db = new DatabaseHandler(context);

        languageSpinner = view.findViewById(R.id.languageSpinner);
        baudRateSeekBar = view.findViewById(R.id.baudRateSeekBar);
        savedDataButton = view.findViewById(R.id.savedDataButton);
        exportButton = view.findViewById(R.id.buttonExport);
        applyButton = view.findViewById(R.id.buttonApply);
        logoutButton = view.findViewById(R.id.buttonLogOut);
        labelBaudRateTextView = view.findViewById(R.id.labelBaudRateTextView);

        labelBaudRateTextView.setText("Baud rate " + Integer.toString(listValues.get((int)valueBaudRate)));

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
                labelBaudRateTextView.setText("Baud rate " + Integer.toString(listValues.get((int)valueBaudRate)));
                try {
                    SplashScreen.myXbeeDevice.setParameter("BD", ByteBuffer.allocate(8).putLong(valueBaudRate).array());
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

        // Handling the event of attaching files.
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), CodeWordScreen.class);
                startActivityForResult(intent, 14);
            }
        });

        savedDataButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DataScreen.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), StartScreen.class);
            startActivity(intent);
            Objects.requireNonNull(getActivity()).finish();
        });

        applyButton.setOnClickListener(v -> {
            SplashScreen.BAUD_RATE = listValues.get((int)valueBaudRate);
            Toast.makeText(getContext(), "The baud rate was changed to " + SplashScreen.BAUD_RATE, Toast.LENGTH_SHORT).show();
        });

        return view;
    }

}
