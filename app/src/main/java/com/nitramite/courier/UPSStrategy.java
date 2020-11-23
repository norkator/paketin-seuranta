package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings("HardCodedStringLiteral")
public class UPSStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = "UPSStrategy";

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Api url
    private static final String url = "https://www.ups.com/track/api/Track/GetStatus?loc=fi_FI";


    @Override
    public ParcelObject execute(String parcelCode) {
        // Expected phase strings on site
        final String wordDelivered = "TOIMITETTU";
        final String wordInTransport = "Tilaus käsitelty: Valmis UPS:lle";
        final String wordInTransportTwo = "Lähtöskannaus";
        // Objects
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            OkHttpClient client = new OkHttpClient();

            final String json = "{\"Locale\":\"fi_FI\",\"TrackingNumber\":[\"" + parcelCode + "\"]}"; // TODO: make proper way
            Log.i(TAG, json);
            RequestBody body = RequestBody.create(json, JSON);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", Constants.UserAgent)
                    .build();
            Response response = client.newCall(request).execute();

            String jsonResult = response.body().string();

            Log.i(TAG, jsonResult);

            // Parsing got json content
            JSONObject jsonResponse = new JSONObject(jsonResult); // Json content
            JSONObject parcelDetails = jsonResponse.optJSONArray("trackDetails").optJSONObject(0);  // Ge tracking details, this contains array of events
            JSONArray eventsArray = parcelDetails.optJSONArray("shipmentProgressActivities");       // Parcel events array
            JSONObject additionalInformation = parcelDetails.optJSONObject("additionalInformation"); // Additional information

            // Get service "product"
            parcelObject.setProduct(additionalInformation.optJSONObject("serviceInformation").optString("serviceName").replace("&#174;", "®"));
            parcelObject.setWeight(additionalInformation.optString("weight"));
            parseAccessPointDetails(parcelDetails, parcelObject);
            parseRecipientSignature(parcelDetails, parcelObject);

            // Parse events
            if (eventsArray.length() > 0) {
                parcelObject.setIsFound(true); // Parcel is found
                // Fetch data
                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm"); // Api time is: 28.03.2018 11:48
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int a = 0; a < eventsArray.length(); a++) {
                    // Event obj
                    JSONObject eventJsonObject = eventsArray.optJSONObject(a);
                    String description = (eventJsonObject.optString("activityScan").replace("&#228;", "ä").replace("&#246;", "ö"));
                    final String dateStr = eventJsonObject.optString("date");
                    final String timeStr = eventJsonObject.optString("time");

                    //Log.i(TAG, eventJsonObject.toString());

                    // Data validity checking
                    if (!description.equals("") && !dateStr.equals("") && !timeStr.equals("")) {
                        // Event description
                        if (description.contains(wordDelivered)) {
                            parcelObject.setPhase("DELIVERED");
                        }
                        // Construct date and time string
                        Date parseTimeDate = apiDateFormat.parse(
                                dateStr + " " + timeStr
                        );
                        // Format date and time for different formats
                        String finalTimeStamp = showingDateFormat.format(parseTimeDate);
                        String sqliteTimeStamp = SQLiteDateFormat.format(parseTimeDate);
                        // Get location
                        String locationName = eventJsonObject.optString("location");
                        // Add to event object
                        EventObject eventObject = new EventObject(
                                description, finalTimeStamp, sqliteTimeStamp, "", locationName
                        );
                        eventObjects.add(eventObject);
                    }
                }
                // Set events
                parcelObject.setEventObjects(eventObjects);
                // Set phase as default if still null
                if (parcelObject.getPhase().equals("null")) {
                    parcelObject.setPhase("IN_TRANSPORT");
                }
            } else {
                Log.i(TAG, "UPS shipment not found");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (IOException | JSONException | ParseException | NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


    /**
     * Parse pick up location from details
     *
     * @param trackDetails parcel details json
     * @param parcelObject parcel object
     */
    private void parseAccessPointDetails(JSONObject trackDetails, ParcelObject parcelObject) {
        try {
            JSONObject location =
                    Objects.requireNonNull(trackDetails.optJSONObject("upsAccessPoint"))
                            .optJSONObject("location");
            assert location != null;
            parcelObject.setPickupAddress(
                    location.optString("attentionName"),
                    location.optString("streetAddress1"),
                    location.optString("zipCode"),
                    location.optString("city"),
                    location.optString(""),
                    location.optString(""),
                    location.optString("")
            );
        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Parse and save recipient signature, in this case name
     *
     * @param trackDetails parcel details json
     * @param parcelObject parcel object
     */
    private void parseRecipientSignature(JSONObject trackDetails, ParcelObject parcelObject) {
        parcelObject.setRecipientSignature(trackDetails.optString("receivedBy"));
    }


} // End of class
