/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.paketinseuranta;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.nitramite.adapters.AutoCompleteScenario;
import com.nitramite.courier.ParcelObject;
import com.nitramite.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.nitramite.paketinseuranta.Constants.DATABASE_NAME;

/**
 * Created by Martin on 11.1.2016.
 * Database handler, creates tables Parcels, Events, Image data.
 * Parcels: Parcel data storage
 * Events: stored tracking data, get via Parcel ID
 */
@SuppressWarnings("HardCodedStringLiteral")
public class DatabaseHelper extends SQLiteOpenHelper {

    // Logging
    private static final String TAG = "DatabaseHelper";

    // Variables
    private Context context;

    // For database updating
    private List<String> columnsParcels;
    private List<String> columnsEventsData;
    private List<String> columnsImages;
    private boolean upgrade = false;

    // DATABASE VERSION
    private static final int DATABASE_VERSION = 17;
    // 4  = v1.1.2
    // 5  = v1.1.7
    // 6  = v1.2.1 (Archive feature)
    // 7  = v1.2.8 (Package updated status)
    // 8  = v1.3.2 (Big changes on whole app.. tracking data)
    // 9  = v1.3.2 (Update fuck up, had to increment again to cause upgrade run again)
    // 10 = v1.3.4 More updates like original tracking code etc
    // 11 = v1.3.7 Sender and delivery method fields
    // 12 = v1.4.5 Parcel picture feature
    // 13 = v1.5.4 added parcel create date field
    // 14 = v1.8.0 added parcel additional note field
    // 15 = v1.9.2 added parcel product_page field
    // 16 = v1.9.8 added parcel order_date, manual_delivered_date fields
    // 17 = v2.7.15 added paid / unpaid feature for parcels

    // TABLE NAME'S
    private static final String PARCELS_TABLE = "Parcels";
    private static final String EVENTS_TABLE = "Events";
    private static final String IMAGES_TABLE = "Images";

    // -------------------------------------------------------------------

    // TABLE ROWS NAMES - For Parcel data
    private static final String KEY_ID = "id"; // ID OF ADD
    private static final String CARRIER = "carrier"; // CARRIER CODE, WHAT CARRIER PACKAGE
    private static final String CARRIERSTATUS = "carrierstatus"; // CARRIER STATUS, SETS WHERE TO LOOK
    private static final String TRACKINGCODE = "trackingcode"; // MAIN TRACKING CODE
    private static final String TRACKINGCODE2 = "trackingcode2"; // SECOND TRACKING CODE
    private static final String ERRANDCODE = "errandcode"; // ???
    private static final String PHASE = "phase"; // Package status code, example "IN_TRANSPORT"
    private static final String ESTIMATEDDELIVERYTIME = "estimateddeliverytime";
    private static final String NAME = "name";
    private static final String STREET = "street";
    private static final String POSTCODE = "postcode";
    private static final String CITY = "city";
    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";
    private static final String AVAILABILITY = "availability";
    private static final String LASTPICKUPDATE = "lastpickupdate";
    private static final String FI = "fi";
    private static final String SENDER = "sender";
    private static final String LOCKERCODE = "lockercode";
    private static final String EXTRASERVICES = "extraservices";
    private static final String WEIGHT = "weight";
    private static final String HEIGHT = "height";
    private static final String WIDTH = "width";
    private static final String DEPTH = "depth";
    private static final String VOLUME = "volume";
    private static final String DESTINATIONPOSTCODE = "destinationpostcode";
    private static final String DESTINATIONCITY = "destinationcity";
    private static final String DESTINATIONCOUNTRY = "destinationcountry";
    private static final String RECIPIENTSIGNATURE = "recipientsignature";
    private static final String CODAMOUNT = "codamount";
    private static final String CODCURRENCY = "codcurrency";
    private static final String TITLE = "title"; // User's own title for packet (31)
    private static final String PHASE_NUMBER = "phase_number"; // Phase to number for sorting
    private static final String IS_ARCHIVED = "is_archived"; // Is package archived
    private static final String LAST_UPDATE_STATUS = "last_update_status"; // When package is last updated
    private static final String ORIGINAL_TRACKING_CODE = "original_tracking_code"; // When package is added this has been it's original tracking code
    private static final String SENDER_TEXT = "sender_text"; // User settable sender info field (36)
    private static final String DELIVERY_METHOD = "delivery_method"; // Delivery method (user settable) (37)
    private static final String CREATE_DATE = "create_date"; // Parcel create date / when added date time
    private static final String ADDITIONAL_NOTE = "additional_note"; // Additional notes for parcel placed by user (39)
    private static final String PRODUCT_PAGE = "product_page"; // Parcel product page, website etc, user settable (40)
    private static final String ORDER_DATE = "order_date"; // User settable order date (selectable from calendar) (41)
    private static final String MANUAL_DELIVERED_DATE = "manual_delivered_date"; // User set parcel as delivered, this is set at that point with action time (42)
    private static final String PARCEL_PAID = "parcel_paid"; // User can set parcel as paid or unpaid, boolean like (43)

    // -------------------------------------------------------------------

    /* Database for events data */
    private static final String ID = "id";
    private static final String PARCEL_ID = "parcel_id";
    private static final String CARRIER_T = "carrier_t"; // ???
    private static final String DESCRIPTION = "description";
    private static final String TIMESTAMP = "timestamp";
    private static final String TIMESTAMP_SQLITE = "timestamp_sqlite"; // Used to sort on right order
    private static final String LOCATION_CODE = "location_code";
    private static final String LOCATION_NAME = "location_name";

    // -------------------------------------------------------------------

    /* Database for events data */
    private static final String IMAGE_ID = "id"; // Auto increment for table
    private static final String IMAGE_PARCEL_ID = "parcel_id"; // Parcel id
    private static final String IMAGE_DATA = "image_data"; // Image data as blob

    // -------------------------------------------------------------------

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        // ***BACKUP*** Backup old database stuff
        if (upgrade) {
            // Create events table if not exists
            db.execSQL("CREATE TABLE IF NOT EXISTS " + EVENTS_TABLE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, parcel_id TEXT, carrier_t TEXT, description TEXT, timestamp TEXT, timestamp_sqlite DATETIME, location_code TEXT, location_name TEXT)");
            // Create images table if not exists
            db.execSQL("CREATE TABLE IF NOT EXISTS " + IMAGES_TABLE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, parcel_id TEXT, image_data BLOB)");
            // Events data table alter
            columnsEventsData = GetColumns(db, EVENTS_TABLE);
            db.execSQL("ALTER TABLE " + EVENTS_TABLE + " RENAME TO TEMP_" + EVENTS_TABLE);
            // Parcels table alter
            columnsParcels = GetColumns(db, PARCELS_TABLE);
            db.execSQL("ALTER TABLE " + PARCELS_TABLE + " RENAME TO TEMP_" + PARCELS_TABLE);
            // Image table alter
            columnsImages = GetColumns(db, IMAGES_TABLE);
            db.execSQL("ALTER TABLE " + IMAGES_TABLE + " RENAME TO TEMP_" + IMAGES_TABLE);
        }

        // ***CREATE***  Create new Parcel table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + PARCELS_TABLE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, carrier TEXT, carrierstatus TEXT, trackingcode TEXT, trackingcode2 TEXT, errandcode TEXT, phase TEXT, " +
                "estimateddeliverytime TEXT, name TEXT, street TEXT, postcode TEXT, city TEXT, latitude TEXT, longitude TEXT, " +
                "availability TEXT, lastpickupdate TEXT, fi TEXT, sender TEXT, lockercode TEXT, extraservices TEXT, weight TEXT, height TEXT, width TEXT, depth TEXT, " +
                "volume TEXT, destinationpostcode TEXT, destinationcity TEXT, destinationcountry TEXT, recipientsignature TEXT, codamount TEXT, codcurrency TEXT, title TEXT, phase_number TEXT DEFAULT '0', " +
                "is_archived TEXT DEFAULT '0', last_update_status TEXT, original_tracking_code TEXT, sender_text TEXT, delivery_method TEXT, create_date TEXT, additional_note TEXT, product_page TEXT, " +
                "order_date TEXT, manual_delivered_date TEXT, parcel_paid TEXT)");
        // Create tracking data table
        db.execSQL("CREATE TABLE " + EVENTS_TABLE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, parcel_id TEXT, carrier_t TEXT, description TEXT, timestamp TEXT, timestamp_sqlite DATETIME, location_code TEXT, location_name TEXT)");
        // Create image data table
        db.execSQL("CREATE TABLE " + IMAGES_TABLE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, parcel_id TEXT, image_data BLOB)");


        // ***RESTORE***  Restore from old
        if (upgrade) {
            // Parcels
            columnsParcels.retainAll(GetColumns(db, PARCELS_TABLE));
            String parcelCols = join(columnsParcels, ",");
            db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s FROM TEMP_%s", PARCELS_TABLE, parcelCols, parcelCols, PARCELS_TABLE));
            db.execSQL("DROP TABLE TEMP_" + PARCELS_TABLE);
            // Tracking data
            columnsEventsData.retainAll(GetColumns(db, EVENTS_TABLE));
            String trackingCols = join(columnsEventsData, ",");
            db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s FROM TEMP_%s", EVENTS_TABLE, trackingCols, trackingCols, EVENTS_TABLE));
            db.execSQL("DROP TABLE TEMP_" + EVENTS_TABLE);
            // Images data
            columnsImages.retainAll(GetColumns(db, IMAGES_TABLE));
            String imagesCols = join(columnsImages, ",");
            db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s FROM TEMP_%s", IMAGES_TABLE, imagesCols, imagesCols, IMAGES_TABLE));
            db.execSQL("DROP TABLE TEMP_" + IMAGES_TABLE);
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        upgrade = true;
        //db.execSQL("DROP TABLE IF EXISTS " + PARCELS_TABLE);
        //db.execSQL("DROP TABLE IF EXISTS " + EVENTS_TABLE);
        onCreate(db);
    }


    // Returns this database as readable for exported
    public SQLiteDatabase getReadableDatabaseObject() {
        return this.getReadableDatabase();
    }


    // ---------------------------------------------------------------------------------------------


    /**
     * PARCELS DATA
     */

    // INSERT DATA
    @SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
    Long insertData(ParcelObject parcelObject) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Get datetime for insert
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        // Fetch insert clause
        ContentValues contentValues = new ContentValues();
        contentValues.put(CARRIER, parcelObject.getCarrier());
        contentValues.put(CARRIERSTATUS, parcelObject.getCarrierStatus());
        contentValues.put(TRACKINGCODE, parcelObject.getTrackingCode());
        contentValues.put(TRACKINGCODE2, parcelObject.getTrackingCode2());
        contentValues.put(ERRANDCODE, parcelObject.getErrandCode());
        contentValues.put(PHASE, parcelObject.getPhase());
        contentValues.put(ESTIMATEDDELIVERYTIME, parcelObject.getEstimatedDeliveryTime());
        contentValues.put(NAME, parcelObject.getPickupAddressName());
        contentValues.put(STREET, parcelObject.getPickupAddressStreet());
        contentValues.put(POSTCODE, parcelObject.getPickupAddressPostcode());
        contentValues.put(CITY, parcelObject.getPickupAddressCity());
        contentValues.put(LATITUDE, parcelObject.getPickupAddressLatitude());
        contentValues.put(LONGITUDE, parcelObject.getPickupAddressLongitude());
        contentValues.put(AVAILABILITY, parcelObject.getPickupAddressAvailability());
        contentValues.put(LASTPICKUPDATE, parcelObject.getLastPickupDate());
        contentValues.put(FI, ""); // No idea what this is/was
        contentValues.put(SENDER, parcelObject.getSender());
        contentValues.put(LOCKERCODE, parcelObject.getLockerCode());
        contentValues.put(EXTRASERVICES, parcelObject.getExtraServices());
        contentValues.put(WEIGHT, parcelObject.getWeight());
        contentValues.put(HEIGHT, parcelObject.getHeight());
        contentValues.put(WIDTH, parcelObject.getWidth());
        contentValues.put(DEPTH, parcelObject.getDepth());
        contentValues.put(VOLUME, parcelObject.getVolume());
        contentValues.put(DESTINATIONPOSTCODE, parcelObject.getDestinationPostcode());
        contentValues.put(DESTINATIONCITY, parcelObject.getDestinationCity());
        contentValues.put(DESTINATIONCOUNTRY, parcelObject.getDestinationCountry());
        contentValues.put(RECIPIENTSIGNATURE, parcelObject.getRecipientSignature());
        contentValues.put(CODAMOUNT, parcelObject.getCodAmount());
        contentValues.put(CODCURRENCY, parcelObject.getCodCurrency());
        contentValues.put(TITLE, parcelObject.getTitle());
        contentValues.put(LAST_UPDATE_STATUS, parcelObject.getLastUpdateStatus());
        contentValues.put(CREATE_DATE, dateFormat.format(date)); // Insert datetime
        contentValues.put(SENDER_TEXT, parcelObject.getSenderText());
        contentValues.put(DELIVERY_METHOD, parcelObject.getDeliveryMethod());
        contentValues.put(ADDITIONAL_NOTE, parcelObject.getAdditionalNote());
        if (!parcelObject.getOriginalTrackingCode().equals("")) {
            contentValues.put(ORIGINAL_TRACKING_CODE, parcelObject.getOriginalTrackingCode());
        }
        if (!parcelObject.getOrderDate().equals("")) {
            contentValues.put(ORDER_DATE, parcelObject.getOrderDate());
        }
        if (!parcelObject.getDeliveryDate().equals("")) {
            contentValues.put(MANUAL_DELIVERED_DATE, parcelObject.getDeliveryDate());
        }
        contentValues.put(PRODUCT_PAGE, parcelObject.getProductPage());
        return db.insert(PARCELS_TABLE, null, contentValues);
    }


    // Get all package data
    Cursor getAllData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + PARCELS_TABLE + " WHERE " + IS_ARCHIVED + " = '0'" + " AND " + TRACKINGCODE + " != ''" + " ORDER BY " + PHASE_NUMBER + " DESC", null);
        return res;
    }


    /**
     * Get all package data
     *
     * @param updateFailedFirst orders with failed first if provided
     * @param parcelId          only return details for this id package if this value is provided
     * @return return details
     */
    public Cursor getPackagesDataForParcelService(final boolean updateFailedFirst, final String parcelId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p." + KEY_ID + ", p." + CARRIER + ", p." + CARRIERSTATUS + ", p." + TRACKINGCODE + ", p." + PHASE + ", " +
                "(SELECT " + DESCRIPTION + " FROM " + EVENTS_TABLE + " WHERE " + PARCEL_ID + " = " + "p." + KEY_ID + " ORDER BY " + TIMESTAMP_SQLITE + " DESC LIMIT 1" + ")" + " AS " + DESCRIPTION +
                ", p." + TITLE +
                " FROM " + PARCELS_TABLE + " AS p" +
                " WHERE " + "p." + IS_ARCHIVED + " = '0'" + " AND " + "p." + TRACKINGCODE + " != ''" +
                (parcelId.length() > 0 ? " AND " + KEY_ID + " = " + parcelId + "" : "") + // Only one package if parcelId is provided
                " ORDER BY " + (updateFailedFirst ? "p." + LAST_UPDATE_STATUS : "p." + PHASE_NUMBER) + " DESC";
        return db.rawQuery(query, null);
    }


    // Get all package data
    public Cursor getAllDataWithLatestEvent() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p." + KEY_ID + ", " + "p." + TRACKINGCODE + ", " +
                "p." + PHASE + ", " + "p." + FI + ", " + "p." + TITLE + ", " + "p." + LAST_UPDATE_STATUS + ", " +
                "(SELECT " + DESCRIPTION + " FROM " + EVENTS_TABLE + " WHERE " + PARCEL_ID + " = " + "p." + KEY_ID + " ORDER BY " + TIMESTAMP_SQLITE + " DESC LIMIT 1" + ")" + " AS " + DESCRIPTION +
                ", " + "p." + CARRIER + ", " + "p." + SENDER_TEXT + ", " + "p." + DELIVERY_METHOD + ", " + "p." + ADDITIONAL_NOTE + ", " + "p." + CREATE_DATE +
                ", " + "p." + LASTPICKUPDATE + ", " +
                "(SELECT " + TIMESTAMP + " FROM " + EVENTS_TABLE + " WHERE " + PARCEL_ID + " = " + "p." + KEY_ID + " ORDER BY " + TIMESTAMP_SQLITE + " DESC LIMIT 1" + ")" + " AS " + "latestParcelEvent" +
                " FROM " + PARCELS_TABLE + " AS p " +
                " WHERE " + "p." + IS_ARCHIVED + " = '0'" + " ORDER BY " + "p." + PHASE_NUMBER + " DESC";
        Log.i(TAG, query);
        return db.rawQuery(query, null);
    }


    // Is current ID package archived
    public Boolean isCurrentPackageArchived(final String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT " + "COUNT(*)" + " FROM " + PARCELS_TABLE + " WHERE " + KEY_ID + " = " + id + " AND " + IS_ARCHIVED + " = '1'", null);
        res.moveToFirst();
        final Integer count = res.getInt(0);
        res.close();
        return count > 0;
    }


    // Get all archive package data
    public Cursor getAllArchiveData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + PARCELS_TABLE + " WHERE " + IS_ARCHIVED + " = '1'" + " ORDER BY " + KEY_ID + " DESC", null);
        return res;
    }


    // Get all archive data with latest event
    public Cursor getAllArchiveDataWithLatestEvent(String searchStr) {

        Log.i(TAG, " AND (" + TITLE + " LIKE '" + searchStr + "' OR "
                + TRACKINGCODE + " LIKE '" + searchStr + "' OR " + SENDER_TEXT + " LIKE '" + searchStr + "' OR " + DELIVERY_METHOD + " LIKE '" + searchStr + "'" + ")");

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT p." + KEY_ID + ", " + "p." + TRACKINGCODE + ", " +
                "p." + PHASE + ", " + "p." + FI + ", " + "p." + TITLE + ", " + "p." + LAST_UPDATE_STATUS + ", " +
                "(SELECT " + DESCRIPTION + " FROM " + EVENTS_TABLE + " WHERE " + PARCEL_ID + " = " + "p." + KEY_ID + " ORDER BY " + TIMESTAMP_SQLITE + " DESC LIMIT 1" + ")" + " AS " + DESCRIPTION +
                ", " + "p." + CARRIER + ", " + "p." + SENDER_TEXT + ", " + "p." + DELIVERY_METHOD + ", " + "p." + ADDITIONAL_NOTE + ", " + "p." + CREATE_DATE +
                " FROM " + PARCELS_TABLE + " AS p " +
                " WHERE " + "p." + IS_ARCHIVED + " = '1'"

                // Search str if exists
                + (searchStr != null ? " AND (" + TITLE + " LIKE '%" + searchStr + "%' OR "
                + TRACKINGCODE + " LIKE '%" + searchStr + "%' OR " + SENDER_TEXT + " LIKE '%" + searchStr + "%' OR " + DELIVERY_METHOD + " LIKE '%" + searchStr + "%'" + ")" : "")

                + " ORDER BY " + "p." + KEY_ID + " DESC", null);
        return res;
    }


    // Get Archive data for CSV export
    @SuppressWarnings("StringBufferReplaceableByString")
    public Cursor getArchiveForCSVExport(Boolean nameChecked, Boolean parcelCodeChecked, Boolean senderChecked, Boolean deliveryMethodChecked,
                                         Boolean parcelAddDateChecked, Boolean parcelLastEventChecked, Boolean readyForPickupDateChecked, Boolean productPageChecked) {
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder stringBuilder = new StringBuilder();
        // Append select items
        stringBuilder
                .append(nameChecked ? "p." + TITLE + "," : "")
                .append(parcelCodeChecked ? "p." + TRACKINGCODE + "," : "")
                .append(senderChecked ? "p." + SENDER_TEXT + "," : "")
                .append(deliveryMethodChecked ? "p." + DELIVERY_METHOD + "," : "")
                .append(parcelAddDateChecked ? "p." + CREATE_DATE + "," : "")
                .append(parcelLastEventChecked ? "(SELECT " + DESCRIPTION + " FROM " + EVENTS_TABLE + " WHERE " + PARCEL_ID + " = " + "p." + KEY_ID + " ORDER BY " + TIMESTAMP_SQLITE + " DESC LIMIT 1" + ") AS parcel_last_event" + "," : "")
                .append(readyForPickupDateChecked ? "(SELECT " + TIMESTAMP + " FROM " + EVENTS_TABLE + " WHERE " + PARCEL_ID + " = " + "p." + KEY_ID + " ORDER BY " + TIMESTAMP_SQLITE + " DESC LIMIT 1" + ") AS ready_for_pickup_date" + "," : "")
                .append(productPageChecked ? "p." + PRODUCT_PAGE + "," : "");
        // Fetch query
        String queryString = stringBuilder.toString();
        if (queryString.charAt(queryString.length() - 1) == ',') {
            queryString = queryString.substring(0, queryString.length() - 1); // Remove last , char
        }
        // Run query
        Cursor res = db.rawQuery("SELECT " + queryString +
                " FROM " + PARCELS_TABLE + " AS p " +
                " WHERE " + "p." + IS_ARCHIVED + " = '1'" + " ORDER BY " + "p." + KEY_ID + " DESC", null);
        Log.i(TAG, queryString);
        return res;
    }


    // Check for package existence both normal and archive lists
    public Boolean checkForPackageExistence(final String trackingCode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + PARCELS_TABLE + " WHERE " + TRACKINGCODE + " = ?", new String[]{trackingCode});
        cursor.moveToFirst();
        final Boolean exists = cursor.getInt(0) > 0;
        cursor.close(); // Close cursor
        return exists;
    }


    // Get all package data where package is on ready to pickup state
    Cursor getAllDataWhereReadyToPickupState() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + PARCELS_TABLE + " WHERE " + IS_ARCHIVED + " = '0'" + " AND " + PHASE_NUMBER + " = 3" + " ORDER BY " + PHASE_NUMBER + " DESC", null);
        return res;
    }


    // Get id with tracking code
    public Cursor getIdByTrackingcode(String trackingcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + PARCELS_TABLE + " WHERE " + "trackingcode = ?", new String[]{String.valueOf(trackingcode)});
        return res;
    }


    // Get data by id
    Cursor getDataByID(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + PARCELS_TABLE + " WHERE " + "id = ?" /*+ " AND " + TRACKINGCODE + " != ''"*/, new String[]{String.valueOf(id)});
        return res;
    }


    // Get package name for parcel without tracking code
    String getParcelTitleByID(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT " + TITLE + " FROM " + PARCELS_TABLE + " WHERE " + "id = ?" + " LIMIT 1", new String[]{String.valueOf(id)});
        res.moveToFirst();
        String title = res.getString(0);
        res.close();
        db.close();
        return title;
    }


    // Get status information for widget
    String getWidgetStatusInformation() {
        String status = "";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT COUNT(*) FROM " + PARCELS_TABLE + " WHERE " + PHASE_NUMBER + " = ?", new String[]{String.valueOf(PhaseNumber.PHASE_INT_READY_FOR_PICKUP)});
        res.moveToFirst();
        if (res.getCount() > 0) {
            if (res.getInt(0) != 0) {
                status += "Haettavissa: " + res.getString(0);
                if (res.getInt(0) > 1) {
                    status += " pakettia. ";
                } else {
                    status += " paketti. ";
                }
            }
        }
        res = db.rawQuery("SELECT COUNT(*) FROM " + PARCELS_TABLE + " WHERE " + PHASE + " = " + String.valueOf(PhaseNumber.PHASE_INT_IN_TRANSPORT) + " AND " + IS_ARCHIVED + " = '0'", null);
        res.moveToFirst();
        if (res.getCount() > 0) {
            if (res.getInt(0) != 0) {
                status += "Lähellä " + res.getString(0);
                if (res.getInt(0) > 1) {
                    status += " pakettia. ";
                } else {
                    status += " paketti. ";
                }
            }
        }
        res.close();
        if (status.length() == 0) {
            status = "Ei lähellä olevia paketteja";
        }
        return status;
    }


    // Update data
    public boolean updateData(String id, String carrierCode, String carrierStatus, String trackingCode, ParcelObject parcelObject, String lastUpdateStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ID, id);
        contentValues.put(CARRIER, carrierCode);
        contentValues.put(CARRIERSTATUS, carrierStatus);
        contentValues.put(TRACKINGCODE, trackingCode);
        contentValues.put(TRACKINGCODE2, parcelObject.getParcelCode2());
        contentValues.put(ERRANDCODE, parcelObject.getErrandCode());
        contentValues.put(PHASE, parcelObject.getPhase());
        contentValues.put(ESTIMATEDDELIVERYTIME, parcelObject.getEstimatedDeliveryTime());
        contentValues.put(NAME, parcelObject.getPickupAddressName());
        contentValues.put(STREET, parcelObject.getPickupAddressStreet());
        contentValues.put(POSTCODE, parcelObject.getPickupAddressPostcode());
        contentValues.put(CITY, parcelObject.getPickupAddressCity());
        contentValues.put(LATITUDE, parcelObject.getPickupAddressLatitude());
        contentValues.put(LONGITUDE, parcelObject.getPickupAddressLongitude());
        contentValues.put(AVAILABILITY, parcelObject.getPickupAddressAvailability());
        contentValues.put(LASTPICKUPDATE, parcelObject.getLastPickupDate());
        contentValues.put(FI, parcelObject.getProduct());
        contentValues.put(SENDER, parcelObject.getSender());
        if (!parcelObject.getLockerCode().equals("") && !parcelObject.getLockerCode().equals("null")) {
            contentValues.put(LOCKERCODE, parcelObject.getLockerCode());
        }
        contentValues.put(EXTRASERVICES, parcelObject.getExtraServices());
        contentValues.put(WEIGHT, parcelObject.getWeight());
        contentValues.put(HEIGHT, parcelObject.getHeight());
        contentValues.put(WIDTH, parcelObject.getWidth());
        contentValues.put(DEPTH, parcelObject.getDepth());
        contentValues.put(VOLUME, parcelObject.getVolume());
        contentValues.put(DESTINATIONPOSTCODE, parcelObject.getDestinationPostcode());
        contentValues.put(DESTINATIONCITY, parcelObject.getDestinationCity());
        contentValues.put(DESTINATIONCOUNTRY, parcelObject.getDestinationCountry());
        contentValues.put(RECIPIENTSIGNATURE, parcelObject.getRecipientSignature());
        contentValues.put(CODAMOUNT, parcelObject.getCodAmount());
        contentValues.put(CODCURRENCY, parcelObject.getCodCurrency());
        contentValues.put(PHASE_NUMBER, parcelObject.getPhaseToNumber());
        contentValues.put(LAST_UPDATE_STATUS, lastUpdateStatus);
        db.update(PARCELS_TABLE, contentValues, " id = ?", new String[]{id});
        db.close();
        return true;
    }


    // Update phase number (used with ordering of main menu list)
    boolean updatePhaseNumber(String id, String phaseNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ID, id);
        contentValues.put(PHASE_NUMBER, phaseNumber);
        db.update(PARCELS_TABLE, contentValues, " id = ?", new String[]{id});
        db.close();
        return true;
    }


    /**
     * Better method for updating both phase information
     *
     * @param id                id of package
     * @param phaseNumberString number and string phases
     */
    public void updatePhaseNumberAndPhaseString(final String id, final PhaseNumberString phaseNumberString) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ID, id);
        contentValues.put(PHASE_NUMBER, phaseNumberString.getPhaseNumber());
        contentValues.put(PHASE, phaseNumberString.getPhaseString());
        db.update(PARCELS_TABLE, contentValues, " id = ?", new String[]{id});
        db.close();
    }


    // Update archive boolean
    boolean updateArchived(String id, Boolean isArchived) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ID, id);
        if (isArchived) {
            contentValues.put(IS_ARCHIVED, "1");
        } else {
            contentValues.put(IS_ARCHIVED, "0");
        }
        db.update(PARCELS_TABLE, contentValues, " id = ?", new String[]{id});
        db.close();
        return true;
    }


    // Delete data with id
    public boolean deletePackageData(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(PARCELS_TABLE, KEY_ID + " = ?", new String[]{id});    // Delete parcel data
        db.delete(EVENTS_TABLE, PARCEL_ID + " = ?", new String[]{id});  // Delete parcel event data
        db.close();
        return true;
    }


    // Clear parcel events data
    boolean clearParcelEventsData(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(EVENTS_TABLE, PARCEL_ID + " = ?", new String[]{id});  // Clear parcel event data
        db.execSQL("UPDATE " + PARCELS_TABLE + " SET " + PHASE + " = '', " + PHASE_NUMBER + " = '0' " + " WHERE id = " + id);
        db.close();
        return true;
    }


    // Set package as delivered with id
    void updatePackageAsDelivered(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + PARCELS_TABLE + " SET " + PHASE + " = 'DELIVERED', " + PHASE_NUMBER + " = '" + 4 + "', " +
                MANUAL_DELIVERED_DATE + " = '" + Utils.getCurrentTimeStampSqliteString() + "'" + " WHERE id = " + id);
        db.close();
    }


    // Set package title
    public boolean updateTitle(String id, String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + PARCELS_TABLE + " SET title = '" + title + "' WHERE id = " + id);
        db.close();
        return true;
    }


    // Returns package current title
    public String getTitle(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT " + TITLE + " FROM " + PARCELS_TABLE + " WHERE " + ID + " = " + id, null);
        res.moveToFirst();
        final String currentTitle = res.getString(0);
        res.close();
        db.close();
        return currentTitle;
    }


    // Set locker code
    public boolean updateLockerCode(String id, String lockerCode) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + PARCELS_TABLE + " SET " + LOCKERCODE + " = '" + lockerCode + "' WHERE id = " + id);
        db.close();
        return true;
    }


    // Update carrier (change parcel carrier)
    public boolean updateCarrierCode(String id, Integer carrierCode) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + PARCELS_TABLE + " SET " + CARRIER + " = '" + carrierCode + "' WHERE id = " + id);
        db.close();
        return true;
    }


    // Update last update status
    public void updateLastUpdateStatus(final String id, final String lastUpdateStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + PARCELS_TABLE + " SET " + LAST_UPDATE_STATUS + " = '" + lastUpdateStatus + "' WHERE id = " + id);
        db.close();
    }


    // Returns package current title
    public String getParcelCurrentTrackingCode(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT " + TRACKINGCODE + " FROM " + PARCELS_TABLE + " WHERE " + ID + " = " + id, null);
        res.moveToFirst();
        final String currentTrackingCode = res.getString(0);
        res.close();
        db.close();
        return currentTrackingCode;
    }


    // Set new tracking code
    public boolean updateParcelTrackingCode(String id, String trackingCode) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + PARCELS_TABLE + " SET " + TRACKINGCODE + " = '" + trackingCode + "' WHERE id = " + id);
        db.close();
        return true;
    }


    // Get data for package data edit
    public Cursor getEditPackageData(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT " + TITLE + ", " + LOCKERCODE + ", " + CARRIER + ", " + TRACKINGCODE + ", " + SENDER_TEXT + ", " +
                DELIVERY_METHOD + ", " + ADDITIONAL_NOTE + ", " + PRODUCT_PAGE + ", " + ORDER_DATE + ", " + MANUAL_DELIVERED_DATE +
                " FROM " + PARCELS_TABLE + " WHERE " + ID + " = " + id, null);
    }

    // Update data
    public boolean updateEditPackageData(ParcelObject parcelObject) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(TITLE, parcelObject.getTitle());
        contentValues.put(LOCKERCODE, parcelObject.getLockerCode());
        contentValues.put(TRACKINGCODE, parcelObject.getTrackingCode());
        contentValues.put(SENDER_TEXT, parcelObject.getSenderText());
        contentValues.put(DELIVERY_METHOD, parcelObject.getDeliveryMethod());
        contentValues.put(ADDITIONAL_NOTE, parcelObject.getAdditionalNote());
        if (!parcelObject.getOriginalTrackingCode().equals("")) {
            contentValues.put(ORIGINAL_TRACKING_CODE, parcelObject.getOriginalTrackingCode());
        }
        if (!parcelObject.getOrderDate().equals("")) {
            contentValues.put(ORDER_DATE, parcelObject.getOrderDate());
        }
        if (!parcelObject.getDeliveryDate().equals("")) {
            contentValues.put(MANUAL_DELIVERED_DATE, parcelObject.getDeliveryDate());
        }
        contentValues.put(PRODUCT_PAGE, parcelObject.getProductPage());
        db.update(PARCELS_TABLE, contentValues, " id = ?", new String[]{parcelObject.getId()});
        db.close();
        return true;
    }


    /**
     * Get used deliver methods words
     *
     * @return ArrayList list
     */
    public ArrayList<String> getAutocompleteWordsForScenario(AutoCompleteScenario autoCompleteScenario, String inputWord) {
        ArrayList<String> words = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = null;
        switch (autoCompleteScenario) {
            case SENDER:
                res = db.rawQuery("SELECT DISTINCT " + SENDER_TEXT + " FROM " + PARCELS_TABLE + " WHERE " + SENDER_TEXT + " LIKE '%" + inputWord + "%'", null);
                break;
            case DELIVERY_METHOD:
                res = db.rawQuery("SELECT DISTINCT " + DELIVERY_METHOD + " FROM " + PARCELS_TABLE + " WHERE " + DELIVERY_METHOD + " LIKE '%" + inputWord + "%'", null);
                break;
        }
        if (res != null) {
            while (res.moveToNext()) {
                words.add(res.getString(0));
            }
            res.close();
        }
        return words;
    }


    // ---------------------------------------------------------------------------------------------

    /**
     * TRACKING DATA
     */

    // Insert tracking data in a way that data is first verified not to exist
    public boolean insertTrackingData(String parcel_id, String carrier_t, String description, String timeStamp, String timeStampSQLite, String locationCode, String locationName) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Cursor res = db.rawQuery("SELECT COUNT(*) FROM " + EVENTS_TABLE + " WHERE " + PARCEL_ID + " = '"+ parcel_id +"'" + " AND " + DESCRIPTION + " = '" + description +"'", null); // Did not insert all data
        Cursor res = db.rawQuery(
                "SELECT COUNT(*) FROM " + EVENTS_TABLE + " WHERE " + PARCEL_ID + " = ? AND " +
                        DESCRIPTION + " = ? " + " AND " +
                        LOCATION_CODE + " = ? " + " AND " +
                        TIMESTAMP + " = ?" // 10.12.2018 added this to check
                , new String[]{parcel_id, description, locationCode, timeStamp});
        res.moveToFirst();
        Log.i(TAG, "Package with id: " + parcel_id + " returned event db count of: " + res.getInt(0));
        if (res.getInt(0) <= 0) {
            Log.i(TAG, "Inserting row: " + description);
            ContentValues contentValues = new ContentValues();
            contentValues.put(PARCEL_ID, parcel_id);
            contentValues.put(CARRIER_T, carrier_t);
            contentValues.put(DESCRIPTION, description);
            contentValues.put(TIMESTAMP, timeStamp);
            contentValues.put(TIMESTAMP_SQLITE, timeStampSQLite);
            contentValues.put(LOCATION_CODE, locationCode);
            contentValues.put(LOCATION_NAME, locationName);
            long result = db.insert(EVENTS_TABLE, null, contentValues);
            return result != -1;
        }
        res.close();
        return false;
    }


    // Same than below one but return everything
    Cursor getAllTrackingData(String parcelID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + EVENTS_TABLE + " WHERE " + PARCEL_ID + " = " + parcelID +
                " ORDER BY " + TIMESTAMP_SQLITE + " DESC", null);
        return res;
    }


    // Returns parcel events for selected parcel id
    Cursor getAllEventsDataWithParcelID(String parcelID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT " + ID + ", " + PARCEL_ID + ", " + CARRIER_T + ", " + DESCRIPTION + ", " + TIMESTAMP + ", " + LOCATION_CODE + ", " + LOCATION_NAME +
                " FROM " + EVENTS_TABLE + " WHERE " + PARCEL_ID + " = " + parcelID + " ORDER BY " + TIMESTAMP_SQLITE + " DESC", null);
        return res;
    }


    // Return parcel latest event for parcel id
    public String getLatestParcelEvent(String parcelID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT " + DESCRIPTION + " FROM " + EVENTS_TABLE + " WHERE " + PARCEL_ID + " = " + parcelID + " ORDER BY " + TIMESTAMP_SQLITE + " DESC LIMIT 1", null);
        res.moveToFirst();
        final String latestParcelEvent = res.getString(0);
        res.close();
        db.close();
        return latestParcelEvent;
    }


    /**
     * Get approximate delivery time based on events
     * if shipping_date is set, use that as first event
     * if order_date is set by user, will use that as first point
     *
     * @param parcelID id of parcel
     * @return String value of delivery time days
     */
    String getApproximateDeliveryTime(String parcelID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery(
                "SELECT " +
                        "CASE " +
                        "WHEN p.order_date IS NOT NULL AND p.manual_delivered_date IS NOT NULL THEN CAST(julianday(p.manual_delivered_date) - (julianday(p.order_date)) AS Integer) " + // Manual delivered date - Order date
                        "WHEN p.order_date IS NOT NULL THEN CAST(julianday(MAX(e.timestamp_sqlite)) - (julianday(p.order_date)) AS Integer) " + // Last event time - Order date
                        "WHEN p.create_date < MIN(e.timestamp_sqlite) THEN CAST(julianday(MAX(e.timestamp_sqlite)) - (julianday(p.create_date)) AS Integer) " + // // Last event time - parcel create time
                        "ELSE CAST((julianday(MAX(e.timestamp_sqlite)) - julianday(MIN(e.timestamp_sqlite))) AS Integer) END AS dd " +  // Last event time - Newest event time
                        "FROM " + PARCELS_TABLE + " AS p " +
                        "LEFT JOIN " + EVENTS_TABLE + " AS e ON e.parcel_id = p.id " +
                        "WHERE p.id = " + parcelID + " and p.phase_number = 4 " +
                        "ORDER BY e.timestamp_sqlite DESC",
                null);
        res.moveToFirst();
        final String deliveryDays = res.getString(0);
        res.close();
        db.close();
        return deliveryDays;
    }


    public boolean deleteAllTrackingDataWithParcelID(String parcelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + EVENTS_TABLE + " WHERE parcel_id = ?", new String[]{parcelID});
        db.close();
        return true;
    }


    // ---------------------------------------------------------------------------------------------
    /* Image table */

    // Insert image data into image data table
    boolean insertImageData(String parcel_id, byte[] imageData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(PARCEL_ID, parcel_id);
        contentValues.put(IMAGE_DATA, imageData);
        long result = db.insert(IMAGES_TABLE, null, contentValues);
        Log.i(TAG, contentValues.toString());
        return result != -1;
    }


    Cursor loadParcelImages(final String parcelID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT " + IMAGE_ID + ", " + IMAGE_DATA + " FROM " + IMAGES_TABLE + " WHERE " + PARCEL_ID + " = " + parcelID, null);
        return res;
    }


    boolean deleteParcelImage(String imageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + IMAGES_TABLE + " WHERE " + IMAGE_ID + " = ?", new String[]{imageId});
        db.close();
        return true;
    }


    // ---------------------------------------------------------------------------------------------
    /* Database upgrade script */

    private static List<String> GetColumns(SQLiteDatabase db, String tableName) {
        List<String> ar = null;
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 1", null);
            if (c != null) {
                ar = new ArrayList<>(Arrays.asList(c.getColumnNames()));
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return ar;
    }

    public static String join(List<String> list, String delim) {
        StringBuilder buf = new StringBuilder();
        int num = list.size();
        for (int i = 0; i < num; i++) {
            if (i != 0)
                buf.append(delim);
            buf.append(list.get(i));
        }
        return buf.toString();
    }

    // ---------------------------------------------------------------------------------------------

} // End of class