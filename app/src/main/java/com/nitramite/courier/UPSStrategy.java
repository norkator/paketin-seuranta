package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.gson.JsonArray;
import com.nitramite.courier.ups.UpsTokenPair;
import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Locale;

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

import kotlin.Pair;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings("HardCodedStringLiteral")
public class UPSStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = UPSStrategy.class.getSimpleName();

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Api url
    private static final String tokenUrl = "https://www.ups.com/track?loc=en_US";
    private static final String url = "https://www.ups.com/track/api/Track/GetStatus?loc=";

    private static String LOCALE_FI = "fi_FI";
    private static String LOCALE_EN = "en_US";

    @Override
    public ParcelObject execute(String parcelCode, final Locale locale) {
        // Expected phase strings on site
        final String wordDelivered = locale == Locale.FI ? "TOIMITETTU" : "DELIVERED";

        // Objects
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            UpsTokenPair tokenPair = getXSRFToken();
            if (tokenPair.getX_CSRF_TOKEN() == null || tokenPair.getX_XSRF_TOKEN_ST() == null) {
                throw new ParseException("Invalid X-XSRF-TOKEN, got null", 0);
            }

            OkHttpClient client = new OkHttpClient();

            JSONArray trackingNumbers = new JSONArray();
            trackingNumbers.put(parcelCode);
            JSONObject requestBody = new JSONObject();
            requestBody.put("Locale", locale == Locale.FI ? LOCALE_FI : LOCALE_EN);
            requestBody.putOpt("TrackingNumber", (Object) trackingNumbers);
            
            Log.i(TAG, requestBody.toString());

            RequestBody body = RequestBody.create(requestBody.toString(), JSON);

            Request request = new Request.Builder()
                    .url(url + (locale == Locale.FI ? LOCALE_FI : LOCALE_EN))
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", Constants.UserAgent)
                    .addHeader("Cookie", tokenPair.getX_CSRF_TOKEN())
                    .addHeader("X-XSRF-TOKEN", tokenPair.getX_XSRF_TOKEN_ST())
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
                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat(locale == Locale.FI ? "dd.MM.yyyy HH:mm" : "MM/dd/yyyy h:mm");
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


    /**
     * Get and parse token required for api call
     *
     * @return X-XSRF-TOKEN-ST token
     * @throws IOException
     */
    private UpsTokenPair getXSRFToken() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(tokenUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", Constants.UserAgent)
                .build();
        Response response = client.newCall(request).execute();

        String X_CSRF_TOKEN = null;
        String X_XSRF_TOKEN_ST = null;

        Headers headerList = response.headers();
        for (Pair<? extends String, ? extends String> header : headerList) {
            // Log.i(TAG, header.getSecond());
            String second = header.getSecond();
            if (second.contains("X-CSRF-TOKEN") && !second.contains("X-XSRF-TOKEN-ST")) {
                X_CSRF_TOKEN = second.split(";")[0].replace("X-CSRF-TOKEN=", "");
            } else if (second.contains("X-XSRF-TOKEN-ST")) {
                X_XSRF_TOKEN_ST = header.getSecond().split(";")[0].replace("X-XSRF-TOKEN-ST=", "");
            }
        }

        return new UpsTokenPair(X_CSRF_TOKEN, X_XSRF_TOKEN_ST);
    }

} // End of class
