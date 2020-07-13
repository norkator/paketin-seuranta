package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ArraPakettiStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = "ArraPakettiStrategy";

    @Override
    public ParcelObject execute(String parcelCode) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            String url = "https://www.r-kioski.fi/wordpress/wp-admin/admin-ajax.php";

            OkHttpClient client = new OkHttpClient();
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("action", "parcel_tracking")
                    .addFormDataPart("parcel_id", parcelCode)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Host", "www.r-kioski.fi")
                    .addHeader("Referer", "https://www.r-kioski.fi/lahetystenseuranta/")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36")
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .build();
            Response response = client.newCall(request).execute();


            String jsonResult = response.body().string();
            Log.i(TAG, jsonResult);

            // Parsing got json content
            JSONObject jsonResponse = new JSONObject(jsonResult); // Json content
            JSONArray jsonEvents = jsonResponse.optJSONArray("Seurantatiedot"); // Get tracking details

            if (jsonEvents.length() > 0) { // Has content

                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < jsonEvents.length(); i++) {
                    JSONObject event = jsonEvents.getJSONObject(i);

                    if (!event.isNull("Kellonaika")) {

                        Long timeStamp = (event.optLong("Kellonaika") * 1000);
                        Date parseTimeDate = new Date(timeStamp);

                        final String parsedDate = showingDateFormat.format(parseTimeDate);
                        final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);


                        // Get event description
                        String eventDescription = event.optString("Tapahtuma");

                        if (!eventDescription.contains("ei löydy vielä tietoja.")) {

                            String location = "";
                            if (!event.isNull("Paikka")) {
                                location = event.optString("Paikka");
                            }

                            // Pass to object
                            EventObject eventObject = new EventObject(
                                    eventDescription,
                                    parsedDate,
                                    parsedDateSQLiteFormat,
                                    "",
                                    location
                            );
                            // Add object
                            eventObjects.add(eventObject);
                            Log.i(TAG, "arra add event obj");

                        }
                    }
                }
                parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching

                if (eventObjects.size() > 0) {
                    parcelObject.setIsFound(true); // Parcel is found
                    parcelObject.setPhase("TRANSIT");
                }

            } else {
                Log.i(TAG, "ArraPaketti shipment not found");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.i(TAG, e.toString());
        } catch (IOException e) {
            Log.i(TAG, e.toString());
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


} // End of class
