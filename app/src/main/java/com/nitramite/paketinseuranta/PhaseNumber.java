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

    // Phase variable names
    public final static String PHASE_EMPTY = "";
    public final static String PHASE_IN_TRANSPORT_NOT_IN_FINLAND = "IN_TRANSPORT_NOT_IN_FINLAND";
    public final static String PHASE_IN_TRANSPORT = "IN_TRANSPORT";
    public final static String PHASE_TRANSIT = "TRANSIT";
    public final static String PHASE_WAITING = "WAITING";
    public final static String PHASE_DELIVERED = "DELIVERED";
    public final static String PHASE_WAITING_FOR_PICKUP = "WAIT4PICKUP";
    public final static String PHASE_READY_FOR_PICKUP = "READY_FOR_PICKUP";
    public final static String PHASE_RETURNED = "RETURNED";
    public final static String PHASE_RETURNED_TO_SENDER = "RETURNED_TO_SENDER";
    public final static String PHASE_CUSTOMS = "CUSTOMS";

    // Phase variables integers
    public final static Integer PHASE_INT_EMPTY = 0;
    public final static Integer PHASE_INT_IN_TRANSPORT_NOT_IN_FINLAND = 1;
    public final static Integer PHASE_INT_IN_TRANSPORT = 2;
    public final static Integer PHASE_INT_READY_FOR_PICKUP = 3;
    public final static Integer PHASE_INT_DELIVERED = 4;
    public final static Integer PHASE_INT_RETURNED = 5;
    public final static Integer PHASE_INT_CUSTOMS = 6;
    public final static Integer PHASE_INT_WAITING_FOR_PICKUP = 9;


    // Returns number equivalent for phase string
    public static PhaseNumberString phaseToNumber(final String phase, final String lastEventStr) {

        if (!phase.equals(PHASE_RETURNED_TO_SENDER) &&
                (phase.equals(PHASE_IN_TRANSPORT_NOT_IN_FINLAND)
                || lastEventStr.equals("Lähetys on matkalla kohdemaahan")
                || lastEventStr.equals("Lähetys on rekisteröity.")
                || lastEventStr.equals("Lähetys on lähtenyt varastolta")
                || lastEventStr.equals("Lähetys on saapunut varastolle")
                || lastEventStr.equals("Lähetys ei ole vielä saapunut Postille, odotathan"))
        ) {
            return new PhaseNumberString(intToString(PHASE_INT_IN_TRANSPORT_NOT_IN_FINLAND), PHASE_IN_TRANSPORT_NOT_IN_FINLAND);


        } else if (phase.equals(PHASE_DELIVERED)
                || lastEventStr.contains("Luovutettu vastaanottajalle")
                || lastEventStr.contains("Lähetys on toimitettu")
                || lastEventStr.contains("successfully delivered")
                || lastEventStr.contains("Delivered")
                || lastEventStr.contains("Asiakas noutanut")
                || lastEventStr.contains("Lähetys on nyt noudettu. Kiitos")
        ) {
            return new PhaseNumberString(intToString(PHASE_INT_DELIVERED), PHASE_DELIVERED);


        } else if ((phase.equals(PHASE_READY_FOR_PICKUP)
                || lastEventStr.equals("Noudettavissa")
                || lastEventStr.contains("ilmoitus tekstiviestillä")
                || lastEventStr.contains("ilmoitus sähköpostitse")
                || lastEventStr.equals("Vastaanotettu noutopisteessä")
                || lastEventStr.contains("Odottaa vastaanottajan noutoa")
                || (lastEventStr.contains("UPS Access Point") && !lastEventStr.contains("toimipaikkaan odottaa"))
                || lastEventStr.contains("Lähetys on noudettavissa")
                || lastEventStr.contains("Lähetimme vastaanottajalle viestin lähetyksen saapumisesta")
                || lastEventStr.contains("toimitettu noutopisteeseen")
        )) {
            return new PhaseNumberString(intToString(PHASE_INT_READY_FOR_PICKUP), PHASE_READY_FOR_PICKUP);


        } else if (phase.equals(PHASE_IN_TRANSPORT) || phase.equals(PHASE_TRANSIT) || phase.equals(PHASE_WAITING)
                || lastEventStr.equals("Lähetys on saapunut kohdemaahan.")
                || lastEventStr.equals("Lähetys on lajiteltu.")
        ) {
            return new PhaseNumberString(intToString(PHASE_INT_IN_TRANSPORT), PHASE_IN_TRANSPORT);


        } else if (phase.equals(PHASE_RETURNED) || phase.equals(PHASE_RETURNED_TO_SENDER)) {
            return new PhaseNumberString(intToString(PHASE_INT_RETURNED), PHASE_RETURNED);


        } else if (phase.equals(PHASE_CUSTOMS)
                || lastEventStr.contains("odottaa tullaustasi")
                || lastEventStr.contains("tullaus")
        ) {
            return new PhaseNumberString(intToString(PHASE_INT_CUSTOMS), PHASE_CUSTOMS);


        } else if (phase.equals(PHASE_WAITING_FOR_PICKUP)) {
            return new PhaseNumberString(intToString(PHASE_INT_WAITING_FOR_PICKUP), PHASE_WAITING_FOR_PICKUP);
        }


        return new PhaseNumberString(intToString(PHASE_INT_EMPTY), PHASE_EMPTY);
    }


    private static String intToString(int input) {
        return String.valueOf(input);
    }


} // End of class