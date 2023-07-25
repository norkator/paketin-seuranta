package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Locale;
import com.nitramite.paketinseuranta.PhaseNumber;

import org.jetbrains.annotations.NonNls;
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

    @NonNls
    private static final String TAG = ArraPakettiStrategy.class.getSimpleName();

    @NonNls
    private static final String url = "https://www.r-kioski.fi/wordpress/wp-admin/admin-ajax.php";

    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public ParcelObject execute(String parcelCode, final Locale locale) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
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
                    .addHeader("User-Agent", Constants.UserAgent)
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
                    parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT);
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

} 
