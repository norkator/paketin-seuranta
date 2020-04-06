/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.paketinseuranta.notifier;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nitramite.paketinseuranta.Constants;
import com.nitramite.paketinseuranta.DatabaseHelper;
import com.nitramite.paketinseuranta.ParcelService;
import com.nitramite.paketinseuranta.updater.UpdaterLogic;
import com.nitramite.utils.LocaleUtils;

import java.util.ArrayList;

public class PushService extends FirebaseMessagingService {

    private String TAG = this.getClass().getSimpleName();

    private LocaleUtils localeUtils = new LocaleUtils();
    private Integer serviceMode = 999;
    private Boolean enableNotifications = false;
    private String PARCEL_ID = ""; // Used to update only one package
    private Boolean updateFailedFirst = false;
    private Boolean startAsForegroundService = false;

    private Integer taskNumber = 0;
    // Intent to send back
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);

    // Arrays
    private ArrayList<ParcelService.ParcelServiceParcelItem> parcelServiceParcelItems = new ArrayList<>();

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // Triggering the ParcelService to run
        if (remoteMessage.getFrom().equals(PushUtils.TOPIC_UPDATE)) {
            try {
                startCheck();
                Log.i(TAG, "PushService update triggered");
            } catch (IllegalStateException e) {
                Log.i(TAG, e.toString());
            } catch (Exception e) {
                Log.i(TAG, e.toString());
            }
        }
        super.onMessageReceived(remoteMessage);
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
