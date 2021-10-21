package com.beechat.network;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/***
 *  --- ContactsScreen ----
 *  The class that is responsible for the displaying contacts.
 ***/
public class ContactsScreen extends Fragment {

    View view;

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.contacts_screen, container, false);
        return view;
    }
}
