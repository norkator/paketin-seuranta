package com.nitramite.paketinseuranta;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.multidex.MultiDex;

import com.nitramite.utils.Backup;
import com.nitramite.utils.BackupUtils;
import com.nitramite.utils.DialogUtils;
import com.nitramite.utils.LocaleUtils;
import com.nitramite.utils.SharedPreferencesUtils;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Objects;

public class BackupManager extends AppCompatActivity {

    @NonNls
    private static final String TAG = BackupManager.class.getSimpleName();

    private TextView lastBackupDate;

    private SharedPreferences sharedPreferences = null;
    private final LocaleUtils localeUtils = new LocaleUtils();
    private final DialogUtils dialogUtils = new DialogUtils();
    private static final int OPEN_DIRECTORY_REQUEST_CODE = 1;
    private static final int IMPORT_BACKUP_FILE_REQUEST_CODE = 2;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(localeUtils.updateBaseContextLocale(base));
        MultiDex.install(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        localeUtils.setApplicationLanguage(this);
        setContentView(R.layout.activity_backup_manager);
        setTitle(getString(R.string.backup));

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPreferences = SharedPreferencesUtils.getSharedPreferences(getApplicationContext());

        // Find views
        CheckBox timedBackupToggle = findViewById(R.id.timedBackupToggle);
        Button takeBackupBtn = findViewById(R.id.takeBackupBtn);
        Button exportBackupBtn = findViewById(R.id.exportBackupBtn);
        Button importBackupBtn = findViewById(R.id.importBackupBtn);
        Button restoreBackupBtn = findViewById(R.id.restoreBackupBtn);
        lastBackupDate = findViewById(R.id.lastBackupDate);
        setLastBackupTakenView();


        timedBackupToggle.setChecked(sharedPreferences.getBoolean(Constants.SP_TIMED_BACKUP_ENABLED, false));
        timedBackupToggle.setOnCheckedChangeListener((compoundButton, b) -> {
            SharedPreferences setSharedPreferences = SharedPreferencesUtils.getSharedPreferences(getApplicationContext());
            SharedPreferences.Editor normalEditor = setSharedPreferences.edit();
            normalEditor.putBoolean(Constants.SP_TIMED_BACKUP_ENABLED, b);
            normalEditor.apply();
            Toast.makeText(this, getString(R.string.timed_back_up_toggle) + " " + (b ? getString(R.string.timed_backup_on) : getString(R.string.timed_backup_off)), Toast.LENGTH_SHORT).show();
        });


        takeBackupBtn.setOnClickListener(view -> {
            Backup backup = BackupUtils.BackupDatabase(BackupManager.this);
            if (backup.isSuccess()) {
                backupSuccessDialog(this, this.isFinishing(),
                        getString(R.string.main_menu_result),
                        getString(R.string.main_menu_taking_backup_success) + " " + backup.getLocation(),
                        backup
                );
                BackupUtils.SaveBackupDate(this, Calendar.getInstance()); // save last backup date time
                setLastBackupTakenView();
            } else {
                dialogUtils.genericErrorDialog(this, this.isFinishing(),
                        getString(R.string.main_menu_error),
                        getString(R.string.main_menu_taking_backup_failed) + " " + backup.getExceptionString());
            }
        });

        exportBackupBtn.setOnClickListener(v -> {
            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
            File file = BackupUtils.GetBackupFileDestination(this);
            if (file.exists()) {
                String s = "PARCELS.db";
                intentShareFile.setType("*/*");
                intentShareFile.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                        this,
                        this.getApplicationContext().getPackageName() + ".provider",
                        file
                ));
                intentShareFile.putExtra(Intent.EXTRA_SUBJECT, s);
                intentShareFile.putExtra(Intent.EXTRA_TEXT, s);
                intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intentShareFile, s));
            } else {
                Toast.makeText(this, "Backup file does not exist", Toast.LENGTH_SHORT).show();
            }
        });

        importBackupBtn.setOnClickListener(v -> {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
            chooseFile.setType("*/*");
            startActivityForResult(
                    Intent.createChooser(chooseFile, "Choose a backup file"),
                    IMPORT_BACKUP_FILE_REQUEST_CODE
            );
        });

        restoreBackupBtn.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.confirm)
                .setMessage(R.string.continue_with_backup_restore)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.yes_btn, (dialog, whichButton) -> {
                    Backup backup = BackupUtils.RestoreDatabase(BackupManager.this);
                    if (backup.isSuccess()) {
                        Toast.makeText(BackupManager.this, R.string.main_menu_restore_successfull, Toast.LENGTH_LONG).show();
                        terminateApp();
                    } else {
                        dialogUtils.genericErrorDialog(this, this.isFinishing(), getString(R.string.main_menu_error),
                                getString(R.string.main_menu_restore_un_successfull) + " " + backup.getExceptionString());
                    }
                })
                .setNegativeButton(R.string.no_btn, null).show());


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_DIRECTORY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.i(TAG, Objects.requireNonNull(data.getDataString()));
        } else if (requestCode == IMPORT_BACKUP_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri content_describer = data.getData();
            assert content_describer != null;
            Log.d(TAG, Objects.requireNonNull(content_describer.getPath()));
            try {
                InputStream inputStream = this.getContentResolver().openInputStream(content_describer);
                assert inputStream != null;
                if (BackupUtils.ImportDatabase(this, inputStream)) {
                    Toast.makeText(this, "Database imported. You can now use restore button.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to import database from external resource", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void backupSuccessDialog(Context context, boolean isFinishing, final String title, final String description, Backup backup) {
        try {
            if (!isFinishing) {
                new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage(description)
                        .setPositiveButton(R.string.main_menu_close, (dialog, which) -> {
                        })
                        .setIcon(R.mipmap.ps_logo_round)
                        .show();
            }
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        }
    }


    /**
     * Shut down app
     */
    private void terminateApp() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            finishAffinity();
            System.exit(0);
        }, 2000);
    }


    private void setLastBackupTakenView() {
        final String lastBackupDateStr = sharedPreferences.getString(Constants.SP_TIMED_BACKUP_LAST_DATE, null);
        lastBackupDate.setText(lastBackupDateStr == null ? getString(R.string.no_timed_backups_taken) : getString(R.string.last_backup) + lastBackupDateStr);
    }


    // Menu items
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}