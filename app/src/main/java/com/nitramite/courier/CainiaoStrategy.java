/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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

    private String getRequest(String url) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        SSLContext sc = SSLContext.getInstance("SSL");
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
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
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
        return response.toString();
    }

    @Override
    public ParcelObject execute(String parcelCode) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);

        try {
            String url = "https://global.cainiao.com/detail.htm?mailNoList=" + parcelCode;

            final String htmlResult = getRequest(url);
            Document trackingHTML = Jsoup.parse(htmlResult);
            Element jsonElement = trackingHTML.getElementById("waybill_list_val_box");
            if (jsonElement != null) {
                String jsonResult = jsonElement.text();
                JSONObject responseObject = new JSONObject(jsonResult);
                if (responseObject.optBoolean("success")) {
                    JSONArray dataItems = responseObject.getJSONArray("data");

                    if (dataItems.length() > 0) {
                        JSONObject detailsObject = dataItems.getJSONObject(0);

                        if (detailsObject.optBoolean("success")) {
                            proceedParsing(parcelObject, detailsObject);
                        } else {
                            JSONArray originCpList = detailsObject.optJSONArray("originCpList");
                            if (originCpList != null && originCpList.length() > 0) {
                                Log.i(TAG, "Fetching via fallback");
                                JSONObject origin = originCpList.optJSONObject(0);
                                if (origin != null) {
                                    String oCode = origin.optString("cpCode", "");
                                    String fallbackUrl = "https://slw16.global.cainiao.com/trackSyncQueryRpc/queryAllLinkTrace.json?callback=s&mailNo=" + parcelCode + "&originCp=" + oCode;
                                    String responseNotJson = getRequest(fallbackUrl).replace("s(", "");
                                    responseNotJson = responseNotJson.substring(0, responseNotJson.length() - 1);
                                    JSONObject responseJson = new JSONObject(responseNotJson);
                                    if (responseJson.optBoolean("success")) {
                                        proceedParsing(parcelObject, responseJson);
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                Log.i(TAG, "Cainiao Packet fetching failed to find the JSON");
                parcelObject.setIsFound(false); // Parcel not found
            }

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

    private void proceedParsing(ParcelObject parcelObject, JSONObject detailsObject) throws JSONException, ParseException {
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        parcelObject.setIsFound(true);
        parcelObject.setPhase("IN_TRANSPORT");
        parcelObject.setDestinationCountry(detailsObject.optString("destCountry"));

        // Parse events
        JSONObject section2 = detailsObject.optJSONObject("section2");

        if (section2 != null) {
            JSONArray details = section2.optJSONArray("detailList");
            if (details != null && details.length() > 0) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject event = details.optJSONObject(i);
                    eventObjects.add(parseEventObject(event));
                }
            }
        }

        JSONObject event = detailsObject.optJSONObject("latestTrackingInfo");
        if (event != null && eventObjects.size() < 1) {
            eventObjects.add(parseEventObject(event));
        }
        setBasicDetails(parcelObject, detailsObject);
        // Add to stack
        parcelObject.setEventObjects(eventObjects);
    }

    private EventObject parseEventObject(JSONObject event) throws JSONException, ParseException {
        @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        @SuppressLint("SimpleDateFormat") DateFormat apiDateFormatNoTime = new SimpleDateFormat("yyyy-MM-dd");
        @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStamp = event.optString("time");
        Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));

        final String parsedDate = showingDateFormat.format(parseTimeDate);
        final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);
        final String eventDescription = event.getString("desc");

        // Pass to object

        return new EventObject(
                eventDescription, parsedDate, parsedDateSQLiteFormat, "null", "null"
        );
    }


    private void setBasicDetails(ParcelObject parcelObject, JSONObject jsonObject) {
        String dest = jsonObject.optString("destCountry", "null");
        String status = jsonObject.optString("status", "");
        parcelObject.setDestinationCountry(dest);

        if (status.equals("LTL_SIGNIN") || status.equals("SIGNIN") || status.equals("OWS_SIGNIN") || status.contains("WAIT4SIGNIN")) {
            parcelObject.setPhase("DELIVERED");
        } else if (status.equals("CWS_WAIT4SIGNIN") || status.equals("LTL_WAIT4SIGNIN") || status.equals("WAIT4SIGNIN") || status.contains("WAIT4PICKUP")) {
            parcelObject.setPhase("READY_FOR_PICKUP");
        } else if (status.contains("RETURN")) {
            parcelObject.setPhase("RETURNED");
        }
    }


} // End of class
