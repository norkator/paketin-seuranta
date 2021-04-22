package com.nitramite.paketinseuranta;

import com.nitramite.courier.Courier;
import com.nitramite.courier.DHLExpressStrategy;
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
public class StrategyTest {

    //  Logging
    private static final String TAG = StrategyTest.class.getSimpleName();

    private Courier courier = new Courier();


    @Test
    public void posti_strategy_is_healthy() {
        courier.setCourierStrategy(new PostiStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/posti");
        ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);

        assertEquals(true, parcelObject.getIsFound());
    }


    @Test
    public void dhl_express_strategy_is_healthy() {
        courier.setCourierStrategy(new DHLExpressStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/dhl");
        ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);

        assertEquals(true, parcelObject.getIsFound());
    }

}