package com.nitramite.courier;

import android.util.Log;
import com.nitramite.paketinseuranta.EventObject;

import org.jetbrains.annotations.NonNls;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;
import java.util.ArrayList;

public class ChinaPostAirMailStrategy implements CourierStrategy {

    // Logging
    @NonNls
    private static final String TAG = "CPRAM";


    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public ParcelObject execute(String parcelCode) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            Document document = Jsoup.connect("http://track-chinapost.com/result_china.php?order_no=" + parcelCode + "&submit_order=Click+to+Track")
                    .timeout(30 * 1000)
                    .get();

            Element table4 = document.select("table").get(3);


            for (int i = 0; i < table4.select("tr").size(); i++) {
                Element tr = table4.select("tr").get(i);
                Log.i(TAG, tr.toString());

                if (!tr.toString().contains("<th>")) {


                    /*
                    String[] split = tr.toString().split("<td>");
                    final String localDateTime = split[1].replace("<td>", "").replace("</td>", "");
                    final String activity = split[2].replace("<td>", "").replace("</td>", "");
                    final String location = split[3].replace("<td>", "").replace("</td>", "");
                    final String remarks = split[4].replace("<td>", "").replace("</td>", "");
                    Log.i(TAG,
                            "Datetime: " + localDateTime + "  " +
                                    "Activity: " + activity + "  " +
                                    "Location: " + location + "  " +
                                    "Remarks: " + remarks + "  "
                    );
                    */

                }
            }

            /*

            // Events
            JSONObject jsonObject = new JSONObject(jsonResult);
            JSONArray eventsArray = jsonObject.optJSONArray("data"); // Get item events

            // Parcel phase handling
            if (eventsArray.length() > 0) {
                parcelObject.setIsFound(true); // Parcel is found
                parcelObject.setPhase("TRANSIT"); // Phase
            }

            @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < eventsArray.length(); i++) {
                JSONObject event = eventsArray.getJSONObject(i);

                String timeStamp = event.optString("time");
                Date parseTimeDate = apiDateFormat.parse(timeStamp);

                final String parsedDate = showingDateFormat.format(parseTimeDate);
                final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);

                // Get event description
                String eventDescription = event.optString("context");

                // Pass to object
                EventObject eventObject = new EventObject(
                        eventDescription,
                        parsedDate,
                        parsedDateSQLiteFormat,
                        "",
                        ""
                );

                // Add object
                eventObjects.add(eventObject);
            }
            parcelObject.setEventObjects(eventObjects); // Set event object into parcel object for later fetching
            */
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }

} // End of class