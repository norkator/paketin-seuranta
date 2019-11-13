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
import java.net.SocketTimeoutException;
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
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MatkahuoltoStrategy implements CourierStrategy, HostnameVerifier {

    // Logging
    private static final String TAG = "MatkahuoltoStrategy";

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
            String url = "https://www.api.matkahuolto.io/search/trackingInfo?language=fi&parcelNumber=" + parcelCode;
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

            JSONObject jsonResponse = new JSONObject(jsonResult);
            JSONArray trackingEvents = jsonResponse.optJSONArray("trackingEvents");


            if (trackingEvents.length() > 0) {
                parcelObject.setIsFound(true); // Parcel is found
                parcelObject.setPhase("IN_TRANSPORT");

                // Get product type
                parcelObject.setProduct(jsonResponse.optString("productCategory"));

                // Parse events
                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < trackingEvents.length(); i++) {
                    JSONObject event = trackingEvents.getJSONObject(i);

                    String timeStamp = event.optString("date") + " " + event.optString("time");
                    Date parseTimeDate = Utils.postiOffsetDateHours(apiDateFormat.parse(timeStamp));

                    final String parsedDate = showingDateFormat.format(parseTimeDate);
                    final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);

                    // Log.i(TAG, "After parsing date format is: " + parsedDate);
                    // Log.i(TAG, "After parsing SQLite date format is: " + parsedDateSQLiteFormat);

                    String eventDescription = event.optString("description");

                    String place = "";
                    if (!event.optString("place").equals("null")) {
                        place = event.optString("place");
                    }

                    // Pass to object
                    EventObject eventObject = new EventObject(
                            eventDescription, parsedDate, parsedDateSQLiteFormat, "", place
                    );
                    // Add object
                    eventObjects.add(eventObject);

                }
                parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
            } else {
                Log.i(TAG, "Matkahuolto shipment not found");
                parcelObject.setIsFound(false); // Parcel not found
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.i(TAG, e.toString());
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
        } catch (SSLException e) {
            Log.i(TAG, "RESET BY PEER FOR " + parcelCode);
            parcelObject.setUpdateFailed(true);
            e.printStackTrace();
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