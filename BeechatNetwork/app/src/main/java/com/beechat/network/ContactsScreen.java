package com.beechat.network;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.digi.xbee.api.android.DigiMeshDevice;

import java.util.ArrayList;
import java.util.List;


/***
 *  --- ContactsScreen ----
 *  The class that is responsible for the displaying contacts.
 ***/
public class ContactsScreen extends Fragment {

    public static CustomContactAdapter remoteXBeeDeviceAdapterName;
    public static List<String> contacts = new ArrayList<>();
    View view;
    ListView contactsListView;


    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.contacts_screen, container, false);

        // Reading all users
        System.out.println("Reading: " + "Reading all users..");
        List<User> users = NearbyDevicesScreen.db.getAllUsers();
        List<String> xbee_contacts = new ArrayList<>();

        for (User cn : users) {
            xbee_contacts.add(cn.getName());
        }
        contacts = xbee_contacts;

        contactsListView = (ListView) view.findViewById(R.id.contactsListView);
        remoteXBeeDeviceAdapterName = new CustomContactAdapter(getActivity(), contacts);
        contactsListView.setAdapter(remoteXBeeDeviceAdapterName);
        return view;
    }

    /***
     *  --- CustomContactAdapter ----
     *  The class that initializes the list of available devices.
     ***/
    class CustomContactAdapter extends ArrayAdapter<String> {
        private Context context;

        CustomContactAdapter(@NonNull Context context, List<String> contacts) {
            super(context, -1, contacts);
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            String contact = contacts.get(position);

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 30, 40, 30);

            TextView nameText = new TextView(context);
            nameText.setText(contact);
            nameText.setTypeface(nameText.getTypeface(), Typeface.BOLD);
            nameText.setTextSize(18);
            layout.addView(nameText);

            return layout;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        remoteXBeeDeviceAdapterName.notifyDataSetChanged();
    }

}
