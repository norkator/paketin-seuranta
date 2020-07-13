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

public class YanwenStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = "YanwenStrategy";

    // Api url
    private static final String url = "http://track.yw56.com.cn/en-US/";


    @Override
    public ParcelObject execute(String parcelCode) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();

        try {
            Document document = Jsoup.connect(url)
                    .data("InputTrackNumbers", parcelCode)
                    .timeout(0)
                    .post();

            // Log.i(TAG, document.toString());


            // Parse current status from html
            Elements deliveryStatusElements = document.getElementsByClass("media-middle");
            for (Element dElement : deliveryStatusElements) {
                // Log.i(TAG, dElement.toString());
                if (dElement.select("i.btn-success").size() > 0) {
                    // Log.i(TAG, dElement.select("i.btn-success").html());
                    if (dElement.select("i.btn-success").text().contains("In transport")) {
                        parcelObject.setIsFound(true);
                        parcelObject.setPhase("IN_TRANSPORT");
                    } else if (dElement.select("i.btn-success").text().contains("Track End")) {
                        parcelObject.setIsFound(true);
                        parcelObject.setPhase("IN_TRANSPORT");
                    } else if (dElement.select("i.btn-success").text().contains("Delivered")) {
                        parcelObject.setIsFound(true);
                        parcelObject.setPhase("DELIVERED");
                    }
                }
            }


            // Parse events
            @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            @SuppressLint("SimpleDateFormat") DateFormat apiDateFormatNoTime = new SimpleDateFormat("yyyy-MM-dd");
            @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


            Elements tableElements = document.getElementsByClass("table-hover");
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
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


} // End of class
