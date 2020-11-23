package com.nitramite.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import androidx.preference.PreferenceManager;
import android.util.Log;

import com.nitramite.paketinseuranta.Constants;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings("HardCodedStringLiteral")
public class LocaleUtils {

    //  Logging
    private static final String TAG = "LocaleUtils";


    /**
     * Create base context and set locale manually
     *
     * @param context app context
     * @return custom app context with locale
     */
    public Context updateBaseContextLocale(Context context) {
        String localeStr = getLocaleString(context);
        if (localeStr != null) {
            // Override language with selected one
            Log.i(TAG, localeStr);
            Locale locale = new Locale(localeStr.toLowerCase(Locale.getDefault()), localeStr.toUpperCase(Locale.getDefault()));
            Locale.setDefault(locale);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return updateResourcesLocaleQ(context, locale);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return updateResourcesLocale(context, locale);
            }

            return updateResourcesLocaleLegacy(context, locale);
        } else {
            // No language override, set default
            return context;
        }
    }

    /**
     * Set locale, Android Q up version
     *
     * @param context app context
     * @param locale  locale
     * @return context with new locale
     */
    @TargetApi(Build.VERSION_CODES.Q)
    private Context updateResourcesLocaleQ(Context context, Locale locale) {
        Configuration configuration = context.getResources().getConfiguration();
        Set<Locale> set = new LinkedHashSet<>();
        set.add(locale);
        LocaleList all = LocaleList.getDefault();
        for (int i = 0; i < all.size(); i++) {
            set.add(all.get(i));
        }
        Locale[] locales = set.toArray(new Locale[0]);
        configuration.setLocales(new LocaleList(locales));
        return context.createConfigurationContext(configuration);
    }

    /**
     * Set locale, Android N up version
     *
     * @param context app context
     * @param locale  locale
     * @return context with new locale
     */
    @TargetApi(Build.VERSION_CODES.N)
    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }


    /**
     * Set locale, legacy version
     *
     * @param context app context
     * @param locale  locale
     * @return context with new locale
     */
    private Context updateResourcesLocaleLegacy(Context context, Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }


    private String getLocaleString(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(Constants.SP_APPLICATION_LANGUAGE, null);
    }


    public void setApplicationLanguage(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String localeStr = getLocaleString(context);
                Locale locale = new Locale(localeStr.toLowerCase(Locale.getDefault()), localeStr.toUpperCase(Locale.getDefault()));
                Resources activityRes = context.getResources();
                Configuration activityConf = activityRes.getConfiguration();
                activityConf.setLocale(locale);
                activityRes.updateConfiguration(activityConf, activityRes.getDisplayMetrics());
                Resources applicationRes = context.getApplicationContext().getResources();
                Configuration applicationConf = applicationRes.getConfiguration();
                applicationConf.setLocale(locale);
                applicationRes.updateConfiguration(applicationConf, applicationRes.getDisplayMetrics());
            }
        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


} // End of class