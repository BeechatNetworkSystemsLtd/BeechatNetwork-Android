package com.beechat.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import android.widget.ImageButton;


/***
 *  --- ContactsScreen ---
 *  The class that is responsible for the displaying contacts.
 ***/
public class ContactsScreen extends Fragment {
    Context context;
    Resources resources;

    View view;
    ListView contactsListView;
    static DatabaseHandler db;
    List<Contact> contactsFromDb;
    static String selectedXbeeAddress, selectedUserId, selectedName;

    ImageButton goToNearby;
    static CustomContactAdapter remoteXBeeDeviceAdapterName;
    static ArrayList<String> contactNames = new ArrayList<>();
    static ArrayList<String> contactXbeeAddress = new ArrayList<>();
    public static ArrayList<String> contactUserIds = new ArrayList<>();
    public static ArrayList<ContactInfo> contactInfos = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.contacts_screen, container, false);

        context = LocaleHelper.setLocale(getActivity(), WelcomeScreen.language);
        resources = context.getResources();
        goToNearby = view.findViewById(R.id.goToNearby);

        goToNearby.setOnClickListener(new View.OnClickListener() {
            /*final ProgressDialog dialog = ProgressDialog.show(getActivity(), resources.getString(R.string.connecting_device_title),
                    resources.getString(R.string.connecting_device_description), true);
            */
            public void onClick(View v) {
                new Thread(() -> {
                    Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                        //dialog.dismiss();
                        Intent intent = new Intent(getActivity(), NearbyDevicesScreen.class);
                        startActivity(intent);
                    });
                }).start();
            }
        });

        db = new DatabaseHandler(getActivity());

        contactNames.clear();
        contactXbeeAddress.clear();
        contactUserIds.clear();
        contactInfos.clear();

        contactsFromDb = db.getAllContacts(Blake3.toString(SplashScreen.myGeneratedUserId));

        for (Contact cn : contactsFromDb) {
            contactNames.add(cn.getName());
            contactXbeeAddress.add(cn.getXbeeDeviceNumber());
            contactUserIds.add(cn.getUserId());
            ContactInfo ci = new ContactInfo();
            ci.name = cn.getName();
            if (ci.name == null) {
                ci.name = "User";
                ci.label = "U";
                ci.mes = "";
                ci.date = "";
                contactInfos.add(ci);
                continue;
            }
            ci.label = ci.name.substring(0, 1);
            ci.mes = db.getLastMessage(Blake3.toString(SplashScreen.myGeneratedUserId), SplashScreen.addressMyXbeeDevice, cn.getUserId(), cn.getXbeeDeviceNumber());
            if (ci.mes.endsWith("S") || ci.mes.endsWith("P") || ci.mes.endsWith("F")) {
                ci.mes = ChatScreen.removeLastChar(ci.mes);
                ci.mes = ChatScreen.removeLastChar(ci.mes);
            }
            if (ci.mes == null) {
                ci.mes = "File";
                ci.date = "06:14";
                contactInfos.add(ci);
                continue;
            }
            String[] mesData = ci.mes.split("\n");
            ci.mes = mesData[0];
            if (mesData.length > 1) {
                ci.date = mesData[1];
            }
            if (ci.mes == null) ci.mes = "";
            if (ci.date == null) ci.date = "";
            contactInfos.add(ci);
        }

        contactsListView = view.findViewById(R.id.contactsListView);
        remoteXBeeDeviceAdapterName = new CustomContactAdapter(Objects.requireNonNull(getActivity()), contactInfos);
        contactsListView.setAdapter(remoteXBeeDeviceAdapterName);

        // Handling an event on clicking an item from the list of available devices.
        contactsListView.setOnItemClickListener((adapterView, view, i, l) -> {

            selectedXbeeAddress = contactXbeeAddress.get(i);
            selectedUserId = contactUserIds.get(i);
            selectedName = contactNames.get(i);

            connectToContactDevice();

        });
        return view;
    }

    static class ContactInfo {
        public String name;
        public String mes;
        public String date;
        public String label;
        public boolean checked;
    }

    /***
     *  --- CustomContactAdapter ---
     *  The class that initializes the list of available devices.
     ***/
    static class CustomContactAdapter extends ArrayAdapter<ContactInfo> {
        private final Context context;

        CustomContactAdapter(@NonNull Context context, List<ContactInfo> contacts) {
            super(context, 0, contacts);
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if (listItem == null) {
                listItem = LayoutInflater.from(context).inflate(R.layout.contact_layout, parent, false);
            }

            ContactInfo contact = contactInfos.get(position);

            TextView label = (TextView)listItem.findViewById(R.id.contact_label);
            label.setText(contact.label);

            TextView name = (TextView)listItem.findViewById(R.id.contact_name);
            name.setText(contact.name);

            TextView mes = (TextView)listItem.findViewById(R.id.contact_mes);
            mes.setText(contact.mes);

            TextView date = (TextView)listItem.findViewById(R.id.contact_date);
            date.setText(contact.date);

            return listItem;
        }
    }

    public static void onRefresh() {
        remoteXBeeDeviceAdapterName.notifyDataSetChanged();
    }

    private void connectToContactDevice() {
        final ProgressDialog dialog = ProgressDialog.show(getActivity(), resources.getString(R.string.connecting_device_title),
                resources.getString(R.string.connecting_device_description), true);

        new Thread(() -> {
            Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                dialog.dismiss();
                Intent intent = new Intent(getActivity(), ChatScreen.class);
                intent.putExtra("key_myUserId", Blake3.toString(SplashScreen.myGeneratedUserId));
                intent.putExtra("key_myXbeeAddress", SplashScreen.addressMyXbeeDevice);
                intent.putExtra("key_selectedName", selectedName);
                intent.putExtra("key_selectedUserId", selectedUserId);
                intent.putExtra("key_selectedXbeeAddress", selectedXbeeAddress);
                startActivity(intent);
            });
        }).start();
    }
}
