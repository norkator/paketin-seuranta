package com.nitramite.paketinseuranta;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.nitramite.utils.Backup;
import com.nitramite.utils.BackupUtils;
import com.nitramite.utils.DialogUtils;
import com.nitramite.utils.SharedPreferencesUtils;

import org.jetbrains.annotations.NonNls;

import java.util.Objects;

public class BackupManager extends AppCompatActivity {


    //  Logging
    @NonNls
    private static final String TAG = "BackupManager";

    // Variables
    private DialogUtils dialogUtils = new DialogUtils();
    private static final int OPEN_DIRECTORY_REQUEST_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_manager);

        SharedPreferences sharedPreferences = SharedPreferencesUtils.getSharedPreferences(getApplicationContext());

        // Find views
        CheckBox timedBackupToggle = findViewById(R.id.timedBackupToggle);
        Button takeBackupBtn = findViewById(R.id.takeBackupBtn);
        Button restoreBackupBtn = findViewById(R.id.restoreBackupBtn);
        TextView lastBackupDate = findViewById(R.id.lastBackupDate);


        final String lastBackupDateStr = sharedPreferences.getString(Constants.SP_TIMED_BACKUP_LAST_DATE, null);
        lastBackupDate.setText(lastBackupDateStr == null ? getString(R.string.no_timed_backups_taken) : getString(R.string.last_backup) + lastBackupDateStr);


        timedBackupToggle.setChecked(sharedPreferences.getBoolean(Constants.SP_TIMED_BACKUP_ENABLED, false));
        timedBackupToggle.setOnCheckedChangeListener((compoundButton, b) -> {
            if (hasPermission(BackupManager.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                SharedPreferences setSharedPreferences = SharedPreferencesUtils.getSharedPreferences(getApplicationContext());
                SharedPreferences.Editor normalEditor = setSharedPreferences.edit();
                normalEditor.putBoolean(Constants.SP_TIMED_BACKUP_ENABLED, b);
                normalEditor.apply();
                Toast.makeText(this, getString(R.string.timed_back_up_toggle) + " " + (b ? getString(R.string.timed_backup_on) : getString(R.string.timed_backup_off)), Toast.LENGTH_SHORT).show();
            } else {
                timedBackupToggle.setChecked(false);
            }
        });


        takeBackupBtn.setOnClickListener(view -> {

            if (hasPermission(BackupManager.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Backup backup = BackupUtils.backupDatabase(BackupManager.this);
                if (backup.isSuccess()) {
                    backupSuccessDialog(this, this.isFinishing(),
                            getString(R.string.main_menu_result),
                            getString(R.string.main_menu_taking_backup_success) + " " + backup.getLocation(),
                            backup
                    );
                } else {
                    dialogUtils.genericErrorDialog(this, this.isFinishing(),
                            getString(R.string.main_menu_error),
                            getString(R.string.main_menu_taking_backup_failed) + " " + backup.getExceptionString());
                }
            }


        });

        restoreBackupBtn.setOnClickListener(view -> {
            if (hasPermission(BackupManager.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Backup backup = BackupUtils.restoreDatabase(BackupManager.this);
                if (backup.isSuccess()) {
                    Toast.makeText(BackupManager.this, R.string.main_menu_restore_successfull, Toast.LENGTH_LONG).show();
                    terminateApp();
                } else {
                    dialogUtils.genericErrorDialog(this, this.isFinishing(), getString(R.string.main_menu_error),
                            getString(R.string.main_menu_restore_un_successfull) + " " + backup.getExceptionString());
                }
            }
        });


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
            if (ContextCompat.checkSelfPermission(BackupManager.this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{permission}, 1);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_DIRECTORY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.i(TAG, Objects.requireNonNull(data.getDataString()));
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
                        /*
                        .setNeutralButton(R.string.open_bu_location, (dialog, which) -> {
                            openFolderLocation(backup);
                        })
                        */
                        .setIcon(R.mipmap.ps_logo_round)
                        .show();
            }
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        }
    }


    /**
     * Open file browser for backup file location
     *
     * @param backup object
     */
    public void openFolderLocation(Backup backup) {
        try {
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setDataAndType(backup.getUri(), "application/x-sqlite3");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            dialogUtils.genericErrorDialog(this, this.isFinishing(), getString(R.string.main_menu_error), e.toString());
        }
    }


    /**
     * Shut down app
     */
    private void terminateApp() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            finishAffinity();
            System.exit(0);
        }, 2000);
    }

}