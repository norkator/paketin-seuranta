package com.nitramite.courier;

import android.annotation.SuppressLint;
import com.nitramite.paketinseuranta.EventObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("HardCodedStringLiteral")
public class USPSStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = "USPSStrategy";


    @Override
    public ParcelObject execute(String parcelCode, final com.nitramite.utils.Locale locale) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();

        try {
            Document document = Jsoup.connect(
                    "https://tools.usps.com/go/TrackConfirmAction_input?qtc_tLabels1=" + parcelCode
            )
                    .timeout(0)
                    .get();

            // Parse current status from html
            Elements deliveryStatusElements = document.getElementsByClass("delivery_status");
            for(Element dElement : deliveryStatusElements ) {
                if (dElement.select("strong").size() > 0) {
                    if (dElement.select("strong").text().equals("In-Transit")) {
                        parcelObject.setIsFound(true);
                        parcelObject.setPhase("IN_TRANSPORT");
                    }
                }
            }


            // Parse events
            @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("MMMM dd, yyyy, hh:mm a", Locale.US);
            @SuppressLint("SimpleDateFormat") DateFormat apiDateFormatNoTime = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
            @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Elements eventsTopElements = document.getElementsByClass("thPanalAction");
            String[] nodes = eventsTopElements.html().split("<hr>");
            for (String nodeStr : nodes) {

                // Parse time
                String timeContent = Jsoup.parse(nodeStr).select("strong").text();
                //Log.i(TAG, "TimeContent: " + timeContent);

                // Time can be:
                // February 18, 2019, 2:22 am
                // February 17, 2019
                Date parseTimeDate = null;
                try {
                    parseTimeDate = apiDateFormat.parse(timeContent);
                } catch (ParseException e) {
                    try {
                        parseTimeDate = apiDateFormatNoTime.parse(timeContent);
                    } catch (ParseException e1) {
                        e1.printStackTrace();
                    }
                }


                String finalTimeStamp = showingDateFormat.format(parseTimeDate);
                String sqliteTimeStamp = SQLiteDateFormat.format(parseTimeDate);


                // Parse description
                String description = Jsoup.parse(nodeStr.split("<br>")[1]).select("span").text();
                //Log.i(TAG, "Description: " + description);


                // Add event
                EventObject eventObject = new EventObject(
                        description, finalTimeStamp, sqliteTimeStamp, "", ""
                );
                eventObjects.add(eventObject);
            }
            parcelObject.setEventObjects(eventObjects);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }

} // End of class