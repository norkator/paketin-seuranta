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
public class SchenkerStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = SchenkerStrategy.class.getSimpleName();

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Api url
    private static final String urlShipmentId = "https://eschenker.dbschenker.com/nges-portal/public/tracking-v2/resources/api/public/shipments";
    private static final String urlShipmentDetails = "https://eschenker.dbschenker.com/nges-portal/public/tracking-v2/resources/api/public/shipments/land/"; // + shipmentId

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
                    .url(urlShipmentId + "?query=" + parcelCode + "&referenceType=ShippersRefNo")
                    .addHeader("User-Agent", Constants.UserAgent)
                    .build();
            Response response = client.newCall(request).execute();
            String jsonResult = response.body().string();

            JSONObject jsonResponse = new JSONObject(jsonResult);
            JSONArray parcelObjects = Objects.requireNonNull(jsonResponse.optJSONObject("tracker.output")).optJSONArray("consignment");
            JSONObject parcelJsonObject = Objects.requireNonNull(parcelObjects).optJSONObject(0);
            Log.i(TAG, "Schenker Parcel: " + parcelJsonObject.toString());



        } catch (NullPointerException | IOException | JSONException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


}
