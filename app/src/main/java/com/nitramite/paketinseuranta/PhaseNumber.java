package com.nitramite.paketinseuranta;

public class PhaseNumber {

    // Status variable names
    final static String PHASE_DELIVERED_STRING = "DELIVERED";

    // Status variables integers
    public final static Integer PHASE_IN_TRANSPORT = 2;
    final static Integer PHASE_READY_FOR_PICKUP = 3;

    // Status variables strings
    final static String PHASE_IN_TRANSPORT_STR = "2";
    public final static String PHASE_READY_FOR_PICKUP_STR = "3";


    // Returns number equivalent for phase string
    public static PhaseNumberString phaseToNumber(final String phaseString) {

        if (phaseString.equals("INTRANSPORT_NOTINFINLAND")) {
            return new PhaseNumberString("1", "INTRANSPORT_NOTINFINLAND");

        } else if (phaseString.equals("IN_TRANSPORT") || phaseString.equals("Lähetys on saapunut kohdemaahan.") || phaseString.equals("Lähetys on lajiteltu.")
                || phaseString.equals("TRANSIT") || phaseString.equals("WAITING")) {
            return new PhaseNumberString("2", "IN_TRANSPORT");

        } else if (phaseString.equals("READY_FOR_PICKUP") || phaseString.equals("Noudettavissa") || phaseString.contains("ilmoitus tekstiviestillä")
                || phaseString.contains("ilmoitus sähköpostitse") || phaseString.equals("Vastaanotettu noutopisteessä")
                || phaseString.contains("Odottaa vastaanottajan noutoa") || ( phaseString.contains("UPS Access Point") && !phaseString.contains("toimipaikkaan odottaa"))
                || phaseString.contains("Lähetys on noudettavissa") || phaseString.contains("Lähetimme vastaanottajalle viestin lähetyksen saapumisesta")
        ) {
            return new PhaseNumberString("3", "READY_FOR_PICKUP");

        } else if ( phaseString.equals("DELIVERED") || phaseString.contains("Luovutettu vastaanottajalle") || phaseString.contains("Lähetys on toimitettu")
                || phaseString.contains("successfully delivered") ||phaseString.contains("Delivered") || phaseString.contains("Asiakas noutanut")
                || phaseString.contains("Lähetys on nyt noudettu. Kiitos")
        ) {
            return new PhaseNumberString("4", "DELIVERED");

        } else if ( phaseString.equals("RETURNED") || phaseString.equals("RETURNED_TO_SENDER") ) {
            return new PhaseNumberString("5", "RETURNED");

        } else if ( phaseString.equals("CUSTOMS") || phaseString.contains("odottaa tullaustasi") || phaseString.contains("tullaus") ) {
            return new PhaseNumberString("6", "CUSTOMS");
        }


        return new PhaseNumberString("0", "");
    }


} // End of class