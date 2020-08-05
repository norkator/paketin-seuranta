/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.paketinseuranta;

import android.os.AsyncTask;

import com.nitramite.courier.ArraPakettiStrategy;
import com.nitramite.courier.CainiaoStrategy;
import com.nitramite.courier.Courier;
import com.nitramite.courier.CourierStrategy;
import com.nitramite.courier.DHLActiveTrackingStrategy;
import com.nitramite.courier.DHLAmazonStrategy;
import com.nitramite.courier.DHLExpressStrategy;
import com.nitramite.courier.FedExStrategy;
import com.nitramite.courier.FourPXStrategy;
import com.nitramite.courier.MatkahuoltoStrategy;
import com.nitramite.courier.ParcelObject;
import com.nitramite.courier.PostNordStrategy;
import com.nitramite.courier.PostiStrategy;
import com.nitramite.courier.UPSStrategy;
import com.nitramite.courier.YanwenStrategy;
import com.nitramite.utils.CarrierUtils;

import java.util.ArrayList;


@SuppressWarnings("SpellCheckingInspection")
public class CarrierDetectorTask extends AsyncTask<String, String, String> {

    // Variables
    private static final String TAG = "CarrierDetectorTask";
    private CarrierDetectorTaskInterface listener;
    private String parcelCode;

    // Constructor
    CarrierDetectorTask(CarrierDetectorTaskInterface listener, final String parcelCode) {
        this.listener = listener;
        this.parcelCode = parcelCode;
    }

    @Override
    protected String doInBackground(String... params) {
        // Init variables
        Courier courier = new Courier();
        ParcelObject parcelObject = null;

        ArrayList<CourierStrategy> courierStrategies = new ArrayList<>();
        ArrayList<Integer> courierIntegers = new ArrayList<>();

        courierStrategies.add(new PostiStrategy());
        courierIntegers.add(CarrierUtils.CARRIER_POSTI);

        courierStrategies.add(new MatkahuoltoStrategy());
        courierIntegers.add(CarrierUtils.CARRIER_MATKAHUOLTO);

        courierStrategies.add(new DHLExpressStrategy());
        courierIntegers.add(CarrierUtils.CARRIER_DHL_EXPRESS);

        courierStrategies.add(new DHLExpressStrategy());
        courierIntegers.add(CarrierUtils.CARRIER_DHL_EXPRESS);

        courierStrategies.add(new DHLAmazonStrategy());
        courierIntegers.add(CarrierUtils.CARRIER_DHL_AMAZON);

        courierStrategies.add(new DHLActiveTrackingStrategy());
        courierIntegers.add(CarrierUtils.CARRIER_DHL_ACTIVE_TRACKING);

        courierStrategies.add(new UPSStrategy());
        courierIntegers.add(CarrierUtils.CARRIER_UPS);

        courierStrategies.add(new FedExStrategy());
        courierIntegers.add(CarrierUtils.CARRIER_FEDEX);

        courierStrategies.add(new PostNordStrategy());
        courierIntegers.add(CarrierUtils.CARRIER_POSTNORD);

        courierStrategies.add(new ArraPakettiStrategy());
        courierIntegers.add(CarrierUtils.CARRIER_ARRA_PAKETTI);

        courierStrategies.add(new YanwenStrategy());
        courierIntegers.add(CarrierUtils.CARRIER_YANWEN);

        courierStrategies.add(new CainiaoStrategy());
        courierIntegers.add(CarrierUtils.CARRIER_CAINIAO);

        courierStrategies.add(new FourPXStrategy());
        courierIntegers.add(CarrierUtils.CARRIER_4PX);

        int increment = 100 / courierStrategies.size();
        int progress = increment;
        for (int i = 0; i < courierStrategies.size(); i++) {
            if (isCancelled()) {
                break;
            }
            listener.onProgressbarProgressUpdate(progress);
            progress = progress + increment;
            try {
                courier.setCourierStrategy(courierStrategies.get(i));
                parcelObject = courier.executeCourierStrategy((this.parcelCode));
                if (parcelObject.getIsFound()) {
                    listener.onCarrierDetected(courierIntegers.get(i));
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        listener.onCarrierDetectorEnded();
    }


} // End of asyncTask
