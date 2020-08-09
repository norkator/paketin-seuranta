package com.nitramite.paketinseuranta;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.nitramite.utils.Backup;
import com.nitramite.utils.BackupUtils;
import com.nitramite.utils.SharedPreferencesUtils;
import com.nitramite.utils.dialogUtils;

public class BackupManager extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_manager);

        SharedPreferences sharedPreferences = SharedPreferencesUtils.getSharedPreferences(getApplicationContext());

        // Find views
        CheckBox timedBackupToggle = findViewById(R.id.timedBackupToggle);
        Button takeBackupBtn = findViewById(R.id.takeBackupBtn);
        Button restoreBackupBtn = findViewById(R.id.restoreBackupBtn);


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
                    dialogUtils.genericErrorDialog(
                            getApplicationContext(),
                            this.isFinishing(),
                            getString(R.string.main_menu_result),
                            getString(R.string.main_menu_taking_backup_success) + " " + backup.getLocation() + " " + backup.getFileName()
                    );
                } else {
                    dialogUtils.genericErrorDialog(getApplicationContext(), this.isFinishing(), getString(R.string.main_menu_error), getString(R.string.main_menu_taking_backup_failed));
                }
            }
        });

        restoreBackupBtn.setOnClickListener(view -> {
            if (hasPermission(BackupManager.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Backup backup = BackupUtils.restoreDatabase(BackupManager.this);
                if (backup.isSuccess()) {
                    Toast.makeText(BackupManager.this, R.string.main_menu_restore_successfull, Toast.LENGTH_LONG).show();
                    BackupManager.this.finish();
                } else {
                    dialogUtils.genericErrorDialog(getApplicationContext(), this.isFinishing(), getString(R.string.main_menu_error), getString(R.string.main_menu_restore_un_successfull));
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

}