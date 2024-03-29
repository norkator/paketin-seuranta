package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Locale;
import com.nitramite.paketinseuranta.PhaseNumber;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("HardCodedStringLiteral")
public class DHLAmazonStrategy implements CourierStrategy {

    
    private static final String TAG = DHLAmazonStrategy.class.getSimpleName();

    @Override
    public ParcelObject execute(String parcelCode, final Locale locale) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            String url = "https://www.dhl.com/nolp?piece-code=" + parcelCode + "&language-code=en";

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", Constants.UserAgent)
                    .addHeader("Host", "www.dhl.com")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Cache-Control", "public")
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                    .build();
            Response response = client.newCall(request).execute();

            Log.i(TAG, response.body().string());

            // Parse xml response
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(response.body().string()));
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
                        parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT);
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
        } catch (IOException | NullPointerException | ParserConfigurationException | SAXException | ParseException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


} 