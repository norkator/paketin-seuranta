/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.courier;

import android.util.Log;

import com.google.gson.Gson;
import com.nitramite.courier.fourpx.FourPXEvent;
import com.nitramite.courier.fourpx.FourPXParcel;
import com.nitramite.courier.fourpx.ListTrackResponse;
import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.OkHttpUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class FourPXStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = "FourPXStrategy";
    private static final String URL = "http://track.4px.com/track/v2/front/listTrack";


    private String getRequest(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Constants.UserAgent)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    private String generatePostBody(String trackingCode) {
        try {
            JSONObject object = new JSONObject();
            JSONArray array = new JSONArray();
            array.put(trackingCode);
            object.put("serveCodes", array);
            object.put("language", "en-us");
            return object.toString();
        } catch (Exception ignored) {
        }
        return "";
    }

    private String postJSONRequest(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, OkHttpUtils.JSON); // new
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Constants.UserAgent)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        ResponseBody respBody = response.body();
        return respBody != null ? respBody.string() : null;
    }

    @Override
    public ParcelObject execute(String parcelCode) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);

        try {

            String postResult = postJSONRequest(URL, generatePostBody(parcelCode));

            if (postResult != null) {
                // Validating JSON
                if (!isValidJSONObject(postResult)) {
                    Log.i(TAG, "FPX strategy fetching failed to parse JSON");
                    parcelObject.setIsFound(false); // Parcel not found
                    return parcelObject;
                }

                // If parcel is not found due to an server error, interrupting
                ListTrackResponse trackResponse = new Gson().fromJson(postResult, ListTrackResponse.class);
                if (!trackResponse.isParcelFound()) {
                    Log.i(TAG, "FPX strategy fetching failed finding any parcel details");
                    parcelObject.setIsFound(false); // Parcel not found
                    return parcelObject;
                }

                // If the parcel isn't found in 4PX system, interrupting
                FourPXParcel pxParcel = trackResponse.getParcel();
                if (pxParcel.getServerCode() == null || pxParcel.getServerCode().equals("")) {
                    Log.i(TAG, "FPX strategy parcel not found");
                    parcelObject.setIsFound(false); // Parcel not found
                    return parcelObject;
                }

                if (pxParcel.getDestinationName() != null)
                    parcelObject.setDestinationCountry(pxParcel.getDestinationName());

                proceedParsing(pxParcel, parcelObject);
            } else {
                Log.i(TAG, "FPX strategy fetching failed to read response");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


    private boolean isValidJSONObject(String json) {
        try {
            new JSONObject(json);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private void proceedParsing(FourPXParcel fourPXParcel, ParcelObject parcelObject) throws JSONException, ParseException {
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        parcelObject.setIsFound(true);
        parcelObject.setPhase("IN_TRANSPORT");

        // Parse events
        for (FourPXEvent event : fourPXParcel.getEvents()) {
            eventObjects.add(event.toEventObject());
        }

        setBasicDetails(parcelObject, fourPXParcel);
        // Add to stack
        parcelObject.setEventObjects(eventObjects);
    }


    private void setBasicDetails(ParcelObject parcelObject, FourPXParcel parcel) {
        String status = "";
        String desc = "";
        boolean hasArrivedAtDestCountry = false;
        for (FourPXEvent event : parcel.getEvents()) {
            if (event.getTkCode() != null)
                if (event.getTkCode().equals("FPX_M_ATA")) {
                    hasArrivedAtDestCountry = true;
                    break;
                }
        }
        if (!parcel.getEvents().isEmpty()) {
            FourPXEvent firstEvent = parcel.getEvents().get(0);
            if (!firstEvent.getTkCode().isEmpty()) {
                status = parcel.getEvents().get(0).getTkCode();
                desc = parcel.getEvents().get(0).getTkDesc();
            }
        }
        // This part is tricky. I didn't find any list of what different TkCodes mean. I am waiting my parcel to be delivered, and possibly extracting info from that
        if (!hasArrivedAtDestCountry && parcel.getDestinationCode().equals("FI")) {
            parcelObject.setPhase("INTRANSPORT_NOTINFINLAND");
        }
        if (status.toLowerCase().contains("delivered") || desc.toLowerCase().contains("delivered")) {
            parcelObject.setPhase("DELIVERED");
        }
    }


} // End of class
