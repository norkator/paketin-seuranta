/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author norkator
 */

package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.paketinseuranta.PhaseNumber;
import com.nitramite.utils.Locale;
import com.nitramite.utils.OkHttpUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


@SuppressWarnings("HardCodedStringLiteral")
public class DpdStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = "DpdStrategy";
    private static final String URL = "https://tracking.dpd.de/rest/plc/en_US/";


    private String getRequest(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Constants.UserAgent)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
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

    @SuppressWarnings("ConstantConditions")
    @Override
    public ParcelObject execute(String parcelCode, final Locale locale) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);

        try {
            String getResult = getRequest(URL + parcelCode);

            if (!isValidJSONObject(getResult)) {
                Log.i(TAG, "Dpd strategy fetching failed to parse JSON");
                parcelObject.setIsFound(false); // Parcel not found
                return parcelObject;
            }

            JSONObject shipment = new JSONObject(getResult);
            JSONObject parcelLifeCycleData = shipment
                    .optJSONObject("parcellifecycleResponse")
                    .optJSONObject("parcelLifeCycleData");

            JSONObject shipmentInfo = parcelLifeCycleData.optJSONObject("shipmentInfo");
            JSONArray shipmentEvents = parcelLifeCycleData.optJSONArray("statusInfo");


            setBasicDetails(parcelObject, shipmentInfo);
            setEvents(parcelObject, shipmentEvents);

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


    private void setBasicDetails(ParcelObject parcelObject, JSONObject shipmentInfo) {
        parcelObject.setProduct(shipmentInfo.optString("productName"));
        parcelObject.setDestinationCountry(shipmentInfo.optString("receiverCountryIsoCode"));
    }


    @SuppressWarnings("ConstantConditions")
    private void setEvents(ParcelObject parcelObject, JSONArray shipmentEvents) {
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            // Parse events
            @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("dd.MM.yyyy, HH:mm");
            @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < shipmentEvents.length(); i++) {
                JSONObject scan = shipmentEvents.optJSONObject(i);

                if (scan.optBoolean("isCurrentStatus")) {
                    parseShipmentStatus(parcelObject, scan.optString("status"));
                }

                // Description
                String description = scan.optString("label");
                        /*scan.optJSONObject("description")
                        .optJSONArray("content")
                        .opt(0).toString();*/

                // Date
                Date apiDate = apiDateFormat.parse(scan.optString("date"));
                String parsedShowingDate = showingDateFormat.format(apiDate);
                String parsedDateSQLiteFormat = SQLiteDateFormat.format(apiDate);

                // Location
                String locationName = scan.optString("location");
                EventObject eventObject = new EventObject(
                        description, parsedShowingDate, parsedDateSQLiteFormat, "", locationName
                );

                eventObjects.add(eventObject);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
    }


    /**
     * Parses status which is known as "phase"
     *
     * @param parcelObject object to update on database
     * @param status       api return status for isCurrentStatus element
     * @apiNote Dpd status codes: ACCEPTED, ON_THE_ROAD, AT_DELIVERY_DEPOT, OUT_FOR_DELIVERY, DELIVERED
     */
    private void parseShipmentStatus(ParcelObject parcelObject, String status) {
        parcelObject.setIsFound(true);
        switch (status) {
            case "ACCEPTED":
                parcelObject.setPhase(PhaseNumber.PHASE_WAITING_FOR_PICKUP);
                break;
            case "ON_THE_ROAD":
            case "AT_DELIVERY_DEPOT":
            case "OUT_FOR_DELIVERY":
                parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT);
                break;
            case "DELIVERED":
                parcelObject.setPhase(PhaseNumber.PHASE_DELIVERED);
                break;
        }
    }


} // End of class
