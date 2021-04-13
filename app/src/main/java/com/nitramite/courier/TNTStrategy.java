package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.paketinseuranta.PhaseNumber;
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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings({"HardCodedStringLiteral", "FieldCanBeLocal"})
public class TNTStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = TNTStrategy.class.getSimpleName();

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Api url
    private static final String url = "https://www.tnt.com/api/v3/shipment";

    private static String LOCALE_FI = "fi_FI";
    private static String LOCALE_EN = "en_GB";

    @Override
    public ParcelObject execute(String parcelCode, final Locale locale) {

        // Objects
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url + "?con=" + parcelCode + "&locale=" + (locale == Locale.FI ? LOCALE_FI : LOCALE_EN) + "&searchType=CON&channel=OPENTRACK")
                    .addHeader("User-Agent", Constants.UserAgent)
                    .build();
            Response response = client.newCall(request).execute();
            String jsonResult = response.body().string();

            JSONObject jsonResponse = new JSONObject(jsonResult);
            JSONArray parcelObjects = Objects.requireNonNull(jsonResponse.optJSONObject("tracker.output")).optJSONArray("consignment");
            JSONObject parcelJsonObject = Objects.requireNonNull(parcelObjects).optJSONObject(0);
            Log.i(TAG, "TNT Parcel: " + parcelJsonObject.toString());


            // Origin address
            JSONObject originAddress = parcelJsonObject.optJSONObject("originAddress");
            parcelObject.setPickupAddressCity(originAddress.optString("city") + ", " + originAddress.optString("country"));

            // Destination address
            JSONObject destAddress = parcelJsonObject.optJSONObject("destinationAddress");
            parcelObject.setDestinationCity(destAddress.optString("city"));
            parcelObject.setDestinationCountry(destAddress.optString("country"));

            // Status (phase)
            JSONObject status = parcelJsonObject.optJSONObject("status");
            if (status != null) {
                if (status.optBoolean("isDelivered")) {
                    parcelObject.setPhase(PhaseNumber.PHASE_DELIVERED);
                } else if (status.optBoolean("isPending")) {
                    parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT);
                }
            }

            // Parse events
            JSONArray events = parcelJsonObject.optJSONArray("events");
            if (events.length() > 0) {
                parcelObject.setIsFound(true); // Parcel is found
                // Fetch data
                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int a = 0; a < events.length(); a++) {
                    // Event obj
                    JSONObject eventJsonObject = events.optJSONObject(a);

                    String description = eventJsonObject.optString("statusDescription");
                    String dateStr = eventJsonObject.optString("date");

                    // Construct date and time string
                    Date parseTimeDate = apiDateFormat.parse(dateStr);

                    // Format date and time for different formats
                    String finalTimeStamp = showingDateFormat.format(Objects.requireNonNull(parseTimeDate));
                    String sqliteTimeStamp = SQLiteDateFormat.format(parseTimeDate);

                    // Get location
                    JSONObject location = eventJsonObject.optJSONObject("location");
                    String locationName = Objects.requireNonNull(location).optString("city") + ", " + location.optString("country");
                    String locationCode = location.optString("countryCode");


                    // Add to event object
                    EventObject eventObject = new EventObject(
                            description, finalTimeStamp, sqliteTimeStamp, locationCode, locationName
                    );
                    eventObjects.add(eventObject);
                }

                // Set events
                parcelObject.setEventObjects(eventObjects);
            } else {
                Log.i(TAG, "TNT shipment not found");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (NullPointerException | IOException | JSONException | ParseException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


}
