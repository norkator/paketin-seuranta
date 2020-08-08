/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.paketinseuranta.notifier;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nitramite.paketinseuranta.Constants;
import com.nitramite.paketinseuranta.DatabaseHelper;
import com.nitramite.paketinseuranta.updater.UpdaterLogic;
import com.nitramite.utils.LocaleUtils;

import org.joda.time.Duration;
import org.joda.time.Interval;

import java.util.Date;
import java.util.Objects;

@SuppressWarnings("HardCodedStringLiteral")
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
public class PushService extends FirebaseMessagingService {

    private String TAG = this.getClass().getSimpleName();

    private LocaleUtils localeUtils = new LocaleUtils();
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);

    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // Triggering the ParcelService to run
        Log.e(TAG, "onMessageReceived " + remoteMessage.getFrom());
        if (Objects.equals(remoteMessage.getFrom(), PushUtils.TOPIC_UPDATE)) {
            try {
                Log.e(TAG, "Push Update triggered");
                if (permittedToUpdate()) {
                    Log.e(TAG, "Update permitted");
                    startCheck();
                }
            } catch (Exception e) {
                Log.i(TAG, e.toString());
            }
        }
        super.onMessageReceived(remoteMessage);
    }

    /**
     * Checking if user-defined interval is over. If it is, returning true and saving current time
     *
     * @return boolean Is the interval over?
     */
    private boolean permittedToUpdate() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        double parcelInterval = Double.parseDouble(sharedPreferences.getString(Constants.SP_UPDATE_INTERVAL, "1.0").replace(",", "."));
        Date currentDateObject = new Date();
        Date lastUpdate = new Date(sharedPreferences.getLong(Constants.SP_LAST_PUSH_UPDATE, currentDateObject.getTime()));
        Duration period = new Interval(lastUpdate.getTime(), currentDateObject.getTime()).toDuration();
        long intervalAsMinutes = (long) (parcelInterval * 60);
        boolean permitted = period.getStandardMinutes() >= intervalAsMinutes;
        if (permitted)
            sharedPreferences.edit().putLong(Constants.SP_LAST_PUSH_UPDATE, currentDateObject.getTime()).apply();
        return permitted;
    }

    /**
     * Doing so we defend the Google's Doze, because it doesn't allow anyone to start a service here
     */
    public void startCheck() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean notifications = sharedPreferences.getBoolean(Constants.SP_PARCEL_UPDATE_NOTIFICATIONS, false);
        UpdaterLogic.getUpdaterThread(this, localeUtils, notifications, false, 0, "", databaseHelper).start();
    }


}
