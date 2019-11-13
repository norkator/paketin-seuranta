package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;
import com.nitramite.paketinseuranta.EventObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

public class UPSStrategy implements CourierStrategy, HostnameVerifier {

    // Logging
    private static final String TAG = "UPSStrategy";


    // Host name verifier
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }


    @Override
    public ParcelObject execute(String parcelCode) {
        // Expected phase strings on site
        final String wordDelivered = "TOIMITETTU";
        final String wordInTransport = "Tilaus käsitelty: Valmis UPS:lle";
        final String wordInTransportTwo = "Lähtöskannaus";
        // Objects
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
            String url = "https://www.ups.com/track/api/Track/GetStatus?loc=fi_FI";
            URL obj = new URL(url);
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) obj.openConnection();
            httpsURLConnection.setConnectTimeout(5000);     // Timeout for connecting
            httpsURLConnection.setReadTimeout(5000);        // Timeout for reading content
            httpsURLConnection.setSSLSocketFactory(sc.getSocketFactory());
            httpsURLConnection.setHostnameVerifier(this);
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setRequestProperty("Content-Type", "application/json");
            httpsURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Mobile Safari/537.36");
            OutputStream outputStream = httpsURLConnection.getOutputStream();
            final String postBody = "{\"Locale\":\"fi_FI\",\"TrackingNumber\":[\"" + parcelCode + "\"]}";
            outputStream.write(postBody.getBytes());
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(httpsURLConnection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            String jsonResult = response.toString();
            Log.i(TAG, jsonResult);

            // Parsing got json content
            JSONObject jsonResponse = new JSONObject(jsonResult);                                   // Json content
            JSONObject parcelDetails = jsonResponse.optJSONArray("trackDetails").optJSONObject(0);  // Ge tracking details, this contains array of events
            JSONArray eventsArray = parcelDetails.optJSONArray("shipmentProgressActivities");       // Parcel events array
            JSONObject additionalInformation = parcelDetails.optJSONObject("additionalInformation"); // Additional information

            // Get service "product"
            parcelObject.setProduct(additionalInformation.optJSONObject("serviceInformation").optString("serviceName").replace("&#174;", "®"));
            parcelObject.setWeight(additionalInformation.optString("weight"));

            // Parse events
            if (eventsArray.length() > 0) {
                parcelObject.setIsFound(true); // Parcel is found
                // Fetch data
                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm"); // Api time is: 28.03.2018 11:48
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int a = 0; a < eventsArray.length(); a++) {
                    // Event obj
                    JSONObject eventJsonObject = eventsArray.optJSONObject(a);
                    String description = (eventJsonObject.optString("activityScan").replace("&#228;", "ä").replace("&#246;", "ö"));
                    final String dateStr = eventJsonObject.optString("date");
                    final String timeStr = eventJsonObject.optString("time");

                    //Log.i(TAG, eventJsonObject.toString());

                    // Data validity checking
                    if (!description.equals("") && !dateStr.equals("") && !timeStr.equals("")) {
                        // Event description
                        if (description.contains(wordDelivered)) {
                            parcelObject.setPhase("DELIVERED");
                        }
                        // Construct date and time string
                        Date parseTimeDate = apiDateFormat.parse(
                                dateStr + " " + timeStr
                        );
                        // Format date and time for different formats
                        String finalTimeStamp = showingDateFormat.format(parseTimeDate);
                        String sqliteTimeStamp = SQLiteDateFormat.format(parseTimeDate);
                        // Get location
                        String locationName = eventJsonObject.optString("location");
                        // Add to event object
                        EventObject eventObject = new EventObject(
                                description, finalTimeStamp, sqliteTimeStamp, "", locationName
                        );
                        eventObjects.add(eventObject);
                    }
                }
                // Set events
                parcelObject.setEventObjects(eventObjects);
                // Set phase as default if still null
                if (parcelObject.getPhase().equals("null")) {
                    parcelObject.setPhase("IN_TRANSPORT");
                }
            } else {
                Log.i(TAG, "UPS shipment not found");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


} // End of class
