package com.nitramite.paketinseuranta;

/**
 * Class for phase number and string combination objects
 */
public class PhaseNumberString {


    // Variables
    private String phaseNumber;
    private String phaseString;


    // Constructor
    PhaseNumberString(final String phaseNumber, final String phaseString) {
        this.phaseNumber = phaseNumber;
        this.phaseString = phaseString;
    }

    public String getPhaseNumber() {
        return phaseNumber;
    }

    public String getPhaseString() {
        return phaseString;
    }

}