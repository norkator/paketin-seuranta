package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Locale;
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
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("HardCodedStringLiteral")
public class PostNordStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = PostNordStrategy.class.getSimpleName();

    @Override
    public ParcelObject execute(String parcelCode, final Locale locale) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            String url = "https://api2.postnord.com/rest/shipment/v5/trackandtrace/ntt/shipment/recipientview?id=" + parcelCode + "&locale=" + (locale == Locale.FI ? "fi" : "en");

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", Constants.UserAgent)
                    .build();
            Response response = client.newCall(request).execute();
            String jsonResult = response.body().string();

            Log.i(TAG, jsonResult);

            // Parsing got json content
            JSONObject jsonResponse = new JSONObject(jsonResult);                       // Json content
            JSONObject jsonShipmentObj = jsonResponse.optJSONObject("TrackingInformationResponse")
                    .optJSONArray("shipments").getJSONObject(0);

            //Log.i(TAG, jsonShipmentObj.toString());

            if (!jsonShipmentObj.isNull("shipmentId")) { // Has content
                parcelObject.setIsFound(true); // Parcel is found

                parcelObject.setParcelCode2(jsonShipmentObj.optString("shipmentId"));
                parcelObject.setPhase(jsonShipmentObj.optString("status"));
                parcelObject.setEstimatedDeliveryTime(jsonShipmentObj.optString("estimatedTimeOfArrival"));
                if (!jsonShipmentObj.isNull("totalWeight")) {
                    parcelObject.setWeight(jsonShipmentObj.optJSONObject("totalWeight").optString("value"));
                }
                if (!jsonShipmentObj.isNull("assessedVolume")) {
                    parcelObject.setVolume(jsonShipmentObj.optJSONObject("assessedVolume").optString("value") + " " + jsonShipmentObj.optJSONObject("assessedVolume").optString("unit"));
                }
                if (!jsonShipmentObj.isNull("service")) {
                    parcelObject.setProduct(jsonShipmentObj.optJSONObject("service").optString("name"));
                }

                // Parse events
                JSONObject item = jsonShipmentObj.optJSONArray("items").getJSONObject(0); // Get item information
                JSONArray eventsArray = item.optJSONArray("events"); // Get item events

                // Parcel phase handling
                String itemStatus = item.optString("status");
                Log.i(TAG, "Postnord package status: " + itemStatus);
                if (itemStatus.equals("EN_ROUTE") || itemStatus.equals("INFORMED")) {
                    parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT); // Phase
                } else {
                    parcelObject.setPhase(itemStatus);
                }

                parseSizingDetails(item, parcelObject);
                parseConsignorDetails(jsonShipmentObj, parcelObject);
                parseConsigneeDetails(jsonShipmentObj, parcelObject);


                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < eventsArray.length(); i++) {
                    JSONObject event = eventsArray.getJSONObject(i);

                    String timeStamp = event.optString("eventTime");
                    Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));

                    String parsedDate = showingDateFormat.format(parseTimeDate);
                    String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);

                    // Get event description
                    String eventDescription = event.optString("eventDescription");

                    JSONObject eventLocationObj = event.optJSONObject("location");

                    // Pass to object
                    EventObject eventObject = new EventObject(
                            eventDescription,
                            parsedDate,
                            parsedDateSQLiteFormat,
                            eventLocationObj.optString("postcode"),
                            eventLocationObj.optString("displayName")
                    );
                    // Add object
                    eventObjects.add(eventObject);
                }
                parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
            } else {
                Log.i(TAG, "PostNord shipment not found");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Log.i(TAG, e.toString());
        } catch (ParseException | NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


    /**
     * Parses sizing details from item
     *
     * @param item         item object
     * @param parcelObject parcel object for saving
     */
    private void parseSizingDetails(JSONObject item, ParcelObject parcelObject) {
        try {
            JSONObject statedMeasurement = item.optJSONObject("statedMeasurement");

            assert statedMeasurement != null;
            parcelObject.setDepth(getMetersToCentimeters(
                    Objects.requireNonNull(statedMeasurement.optJSONObject("length")).optString("value"),
                    Objects.requireNonNull(statedMeasurement.optJSONObject("length")).optString("unit")
            ));
            parcelObject.setHeight(getMetersToCentimeters(
                    Objects.requireNonNull(statedMeasurement.optJSONObject("height")).optString("value"),
                    Objects.requireNonNull(statedMeasurement.optJSONObject("height")).optString("unit")
            ));
            parcelObject.setWidth(getMetersToCentimeters(
                    Objects.requireNonNull(statedMeasurement.optJSONObject("width")).optString("value"),
                    Objects.requireNonNull(statedMeasurement.optJSONObject("width")).optString("unit")
            ));

        } catch (NullPointerException | AssertionError e) {
            Log.e(TAG, e.toString());
        }
    }


    /**
     * Converts meters to centimeters if exists
     *
     * @param value string value
     * @param unit  value unit
     * @return string with unit string
     */
    private String getMetersToCentimeters(String value, String unit) {
        if (unit.equals("m")) {
            return String.valueOf(Double.parseDouble(value) * 100) + " cm";
        } else {
            return "- cm";
        }
    }


    /**
     * Set sender name and address details
     *
     * @param jsonShipmentObj item object
     * @param parcelObject    parcel object for saving
     */
    private void parseConsignorDetails(JSONObject jsonShipmentObj, ParcelObject parcelObject) {
        try {
            JSONObject consignor = jsonShipmentObj.optJSONObject("consignor");
            assert consignor != null;
            JSONObject address = consignor.optJSONObject("address");
            assert address != null;
            parcelObject.setSender(
                    consignor.optString("name") + ", " +
                            address.optString("postCode") + ", " +
                            address.optString("country")
            );

        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
        }
    }


    /**
     * Set receiver address details
     *
     * @param jsonShipmentObj item object
     * @param parcelObject    parcel object for saving
     */
    private void parseConsigneeDetails(JSONObject jsonShipmentObj, ParcelObject parcelObject) {
        try {
            JSONObject consignee = jsonShipmentObj.optJSONObject("consignee");
            assert consignee != null;
            JSONObject address = consignee.optJSONObject("address");
            assert address != null;
            parcelObject.setDestinationCity(address.optString("city"));
            parcelObject.setDestinationPostcode(address.optString("postCode"));
            parcelObject.setDestinationCountry(address.optString("country"));

        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
        }
    }


} // End of class
