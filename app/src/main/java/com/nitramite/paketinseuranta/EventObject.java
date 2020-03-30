package com.nitramite.paketinseuranta;

// Parcel event object
public class EventObject {

    // Variables
    private String description;             // Event description
    private String timeStamp;               // Event timestamp
    private String timeStampSQLiteFormat;   // SQLite suitable timestamp conversion ('2007-01-01 10:00:00')
    private String locationCode;            // Event location code
    private String locationName;            // Event locationName
    private String time;                    // Time only if required on parsing purposes

    public EventObject(final String description, final String timeStamp, final String timeStampSQLiteFormat, final String locationCode, final String locationName) {
        this.description = description;
        this.timeStamp = timeStamp;
        this.timeStampSQLiteFormat = timeStampSQLiteFormat;
        this.locationCode = locationCode;
        this.locationName = locationName;
    }

    // ---------------------------------------------------------------------------------------------
    // Getters

    public String getDescription() {
        return this.description;
    }

    public String getTimeStamp() {
        return this.timeStamp;
    }

    public String getTimeStampSQLiteFormat() {
        return this.timeStampSQLiteFormat;
    }

    public String getLocationCode() {
        return this.locationCode;
    }

    public String getLocationName() {
        return this.locationName;
    }

    public String getTime() {
        return this.time;
    }

    // ---------------------------------------------------------------------------------------------
    // Setters

    public void setTime(String time) {
        this.time = time;
    }

    // ---------------------------------------------------------------------------------------------

} // End of class
