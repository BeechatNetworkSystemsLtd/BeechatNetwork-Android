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

import com.digi.xbee.api.DigiMeshNetwork;
import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.android.DigiMeshDevice;
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
    public static String senderId = null;
    public static String receiverId = null;
    public static Editable nameContact = null;
    public static List<String> xbee_names = new ArrayList<>();
    public static List<String> names = new ArrayList<>();
    private static String selectedDevice = null;

    public static DigiMeshDevice myContactDevice;
    public static ArrayList<String> dmaContactDevices = new ArrayList<>();
    public static Editable name = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.contacts_screen, container, false);
        context = LocaleHelper.setLocale(getActivity(), SelectLanguageScreen.language);
        resources = context.getResources();
        myContactDevice = new DigiMeshDevice(getActivity(), BAUD_RATE, permissionListener);
        // Reading all users
        System.out.println("Reading: " + "Reading all users..");
        List<User> users = SplashScreen.db.getAllUsers();
        List<String> xbee_contacts = new ArrayList<>();

        for (User cn : users) {
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
                // Reading all users
                System.out.println("Reading: " + "Reading all users..");
                List<User> users = SplashScreen.db.getAllUsers();


                for (User cn : users) {
                    xbee_names.add(cn.getXbeeDeviceNumber());
                    names.add(cn.getName());
                }

                /*if (!NearbyDevicesScreen.dmaDevices.isEmpty()) {
                    senderId = NearbyDevicesScreen.getDMDevice().toString();
                    selectedDevice = senderId;//remoteXBeeDeviceAdapter.getItem(i);
                    receiverId = selectedDevice;
                    connectToDevice(selectedDevice);
                } else {*/
                    senderId = getDMContactDevice().toString();
                    selectedDevice = xbee_names.get(i);
                    receiverId = selectedDevice;
                    connectToContactDevice(selectedDevice);

            }
        });
        return view;
    }


    /***
     *  --- getDMDevice() ----
     *  The function of gaining access to your device..
     ***/
    public static DigiMeshDevice getDMContactDevice() {
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
        List<User> users = SplashScreen.db.getAllUsers();
        List<String> xbee_contacts = new ArrayList<>();

        for (User cn : users) {
            xbee_contacts.add(cn.getName());
        }
        contacts = xbee_contacts;
        remoteXBeeDeviceAdapterName.notifyDataSetChanged();
    }

    /***
     *  --- connectToDevice(String) ----
     *  The function of establishing a connection with the selected device.
     *
     *  @param device Selected device number.
     ***/
    private void connectToDevice(final String device) {
        final ProgressDialog dialog = ProgressDialog.show(getActivity(), resources.getString(R.string.connecting_device_title),
                resources.getString(R.string.connecting_device_description), true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NearbyDevicesScreen.myDevice.open();

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Intent intent = new Intent(getActivity(), ChatScreen.class);
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
                    NearbyDevicesScreen.myDevice.close();
                }
            }
        }).start();
    }

    private void connectToContactDevice(final String device) {
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
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                List<RemoteXBeeDevice> devices = null;

                if (myContactDevice.isOpen()) {
                    myContactDevice.close();
                }
                try {
                    myContactDevice.open();

                    DigiMeshNetwork network = (DigiMeshNetwork) myContactDevice.getNetwork();
                    network.startDiscoveryProcess();

                    while (network.isDiscoveryRunning()) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    devices = network.getDevices();
                    int i = 0;
                    while (i<devices.size()) {
                        dmaContactDevices.add(devices.get(i).get64BitAddress().toString());
                        i=i+1;
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Intent intent = new Intent(getActivity(), ChatScreen.class);
                            startActivity(intent);
                        }
                    });
                } catch (final XBeeException e) {
                    e.printStackTrace();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            new AlertDialog.Builder(getActivity()).setTitle(getResources().getString(R.string.error_connecting_title))
                                    .setMessage(getResources().getString(R.string.error_connecting_description, e.getMessage()))
                                    .setPositiveButton(android.R.string.ok, null).show();
                        }
                    });
                    myContactDevice.close();
                }
            }
        }).start();*/
    }
}
