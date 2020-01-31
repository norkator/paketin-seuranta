/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 */

package com.nitramite.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nitramite.paketinseuranta.Constants;



public class ThemeUtils {

    public enum Theme {
        BASIC(1),
        DARK (2);

        private int num;
        Theme(int num) {this.num = num;}

        public static Theme getCurrentTheme(Context context) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            int theme = Integer.valueOf(preferences.getString(Constants.SP_THEME_SELECTION, "1"));
            for (Theme b : Theme.values()) {
                if (b.num == theme) {

                    return b;
                }
            }
            return BASIC;
        }

        public static boolean isDarkTheme(Context context) {
            Theme currentTheme = getCurrentTheme(context);
            return currentTheme == DARK;
        }


    }

}
