/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Locale;

import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BringStrategy implements CourierStrategy {

    // Logging
    @NonNls
    private static final String TAG = BringStrategy.class.getSimpleName();

    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public ParcelObject execute(String parcelCode, final Locale locale) {

        // Api url
        String url = "https://tracking.bring.com/tracking/api/fetch/" + parcelCode + "?lang=en";

        ParcelObject parcelObject = new ParcelObject(parcelCode);
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", Constants.UserAgent)
                    .build();
            Response response = client.newCall(request).execute();

            String jsonResult = response.body().string();
            Log.i(TAG, jsonResult);

            // Parsing got json content
            JSONObject jsonResponse = new JSONObject(jsonResult); // Json content
            JSONObject consignmentSet = jsonResponse.getJSONArray("consignmentSet").getJSONObject(0);


            if (hasParcelDetails(consignmentSet)) { // parcel details exists
                JSONObject packageSet = consignmentSet.getJSONArray("packageSet").getJSONObject(0);

                JSONArray jsonEvents = packageSet.getJSONArray("eventSet"); // Get events
                setParcelDetails(parcelObject, packageSet, jsonEvents);
                parcelObject.setEventObjects(setParcelEvents(jsonEvents));

            } else {
                Log.i(TAG, "Bring shipment not found");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.i(TAG, e.toString());
        } catch (IOException e) {
            Log.i(TAG, e.toString());
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


    /**
     * Checks key existence from json object
     *
     * @param jsonObject any json object
     * @return boolean
     */
    @SuppressWarnings("HardCodedStringLiteral")
    private boolean hasParcelDetails(JSONObject jsonObject) {
        return jsonObject.has("packageSet");
    }


    /**
     * Set's basic parcel details
     *
     * @param parcelObject json object of parcel where data is saved
     * @param packageSet   parcel basic data object
     * @param jsonEvents   parcel events array
     */
    @SuppressWarnings("HardCodedStringLiteral")
    private void setParcelDetails(ParcelObject parcelObject, JSONObject packageSet, JSONArray jsonEvents) {
        parcelObject.setIsFound(true);
        parcelObject.setPhase(jsonEvents.optJSONObject(0).optString("status")); // Only events have "phase" status, first is newest
        parcelObject.setWeight(packageSet.optString("weightInKgs"));
        parcelObject.setHeight(packageSet.optString("heightInCm"));
        parcelObject.setWidth(packageSet.optString("widthInCm"));
        parcelObject.setDepth(packageSet.optString("lengthInCm"));
        parcelObject.setVolume(packageSet.optString("volumeInDm3"));
        parcelObject.setSender(packageSet.optString("senderName"));
    }


    /**
     * Parse and add parcel events to event objects
     *
     * @param jsonEvents events for parcel
     * @return event objects array
     */
    @SuppressWarnings("HardCodedStringLiteral")
    private ArrayList<EventObject> setParcelEvents(JSONArray jsonEvents) {
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < jsonEvents.length(); i++) {
                JSONObject event = jsonEvents.getJSONObject(i);


                String timeStamp = event.optString("dateIso");
                Date parseTimeDate = null;

                parseTimeDate = apiDateFormat.parse(timeStamp);

                String parsedDate = showingDateFormat.format(parseTimeDate);
                String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);


                // Get event description
                String eventDescription = event.optString("description");
                String locationCode = event.optString("postalCode");
                String locationName = event.optString("city") + ", " +
                        event.optString("country");

                EventObject eventObject = new EventObject(
                        eventDescription,
                        parsedDate,
                        parsedDateSQLiteFormat,
                        locationCode,
                        locationName
                );
                // Add object
                eventObjects.add(eventObject);

            }
        } catch (ParseException | JSONException e) {
            e.printStackTrace();
        }
        return eventObjects;
    }


}
