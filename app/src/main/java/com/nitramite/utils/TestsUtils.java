package com.nitramite.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

public class TestsUtils {

    //  Logging
    private static final String TAG = TestsUtils.class.getSimpleName();

    /**
     * Extract test tracking number from Aftership webpage used for running tests
     *
     * @param url of afterShip web page
     * @return tracking code extracted with css query
     */
    public static String GetTestTrackingCode(String url) {
        try {
            Document document = Jsoup.connect(url)
                    .timeout(30 * 1000)
                    .get();
            Element element = document.select("#gatsby-focus-wrapper > main > section:nth-child(1) > div > div.stack-item.Em51._1ucn.w-100 > section > div > p > a").first();
            return element.text();
        } catch (NullPointerException | IOException e) {
            return null;
        }
    }

}
