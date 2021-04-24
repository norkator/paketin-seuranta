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
import com.nitramite.paketinseuranta.PhaseNumber;
import com.nitramite.utils.Locale;
import com.nitramite.utils.Utils;

import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CainiaoStrategy implements CourierStrategy {

    // Logging
    @NonNls
    private static final String TAG = CainiaoStrategy.class.getSimpleName();


    @SuppressWarnings("HardCodedStringLiteral")
    private String getRequest(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Constants.UserAgent)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public ParcelObject execute(String parcelCode, final Locale locale) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);

        try {
            String url = "https://global.cainiao.com/detail.htm?mailNoList=" + parcelCode;

            final String htmlResult = getRequest(url);
            Document trackingHTML = Jsoup.parse(htmlResult);
            Element jsonElement = trackingHTML.getElementById("waybill_list_val_box");
            if (jsonElement != null) {
                String jsonResult = jsonElement.text();
                JSONObject responseObject = new JSONObject(jsonResult);
                if (responseObject.optBoolean("success")) {
                    JSONArray dataItems = responseObject.getJSONArray("data");

                    if (dataItems.length() > 0) {
                        JSONObject detailsObject = dataItems.getJSONObject(0);

                        if (detailsObject.optBoolean("success")) {
                            proceedParsing(parcelObject, detailsObject);
                        } else {
                            JSONArray originCpList = detailsObject.optJSONArray("originCpList");
                            if (originCpList != null && originCpList.length() > 0) {
                                Log.i(TAG, "Fetching via fallback");
                                JSONObject origin = originCpList.optJSONObject(0);
                                if (origin != null) {
                                    String oCode = origin.optString("cpCode", "");
                                    String fallbackUrl = "https://slw16.global.cainiao.com/trackSyncQueryRpc/queryAllLinkTrace.json?callback=s&mailNo=" + parcelCode + "&originCp=" + oCode;
                                    String responseNotJson = getRequest(fallbackUrl).replace("s(", "");
                                    responseNotJson = responseNotJson.substring(0, responseNotJson.length() - 1);
                                    JSONObject responseJson = new JSONObject(responseNotJson);
                                    if (responseJson.optBoolean("success")) {
                                        proceedParsing(parcelObject, responseJson);
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                Log.i(TAG, "Cainiao Packet fetching failed to find the JSON");
                parcelObject.setIsFound(false); // Parcel not found
            }

        } catch (IOException | ParseException | JSONException | NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private void proceedParsing(ParcelObject parcelObject, JSONObject detailsObject) throws JSONException, ParseException {
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        parcelObject.setIsFound(true);
        parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT);
        parcelObject.setDestinationCountry(detailsObject.optString("destCountry"));

        // Parse events
        JSONObject section2 = detailsObject.optJSONObject("section2");

        if (section2 != null) {
            JSONArray details = section2.optJSONArray("detailList");
            if (details != null && details.length() > 0) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject event = details.optJSONObject(i);
                    eventObjects.add(parseEventObject(event));
                }
            }
        }

        JSONObject event = detailsObject.optJSONObject("latestTrackingInfo");
        if (event != null && eventObjects.size() < 1) {
            eventObjects.add(parseEventObject(event));
        }
        setBasicDetails(parcelObject, detailsObject);
        // Add to stack
        if (!eventObjects.isEmpty())
            parcelObject.setEventObjects(eventObjects);
        else
            parcelObject.setEventObjects(null);
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private EventObject parseEventObject(JSONObject event) throws JSONException, ParseException {
        @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        @SuppressLint("SimpleDateFormat") DateFormat apiDateFormatNoTime = new SimpleDateFormat("yyyy-MM-dd");
        @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStamp = event.optString("time");
        Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));

        final String parsedDate = showingDateFormat.format(parseTimeDate);
        final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);
        final String eventDescription = event.getString("desc");

        // Pass to object

        return new EventObject(
                eventDescription, parsedDate, parsedDateSQLiteFormat, "null", "null"
        );
    }


    @SuppressWarnings("HardCodedStringLiteral")
    private void setBasicDetails(ParcelObject parcelObject, JSONObject jsonObject) {
        String dest = jsonObject.optString("destCountry", "null");
        String status = jsonObject.optString("status", "");
        parcelObject.setDestinationCountry(dest);
        Log.i(TAG, status);
        if (status.equals("LTL_SIGNIN") || status.equals("SIGNIN") || status.equals("OWS_SIGNIN") || status.contains("WAIT4SIGNIN")) {
            parcelObject.setPhase(PhaseNumber.PHASE_DELIVERED);
        } else if (status.equals("CWS_WAIT4SIGNIN") || status.equals("LTL_WAIT4SIGNIN") || status.equals("WAIT4SIGNIN")) {
            parcelObject.setPhase(PhaseNumber.PHASE_READY_FOR_PICKUP);
        } else if (status.contains("WAIT4PICKUP")) {
            parcelObject.setPhase(PhaseNumber.PHASE_WAITING_FOR_PICKUP);
        } else if (status.contains("RETURN")) {
            parcelObject.setPhase(PhaseNumber.PHASE_RETURNED);
        }
    }


} // End of class
