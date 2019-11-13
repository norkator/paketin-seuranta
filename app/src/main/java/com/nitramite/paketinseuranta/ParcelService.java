package com.nitramite.paketinseuranta;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.nitramite.courier.ArraPakettiStrategy;
import com.nitramite.courier.CainiaoStrategy;
import com.nitramite.courier.ChinaPostAirMailStrategy;
import com.nitramite.courier.Courier;
import com.nitramite.courier.DHLActiveTrackingStrategy;
import com.nitramite.courier.DHLAmazonStrategy;
import com.nitramite.courier.DHLExpressStrategy;
import com.nitramite.courier.FedExStrategy;
import com.nitramite.courier.GlsStrategy;
import com.nitramite.courier.MatkahuoltoStrategy;
import com.nitramite.courier.ParcelObject;
import com.nitramite.courier.PostNordStrategy;
import com.nitramite.courier.PostiStrategy;
import com.nitramite.courier.UPSStrategy;
import com.nitramite.courier.USPSStrategy;
import com.nitramite.courier.YanwenStrategy;
import com.nitramite.utils.CarrierUtils;
import com.nitramite.utils.LocaleUtils;
import com.nitramite.utils.Utils;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;


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

    private Integer taskNumber = 0;
    Intent broadcastIntent = new Intent("service_broadcast");
    // Intent to send back
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);

    // Arrays
    private ArrayList<ParcelServiceParcelItem> parcelServiceParcelItems = new ArrayList<>();

    // Logging
    private static final String TAG = "ParcelService";


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(localeUtils.updateBaseContextLocale(base));
    }


    @Override
    public void onCreate() {
        //LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(this);
        taskNumber = 0;
    }


    @SuppressWarnings("DanglingJavadoc")
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Log.i(TAG, " _____ _____ _____ _____ _____ _____    _____   ");
        Log.i(TAG, "|     |  _  | __  |_   _|     |   | |  |  |  |  ");
        Log.i(TAG, "| | | |     |    -| | | |-   -| | | |  |    -|_ ");
        Log.i(TAG, "|_|_|_|__|__|__|__| |_| |_____|_|___|  |__|__|_|");

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
        new Thread(new Runnable() {
            @SuppressWarnings("SpellCheckingInspection")
            @Override
            public void run() {
                //Your logic that service will perform will be placed here
                //**********************************************************************************
                parcelServiceParcelItems.clear(); // Clear array before continuing


                try {

                    if (serviceMode == 0) {
                        Cursor cur = databaseHelper.getPackagesDataForParcelService(updateFailedFirst, PARCEL_ID);
                        while (cur.moveToNext()) {
                            parcelServiceParcelItems.add(
                                    new ParcelServiceParcelItem(
                                            cur.getString(0), cur.getString(1), cur.getString(2),
                                            cur.getString(3), cur.getString(4), cur.getString(4),
                                            cur.getString(5), cur.getString(5), cur.getString(6)
                                    )
                            );
                        } // WHILE ENDS
                        taskNumber = cur.getCount();
                        cur.close();

                        Courier courier = new Courier(); // Initialize with posti strategy
                        for (int i = 0; i < taskNumber; i++) {
                            if (i > 1) {
                                Thread.sleep(800); // Sleep little bit
                            }

                            // Get basic information
                            String ID = parcelServiceParcelItems.get(i).parcelDatabaseID;
                            int carrierCode = Integer.parseInt(parcelServiceParcelItems.get(i).getParcelCarrierCode());
                            int carrierStatus = Integer.parseInt(parcelServiceParcelItems.get(i).getParcelCarrierStatusCode());

                            Log.i(TAG, "Run for id: " + ID);

                            // --------------------------------------------------------------------------------------------------------------


                            // Find package from right courier
                            ParcelObject parcelObject = null;
                            switch (carrierCode) {
                                // Posti and china post packages
                                case CarrierUtils.CARRIER_POSTI:
                                case CarrierUtils.CARRIER_CHINA:
                                    courier.setCourierStrategy(new PostiStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    Log.i(TAG, "# Running Posti #");

                                    break;
                                // Matkahuolto
                                case CarrierUtils.CARRIER_MATKAHUOLTO:
                                    Log.i(TAG, "# Running Matkahuolto #");
                                    courier.setCourierStrategy(new MatkahuoltoStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    break;
                                // DHL Express
                                case CarrierUtils.CARRIER_DHL_EXPRESS:
                                    Log.i(TAG, "# Running DHL #");
                                    courier.setCourierStrategy(new DHLExpressStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    break;
                                // UPS
                                case CarrierUtils.CARRIER_UPS:
                                    Log.i(TAG, "# Running UPS #");
                                    courier.setCourierStrategy(new UPSStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    break;
                                // FedEx
                                case CarrierUtils.CARRIER_FEDEX:
                                    Log.i(TAG, "# Running FedEx #");
                                    courier.setCourierStrategy(new FedExStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    break;
                                // PostNord
                                case CarrierUtils.CARRIER_POSTNORD:
                                    Log.i(TAG, "# Running PostNord #");
                                    courier.setCourierStrategy(new PostNordStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    break;
                                // ArraPaketti
                                case CarrierUtils.CARRIER_ARRA_PAKETTI:
                                    Log.i(TAG, "# Running ArraPaketti #");
                                    courier.setCourierStrategy(new ArraPakettiStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    break;
                                // DHL Amazon
                                case CarrierUtils.CARRIER_DHL_AMAZON:
                                    Log.i(TAG, "# Running DHL Amazon #");
                                    courier.setCourierStrategy(new DHLAmazonStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    break;
                                // DHL Active Tracking
                                case CarrierUtils.CARRIER_DHL_ACTIVE_TRACKING:
                                    Log.i(TAG, "# Running DHL Active Tracking #");
                                    courier.setCourierStrategy(new DHLActiveTrackingStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    break;
                                // USPS (United States Postal Service)
                                case CarrierUtils.CARRIER_USPS:
                                    Log.i(TAG, "# Running USPS #");
                                    courier.setCourierStrategy(new USPSStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    break;
                                // Yanwen
                                case CarrierUtils.CARRIER_YANWEN:
                                    Log.i(TAG, "# Running Yanwen #");
                                    courier.setCourierStrategy(new YanwenStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    break;
                                // GLS
                                case CarrierUtils.CARRIER_GLS:
                                    Log.i(TAG, "# Running GLS #");
                                    courier.setCourierStrategy(new GlsStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    break;
                                // Cainiao
                                case CarrierUtils.CARRIER_CAINIAO:
                                    Log.i(TAG, "# Running Cainiao #");
                                    courier.setCourierStrategy(new CainiaoStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    break;
                                // China Post Registered Air Mail
                                case CarrierUtils.CARRIER_CPRAM:
                                    Log.i(TAG, "# Running China Post Registered Air Mail #");
                                    courier.setCourierStrategy(new ChinaPostAirMailStrategy());
                                    parcelObject = courier.executeCourierStrategy(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    break;

                                // Handle default case
                                default: // Other package type
                                    ParcelObject parcelObjectOther = new ParcelObject(parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    parcelObjectOther.setIsFound(false);
                                    parcelObject = parcelObjectOther;
                                    break;
                            }
                            if (parcelObject.getIsFound()) {
                                // Set new phase
                                parcelServiceParcelItems.get(i).setParcelPhaseItemNew(parcelObject.getPhase());
                                databaseHelper.updateData(
                                        ID,
                                        String.valueOf(carrierCode),
                                        String.valueOf(carrierStatus),
                                        parcelServiceParcelItems.get(i).getParcelCodeItem(),
                                        parcelObject,
                                        getString(R.string.last_updated_beginning_part) + " " + Utils.getCurrentTimeStampString() // Last update status
                                );
                                if (parcelObject.getEventObjects() != null) {
                                    for (int e = 0; e < parcelObject.getEventObjects().size(); e++) {
                                        databaseHelper.insertTrackingData(
                                                ID, String.valueOf(carrierStatus),
                                                parcelObject.getEventObjects().get(e).getDescription(),
                                                parcelObject.getEventObjects().get(e).getTimeStamp(),
                                                parcelObject.getEventObjects().get(e).getTimeStampSQLiteFormat(),
                                                parcelObject.getEventObjects().get(e).getLocationCode(),
                                                parcelObject.getEventObjects().get(e).getLocationName()
                                        );
                                    }
                                    // Change phase based on last event (used for ordering properly in the end)
                                    final String eventString = databaseHelper.getLatestParcelEvent(ID); // Get last event string
                                    final PhaseNumberString phaseNumberString = PhaseNumber.phaseToNumber(eventString); // Turn event string into number and corresponding phase string
                                    if (!phaseNumberString.getPhaseString().equals("")) { // Verify
                                        databaseHelper.updatePhaseNumberAndPhaseString(ID, phaseNumberString); // Update parcel details with new phase
                                    }
                                    // Set latest event for comparing (notifications)
                                    parcelServiceParcelItems.get(i).setParcelEventStringNew(eventString);
                                }
                            } else {
                                if (carrierCode == CarrierUtils.CARRIER_OTHER) { // Handle other package type
                                    databaseHelper.updateLastUpdateStatus(
                                            ID, getString(R.string.parcel_service_other_package_type) + " " + Utils.getCurrentTimeStampString() // Last update status for other package type
                                    );
                                } else {
                                    databaseHelper.updateLastUpdateStatus(
                                            ID, getString(R.string.last_updated_beginning_part_not_found) + " " + Utils.getCurrentTimeStampString() // Last update status for not found one
                                    );
                                }
                            }




                            // ---------------------------------------------------------------------
                            // Package update progressbar progress update
                            Intent broadcastIntent = new Intent("service_package_broadcast");
                            LocalBroadcastManager.getInstance(ParcelService.this).sendBroadcast(broadcastIntent);
                            broadcastIntent.putExtra("TASK_NUMBER", i);
                            broadcastIntent.putExtra("TASK_NUMBER_TOTAL", taskNumber);
                            broadcastIntent.putExtra("TASK_PACKAGE_IDENTITY",
                                    (parcelServiceParcelItems.get(i).getParcelNameItem().length() > 0 ? parcelServiceParcelItems.get(i).getParcelNameItem() :
                                            parcelServiceParcelItems.get(i).getParcelCodeItem())
                            );
                        } // End for loop

                        // -------------------------------------------------------------------------

                        // Notification when data changed and notifications allowed
                        if (enableNotifications) {
                            for (int i = 0; i < parcelServiceParcelItems.size(); i++) {
                                try {
                                    Log.i(TAG, "-------------------------------");
                                    Log.i(TAG, "Compare phase result: " + String.valueOf(parcelServiceParcelItems.get(i).getParcelPhaseItemOld().length() != parcelServiceParcelItems.get(i).getParcelPhaseItemNew().length()));
                                    Log.i(TAG, "Compare event result: " + String.valueOf(!parcelServiceParcelItems.get(i).getParcelEventStringOld().equals(parcelServiceParcelItems.get(i).getParcelEventStringNew())));
                                    Log.i(TAG, "Notification would be: " + parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    Log.i(TAG, "..with event string: " + parcelServiceParcelItems.get(i).getParcelEventStringNew());
                                    Log.i(TAG, "-------------------------------");

                                    // Look for difference in states and event strings
                                    if (parcelServiceParcelItems.get(i).getParcelPhaseItemOld().length() != parcelServiceParcelItems.get(i).getParcelPhaseItemNew().length()
                                            || !parcelServiceParcelItems.get(i).getParcelEventStringOld().equals(parcelServiceParcelItems.get(i).getParcelEventStringNew())) {
                                        // Has difference => show notification for this item
                                        showNotification(
                                                parcelServiceParcelItems.get(i).getParcelCodeItem(),
                                                parcelServiceParcelItems.get(i).getParcelEventStringNew()
                                        );
                                    }
                                } catch (NullPointerException ignored) {
                                }
                            }
                        }
                    } // Mode 0

                } catch (IndexOutOfBoundsException e) {
                    Log.i(TAG, e.toString());
                } catch (IllegalStateException e) {
                    Log.i(TAG, e.toString());
                } catch (ConcurrentModificationException e) {
                    Log.i(TAG, e.toString());
                } catch (InterruptedException e) {
                    Log.i(TAG, e.toString());
                } catch (ClassCastException e) {
                    Log.i(TAG, e.toString());
                }

                //********************************************************************************************************************************************
                Log.i(TAG, "Service run fine!");
                databaseHelper.close();
                LocalBroadcastManager.getInstance(ParcelService.this).sendBroadcast(broadcastIntent); // SEND BROADCAST TO ACTIVITIES
                stopSelf();
            } // RUN
        }).start();
        return Service.START_NOT_STICKY; // ANDROID OS WONT BOTHER STARTING SERVICE AGAIN IF SYSTEM RESOURCES RUN OUT
    } // END OF SERVICE onSTART


    // ---------------------------------------------------------------------------------------------


    // Creates notification
    private void showNotification(final String changedParcelCodeItem, final String currentEventText) {
        Intent intent = new Intent(ParcelService.this, MainMenu.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(ParcelService.this, 0, intent, 0);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_HIGH);
            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }
        // Init notification and show it
        NotificationCompat.Builder notification = new NotificationCompat.Builder(ParcelService.this, NOTIFICATION_CHANNEL_ID);
        notification.setSmallIcon(R.drawable.notifsmall);
        notification.setContentTitle("Paketin Seuranta");
        notification.setContentText("Tilassa muutos: " + changedParcelCodeItem + " - " + currentEventText);
        notification.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.logo));
        notification.setContentIntent(pendingIntent);
        notification.setAutoCancel(true);
        assert notificationManager != null;
        notificationManager.notify(1, notification.build());
    }

    // ---------------------------------------------------------------------------------------------

    // New parcel service parcel item for comparing etc
    private class ParcelServiceParcelItem {

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
        ParcelServiceParcelItem(String parcelDatabaseID_, String parcelCarrierCode_, String parcelCarrierStatusCode_,
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

        String getParcelCarrierCode() {
            return parcelCarrierCode;
        }

        String getParcelCarrierStatusCode() {
            return parcelCarrierStatusCode;
        }

        String getParcelCodeItem() {
            return parcelCodeItem;
        }

        String getParcelPhaseItemOld() {
            return parcelPhaseItemOld;
        }

        String getParcelPhaseItemNew() {
            return parcelPhaseItemNew;
        }

        String getParcelEventStringOld() {
            return parcelEventStringOld;
        }

        String getParcelEventStringNew() {
            return parcelEventStringNew;
        }

        String getParcelNameItem() {
            return parcelNameItem;
        }

        // ------------------------------------------
        // Setters

        void setParcelPhaseItemNew(String parcelPhaseItemNew) {
            this.parcelPhaseItemNew = parcelPhaseItemNew;
        }

        void setParcelEventStringNew(String parcelEventStringNew) {
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
