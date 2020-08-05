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

public class BringStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = "BringStrategy";

    @Override
    public ParcelObject execute(String parcelCode) {

        // Api url
        String url = "https://tracking.bring.com/tracking/api/fetch/" + parcelCode + "?lang=en";

        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Host", "tracking.bring.com")
                    .addHeader("User-Agent", Constants.UserAgent)
                    .build();
            Response response = client.newCall(request).execute();

            String jsonResult = response.body().string();
            Log.i(TAG, jsonResult);

            // Parsing got json content
            JSONObject jsonResponse = new JSONObject(jsonResult); // Json content
            JSONObject consignmentSet = jsonResponse.getJSONArray("consignmentSet").getJSONObject(0);


            if (hasParcelDetails(consignmentSet, "packageSet")) { // parcel details exists
                parcelObject.setIsFound(true);

                JSONObject packageSet = consignmentSet.getJSONArray("packageSet").getJSONObject(0);
                parcelObject.setWeight(packageSet.optString("weightInKgs"));
                parcelObject.setHeight(packageSet.optString("heightInCm"));
                parcelObject.setWidth(packageSet.optString("widthInCm"));
                parcelObject.setDepth(packageSet.optString("lengthInCm"));
                parcelObject.setVolume(packageSet.optString("volumeInDm3"));
                parcelObject.setSender(packageSet.optString("senderName"));

                // Get events
                JSONArray jsonEvents = packageSet.getJSONArray("eventSet");

                // Only events have "phase" status, first is newest
                parcelObject.setPhase(jsonEvents.optJSONObject(0).optString("status"));

                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < jsonEvents.length(); i++) {
                    JSONObject event = jsonEvents.getJSONObject(i);


                    String timeStamp = event.optString("dateIso");
                    Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));
                    final String parsedDate = showingDateFormat.format(parseTimeDate);
                    final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);


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
                parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching


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
        } catch (NullPointerException | ParseException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


    private boolean hasParcelDetails(JSONObject jsonObject, String keyName) {
        return jsonObject.has(keyName);
    }


} // End of class
