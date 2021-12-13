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

import com.digi.xbee.api.exceptions.XBeeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/***
 *  --- ContactsScreen ---
 *  The class that is responsible for the displaying contacts.
 ***/
public class ContactsScreen extends Fragment {
    Context context;
    Resources resources;

    View view;
    ListView contactsListView;
    DatabaseHandler db;
    List<Contact> contactsFromDb;
    String selectedXbeeAddress, selectedUserId, selectedName;

    static CustomContactAdapter remoteXBeeDeviceAdapterName;
    static ArrayList<String> contactNames = new ArrayList<>();
    static ArrayList<String> contactXbeeAddress = new ArrayList<>();
    static ArrayList<String> contactUserIds = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.contacts_screen, container, false);

        context = LocaleHelper.setLocale(getActivity(), WelcomeScreen.language);
        resources = context.getResources();

        db = new DatabaseHandler(getActivity());

        contactsFromDb = db.getAllContacts();

        for (Contact cn : contactsFromDb) {
            contactNames.add(cn.getName());
            contactXbeeAddress.add(cn.getXbeeDeviceNumber());
            contactUserIds.add(cn.getUserId());
        }

        contactsListView = view.findViewById(R.id.contactsListView);
        remoteXBeeDeviceAdapterName = new CustomContactAdapter(Objects.requireNonNull(getActivity()), contactNames);
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

    /***
     *  --- CustomContactAdapter ---
     *  The class that initializes the list of available devices.
     ***/
    static class CustomContactAdapter extends ArrayAdapter<String> {
        private final Context context;

        CustomContactAdapter(@NonNull Context context, List<String> contacts) {
            super(context, -1, contacts);
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            String contact = contactNames.get(position);

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

    public static void onRefresh() {
        remoteXBeeDeviceAdapterName.notifyDataSetChanged();
    }

    private void connectToContactDevice() {
        final ProgressDialog dialog = ProgressDialog.show(getActivity(), resources.getString(R.string.connecting_device_title),
                resources.getString(R.string.connecting_device_description), true);

        new Thread(() -> {
            try {
                SplashScreen.myXbeeDevice.open();

                Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                    dialog.dismiss();
                    Intent intent = new Intent(getActivity(), ChatScreen.class);
                    intent.putExtra("key_myUserId", SplashScreen.myXbeeDevice.getNodeID());
                    intent.putExtra("key_myXbeeAddress", SplashScreen.myXbeeDevice.get64BitAddress().toString());
                    intent.putExtra("key_selectedName", selectedName);
                    intent.putExtra("key_selectedUserId", selectedUserId);
                    intent.putExtra("key_selectedXbeeAddress", selectedXbeeAddress);
                    startActivity(intent);
                });
            } catch (final XBeeException e) {
                e.printStackTrace();
                Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                    dialog.dismiss();
                    new AlertDialog.Builder(getActivity()).setTitle(resources.getString(R.string.error_connecting_title))
                            .setMessage(resources.getString(R.string.error_connecting_description, e.getMessage()))
                            .setPositiveButton(android.R.string.ok, null).show();
                });
                SplashScreen.myXbeeDevice.close();
            }
        }).start();
    }


}
