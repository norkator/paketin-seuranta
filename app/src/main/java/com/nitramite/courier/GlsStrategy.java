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

public class GlsStrategy implements CourierStrategy, HostnameVerifier {

    // Logging
    private static final String TAG = "GlsStrategy";

    // Host name verifier
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }


    @Override
    public ParcelObject execute(final String parcelCode) {
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
            String url = "https://gls-group.eu/app/service/open/rest/EU/en/rstt001?match=" + parcelCode;
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
            // Log.i(TAG, jsonResult);

            // Parsing got json content
            JSONObject tuStatusObject = new JSONObject(jsonResult).optJSONArray("tuStatus").optJSONObject(0); // tuStatus has one object

            JSONArray eventsArray = tuStatusObject.optJSONArray("history"); // Get events
            JSONArray infoArray = tuStatusObject.optJSONArray("infos");
            JSONObject statusObject = tuStatusObject.optJSONObject("progressBar");


            if (tuStatusObject.length() > 0) {
                parcelObject.setIsFound(true); // Parcel is found

                // In transport
                if (statusObject.getString("statusInfo").equals("INTRANSIT")) {
                    parcelObject.setPhase("IN_TRANSPORT");
                }
                // TODO; Needs more statuses


                // Parse info's
                for (int f = 0; f < infoArray.length(); f++) {
                    JSONObject infoObj = new JSONObject(infoArray.get(f).toString());
                    if (infoObj.getString("type").equals("WEIGHT")) {
                        parcelObject.setWeight(infoObj.getString("value").replace("kg", ""));
                    } else if (infoObj.getString("type").equals("PRODUCT")) {
                        parcelObject.setProduct(infoObj.getString("value"));
                    }
                }


                // Parse events
                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < eventsArray.length(); i++) {
                    JSONObject event = eventsArray.getJSONObject(i);

                    /*
                    "address":{
                        "city":"Neuenstein",
                        "countryCode":"DE",
                        "countryName":"Germany"
                    },
                    "date":"2019-06-19",
                    "time":"18:34:58",
                    "evtDscr":"The parcel has reached the parcel center and was sorted manually."
                     */

                    String timeStamp = event.optString("date") + " " + event.optString("time");
                    Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));

                    final String parsedDate = showingDateFormat.format(parseTimeDate);
                    final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);
                    final String eventDescription = event.getString("evtDscr");

                    JSONObject locationObject = event.getJSONObject("address");
                    final String locationName = locationObject.getString("city") + ", " + locationObject.getString("countryName");


                    // Pass to object
                    EventObject eventObject = new EventObject(
                            eventDescription, parsedDate, parsedDateSQLiteFormat, "", locationName
                    );
                    // Add object
                    eventObjects.add(eventObject);

                }
                parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
            } else {
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