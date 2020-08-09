package com.nitramite.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

public class Backup {

    private boolean success = false;
    private String exceptionString = "N/A";
    private String location = "N/A";
    private String fileName = "N/A";

    private ContentResolver contentResolver;
    private ContentValues contentValues;
    private Uri uri;

    public Backup() {
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setContentResolver(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public ContentResolver getContentResolver() {
        return contentResolver;
    }

    public void setContentValues(ContentValues contentValues) {
        this.contentValues = contentValues;
    }

    public ContentValues getContentValues() {
        return contentValues;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public Uri getUri() {
        return uri;
    }

    public void setExceptionString(String exceptionString) {
        this.exceptionString = exceptionString;
    }

    public String getExceptionString() {
        return exceptionString;
    }

}
