package com.nitramite.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.nitramite.paketinseuranta.Constants;
import com.nitramite.paketinseuranta.R;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.nitramite.paketinseuranta.Constants.DATABASE_NAME;

public class BackupUtils {

    @NonNls
    private static final String TAG = "BackupUtils";


    /**
     * Backup database
     *
     * @param context view context
     * @return Backup object
     */
    public static Backup backupDatabase(Context context) {
        Backup backup = new Backup();
        try {
            FileInputStream dbFileInputStream = getDBFileInputStream(context, backup);
            OutputStream dbFileOutputStream = getDBOutputStream(context, backup);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = dbFileInputStream.read(buffer)) > 0) {
                dbFileOutputStream.write(buffer, 0, length);
            }
            dbFileOutputStream.flush();
            dbFileOutputStream.close();
            dbFileInputStream.close();

            backup.setSuccess(true);
            return backup;
        } catch (IOException e) {
            backup.setExceptionString(e.toString());
            return backup;
        }
    }


    /**
     * Restore database
     *
     * @param context view context
     * @return Backup object
     */
    public static Backup restoreDatabase(Context context) {
        Backup backup = new Backup();
        try {
            File targetPathFile = getCleanedAppDBTargetPath(context);
            FileInputStream fileInputStream = getDBFileInputSteamFromExternalStorage(context);

            Log.i(TAG, targetPathFile.getPath());

            OutputStream output = new FileOutputStream(targetPathFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fileInputStream.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            output.flush();
            output.close();
            fileInputStream.close();

            backup.setSuccess(true);
            return backup;
        } catch (IOException e) {
            backup.setExceptionString(e.toString());
            return backup;
        }
    }


    /**
     * Get database file input stream object
     *
     * @param context Context
     * @return file input stream
     * @throws FileNotFoundException database file not found
     */
    private static FileInputStream getDBFileInputStream(Context context, Backup backup) throws FileNotFoundException {
        @SuppressLint("SdCardPath") final String databaseLocation = "/data/data/" + context.getApplicationContext().getPackageName() + "/databases/" + DATABASE_NAME;
        File databaseFile = new File(databaseLocation);
        backup.setFileName(DATABASE_NAME);
        return new FileInputStream(databaseFile);
    }


    /**
     * Get database file output stream object
     *
     * @param context Context
     * @return file output stream
     * @throws FileNotFoundException not found
     */
    private static OutputStream getDBOutputStream(Context context, Backup backup) throws NullPointerException, IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            String filepath = "";
            File dbFile = new File(context.getExternalFilesDir(null), DATABASE_NAME);

            filepath = dbFile.getAbsolutePath();
            Log.i(TAG, filepath);

            if (!dbFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dbFile.createNewFile();
            }
            backup.setLocation(filepath);
            backup.setUri(Uri.parse(filepath));
            return new FileOutputStream(dbFile);
        } else {

            @SuppressWarnings("deprecation") String downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
            File file = new File(downloadsDir, DATABASE_NAME);
            OutputStream outputStream = new FileOutputStream(file);
            backup.setLocation(context.getString(R.string.backup_location_external_storage_downloads_dir) + " " + DATABASE_NAME);
            return outputStream;
        }
    }


    /**
     * Get file input stream from db source path
     *
     * @param context Context
     * @return file input steam
     * @throws FileNotFoundException db file not found
     */
    private static FileInputStream getDBFileInputSteamFromExternalStorage(Context context) throws FileNotFoundException, NullPointerException {
        FileInputStream fileInputStream = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File file = new File(context.getExternalFilesDir((String) null), DATABASE_NAME);
            fileInputStream = new FileInputStream(file);
        } else {
            @SuppressWarnings("deprecation") String downloadsDir = Environment.getExternalStorageDirectory() + "/Download/";
            File file = new File(downloadsDir, DATABASE_NAME);
            fileInputStream = new FileInputStream(file);
        }
        return fileInputStream;
    }


    /**
     * Get file object of target db restore path, also cleans destination from old files
     *
     * @param context Context
     * @return file object
     */
    private static File getCleanedAppDBTargetPath(Context context) {
        @SuppressLint("SdCardPath") final String dbOnAppPath = "/data/data/" + context.getApplicationContext().getPackageName() + "/databases/";
        File file = new File(dbOnAppPath + DATABASE_NAME);
        if (file.exists()) {
            file.delete(); // Clean existing junk first from application
        }
        File dbShmFile = new File(dbOnAppPath + DATABASE_NAME + "-shm");
        if (dbShmFile.exists()) {
            dbShmFile.delete();
        }
        File dbWalFile = new File(dbOnAppPath + DATABASE_NAME + "-wal");
        if (dbWalFile.exists()) {
            dbWalFile.delete();
        }
        return file;
    }



    @SuppressWarnings("HardCodedStringLiteral")
    public static void SaveBackupDate(Context context, Calendar calendar) {
        Date now = calendar.getTime();
        SimpleDateFormat simpleDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        String strDt = simpleDate.format(now);
        SharedPreferences setSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor normalEditor = setSharedPreferences.edit();
        normalEditor.putString(Constants.SP_TIMED_BACKUP_LAST_DATE, strDt);
        normalEditor.apply();
        Log.i(TAG, "Backup is taken and date is saved: " + strDt);
    }


}
