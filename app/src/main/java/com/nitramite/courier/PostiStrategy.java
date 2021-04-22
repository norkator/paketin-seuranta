package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Locale;
import com.nitramite.utils.LocaleUtils;
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

import javax.net.ssl.SSLException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("HardCodedStringLiteral")
public class PostiStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = "PostiStrategy";

    @Override
    public ParcelObject execute(final String parcelCode, final Locale locale) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            String url = "https://www.posti.fi/henkiloasiakkaat/seuranta/api/shipments/" + parcelCode;

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", Constants.UserAgent)
                    .build();
            Response response = client.newCall(request).execute();
            String jsonResult = response.body().string();


            // Parsing got json content
            // Log.i(TAG, "Parsing posti: " + parcelCode);
            JSONObject jsonResponse = new JSONObject(jsonResult);                       // Json content
            JSONArray jsonMainNode = jsonResponse.optJSONArray("shipments");            // Get "shipments" array
            JSONObject jsonChildNode = jsonMainNode.getJSONObject(0);                   // Get first object from "shipments" array

            // Log.i(TAG, jsonChildNode.toString());

            if (jsonChildNode.length() > 0) {
                parcelObject.setIsFound(true); // Parcel is found

                parcelObject.setParcelCode2(jsonChildNode.optString("trackingCode"));
                parcelObject.setErrandCode(jsonChildNode.optString("errandCode"));
                parcelObject.setPhase(jsonChildNode.optString("phase"));


                // Parse estimate delivery time
                try {
                    @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ
                    @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    String timeStamp = jsonChildNode.optString("estimatedDeliveryTime");
                    Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));
                    final String parsedDate = showingDateFormat.format(parseTimeDate);
                    parcelObject.setEstimatedDeliveryTime(parsedDate);
                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }


                if (!jsonChildNode.isNull("pickupAddress")) {
                    JSONObject pickupAddress = jsonChildNode.getJSONObject("pickupAddress");
                    if (pickupAddress.length() > 4) {
                        parcelObject.setPickupAddress(
                                pickupAddress.optString("name"),
                                pickupAddress.optString("street"),
                                pickupAddress.optString("postcode"),
                                pickupAddress.optString("city"),
                                pickupAddress.optString("latitude"),
                                pickupAddress.optString("longitude"),
                                pickupAddress.optString("availability")
                        );
                    }
                }


                // Parse last pickup date
                try {
                    @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd"); // yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ
                    @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy");
                    String timeStamp = jsonChildNode.optString("lastPickupDate");
                    Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));
                    final String parsedDate = showingDateFormat.format(parseTimeDate);
                    parcelObject.setLastPickupDate(parsedDate);
                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }

                if (jsonChildNode.getJSONObject("product").has("name")) {
                    if (!jsonChildNode.getJSONObject("product").has("name")) {
                        parcelObject.setProduct(
                                jsonChildNode.getJSONObject("product").getJSONObject("name").optString(locale == Locale.FI? "fi" : "en")
                        );
                    }
                }
                parcelObject.setSender(jsonChildNode.optString("sender"));
                parcelObject.setLockerCode(jsonChildNode.optString("lockerCode"));
                JSONArray extraServicesArray = jsonChildNode.getJSONArray("extraServices");
                if (extraServicesArray.length() > 0) {
                    JSONObject extraServiceObj = extraServicesArray.getJSONObject(0); // Get only first
                    if (extraServiceObj != null) {
                        JSONObject extraServiceNameObj = extraServiceObj.optJSONObject("name");
                        if (extraServiceNameObj != null) {
                            parcelObject.setExtraServices(
                                    extraServiceNameObj.optString(locale == Locale.FI? "fi" : "en")
                            );
                        }
                    }
                }
                parcelObject.setWeight(jsonChildNode.optString("weight"));
                parcelObject.setHeight(jsonChildNode.optString("height"));
                parcelObject.setWidth(jsonChildNode.optString("width"));
                parcelObject.setDepth(jsonChildNode.optString("depth"));
                parcelObject.setVolume(jsonChildNode.optString("volume"));
                parcelObject.setDestinationPostcode(jsonChildNode.optString("destinationPostcode"));
                parcelObject.setDestinationCity(jsonChildNode.optString("destinationCity"));
                parcelObject.setDestinationCountry(jsonChildNode.optString("destinationCountry"));
                parcelObject.setRecipientSignature(jsonChildNode.optString("recipientSignature"));
                parcelObject.setCodAmount(jsonChildNode.optString("codAmount"));
                parcelObject.setCodCurrency(jsonChildNode.optString("codCurrency"));


                // Parse events
                JSONArray eventsArray = jsonChildNode.optJSONArray("events");
                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < eventsArray.length(); i++) {
                    JSONObject event = eventsArray.getJSONObject(i);

                    String timeStamp = event.optString("timestamp");
                    Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));

                    final String parsedDate = showingDateFormat.format(parseTimeDate);
                    final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);

                    // Log.i(TAG, "After parsing date format is: " + parsedDate);
                    // Log.i(TAG, "After parsing SQLite date format is: " + parsedDateSQLiteFormat);

                    JSONObject eventJsonObj = eventsArray.optJSONObject(i);
                    JSONObject eventObj = eventJsonObj.optJSONObject("description");
                    String eventDescription = "-";
                    if (eventObj != null) {
                        eventDescription = eventObj.optString(locale == Locale.FI? "fi" : "en");
                    }

                    // Pass to object
                    EventObject eventObject = new EventObject(
                            eventDescription, parsedDate, parsedDateSQLiteFormat, event.optString("locationCode"), event.optString("locationName")
                    );
                    // Add object
                    eventObjects.add(eventObject);

                }
                parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
            } else {
                Log.i(TAG, "Posti shipment not found");
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