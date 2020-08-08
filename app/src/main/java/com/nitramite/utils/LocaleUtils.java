package com.nitramite.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nitramite.paketinseuranta.Constants;

import java.util.Locale;

@SuppressWarnings("HardCodedStringLiteral")
public class LocaleUtils {

    //  Logging
    private static final String TAG = "LocaleUtils";

    // Set locale manually
    public Context updateBaseContextLocale(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String localeStr = sharedPreferences.getString(Constants.SP_APPLICATION_LANGUAGE, null);
        if (localeStr != null) {
            // Override language with selected one
            Log.i(TAG, localeStr);
            Locale locale = new Locale(localeStr.toLowerCase(Locale.getDefault()), localeStr.toUpperCase(Locale.getDefault()));
            Locale.setDefault(locale);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return updateResourcesLocale(context, locale);
            }

            return updateResourcesLocaleLegacy(context, locale);
        } else {
            // No language override, set default
            return context;
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    private Context updateResourcesLocaleLegacy(Context context, Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }


} // End of class