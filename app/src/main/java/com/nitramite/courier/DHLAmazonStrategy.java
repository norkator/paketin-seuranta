package com.nitramite.courier;

import javax.net.ssl.HostnameVerifier;
import android.annotation.SuppressLint;
import android.util.Log;
import com.nitramite.paketinseuranta.EventObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class DHLAmazonStrategy implements CourierStrategy, HostnameVerifier {

    // Logging
    private static final String TAG = "DHLAmazonStrategy";

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
            String url = "https://www.logistics.dhl/nolp?piece-code=" + parcelCode + "&language-code=en";
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
            con.setConnectTimeout(5000);    // Timeout for connecting
            con.setReadTimeout(5000);       // Timeout for reading content
            con.setSSLSocketFactory(sc.getSocketFactory());
            con.setHostnameVerifier(this);
            con.setRequestMethod("GET");
            String USER_AGENT = "Mozilla/5.0";
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Host", "www.logistics.dhl");
            con.setRequestProperty("Connection", "keep-alive");
            con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            // Get input steam
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine).append("\n");
            }
            // Close connection
            in.close();

            // Parse xml response
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(response.toString()));
            org.w3c.dom.Document document = documentBuilder.parse(is);

            org.w3c.dom.Element element = document.getDocumentElement();
            element.normalize();

            NodeList requestIdNodeList = element.getElementsByTagName("data"); // Get first "request-id="173048a0-d7a7-452e-9a7e-4dad6cdbc3a0"" element
            if (requestIdNodeList.getLength() > 0) {

                Node pieceStatusPublicListNode = requestIdNodeList.item(0);
                NodeList publicListNodeList = pieceStatusPublicListNode.getChildNodes(); // Get second "name="piece-status-public-list"" node list

                // code="0" => seems to mean, parcel is found
                // code="100" => seems to mean that parcel is NOT found

                if (publicListNodeList.getLength() > 0) {
                    //parcelObject.setIsFound(true); // Parcel is found
                    //parcelObject.setPhase("TRANSIT");
                    // Do not set found in  here, everything is available here so far

                    Node pieceStatusPublicNode = publicListNodeList.item(0);

                    // pieceStatusPublicNode has basic information data
                    org.w3c.dom.Element basicInfoElement = (org.w3c.dom.Element) pieceStatusPublicNode;
                    parcelObject.setDestinationCountry(basicInfoElement.getAttribute("dest-country"));
                    parcelObject.setSender(basicInfoElement.getAttribute("origin-country")); // Remove if created problem to put origin country to sender detail
                    parcelObject.setProduct(basicInfoElement.getAttribute("product-name"));
                    parcelObject.setWeight(basicInfoElement.getAttribute("shipment-weight"));

                    // Parse events
                    NodeList eventsNodeList = pieceStatusPublicNode.getChildNodes();

                    // Is parcel found
                    if (eventsNodeList.getLength() > 0) {
                        parcelObject.setIsFound(true); // Parcel is found
                        parcelObject.setPhase("TRANSIT");
                    }

                    @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                    @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    for (int i = 0; i < eventsNodeList.getLength(); i++) {
                        org.w3c.dom.Element eventElement = (org.w3c.dom.Element) eventsNodeList.item(i);
                        // Description
                        String description = eventElement.getAttribute("event-text"); // Changed to text from event-status
                        Log.i(TAG, description);
                        // Date
                        Date apiDate = apiDateFormat.parse(eventElement.getAttribute("event-timestamp"));
                        String parsedShowingDate = showingDateFormat.format(apiDate);
                        String parsedDateSQLiteFormat = SQLiteDateFormat.format(apiDate);
                        // Location
                        String locationName = eventElement.getAttribute("event-location");
                        if (!locationName.equals("")) {
                            locationName += ", ";
                        }
                        locationName += eventElement.getAttribute("event-country");
                        // Save parsed values
                        EventObject eventObject = new EventObject(
                                description, parsedShowingDate, parsedDateSQLiteFormat, "", locationName
                        );
                        eventObjects.add(eventObject);
                    }
                    parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
                }
            }
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


} // End of class