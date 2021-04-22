package com.nitramite.paketinseuranta;

import android.util.Log;

import com.nitramite.courier.Courier;
import com.nitramite.courier.ParcelObject;
import com.nitramite.courier.PostiStrategy;
import com.nitramite.utils.TestsUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class PostiStrategyTest {

    //  Logging
    private static final String TAG = PostiStrategyTest.class.getSimpleName();

    private Courier courier = new Courier();


    @Test
    public void posti_strategy_is_healthy() {
        courier.setCourierStrategy(new PostiStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/posti");
        ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);

        // Log.i(TAG, parcelObject.)

        assertEquals(true, parcelObject.getIsFound());
    }

}