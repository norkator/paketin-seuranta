package com.nitramite.courier

import com.nitramite.paketinseuranta.EventObject
import com.nitramite.paketinseuranta.PhaseNumber
import java.util.ArrayList

class ParcelObject(private var parcelCode: String) {

    // Variables of this object
    private var id = "-1"
    private var isFound = false        // Master boolean is parcel information found

    //
    private var carrier = ""            // Carrier number as string
    private var title = ""
    private var parcelCode2 = "null"
    private var errandCode = "null"
    private var phase = "null"
    private var estimatedDeliveryTime = "null"

    //
    private var pickupAddressName = "null"
    private var pickupAddressStreet = "null"
    private var pickupAddressPostcode = "null"
    private var pickupAddressCity = "null"
    private var pickupAddressLatitude = "null"
    private var pickupAddressLongitude = "null"
    private var pickupAddressAvailability = "null"

    //
    private var lastPickupDate = "null"
    private var product = "null"        // Example: Product -> Name -> "Kirjattu kirje"
    private var sender = "null"
    private var lockerCode = "null"
    private var extraServices = "null"  // Example: extraServices -> name -> "Postin kuljetuspalvelu"
    private var weight = "null"
    private var height = "null"
    private var width = "null"
    private var depth = "null"
    private var volume = "null"
    private var destinationPostcode = "null"
    private var destinationCity = "null"
    private var destinationCountry = "null"
    private var recipientSignature = "null"
    private var codAmount = "null"
    private var codCurrency = "null"
    private var carrierStatus = ""
    private var trackingCode = ""
    private var trackingCode2 = ""
    private var lastUpdateStatus = ""
    private var originalTrackingCode = ""
    private var senderText = ""
    private var deliveryMethod = ""
    private var additionalNote = ""
    private var orderDate = ""
    private var deliveryDate = ""
    private var productPage = ""

    private var updateFailed = false

    // Events
    private var eventObjects: ArrayList<EventObject>? = null

    // ---------------------------------------------------------------------------------------------
    // Get methods


    fun getId(): String {
        return id
    }

    fun getIsFound(): Boolean {
        return this.isFound
    }

    fun getFound(): Boolean {
        return isFound
    }

    fun getCarrier(): String {
        return carrier
    }

    fun getTitle(): String {
        return title
    }

    fun getParcelCode(): String {
        return this.parcelCode
    }

    fun getParcelCode2(): String {
        return this.parcelCode2
    }

    fun getErrandCode(): String {
        return this.errandCode
    }

    fun getPhase(): String {
        return this.phase
    }

    fun getPhaseToNumber(): String {
        return PhaseNumber.phaseToNumber(this.phase, "").phaseNumber
    }

    fun getEstimatedDeliveryTime(): String {
        return this.estimatedDeliveryTime
    }

    fun getPickupAddressName(): String {
        return this.pickupAddressName
    }

    fun getPickupAddressStreet(): String {
        return this.pickupAddressStreet
    }

    fun getPickupAddressPostcode(): String {
        return this.pickupAddressPostcode
    }

    fun getPickupAddressCity(): String {
        return this.pickupAddressCity
    }

    fun getPickupAddressLatitude(): String {
        return this.pickupAddressLatitude
    }

    fun getPickupAddressLongitude(): String {
        return this.pickupAddressLongitude
    }

    fun getPickupAddressAvailability(): String {
        return this.pickupAddressAvailability
    }

    fun getLastPickupDate(): String {
        return this.lastPickupDate
    }

    fun getProduct(): String {
        return this.product
    }

    fun getSender(): String {
        return this.sender
    }

    fun getLockerCode(): String {
        return this.lockerCode
    }

    fun getExtraServices(): String {
        return this.extraServices
    }

    fun getWeight(): String {
        return this.weight
    }

    fun getHeight(): String {
        return this.height
    }

    fun getWidth(): String {
        return this.width
    }

    fun getDepth(): String {
        return this.depth
    }

    fun getVolume(): String {
        return this.volume
    }

    fun getDestinationPostcode(): String {
        return this.destinationPostcode
    }

    fun getDestinationCity(): String {
        return this.destinationCity
    }

    fun getDestinationCountry(): String {
        return this.destinationCountry
    }

    fun getRecipientSignature(): String {
        return this.recipientSignature
    }

    fun getCodAmount(): String {
        return this.codAmount
    }

    fun getCodCurrency(): String {
        return this.codCurrency
    }

    fun getEventObjects(): ArrayList<EventObject>? {
        return this.eventObjects
    }

    fun getUpdateFailed(): Boolean {
        return updateFailed
    }

    fun getLastUpdateStatus(): String {
        return lastUpdateStatus
    }

    fun getOriginalTrackingCode(): String {
        return originalTrackingCode
    }

    fun getCarrierStatus(): String {
        return carrierStatus
    }

    fun getTrackingCode(): String {
        return trackingCode
    }

    fun getTrackingCode2(): String {
        return trackingCode2
    }

    fun getSenderText(): String {
        return senderText
    }

    fun getDeliveryMethod(): String {
        return deliveryMethod
    }

    fun getAdditionalNote(): String {
        return additionalNote
    }

    fun getOrderDate(): String {
        return orderDate
    }

    fun getDeliveryDate(): String {
        return deliveryDate
    }

    fun getProductPage(): String {
        return productPage
    }

    // ---------------------------------------------------------------------------------------------
    // Set methods


    fun setId(id: String) {
        this.id = id
    }

    fun setIsFound(isFound: Boolean) {
        this.isFound = isFound
    }

    fun setFound(found: Boolean) {
        isFound = found
    }

    fun setCarrier(carrier: String) {
        this.carrier = carrier
    }

    fun setTitle(title: String) {
        this.title = title
    }

    fun setParcelCode2(parcelCode2: String) {
        this.parcelCode2 = parcelCode2
    }

    fun setErrandCode(errandCode: String) {
        this.errandCode = errandCode
    }

    fun setPhase(phase: String) {
        this.phase = phase
    }

    fun setEstimatedDeliveryTime(estimatedDeliveryTime: String) {
        this.estimatedDeliveryTime = estimatedDeliveryTime
    }

    fun setPickupAddress(name: String, street: String, postCode: String, city: String,
                         latitude: String, longitude: String, availability: String) {
        this.pickupAddressName = name
        this.pickupAddressStreet = street
        this.pickupAddressPostcode = postCode
        this.pickupAddressCity = city
        this.pickupAddressLatitude = latitude
        this.pickupAddressLongitude = longitude
        this.pickupAddressAvailability = availability
    }

    fun setLastPickupDate(lastPickupDate: String) {
        this.lastPickupDate = lastPickupDate
    }

    fun setProduct(product: String) {
        this.product = product
    }

    fun setSender(sender: String) {
        this.sender = sender
    }

    fun setLockerCode(lockerCode: String) {
        this.lockerCode = lockerCode
    }

    fun setExtraServices(extraServices: String) {
        this.extraServices = extraServices
    }

    fun setWeight(weight: String) {
        this.weight = weight
    }

    fun setHeight(height: String) {
        this.height = height
    }

    fun setWidth(width: String) {
        this.width = width
    }

    fun setDepth(depth: String) {
        this.depth = depth
    }

    fun setVolume(volume: String) {
        this.volume = volume
    }

    fun setDestinationPostcode(destinationPostcode: String) {
        this.destinationPostcode = destinationPostcode
    }

    fun setDestinationCity(destinationCity: String) {
        this.destinationCity = destinationCity
    }

    fun setDestinationCountry(destinationCountry: String) {
        this.destinationCountry = destinationCountry
    }

    fun setParcelCode(parcelCode: String) {
        this.parcelCode = parcelCode
    }

    fun setPickupAddressAvailability(pickupAddressAvailability: String) {
        this.pickupAddressAvailability = pickupAddressAvailability
    }

    fun setPickupAddressCity(pickupAddressCity: String) {
        this.pickupAddressCity = pickupAddressCity
    }

    fun setPickupAddressLatitude(pickupAddressLatitude: String) {
        this.pickupAddressLatitude = pickupAddressLatitude
    }

    fun setPickupAddressLongitude(pickupAddressLongitude: String) {
        this.pickupAddressLongitude = pickupAddressLongitude
    }

    fun setPickupAddressName(pickupAddressName: String) {
        this.pickupAddressName = pickupAddressName
    }

    fun setPickupAddressPostcode(pickupAddressPostcode: String) {
        this.pickupAddressPostcode = pickupAddressPostcode
    }

    fun setPickupAddressStreet(pickupAddressStreet: String) {
        this.pickupAddressStreet = pickupAddressStreet
    }

    fun setRecipientSignature(recipientSignature: String) {
        this.recipientSignature = recipientSignature
    }

    fun setCodAmount(codAmount: String) {
        this.codAmount = codAmount
    }

    fun setCodCurrency(codCurrency: String) {
        this.codCurrency = codCurrency
    }

    fun setEventObjects(eventObjects: ArrayList<EventObject>?) {
        this.eventObjects = eventObjects
    }

    fun setUpdateFailed(updateFailed: Boolean) {
        this.updateFailed = updateFailed
    }

    fun setLastUpdateStatus(lastUpdateStatus: String) {
        this.lastUpdateStatus = lastUpdateStatus
    }

    fun setOriginalTrackingCode(originalTrackingCode: String) {
        this.originalTrackingCode = originalTrackingCode
    }

    fun setCarrierStatus(carrierStatus: String) {
        this.carrierStatus = carrierStatus
    }

    fun setTrackingCode(trackingCode: String) {
        this.trackingCode = trackingCode
    }

    fun setTrackingCode2(trackingCode2: String) {
        this.trackingCode2 = trackingCode2
    }

    fun setSenderText(senderText: String) {
        this.senderText = senderText
    }

    fun setDeliveryMethod(deliveryMethod: String) {
        this.deliveryMethod = deliveryMethod
    }

    fun setAdditionalNote(additionalNote: String) {
        this.additionalNote = additionalNote
    }

    fun setOrderDate(orderDate: String) {
        this.orderDate = orderDate
    }

    fun setDeliveryDate(deliveryDate: String) {
        this.deliveryDate = deliveryDate
    }

    fun setProductPage(productPage: String) {
        this.productPage = productPage
    }

    // ---------------------------------------------------------------------------------------------


} // End of class