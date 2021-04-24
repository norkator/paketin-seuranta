package com.nitramite.utils;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("HardCodedStringLiteral")
public class Utils {

    //  Logging
    private static final String TAG = Utils.class.getSimpleName();


    // Get current datetime string
    public static String getCurrentTimeStampString() {
        try {
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            return dateFormat.format(new Date());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    // Get current datetime string as sqlite suitable format
    public static String getCurrentTimeStampSqliteString() {
        try {
            @SuppressLint("SimpleDateFormat")
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.format(new Date());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    // Convert's dp value to pixels (DisplayMetrics metrics = getResources().getDisplayMetrics())
    public static int dpToPixels(int dp, DisplayMetrics metrics) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }


    // Add's basically three hours to given date
    public static Date postiOffsetDateHours(Date currentDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        TimeZone mTimeZone = calendar.getTimeZone();
        int mGMTOffset = mTimeZone.getRawOffset();
        int timeOffset = (int) TimeUnit.HOURS.convert(mGMTOffset, TimeUnit.MILLISECONDS);
        calendar.add(Calendar.HOUR, timeOffset);
        return calendar.getTime();
    }


    // Month string to corresponding number
    public static String monthStringToMonthNumber(String dateString) {
        if (dateString.contains("tammikuu") || dateString.contains("January")) {
            return "01";
        } else if (dateString.contains("helmikuu") || dateString.contains("February")) {
            return "02";
        } else if (dateString.contains("maaliskuu") || dateString.contains("March")) {
            return "03";
        } else if (dateString.contains("huhtikuu") || dateString.contains("April")) {
            return "04";
        } else if (dateString.contains("toukokuu") || dateString.contains("May")) {
            return "05";
        } else if (dateString.contains("kesäkuu") || dateString.contains("June")) {
            return "06";
        } else if (dateString.contains("heinäkuu") || dateString.contains("July")) {
            return "07";
        } else if (dateString.contains("elokuu") || dateString.contains("August")) {
            return "08";
        } else if (dateString.contains("syyskuu") || dateString.contains("September")) {
            return "09";
        } else if (dateString.contains("lokakuu") || dateString.contains("October")) {
            return "10";
        } else if (dateString.contains("marraskuu") || dateString.contains("November")) {
            return "11";
        } else if (dateString.contains("joulukuu") || dateString.contains("December")) {
            return "12";
        }
        return "0";
    }


    // Resize bitmap without cropping
    @SuppressWarnings("SameParameterValue")
    public static Bitmap resizeBitmap(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }

} // End of class