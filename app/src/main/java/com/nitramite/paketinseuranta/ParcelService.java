/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.paketinseuranta;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.nitramite.paketinseuranta.updater.UpdaterLogic;
import com.nitramite.utils.LocaleUtils;


/**
 * Created by Martin on 25.1.2016.
 * This is Parcel data update controller.
 * Works with modes
 */
public class ParcelService extends Service {

    // Main variables
    private LocaleUtils localeUtils = new LocaleUtils();
    private Integer serviceMode = 999;
    private Boolean enableNotifications = false;
    private String PARCEL_ID = ""; // Used to update only one package
    private Boolean updateFailedFirst = false;
    private Boolean startAsForegroundService = false;
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);


    // Logging
    private static final String TAG = "ParcelService";


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(localeUtils.updateBaseContextLocale(base));
    }

    /**
     * Small notice: If you do any modifications, please make sure you apply them also to the "PushService" file.
     * @param intent Intent
     * @param flags Flags
     * @param startId StartId
     * @return Result
     */

    @SuppressWarnings("DanglingJavadoc")
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        /**
         * MODE -->
         * 0 = normal update with notifications false or true
         * 1 = events update --> PARCEL_ID = id to check updates for
         */
        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.i(TAG, "! No extras, service quitting !");
        } else {
            serviceMode = extras.getInt("MODE");
            enableNotifications = extras.getBoolean("NOTIFICATIONS");
            PARCEL_ID = extras.getString("PARCEL_ID");
            updateFailedFirst = extras.getBoolean("UPDATE_FAILED_FIRST");
            startAsForegroundService = extras.getBoolean("START_AS_FOREGROUND_SERVICE");
        }


        // https://stackoverflow.com/questions/46445265/android-8-0-java-lang-illegalstateexception-not-allowed-to-start-service-inten
        if (startAsForegroundService) {
            startServiceAsForeground();
        }


        // PARCEL UPDATE THREAD
        UpdaterLogic.getUpdaterThread(this, localeUtils, enableNotifications, updateFailedFirst, serviceMode, PARCEL_ID, databaseHelper).start();

        return Service.START_NOT_STICKY; // ANDROID OS WONT BOTHER STARTING SERVICE AGAIN IF SYSTEM RESOURCES RUN OUT
    } // END OF SERVICE onSTART

    // New parcel service parcel item for comparing etc
    public static class ParcelServiceParcelItem {

        // Variables
        private String parcelDatabaseID;
        private String parcelCarrierCode;
        private String parcelCarrierStatusCode;
        private String parcelCodeItem;
        private String parcelNameItem;
        private String parcelPhaseItemOld;
        private String parcelPhaseItemNew;
        private String parcelEventStringOld;
        private String parcelEventStringNew;

        // Constructor
        public ParcelServiceParcelItem(String parcelDatabaseID_, String parcelCarrierCode_, String parcelCarrierStatusCode_,
                                String parcelCodeItem_, String parcelPhaseItemOld_, String parcelPhaseItemNew_,
                                String parcelEventStringOld_, String parcelEventStringNew_,
                                String parcelNameItem_) {
            parcelDatabaseID = parcelDatabaseID_;
            parcelCarrierCode = parcelCarrierCode_;
            parcelCarrierStatusCode = parcelCarrierStatusCode_;
            parcelCodeItem = parcelCodeItem_;
            parcelPhaseItemOld = parcelPhaseItemOld_;
            parcelPhaseItemNew = parcelPhaseItemNew_;
            parcelEventStringOld = parcelEventStringOld_;
            parcelEventStringNew = parcelEventStringNew_;
            parcelNameItem = parcelNameItem_;
        }

        // ------------------------------------------
        // Getters

        public String getParcelDatabaseID() {
            return parcelDatabaseID;
        }

        public String getParcelCarrierCode() {
            return parcelCarrierCode;
        }

        public String getParcelCarrierStatusCode() {
            return parcelCarrierStatusCode;
        }

        public String getParcelCodeItem() {
            return parcelCodeItem;
        }

        public String getParcelPhaseItemOld() {
            return parcelPhaseItemOld;
        }

        public String getParcelPhaseItemNew() {
            return parcelPhaseItemNew;
        }

        public String getParcelEventStringOld() {
            return parcelEventStringOld;
        }

        public String getParcelEventStringNew() {
            return parcelEventStringNew;
        }

        public String getParcelNameItem() {
            return parcelNameItem;
        }

        // ------------------------------------------
        // Setters

        public void setParcelPhaseItemNew(String parcelPhaseItemNew) {
            this.parcelPhaseItemNew = parcelPhaseItemNew;
        }

        public void setParcelEventStringNew(String parcelEventStringNew) {
            this.parcelEventStringNew = parcelEventStringNew;
        }

    } // End of class


    // ---------------------------------------------------------------------------------------------


    /**
     * Service foreground problem solutions for android O and up
     * https://stackoverflow.com/questions/47531742/startforeground-fail-after-upgrade-to-android-8-1
     */
    private void startServiceAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";
            String channelName = "ParcelService";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.notifsmall)
                    .setContentTitle("Paketin Seuranta updating parcel statuses")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(2, notification);
        }
    }



    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


    @Override
    public void onDestroy() {
    }

} // End of class
