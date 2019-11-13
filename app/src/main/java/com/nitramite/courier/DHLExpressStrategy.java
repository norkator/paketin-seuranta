package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;
import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class DHLExpressStrategy implements CourierStrategy, HostnameVerifier {

    // Logging
    private static final String TAG = "DHLExpressStrategy";


    // Host name verifier
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }


    @Override
    public ParcelObject execute(String parcelCode) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    @SuppressLint("TrustAllX509TrustManager")
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    @SuppressLint("TrustAllX509TrustManager")
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            String url = "https://www.dhl.fi/shipmentTracking?AWB=" + parcelCode + "&countryCode=fi&languageCode=fi";
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
            con.setConnectTimeout(5000);    // Timeout for connecting
            con.setReadTimeout(5000);       // Timeout for reading content
            con.setSSLSocketFactory(sc.getSocketFactory());
            con.setHostnameVerifier(this);
            con.setRequestMethod("GET");
            String USER_AGENT = "Mozilla/5.0";
            con.setRequestProperty("User-Agent", USER_AGENT);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            String jsonResult = response.toString();

            // Parsing got json content
            JSONObject jsonResponse = new JSONObject(jsonResult);                       // Json content
            JSONArray jsonMainNode = jsonResponse.optJSONArray("results");              // Get "results" array
            JSONObject jsonChildNode = jsonMainNode.getJSONObject(0);                   // Get first object from "results" array
            Log.i(TAG, jsonChildNode.toString()); // Print whole response


            if (jsonChildNode.length() > 0) {
                parcelObject.setIsFound(true); // Parcel is found

                // Parse all package related normal data found
                if (jsonChildNode.has("delivery")) {
                    parcelObject.setPhase(jsonChildNode.getJSONObject("delivery").getString("status").toUpperCase()); // Phase
                } else {
                    parcelObject.setPhase("TRANSIT"); // Set as transit phase since it's still coming
                }
                parcelObject.setDestinationCountry(jsonChildNode.getJSONObject("destination").getString("value")); // Destination
                parcelObject.setProduct(jsonChildNode.getString("label"));
                parcelObject.setRecipientSignature(jsonChildNode.getString("description"));


                // Try to parse estimate delivery time
                try {
                    if (jsonChildNode.has("edd")) {
                        @SuppressLint("SimpleDateFormat") DateFormat tf1 = new SimpleDateFormat("yyyy-MM-dd");
                        @SuppressLint("SimpleDateFormat") DateFormat tf2 = new SimpleDateFormat("dd.MM.yyyy");
                        final JSONObject deliveryTimeObject = jsonChildNode.getJSONObject("edd");
                        String date = deliveryTimeObject.optString("date");
                        String monthNumber = Utils.finnishMonthStringToMonthNumber(date);
                        String dayNumber = date.replace(" ", "")
                                .replace("tammikuu", "").replace("helmikuu", "").replace("maaliskuu", "").replace("huhtikuu", "")
                                .replace("toukokuu", "").replace("kesäkuu", "").replace("heinäkuu", "").replace("elokuu", "")
                                .replace("syyskuu", "").replace("lokakuu", "").replace("marraskuu", "").replace("joulukuu", "")
                                .split(",")[1];
                        String yearNumber = date.replace(" ", "").split(",")[2];
                        Date estimateDeliveryDate = tf1.parse(yearNumber + "-" + monthNumber + "-" + dayNumber);
                        //Log.i(TAG, yearNumber + "-" + monthNumber + "-" + dayNumber);
                        parcelObject.setEstimatedDeliveryTime(tf2.format(estimateDeliveryDate));
                    }
                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }


                // Parse events
                JSONArray checkPoints = jsonChildNode.getJSONArray("checkpoints");
                // Declare time formats here
                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Log.i(TAG, String.valueOf(checkPoints.length()));
                for (int i = 0; i < checkPoints.length(); i++) {
                    JSONObject checkPointObject = checkPoints.getJSONObject(i);
                    // Description
                    String description = checkPointObject.getString("description");
                    Log.i(TAG, description);
                    // Date
                    // example: "maanantai, heinäkuu 16, 2018 "
                    String date = checkPointObject.getString("date");
                    String monthNumber = Utils.finnishMonthStringToMonthNumber(date);
                    String dayNumber = date.replace(" ", "")
                            .replace("tammikuu", "").replace("helmikuu", "").replace("maaliskuu", "").replace("huhtikuu", "")
                            .replace("toukokuu", "").replace("kesäkuu", "").replace("heinäkuu", "").replace("elokuu", "")
                            .replace("syyskuu", "").replace("lokakuu", "").replace("marraskuu", "").replace("joulukuu", "")
                            .split(",")[1];
                    String yearNumber = date.replace(" ", "").split(",")[2];
                    String time = checkPointObject.getString("time");
                    Date apiDate = apiDateFormat.parse(yearNumber + "-" + monthNumber + "-" + dayNumber + " " + time);
                    String parsedShowingDate = showingDateFormat.format(apiDate);
                    String parsedDateSQLiteFormat = SQLiteDateFormat.format(apiDate);
                    String locationName = checkPointObject.getString("location");
                    EventObject eventObject = new EventObject(
                            description, parsedShowingDate, parsedDateSQLiteFormat, "", locationName
                    );
                    eventObjects.add(eventObject);
                }
                parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
            } else {
                Log.i(TAG, "DHL shipment not found");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.i(TAG, e.toString());
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


} // End of class
