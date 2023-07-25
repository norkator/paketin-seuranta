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

    
    private static final String TAG = SchenkerStrategy.class.getSimpleName();

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Api url
    private static final String urlShipmentId = "https://eschenker.dbschenker.com/nges-portal/public/tracking-v2/resources/api/public/shipments";
    private static final String urlShipmentDetails = "https://eschenker.dbschenker.com/nges-portal/public/tracking-v2/resources/api/public/shipments/land/";

    // private static String LOCALE_FI = "fi_FI";
    // private static String LOCALE_EN = "en_GB";

    @SuppressLint("SimpleDateFormat")
    private static DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    @SuppressLint("SimpleDateFormat")
    private static DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    @SuppressLint("SimpleDateFormat")
    private static DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
            // Log.i(TAG, "shipment details: " + shipmentDetails);

            assert shipmentDetails != null;
            JSONObject parcelJsonObject = new JSONObject(shipmentDetails);
            JSONArray events = parcelJsonObject.optJSONArray("events");

            // Status | phase parsing
            JSONArray statusArray = parcelJsonObject.optJSONArray("progressBar");
            assert statusArray != null;
            if (statusArray.getJSONObject(5).optBoolean("active")) {
                parcelObject.setPhase(PhaseNumber.PHASE_DELIVERED);
            } else if (statusArray.getJSONObject(0).optBoolean("active")) {
                parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT);
            }


            parcelObject.setProduct(parcelJsonObject.optString("product"));
            JSONObject goods = parcelJsonObject.getJSONObject("goods");
            parcelObject.setWeight(goods.optJSONObject("weight").optString("value"));
            parcelObject.setQuantity(goods.optString("pieces"));

            String agreedDeliveryTime = parcelJsonObject.optJSONObject("deliveryDate").optString("agreed");
            String estimatedDeliveryTime = parcelJsonObject.optJSONObject("deliveryDate").optString("estimated");
            Date deliveryDate = null;
            if (!agreedDeliveryTime.equals("null")) {
                deliveryDate = apiDateFormat.parse(agreedDeliveryTime);
            } else if (!estimatedDeliveryTime.equals("null")) {
                deliveryDate = apiDateFormat.parse(estimatedDeliveryTime);
            }
            parcelObject.setEstimatedDeliveryTime(
                    showingDateFormat.format(Objects.requireNonNull(deliveryDate))
            );


            // Parse events
            assert events != null;
            if (events.length() > 0) {
                parcelObject.setIsFound(true); // Parcel is found
                // Fetch data
                for (int a = 0; a < events.length(); a++) {
                    // Event obj
                    JSONObject eventJsonObject = events.optJSONObject(a);

                    String description = eventJsonObject.optString("comment");
                    String dateStr = eventJsonObject.optString("date");

                    // Construct date and time string
                    Date parseTimeDate = apiDateFormat.parse(dateStr);

                    // Format date and time for different formats
                    String finalTimeStamp = showingDateFormat.format(Objects.requireNonNull(parseTimeDate));
                    String sqliteTimeStamp = SQLiteDateFormat.format(parseTimeDate);

                    // Get location
                    JSONObject location = eventJsonObject.optJSONObject("location");
                    String locationName = location.optString("name");
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
                Log.i(TAG, "Schenker shipment not found");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (NullPointerException | JSONException | ParseException e) {
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
