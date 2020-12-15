package com.nitramite.courier;

import android.annotation.SuppressLint;

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
public class DHLActiveTrackingStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = "DHLActiveTracking";


    @Override
    public ParcelObject execute(String parcelCode, final Locale locale) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            String url = "https://www.dhl.com/DatPublic/search.do?autoSearch=true&l=fi&directDownload=XML&statusHistory=true&search=consignmentId&a=" + parcelCode;

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

            // Log.i(TAG, response.body().string());

            // Parse xml response
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(response.body().string()));
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
                    parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT);
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
        } catch (IOException | ParseException | SAXException | ParserConfigurationException | NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


} // End of class
