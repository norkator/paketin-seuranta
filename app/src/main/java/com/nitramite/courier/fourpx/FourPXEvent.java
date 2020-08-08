/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.courier.fourpx;

import com.google.gson.annotations.SerializedName;
import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.DateTimeUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@SuppressWarnings("HardCodedStringLiteral")
public class FourPXEvent {

    @SerializedName("tkCategoryCode")
    private String tkCategoryCode;

    @SerializedName("tkCategoryName")
    private String tkCategoryName;

    @SerializedName("tkCode")
    private String tkCode;

    // Yes, they are using unix timestamp
    @SerializedName("tkDate")
    private long tkDate;

    @SerializedName("tkDesc")
    private String tkDesc;

    @SerializedName("tkLocation")
    private String tkLocation;

    // This is important, when we will convert that timestamp to a date
    @SerializedName("tkTimezone")
    private String tkTimezone;

    public FourPXEvent(String tkCategoryCode, String tkCategoryName, String tkCode, long tkDate, String tkDesc, String tkLocation, String tkTimezone) {
        this.tkCategoryCode = tkCategoryCode;
        this.tkCategoryName = tkCategoryName;
        this.tkCode = tkCode;
        this.tkDate = tkDate;
        this.tkDesc = tkDesc;
        this.tkLocation = tkLocation;
        this.tkTimezone = tkTimezone;
    }

    public String getTkCategoryCode() {
        return tkCategoryCode;
    }

    public void setTkCategoryCode(String tkCategoryCode) {
        this.tkCategoryCode = tkCategoryCode;
    }

    public String getTkCategoryName() {
        return tkCategoryName;
    }

    public void setTkCategoryName(String tkCategoryName) {
        this.tkCategoryName = tkCategoryName;
    }

    public String getTkCode() {
        return tkCode;
    }

    public void setTkCode(String tkCode) {
        this.tkCode = tkCode;
    }

    public long getTkDate() {
        return tkDate;
    }

    public void setTkDate(long tkDate) {
        this.tkDate = tkDate;
    }

    public String getTkDesc() {
        return tkDesc;
    }

    public void setTkDesc(String tkDesc) {
        this.tkDesc = tkDesc;
    }

    public String getTkLocation() {
        return tkLocation;
    }

    public void setTkLocation(String tkLocation) {
        this.tkLocation = tkLocation;
    }

    public String getTkTimezone() {
        return tkTimezone;
    }

    public void setTkTimezone(String tkTimezone) {
        this.tkTimezone = tkTimezone;
    }

    public EventObject toEventObject() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getTkDate());
        if (getTkTimezone() != null && !getTkTimezone().isEmpty())
            calendar.setTimeZone(TimeZone.getTimeZone(getTkTimezone()));
        Date date = calendar.getTime();
        return new EventObject(getTkDesc(), DateTimeUtils.showingDateFormat.format(date), DateTimeUtils.SQLiteDateFormat.format(date), "", getTkLocation());
    }
}
