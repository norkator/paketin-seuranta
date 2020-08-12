/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.paketinseuranta;

@SuppressWarnings("HardCodedStringLiteral")
public class PhaseNumber {

    // Status variable names
    final static String PHASE_DELIVERED_STRING = "DELIVERED";
    public final static String PHASE_WAITING_FOR_PICKUP = "WAIT4PICKUP";

    // Status variables integers
    public final static Integer PHASE_IN_TRANSPORT = 2;
    final static Integer PHASE_READY_FOR_PICKUP = 3;

    // Status variables strings
    final static String PHASE_IN_TRANSPORT_STR = "2";
    public final static String PHASE_READY_FOR_PICKUP_STR = "3";


    // Returns number equivalent for phase string
    public static PhaseNumberString phaseToNumber(final String phase, final String lastEventStr) {

        if (phase.equals("IN_TRANSPORT_NOT_IN_FINLAND")) {
            return new PhaseNumberString("1", "IN_TRANSPORT_NOT_IN_FINLAND");


        } else if (phase.equals("IN_TRANSPORT") || phase.equals("TRANSIT") || phase.equals("WAITING")
                || lastEventStr.equals("Lähetys on saapunut kohdemaahan.")
                || lastEventStr.equals("Lähetys on lajiteltu.")
        ) {
            return new PhaseNumberString("2", "IN_TRANSPORT");


        } else if (phase.equals("DELIVERED")
                || lastEventStr.contains("Luovutettu vastaanottajalle")
                || lastEventStr.contains("Lähetys on toimitettu")
                || lastEventStr.contains("successfully delivered")
                || lastEventStr.contains("Delivered")
                || lastEventStr.contains("Asiakas noutanut")
                || lastEventStr.contains("Lähetys on nyt noudettu. Kiitos")
        ) {
            return new PhaseNumberString("4", "DELIVERED");


        } else if ((phase.equals("READY_FOR_PICKUP")
                || lastEventStr.equals("Noudettavissa")
                || lastEventStr.contains("ilmoitus tekstiviestillä")
                || lastEventStr.contains("ilmoitus sähköpostitse")
                || lastEventStr.equals("Vastaanotettu noutopisteessä")
                || lastEventStr.contains("Odottaa vastaanottajan noutoa")
                || (lastEventStr.contains("UPS Access Point") && !lastEventStr.contains("toimipaikkaan odottaa"))
                || lastEventStr.contains("Lähetys on noudettavissa")
                || lastEventStr.contains("Lähetimme vastaanottajalle viestin lähetyksen saapumisesta")
        )) {
            return new PhaseNumberString("3", "READY_FOR_PICKUP");


        } else if (phase.equals("RETURNED") || phase.equals("RETURNED_TO_SENDER")) {
            return new PhaseNumberString("5", "RETURNED");


        } else if (phase.equals("CUSTOMS")
                || lastEventStr.contains("odottaa tullaustasi")
                || lastEventStr.contains("tullaus")
        ) {
            return new PhaseNumberString("6", "CUSTOMS");


        } else if (phase.equals(PHASE_WAITING_FOR_PICKUP)) {
            return new PhaseNumberString("9", PHASE_WAITING_FOR_PICKUP);
        }


        return new PhaseNumberString("0", "");
    }


} // End of class