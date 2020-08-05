/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.paketinseuranta.updater;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
import com.nitramite.paketinseuranta.DatabaseHelper;
import com.nitramite.paketinseuranta.MainMenu;
import com.nitramite.paketinseuranta.ParcelService;
import com.nitramite.paketinseuranta.PhaseNumber;
import com.nitramite.paketinseuranta.PhaseNumberString;
import com.nitramite.paketinseuranta.R;
import com.nitramite.utils.CarrierUtils;
import com.nitramite.utils.LocaleUtils;
import com.nitramite.utils.Utils;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class UpdaterLogic {

    private static String TAG = "UpdaterLogic";

    public static Thread getUpdaterThread(Context context, LocaleUtils localeUtils, boolean enableNotifications, boolean updateFailedFirst, int serviceMode, String PARCEL_ID, DatabaseHelper databaseHelper) {
        List<ParcelService.ParcelServiceParcelItem> parcelServiceParcelItems = new ArrayList<>();
        return new Thread(new Runnable() {
            @SuppressWarnings("SpellCheckingInspection")
            @Override
            public void run() {
                int taskNumber = 0;

                //Your logic that service will perform will be placed here
                //**********************************************************************************
                parcelServiceParcelItems.clear(); // Clear array before continuing


                try {
                    if (serviceMode == 0) {
                        Cursor cur = databaseHelper.getPackagesDataForParcelService(updateFailedFirst, PARCEL_ID);
                        while (cur.moveToNext()) {
                            parcelServiceParcelItems.add(
                                    new ParcelService.ParcelServiceParcelItem(
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
                            String ID = parcelServiceParcelItems.get(i).getParcelDatabaseID();
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
                                        context.getString(R.string.last_updated_beginning_part) + " " + Utils.getCurrentTimeStampString() // Last update status
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
                                            ID, context.getString(R.string.parcel_service_other_package_type) + " " + Utils.getCurrentTimeStampString() // Last update status for other package type
                                    );
                                } else {
                                    databaseHelper.updateLastUpdateStatus(
                                            ID, context.getString(R.string.last_updated_beginning_part_not_found) + " " + Utils.getCurrentTimeStampString() // Last update status for not found one
                                    );
                                }
                            }


                            // ---------------------------------------------------------------------
                            // Package update progressbar progress update
                            Intent broadcastIntent = new Intent("service_package_broadcast");
                            LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
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
                                    Log.i(TAG, "Compare phase result: " + (parcelServiceParcelItems.get(i).getParcelPhaseItemOld().length() != parcelServiceParcelItems.get(i).getParcelPhaseItemNew().length()));
                                    Log.i(TAG, "Compare event result: " + !parcelServiceParcelItems.get(i).getParcelEventStringOld().equals(parcelServiceParcelItems.get(i).getParcelEventStringNew()));
                                    Log.i(TAG, "Notification would be: " + parcelServiceParcelItems.get(i).getParcelCodeItem());
                                    Log.i(TAG, "..with event string: " + parcelServiceParcelItems.get(i).getParcelEventStringNew());
                                    Log.i(TAG, "-------------------------------");

                                    // Look for difference in states and event strings
                                    if (parcelServiceParcelItems.get(i).getParcelPhaseItemOld().length() != parcelServiceParcelItems.get(i).getParcelPhaseItemNew().length()
                                            || !parcelServiceParcelItems.get(i).getParcelEventStringOld().equals(parcelServiceParcelItems.get(i).getParcelEventStringNew())) {
                                        // Has difference => show notification for this item
                                        showNotification(
                                                context,
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
                Intent broadcastIntent = new Intent("service_broadcast");
                LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent); // SEND BROADCAST TO ACTIVITIES
            } // RUN
        });
    }

    // ---------------------------------------------------------------------------------------------


    // Creates notification
    private static void showNotification(Context context, final String changedParcelCodeItem, final String currentEventText) {
        Intent intent = new Intent(context, MainMenu.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        notification.setSmallIcon(R.drawable.notifsmall);
        notification.setContentTitle(context.getString(R.string.app_name));
        notification.setContentText(context.getString(R.string.notification_message, changedParcelCodeItem, currentEventText));
        notification.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.logo));
        notification.setContentIntent(pendingIntent);
        notification.setAutoCancel(true);
        assert notificationManager != null;
        notificationManager.notify(1, notification.build());
    }

    // ---------------------------------------------------------------------------------------------

}
