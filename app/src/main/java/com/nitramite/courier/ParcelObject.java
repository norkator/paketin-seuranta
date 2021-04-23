package com.nitramite.courier;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.paketinseuranta.PhaseNumber;

import java.util.ArrayList;

@SuppressWarnings("FieldCanBeLocal")
public class ParcelObject {

    // Variables of this object
    private String id = "-1";
    private Boolean isFound = false;        // Master boolean is parcel information found
    //
    private String carrier = "";            // Carrier number as string
    private String title = "";
    //
    private String parcelCode = "null";
    private String parcelCode2 = "null";
    private String errandCode = "null";
    private String phase = "null";
    private String estimatedDeliveryTime = "null";
    //
    private String pickupAddressName = "null";
    private String pickupAddressStreet = "null";
    private String pickupAddressPostcode = "null";
    private String pickupAddressCity = "null";
    private String pickupAddressLatitude = "null";
    private String pickupAddressLongitude = "null";
    private String pickupAddressAvailability = "null";
    //
    private String lastPickupDate = "null";
    private String product = "null";        // Example: Product -> Name -> "Kirjattu kirje"
    private String sender = "null";
    private String lockerCode = "null";
    private String extraServices = "null";  // Example: extraServices -> name -> "Postin kuljetuspalvelu"
    private String weight = "null";
    private String height = "null";
    private String width = "null";
    private String depth = "null";
    private String volume = "null";
    private String destinationPostcode = "null";
    private String destinationCity = "null";
    private String destinationCountry = "null";
    private String recipientSignature = "null";
    private String codAmount = "null";
    private String codCurrency = "null";
    private String carrierStatus = "";
    private String trackingCode = "";
    private String trackingCode2 = "";
    private String lastUpdateStatus = "";
    private String originalTrackingCode = "";
    private String senderText = "";
    private String deliveryMethod = "";
    private String additionalNote = "";
    private String orderDate = "";
    private String deliveryDate = "";
    private String productPage = "";
    private String parcelPaid = "";
    private String quantity = "";

    private Boolean updateFailed = false;

    // Events
    private ArrayList<EventObject> eventObjects = null;

    // Constructor
    public ParcelObject(final String parcelCode) {
        this.parcelCode = parcelCode;
    }

    // ---------------------------------------------------------------------------------------------
    // Get methods


    public String getId() {
        return id;
    }

    public Boolean getIsFound() {
        return this.isFound;
    }

    public Boolean getFound() {
        return isFound;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getTitle() {
        return title;
    }

    public String getParcelCode() {
        return this.parcelCode;
    }

    public String getParcelCode2() {
        return this.parcelCode2;
    }

    public String getErrandCode() {
        return this.errandCode;
    }

    public String getPhase() {
        return this.phase;
    }

    public String getPhaseToNumber() {
        return PhaseNumber.phaseToNumber(this.phase, "").getPhaseNumber();
    }

    public String getEstimatedDeliveryTime() {
        return this.estimatedDeliveryTime;
    }

    public String getPickupAddressName() {
        return this.pickupAddressName;
    }

    public String getPickupAddressStreet() {
        return this.pickupAddressStreet;
    }

    public String getPickupAddressPostcode() {
        return this.pickupAddressPostcode;
    }

    public String getPickupAddressCity() {
        return this.pickupAddressCity;
    }

    public String getPickupAddressLatitude() {
        return this.pickupAddressLatitude;
    }

    public String getPickupAddressLongitude() {
        return this.pickupAddressLongitude;
    }

    public String getPickupAddressAvailability() {
        return this.pickupAddressAvailability;
    }

    public String getLastPickupDate() {
        return this.lastPickupDate;
    }

    public String getProduct() {
        return this.product;
    }

    public String getSender() {
        return this.sender;
    }

    public String getLockerCode() {
        return this.lockerCode;
    }

    public String getExtraServices() {
        return this.extraServices;
    }

    public String getWeight() {
        return this.weight;
    }

    public String getHeight() {
        return this.height;
    }

    public String getWidth() {
        return this.width;
    }

    public String getDepth() {
        return this.depth;
    }

    public String getVolume() {
        return this.volume;
    }

    public String getDestinationPostcode() {
        return this.destinationPostcode;
    }

    public String getDestinationCity() {
        return this.destinationCity;
    }

    public String getDestinationCountry() {
        return this.destinationCountry;
    }

    public String getRecipientSignature() {
        return this.recipientSignature;
    }

    public String getCodAmount() {
        return this.codAmount;
    }

    public String getCodCurrency() {
        return this.codCurrency;
    }

    public ArrayList<EventObject> getEventObjects() {
        return this.eventObjects;
    }

    public Boolean getUpdateFailed() {
        return updateFailed;
    }

    public String getLastUpdateStatus() {
        return lastUpdateStatus;
    }

    public String getOriginalTrackingCode() {
        return originalTrackingCode;
    }

    public String getCarrierStatus() {
        return carrierStatus;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public String getTrackingCode2() {
        return trackingCode2;
    }

    public String getSenderText() {
        return senderText;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public String getAdditionalNote() {
        return additionalNote;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public String getProductPage() {
        return productPage;
    }

    public String getParcelPaid() {
        return parcelPaid;
    }

    public String getQuantity() {
        return quantity;
    }

    // ---------------------------------------------------------------------------------------------
    // Set methods


    public void setId(String id) {
        this.id = id;
    }

    public void setIsFound(Boolean isFound) {
        this.isFound = isFound;
    }

    public void setFound(Boolean found) {
        isFound = found;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setParcelCode2(String parcelCode2) {
        this.parcelCode2 = parcelCode2;
    }

    public void setErrandCode(String errandCode) {
        this.errandCode = errandCode;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public void setEstimatedDeliveryTime(String estimatedDeliveryTime) {
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }

    public void setPickupAddress(String name, String street, String postCode, String city,
                                 String latitude, String longitude, String availability) {
        this.pickupAddressName = name;
        this.pickupAddressStreet = street;
        this.pickupAddressPostcode = postCode;
        this.pickupAddressCity = city;
        this.pickupAddressLatitude = latitude;
        this.pickupAddressLongitude = longitude;
        this.pickupAddressAvailability = availability;
    }

    public void setLastPickupDate(String lastPickupDate) {
        this.lastPickupDate = lastPickupDate;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setLockerCode(String lockerCode) {
        this.lockerCode = lockerCode;
    }

    public void setExtraServices(String extraServices) {
        this.extraServices = extraServices;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public void setDestinationPostcode(String destinationPostcode) {
        this.destinationPostcode = destinationPostcode;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public void setDestinationCountry(String destinationCountry) {
        this.destinationCountry = destinationCountry;
    }

    public void setParcelCode(String parcelCode) {
        this.parcelCode = parcelCode;
    }

    public void setPickupAddressAvailability(String pickupAddressAvailability) {
        this.pickupAddressAvailability = pickupAddressAvailability;
    }

    public void setPickupAddressCity(String pickupAddressCity) {
        this.pickupAddressCity = pickupAddressCity;
    }

    public void setPickupAddressLatitude(String pickupAddressLatitude) {
        this.pickupAddressLatitude = pickupAddressLatitude;
    }

    public void setPickupAddressLongitude(String pickupAddressLongitude) {
        this.pickupAddressLongitude = pickupAddressLongitude;
    }

    public void setPickupAddressName(String pickupAddressName) {
        this.pickupAddressName = pickupAddressName;
    }

    public void setPickupAddressPostcode(String pickupAddressPostcode) {
        this.pickupAddressPostcode = pickupAddressPostcode;
    }

    public void setPickupAddressStreet(String pickupAddressStreet) {
        this.pickupAddressStreet = pickupAddressStreet;
    }

    public void setRecipientSignature(String recipientSignature) {
        this.recipientSignature = recipientSignature;
    }

    public void setCodAmount(String codAmount) {
        this.codAmount = codAmount;
    }

    public void setCodCurrency(String codCurrency) {
        this.codCurrency = codCurrency;
    }

    public void setEventObjects(final ArrayList<EventObject> eventObjects) {
        this.eventObjects = eventObjects;
    }

    public void setUpdateFailed(Boolean updateFailed) {
        this.updateFailed = updateFailed;
    }

    public void setLastUpdateStatus(String lastUpdateStatus) {
        this.lastUpdateStatus = lastUpdateStatus;
    }

    public void setOriginalTrackingCode(String originalTrackingCode) {
        this.originalTrackingCode = originalTrackingCode;
    }

    public void setCarrierStatus(String carrierStatus) {
        this.carrierStatus = carrierStatus;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public void setTrackingCode2(String trackingCode2) {
        this.trackingCode2 = trackingCode2;
    }

    public void setSenderText(String senderText) {
        this.senderText = senderText;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public void setAdditionalNote(String additionalNote) {
        this.additionalNote = additionalNote;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public void setProductPage(String productPage) {
        this.productPage = productPage;
    }

    public void setParcelPaid(String parcelPaid) {
        this.parcelPaid = parcelPaid;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    // ---------------------------------------------------------------------------------------------


} // End of class