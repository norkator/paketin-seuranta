/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.paketinseuranta;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.multidex.MultiDex;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.nitramite.paketinseuranta.notifier.PushUtils;
import com.nitramite.utils.LocaleUtils;
import com.nitramite.utils.ThemeUtils;

import java.util.Locale;

/**
 * Created by Martin on 28.1.2016.
 */
@SuppressWarnings("HardCodedStringLiteral")
public class MyPreferencesActivity extends AppCompatActivity {


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, MainMenu.class));
    }


    //  Logging
    private static final String TAG = MyPreferencesActivity.class.getSimpleName();


    // Components
    private LocaleUtils localeUtils = new LocaleUtils();


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(localeUtils.updateBaseContextLocale(base));
        MultiDex.install(this);
    }


    protected <T extends Fragment> T initFragment(@IdRes int target,
                                                  @NonNull T fragment) {
        return initFragment(target, fragment, null);
    }

    protected <T extends Fragment> T initFragment(@IdRes int target,
                                                  @NonNull T fragment,
                                                  @Nullable Locale locale) {
        return initFragment(target, fragment, locale, null);
    }

    protected <T extends Fragment> T initFragment(@IdRes int target,
                                                  @NonNull T fragment,
                                                  @Nullable Locale locale,
                                                  @Nullable Bundle extras) {
        Bundle args = new Bundle();

        if (extras != null) {
            args.putAll(extras);
        }

        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(target, fragment)
                .commitAllowingStateLoss();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ThemeUtils.Theme.isDarkThemeForced(getBaseContext())) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
        } else if (ThemeUtils.Theme.isAutoTheme(getBaseContext())) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }


        setContentView(R.layout.activity_preferences);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        // Do not add custom layout for lollipop devices or we lose the widgets animation
        // (app compat bug?)


        // Setting the toolbar again, to add the listener


        MyPreferenceFragment myPreferenceFragment = new MyPreferenceFragment();
        initFragment(R.id.content, myPreferenceFragment);


        // Change listener
        myPreferenceFragment.getFragmentManager().executePendingTransactions();
        ListPreference listPreference = myPreferenceFragment.findPreference(Constants.SP_THEME_SELECTION);
        listPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            recreate();
            return true;
        });

        ListPreference listPreferenceLanguage = myPreferenceFragment.findPreference(Constants.SP_APPLICATION_LANGUAGE);
        listPreferenceLanguage.setOnPreferenceChangeListener((preference, newValue) -> {
            recreate();
            return true;
        });


        // EditTextPreference parcels_update_rate = (EditTextPreference) myPreferenceFragment.findPreference("parcels_update_rate");
        SwitchPreference parcelAutomaticUpdateSwitch = myPreferenceFragment.findPreference(Constants.SP_PARCELS_AUTOMATIC_UPDATE);
        parcelAutomaticUpdateSwitch.setOnPreferenceChangeListener((preference, o) -> {
            if (o.toString().equals("false")) {
                PushUtils.unsubscribeFromTopic(PushUtils.TOPIC_UPDATE);
            } else
                PushUtils.subscribeToTopic(PushUtils.TOPIC_UPDATE);
            return true;
        });


    }

    public static class MyPreferenceFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            } else {
                MyPreferencesActivity.this.onBackPressed();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


} // END OF CLASS
