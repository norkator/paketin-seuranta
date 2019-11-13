package com.nitramite.paketinseuranta;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.multidex.MultiDex;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.nitramite.utils.LocaleUtils;

/**
 * Created by Martin on 28.1.2016.
 */
public class MyPreferencesActivity extends com.fnp.materialpreferences.PreferenceActivity {


    //  Logging
    private static final String TAG = "MyPreferencesActivity";


    // Components
    private LocaleUtils localeUtils = new LocaleUtils();


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(localeUtils.updateBaseContextLocale(base));
        MultiDex.install(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyPreferenceFragment myPreferenceFragment = new MyPreferenceFragment();
        setPreferenceFragment(myPreferenceFragment);


        // Change listener
        myPreferenceFragment.getFragmentManager().executePendingTransactions();
        ListPreference listPreference = (ListPreference) myPreferenceFragment.findPreference(Constants.SP_THEME_SELECTION);
        listPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            Toast.makeText(MyPreferencesActivity.this, R.string.my_preferences_activity_theme_change_takes_effect_after_restart, Toast.LENGTH_LONG).show();
            return true;
        });

        ListPreference listPreferenceLanguage = (ListPreference) myPreferenceFragment.findPreference(Constants.SP_APPLICATION_LANGUAGE);
        listPreferenceLanguage.setOnPreferenceChangeListener((preference, newValue) -> {
            Toast.makeText(MyPreferencesActivity.this, R.string.my_preferences_application_language_override_change_takes_effect_after_restart, Toast.LENGTH_LONG).show();
            return true;
        });


        // EditTextPreference parcels_update_rate = (EditTextPreference) myPreferenceFragment.findPreference("parcels_update_rate");
        SwitchPreference parcelAutomaticUpdateSwitch = (SwitchPreference) myPreferenceFragment.findPreference(Constants.SP_PARCELS_AUTOMATIC_UPDATE);
        parcelAutomaticUpdateSwitch.setOnPreferenceChangeListener((preference, o) -> {
            if (o.toString().equals("false")) {
                stopService(new Intent(MyPreferencesActivity.this, ParcelServiceTimer.class));
                stopService(new Intent(MyPreferencesActivity.this, ParcelService.class));
            }
            return true;
        });




    }

    public static class MyPreferenceFragment extends com.fnp.materialpreferences.PreferenceFragment {
        @Override
        public int addPreferencesFromResource() {
            return R.xml.preferences; // Your preference file
        }
    }

} // END OF CLASS
