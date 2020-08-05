/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.courier.fourpx;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FourPXParcel {

    @SerializedName("ctEndCode")
    private String destinationCode;

    @SerializedName("ctEndName")
    private String destinationName;

    @SerializedName("ctStartCode")
    private String originCode;

    @SerializedName("ctStartName")
    private String originName;

    // How many days passed since the shipment has been registered
    @SerializedName("duration")
    private int duration;

    // Code that user specified
    @SerializedName("queryCode")
    private String queryCode;


    // Code that the system has
    @SerializedName("serverCode")
    private String serverCode;

    @SerializedName("shipperCode")
    private String shipperCode;

    @SerializedName("status")
    private int status;

    @SerializedName("tracks")
    private List<FourPXEvent> events;

    public List<FourPXEvent> getEvents() {
        return events;
    }

    public void setEvents(List<FourPXEvent> events) {
        this.events = events;
    }

    public String getDestinationCode() {
        return destinationCode;
    }

    public void setDestinationCode(String destinationCode) {
        this.destinationCode = destinationCode;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public String getOriginCode() {
        return originCode;
    }

    public void setOriginCode(String originCode) {
        this.originCode = originCode;
    }

    public String getOriginName() {
        return originName;
    }

    public void setOriginName(String originName) {
        this.originName = originName;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getQueryCode() {
        return queryCode;
    }

    public void setQueryCode(String queryCode) {
        this.queryCode = queryCode;
    }

    public String getServerCode() {
        return serverCode;
    }

    public void setServerCode(String serverCode) {
        this.serverCode = serverCode;
    }

    public String getShipperCode() {
        return shipperCode;
    }

    public void setShipperCode(String shipperCode) {
        this.shipperCode = shipperCode;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
