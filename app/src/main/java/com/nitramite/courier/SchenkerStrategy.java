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
    private static final String urlShipmentDetails = "https://eschenker.dbschenker.com/nges-portal/public/tracking-v2/resources/api/public/shipments/land/";

    private static String LOCALE_FI = "fi_FI";
    private static String LOCALE_EN = "en_GB";

    @Override
    public ParcelObject execute(String parcelCode, final Locale locale) {

        // Objects
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            String shipmentId = getShipmentId(parcelCode);
            Log.i(TAG, "shipment id: " + shipmentId);

            if (shipmentId == null) {
                parcelObject.setIsFound(false);
                return parcelObject;
            }

            String shipmentDetails = getShipmentDetails(shipmentId);
            Log.i(TAG, "shipment details: " + shipmentDetails);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


    /**
     * Return shipment id for provided tracking number
     *
     * @param parcelCode which is ShippersRefNo
     * @return id from Schenker system or null if not found
     */
    private String getShipmentId(String parcelCode) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(urlShipmentId + "?query=" + parcelCode + "&referenceType=ShippersRefNo")
                    .addHeader("User-Agent", Constants.UserAgent)
                    .build();
            Response response = client.newCall(request).execute();
            String jsonResult = response.body().string();
            Log.i(TAG, "ShipmentId query: " + jsonResult);
            JSONObject jsonResponse = new JSONObject(jsonResult);
            JSONArray jsonArray = jsonResponse.getJSONArray("result");
            if (jsonArray.length() == 0) {
                return null;
            }
            return jsonArray.getJSONObject(0).optString("id");
        } catch (NullPointerException | IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Get normal shipment details and events
     *
     * @param shipmentId from shipment id query
     * @return shipment details in string form
     */
    private String getShipmentDetails(String shipmentId) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(urlShipmentDetails + shipmentId)
                    .addHeader("User-Agent", Constants.UserAgent)
                    .build();
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}
