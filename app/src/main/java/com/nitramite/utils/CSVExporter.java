package com.nitramite.utils;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

import com.nitramite.paketinseuranta.DatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CSVExporter {

    //  Logging
    private static final String TAG = "CSVExporter";

    // Export csv
    public String exportCSV(DatabaseHelper databaseHelper,
                            Boolean nameChecked, Boolean parcelCodeChecked, Boolean senderChecked, Boolean deliveryMethodChecked,
                            Boolean parcelAddDateChecked, Boolean parcelLastEventChecked, Boolean readyForPickupDateChecked,
                            Boolean productPageChecked) throws IOException {
        if (!FileUtils.isExternalStorageWritable()) {
            throw new IOException("Cannot write to external storage");
        }

        File csvDirectory = FileUtils.createDirIfNotExist(
                Environment.getExternalStorageDirectory() + "/PaketinSeuranta/CSV/");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
        String fileName = "archive_export_" + sdf.format(new Date()) + ".csv";

        File csvExportFile = new File(csvDirectory, fileName);
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
            csvWrite = new CSVWriter(new OutputStreamWriter(new FileOutputStream(csvExportFile), "UTF-8"));
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