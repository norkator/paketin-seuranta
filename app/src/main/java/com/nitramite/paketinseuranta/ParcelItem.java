/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.paketinseuranta;

import com.nitramite.utils.CarrierUtils;

public class ParcelItem {


    // Variables
    private String parcelId = null;
    private String parcelCode = null;
    private String parcelPhase = null;
    private String parcelType = null;
    private String parcelTitle = null;
    private String parcelUpdateStatus = null;
    private String parcelLatestEventDescription = null;
    private String parcelCarrier = null;
    private String parcelLastPickupDate = null;

    // Archive only
    private Boolean isArchivedPackage = false;
    private String parcelSender = null;
    private String parcelDeliveryMethod = null;
    private String parcelAdditionalNote = null;
    private String parcelCreateDate = null;
    private String lastEventDate = null;


    // Constructor
    ParcelItem(String parcelId, String parcelCode, String parcelPhase, String parcelType, String parcelTitle,
               String parcelUpdateStatus, String parcelLatestEventDescription, String parcelCarrier,
               String parcelSender, String parcelDeliveryMethod, String parcelAdditionalNote, String parcelCreateDate,
               String parcelLastPickupDate, String lastEventDate) {
        this.parcelId = parcelId;
        this.parcelCode = parcelCode;
        this.parcelPhase = parcelPhase;
        this.parcelType = parcelType;
        this.parcelTitle = parcelTitle;
        this.parcelUpdateStatus = parcelUpdateStatus;
        this.parcelLatestEventDescription = parcelLatestEventDescription;
        this.parcelCarrier = parcelCarrier;
        this.parcelSender = parcelSender;
        this.parcelDeliveryMethod = parcelDeliveryMethod;
        this.parcelAdditionalNote = parcelAdditionalNote;
        this.parcelCreateDate = parcelCreateDate;
        this.parcelLastPickupDate = parcelLastPickupDate;
        this.lastEventDate = lastEventDate;
    }

    public String getLastEventDate() {
        return lastEventDate;
    }

    public void setLastEventDate(String lastEventDate) {
        this.lastEventDate = lastEventDate;
    }

    String getParcelId() {
        return this.parcelId;
    }

    public String getParcelCode() {
        return (this.parcelCode.equals("") ? "-" : this.parcelCode); // If no parcel code, show line
    }

    public String getParcelPhase() {
        return this.parcelPhase;
    }

    public String getParcelType() {
        return this.parcelType;
    }

    public String getParcelTitle() {
        return this.parcelTitle;
    }

    public String getParcelUpdateStatus() {
        return this.parcelUpdateStatus;
    }

    public String getParcelLatestEventDescription() {
        return this.parcelLatestEventDescription;
    }

    public String getParcelCarrier() {
        return parcelCarrier;
    }

    public Integer getParcelCarrierNumber() {
        try {
            return Integer.parseInt(parcelCarrier);
        } catch (NumberFormatException e) {
            return CarrierUtils.CARRIER_POSTI;
        }
    }

    public String getParcelSender() {
        return parcelSender;
    }

    public String getParcelDeliveryMethod() {
        return parcelDeliveryMethod;
    }

    public String getParcelAdditionalNote() {
        return parcelAdditionalNote;
    }

    public void setArchivedPackage(Boolean archivedPackage) {
        isArchivedPackage = archivedPackage;
    }

    public Boolean getArchivedPackage() {
        return isArchivedPackage;
    }

    public String getParcelCreateDate() {
        return parcelCreateDate;
    }

    public String getParcelLastPickupDate() {
        return parcelLastPickupDate;
    }
} // End of class