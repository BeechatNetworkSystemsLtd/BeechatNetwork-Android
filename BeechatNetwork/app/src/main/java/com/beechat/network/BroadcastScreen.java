package com.beechat.network;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;


/***
 *  --- BroadcastScreen ---
 *  This class is responsible for data broadcasts.
 ***/
public class BroadcastScreen extends Fragment {

    // Variables
    View view;
    TextView textViewMyID;
    DatabaseHandler DB;

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.broadcast_screen, container, false);

        DB = new DatabaseHandler(getContext());

        textViewMyID = view.findViewById(R.id.textViewMyID);

        textViewMyID.setText("My ID \n" + Blake3.toString(SplashScreen.myGeneratedUserId));
        return view;
    }
}
