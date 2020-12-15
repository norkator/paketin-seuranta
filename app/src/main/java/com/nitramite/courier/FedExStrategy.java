package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.paketinseuranta.PhaseNumber;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings("HardCodedStringLiteral")
public class FedExStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = "FedExStrategy";

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    public ParcelObject execute(String parcelCode) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            String url = "https://www.fedex.com/trackingCal/track" +
                    "?action=trackpackages&locale=en_IN&version=1&format=json&data={%22TrackPackagesRequest%22:{%22appType%22:%22WTRK%22,%22uniqueKey%22:%22%22,%22processingParameters%22:{}," +
                    "%22trackingInfoList%22:[{%22trackNumberInfo%22:{%22trackingNumber%22:%22" + parcelCode + "%22,%22trackingQualifier%22:%22%22,%22trackingCarrier%22:%22%22}}]}}&_=1421213504837";

            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create("", JSON);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Host", "www.fedex.com")
                    .addHeader("Referer", "https://www.fedex.com/apps/fedextrack/index.html?action=track&tracknumbers=" + parcelCode + "&locale=en_US&cntry_code=en")
                    .addHeader("User-Agent", Constants.UserAgent)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .build();
            Response response = client.newCall(request).execute();

            String jsonResult = response.body().string();
            Log.i(TAG, jsonResult);

            // Parsing got json content
            JSONObject jsonResponse = new JSONObject(jsonResult);                                   // Get while json content
            JSONObject trackPackagesResponse = jsonResponse.getJSONObject("TrackPackagesResponse"); // Get TrackPackagesResponse object
            JSONArray packageList = trackPackagesResponse.optJSONArray("packageList");              // Get "packageList" array
            JSONObject jsonChildNode = packageList.getJSONObject(0);                                // Get first object from "packageList" array
            JSONArray scanEventList = jsonChildNode.getJSONArray("scanEventList");                  // Events, api gives this with one null field object by default

            // Parsing
            if (jsonChildNode.length() > 0 && !scanEventList.optJSONObject(0).optString("date").equals("")) {
                parcelObject.setIsFound(true); // Parcel is found

                // Parse all package related normal data which is found
                final String keyStatusStr = jsonChildNode.getString("keyStatus").toUpperCase(Locale.getDefault());
                if (keyStatusStr.equals("LABEL CREATED") || keyStatusStr.contains("TRANSPORT") || keyStatusStr.contains("TRANSIT") || keyStatusStr.equals("SHIPMENT EXCEPTION") || keyStatusStr.equals("CLEARANCE DELAY")) {
                    parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT); // Phase
                } else if (keyStatusStr.contains("DELIVERY") || keyStatusStr.contains("DELIVERED")) {
                    parcelObject.setPhase(PhaseNumber.PHASE_DELIVERED); // Phase
                } else {
                    parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT); // Phase
                }

                parcelObject.setDestinationCountry(jsonChildNode.getString("recipientCntryCD")); // Destination
                parcelObject.setProduct(jsonChildNode.getString("serviceDesc"));
                parcelObject.setWeight(jsonChildNode.getString("displayTotalKgsWgt").replace(" kgs", ""));

                // Parse events
                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < scanEventList.length(); i++) {
                    JSONObject scanEventListObject = scanEventList.getJSONObject(i);
                    // Description
                    String description = scanEventListObject.getString("status");
                    // Date
                    String date = scanEventListObject.getString("date").replace("\\u002d", "-");
                    String time = scanEventListObject.getString("time").replace("\\u003a", ":");
                    Date apiDate = apiDateFormat.parse(date + " " + time);
                    String parsedShowingDate = showingDateFormat.format(apiDate);
                    String parsedDateSQLiteFormat = SQLiteDateFormat.format(apiDate);
                    // Location
                    String locationName = scanEventListObject.getString("scanLocation");
                    EventObject eventObject = new EventObject(
                            description, parsedShowingDate, parsedDateSQLiteFormat, "", locationName
                    );
                    eventObjects.add(eventObject);
                }
                parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
            } else {
                Log.i(TAG, "FedEx shipment not found");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (IOException | JSONException | ParseException | NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


} // End of class
