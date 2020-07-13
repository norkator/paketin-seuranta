package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Utils;

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

public class GlsStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = "GlsStrategy";

    @Override
    public ParcelObject execute(final String parcelCode) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            String url = "https://gls-group.eu/app/service/open/rest/EU/en/rstt001?match=" + parcelCode;

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build();
            Response response = client.newCall(request).execute();
            String jsonResult = response.body().string();


            Log.i(TAG, jsonResult);

            // Parsing got json content
            JSONObject tuStatusObject = new JSONObject(jsonResult).optJSONArray("tuStatus").optJSONObject(0); // tuStatus has one object

            JSONArray eventsArray = tuStatusObject.optJSONArray("history"); // Get events
            JSONArray infoArray = tuStatusObject.optJSONArray("infos");
            JSONObject statusObject = tuStatusObject.optJSONObject("progressBar");


            if (tuStatusObject.length() > 0) {
                parcelObject.setIsFound(true); // Parcel is found

                // In transport
                if (statusObject.getString("statusInfo").equals("INTRANSIT")) {
                    parcelObject.setPhase("IN_TRANSPORT");
                }
                // TODO; Needs more statuses


                // Parse info's
                for (int f = 0; f < infoArray.length(); f++) {
                    JSONObject infoObj = new JSONObject(infoArray.get(f).toString());
                    if (infoObj.getString("type").equals("WEIGHT")) {
                        parcelObject.setWeight(infoObj.getString("value").replace("kg", ""));
                    } else if (infoObj.getString("type").equals("PRODUCT")) {
                        parcelObject.setProduct(infoObj.getString("value"));
                    }
                }


                // Parse events
                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < eventsArray.length(); i++) {
                    JSONObject event = eventsArray.getJSONObject(i);

                    String timeStamp = event.optString("date") + " " + event.optString("time");
                    Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));

                    final String parsedDate = showingDateFormat.format(parseTimeDate);
                    final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);
                    final String eventDescription = event.getString("evtDscr");

                    JSONObject locationObject = event.getJSONObject("address");
                    final String locationName = locationObject.getString("city") + ", " + locationObject.getString("countryName");


                    // Pass to object
                    EventObject eventObject = new EventObject(
                            eventDescription, parsedDate, parsedDateSQLiteFormat, "", locationName
                    );
                    // Add object
                    eventObjects.add(eventObject);

                }
                parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
            } else {
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.i(TAG, e.toString());
        } catch (IOException | ParseException | NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


} // End of class