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

import androidx.fragment.app.Fragment;

import java.util.Arrays;
import java.util.List;


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
    Button savedDataButton, applyButton, logoutButton;

    // Constants
    List<Integer> listValues = Arrays.asList(1200, 2400, 4800, 9600, 19200, 39400, 57600);
    int valueBaudRate = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.settings_screen, container, false);

        context = LocaleHelper.setLocale(getActivity(), WelcomeScreen.language);
        resources = context.getResources();

        languageSpinner = view.findViewById(R.id.languageSpinner);
        baudRateSeekBar = view.findViewById(R.id.baudRateSeekBar);
        savedDataButton = view.findViewById(R.id.savedDataButton);
        applyButton = view.findViewById(R.id.buttonApply);
        logoutButton = view.findViewById(R.id.buttonLogOut);
        labelBaudRateTextView = view.findViewById(R.id.labelBaudRateTextView);

        labelBaudRateTextView.setText(listValues.get(0) + "/" + listValues.get(6));

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
                labelBaudRateTextView.setText(listValues.get(valueBaudRate) + "/" + listValues.get(6));
            }
        });

        savedDataButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DataScreen.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), StartScreen.class);
            startActivity(intent);
            getActivity().finish();
        });

        applyButton.setOnClickListener(v -> {
            SplashScreen.BAUD_RATE = listValues.get(valueBaudRate);
            Toast.makeText(getContext(), "The BAUD RATE was change to " + SplashScreen.BAUD_RATE, Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}
