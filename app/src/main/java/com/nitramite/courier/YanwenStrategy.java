package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Locale;
import com.nitramite.paketinseuranta.PhaseNumber;

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

@SuppressWarnings("HardCodedStringLiteral")
public class YanwenStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = "YanwenStrategy";

    // Api url
    private static final String url = "https://track.yw56.com.cn/home/index?aspxerrorpath=/en-US0&InputTrackNumbers=";


    @Override
    public ParcelObject execute(String parcelCode, final Locale locale) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        try {
            // Get website with tracking info
            Document webPage = getSiteData(parcelCode);

            // Parse status
            parseStatus(webPage, parcelObject);

            // Parse events
            parseEvents(webPage, parcelObject);

        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


    /**
     * Load Yanwen website
     *
     * @param parcelCode parcel code
     * @return html document
     * @throws IOException throw exception if loading fails
     */
    private Document getSiteData(String parcelCode) throws IOException {
        return Jsoup.connect(url + parcelCode)
                .header("Content-Type", "text/html")
                .timeout(0)
                .post();
    }


    /**
     * Parses last status of parcel
     *
     * @param webPage      html content
     * @param parcelObject parcel object
     */
    private void parseStatus(Document webPage, ParcelObject parcelObject) {
        // Parse current status from html
        Elements elements = webPage.select("#accordion > div > div.panel-heading > div:nth-child(1) > div.col-md-9 > div > a");
        if (elements.size() > 0) {
            String lastEventHtml = elements.first().html();
            Log.i(TAG, lastEventHtml);
            if (lastEventHtml.contains("In transport") || lastEventHtml.contains("has arrived in the country of destination")) {
                parcelObject.setIsFound(true);
                parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT);
            } else if (lastEventHtml.contains("Track End")) {
                parcelObject.setIsFound(true);
                parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT);
            } else if (lastEventHtml.contains("Delivered") || lastEventHtml.contains("delivered to the recipient")) {
                parcelObject.setIsFound(true);
                parcelObject.setPhase(PhaseNumber.PHASE_DELIVERED);
            }
        }
    }


    /**
     * Parse events
     *
     * @param webPage      html content
     * @param parcelObject parcel object
     */
    private void parseEvents(Document webPage, ParcelObject parcelObject) {
        ArrayList<EventObject> eventObjects = new ArrayList<>();

        @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        @SuppressLint("SimpleDateFormat") DateFormat apiDateFormatNoTime = new SimpleDateFormat("yyyy-MM-dd");
        @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Elements tableElements = webPage.getElementsByClass("table table-hover");
        for (Element tableElement : tableElements) {
            Elements trElement = tableElement.select("tr");
            for (Element tdElement : trElement) {
                Elements tdElements = tdElement.select("td");
                if (tdElements.size() == 2) {

                    // Parse time
                    String timeContent = tdElements.get(0).text();
                    // Log.i(TAG, timeContent);

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
                    String description = tdElements.get(1).text();
                    // Log.i(TAG, description);

                    // Add event
                    EventObject eventObject = new EventObject(
                            description, finalTimeStamp, sqliteTimeStamp, "", ""
                    );
                    eventObjects.add(eventObject);
                }
            }
        }

        // Add to stack
        parcelObject.setEventObjects(eventObjects);
    }


} // End of class
