/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.paketinseuranta;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.multidex.MultiDex;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nitramite.adapters.ParcelsAdapter;
import com.nitramite.adapters.ParcelsAdapterListener;
import com.nitramite.adapters.RecyclerItemTouchHelper;
import com.nitramite.utils.CSVExporter;
import com.nitramite.utils.LocaleUtils;
import com.nitramite.utils.ThemeUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class Archive extends AppCompatActivity implements ParcelsAdapterListener, RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    private static final String TAG = Archive.class.getSimpleName();

    // Main items
    private final DatabaseHelper databaseHelper = new DatabaseHelper(this);
    private final ArrayList<ParcelItem> parcelItems = new ArrayList<>();

    // Components
    private InputMethodManager inputMethodManager;
    private final LocaleUtils localeUtils = new LocaleUtils();
    private View emptyView = null;
    private RecyclerView recyclerView;
    private ParcelsAdapter adapter = null;
    private CardView searchQueryCard;
    private EditText searchArchiveInput;
    private ImageView clearToolBarImage;

    // Variables
    private static final int PARCEL_ACTIVITY_RESULT = 3;  // The request code

    @Override
    protected void onResume() {
        super.onResume();
        if (searchArchiveInput != null) {
            if (searchArchiveInput.getText().toString().length() > 0) {
                triggerSearch();
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(localeUtils.updateBaseContextLocale(base));
        MultiDex.install(this);
    }

    @SuppressLint({"RestrictedApi", "SourceLockedOrientationActivity"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set theme
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        super.onCreate(savedInstanceState);
        if (ThemeUtils.Theme.isDarkTheme(getBaseContext())) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
        } else if (ThemeUtils.Theme.isAutoTheme(getBaseContext())) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        localeUtils.setApplicationLanguage(this);
        setContentView(R.layout.activity_archive);
        setTitle(R.string.archive_title);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        // Screen orientation setting
        if (sharedPreferences.getBoolean(Constants.SP_PORTRAIT_ORIENTATION_ONLY, false)) {
            super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // Get system services
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Find and int views
        emptyView = findViewById(R.id.emptyView);
        recyclerView = findViewById(R.id.archiveItemsList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        searchQueryCard = findViewById(R.id.searchQueryCard);
        searchQueryCard.setVisibility(View.GONE);
        searchArchiveInput = findViewById(R.id.searchArchiveInput);
        clearToolBarImage = findViewById(R.id.clearToolBarImage);
        clearToolBarImage.setVisibility(View.GONE);
        ImageView searchToolBarImage = findViewById(R.id.searchToolBarImage);
        if (sharedPreferences.getString(Constants.SP_THEME_SELECTION, "").equals("2")) {
            searchToolBarImage.setImageResource(R.drawable.ic_search);
            clearToolBarImage.setImageResource(R.drawable.ic_clear);
        }

        clearToolBarImage.setOnClickListener(view -> {
            searchArchiveInput.setText("");
        });

        searchArchiveInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                triggerSearch();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // Activity results set
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);


        // Read items
        readItems(null);
    } // End of onCreate()


    // ---------------------------------------------------------------------------------------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PARCEL_ACTIVITY_RESULT) {
            readItems(null);
        }
    }

    // ---------------------------------------------------------------------------------------------

    // Get archive items
    private void readItems(String searchStr) {
        parcelItems.clear();
        Cursor res = databaseHelper.getAllArchiveDataWithLatestEvent(searchStr);
        while (res.moveToNext()) {
            ParcelItem parcelItem = new ParcelItem(
                    res.getString(0),
                    res.getString(1),
                    res.getString(2),
                    res.getString(3),
                    res.getString(4),
                    res.getString(5),      // Last update status
                    res.getString(6),      // Latest event description
                    res.getString(7),      // Carrier
                    res.getString(8),
                    res.getString(9),
                    res.getString(10),
                    res.getString(11),
                    null,           // Last pickup date,
                    null,                 // Last event date, null because not used here
                    null
            );
            parcelItem.setArchivedPackage(true); // This package is archived item
            parcelItems.add(parcelItem);
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
                adapter = new ParcelsAdapter(Archive.this, parcelItems, true, false);
                adapter.setClickListeners(this);
                recyclerView.setAdapter(adapter);


                ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(
                        0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, this, RecyclerItemTouchHelper.ActivityTarget.ARCHIVE
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
        String parcelId = parcelItems.get(position).getParcelId();
        Intent i = new Intent(Archive.this, Parcel.class);
        i.putExtra("PARCEL_ID", parcelId); //NON-NLS
        startActivityForResult(i, PARCEL_ACTIVITY_RESULT);
    }

    @Override
    public void onItemLongClick(View view, int position) {
        setDeliveredConfirmationDialog(position);
    }


    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        switch (direction) {
            case 4:
                directionLeftDelete(position);
                break;
            case 8:
                directionRightReturn(position);
                break;
        }
    }


    // Swipe from right to left deletes item
    private void directionLeftDelete(int position) {
        deleteItemConfirmationDialog(position);
    }


    // Swipe from left to right returns item on list
    private void directionRightReturn(int position) {
        String codeId = parcelItems.get(position).getParcelId();
        databaseHelper.updateArchived(codeId, false);
        readItems(null);
    }


    private void deleteItemConfirmationDialog(int position) {
        new AlertDialog.Builder(Archive.this)
                .setTitle(R.string.archive_delete_title)
                .setMessage(R.string.archive_delete_description)
                .setPositiveButton(R.string.yes_btn, (dialog, which) -> {
                    String codeId = parcelItems.get(position).getParcelId();
                    databaseHelper.deletePackageData(codeId);
                    readItems(null);
                })
                .setNegativeButton(R.string.no_btn, (dialog, which) -> {
                    // Return
                })
                .setIcon(R.mipmap.ps_logo_round)
                .show();
    }


    private void setDeliveredConfirmationDialog(final int position) {
        new AlertDialog.Builder(Archive.this)
                .setTitle(R.string.archive_delivered_status_set_title)
                .setMessage(R.string.archive_delivered_status_set_description)
                .setPositiveButton(R.string.yes_btn, (dialog, which) -> {
                    String codeId = parcelItems.get(position).getParcelId();
                    databaseHelper.updatePackageAsDelivered(codeId);
                    readItems(null);
                })
                .setNegativeButton(R.string.no_btn, (dialog, which) -> {
                    // Return
                })
                .setIcon(R.mipmap.ps_logo_round)
                .show();
    }


    // CSV export dialog
    private void csvExportDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.csv_export_layout);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        // Better instruction of output folder
        final TextView csvExportDescription = dialog.findViewById(R.id.csvExportDescription);
        String outputFolder = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            outputFolder = Objects.requireNonNull(this.getExternalFilesDir(null)).getPath();
        } else {
            outputFolder = CSVExporter.CSV_OLD_DIR;
        }
        String f = getString(R.string.csv_export_layout_csv_export_properties_summary) + " " + outputFolder + " " + getString(R.string.to_folder);
        csvExportDescription.setText(f);

        // Find action buttons
        final Button openLocationBtn = dialog.findViewById(R.id.openLocationBtn);
        final Button dismissBtn = dialog.findViewById(R.id.dismissBtn);
        final Button exportBtn = dialog.findViewById(R.id.exportBtn);

        // Find checkboxes
        final CheckBox nameCB = dialog.findViewById(R.id.nameCB);
        final CheckBox trackingCodeCB = dialog.findViewById(R.id.trackingCodeCB);
        final CheckBox senderCB = dialog.findViewById(R.id.senderCB);
        final CheckBox deliveryMethodCB = dialog.findViewById(R.id.deliveryMethodCB);
        final CheckBox parcelAddDateCB = dialog.findViewById(R.id.parcelAddDateCB);
        final CheckBox lastParcelEventCB = dialog.findViewById(R.id.lastParcelEventCB);
        final CheckBox readyForPickupDateCB = dialog.findViewById(R.id.readyForPickupDateCB);
        final CheckBox productPageCB = dialog.findViewById(R.id.productPageCB);

        // Listeners
        exportBtn.setOnClickListener(view -> {
            CSVExporter csvExporter = new CSVExporter();
            try {
                final String exportFileName = csvExporter.exportCSV(
                        this,
                        databaseHelper,
                        nameCB.isChecked(),
                        trackingCodeCB.isChecked(),
                        senderCB.isChecked(),
                        deliveryMethodCB.isChecked(),
                        parcelAddDateCB.isChecked(),
                        lastParcelEventCB.isChecked(),
                        readyForPickupDateCB.isChecked(),
                        productPageCB.isChecked()
                );
                dialog.dismiss();
                if (exportFileName == null) {
                    genericTextDialog(getString(R.string.main_menu_error), getString(R.string.archive_csv_export_failed_description));
                } else {
                    Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                    File file = csvExporter.GetCSVExportFile(this, exportFileName);
                    if (file.exists()) {
                        intentShareFile.setType("*/*");
                        intentShareFile.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                this,
                                this.getApplicationContext().getPackageName() + ".provider",
                                file
                        ));
                        intentShareFile.putExtra(Intent.EXTRA_SUBJECT, exportFileName);
                        intentShareFile.putExtra(Intent.EXTRA_TEXT, exportFileName);
                        intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(intentShareFile, exportFileName));
                    } else {
                        Toast.makeText(this, "CSV file to export does not exist!", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (IOException e) {
                genericTextDialog(getString(R.string.main_menu_error), e.toString());
                e.printStackTrace();
            }
        });
        dismissBtn.setOnClickListener(view -> dialog.dismiss());
        openLocationBtn.setOnClickListener(v -> openCSVFolder());
    }


    public void openCSVFolder() {
        String path = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            path = Objects.requireNonNull(this.getExternalFilesDir(null)).getPath();
        } else {
            path = CSVExporter.CSV_OLD_DIR;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Log.i(TAG, path);
        Uri uri = Uri.parse(path);
        intent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Open folder"));
    }


    // Generic text dialog
    private void genericTextDialog(final String title, final String description) {
        if (!this.isFinishing()) {
            new android.app.AlertDialog.Builder(Archive.this)
                    .setTitle(title)
                    .setMessage(description)
                    .setPositiveButton(R.string.main_menu_close, (dialog, which) -> {
                    })
                    .setIcon(R.mipmap.ps_logo_round)
                    .show();
        }
    }


    public void toggleSearch() {
        final boolean searchCardVisible = searchQueryCard.getVisibility() == View.VISIBLE;
        searchQueryCard.setVisibility(searchCardVisible ? View.GONE : View.VISIBLE);
        if (!searchCardVisible) {
            searchArchiveInput.requestFocus();
            inputMethodManager.showSoftInput(searchArchiveInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }


    private void triggerSearch() {
        final String searchText = searchArchiveInput.getText().toString();
        if (searchText.length() > 0) {
            int c = (searchText.length() % 2);
            if (c == 0) {
                readItems(searchText);
            }
            clearToolBarImage.setVisibility(View.VISIBLE);
        } else {
            readItems(null);
            clearToolBarImage.setVisibility(View.GONE);
        }
    }


    // Menu items
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_archive, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_help) {
            android.app.AlertDialog alertDialog1 = new android.app.AlertDialog.Builder(this).create();
            alertDialog1.setTitle(getString(R.string.archive_hints));
            alertDialog1.setMessage(
                    getString(R.string.arhive_hint_message_part_one) + "\n\n" +
                            getString(R.string.arhive_hint_message_part_two) + "\n\n" +
                            getString(R.string.arhive_hint_message_part_three)
            );
            alertDialog1.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.archive_close_button), (dialog, which) -> {
            });
            alertDialog1.setIcon(R.mipmap.ps_logo_round);
            alertDialog1.show();
            return true;
        }
        if (id == R.id.action_export) {
            csvExportDialog();
            return true;
        }
        if (id == R.id.action_search) {
            toggleSearch();
            return true;
        }
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}