package com.nitramite.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.nitramite.paketinseuranta.DatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("HardCodedStringLiteral")
public class CSVExporter {

    //  Logging
    private static final String TAG = CSVExporter.class.getSimpleName();

    public static final String CSV_OLD_DIR = Environment.getExternalStorageDirectory() + "/PaketinSeuranta/CSV/";

    // Export csv
    public String exportCSV(Context context, DatabaseHelper databaseHelper,
                            Boolean nameChecked, Boolean parcelCodeChecked, Boolean senderChecked, Boolean deliveryMethodChecked,
                            Boolean parcelAddDateChecked, Boolean parcelLastEventChecked, Boolean readyForPickupDateChecked,
                            Boolean productPageChecked) throws IOException {
        if (!FileUtils.isExternalStorageWritable()) {
            throw new IOException("Cannot write to external storage");
        }

        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
        String fileName = "archive_export_" + sdf.format(new Date()) + ".csv";
        File csvExportFile = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            csvExportFile = new File(context.getExternalFilesDir(null), fileName);
        } else {
            File csvDirectory = FileUtils.createDirIfNotExist(CSV_OLD_DIR);
            csvExportFile = new File(csvDirectory, fileName);
        }


        boolean success = csvExportFile.createNewFile();
        if (!success) {
            throw new IOException("Failed to create the cvs file");
        }
        final boolean result = writeCsv(csvExportFile, databaseHelper, nameChecked, parcelCodeChecked, senderChecked, deliveryMethodChecked,
                parcelAddDateChecked, parcelLastEventChecked, readyForPickupDateChecked, productPageChecked
        );
        return result ? csvExportFile.getName() : null;
    }


    // Write csv
    private static boolean writeCsv(File csvExportFile, DatabaseHelper databaseHelper,
                                    Boolean nameChecked, Boolean parcelCodeChecked, Boolean senderChecked, Boolean deliveryMethodChecked,
                                    Boolean parcelAddDateChecked, Boolean parcelLastEventChecked, Boolean readyForPickupDateChecked,
                                    Boolean productPageChecked) {
        CSVWriter csvWrite = null;
        try {
            // Init writer
            csvWrite = new CSVWriter(new OutputStreamWriter(new FileOutputStream(csvExportFile), StandardCharsets.UTF_8));
            // Prepare cursor
            Cursor cursor = databaseHelper.getArchiveForCSVExport(nameChecked, parcelCodeChecked, senderChecked, deliveryMethodChecked,
                    parcelAddDateChecked, parcelLastEventChecked, readyForPickupDateChecked, productPageChecked);
            // Write column names once
            csvWrite.writeNext(cursor.getColumnNames());
            // Write data
            while (cursor.moveToNext()) {
                int columns = cursor.getColumnCount();
                String[] columnArr = new String[columns];
                for (int i = 0; i < columns; i++) {
                    String itemStr = cursor.getString(i);
                    if (itemStr != null) {
                        if (itemStr.length() > 1) {
                            if (itemStr.charAt(itemStr.length() - 1) == ' ') {
                                itemStr = itemStr.substring(0, itemStr.length() - 1); // Trim away white spaces
                            }
                        }
                    }
                    columnArr[i] = itemStr;
                }
                csvWrite.writeNext(columnArr);
            }
            return true;
        } catch (Exception e) {
            Log.i(TAG, e.toString());
            return false;
        } finally {
            if (csvWrite != null) {
                try {
                    csvWrite.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

} // End of class