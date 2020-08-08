/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.paketinseuranta;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.multidex.MultiDex;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.nitramite.utils.CarrierUtils;
import com.nitramite.utils.LocaleUtils;
import com.nitramite.utils.ThemeUtils;
import com.nitramite.utils.Utils;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import org.jetbrains.annotations.NonNls;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
public class Parcel extends AppCompatActivity implements OnMapReadyCallback, SwipeRefreshLayout.OnRefreshListener, CarrierDetectorTaskInterface {

    // Logging
    @NonNls
    private static final String TAG = "Parcel";

    // Activity request codes
    private static final int ACTIVITY_RESULT_PARCEL_EDITOR = 3;  // The request code

    // Components
    private LocaleUtils localeUtils = new LocaleUtils();
    private File parcelImageDirectory;
    private static final int CAMERA_REQUEST = 1888;
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);
    private TextView trackingNumberText;
    private ImageView code128Output;
    private LinearLayout parcelInfoOutput;
    private double latitude = 0, longitude = 0;
    private String ID;
    private String barcode_data;
    private String lockerCode = "";
    private ArrayList<String> parcelDataArray = new ArrayList<>();
    private LinearLayout lockerCodeView;
    private TextView lockerCodeTV;
    private LinearLayout parcelEventsView;
    private TextView noEventsTV;
    private LinearLayout deliveredView, estimateDeliveryOrLastPickupDateView;
    private TextView deliveredTV, estimateDeliveryOrLastPickupDateTV;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Spinner selectCarrierSpinner;
    private LinearLayout parcelImagesLayout;
    private SharedPreferences sharedPreferences;
    private LinearLayout detectedLayout; // On carrier detector dialog
    private ProgressBar detectCarrierProgressBar; // On carrier detector dialog
    private ScrollView contentLayout; // Swipe left right change package layout
    private LayoutInflater layoutInflater;
    private CarrierDetectorTask carrierDetectorTask;
    private Button tryDetectCourierBtn;

    // Dialogs
    private Dialog carrierDetectorDialog;

    // Animations
    private AlphaAnimation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);

    // Vibration
    private Vibrator vibrator;
    private final int vibTime = 50;

    @Override
    public void onRefresh() {
        refreshParcels();
    }

    // Parcel service finish broad cast receiver
    private BroadcastReceiver dataChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            swipeRefreshLayout.setRefreshing(false);
            readParcelDataFromSqlite();
        }
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(localeUtils.updateBaseContextLocale(base));
        MultiDex.install(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    protected void onStart() {
        super.onStart();
        // Register finish receiver
        IntentFilter intentFilter = new IntentFilter("service_broadcast");
        LocalBroadcastManager.getInstance(this).registerReceiver(dataChangeReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataChangeReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataChangeReceiver);
    }

    @SuppressWarnings("HardCodedStringLiteral")
    @SuppressLint("ClickableViewAccessibility")
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
        setContentView(R.layout.activity_parcel);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.parcel_title);

        // Screen orientation setting
        if (sharedPreferences.getBoolean(Constants.SP_PORTRAIT_ORIENTATION_ONLY, false)) {
            super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }


        // Activity results set
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);

        // Get vibrator
        vibrator = (Vibrator) Parcel.this.getSystemService(Context.VIBRATOR_SERVICE);

        // Find views
        trackingNumberText = findViewById(R.id.trackingnumberText);
        code128Output = findViewById(R.id.code128Output);
        parcelInfoOutput = findViewById(R.id.parcelInfoOutput);
        lockerCodeView = findViewById(R.id.lockerCodeView);
        lockerCodeTV = findViewById(R.id.lockerCodeTV);
        lockerCodeView.setVisibility(View.GONE);
        parcelEventsView = findViewById(R.id.parcelEventsView);
        noEventsTV = findViewById(R.id.noEventsTV);
        deliveredView = findViewById(R.id.deliveredView);
        deliveredView.setVisibility(View.GONE);
        deliveredTV = findViewById(R.id.deliveredTV);
        estimateDeliveryOrLastPickupDateView = findViewById(R.id.estimateDeliveryOrLastPickupDateView);
        estimateDeliveryOrLastPickupDateView.setVisibility(View.GONE);
        estimateDeliveryOrLastPickupDateTV = findViewById(R.id.estimateDeliveryOrLastPickupDateTV);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);
        contentLayout = findViewById(R.id.contentLayout);
        selectCarrierSpinner = findViewById(R.id.selectCarrierSpinner);
        parcelImagesLayout = findViewById(R.id.parcelImagesLayout);
        tryDetectCourierBtn = findViewById(R.id.tryDetectCourierBtn);

        // Init layout inflater for additional details
        layoutInflater = LayoutInflater.from(Parcel.this);


        // Get db id of parcel
        Intent intent = getIntent();
        ID = intent.getStringExtra("PARCEL_ID"); //NON-NLS

        // Deactivate swipe to refresh if archived package
        if (databaseHelper.isCurrentPackageArchived(ID)) {
            // swipeRefreshLayout.setOnRefreshListener(null); // enable if lower one not working
            swipeRefreshLayout.setEnabled(false);
        }

        FloatingActionButton fab = findViewById(R.id.fab);

        final Dialog dialog = new Dialog(Parcel.this); // CALL DIALOG ON onCreate BUT NOT IN BUTTON ON CLICK
        dialog.setTitle(R.string.parcel_pick_up_destination_on_map_title);
        dialog.setContentView(R.layout.activity_maps);
        dialog.setCanceledOnTouchOutside(false);

        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        fab.setOnClickListener(view -> {
            if (latitude == 0) {
                Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content), R.string.parcel_no_pick_up_destination_coordinates, Snackbar.LENGTH_SHORT);
                ((TextView) (snackBar.getView().findViewById(com.google.android.material.R.id.snackbar_text))).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.colorWhite));
                snackBar.show();
            } else {
                // ON MAP
                dialog.show();
                dialog.getWindow().setAttributes(lp);
                // Setup map
                SupportMapFragment mapFragment = (SupportMapFragment) Parcel.this.getSupportFragmentManager().findFragmentById(R.id.map);
                mapFragment.getMapAsync(Parcel.this);
            }
        });

        // Try courier detector btn
        tryDetectCourierBtn.setOnClickListener(view -> {
            carrierDetectorDialog(true);
        });

        if (sharedPreferences.getBoolean(Constants.SP_PARCEL_SWIPE_CHANGE, true)) {
            contentLayout.setOnTouchListener(new OnSwipeTouchListener(Parcel.this) {
                @Override
                public void onSwipeRight() {
                    swipeSwitchPackage(false);
                }

                @Override
                public void onSwipeLeft() {
                    swipeSwitchPackage(true);
                }
            });
        }


        readParcelDataFromSqlite();
        createParcelImageDirectory();
    } // END OF onCreate()


    // API 19 KITKAT STATUS BAR TINTING
    @TargetApi(19)
    private void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }


    // Swipe action switch package
    private void swipeSwitchPackage(final boolean nextPackage) {
        ArrayList<ParcelItem> parcelItems = new ArrayList<>();
        Cursor res = null;
        Boolean wasArchived = false;
        if (databaseHelper.isCurrentPackageArchived(ID)) {
            res = databaseHelper.getAllArchiveDataWithLatestEvent(null); // Get packages from archived ones
            wasArchived = true;
        } else {
            res = databaseHelper.getAllDataWithLatestEvent(); // Get packages from normal ones
            wasArchived = false;
        }
        while (res.moveToNext()) {
            parcelItems.add(new ParcelItem(
                    res.getString(0),
                    res.getString(1),
                    res.getString(2),
                    res.getString(3),
                    res.getString(4),
                    res.getString(5),   // Last update status
                    res.getString(6),   // Latest event description
                    res.getString(7),   // Carrier
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }
        for (int i = 0; i < parcelItems.size(); i++) {
            if (parcelItems.get(i).getParcelId().equals(ID)) {
                ID = parcelItems.get((nextPackage ? i + 1 : i - 1)).getParcelId();
                Toast.makeText(this, getString(R.string.parcel_changing_to) + " " + (nextPackage ? getString(R.string.parcel_chaging_to_next) : getString(R.string.parcel_changing_to_earlier)) + " " + (wasArchived ? getString(R.string.parcel_change_to_archived_package) : ""), Toast.LENGTH_SHORT).show();
                readParcelDataFromSqlite();
                vibrate();
                // Animate
                contentLayout.startAnimation(fadeInAnimation);
                fadeInAnimation.setDuration(500);
                break;
            }
        }
    }


    /**
     * Switches to next package after archive or delete actions
     * return to MainMenu if there's no next package
     */
    private void switchToNextPackageFromCurrent(final ArrayList<ParcelItem> parcelItems) {
        try {
            for (int i = 0; i < parcelItems.size(); i++) {
                if (parcelItems.get(i).getParcelId().equals(ID)) {
                    final int next = i + 1;
                    ID = parcelItems.get(next).getParcelId();
                    Toast.makeText(this, getString(R.string.parcel_changing_to) + " " + getString(R.string.parcel_chaging_to_next), Toast.LENGTH_SHORT).show();
                    readParcelDataFromSqlite();
                    vibrate();
                    // Animate
                    contentLayout.startAnimation(fadeInAnimation);
                    fadeInAnimation.setDuration(500);
                    break;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Parcel.this.finish(); // Go back to MainMenu
        }
    }


    /**
     * Return current parcel items for switching to next from deletion or archive
     *
     * @return Current parcel items
     */
    private ArrayList<ParcelItem> getParcelItems() {
        ArrayList<ParcelItem> parcelItems = new ArrayList<>();
        Cursor res = databaseHelper.getAllDataWithLatestEvent();
        while (res.moveToNext()) {
            parcelItems.add(new ParcelItem(
                    res.getString(0),
                    res.getString(1),
                    res.getString(2),
                    res.getString(3),
                    res.getString(4),
                    res.getString(5),   // Last update status
                    res.getString(6),   // Latest event description
                    res.getString(7),   // Carrier
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }
        return parcelItems;
    }


    // Get parcel data from database
    public void readParcelDataFromSqlite() {
        try {
            parcelDataArray.clear();
            parcelInfoOutput.removeAllViews();
            Cursor res = databaseHelper.getDataByID(ID);
            if (res.getCount() > 0) {
                res.moveToFirst();
                parcelDataArray.add(res.getString(3)); // GET TRACKING CODE
                parcelDataArray.add(res.getString(0)); // GET ID
                trackingNumberText.setText(parcelDataArray.get(0));
                barcode_data = parcelDataArray.get(0);
                // AND SO ON


                /*
                 * Initialized parcel selectCarrierSpinner with
                 * same adapter used in fragment
                 */
                FragmentTrackedDelivery fragmentTrackedDelivery = new FragmentTrackedDelivery();
                fragmentTrackedDelivery.setCarrierSpinnerData(
                        Parcel.this, selectCarrierSpinner, res.getInt(1), databaseHelper, ID
                );


                // CREATE AND SET CODE-128 BARCODE ON IMAGEVIEW
                // GET AND CALCULATE SCREEN WIDTH PIXELS
                DisplayMetrics displayMetrics = new DisplayMetrics();
                WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
                wm.getDefaultDisplay().getMetrics(displayMetrics);
                int screenWidth = displayMetrics.widthPixels;
                //Toast.makeText(Parcel.this, "" + screenWidth, Toast.LENGTH_LONG).show();
                Bitmap bitmap = null;
                try {
                    bitmap = encodeAsBitmap(barcode_data, BarcodeFormat.CODE_128, screenWidth, 240);
                    code128Output.setImageBitmap(bitmap);
                } catch (WriterException e) {
                    e.printStackTrace();
                }


                // Package estimate delivery
                if (!res.getString(6).equals(PhaseNumber.PHASE_DELIVERED_STRING) && !res.getString(7).equals("null") && res.getString(7).length() > 4) {
                    estimateDeliveryOrLastPickupDateView.setVisibility(View.VISIBLE);
                    final String deliveryTimeString = getString(R.string.parcel_estimate_delivery) + " " + res.getString(7);
                    estimateDeliveryOrLastPickupDateTV.setText(deliveryTimeString);
                } else {
                    estimateDeliveryOrLastPickupDateView.setVisibility(View.GONE);
                }


                // Package estimate delivery
                if (!res.getString(6).equals(PhaseNumber.PHASE_DELIVERED_STRING) && !res.getString(15).equals("null") && res.getString(15).length() > 4) {
                    estimateDeliveryOrLastPickupDateView.setVisibility(View.VISIBLE);
                    final String deliveryTimeString = getString(R.string.parcel_last_pickup_date) + " " + res.getString(15);
                    estimateDeliveryOrLastPickupDateTV.setText(deliveryTimeString);
                } else {
                    estimateDeliveryOrLastPickupDateView.setVisibility(View.GONE);
                }


                // Package delivered
                if (res.getString(6).equals(PhaseNumber.PHASE_DELIVERED_STRING)) {
                    deliveredView.setVisibility(View.VISIBLE);
                } else {
                    deliveredView.setVisibility(View.GONE);
                }


                // ADD, BUILD and CREATE PARCEL INFO
                // NAME - STREET - POSTCODE - CITY
                if (res.getString(8).length() <= 4) {
                    //parcelInfoArray.add("Ei noutotietoja!");
                } else {
                    addAdditionalDetailsLine(getString(R.string.parcel_pick_up_address), res.getString(8) + ", " + res.getString(9) + ", " + res.getString(10) + ", " + res.getString(11));
                }
                // PRODUCT
                if (res.getString(16).length() <= 4) {
                    addAdditionalDetailsLine(getString(R.string.parcel_parcel_type), getString(R.string.parcel_parcel_type_unknown_word));
                } else {
                    addAdditionalDetailsLine(getString(R.string.parcel_parcel_type), res.getString(16));
                }
                // SENDER
                if (res.getString(17).length() <= 4) {
                    //parcelInfoArray.add("Lähettäjä ei tiedossa!");
                } else {
                    addAdditionalDetailsLine(getString(R.string.parcel_sender), res.getString(17));
                }
                // LOCKER CODE
                if (res.getString(18).length() > 2 && !res.getString(18).equals("null")) {
                    final String lockerCodeDB = res.getString(18);
                    addAdditionalDetailsLine(getString(R.string.parcel_locker_code), lockerCodeDB);
                    lockerCodeView.setVisibility(View.VISIBLE);
                    lockerCodeTV.setText(lockerCodeDB);
                    this.lockerCode = lockerCodeDB;
                } else {
                    lockerCodeView.setVisibility(View.GONE);
                }
                // Extra service
                if (!res.getString(19).equals("null") && res.getString(19).length() > 0) {
                    addAdditionalDetailsLine(getString(R.string.parcel_service), res.getString(19));
                }
                // Weight
                if (!res.getString(20).equals("null") && res.getString(20).length() > 0) {
                    addAdditionalDetailsLine(getString(R.string.parcel_weight), res.getString(20) + " kg"); //NON-NLS
                }
                // Height
                if (!res.getString(21).equals("null") && res.getString(21).length() > 0) {
                    addAdditionalDetailsLine(getString(R.string.parcel_height), res.getString(21));
                }
                // Width
                if (!res.getString(22).equals("null") && res.getString(22).length() > 0) {
                    addAdditionalDetailsLine(getString(R.string.parcel_width), res.getString(22));
                }
                // Depth
                if (!res.getString(23).equals("null") && res.getString(23).length() > 0) {
                    addAdditionalDetailsLine(getString(R.string.parcel_depth), res.getString(23));
                }
                // Volume
                if (!res.getString(24).equals("null") && res.getString(24).length() > 0) {
                    addAdditionalDetailsLine(getString(R.string.parcel_volume), res.getString(24));
                }
                // DESTINATIONPOSTCODE - DESTINATIONCITY - DESTINATIONCOUNTRY
                //parcelInfoArray.add("Kohdepostinumero: " + res.getString(23) + " Kohdekaupunki: " + res.getString(24) + " Kohdemaa: " + res.getString(25));
                if (res.getString(25).length() >= 5) {
                    addAdditionalDetailsLine(getString(R.string.parcel_destination_post_code), res.getString(25) + " ");
                }
                if (res.getString(26).length() >= 5) {
                    addAdditionalDetailsLine(getString(R.string.parcel_destination_city), res.getString(26) + " ");
                }
                if (res.getString(27).length() >= 5) {
                    addAdditionalDetailsLine(getString(R.string.parcel_destination_country), res.getString(27) + " ");
                }
                // RECIPIENTSIGNATURE
                if (res.getString(28).length() <= 4) {
                    // DO NOTHING
                } else {
                    addAdditionalDetailsLine(getString(R.string.parcel_recipient_signature), res.getString(28));
                }
                // CODAMOUNT - CODCURRENCY
                if (res.getString(29).length() <= 4) {
                    // DO NOTHING
                } else {
                    addAdditionalDetailsLine(getString(R.string.parcel_other), res.getString(29) + "  " + res.getString(30));
                }
                // Original tracking code
                if (res.getString(35) != null) {
                    if (res.getString(35).length() > 0) {
                        addAdditionalDetailsLine(getString(R.string.parcel_original_tracking_code), res.getString(35));
                    }
                }
                // Parcel added create time
                if (res.getString(38) != null) {
                    if (res.getString(38).length() > 0) {
                        addAdditionalDetailsLine(getString(R.string.parcel_parcel_added_time_stamp), res.getString(38));
                    }
                }

                addAdditionalDetailsLine("", ""); // Add one as separator here

                // Package name
                if (res.getString(31) != null) {
                    if (res.getString(31).length() > 0) {
                        addAdditionalDetailsLine(getString(R.string.parcel_package_name), res.getString(31));
                        setTitle(res.getString(31)); // Swap activity title to this package
                    } else {
                        setTitle(getString(R.string.parcel_title));
                    }
                } else {
                    setTitle(getString(R.string.parcel_title));
                }
                // Sender text
                if (res.getString(36) != null) {
                    if (res.getString(36).length() > 0) {
                        addAdditionalDetailsLine(getString(R.string.parcel_sender), res.getString(36));
                    }
                }
                // Delivery method
                if (res.getString(37) != null) {
                    if (res.getString(37).length() > 0) {
                        addAdditionalDetailsLine(getString(R.string.parcel_delivery_method), res.getString(37));
                    }
                }
                // Parcel product page
                if (res.getString(40) != null) {
                    if (res.getString(40).length() > 0) {
                        addAdditionalDetailsLine(getString(R.string.package_edit_dialog_product_page) + ": ", res.getString(40));
                    }
                }
                // Parcel additional notes
                if (res.getString(39) != null) {
                    if (res.getString(39).length() > 0) {
                        addAdditionalDetailsLine(getString(R.string.parcel_additional_notes), res.getString(39));
                    }
                }
                // Order date if set
                if (res.getString(41) != null) {
                    if (res.getString(41).length() > 0) {
                        addAdditionalDetailsLine(getString(R.string.parcel_order_date) + ": ", res.getString(41));
                    }
                }
                // Manually set as delivered
                if (res.getString(42) != null) {
                    if (res.getString(42).length() > 0) {
                        addAdditionalDetailsLine(getString(R.string.parcel_manually_set_delivered) + ": ", res.getString(42));
                    }
                }

                // Current phase (technical detail)
                if (res.getString(6) != null) {
                    if (res.getString(6).length() > 0) {
                        addAdditionalDetailsLine(getString(R.string.parcel_phase) + ": ", res.getString(6));
                    }
                }

                // LATITUDE - LONGITUDE
                if (res.getString(12).length() < 5) {
                    // DO NOTHING, LATITUDE IS MORE LIKE EMPTY
                } else {
                    latitude = Double.parseDouble(res.getString(12));
                    longitude = Double.parseDouble(res.getString(13));
                    //Toast.makeText(Parcel.this, "" + latitude + " " + longitude, Toast.LENGTH_LONG).show();
                }


                // Approximate delivery time
                String differenceDays = databaseHelper.getApproximateDeliveryTime(ID);
                if (differenceDays != null) {
                    final String deliveryStr = getString(R.string.delivered_in) + " " + differenceDays + " " + getString(R.string.parcel_in_days);
                    deliveredTV.setText(deliveryStr);
                }


                // Draw events
                parcelEventsView.removeAllViews(); // Clear contents
                Cursor eventsCursor = databaseHelper.getAllEventsDataWithParcelID(ID);
                noEventsTV.setVisibility((eventsCursor.getCount() > 0 ? View.GONE : View.VISIBLE));
                tryDetectCourierBtn.setVisibility((eventsCursor.getCount() > 0 ? View.GONE : View.VISIBLE));
                int c = 0;
                while (eventsCursor.moveToNext()) {
                    final String eventDescription = eventsCursor.getString(3);
                    final String eventTimeStamp = eventsCursor.getString(4);
                    final String eventLocationCode = eventsCursor.getString(5);
                    final String eventLocationName = eventsCursor.getString(6);

                    // Main view
                    final LinearLayout eventLayoutMain = newEventLayoutMain(); // Get new event layout

                    // Left dots
                    final LinearLayout eventLayoutLeftSide = newEventLayoutLeftSide(); // Get new left side event layout
                    eventLayoutLeftSide.addView(newEventView(c == 0)); // 1
                    eventLayoutLeftSide.addView(newEventView(false)); // 2
                    eventLayoutLeftSide.addView(newEventView(false)); // 3
                    //eventLayoutLeftSide.addView(newEventView(false)); // 4

                    // Right
                    final LinearLayout eventLayoutRightSide = newEventLayoutRightSide(); // Get new right side
                    eventLayoutRightSide.addView(newDescriptionTextView(eventDescription));
                    eventLayoutRightSide.addView(newNormalTextView(eventTimeStamp));
                    eventLayoutRightSide.addView(newNormalTextView((!eventLocationCode.equals("null") || !eventLocationName.equals("null")) ? ((!eventLocationCode.equals("null") ? eventLocationCode + " " : "") + eventLocationName) : getString(R.string.location_not_available)));


                    // Append
                    eventLayoutMain.addView(eventLayoutLeftSide);
                    eventLayoutMain.addView(eventLayoutRightSide);
                    parcelEventsView.addView(eventLayoutMain); // Add main layout into stack
                    c++;
                }

                loadParcelImages();
            } else {
                clearAllViewsContents();
                try {
                    setTitle(databaseHelper.getParcelTitleByID(ID));
                } catch (Exception ignored) {
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }


    /**
     * Clear all views contents
     */
    private void clearAllViewsContents() {
        parcelEventsView.removeAllViews();
        trackingNumberText.setText("");
        lockerCodeTV.setText("");
        code128Output.setImageBitmap(null);
        parcelInfoOutput.removeAllViews();
        selectCarrierSpinner.setAdapter(null);
        deliveredView.setVisibility(View.GONE);
        estimateDeliveryOrLastPickupDateView.setVisibility(View.GONE);
    }


    // ---------------------------------------------------------------------------------------------
    // Functions for drawing events dynamically

    private LinearLayout newEventLayoutMain() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        return linearLayout;
    }

    private LinearLayout newEventLayoutLeftSide() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(
                new LinearLayout.LayoutParams(
                        Utils.dpToPixels(50, displayMetrics),
                        Utils.dpToPixels(70, displayMetrics)
                )
        );
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        return linearLayout;
    }

    private LinearLayout newEventLayoutRightSide() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        return linearLayout;
    }

    private View newEventView(boolean isCircle) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        View view = new View(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                Utils.dpToPixels((isCircle ? 15 : 4), displayMetrics),
                Utils.dpToPixels((isCircle ? 15 : 15), displayMetrics)
        );
        params.setMargins(0, Utils.dpToPixels(5, displayMetrics), 0, 0); // Margin top 5dp
        view.setLayoutParams(params);
        view.setBackgroundResource((isCircle ? R.drawable.circle_green : R.drawable.vertical_bar_gray));
        return view;
    }

    private TextView newDescriptionTextView(final String descriptionText) {
        TextView textView = new TextView(this);
        textView.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        textView.setTextAppearance(this, android.R.style.TextAppearance_Medium); // Set text appearance
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD); // Set bold style
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setText(descriptionText);
        textView.setTextIsSelectable(true);
        if (sharedPreferences.getString(Constants.SP_THEME_SELECTION, "").equals("2")) {
            textView.setTextColor(ContextCompat.getColor(this, R.color.color_white));
        }
        return textView;
    }

    private TextView newNormalTextView(final String text) {
        TextView textView = new TextView(this);
        textView.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        textView.setText(text);
        textView.setTextIsSelectable(true);
        if (sharedPreferences.getString(Constants.SP_THEME_SELECTION, "").equals("2")) {
            textView.setTextColor(ContextCompat.getColor(this, R.color.color_white));
        }
        return textView;
    }

    // ---------------------------------------------------------------------------------------------
    /* Functions for drawing other details view contens dynamically */


    /**
     * This method inflates parcel_additional_details_row and puts data into it and adds into addition info stack
     *
     * @param title       Title for data
     * @param description description for data
     */
    public void addAdditionalDetailsLine(final String title, final String description) {
        View inflatedLayout = layoutInflater.inflate(R.layout.parcel_additional_details_row, null, true);
        final TextView additionalTitle = inflatedLayout.findViewById(R.id.additionalTitle);

        final TextView additionalDescription = inflatedLayout.findViewById(R.id.additionalDescription);
        additionalDescription.setTextIsSelectable(true); // Enable selectable text

        additionalTitle.setText(title);
        additionalDescription.setText(description);
        parcelInfoOutput.addView(inflatedLayout);
    }


    // ---------------------------------------------------------------------------------------------


    /**************************************************************
     * getting from com.google.zxing.client.android.encode.QRCodeEncoder
     *
     * See the sites below
     * http://code.google.com/p/zxing/
     * http://code.google.com/p/zxing/source/browse/trunk/android/src/com/google/zxing/client/android/encode/EncodeActivity.java
     * http://code.google.com/p/zxing/source/browse/trunk/android/src/com/google/zxing/client/android/encode/QRCodeEncoder.java
     */

    // https://www.shodor.org/stella2java/rgbint.html
    private static final int BACKGROUND = 0xFAFAFAFA; // 0xFFFFFFFF
    private static final int BC_BARS = 0xFF000000;


    public static Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int img_width, int img_height) throws WriterException {
        String contentsToEncode = contents;
        if (contentsToEncode == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = null;
        String encoding = guessAppropriateEncoding(contentsToEncode);
        if (encoding != null) {
            hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result;
        try {
            result = writer.encode(contentsToEncode, format, img_width, img_height, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ?
                        BC_BARS : BACKGROUND;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8"; //NON-NLS
            }
        }
        return null;
    }


    // MENU ITEMS
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the MainMenu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_parcel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit_package) {
            Intent intent = new Intent(Parcel.this, ParcelEditor.class);
            //noinspection HardCodedStringLiteral
            intent.putExtra("PARCEL_ID", ID);
            startActivityForResult(intent, ACTIVITY_RESULT_PARCEL_EDITOR);
            return true;
        }
        if (id == R.id.action_clipboard) {
            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", parcelDataArray.get(0));
                clipboard.setPrimaryClip(clip);
                Toast.makeText(Parcel.this, R.string.parcel_code_copied_to_clip_board, Toast.LENGTH_SHORT).show();
            } catch (IndexOutOfBoundsException e) {
                Toast.makeText(this, R.string.parcel_nothing_to_copy, Toast.LENGTH_LONG).show();
            }
            return true;
        }
        if (id == R.id.action_take_image) {
            if (hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)) {
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
                Uri outputFileUri = Uri.fromFile(parcelImageDirectory);
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                startActivityForResult(intent, CAMERA_REQUEST);
            }
            return true;
        }
        if (id == R.id.action_help) {
            AlertDialog alertDialog1 = new AlertDialog.Builder(this).create();
            alertDialog1.setTitle(getString(R.string.parcel_hints_title));
            alertDialog1.setMessage(
                    getString(R.string.parcel_hints_part_one) +
                            getString(R.string.parcel_hints_part_two) + "\n\n" +
                            getString(R.string.parcel_hints_part_three) + "\n\n" +
                            getString(R.string.parcel_hints_part_four) + "\n\n" +
                            getString(R.string.parcel_hints_part_five) + "\n\n" +
                            getString(R.string.parcel_hints_part_six)
            );
            alertDialog1.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.parcel_close_button), (dialog, which) -> {
            });
            alertDialog1.setIcon(R.mipmap.ps_logo_round);
            alertDialog1.show();
            return true;
        }
        if (id == R.id.action_carrier_detector) {
            carrierDetectorDialog(true);
            return true;
        }
        if (id == R.id.action_archive_package) {
            ArrayList<ParcelItem> parcelItems = getParcelItems();
            if (databaseHelper.updateArchived(ID, true)) {
                switchToNextPackageFromCurrent(parcelItems);
            }
            return true;
        }
        if (id == R.id.action_delete_package) {
            deleteItemConfirmationDialog();
            return true;
        }
        if (id == R.id.action_clear_events) {
            clearParcelEventsConfirmDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Set map parameters
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setTrafficEnabled(true);
        googleMap.setIndoorEnabled(true);
        googleMap.setBuildingsEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        // Set position
        googleMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(parcelDataArray.get(0)).snippet(""));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 16f));
    }


    // ---------------------------------------------------------------------------------------------
    // Helpers


    /**
     * Clear parcel current events confirm and execute
     */
    private void clearParcelEventsConfirmDialog() {
        new AlertDialog.Builder(Parcel.this)
                .setTitle(R.string.clear_events)
                .setMessage(R.string.clear_current_events_for_this_parcel)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    if (databaseHelper.clearParcelEventsData(ID)) {
                        readParcelDataFromSqlite();
                    }
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    // Return
                })
                .setIcon(R.mipmap.ps_logo_round)
                .show();
    }


    /**
     * Package deletion confirmation dialog, which to next package after deletion
     */
    private void deleteItemConfirmationDialog() {
        new AlertDialog.Builder(Parcel.this)
                .setTitle(R.string.main_menu_deletion_title)
                .setMessage(R.string.main_menu_you_are_about_to_delete_following_package_from_list_shorter)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    ArrayList<ParcelItem> parcelItems = getParcelItems();
                    if (databaseHelper.deletePackageData(ID)) {
                        switchToNextPackageFromCurrent(parcelItems);
                    }
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    // Return
                })
                .setIcon(R.mipmap.ps_logo_round)
                .show();
    }


    // Vibrate
    private void vibrate() {
        vibrator.vibrate(vibTime);
    }


    // Generic use error dialog
    private void genericErrorDialog(final String title, final String description) {
        if (!this.isFinishing()) {
            new AlertDialog.Builder(Parcel.this)
                    .setTitle(title)
                    .setMessage(description)
                    .setPositiveButton(R.string.parcel_close_button, (dialog, which) -> {
                    })
                    .setIcon(R.mipmap.ps_logo_round)
                    .show();
        }
    }


    // Check if service is running
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    // Refresh Parcel's data
    @SuppressWarnings("HardCodedStringLiteral")
    private void refreshParcels() {
        if (isMyServiceRunning(ParcelService.class)) {
            Toast.makeText(Parcel.this, R.string.parcel_search_running_try_again_later, Toast.LENGTH_LONG).show();
            swipeRefreshLayout.setRefreshing(true);
        } else {
            if (!isMyServiceRunning(ParcelService.class)) {
                // Start service
                Intent intent = new Intent(this, ParcelService.class);
                intent.putExtra("MODE", 0);
                intent.putExtra("NOTIFICATIONS", false);
                intent.putExtra("PARCEL_CODE", "");
                intent.putExtra("PARCEL_ID", ID);
                intent.putExtra("UPDATE_FAILED_FIRST", false);
                intent.putExtra("COURIER_ICONS_ENABLED", true);
                intent.putExtra("START_AS_FOREGROUND_SERVICE", false);
                startService(intent);
                swipeRefreshLayout.setRefreshing(true);
            }
        }
    }


    // Activity result
    @SuppressWarnings("HardCodedStringLiteral")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Package photo feature
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            try {
                String photoPath = Environment.getExternalStorageDirectory() + "/PaketinSeuranta/Kuvat/" + "temp.png";
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);
                saveImageToDB(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == ACTIVITY_RESULT_PARCEL_EDITOR) {
            readParcelDataFromSqlite();
        }
    }


    // Save image to database
    private void saveImageToDB(Bitmap img) {
        byte[] data = getBitmapAsByteArray(img);
        databaseHelper.insertImageData(ID, data);
        Toast.makeText(this, R.string.parcel_picture_saved, Toast.LENGTH_LONG).show();
        loadParcelImages();
    }


    // Convert byteArrayBitmap
    private static byte[] getBitmapAsByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap = Utils.resizeBitmap(bitmap, 576, 704);
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }


    // Load parcel images from database
    private void loadParcelImages() {
        try {
            parcelImagesLayout.removeAllViews();
            Cursor cur = databaseHelper.loadParcelImages(ID);
            while (cur.moveToNext()) {
                final String imageId = cur.getString(0);
                byte[] imgByte = cur.getBlob(1);
                appendParcelImage(imageId, BitmapFactory.decodeByteArray(imgByte, 0, imgByte.length));
            }
            cur.close();
        } catch (IllegalStateException ignored) {
        }
    }


    // Append new parcel image
    private void appendParcelImage(final String imageId, final Bitmap bitmap) {
        ImageView parcelImage = new ImageView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(180, 240);
        layoutParams.setMargins(5, 5, 5, 5);
        parcelImage.setLayoutParams(layoutParams);
        parcelImage.setImageBitmap(bitmap);
        parcelImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageViewDialog(imageId, bitmap);
            }
        });
        parcelImagesLayout.addView(parcelImage);
    }


    // Show selected image in dialog
    private void imageViewDialog(final String imageId, final Bitmap bitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(R.string.parcel_close_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        })
                .setNegativeButton(R.string.parcel_delete_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deletePackageImageConfirmationDialog(imageId);
                    }
                });
        final AlertDialog dialog = builder.create();
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.image_view_dialog, null);
        dialog.setView(dialogLayout);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
        ImageView image = dialog.findViewById(R.id.dialogImage);
        image.setImageBitmap(bitmap);
    }

    // Confirmation dialog for image deletion
    private void deletePackageImageConfirmationDialog(final String imageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.parcel_do_you_want_to_delete_this_image);
        builder.setCancelable(true);
        builder.setPositiveButton(getString(R.string.parcel_delete_button), (dialog, which) -> {
            if (databaseHelper.deleteParcelImage(imageId)) {
                loadParcelImages();
                Toast.makeText(Parcel.this, R.string.parcel_picture_removed, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.parcel_cancel, (dialog, which) -> {
        });
        AlertDialog dialog = builder.create();
        dialog.show();
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
            if (ContextCompat.checkSelfPermission(Parcel.this, permission) == PackageManager.PERMISSION_GRANTED) {
            } else {
                requestPermissions(new String[]{permission}, 1);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createParcelImageDirectory();
        }
    }


    // If has external storage permission, do image dir for package image
    @SuppressWarnings("HardCodedStringLiteral")
    private void createParcelImageDirectory() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            File root = new File(Environment.getExternalStorageDirectory() + File.separator + "PaketinSeuranta" + File.separator + "Kuvat" + File.separator);
            root.mkdirs();
            parcelImageDirectory = new File(root, "temp.png");
        }
    }


    // ---------------------------------------------------------------------------------------------
    /* Carrier detector features */

    // Ask user to give pin code
    private void carrierDetectorDialog(boolean autoStart) {
        if (parcelDataArray.size() > 0) {
            carrierDetectorDialog = new Dialog(this, R.style.carrierDetectorDialogStyle);
            if (carrierDetectorDialog.getWindow() == null) {
                return;
            }
            carrierDetectorDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            carrierDetectorDialog.setContentView(R.layout.carrier_detector_dialog);
            carrierDetectorDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            carrierDetectorDialog.setCanceledOnTouchOutside(false);
            carrierDetectorDialog.show();
            detectCarrierProgressBar = carrierDetectorDialog.findViewById(R.id.progressBar);
            detectedLayout = carrierDetectorDialog.findViewById(R.id.detectedLayout);
            detectedLayout.removeAllViews();
            final Button startDetectorBtn = carrierDetectorDialog.findViewById(R.id.startDetectorBtn);
            final Button closeDialogBtn = carrierDetectorDialog.findViewById(R.id.closeDialogBtn);
            if (autoStart) {
                detectCarrierProgressBar.setVisibility(View.VISIBLE);
                detectCarrierProgressBar.setProgress(0);
                carrierDetectorTask = new CarrierDetectorTask(Parcel.this, parcelDataArray.get(0));
                carrierDetectorTask.execute();
                startDetectorBtn.setVisibility(View.GONE);
            }
            startDetectorBtn.setOnClickListener(view -> {
                detectCarrierProgressBar.setVisibility(View.VISIBLE);
                detectCarrierProgressBar.setProgress(0);
                carrierDetectorTask = new CarrierDetectorTask(Parcel.this, parcelDataArray.get(0));
                carrierDetectorTask.execute();
            });
            closeDialogBtn.setOnClickListener(view -> {
                carrierDetectorDialog.dismiss();
                if (carrierDetectorTask != null) {
                    carrierDetectorTask.cancel(true);
                }
            });
        } else {
            Toast.makeText(this, R.string.no_code, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onCarrierDetected(final Integer carrierId) {
        runOnUiThread(() -> {
            Log.i(TAG, String.valueOf(carrierId));
            switch (carrierId) {
                case CarrierUtils.CARRIER_POSTI:
                    addCarrierForDetectionView(carrierId, R.mipmap.posti_logo);
                    break;
                case CarrierUtils.CARRIER_MATKAHUOLTO:
                    addCarrierForDetectionView(carrierId, R.mipmap.matkahuolto_logo);
                    break;
                case CarrierUtils.CARRIER_DHL_EXPRESS:
                    addCarrierForDetectionView(carrierId, R.mipmap.dhl_logo);
                    break;
                case CarrierUtils.CARRIER_DHL_AMAZON:
                    addCarrierForDetectionView(carrierId, R.mipmap.dhl_logo);
                    break;
                case CarrierUtils.CARRIER_DHL_ACTIVE_TRACKING:
                    addCarrierForDetectionView(carrierId, R.mipmap.dhl_logo);
                    break;
                case CarrierUtils.CARRIER_UPS:
                    addCarrierForDetectionView(carrierId, R.mipmap.ups_logo);
                    break;
                case CarrierUtils.CARRIER_FEDEX:
                    addCarrierForDetectionView(carrierId, R.mipmap.fedex_logo);
                    break;
                case CarrierUtils.CARRIER_POSTNORD:
                    addCarrierForDetectionView(carrierId, R.mipmap.postnord_logo);
                    break;
                case CarrierUtils.CARRIER_ARRA_PAKETTI:
                    addCarrierForDetectionView(carrierId, R.mipmap.arra_logo);
                    break;
                case CarrierUtils.CARRIER_YANWEN:
                    addCarrierForDetectionView(carrierId, R.mipmap.yanwen_logo);
                    break;
                case CarrierUtils.CARRIER_4PX:
                    addCarrierForDetectionView(carrierId, R.mipmap.fpx_logo);
                    break;
                case CarrierUtils.CARRIER_CAINIAO:
                    addCarrierForDetectionView(carrierId, R.mipmap.cainiao_logo);
                    break;
                case CarrierUtils.CARRIER_BRING:
                    addCarrierForDetectionView(carrierId, R.mipmap.bring_logo);
                    break;
            }
        });
    }


    /**
     * Method which sets detected carrier item icon which has click listener to set selection as carrier
     *
     * @param carrierId           Carrier id number
     * @param carrierIconDrawable Corresponding carrier logo for imageView "button"
     */
    private void addCarrierForDetectionView(final Integer carrierId, final int carrierIconDrawable) {
        ImageView imageView = new ImageView(this);


        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                Utils.dpToPixels(80, getResources().getDisplayMetrics()),
                Utils.dpToPixels(70, getResources().getDisplayMetrics())
        );
        layoutParams.setMargins(0, 10, 0, 10);
        imageView.setLayoutParams(layoutParams);

        imageView.setLayoutParams(layoutParams);
        imageView.setBackgroundResource(carrierIconDrawable);
        imageView.setOnClickListener(view -> {
            if (databaseHelper.updateCarrierCode(ID, carrierId)) {
                Toast.makeText(Parcel.this, R.string.courier_set, Toast.LENGTH_SHORT).show();
                readParcelDataFromSqlite(); // Refresh current parcel data
                refreshParcels(); // Refresh parcels data
                if (carrierDetectorDialog != null) {
                    carrierDetectorDialog.dismiss();
                }
                if (carrierDetectorTask != null) {
                    carrierDetectorTask.cancel(true);
                }
            }
        });


        detectedLayout.addView(imageView);
    }


    @Override
    public void onCarrierDetectorEnded() {
        runOnUiThread(() -> {
            detectCarrierProgressBar.setVisibility(View.GONE);
        });
    }

    @Override
    public void onProgressbarProgressUpdate(Integer progress) {
        runOnUiThread(() -> {
            detectCarrierProgressBar.setProgress(progress);
        });
    }


    // ---------------------------------------------------------------------------------------------

} // End of class