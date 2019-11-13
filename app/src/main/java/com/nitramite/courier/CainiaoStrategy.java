package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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

public class CainiaoStrategy implements CourierStrategy, HostnameVerifier {

    // Logging
    private static final String TAG = "CainiaoStrategy";


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
            String url = "https://slw16.global.cainiao.com/trackRefreshRpc/refresh.json?&mailNo=" + parcelCode;
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

            // Log.i(TAG, document.toString());
            final String jsonResult = response.toString().replace("(", "").replace(")", "");
            JSONObject detailsObject = new JSONObject(jsonResult);

            // Log.i(TAG, detailsObject.optString("destCountry"));

            parcelObject.setDestinationCountry(detailsObject.optString("destCountry"));

            // Parse events
            @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            @SuppressLint("SimpleDateFormat") DateFormat apiDateFormatNoTime = new SimpleDateFormat("yyyy-MM-dd");
            @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


            JSONObject event = detailsObject.optJSONObject("latestTrackingInfo");

            String timeStamp = event.optString("time");
            Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));

            final String parsedDate = showingDateFormat.format(parseTimeDate);
            final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);
            final String eventDescription = event.getString("desc");

            // Pass to object
            EventObject eventObject = new EventObject(
                    eventDescription, parsedDate, parsedDateSQLiteFormat, "", ""
            );

            eventObjects.add(eventObject);

            // Parse this as last
            if (!detailsObject.isNull("destCountry")) {
                parcelObject.setIsFound(true);
                parcelObject.setPhase("IN_TRANSPORT");
            }

            // Add to stack
            parcelObject.setEventObjects(eventObjects);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


} // End of class
