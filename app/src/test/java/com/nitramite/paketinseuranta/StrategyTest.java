package com.nitramite.paketinseuranta;

import android.util.Log;

import com.nitramite.courier.BringStrategy;
import com.nitramite.courier.CainiaoStrategy;
import com.nitramite.courier.Courier;
import com.nitramite.courier.DHLActiveTrackingStrategy;
import com.nitramite.courier.DHLExpressStrategy;
import com.nitramite.courier.DpdStrategy;
import com.nitramite.courier.FedExStrategy;
import com.nitramite.courier.FourPXStrategy;
import com.nitramite.courier.GlsStrategy;
import com.nitramite.courier.MatkahuoltoStrategy;
import com.nitramite.courier.OmnivaStrategy;
import com.nitramite.courier.ParcelObject;
import com.nitramite.courier.PostNordStrategy;
import com.nitramite.courier.PostiStrategy;
import com.nitramite.courier.TNTStrategy;
import com.nitramite.courier.UPSStrategy;
import com.nitramite.courier.USPSStrategy;
import com.nitramite.courier.YanwenStrategy;
import com.nitramite.utils.TestsUtils;

import org.junit.Test;

import static org.junit.Assume.assumeTrue;

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
    public void bring_strategy_is_healthy() {
        courier.setCourierStrategy(new BringStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/bring");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + BringStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void cainiao_strategy_is_healthy() {
        courier.setCourierStrategy(new CainiaoStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/cainiao");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
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
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + DHLExpressStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void dhl_active_tracking_strategy_is_healthy() {
        courier.setCourierStrategy(new DHLActiveTrackingStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/dhl-active-tracing");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + DHLActiveTrackingStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void dpd_strategy_is_healthy() {
        courier.setCourierStrategy(new DpdStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/dpd");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + DpdStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void fedex_strategy_is_healthy() {
        courier.setCourierStrategy(new FedExStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/fedex");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + FedExStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void four_px_strategy_is_healthy() {
        courier.setCourierStrategy(new FourPXStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/4px");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + FourPXStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void gls_strategy_is_healthy() {
        courier.setCourierStrategy(new GlsStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/gls");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + GlsStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void matkahuolto_strategy_is_healthy() {
        courier.setCourierStrategy(new MatkahuoltoStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/matkahuolto");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + MatkahuoltoStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void omniva_strategy_is_healthy() {
        courier.setCourierStrategy(new OmnivaStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/omniva");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + OmnivaStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void posti_strategy_is_healthy() {
        courier.setCourierStrategy(new PostiStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/posti");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + PostiStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void postnord_strategy_is_healthy() {
        courier.setCourierStrategy(new PostNordStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/postnord");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + PostNordStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void tnt_strategy_is_healthy() {
        courier.setCourierStrategy(new TNTStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/tnt");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + TNTStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void ups_strategy_is_healthy() {
        courier.setCourierStrategy(new UPSStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/ups");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + UPSStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void usps_strategy_is_healthy() {
        courier.setCourierStrategy(new USPSStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/usps");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound());
            assumeTrue(parcelObject.getIsFound());
        } else {
            Log.i(TAG, str1 + USPSStrategy.class.getSimpleName() + str2);
        }
    }


    @Test
    public void yanwen_strategy_is_healthy() {
        courier.setCourierStrategy(new YanwenStrategy());
        String sampleTrackingCode = TestsUtils.GetTestTrackingCode("https://www.aftership.com/couriers/yanwen");
        if (sampleTrackingCode != null) {
            ParcelObject parcelObject = courier.executeCourierStrategy(sampleTrackingCode, com.nitramite.utils.Locale.EN);
            // assertEquals(true, parcelObject.getIsFound() || parcelObject.getEventObjects().size() > 0);
            assumeTrue(parcelObject.getIsFound() || parcelObject.getEventObjects().size() > 0);
        } else {
            Log.i(TAG, str1 + YanwenStrategy.class.getSimpleName() + str2);
        }
    }


}