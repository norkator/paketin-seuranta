package com.nitramite.courier;

import javax.net.ssl.HostnameVerifier;
import android.annotation.SuppressLint;
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

public class DHLActiveTrackingStrategy implements CourierStrategy, HostnameVerifier {

    // Logging
    private static final String TAG = "DHLActiveTrackingStrategy";

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
            String url = "https://www.logistics.dhl/DatPublic/search.do?autoSearch=true&l=fi&directDownload=XML&statusHistory=true&search=consignmentId&a=" + parcelCode;
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
            con.setRequestProperty("Cache-Control", "no-cache");
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

            //Log.i(TAG, element.getTextContent());

            NodeList consignmentsNodeList = element.getElementsByTagName("consignment"); // Get parcel consignment tag
            if (consignmentsNodeList.getLength() > 0) {

                Node consignmentChildNode = consignmentsNodeList.item(0);
                org.w3c.dom.Element consignmentElement = (org.w3c.dom.Element) consignmentChildNode;

                // Get data for parcel
                parcelObject.setIsFound(true); // Parcel is found
                parcelObject.setSender(consignmentElement.getElementsByTagName("consignorCity").item(0).getTextContent());
                parcelObject.setDestinationCountry(consignmentElement.getElementsByTagName("consigneeCountry").item(0).getTextContent());
                parcelObject.setDestinationCity(consignmentElement.getElementsByTagName("consigneeCity").item(0).getTextContent());
                parcelObject.setPhase(consignmentElement.getElementsByTagName("statusCode").item(0).getTextContent());
                parcelObject.setWeight(consignmentElement.getElementsByTagName("weight").item(0).getTextContent());

                // -----------------------------

                // Get events nodes
                NodeList eventsNodeList = consignmentElement.getElementsByTagName("statusHistory");

                // Is in transit
                if (eventsNodeList.getLength() > 0) {
                    parcelObject.setPhase("TRANSIT");
                }

                @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < eventsNodeList.getLength(); i++) {
                    org.w3c.dom.Element eventElement = (org.w3c.dom.Element) eventsNodeList.item(i);

                    // Description
                    String description = eventElement.getElementsByTagName("statusText").item(0).getTextContent(); //Log.i(TAG, description);

                    // Date
                    Date apiDate = apiDateFormat.parse(
                            eventElement.getElementsByTagName("statusDate").item(0).getTextContent() + " " +
                                    eventElement.getElementsByTagName("statusTime").item(0).getTextContent()
                    );
                    String parsedShowingDate = showingDateFormat.format(apiDate);
                    String parsedDateSQLiteFormat = SQLiteDateFormat.format(apiDate);

                    // Location
                    final String locationName = eventElement.getElementsByTagName("statusTerminal").item(0).getTextContent();

                    // Save parsed values
                    EventObject eventObject = new EventObject(
                            description, parsedShowingDate, parsedDateSQLiteFormat, "", locationName
                    );
                    eventObjects.add(eventObject);
                }
                parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
                // -----------------------------
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
