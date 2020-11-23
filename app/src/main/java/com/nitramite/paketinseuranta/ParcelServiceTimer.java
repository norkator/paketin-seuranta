package com.nitramite.paketinseuranta;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.preference.PreferenceManager;

import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("HardCodedStringLiteral")
public class ParcelServiceTimer extends Service {

    // Logging
    private static final String TAG = "ParcelServiceTimer";

    // Default update interval
    public long NOTIFY_INTERVAL = 60 * 60000; // 60 minutes

    // Variables
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Timer mTimer = null;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        long hours = Long.parseLong(sharedPreferences.getString("parcels_update_rate", "1"));
        NOTIFY_INTERVAL = hours * (60 * 60000);


        // cancel if already existed
        if (mTimer != null) {
            mTimer.cancel();
        } else {
            // recreate new
            mTimer = new Timer();
        }

        Log.i(TAG, "Timer service initialization");

        // schedule task
        try {
            mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(), 0, NOTIFY_INTERVAL);
        } catch (IllegalArgumentException ignored) {
        }
    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }


    @SuppressWarnings("HardCodedStringLiteral")
    class TimeDisplayTimerTask extends TimerTask {

        @Override
        public void run() {
            // run on another thread
            mHandler.post(() -> {
                Log.i(TAG, "Running timer post handler");

                // display toast
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                boolean developerServiceToasts = sharedPreferences.getBoolean("developer_service_toasts", false);
                if (developerServiceToasts) {
                    Toast.makeText(getApplicationContext(), getDateTime(), Toast.LENGTH_SHORT).show();
                } // END OF IF DEV TOASTS
                try {
                    // START PARCEL SERVICE
                    Intent intent = new Intent(ParcelServiceTimer.this, ParcelService.class);
                    intent.putExtra("MODE", 0);
                    intent.putExtra("NOTIFICATIONS", sharedPreferences.getBoolean(Constants.SP_PARCEL_UPDATE_NOTIFICATIONS, false));
                    intent.putExtra("PARCEL_CODE", "");
                    intent.putExtra("PARCEL_ID", "");
                    intent.putExtra("UPDATE_FAILED_FIRST", false);
                    intent.putExtra("COURIER_ICONS_ENABLED", false);
                    intent.putExtra("START_AS_FOREGROUND_SERVICE", true);

                    // Handling service starting in right way
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }

                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }
            });
        }

        // GET DATE TOAST
        private String getDateTime() {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("[dd/MM/yyyy - HH:mm:ss]");
            return sdf.format(new Date());
        }
    }


    @Override
    public void onDestroy() {
        Log.i(TAG, "TimerService killed");
        stopSelf();
    }


} // End of class