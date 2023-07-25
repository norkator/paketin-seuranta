package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.paketinseuranta.PhaseNumber;
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
import java.util.Locale;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("HardCodedStringLiteral")
public class DHLExpressStrategy implements CourierStrategy {

    
    private static final String TAG = DHLExpressStrategy.class.getSimpleName();

    // Config (rate limited 250 calls per day with a maximum of 1 call per second)
    private static final String serviceUrl = "https://api-eu.dhl.com/track/shipments";
    private static final String apiKey = "GjXLDK3A4zjZQ8YGzXER33rZbzyJL2nW";

    @Override
    public ParcelObject execute(String parcelCode, final com.nitramite.utils.Locale locale) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();

        try {
            String jsonResult = getTrackingData(parcelCode, locale);

            // Parsing got json content
            JSONObject jsonResponse = new JSONObject(jsonResult);
            JSONArray jsonMainNode = jsonResponse.optJSONArray("shipments");
            JSONObject jsonChildNode = Objects.requireNonNull(jsonMainNode).getJSONObject(0);


            if (jsonChildNode.length() > 0) {
                parcelObject.setIsFound(true); // Parcel is found

                // Parse all package related normal data found
                if (jsonChildNode.has("status")) {
                    String phase = jsonChildNode.getJSONObject("status").getString("status").toUpperCase(Locale.getDefault());
                    if (phase.equals(PhaseNumber.PHASE_TRANSIT)) {
                        phase = PhaseNumber.PHASE_IN_TRANSPORT;
                    }
                    parcelObject.setPhase(phase); // Phase
                } else {
                    parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT); // Set as in transport phase since it's still coming
                }
                parcelObject.setDestinationCountry(jsonChildNode.getJSONObject("destination")
                        .getJSONObject("address").getString("addressLocality")); // Destination
                parcelObject.setProduct(jsonChildNode.getString("service"));


                // Parse events
                JSONArray events = jsonChildNode.getJSONArray("events");
                // Declare time formats here
                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < events.length(); i++) {
                    JSONObject event = events.getJSONObject(i);

                    String description = event.getString("description");

                    String timeStamp = event.optString("timestamp");
                    Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));
                    final String parsedDate = showingDateFormat.format(parseTimeDate);
                    final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);

                    String locationName = event.getJSONObject("location").getJSONObject("address").getString("addressLocality");

                    EventObject eventObject = new EventObject(
                            description, parsedDate, parsedDateSQLiteFormat, "", locationName
                    );
                    eventObjects.add(eventObject);
                }
                parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
            } else {
                Log.i(TAG, "DHL express shipment not found");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (NullPointerException | IllegalArgumentException | JSONException | ParseException | IOException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


    /**
     * Get tracking data
     *
     * @return tracking data in json format
     * @throws IOException in case of failure
     */
    private String getTrackingData(String parcelCode, final com.nitramite.utils.Locale locale) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(serviceUrl + "?trackingNumber=" + parcelCode + "&service=express&originCountryCode=FI&requesterCountryCode=" + (locale == com.nitramite.utils.Locale.FI ? "FI" : "EN"))
                .addHeader("User-Agent", Constants.UserAgent)
                .addHeader("Accept", Constants.ContentType)
                .addHeader("DHL-API-Key", apiKey)
                .build();
        Response response = client.newCall(request).execute();
        return Objects.requireNonNull(response.body()).string();
    }

}
