/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.utils;

import android.annotation.SuppressLint;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class DateTimeUtils {

    @SuppressLint("SimpleDateFormat")
    public static DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    @SuppressLint("SimpleDateFormat")
    public static DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
}
