package com.nitramite.paketinseuranta;

import android.util.Log;

import com.nitramite.courier.CainiaoStrategy;
import com.nitramite.courier.Courier;
import com.nitramite.courier.DHLExpressStrategy;
import com.nitramite.courier.FedExStrategy;
import com.nitramite.courier.MatkahuoltoStrategy;
import com.nitramite.courier.ParcelObject;
import com.nitramite.courier.PostiStrategy;
import com.nitramite.courier.UPSStrategy;
import com.nitramite.courier.YanwenStrategy;
import com.nitramite.utils.TestsUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@SuppressWarnings("FieldCanBeLocal")
public class StrategyTest {

    //  Logging
    private static final String TAG = StrategyTest.class.getSimpleName();

    private Courier courier = new Courier();
    private static String str1 = "!!!SKIPPED!!! ";
    private static String str2 = " has no tracking code available for testing!";


    @Test
    public void cainiao_strategy_is_healthy() {
        courier.setCourierStrategy(new CainiaoStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/cainiao");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            assertEquals(true, parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + CainiaoStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void dhl_express_strategy_is_healthy() {
        courier.setCourierStrategy(new DHLExpressStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/dhl");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            assertEquals(true, parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + DHLExpressStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void fedex_strategy_is_healthy() {
        courier.setCourierStrategy(new FedExStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/fedex");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            assertEquals(true, parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + FedExStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void matkahuolto_strategy_is_healthy() {
        courier.setCourierStrategy(new MatkahuoltoStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/matkahuolto");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            assertEquals(true, parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + MatkahuoltoStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void posti_strategy_is_healthy() {
        courier.setCourierStrategy(new PostiStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/posti");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            assertEquals(true, parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + PostiStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void ups_strategy_is_healthy() {
        courier.setCourierStrategy(new UPSStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/ups");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            assertEquals(true, parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + UPSStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void yanwen_strategy_is_healthy() {
        courier.setCourierStrategy(new YanwenStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/yanwen");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            assertEquals(true, parcelObject.getIsFound() || parcelObject.getEventObjects().size() > 0);
        } else {
            Log.i(TAG, str1 + YanwenStrategy.class.getSimpleName() + str2);
        }
    }


}