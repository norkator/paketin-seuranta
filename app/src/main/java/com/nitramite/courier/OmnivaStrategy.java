package com.nitramite.courier;

import android.annotation.SuppressLint;
import android.util.Log;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.paketinseuranta.PhaseNumber;
import com.nitramite.utils.Locale;
import com.nitramite.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import javax.net.ssl.SSLException;

@SuppressWarnings("HardCodedStringLiteral")
public class OmnivaStrategy implements CourierStrategy {

    // Logging
    private static final String TAG = OmnivaStrategy.class.getSimpleName();

    @Override
    public ParcelObject execute(final String parcelCode, final Locale locale) {
        ParcelObject parcelObject = new ParcelObject(parcelCode);
        ArrayList<EventObject> eventObjects = new ArrayList<>();
        try {
            String url = "https://www.omniva.ee/api/search.php?search_barcode=" + parcelCode + "&lang=eng";

            Document document = Jsoup.connect(url)
                    .timeout(30 * 1000)
                    .get();
            Element table = document.select("body > table > tbody").first();


            @SuppressLint("SimpleDateFormat") DateFormat apiDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            @SuppressLint("SimpleDateFormat") DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            @SuppressLint("SimpleDateFormat") DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            int tableSize = table.select("tr").size();
            if (tableSize > 0) {
                parcelObject.setIsFound(true);
                parcelObject.setPhase(PhaseNumber.PHASE_IN_TRANSPORT);
            }
            for (int i = 0; i < tableSize; i++) {
                Element tr = table.select("tr").get(i);

                String status = tr.select("td").get(0).text();
                String timeStamp = tr.select("td").get(1).text();
                String description = tr.select("td").get(2).text();

                if (status.equals("Delivery")) {
                    parcelObject.setPhase(PhaseNumber.PHASE_DELIVERED);
                }

                Date parseTimeDate = apiDateFormat.parse(timeStamp);
                final String parsedDate = showingDateFormat.format(Objects.requireNonNull(parseTimeDate));
                final String parsedDateSQLiteFormat = SQLiteDateFormat.format(parseTimeDate);
                EventObject eventObject = new EventObject(
                        description, parsedDate, parsedDateSQLiteFormat, status, ""
                );
                eventObjects.add(eventObject);
            }
            parcelObject.setEventObjects(eventObjects);
        } catch (SSLException e) {
            parcelObject.setUpdateFailed(true);
            Log.e(TAG, e.toString());
            e.printStackTrace();
        } catch (IOException | NullPointerException | ParseException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        return parcelObject;
    }


} // End of class