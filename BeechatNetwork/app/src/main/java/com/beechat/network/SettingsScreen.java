package com.beechat.network;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
//import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/***
 *  --- SettingScreen ----
 *  The class that is responsible for the application settings.
 ***/
public class SettingsScreen extends Fragment {
    Context context;
    Resources resources;
    private View view;
    private Spinner languageSpinner;
    public static SeekBar baudRateSeekBar;
    private TextView labelBaudRateTextView;
    private Button savedDataButton, applyButton;
    int valueBaudRate = 0;

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.settings_screen, container, false);
        context = LocaleHelper.setLocale(getActivity(), WelcomeScreen.language);
        resources = context.getResources();
        //languageSpinner = (Spinner)  view.findViewById(R.id.languageSpinner);
        baudRateSeekBar = (SeekBar) view.findViewById(R.id.baudRateSeekBar);

        labelBaudRateTextView = (TextView) view.findViewById(R.id.labelBaudRateTextView);
        List<Integer> listValues = Arrays.asList(1200, 2400, 4800, 9600, 19200, 39400, 57600);
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

        savedDataButton = (Button) view.findViewById(R.id.savedDataButton);
        savedDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), DataScreen.class);
                startActivity(intent);
            }
        });

        applyButton = (Button) view.findViewById(R.id.buttonApply);
        applyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                StartScreen.BAUD_RATE = listValues.get(valueBaudRate);
                Toast.makeText(getContext(), "The BAUD RATE was change to " + StartScreen.BAUD_RATE, Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
