/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.paketinseuranta;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.multidex.MultiDex;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.nitramite.adapters.CustomEventsRecyclerViewAdapter;
import com.nitramite.adapters.ParcelsAdapter;
import com.nitramite.adapters.ParcelsAdapterListener;
import com.nitramite.adapters.RecyclerItemTouchHelper;
import com.nitramite.paketinseuranta.notifier.PushUtils;
import com.nitramite.utils.BackupUtils;
import com.nitramite.utils.LocaleUtils;
import com.nitramite.utils.ThemeUtils;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainMenu extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener,
        PurchasesUpdatedListener, ParcelsAdapterListener, RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    //  Logging
    @NonNls
    private static final String TAG = MainMenu.class.getSimpleName();

    // In app billing
    private BillingClient mBillingClient;

    // Main items
    private final DatabaseHelper databaseHelper = new DatabaseHelper(this);
    private final ArrayList<ParcelItem> parcelItems = new ArrayList<>();

    // Tracking data list views
    private final ArrayList<String> trackingEvents_FIS = new ArrayList<>();
    private final ArrayList<String> trackingEvents_TIMESTAMPS = new ArrayList<>();
    private final ArrayList<String> trackingEvents_LOCATIONCODES = new ArrayList<>();
    private final ArrayList<String> trackingEvents_LOCATIONNAMES = new ArrayList<>();

    // Components
    private final LocaleUtils localeUtils = new LocaleUtils();
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyView = null;
    private RecyclerView recyclerView;
    private ParcelsAdapter adapter = null;
    private SharedPreferences sharedPreferences;
    private Boolean SP_UPDATE_FAILED_FIRST = false;
    private boolean lastUpdate = false;

    // Activity request codes
    private static final int ACTIVITY_RESULT_PARCEL = 1;  // The request code
    private static final int ACTIVITY_RESULT_ARCHIVE = 2;  // The request code
    public static final int ACTIVITY_RESULT_PARCEL_EDITOR = 3;  // The request code


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(localeUtils.updateBaseContextLocale(base));
        MultiDex.install(this);
    }


    // Parcel service finish broad cast receiver
    private final BroadcastReceiver dataChangeReceiver = new BroadcastReceiver() {
        @SuppressWarnings("HardCodedStringLiteral")
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            int serviceMode = 0;
            if (extras != null) {
                serviceMode = extras.getInt("MODE");
            }
            if (serviceMode == 0) {
                swipeRefreshLayout.setRefreshing(false);
                readItems();
            }
        }
    };


    // Package update animation receiver for each service service package update
    private final BroadcastReceiver packageUpdateChangeReceiver = new BroadcastReceiver() {
        @SuppressWarnings("HardCodedStringLiteral")
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                swipeRefreshLayout.setRefreshing(true);
                readItems(); // Refreshes list view
                if (sharedPreferences.getBoolean("SP_PACKAGE_UPDATE_NOTIFY_SNACKBAR", true)) {
                    if (extras.getString("TASK_PACKAGE_IDENTITY") != null) {
                        final Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content), extras.getString("TASK_PACKAGE_IDENTITY") + " " + getString(R.string.main_menu_snackbar_message_updated), Snackbar.LENGTH_SHORT);
                        ((TextView) (snackBar.getView().findViewById(com.google.android.material.R.id.snackbar_text))).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.colorWhite));
                        snackBar.show();
                    }
                }
            }
        }
    };


    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    protected void onStart() {
        super.onStart();
        // Register finish receiver
        IntentFilter intentFilter = new IntentFilter("service_broadcast");
        LocalBroadcastManager.getInstance(this).registerReceiver(dataChangeReceiver, intentFilter);
        // Register update receiver
        IntentFilter packageIntentFilter = new IntentFilter("service_package_broadcast");
        LocalBroadcastManager.getInstance(this).registerReceiver(packageUpdateChangeReceiver, packageIntentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(dataChangeReceiver);
            if (sharedPreferences != null) {
                if (sharedPreferences.getBoolean(Constants.SP_PARCELS_AUTOMATIC_UPDATE, true)) {
                    PushUtils.subscribeToTopic(PushUtils.TOPIC_UPDATE);
                } else
                    PushUtils.unsubscribeFromTopic(PushUtils.TOPIC_UPDATE);

            }
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataChangeReceiver);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set theme
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (ThemeUtils.Theme.isDarkThemeForced(getBaseContext())) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
        } else if (ThemeUtils.Theme.isAutoTheme(getBaseContext())) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);


        // Screen orientation setting
        if (sharedPreferences.getBoolean(Constants.SP_PORTRAIT_ORIENTATION_ONLY, false)) {
            super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }


        // Stop service if running
        stopService(new Intent(MainMenu.this, ParcelService.class));

        // Shared preferences
        boolean SP_UPDATE_PARCELS_ON_STARTUP = sharedPreferences.getBoolean(Constants.SP_UPDATE_PARCELS_ON_STARTUP, true);
        SP_UPDATE_FAILED_FIRST = sharedPreferences.getBoolean(Constants.SP_UPDATE_FAILED_FIRST, false);
        lastUpdate = sharedPreferences.getBoolean(Constants.SP_PACKAGE_LAST_CHANGE, false);
        // If automatic background updating is disabled ensure that service is not running
        boolean SP_PARCELS_AUTOMATIC_UPDATE = sharedPreferences.getBoolean(Constants.SP_PARCELS_AUTOMATIC_UPDATE, true);
        if (SP_PARCELS_AUTOMATIC_UPDATE) {
            PushUtils.subscribeToTopic(PushUtils.TOPIC_UPDATE);
        } else {
            PushUtils.unsubscribeFromTopic(PushUtils.TOPIC_UPDATE);
        }


        // Intro logic, show intro if not shown yet
        if (!sharedPreferences.getBoolean(Constants.SP_INTRO_IS_SHOWN, false)) {
            startActivity(new Intent(MainMenu.this, Intro.class));
        }


        // -----------------------------------------------------------------------------------------

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);
        emptyView = findViewById(R.id.emptyView);
        recyclerView = findViewById(R.id.trackItemsList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // Floating action buttons
        FloatingActionButton floatingAddParcelBtn = findViewById(R.id.floatingAddParcelBtn);
        floatingAddParcelBtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainMenu.this, ParcelEditor.class);
            startActivityForResult(intent, ACTIVITY_RESULT_PARCEL_EDITOR);
        });


        readItems();


        // Start automatic refresh task if enabled from settings
        if (SP_UPDATE_PARCELS_ON_STARTUP) {
            refreshParcelDataTask();
        }

        // Backup feature
        checkForAutomaticBackup();

        // Init in app billing
        initInAppBilling();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }


    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        swipeRefreshLayout.setRefreshing(false);
        if (requestCode == ACTIVITY_RESULT_PARCEL || requestCode == ACTIVITY_RESULT_ARCHIVE || requestCode == ACTIVITY_RESULT_PARCEL_EDITOR) {
            readItems();
        }
        try {
            // If parcel id is provided, update it right away
            final String parcelId = data.getStringExtra("PARCEL_ID");
            if (requestCode == ACTIVITY_RESULT_PARCEL_EDITOR && parcelId != null) {
                // Start service
                swipeRefreshLayout.setRefreshing(true);
                Intent intent = new Intent(this, ParcelService.class);
                intent.putExtra("MODE", 0);
                intent.putExtra("PARCEL_ID", parcelId);
                startService(intent);
            }
        } catch (NullPointerException ignored) {
        }
    }


    // Shows package events dialog
    public void showPackageEventsDialog() {
        if (trackingEvents_FIS.size() > 0) {
            final Dialog dialog = new Dialog(MainMenu.this);
            dialog.setTitle(getString(R.string.content_parcel_parcel_events_title));
            dialog.setContentView(R.layout.custom_events_recycler_view);
            dialog.setCanceledOnTouchOutside(false);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(Objects.requireNonNull(dialog.getWindow()).getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            dialog.show();
            dialog.getWindow().setAttributes(lp);
            CustomEventsRecyclerViewAdapter customEventsRecyclerViewAdapter = new CustomEventsRecyclerViewAdapter(trackingEvents_FIS, trackingEvents_TIMESTAMPS, trackingEvents_LOCATIONCODES, trackingEvents_LOCATIONNAMES);
            RecyclerView customEventsRecyclerView = dialog.findViewById(R.id.customEventsRecyclerView);
            customEventsRecyclerView.setAdapter(customEventsRecyclerViewAdapter);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            customEventsRecyclerView.setLayoutManager(linearLayoutManager);
        } else {
            Toast.makeText(this, R.string.main_menu_did_not_get_event_statuses_try_again_later, Toast.LENGTH_SHORT).show();
        }
    }


    // ---------------------------------------------------------------------------------------------


    @SuppressWarnings("HardCodedStringLiteral")
    public void refreshParcelDataTask() {
        try {
            if (!isMyServiceRunning(ParcelService.class)) {
                // Start service
                Intent intent = new Intent(this, ParcelService.class);
                intent.putExtra("MODE", 0);
                intent.putExtra("NOTIFICATIONS", false); // Normally false
                intent.putExtra("PARCEL_CODE", "");
                intent.putExtra("PARCEL_ID", "");
                intent.putExtra("UPDATE_FAILED_FIRST", SP_UPDATE_FAILED_FIRST);
                intent.putExtra("COURIER_ICONS_ENABLED", true);
                intent.putExtra("START_AS_FOREGROUND_SERVICE", false);
                startService(intent);
                swipeRefreshLayout.setRefreshing(true);
            }
            readItems();
        } catch (RuntimeException ignored) {
        }
    }


    // Get parcel items
    private void readItems() {
        parcelItems.clear();
        Cursor res = databaseHelper.getAllDataWithLatestEvent();
        while (res.moveToNext()) {
            parcelItems.add(new ParcelItem(
                    res.getString(0),
                    res.getString(1),
                    res.getString(2),
                    res.getString(3),
                    res.getString(4),
                    res.getString(5),       // Last update status
                    res.getString(6),       // Latest event description
                    res.getString(7),       // Parcel carrier
                    res.getString(8),
                    res.getString(9),
                    res.getString(10),
                    res.getString(11),
                    res.getString(12),      // Last pickup date
                    res.getString(13),      // Last event date
                    res.getString(14)       // Parcel paid
            ));
        }
        updateListView();
    }


    // ---------------------------------------------------------------------------------------------

    // Update list view
    @SuppressLint("NotifyDataSetChanged")
    public void updateListView() {
        if (parcelItems.size() > 0) {
            emptyView.setVisibility(View.GONE);
            if (adapter == null) {
                adapter = new ParcelsAdapter(MainMenu.this, parcelItems, true, lastUpdate);
                adapter.setClickListeners(this);
                recyclerView.setAdapter(adapter);


                ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(
                        0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, this, RecyclerItemTouchHelper.ActivityTarget.MAIN_MENU
                );
                new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
            } else {
                adapter.notifyDataSetChanged();
            }
        } else {
            emptyView.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onItemClick(View view, int position) {
        try {
            String parcelId = parcelItems.get(position).getParcelId();
            Intent intent = new Intent(MainMenu.this, Parcel.class);
            intent.putExtra("PARCEL_ID", parcelId);
            startActivityForResult(intent, ACTIVITY_RESULT_PARCEL);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, e.toString());
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemLongClick(View view, int position) {
        String[] longTapArrayItems = getResources().getStringArray(R.array.pref_long_tap_values);
        String longTapSelectedAction = sharedPreferences.getString(Constants.SP_MAIN_MENU_LIST_LONG_TAP_ACTION, "");

        assert longTapSelectedAction != null;
        if (longTapSelectedAction.equals(longTapArrayItems[0])) {
            if (isMyServiceRunning(ParcelService.class)) {
                Toast.makeText(MainMenu.this, R.string.main_menu_update_in_progress_try_again_later, Toast.LENGTH_LONG).show();
            } else {
                trackingEvents_FIS.clear();
                trackingEvents_TIMESTAMPS.clear();
                trackingEvents_LOCATIONCODES.clear();
                trackingEvents_LOCATIONNAMES.clear();
                Cursor res = databaseHelper.getAllTrackingData(parcelItems.get(position).getParcelId());
                if (res.getCount() == 0) {
                    //noinspection HardCodedStringLiteral
                    Log.i(TAG, "Got nothing saved on tracking table with parcelID: " + parcelItems.get(position).getParcelCode());
                }
                while (res.moveToNext()) {
                    trackingEvents_FIS.add(res.getString(3));
                    trackingEvents_TIMESTAMPS.add(res.getString(4));
                    trackingEvents_LOCATIONCODES.add(res.getString(6));
                    trackingEvents_LOCATIONNAMES.add(res.getString(7));
                }
                res.close();
                showPackageEventsDialog();
            }
        } else if (longTapSelectedAction.equals(longTapArrayItems[1])) { // Clipboard
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", parcelItems.get(position).getParcelCode());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(MainMenu.this, R.string.main_menu_code_copied_to_your_clipboard, Toast.LENGTH_SHORT).show();
        } else if (longTapSelectedAction.equals("") || longTapSelectedAction.equals(longTapArrayItems[2])) { // Edit information
            Intent intent = new Intent(MainMenu.this, ParcelEditor.class);
            //noinspection HardCodedStringLiteral
            intent.putExtra("PARCEL_ID", parcelItems.get(position).getParcelId());
            startActivityForResult(intent, ACTIVITY_RESULT_PARCEL_EDITOR);
        } else {
            Toast.makeText(MainMenu.this, R.string.main_menu_long_click_function_selection_not_made_on_settings, Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        switch (direction) {
            case 4:
                directionLeftDelete(position);
                break;
            case 8:
                directionRightArchive(position);
                break;
        }
    }


    // Swipe from right to left deletes item
    private void directionLeftDelete(int position) {
        if (isMyServiceRunning(ParcelService.class)) {
            Toast.makeText(MainMenu.this, R.string.main_menu_cannot_delete_updating_packages_data, Toast.LENGTH_LONG).show();
        } else {
            deleteItemConfirmationDialog(position);
        }
    }


    // Swipe from left to right deletes item
    private void directionRightArchive(int position) {
        String codeId = parcelItems.get(position).getParcelId();
        databaseHelper.updateArchived(codeId, true);
        readItems();
    }


    private void deleteItemConfirmationDialog(int position) {
        new AlertDialog.Builder(MainMenu.this)
                .setTitle(R.string.main_menu_deletion_title)
                .setMessage(R.string.main_menu_you_are_about_to_delete_following_package_from_list)
                .setPositiveButton(R.string.yes_btn, (dialog, which) -> {
                    String codeId = parcelItems.get(position).getParcelId();
                    databaseHelper.deletePackageData(codeId);
                    readItems();
                })
                .setNegativeButton(R.string.no_btn, (dialog, which) -> {
                    // Return
                })
                .setIcon(R.mipmap.ps_logo_round)
                .show();
    }


    // Check if service is running
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }


    // About dialog
    private void aboutDialog() {
        String appVersionCode = "-";
        String appVersionName = "-";
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            appVersionCode = String.valueOf(pInfo.versionCode);
            appVersionName = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        final String versionDetails =
                getString(R.string.main_menu_version_details) + "\n" +
                        getString(R.string.main_menu_version_code) + " " + appVersionCode + "\n" +
                        getString(R.string.main_menu_version_name) + " " + appVersionName;


        AboutDialog.Builder aboutDialog = new AboutDialog.Builder(MainMenu.this)
                // .setImageRecourse(R.mipmap.ps_logo_round)
                .setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ps_logo_round_big))
                .setTextTitle(R.string.main_menu_about_title)
                .setTitleColor(ThemeUtils.Theme.isDarkTheme(getBaseContext()) ? R.color.color_white : R.color.black)
                .setSubtitleColor(ThemeUtils.Theme.isDarkTheme(getBaseContext()) ? R.color.color_white : R.color.black)
                .setTextSubTitle(R.string.main_menu_about_sub_title)
                .setBody(getString(R.string.main_menu_about_body) + " " +
                        getString(R.string.main_menu_body_part_two) +
                        "\n\n" + versionDetails
                )
                // .setNegativeColor(R.color.colorPrimary)
                .setNegativeButtonText(R.string.main_menu_close)
                .setOnNegativeClicked((view, dialog) -> dialog.dismiss())
                .setUserManualButtonText(R.string.menu_menu_user_manual)
                .setOnUserManualClicked((view, dialog) -> {
                    //noinspection HardCodedStringLiteral
                    String url = "http://www.nitramite.com/paketin-seuranta.html";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                })
                /* .setAutoHide(true)*/
                .build();
        aboutDialog.show();
    }


    // ---------------------------------------------------------------------------------------------

    // Refresh Parcel's data
    private void refreshParcels() {
        if (isMyServiceRunning(ParcelService.class)) {
            Toast.makeText(MainMenu.this, R.string.main_menu_update_in_progress_try_again_later, Toast.LENGTH_LONG).show();
            swipeRefreshLayout.setRefreshing(false);
        } else {
            swipeRefreshLayout.setRefreshing(true);
            Cursor res = databaseHelper.getAllData();
            int TaskCount = res.getCount();
            if (TaskCount == 0) {
                Toast.makeText(MainMenu.this, R.string.main_menu_list_is_empty_nothing_to_update, Toast.LENGTH_LONG).show();
                swipeRefreshLayout.setRefreshing(false);
            } else {
                final Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content), R.string.main_menu_updating, Snackbar.LENGTH_LONG);
                ((TextView) (snackBar.getView().findViewById(com.google.android.material.R.id.snackbar_text))).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.colorWhite));
                snackBar.show();
                refreshParcelDataTask();
            }
        }
    }


    @Override
    public void onRefresh() {
        refreshParcels();
    }

    // MENU ITEMS
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_barcode_list_icon) {
            if (parcelItems.size() > 0) {
                startActivity(new Intent(MainMenu.this, BarcodeList.class));
            } else {
                Toast.makeText(this, R.string.main_menu_your_list_is_empty, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (id == R.id.action_archive_icon) {
            startActivityForResult(new Intent(MainMenu.this, Archive.class), ACTIVITY_RESULT_ARCHIVE);
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(MainMenu.this, MyPreferencesActivity.class));
            finish();
            return true;
        }
        if (id == R.id.action_about) {
            aboutDialog();
            return true;
        }
        if (id == R.id.action_database_dump) {
            startActivity(new Intent(MainMenu.this, BackupManager.class));
            return true;
        }
        if (id == R.id.action_support_development) {
            inAppPurchase(Constants.ITEM_SKU_DONATE);
            return true;
        }
        if (id == R.id.action_join_development) {
            openUrl("https://github.com/norkator/paketin-seuranta");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // Helper to request wanted permission
    private boolean hasPermission(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    requestPermission(permission);
                    return false;
                }
            }
        }
        return true;
    }


    public void requestPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(MainMenu.this, permission) == PackageManager.PERMISSION_GRANTED) {
            } else {
                requestPermissions(new String[]{permission}, 1);
            }
        }
    }


    @SuppressWarnings("HardCodedStringLiteral")
    private void checkForAutomaticBackup() {
        if (sharedPreferences.getBoolean(Constants.SP_TIMED_BACKUP_ENABLED, false)) {
            final String lastBackupDate = sharedPreferences.getString(Constants.SP_TIMED_BACKUP_LAST_DATE, null);
            Calendar calendar = Calendar.getInstance();
            if (lastBackupDate == null) {
                BackupUtils.BackupDatabase(this);
                BackupUtils.SaveBackupDate(this, calendar);
            } else {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                    calendar.setTime(Objects.requireNonNull(sdf.parse(lastBackupDate)));
                    calendar.add(Calendar.DATE, 5);
                    if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                        BackupUtils.BackupDatabase(this);
                        BackupUtils.SaveBackupDate(this, calendar);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void openUrl(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }


    // ---------------------------------------------------------------------------------------------
    // Helpers

    // Generic use error dialog
    private void genericErrorDialog(final String title, final String description) {
        if (!this.isFinishing()) {
            new AlertDialog.Builder(MainMenu.this)
                    .setTitle(title)
                    .setMessage(description)
                    .setPositiveButton(R.string.main_menu_close, (dialog, which) -> {
                    })
                    .setIcon(R.mipmap.ps_logo_round)
                    .show();
        }
    }


    // ---------------------------------------------------------------------------------------------
    // In app billing

    // Initialize in app billing feature
    private void initInAppBilling() {
        // In app billing
        mBillingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NotNull BillingResult billingResult) {
            }

            @Override
            public void onBillingServiceDisconnected() {
            }
        });
    }


    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                for (String sku : purchase.getSkus()) {
                    if (sku.equals(Constants.ITEM_SKU_DONATE)) {
                        Toast.makeText(MainMenu.this, R.string.thank_you_for_donation, Toast.LENGTH_LONG).show();
                        acknowledgePurchase(purchase);
                    }
                }
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED && purchases != null) {
            for (Purchase purchase : purchases) {
                if (!purchase.isAcknowledged()) {
                    acknowledgePurchase(purchase);
                }
            }
        }
    }


    /**
     * Acknowledge purchase required by billing lib 2.x++
     *
     * @param purchase billing purchase
     */
    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams acknowledgePurchaseParams =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
        mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
    }


    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = billingResult -> {
        // This is required, will otherwise return money back in day or two
        Toast.makeText(MainMenu.this, R.string.purchase_acknowledged, Toast.LENGTH_SHORT).show();
    };


    public void inAppPurchase(final String IAP_ITEM_SKU) {
        if (mBillingClient.isReady()) {
            List<String> skuList = new ArrayList<>();
            skuList.add(IAP_ITEM_SKU);
            SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder()
                    .setSkusList(skuList).setType(BillingClient.SkuType.INAPP).build();
            mBillingClient.querySkuDetailsAsync(skuDetailsParams, (billingResult, skuDetailsList) -> {
                try {
                    assert skuDetailsList != null;
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetailsList.get(0))
                            .build();
                    mBillingClient.launchBillingFlow(MainMenu.this, flowParams);
                } catch (IndexOutOfBoundsException e) {
                    genericErrorDialog(getString(R.string.main_menu_error), e.toString());
                }
            });
        } else {
            genericErrorDialog(getString(R.string.billing_service), getString(R.string.billing_service_not_initialized_error));
            initInAppBilling();
        }
    }

} 