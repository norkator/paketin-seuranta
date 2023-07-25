package com.nitramite.paketinseuranta;

/**
 * Class for phase number and string combination objects
 */
public class PhaseNumberString {

    private final String phaseNumber;
    private final String phaseString;

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