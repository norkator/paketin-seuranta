package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Locale;
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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings("HardCodedStringLiteral")
public class PostiStrategy implements CourierStrategy {

    
    private static final String TAG = PostiStrategy.class.getSimpleName();

    private static final String TOKEN_URL = "https://auth-service.posti.fi/api/v1/anonymous_token";
    private static final String PARCEL_URL = "https://oma.posti.fi/graphql/v2";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    public ParcelObject execute(final String parcelCode, final Locale locale) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            PostiToken postiToken = getTokens();
            RequestBody postBody = RequestBody.create(
                    getGraphQlQueryBody(parcelCode), JSON
            );
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(PARCEL_URL)
                    .addHeader("User-Agent", Constants.UserAgent)
                    .addHeader("content-type", Constants.ContentType)
                    .addHeader("authorization", "Bearer " + postiToken.getIdToken())
                    .addHeader("x-omaposti-roles", postiToken.getRoleToken())
                    .post(postBody)
                    .build();
            Response response = client.newCall(request).execute();
            String jsonResult = response.body().string();
            // Log.i(TAG, "Post GraphQl body");
            // Log.i(TAG, jsonResult);


            JSONObject body = new JSONObject(jsonResult);
            JSONArray shipmentView = body.optJSONObject("data").optJSONArray("shipmentView");
            JSONObject jsonChildNode = shipmentView.getJSONObject(0);


            if (shipmentView.length() > 0) {
                parcelObject.setIsFound(true); // Parcel is found

                JSONObject parcel = jsonChildNode.optJSONObject("parcel");

                parcelObject.setParcelCode2(parcel.optString("otherTrackingNumber"));
                parcelObject.setErrandCode(parcel.optString("errandCode"));
                parcelObject.setPhase(parcel.optJSONObject("status").optString("code"));

                // Parse estimate delivery time
                try {
                    @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ
                    @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    String timeStamp = parcel.optString("estimatedDeliveryTime");
                    Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));
                    final String parsedDate = showingDateFormat.format(parseTimeDate);
                    parcelObject.setEstimatedDeliveryTime(parsedDate);
                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }

                if (!parcel.isNull("pickupPoint")) {
                    JSONObject pickupPoint = parcel.optJSONObject("pickupPoint");
                    parcelObject.setPickupAddress(
                            pickupPoint.optString("street1"),
                            pickupPoint.optString("street2"),
                            pickupPoint.optString("postcode"),
                            pickupPoint.optString("city"),
                            pickupPoint.optString("latitude"),
                            pickupPoint.optString("longitude"),
                            pickupPoint.optString("availabilityTime")
                    );
                }

                // Parse last pickup date
                try {
                    @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd"); // yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ
                    @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy");
                    String timeStamp = parcel.optString("lastCollectionDate");
                    Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));
                    final String parsedDate = showingDateFormat.format(parseTimeDate);
                    parcelObject.setLastPickupDate(parsedDate);
                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }

                parcelObject.setWeight(parcel.optJSONObject("weight").optString("value"));
                parcelObject.setHeight(parcel.optJSONObject("height").optString("value"));
                parcelObject.setWidth(parcel.optJSONObject("width").optString("value"));
                parcelObject.setDepth(parcel.optJSONObject("length").optString("value"));
                parcelObject.setVolume(parcel.optJSONObject("volume").optString("value"));

                parcelObject.setDestinationPostcode(parcel.optJSONObject("destination").optString("postcode"));
                parcelObject.setDestinationCity(parcel.optJSONObject("destination").optString("city"));
                parcelObject.setDestinationCountry(parcel.optJSONObject("destination").optString("country"));


                // Parse events
                String userLocale = locale == Locale.FI ? "fi" : "en";
                JSONArray eventsArray = parcel.optJSONArray("events");
                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < eventsArray.length(); i++) {

                    JSONObject event = eventsArray.getJSONObject(i);

                    String timeStamp = event.optString("timestamp");
                    Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));

                    final String parsedDate = showingDateFormat.format(parseTimeDate);
                    final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);

                    String eventDescription = "-";
                    JSONArray descriptions = event.optJSONArray("eventDescription");
                    for (int d = 0; d < descriptions.length(); d++) {
                        JSONObject desc = descriptions.optJSONObject(d);
                        if (desc.optString("lang").equals(userLocale)) {
                            eventDescription = desc.optString("value");
                        }
                    }

                    // Pass to object
                    EventObject eventObject = new EventObject(
                            eventDescription, parsedDate, parsedDateSQLiteFormat,
                            event.optString("eventCode"), event.optString("city")
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


    /**
     * Get required tokens for graphql call
     *
     * @return
     * @throws IOException
     * @throws JSONException
     */
    private PostiToken getTokens() throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();
        RequestBody postBody = RequestBody.create("", JSON);
        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .addHeader("User-Agent", Constants.UserAgent)
                .addHeader("content-type", Constants.ContentType)
                .post(postBody)
                .build();
        Response response = client.newCall(request).execute();
        String body = response.body().string();
        // Log.i(TAG, "Token response body");
        // Log.i(TAG, body);
        JSONObject jsonBody = new JSONObject(body);
        JSONArray roleTokens = jsonBody.optJSONArray("role_tokens");
        JSONObject first = roleTokens.optJSONObject(0);
        return new PostiToken(
                jsonBody.optString("id_token"),
                first.optString("token")
        );
    }


    /**
     * Object holding tokens
     */
    private class PostiToken {
        private String idToken;
        private String roleToken;

        PostiToken(String idToken, String roleToken) {
            this.idToken = idToken;
            this.roleToken = roleToken;
        }

        public String getIdToken() {
            return idToken;
        }

        public String getRoleToken() {
            return roleToken;
        }

    }


    /**
     * Get graphql query for posti api
     *
     * @param parcelCode
     * @return
     * @throws JSONException
     */
    private String getGraphQlQueryBody(String parcelCode) throws JSONException {
        JSONObject query = new JSONObject();
        query.put("operationName", "getShipmentView");
        JSONObject variables = new JSONObject();
        variables.put("externalCode", parcelCode);
        query.putOpt("variables", variables);
        query.put("query", "query getShipmentView($externalCode: String) {\n  shipmentView(externalCode: $externalCode) {\n    id\n    displayId\n    displayName\n    shipmentType\n    userRole\n    parcel {\n      errandCode\n      otherTrackingNumber\n      estimatedDeliveryTime\n      selectedEarliestDeliveryTime\n      selectedLatestDeliveryTime\n      confirmedEarliestDeliveryTime\n      confirmedLatestDeliveryTime\n      lastCollectionDate\n      cashOnDelivery {\n        amount\n        currency\n        __typename\n      }\n      postpayValue {\n        amount\n        currency\n        __typename\n      }\n      createdAt\n      departure {\n        city\n        country\n        postcode\n        __typename\n      }\n      destination {\n        city\n        country\n        postcode\n        __typename\n      }\n      events {\n        city\n        eventCode\n        eventDescription {\n          lang\n          value\n          __typename\n        }\n        reasonCode\n        reasonDescription {\n          lang\n          value\n          __typename\n        }\n        recipientSignature\n        timestamp\n        lockerDetails {\n          lockerCode\n          lockerAddress\n          lockerDescription\n          lockerID\n          lockerRackID\n          __typename\n        }\n        shelfId\n        __typename\n      }\n      modifiedAt\n      parties {\n        consignee {\n          ...party\n          __typename\n        }\n        consignor {\n          ...party\n          __typename\n        }\n        delivery {\n          ...party\n          __typename\n        }\n        payer {\n          ...party\n          __typename\n        }\n        __typename\n      }\n      pickupPoint {\n        availabilityTime\n        city\n        country\n        county\n        latitude\n        locationCode\n        longitude\n        postcode\n        province\n        pupCode\n        state\n        street1\n        street2\n        street3\n        type\n        codPayableOnLocation\n        __typename\n      }\n      references {\n        consignor\n        postiOrderNumber\n        __typename\n      }\n      status {\n        code\n        description {\n          lang\n          value\n          __typename\n        }\n        __typename\n      }\n      trackingNumber\n      volume {\n        unit\n        value\n        __typename\n      }\n      weight {\n        unit\n        value\n        __typename\n      }\n      width {\n        ...length\n        __typename\n      }\n      height {\n        ...length\n        __typename\n      }\n      length {\n        ...length\n        __typename\n      }\n      __typename\n    }\n    parcelExtensions {\n      actions {\n        actionType\n        actionUrl\n        __typename\n      }\n      exceptions {\n        exceptionType\n        __typename\n      }\n      powerOfAttorneyStatus\n      widget {\n        hasWidget\n        url\n        __typename\n      }\n      displayOptions {\n        type\n        __typename\n      }\n      deliveryMethod {\n        type\n        __typename\n      }\n      senderOptions {\n        type\n        __typename\n      }\n      digitalDeclaration {\n        status\n        action {\n          type\n          url\n          __typename\n        }\n        __typename\n      }\n      customsClearance {\n        status\n        __typename\n      }\n      general {\n        omaPostiShipmentUrl\n        __typename\n      }\n      __typename\n    }\n    freight {\n      cashOnDelivery {\n        amount\n        currency\n        __typename\n      }\n      selectedEarliestDeliveryTime\n      selectedLatestDeliveryTime\n      confirmedEarliestDeliveryTime\n      confirmedLatestDeliveryTime\n      createdAt\n      departure {\n        city\n        country\n        postcode\n        __typename\n      }\n      destination {\n        city\n        country\n        postcode\n        __typename\n      }\n      events {\n        city\n        eventCode\n        eventDescription {\n          lang\n          value\n          __typename\n        }\n        reasonCode\n        reasonDescription {\n          lang\n          value\n          __typename\n        }\n        recipientSignature\n        timestamp\n        __typename\n      }\n      goodsItems {\n        packageQuantity {\n          unit\n          value\n          __typename\n        }\n        packages {\n          trackingNumber\n          events {\n            city\n            eventCode\n            eventDescription {\n              lang\n              value\n              __typename\n            }\n            reasonCode\n            reasonDescription {\n              lang\n              value\n              __typename\n            }\n            recipientSignature\n            timestamp\n            __typename\n          }\n          __typename\n        }\n        __typename\n      }\n      modifiedAt\n      product {\n        additionalInfo {\n          lang\n          value\n          __typename\n        }\n        code\n        name {\n          lang\n          value\n          __typename\n        }\n        __typename\n      }\n      references {\n        consignor\n        postiOrderNumber\n        __typename\n      }\n      status {\n        code\n        description {\n          lang\n          value\n          __typename\n        }\n        __typename\n      }\n      parties {\n        consignee {\n          ...party\n          __typename\n        }\n        consignor {\n          ...party\n          __typename\n        }\n        __typename\n      }\n      totalLoadingMeters {\n        unit\n        value\n        __typename\n      }\n      totalPackageQuantity {\n        unit\n        value\n        __typename\n      }\n      totalWeight {\n        unit\n        value\n        __typename\n      }\n      urls {\n        longEPodUrl\n        __typename\n      }\n      waybillNumber\n      deliveryDate {\n        ...dateRange\n        __typename\n      }\n      pickupDate {\n        ...dateRange\n        __typename\n      }\n      __typename\n    }\n    freightExtensions {\n      actions {\n        actionType\n        actionUrl\n        __typename\n      }\n      displayOptions {\n        type\n        __typename\n      }\n      deliveryMethod {\n        type\n        __typename\n      }\n      __typename\n    }\n    aftershipParcel {\n      courier\n      courierData {\n        country\n        defaultLanguage\n        iconUrl\n        id\n        name\n        otherLanguages\n        otherName\n        phone\n        url\n        __typename\n      }\n      departure {\n        city\n        country\n        postcode\n        __typename\n      }\n      destination {\n        city\n        country\n        postcode\n        __typename\n      }\n      estimatedDeliveryTime\n      selectedEarliestDeliveryTime\n      selectedLatestDeliveryTime\n      confirmedEarliestDeliveryTime\n      confirmedLatestDeliveryTime\n      events {\n        city\n        country\n        eventAdditionalInfo {\n          lang\n          value\n          __typename\n        }\n        eventCode\n        eventDescription {\n          lang\n          value\n          __typename\n        }\n        eventShortName {\n          lang\n          value\n          __typename\n        }\n        postcode\n        reasonCode\n        timestamp\n        __typename\n      }\n      modifiedAt\n      parties {\n        consignee {\n          ...party\n          __typename\n        }\n        consignor {\n          ...party\n          __typename\n        }\n        __typename\n      }\n      pickupPoint {\n        city\n        country\n        postcode\n        __typename\n      }\n      status {\n        code\n        description {\n          lang\n          value\n          __typename\n        }\n        __typename\n      }\n      trackingNumber\n      __typename\n    }\n    pendingTracking {\n      courier\n      courierData {\n        country\n        defaultLanguage\n        iconUrl\n        id\n        name\n        otherLanguages\n        otherName\n        phone\n        url\n        __typename\n      }\n      isPlusShipment\n      modifiedAt\n      trackingNumber\n      waybillNumber\n      __typename\n    }\n    __typename\n  }\n}\n\nfragment length on TrackingLength {\n  unit\n  value\n  __typename\n}\n\nfragment party on ShipmentViewParty {\n  name1\n  city\n  country\n  postcode\n  state\n  street1\n  street2\n  street3\n  account\n  __typename\n}\n\nfragment dateRange on TrackingDateRange {\n  earliest\n  latest\n  __typename\n}\n");
        return query.toString();
    }


} 