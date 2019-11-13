package com.nitramite.utils;

import android.content.Context;
import android.os.Environment;
import com.nitramite.paketinseuranta.R;
import java.io.File;

public class FileUtils {

    // Get app directory
    public static String getAppDir(Context context){
        return context.getApplicationContext().getExternalFilesDir(null) + "/" + context.getApplicationContext().getString(R.string.app_name);
    }

    // Creates directory if does not exists
    public static File createDirIfNotExist(String path){
        File dir = new File(path);
        if( !dir.exists() ){
            dir.mkdir();
        }
        dir.getParentFile().mkdirs();
        return new File(path + "/");
    }

    // Checks if external storage is available for read and write
    static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    // Checks if external storage is available to at least read
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

} // End of class