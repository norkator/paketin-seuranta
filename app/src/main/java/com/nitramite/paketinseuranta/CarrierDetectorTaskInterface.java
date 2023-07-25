package com.nitramite.paketinseuranta;

public interface CarrierDetectorTaskInterface {

    void onCarrierDetected(final Integer carrierId);

    void onCarrierDetectorEnded();

    void onProgressbarProgressUpdate(Integer progress);

}