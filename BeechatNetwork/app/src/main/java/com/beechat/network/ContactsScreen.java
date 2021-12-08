package com.beechat.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.digi.xbee.api.android.XBeeDevice;
import com.digi.xbee.api.android.connection.usb.AndroidUSBPermissionListener;
import com.digi.xbee.api.exceptions.XBeeException;

import java.util.ArrayList;
import java.util.List;


/***
 *  --- ContactsScreen ----
 *  The class that is responsible for the displaying contacts.
 ***/
public class ContactsScreen extends Fragment {
    Context context;
    Resources resources;

    private static final int BAUD_RATE = 57600;
    private AndroidUSBPermissionListener permissionListener;
    public static CustomContactAdapter remoteXBeeDeviceAdapterName;
    public static List<String> contacts = new ArrayList<>();
    View view;
    ListView contactsListView;
    public static List<String> xbee_names = new ArrayList<>();
    public static List<String> xbee_user_ids = new ArrayList<>();
    public static List<String> names = new ArrayList<>();
    private static String selectedDevice = null;
    private static String selectedUserId = null;
    private static String selectedName= null;

    public static XBeeDevice myContactDevice;
    public static ArrayList<String> dmaContactDevices = new ArrayList<>();
    public static Editable name = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.contacts_screen, container, false);

        context = LocaleHelper.setLocale(getActivity(), WelcomeScreen.language);
        resources = context.getResources();

        myContactDevice = new XBeeDevice(getActivity(), BAUD_RATE, permissionListener);

        List<Contact> users = SplashScreen.db.getAllContacts();
        List<String> xbee_contacts = new ArrayList<>();

        for (Contact cn : users) {
            xbee_contacts.add(cn.getName());
        }
        contacts = xbee_contacts;

        contactsListView = (ListView) view.findViewById(R.id.contactsListView);
        remoteXBeeDeviceAdapterName = new CustomContactAdapter(getActivity(), contacts);
        contactsListView.setAdapter(remoteXBeeDeviceAdapterName);

        // Handling an event on clicking an item from the list of available devices.
        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                List<Contact> users = SplashScreen.db.getAllContacts();

                for (Contact cn : users) {
                    xbee_user_ids.add(cn.getUserId());
                    xbee_names.add(cn.getXbeeDeviceNumber());
                    names.add(cn.getName());
                }

                selectedUserId = xbee_user_ids.get(i);
                selectedDevice = xbee_names.get(i);
                selectedName = names.get(i);


                connectToContactDevice();

            }
        });
        return view;
    }


    /***
     *  --- getDMDevice() ----
     *  The function of gaining access to your device..
     ***/
    public static XBeeDevice getDMContactDevice() {
        return myContactDevice;
    }

    /***
     *  --- getSelectedDevice() ----
     *  The function of gaining access to the selected device
     ***/
    public static String getSelectedDevice() {
        return selectedDevice;
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
        List<Contact> users = SplashScreen.db.getAllContacts();
        List<String> xbee_contacts = new ArrayList<>();

        for (Contact cn : users) {
            xbee_contacts.add(cn.getName());
        }
        contacts = xbee_contacts;
        remoteXBeeDeviceAdapterName.notifyDataSetChanged();
    }

    private void connectToContactDevice() {
        final ProgressDialog dialog = ProgressDialog.show(getActivity(), resources.getString(R.string.connecting_device_title),
                resources.getString(R.string.connecting_device_description), true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    myContactDevice.open();

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Intent intent = new Intent(getActivity(), ChatScreen.class);
                            intent.putExtra("key_name",selectedName);
                            intent.putExtra("sender_id",myContactDevice.getNodeID());
                            intent.putExtra("xbee_sender",myContactDevice.get64BitAddress().toString());
                            intent.putExtra("receiver_id",selectedUserId);
                            intent.putExtra("xbee_receiver",selectedDevice);
                            startActivity(intent);
                        }
                    });
                } catch (final XBeeException e) {
                    e.printStackTrace();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            new AlertDialog.Builder(getActivity()).setTitle(resources.getString(R.string.error_connecting_title))
                                    .setMessage(resources.getString(R.string.error_connecting_description, e.getMessage()))
                                    .setPositiveButton(android.R.string.ok, null).show();
                        }
                    });
                    myContactDevice.close();
                }
            }
        }).start();
    }
}
