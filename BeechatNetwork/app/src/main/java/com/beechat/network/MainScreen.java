package com.beechat.network;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

/***
 *  --- MainScreen ---
 *  The class that is responsible for the main application window.
 ***/
public class MainScreen extends AppCompatActivity {
    Context context;
    Resources resources;
    ViewPager viewPager;
    TabLayout tabLayout;
    ArrayList<Fragment> fragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);

        context = LocaleHelper.setLocale(MainScreen.this, WelcomeScreen.language);
        resources = context.getResources();

        viewPager = findViewById(R.id.pager);
        tabLayout = findViewById(R.id.tabLayout);

        fragments = new ArrayList<>();

        fragments.add(new NearbyDevicesScreen());
        fragments.add(new ContactsScreen());
        fragments.add(new BroadcastScreen());
        fragments.add(new SettingsScreen());

        FragmentAdapter pagerAdapter = new FragmentAdapter(getSupportFragmentManager(), getApplicationContext(), fragments);
        viewPager.setAdapter(pagerAdapter);

        tabLayout.setupWithViewPager(viewPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.nearby_black);
        tabLayout.getTabAt(1).setIcon(R.drawable.chat_black);
        tabLayout.getTabAt(2).setIcon(R.drawable.broadcast_black);
        tabLayout.getTabAt(3).setIcon(R.drawable.settings_black);

    }
}
