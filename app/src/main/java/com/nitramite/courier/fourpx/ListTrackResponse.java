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

public class ListTrackResponse {

    @SerializedName("data")
    private List<FourPXParcel> parcels;

    public ListTrackResponse(List<FourPXParcel> parcels) {
        this.parcels = parcels;
    }

    public boolean isParcelFound() {
        return parcels != null && !parcels.isEmpty();
    }

    public FourPXParcel getParcel() {
        return parcels.get(0);
    }
}
