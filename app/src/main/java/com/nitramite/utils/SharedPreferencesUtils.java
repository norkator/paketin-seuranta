package com.nitramite.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtils {


    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(getDefaultSharedPreferencesName(context), Context.MODE_PRIVATE);
    }


    private static String getDefaultSharedPreferencesName(Context context) {
        return context.getPackageName() + "_preferences";
    }


}
