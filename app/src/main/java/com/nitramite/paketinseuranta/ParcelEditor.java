package com.nitramite.paketinseuranta;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.multidex.MultiDex;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.nitramite.courier.ParcelObject;
import com.nitramite.utils.LocaleUtils;

import org.jetbrains.annotations.NonNls;

public class ParcelEditor extends AppCompatActivity {

    //  Logging
    @NonNls
    private static final String TAG = "ParcelEditor";

    // Marshmallow+ permission request
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 1;

    // Components
    public ParcelObject parcelObject;
    public FragmentTrackedDeliveryInterface fragmentTrackedDeliveryInterface;
    public ClipboardManager clipboard;
    private LocaleUtils localeUtils = new LocaleUtils();
    public DatabaseHelper databaseHelper = new DatabaseHelper(this);
    public String parcelId = null;
    public FragmentTrackedDelivery.TrackedDeliveryType trackedDeliveryType = FragmentTrackedDelivery.TrackedDeliveryType.NEW_PACKAGE;
    private MenuItem saveBtnMenuItem;
    public boolean saveBtnMenuItemVisibility = false;


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(localeUtils.updateBaseContextLocale(base));
        MultiDex.install(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        localeUtils.setApplicationLanguage(this);
        setContentView(R.layout.activity_parcel_editor);

        // Get services
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        // Find components
        ViewPager viewPager = findViewById(R.id.view_pager);
        TabLayout tabs = findViewById(R.id.tabs);

        // New or existing package
        @NonNls Intent intent = getIntent();
        parcelId = intent.getStringExtra("PARCEL_ID");
        if (isNewParcel()) {
            setTitle(R.string.add_tracked_delivery);
            parcelObject = new ParcelObject(null);
        } else {
            setTitle(R.string.edit_tracked_delivery);
            trackedDeliveryType = FragmentTrackedDelivery.TrackedDeliveryType.EXISTING_PACKAGE;
            tabs.setVisibility(View.GONE);
            parcelObject = new ParcelObject(null);
        }

        // Init adapters
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager(), trackedDeliveryType);
        viewPager.setAdapter(sectionsPagerAdapter);

        tabs.setupWithViewPager(viewPager);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null && fragmentTrackedDeliveryInterface != null) {
                Toast.makeText(ParcelEditor.this, getString(R.string.main_menu_scanned_text) + " " + result.getContents(), Toast.LENGTH_SHORT).show();
                fragmentTrackedDeliveryInterface.onBarcodeScanResult(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(ParcelEditor.this)
                .setTitle(R.string.note)
                .setMessage(R.string.leave_without_saving_changes)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    switch (trackedDeliveryType) {
                        case NEW_PACKAGE:
                            databaseHelper.deletePackageData(parcelId);
                            break;
                        case EXISTING_PACKAGE:
                            break;
                    }
                    setResult(RESULT_OK);
                    finish();
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                })
                .setIcon(R.mipmap.logo)
                .show();
    }


    // Has camera permission
    public boolean hasCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void setFragmentTrackedDeliveryInterface(FragmentTrackedDeliveryInterface fragmentTrackedDeliveryInterface) {
        this.fragmentTrackedDeliveryInterface = fragmentTrackedDeliveryInterface;
    }


    // MENU ITEMS
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_parcel_editor, menu);
        saveBtnMenuItem = menu.findItem(R.id.action_save);
        saveBtnMenuItem.setVisible(this.saveBtnMenuItemVisibility);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            this.fragmentTrackedDeliveryInterface.onSaveChangesActionBtnClick();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Control visibility from fragment
     *
     * @param visible boolean
     */
    public void toggleSaveActionBtnVisibility(boolean visible) {
        if (saveBtnMenuItem != null) {
            saveBtnMenuItem.setVisible(visible);
        }
    }


    /**
     * Is new parcel or editing existing one
     *
     * @return boolean
     */
    public boolean isNewParcel() {
        return this.parcelId == null;
    }


} // End of class