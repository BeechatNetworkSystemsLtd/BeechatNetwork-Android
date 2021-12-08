package com.beechat.network;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/***
 *  --- BroadcastScreen ----
 *  The class that is responsible for the ...
 ***/
public class BroadcastScreen extends Fragment {

    View view;
    TextView textViewMyID;
    DatabaseHandler DB;
    String user_id;
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.broadcast_screen, container, false);
        DB = new DatabaseHandler(getContext());
        user_id = SplashScreen.splashScreenUserId;
        textViewMyID = (TextView) view.findViewById(R.id.textViewMyID);
        textViewMyID.setText("My ID \n"+user_id);
        return view;
    }
}
