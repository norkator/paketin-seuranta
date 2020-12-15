package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Locale;
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

import javax.net.ssl.SSLException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("HardCodedStringLiteral")
public class MatkahuoltoStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = MatkahuoltoStrategy.class.getSimpleName();


    @Override
    public ParcelObject execute(String parcelCode, final Locale locale) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            String url = "https://www.api.matkahuolto.io/search/trackingInfo?language=" + (locale == Locale.FI ? "fi" : "en") + "&parcelNumber=" + parcelCode;

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", Constants.UserAgent)
                    .build();
            Response response = client.newCall(request).execute();
            String jsonResult = response.body().string();


            JSONObject jsonResponse = new JSONObject(jsonResult);
            JSONArray trackingEvents = jsonResponse.optJSONArray("trackingEvents");

            if (trackingEvents.length() > 0) {
                parcelObject.setIsFound(true); // Parcel is found
                parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT);

                // Get product type
                parcelObject.setProduct(jsonResponse.optString("productCategory"));

                // Parse events
                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < trackingEvents.length(); i++) {
                    JSONObject event = trackingEvents.getJSONObject(i);

                    String timeStamp = event.optString("date") + " " + event.optString("time");
                    Date parseTimeDate = apiDateFormat.parse(timeStamp);

                    final String parsedDate = showingDateFormat.format(parseTimeDate);
                    final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);

                    // Log.i(TAG, "After parsing date format is: " + parsedDate);
                    // Log.i(TAG, "After parsing SQLite date format is: " + parsedDateSQLiteFormat);

                    String eventDescription = event.optString("description");

                    String place = "";
                    if (!event.optString("place").equals("null")) {
                        place = event.optString("place");
                    }

                    // Pass to object
                    EventObject eventObject = new EventObject(
                            eventDescription, parsedDate, parsedDateSQLiteFormat, "", place
                    );
                    // Add object
                    eventObjects.add(eventObject);

                }
                parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
            } else {
                Log.i(TAG, "Matkahuolto shipment not found");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.i(TAG, e.toString());
        } catch (SSLException e) {
            Log.i(TAG, "RESET BY PEER FOR " + parcelCode);
            parcelObject.setUpdateFailed(true);
            e.printStackTrace();
        } catch (IOException | ParseException | NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }

} // End of class