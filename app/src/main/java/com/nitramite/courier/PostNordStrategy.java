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

public class PostNordStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = "PostNordStrategy";

    @Override
    public ParcelObject execute(String parcelCode) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            String url = "https://www.postnord.fi/api/pnmw/shipment/" + parcelCode + "/fi";

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", Constants.UserAgent)
                    .build();
            Response response = client.newCall(request).execute();
            String jsonResult = response.body().string();

            Log.i(TAG, jsonResult);

            // Parsing got json content
            JSONObject jsonResponse = new JSONObject(jsonResult);                       // Json content
            JSONObject jsonShipmentObj = jsonResponse.optJSONObject("response")
                    .optJSONObject("trackingInformationResponse")
                    .optJSONArray("shipments").getJSONObject(0);

            //Log.i(TAG, jsonShipmentObj.toString());

            if (!jsonShipmentObj.isNull("shipmentId")) { // Has content
                parcelObject.setIsFound(true); // Parcel is found

                parcelObject.setParcelCode2(jsonShipmentObj.optString("shipmentId"));
                parcelObject.setPhase(jsonShipmentObj.optString("status"));
                parcelObject.setEstimatedDeliveryTime(jsonShipmentObj.optString("estimatedTimeOfArrival"));
                if (!jsonShipmentObj.isNull("totalWeight")) {
                    parcelObject.setWeight(jsonShipmentObj.optJSONObject("totalWeight").optString("value"));
                }
                if (!jsonShipmentObj.isNull("assessedVolume")) {
                    parcelObject.setVolume(jsonShipmentObj.optJSONObject("assessedVolume").optString("value") + " " + jsonShipmentObj.optJSONObject("assessedVolume").optString("unit"));
                }
                if (!jsonShipmentObj.isNull("service")) {
                    parcelObject.setProduct(jsonShipmentObj.optJSONObject("service").optString("name"));
                }

                // Parse events
                JSONObject item = jsonShipmentObj.optJSONArray("items").getJSONObject(0); // Get item information
                JSONArray eventsArray = item.optJSONArray("events"); // Get item events

                //Log.i(TAG, eventsArray.toString());

                // Parcel phase handling
                final String itemStatus = item.optString("status");
                Log.i(TAG, "Postnord package status: " + itemStatus);
                if (itemStatus.equals("EN_ROUTE") || itemStatus.equals("INFORMED")) {
                    parcelObject.setPhase("TRANSIT"); // Phase
                }

                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < eventsArray.length(); i++) {
                    JSONObject event = eventsArray.getJSONObject(i);

                    String timeStamp = event.optString("eventTime");
                    Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));

                    final String parsedDate = showingDateFormat.format(parseTimeDate);
                    final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);

                    // Log.i(TAG, "After parsing date format is: " + parsedDate);
                    // Log.i(TAG, "After parsing SQLite date format is: " + parsedDateSQLiteFormat);

                    // Get event description
                    String eventDescription = event.optString("eventDescription");

                    JSONObject eventLocationObj = event.optJSONObject("location");

                    // Pass to object
                    EventObject eventObject = new EventObject(
                            eventDescription,
                            parsedDate,
                            parsedDateSQLiteFormat,
                            eventLocationObj.optString("postcode"),
                            eventLocationObj.optString("displayName")
                    );
                    // Add object
                    eventObjects.add(eventObject);
                }
                parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
            } else {
                Log.i(TAG, "PostNord shipment not found");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Log.i(TAG, e.toString());
        } catch (ParseException | NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


} // End of class
