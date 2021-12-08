package com.beechat.network;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SelectLanguageScreen extends AppCompatActivity{
    Button  buttonContinue;
    Context context;
    Resources resources;
    Spinner languagesList;
    public static String language = null;
    String[] languages = {"en", "es"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_language_screen);

        // referencing the text and button views
        buttonContinue = findViewById(R.id.continueButton);

        languagesList = findViewById(R.id.languageSpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languagesList.setAdapter(adapter);

        AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String item = (String)parent.getItemAtPosition(position);
                language = item;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        languagesList.setOnItemSelectedListener(itemSelectedListener);
        language = languagesList.getSelectedItem().toString();


        context = LocaleHelper.setLocale(SelectLanguageScreen.this, language);
        resources = context.getResources();
        // setting up on click listener event over the button
        // in order to change the language with the help of
        // LocaleHelper class
        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SelectLanguageScreen.this, SplashScreen.class);
                startActivity(intent);
            }
        });
    }

}

