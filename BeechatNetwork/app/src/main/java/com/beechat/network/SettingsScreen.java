package com.beechat.network;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import java.util.ArrayList;
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
    private Button savedDataButton;



    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.settings_screen, container, false);
        context = LocaleHelper.setLocale(getActivity(), WelcomeScreen.language);
        resources = context.getResources();
        //languageSpinner = (Spinner)  view.findViewById(R.id.languageSpinner);
        baudRateSeekBar = (SeekBar) view.findViewById(R.id.baudRateSeekBar);

        labelBaudRateTextView = (TextView) view.findViewById(R.id.labelBaudRateTextView);
        labelBaudRateTextView.setText(baudRateSeekBar.getProgress() + "/" + baudRateSeekBar.getMax());
        baudRateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int pval = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pval = progress;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //write custom code to on start progress
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                labelBaudRateTextView.setText(pval + "/" + seekBar.getMax());
            }
        });
        //if (baudRateSeekBar.getProgress() == 1) NearbyDevicesScreen.valueBaudRate = 57600;




        savedDataButton = (Button) view.findViewById(R.id.savedDataButton);
        savedDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), DataScreen.class);
                startActivity(intent);
            }
        });

        return view;
    }
}
