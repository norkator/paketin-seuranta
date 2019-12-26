package com.nitramite.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nitramite.utils.CarrierUtils;
import com.nitramite.paketinseuranta.ParcelItem;
import com.nitramite.paketinseuranta.R;

import java.util.ArrayList;

public class CustomParcelsAdapterV2 extends ArrayAdapter<ParcelItem> {

    // Variables
    private final Context context;
    private final ArrayList<ParcelItem> parcelItems;
    private final LayoutInflater inflater;
    private final Boolean showCourierIcon;

    // Constructor
    public CustomParcelsAdapterV2(Context context, ArrayList<ParcelItem> parcelItems, Boolean showCourierIcon) {
        super(context, R.layout.parcel_item, parcelItems);
        // TODO Auto-generated constructor stub

        this.context = context;
        this.parcelItems = parcelItems;
        this.showCourierIcon = showCourierIcon;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @SuppressLint("SetTextI18n")
    @NonNull
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        View rowView;
        if (view == null) {
            rowView = inflater.inflate(R.layout.parcel_item, null, true);
        } else {
            rowView = view;
        }


        // Find views
        // CardView parcelItemCard = rowView.findViewById(R.id.parcelItemCard);
        TextView firstLineBold = rowView.findViewById(R.id.firstLineBold);
        TextView secondLineNormal = rowView.findViewById(R.id.secondLineNormal);
        TextView thirdLineNormal = rowView.findViewById(R.id.thirdLineNormal);
        TextView fourthLineNormal = rowView.findViewById(R.id.fourthLineNormal);
        TextView fourthLineTitle = rowView.findViewById(R.id.fourthLineTitle);
        TextView fifthLineNormal = rowView.findViewById(R.id.fifthLineNormal);
        TextView fifthLineTitle = rowView.findViewById(R.id.fifthLineTitle);
        TextView sixthLineNormal = rowView.findViewById(R.id.sixthLineNormal);
        TextView sixthLineTitle = rowView.findViewById(R.id.sixthLineTitle);
        TextView seventhLineNormal = rowView.findViewById(R.id.seventhLineNormal);
        TextView parcelUpdateStatusTV = rowView.findViewById(R.id.parcelUpdateStatusTV);
        ImageView statusImageView = rowView.findViewById(R.id.statusImageView);
        ImageView courierIcon = rowView.findViewById(R.id.courierIcon);

        // Defaults
        fourthLineNormal.setVisibility(View.GONE);
        fourthLineTitle.setVisibility(View.GONE);
        fifthLineNormal.setVisibility(View.GONE);
        fifthLineTitle.setVisibility(View.GONE);
        sixthLineNormal.setVisibility(View.GONE);
        sixthLineTitle.setVisibility(View.GONE);
        seventhLineNormal.setVisibility(View.GONE);

        String phaseTextFix;
        Integer[] drawerImages = {
                R.drawable.needsupdate,
                R.drawable.notfound,
                R.drawable.intransportnotinfinland,
                R.drawable.intransport,
                R.drawable.readyforpickup,
                R.drawable.delivered,
                R.mipmap.muu_logo,
                R.drawable.returned,
                R.drawable.customs,
        };

        String phase = parcelItems.get(position).getParcelPhase();

        int pos = 1;
        final String latestEventDescription = (parcelItems.get(position).getParcelLatestEventDescription() != null ?
                parcelItems.get(position).getParcelLatestEventDescription() : ""); // Latest event
        phaseTextFix = context.getString(R.string.package_not_found);
        //if (phase.length() == null) {
        //    pos = 1;
        //    phaseTextFix = "Pakettia ei löytynyt!";
        //}

        // Check for item in transit
        if (phase.length() == 11 || phase.equals("TRANSIT")) {
            pos = 3;
            phaseTextFix = "Kuljetuksessa!";
        } else if (phase.length() == 12) {
            pos = 3;
            phaseTextFix = "Kuljetuksessa!";
        } else if (phase.length() == 16 || latestEventDescription.contains("ilmoitus tekstiviestillä") || latestEventDescription.contains("toimitettu noutopisteeseen")) {
            pos = 4;
            phaseTextFix = "Valmiina noudettavaksi!";
        } else if (phase.length() == 9) {
            pos = 5;
            phaseTextFix = "Toimitettu!";
        } else if (phase.length() == 8 || phase.equals("RETURNED_TO_SENDER")) {
            pos = 7;
            phaseTextFix = "Palautettu!";
        } else if (phase.length() == 7 || phase.equals("CUSTOMS")) {
            pos = 8;
            phaseTextFix = "Tullaus vaaditaan!";
        }
        // Not inside Finland
        else if (phase.length() == 24 || latestEventDescription.equals("Lähetys ei ole vielä saapunut Postille, odotathan") ||
                latestEventDescription.equals("Lähetys on saapunut varastolle") || latestEventDescription.equals("Lähetys on lähtenyt varastolta")
                || latestEventDescription.equals("Lähetys on rekisteröity.") || latestEventDescription.equals("Lähetys on matkalla kohdemaahan")
        ) {
            pos = 2;
            phaseTextFix = "Kuljetuksessa!";
        }
        else if (parcelItems.get(position).getParcelCarrier().equals("99") && phase.equals("")) {
            pos = 6;
            phaseTextFix = "";
        }

        // -----------------------------------------------------------------------------------------
        /* Set values */

        // First line
        if (parcelItems.get(position).getParcelTitle() != null && !parcelItems.get(position).getParcelTitle().equals("")) {
            firstLineBold.setText(parcelItems.get(position).getParcelTitle()); // Set given name instead of parcel code
        } else {
            firstLineBold.setText(parcelItems.get(position).getParcelCode()); // Set parcel code
        }


        // Second line normal
        if (parcelItems.get(position).getParcelTitle() != null && !parcelItems.get(position).getParcelTitle().equals("")) {
            secondLineNormal.setVisibility(View.VISIBLE);
            secondLineNormal.setText(parcelItems.get(position).getParcelCode());
        } else {
            secondLineNormal.setVisibility(View.GONE);
        }


        // Third line normal
        if (!parcelItems.get(position).getArchivedPackage()) {
            if (phaseTextFix.equals(context.getString(R.string.package_not_found))) {
                final String text = (!parcelItems.get(position).getParcelCode().equals("-") ? context.getString(R.string.custom_parcels_adapter_package_not_found_check_courier_company) : "");
                if (text.equals("")) {
                    thirdLineNormal.setVisibility(View.GONE);
                } else {
                    thirdLineNormal.setText(text);
                }
            } else {
                final String text = (latestEventDescription != null && latestEventDescription.length() > 0 ? latestEventDescription : phaseTextFix);
                if (text.equals("")) {
                    thirdLineNormal.setVisibility(View.GONE);
                } else {
                    thirdLineNormal.setText(text);
                }
            }
        } else {
            thirdLineNormal.setHeight(10);
        }


        /*
          Archive related lines
         */
        if (parcelItems.get(position).getArchivedPackage()) {
            // Sender
            if (parcelItems.get(position).getParcelSender() != null && !parcelItems.get(position).getParcelSender().equals("")) {
                fourthLineTitle.setVisibility(View.VISIBLE);
                fourthLineTitle.setText(context.getString(R.string.parcel_sender) + " ");
                fourthLineNormal.setVisibility(View.VISIBLE);
                fourthLineNormal.setText(parcelItems.get(position).getParcelSender());
            }
            // Delivery method
            if (parcelItems.get(position).getParcelDeliveryMethod() != null && !parcelItems.get(position).getParcelDeliveryMethod().equals("")) {
                fifthLineTitle.setVisibility(View.VISIBLE);
                fifthLineTitle.setText(context.getString(R.string.parcel_delivery_method) + " ");
                fifthLineNormal.setVisibility(View.VISIBLE);
                fifthLineNormal.setText(parcelItems.get(position).getParcelDeliveryMethod());
            }
            // Additional notes
            if (parcelItems.get(position).getParcelAdditionalNote() != null && !parcelItems.get(position).getParcelAdditionalNote().equals("")) {
                sixthLineTitle.setVisibility(View.VISIBLE);
                sixthLineTitle.setText(context.getString(R.string.parcel_additional_notes) + " ");
                sixthLineNormal.setVisibility(View.VISIBLE);
                sixthLineNormal.setText(parcelItems.get(position).getParcelAdditionalNote());
            }
        }


        // Seventh line
        if (parcelItems.get(position).getParcelLastPickupDate() != null) {
            if (!parcelItems.get(position).getParcelLastPickupDate().equals("") && !parcelItems.get(position).getParcelLastPickupDate().equals("null")) {
                seventhLineNormal.setVisibility(View.VISIBLE);
                seventhLineNormal.setText(context.getString(R.string.parcel_last_pickup_date) + " " + parcelItems.get(position).getParcelLastPickupDate());
            }
        }


        // Fourth line
        if (!parcelItems.get(position).getArchivedPackage()) {
            parcelUpdateStatusTV.setText(parcelItems.get(position).getParcelUpdateStatus());
        } else {
            if (parcelItems.get(position).getParcelCreateDate() != null && !parcelItems.get(position).getParcelCreateDate().equals("null")) {
                parcelUpdateStatusTV.setText(context.getString(R.string.parcel_parcel_added_time_stamp) + " " + parcelItems.get(position).getParcelCreateDate());
            } else {
                parcelUpdateStatusTV.setVisibility(View.GONE);
            }
        }


        // Set image resource
        statusImageView.setImageResource(drawerImages[pos]);

        // Carrier icon resource feature
        if (showCourierIcon) {
            courierIcon.setImageResource(CarrierUtils.getCarrierIconResourceForCarrierNumber(parcelItems.get(position).getParcelCarrierNumber()));
        } else {
            courierIcon.setVisibility(View.GONE);
        }


        return rowView;
    }


} // End of class
