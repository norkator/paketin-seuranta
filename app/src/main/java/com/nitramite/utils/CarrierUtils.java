/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.utils;

import com.nitramite.paketinseuranta.R;

@SuppressWarnings({"SpellCheckingInspection", "WeakerAccess", "HardCodedStringLiteral"})
public class CarrierUtils {

    // Carrier codes
    public static final int CARRIER_POSTI = 0;
    public static final int CARRIER_CHINA = 1;
    public static final int CARRIER_MATKAHUOLTO = 2;
    public static final int CARRIER_DHL_EXPRESS = 3;
    public static final int CARRIER_DHL_AMAZON = 9;
    public static final int CARRIER_DHL_ACTIVE_TRACKING = 10;
    public static final int CARRIER_UPS = 4;
    public static final int CARRIER_FEDEX = 5;
    // public static final int CARRIER_OTHER = 6; // Deleted due need more room for others
    public static final int CARRIER_POSTNORD = 7;
    public static final int CARRIER_ARRA_PAKETTI = 8;
    public static final int CARRIER_USPS = 11;
    public static final int CARRIER_YANWEN = 12;
    public static final int CARRIER_GLS = 13;
    public static final int CARRIER_CAINIAO = 14;
    public static final int CARRIER_4PX = 17;
    public static final int CARRIER_CPRAM = 15;
    public static final int CARRIER_BRING = 16;
    public static final int CARRIER_OTHER = 99;

    // Carries names
    public static final String CARRIER_POSTI_STR = "Posti";
    public static final String CARRIER_POSTI_CHINA_STR = "Aasia / Kiina - Posti";
    public static final String CARRIER_MATKAHUOLTO_STR = "Matkahuolto";
    public static final String CARRIER_DHL_EXPRESS_STR = "DHL (Express)";
    public static final String CARRIER_DHL_AMAZON_STR = "DHL (Amazon)";
    public static final String CARRIER_DHL_ACTIVE_TRACKING_STR = "DHL (Active Tracking)";
    public static final String CARRIER_UPS_STR = "UPS";
    public static final String CARRIER_FEDEX_STR = "FedEx";
    public static final String CARRIER_POSTNORD_STR = "PostNord (normaali)";
    public static final String CARRIER_ARRA_PAKETTI_STR = "Ärrä paketti";
    public static final String CARRIER_USPS_STR = "USPS";
    public static final String CARRIER_YANWEN_STR = "Yanwen";
    public static final String CARRIER_GLS_STR = "GLS";
    public static final String CARRIER_CAINIAO_STR = "Cainiao";
    public static final String CARRIER_4PX_STR = "4PX";
    public static final String CARRIER_CPRAM_STR = "Ch Post R.A. Mail";
    public static final String CARRIER_BRING_STR = "Bring";
    public static final String CARRIER_OTHER_STR = "Muu (ei kohdistu hakua)";


    /**
     * Detect Carrier
     *
     * @param parcelCode parcel tracking code
     * @return carrier code int
     */
    public static int detectCarrier(String parcelCode) {
        int carrierCode = CARRIER_POSTI; // Posti as default

        // Matkahuolto detection
        if (parcelCode.subSequence(0, 2).equals("MA") || parcelCode.subSequence(0, 2).equals("MH")) {
            carrierCode = CARRIER_MATKAHUOLTO;
        }

        return carrierCode;
    }


    // *** CARRIER SYSTEM CHECK PATH DETECTION ***
    // 0 = Check from Itella/Posti only
    // 1 = Check from China-Post and then Itella/Posti
    // When package has arrived to posti system, change value to 1 to only check from posti from that on...
    public static int carrierStatus(String parcelCode) {
        int carrierStatus = 0; // CHECK FROM ITELLA/POSTI ONLY
        if (parcelCode.length() < 14) {
            carrierStatus = 1;
        }
        if (parcelCode.subSequence(0, 2).equals("MA") || parcelCode.subSequence(0, 2).equals("MH")) {
            carrierStatus = 2;
        }
        return carrierStatus;
    }


    /**
     * Return carrier icon resource for carrier string
     *
     * @param carrierStr string like Posti
     * @return carrier icon resource id
     */
    public static Integer getCarrierIconResourceForCarrierName(final String carrierStr) {
        switch (carrierStr) {
            case CARRIER_POSTI_STR:
                return R.mipmap.posti_logo;

            case CARRIER_POSTI_CHINA_STR:
                return R.mipmap.posti_logo;

            case CARRIER_MATKAHUOLTO_STR:
                return R.mipmap.matkahuolto_logo;

            case CARRIER_DHL_EXPRESS_STR:
                return R.mipmap.dhl_logo;

            case CARRIER_DHL_AMAZON_STR:
                return R.mipmap.dhl_logo;

            case CARRIER_DHL_ACTIVE_TRACKING_STR:
                return R.mipmap.dhl_logo;

            case CARRIER_UPS_STR:
                return R.mipmap.ups_logo;

            case CARRIER_FEDEX_STR:
                return R.mipmap.fedex_logo;

            case CARRIER_POSTNORD_STR:
                return R.mipmap.postnord_logo;

            case CARRIER_ARRA_PAKETTI_STR:
                return R.mipmap.arra_logo;

            case CARRIER_USPS_STR:
                return R.mipmap.usps_logo;

            case CARRIER_YANWEN_STR:
                return R.mipmap.yanwen_logo;

            case CARRIER_GLS_STR:
                return R.mipmap.gls_logo;

            case CARRIER_CAINIAO_STR:
                return R.mipmap.cainiao_logo;

            case CARRIER_4PX_STR:
                return R.mipmap.fpx_logo;

            case CARRIER_CPRAM_STR:
                return R.mipmap.cpram;

            case CARRIER_BRING_STR:
                return R.mipmap.bring_logo;

            case CARRIER_OTHER_STR:
                return R.mipmap.muu_logo;
        }

        return R.mipmap.muu_logo; // default return other icon
    }


    /**
     * Return carrier icon resource for carrier number
     *
     * @param carrierNumber number like 0
     * @return carrier icon resource id
     */
    public static Integer getCarrierIconResourceForCarrierNumber(final Integer carrierNumber) {
        switch (carrierNumber) {
            case CARRIER_POSTI:
                return R.mipmap.posti_logo;

            case CARRIER_CHINA:
                return R.mipmap.posti_logo;

            case CARRIER_MATKAHUOLTO:
                return R.mipmap.matkahuolto_logo;

            case CARRIER_DHL_EXPRESS:
                return R.mipmap.dhl_logo;

            case CARRIER_DHL_AMAZON:
                return R.mipmap.dhl_logo;

            case CARRIER_DHL_ACTIVE_TRACKING:
                return R.mipmap.dhl_logo;

            case CARRIER_UPS:
                return R.mipmap.ups_logo;

            case CARRIER_FEDEX:
                return R.mipmap.fedex_logo;

            case CARRIER_POSTNORD:
                return R.mipmap.postnord_logo;

            case CARRIER_ARRA_PAKETTI:
                return R.mipmap.arra_logo;

            case CARRIER_USPS:
                return R.mipmap.usps_logo;

            case CARRIER_YANWEN:
                return R.mipmap.yanwen_logo;

            case CARRIER_GLS:
                return R.mipmap.gls_logo;

            case CARRIER_CAINIAO:
                return R.mipmap.cainiao_logo;

            case CARRIER_4PX:
                return R.mipmap.fpx_logo;

            case CARRIER_CPRAM:
                return R.mipmap.cpram;

            case CARRIER_BRING:
                return R.mipmap.bring_logo;

            case CARRIER_OTHER:
                return R.mipmap.muu_logo;
        }

        return R.mipmap.muu_logo; // default return other icon
    }


} // End of class
