/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.nitramite.paketinseuranta.ParcelItem;
import com.nitramite.paketinseuranta.PhaseNumber;
import com.nitramite.paketinseuranta.R;
import com.nitramite.utils.CarrierUtils;

import java.util.ArrayList;

@SuppressWarnings("HardCodedStringLiteral")
public class CustomParcelsAdapterV2 extends ArrayAdapter<ParcelItem> {

    // Variables
    private final Context context;
    private final ArrayList<ParcelItem> parcelItems;
    private final LayoutInflater inflater;
    private final Boolean showCourierIcon;
    private final Boolean lastUpdate;

    // Constructor
    public CustomParcelsAdapterV2(Context context, ArrayList<ParcelItem> parcelItems, Boolean showCourierIcon, boolean lastUpdate) {
        super(context, R.layout.parcel_item, parcelItems);
        // TODO Auto-generated constructor stub

        this.context = context;
        this.parcelItems = parcelItems;
        this.showCourierIcon = showCourierIcon;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.lastUpdate = lastUpdate;
    }


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
        TextView parcelLastMovementStatusTV = rowView.findViewById(R.id.parcelLastMovementStatusTV);
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
                R.drawable.ic_waiting4pickup
        };

        // Get current parcel item to variable once
        ParcelItem parcelItem = parcelItems.get(position);


        String phase = parcelItem.getParcelPhase();
        int pos = 1;
        final String latestEventDescription = (parcelItem.getParcelLatestEventDescription() != null ?
                parcelItem.getParcelLatestEventDescription() : "");

        phaseTextFix = context.getString(R.string.package_not_found);

        // Check for item in transit
        if (phase.length() == 9) {
            pos = 5;
            phaseTextFix = context.getString(R.string.status_delivered);
        } else if (phase.equals(PhaseNumber.PHASE_WAITING_FOR_PICKUP)) {
            // TODO made new icon. if it's good, let's use that in upcoming ones, as it's SVG not png
            pos = 9;
            phaseTextFix = context.getString(R.string.status_waiting_for_pickup);
        } else if (phase.length() == 11 || phase.equals("TRANSIT")) {
            pos = 3;
            phaseTextFix = context.getString(R.string.status_in_transit);
        } else if (phase.length() == 12) {
            pos = 3;
            phaseTextFix = context.getString(R.string.status_in_transit);
        } else if (phase.length() == 16) {
            pos = 4;
            phaseTextFix = context.getString(R.string.status_ready);
        } else if (phase.length() == 8 || phase.equals("RETURNED_TO_SENDER")) {
            pos = 7;
            phaseTextFix = context.getString(R.string.status_returned);
        } else if (phase.length() == 7) {
            pos = 8;
            phaseTextFix = context.getString(R.string.status_customs);
        }
        // Not inside Finland
        else if (phase.length() == 24) {
            pos = 2;
            phaseTextFix = context.getString(R.string.status_in_transit);
        } else if (parcelItem.getParcelCarrier().equals("99") && phase.equals("")) {
            pos = 6;
            phaseTextFix = "";
        }

        // -----------------------------------------------------------------------------------------
        /* Set values */

        // First line
        if (parcelItem.getParcelTitle() != null && !parcelItem.getParcelTitle().equals("")) {
            firstLineBold.setText(parcelItem.getParcelTitle()); // Set given name instead of parcel code
        } else {
            firstLineBold.setText(parcelItem.getParcelCode()); // Set parcel code
        }


        // Second line normal
        if (parcelItem.getParcelTitle() != null && !parcelItem.getParcelTitle().equals("")) {
            secondLineNormal.setVisibility(View.VISIBLE);
            secondLineNormal.setText(parcelItem.getParcelCode());
        } else {
            secondLineNormal.setVisibility(View.GONE);
        }


        // Third line normal
        if (!parcelItem.getArchivedPackage()) {
            if (phaseTextFix.equals(context.getString(R.string.package_not_found))) {
                final String text = (!parcelItem.getParcelCode().equals("-") ? context.getString(R.string.custom_parcels_adapter_package_not_found_check_courier_company) : "");
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
        if (parcelItem.getArchivedPackage()) {
            // Sender
            if (parcelItem.getParcelSender() != null && !parcelItem.getParcelSender().equals("")) {
                fourthLineTitle.setVisibility(View.VISIBLE);
                String str = context.getString(R.string.parcel_sender) + " ";
                fourthLineTitle.setText(str);
                fourthLineNormal.setVisibility(View.VISIBLE);
                fourthLineNormal.setText(parcelItem.getParcelSender());
            }
            // Delivery method
            if (parcelItem.getParcelDeliveryMethod() != null && !parcelItem.getParcelDeliveryMethod().equals("")) {
                fifthLineTitle.setVisibility(View.VISIBLE);
                String str = context.getString(R.string.parcel_delivery_method) + " ";
                fifthLineTitle.setText(str);
                fifthLineNormal.setVisibility(View.VISIBLE);
                fifthLineNormal.setText(parcelItem.getParcelDeliveryMethod());
            }
            // Additional notes
            if (parcelItem.getParcelAdditionalNote() != null && !parcelItem.getParcelAdditionalNote().equals("")) {
                sixthLineTitle.setVisibility(View.VISIBLE);
                String str = context.getString(R.string.parcel_additional_notes) + " ";
                sixthLineTitle.setText(str);
                sixthLineNormal.setVisibility(View.VISIBLE);
                sixthLineNormal.setText(parcelItem.getParcelAdditionalNote());
            }
        }


        // Seventh line
        if (parcelItem.getParcelLastPickupDate() != null) {
            if (!parcelItem.getParcelLastPickupDate().equals("") && !parcelItem.getParcelLastPickupDate().equals("null")) {
                seventhLineNormal.setVisibility(View.VISIBLE);
                String str = context.getString(R.string.parcel_last_pickup_date) + " " + parcelItem.getParcelLastPickupDate();
                seventhLineNormal.setText(str);
            }
        }


        // Fourth line
        parcelLastMovementStatusTV.setVisibility(!lastUpdate || (parcelItem.getLastEventDate() == null || parcelItem.getArchivedPackage()) ? View.GONE : View.VISIBLE);
        if (!parcelItem.getArchivedPackage()) {
            parcelUpdateStatusTV.setText(parcelItem.getParcelUpdateStatus());
            if (lastUpdate) {
                if (parcelItem.getLastEventDate() != null) {
                    parcelUpdateStatusTV.setVisibility(View.GONE);
                    parcelLastMovementStatusTV.setText(context.getString(R.string.last_change, parcelItem.getLastEventDate()));
                }
            }
        } else {
            if (parcelItem.getParcelCreateDate() != null && !parcelItem.getParcelCreateDate().equals("null")) {
                String str = context.getString(R.string.parcel_parcel_added_time_stamp) + " " + parcelItem.getParcelCreateDate();
                parcelUpdateStatusTV.setText(str);
            } else {
                parcelUpdateStatusTV.setVisibility(View.GONE);
            }
        }


        // Set image resource
        statusImageView.setImageResource(drawerImages[pos]);

        // Carrier icon resource feature
        if (showCourierIcon) {
            courierIcon.setImageResource(CarrierUtils.getCarrierIconResourceForCarrierNumber(parcelItem.getParcelCarrierNumber()));
        } else {
            courierIcon.setVisibility(View.GONE);
        }


        return rowView;
    }


} // End of class
