package com.nitramite.courier;

import com.nitramite.paketinseuranta.EventObject;
import com.nitramite.utils.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;

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



        } catch (SSLException e) {
            parcelObject.setUpdateFailed(true);
            e.printStackTrace();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return parcelObject;
    }


} // End of class