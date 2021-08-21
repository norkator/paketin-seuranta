package com.nitramite.paketinseuranta;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.multidex.MultiDex;
import androidx.preference.PreferenceManager;

import com.nitramite.adapters.CustomBarcodeListAdapter;
import com.nitramite.utils.LocaleUtils;

import java.util.ArrayList;

@SuppressWarnings("FieldCanBeLocal")
public class BarcodeList extends AppCompatActivity {

    // Activity components
    private LocaleUtils localeUtils = new LocaleUtils();
    private Switch showOnlyReadyForPickupSwitch;
    private ListView barcodeList;
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);
    private Boolean SP_BAR_CODE_LIST_ONLY_READY_FOR_PICKUP_PARCELS = false;
    private Button spacingPlusBtn, spacingMinusBtn;
    private Integer barcodeListSpacingValue = 5; // 5dp default


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(localeUtils.updateBaseContextLocale(base));
        MultiDex.install(this);
    }


    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        localeUtils.setApplicationLanguage(this);
        setContentView(R.layout.activity_barcode_list);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        // Find components
        showOnlyReadyForPickupSwitch = findViewById(R.id.showOnlyReadyForPickupSwitch);
        barcodeList = findViewById(R.id.barcodeList);
        spacingPlusBtn = findViewById(R.id.spacingPlusBtn);
        spacingMinusBtn = findViewById(R.id.spacingMinusBtn);


        // Get shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SP_BAR_CODE_LIST_ONLY_READY_FOR_PICKUP_PARCELS = sharedPreferences.getBoolean(Constants.SP_BAR_CODE_LIST_ONLY_READY_FOR_PICKUP_PARCELS, false);
        showOnlyReadyForPickupSwitch.setChecked(SP_BAR_CODE_LIST_ONLY_READY_FOR_PICKUP_PARCELS);
        barcodeListSpacingValue = sharedPreferences.getInt(Constants.SP_BAR_CODE_LIST_SPACING_VALUE, 5);

        // Screen orientation setting
        if (sharedPreferences.getBoolean(Constants.SP_PORTRAIT_ORIENTATION_ONLY, false)) {
            super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        showOnlyReadyForPickupSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            getParameters(b);
            SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            SharedPreferences.Editor editor = setSharedPreferences.edit();
            editor.putBoolean(Constants.SP_BAR_CODE_LIST_ONLY_READY_FOR_PICKUP_PARCELS, b);
            editor.apply();
        });


        spacingPlusBtn.setOnClickListener(view -> {
            barcodeListSpacingValue = barcodeListSpacingValue + 5;
            getParameters(SP_BAR_CODE_LIST_ONLY_READY_FOR_PICKUP_PARCELS);
        });

        spacingMinusBtn.setOnClickListener(view -> {
            if (barcodeListSpacingValue > 5) {
                barcodeListSpacingValue = barcodeListSpacingValue - 5;
                getParameters(SP_BAR_CODE_LIST_ONLY_READY_FOR_PICKUP_PARCELS);
            } else {
                Toast.makeText(BarcodeList.this, R.string.barcode_list_space_cannot_be_made_smaller, Toast.LENGTH_LONG).show();
            }
        });


        getParameters(SP_BAR_CODE_LIST_ONLY_READY_FOR_PICKUP_PARCELS);
    } // End of onCreate()


    /* Get data from database and draw list */
    private void getParameters(final Boolean readyToPickUpOnly) {
        ArrayList<String> parcelCodeItems = new ArrayList<>();
        ArrayList<String> parcelTitleItems = new ArrayList<>();
        Cursor res = null;
        if (readyToPickUpOnly) {
            res = databaseHelper.getAllDataWhereReadyToPickupState();
        } else {
            res = databaseHelper.getAllData();
        }
        while (res.moveToNext()) {
            String code = res.getString(3);
            // Skip duplicate codes
            if (!parcelCodeItems.contains(code)) {
                parcelCodeItems.add(code);
                parcelTitleItems.add(res.getString(31));
            }
        }
        CustomBarcodeListAdapter customBarcodeListAdapter = new CustomBarcodeListAdapter(this, parcelCodeItems, parcelTitleItems, this.barcodeListSpacingValue);
        barcodeList.setAdapter(customBarcodeListAdapter);

        // Save spacing value
        SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = setSharedPreferences.edit();
        editor.putInt(Constants.SP_BAR_CODE_LIST_SPACING_VALUE, barcodeListSpacingValue);
        editor.apply();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


} // End of class