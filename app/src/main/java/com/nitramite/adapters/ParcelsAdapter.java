package com.nitramite.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nitramite.paketinseuranta.ParcelItem;
import com.nitramite.paketinseuranta.PhaseNumber;
import com.nitramite.paketinseuranta.R;
import com.nitramite.utils.CarrierUtils;

import java.util.ArrayList;

public class ParcelsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //  Logging
    private static final String TAG = ParcelsAdapter.class.getSimpleName();

    // Variables
    private final Context context;
    private final ArrayList<ParcelItem> parcelItems;
    private final LayoutInflater mInflater;
    private final Boolean showCourierIcon;
    private final Boolean lastUpdate;
    private int lastAnimPosition = -1;


    // Constructor
    public ParcelsAdapter(Context context, ArrayList<ParcelItem> parcelItems, Boolean showCourierIcon, boolean lastUpdate) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.parcelItems = parcelItems;
        this.showCourierIcon = showCourierIcon;
        this.lastUpdate = lastUpdate;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.parcel_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        ViewHolder holder = (ViewHolder) viewHolder;

        // Defaults
        holder.fourthLineNormal.setVisibility(View.GONE);
        holder.fourthLineTitle.setVisibility(View.GONE);
        holder.fifthLineNormal.setVisibility(View.GONE);
        holder.fifthLineTitle.setVisibility(View.GONE);
        holder.sixthLineNormal.setVisibility(View.GONE);
        holder.sixthLineTitle.setVisibility(View.GONE);
        holder.seventhLineNormal.setVisibility(View.GONE);

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
        if (phase.equals(PhaseNumber.PHASE_DELIVERED)) {
            pos = 5;
            phaseTextFix = context.getString(R.string.status_delivered);
        } else if (phase.equals(PhaseNumber.PHASE_WAITING_FOR_PICKUP)) {
            pos = 9;
            phaseTextFix = context.getString(R.string.status_waiting_for_pickup);
        } else if (phase.equals(PhaseNumber.PHASE_TRANSIT)) {
            pos = 3;
            phaseTextFix = context.getString(R.string.status_in_transit);
        } else if (phase.equals(PhaseNumber.PHASE_IN_TRANSPORT)) {
            pos = 3;
            phaseTextFix = context.getString(R.string.status_in_transit);
        } else if (phase.equals(PhaseNumber.PHASE_READY_FOR_PICKUP)) {
            pos = 4;
            phaseTextFix = context.getString(R.string.status_ready);
        } else if (phase.equals(PhaseNumber.PHASE_RETURNED)
                || phase.equals(PhaseNumber.PHASE_RETURNED_TO_SENDER)
        ) {
            pos = 7;
            phaseTextFix = context.getString(R.string.status_returned);
        } else if (phase.equals(PhaseNumber.PHASE_CUSTOMS)) {
            pos = 8;
            phaseTextFix = context.getString(R.string.status_customs);
        }
        // Not inside Finland
        else if (phase.equals(PhaseNumber.PHASE_IN_TRANSPORT_NOT_IN_FINLAND)) {
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
            holder.firstLineBold.setText(parcelItem.getParcelTitle()); // Set given name instead of parcel code
        } else {
            holder.firstLineBold.setText(parcelItem.getParcelCode()); // Set parcel code
        }


        // Second line normal
        if (parcelItem.getParcelTitle() != null && !parcelItem.getParcelTitle().equals("")) {
            holder.secondLineNormal.setVisibility(View.VISIBLE);
            holder.secondLineNormal.setText(parcelItem.getParcelCode());
        } else {
            holder.secondLineNormal.setVisibility(View.GONE);
        }


        // Third line normal
        if (!parcelItem.getArchivedPackage()) {
            if (phaseTextFix.equals(context.getString(R.string.package_not_found))) {
                final String text = (!parcelItem.getParcelCode().equals("-") ? context.getString(R.string.custom_parcels_adapter_package_not_found_check_courier_company) : "");
                if (text.equals("")) {
                    holder.thirdLineNormal.setVisibility(View.GONE);
                } else {
                    holder.thirdLineNormal.setText(text);
                }
            } else {
                final String text = (latestEventDescription != null && latestEventDescription.length() > 0 ? latestEventDescription : phaseTextFix);
                if (text.equals("")) {
                    holder.thirdLineNormal.setVisibility(View.GONE);
                } else {
                    holder.thirdLineNormal.setText(text);
                }
            }
        } else {
            holder.thirdLineNormal.setHeight(10);
        }


        /*
          Archive related lines
         */
        if (parcelItem.getArchivedPackage()) {
            // Sender
            if (parcelItem.getParcelSender() != null && !parcelItem.getParcelSender().equals("")) {
                holder.fourthLineTitle.setVisibility(View.VISIBLE);
                String str = context.getString(R.string.parcel_sender) + " ";
                holder.fourthLineTitle.setText(str);
                holder.fourthLineNormal.setVisibility(View.VISIBLE);
                holder.fourthLineNormal.setText(parcelItem.getParcelSender());
            }
            // Delivery method
            if (parcelItem.getParcelDeliveryMethod() != null && !parcelItem.getParcelDeliveryMethod().equals("")) {
                holder.fifthLineTitle.setVisibility(View.VISIBLE);
                String str = context.getString(R.string.parcel_delivery_method) + " ";
                holder.fifthLineTitle.setText(str);
                holder.fifthLineNormal.setVisibility(View.VISIBLE);
                holder.fifthLineNormal.setText(parcelItem.getParcelDeliveryMethod());
            }
            // Additional notes
            if (parcelItem.getParcelAdditionalNote() != null && !parcelItem.getParcelAdditionalNote().equals("")) {
                holder.sixthLineTitle.setVisibility(View.VISIBLE);
                String str = context.getString(R.string.parcel_additional_notes) + " ";
                holder.sixthLineTitle.setText(str);
                holder.sixthLineNormal.setVisibility(View.VISIBLE);
                holder.sixthLineNormal.setText(parcelItem.getParcelAdditionalNote());
            }
        }


        // Seventh line
        if (parcelItem.getParcelLastPickupDate() != null) {
            if (!parcelItem.getParcelLastPickupDate().equals("") && !parcelItem.getParcelLastPickupDate().equals("null")) {
                holder.seventhLineNormal.setVisibility(View.VISIBLE);
                String str = context.getString(R.string.parcel_last_pickup_date) + " " + parcelItem.getParcelLastPickupDate();
                holder.seventhLineNormal.setText(str);
            }
        }


        // Fourth line
        holder.parcelLastMovementStatusTV.setVisibility(!lastUpdate || (parcelItem.getLastEventDate() == null || parcelItem.getArchivedPackage()) ? View.GONE : View.VISIBLE);
        if (!parcelItem.getArchivedPackage()) {
            holder.parcelUpdateStatusTV.setText(parcelItem.getParcelUpdateStatus());
            if (lastUpdate) {
                if (parcelItem.getLastEventDate() != null) {
                    holder.parcelUpdateStatusTV.setVisibility(View.GONE);
                    holder.parcelLastMovementStatusTV.setText(context.getString(R.string.last_change, parcelItem.getLastEventDate()));
                }
            }
        } else {
            if (parcelItem.getParcelCreateDate() != null && !parcelItem.getParcelCreateDate().equals("null")) {
                String str = context.getString(R.string.parcel_parcel_added_time_stamp) + " " + parcelItem.getParcelCreateDate();
                holder.parcelUpdateStatusTV.setText(str);
            } else {
                holder.parcelUpdateStatusTV.setVisibility(View.GONE);
            }
        }


        // Set image resource
        holder.statusImageView.setImageResource(drawerImages[pos]);

        // Carrier icon resource feature
        if (showCourierIcon) {
            holder.courierIcon.setImageResource(CarrierUtils.getCarrierIconResourceForCarrierNumber(parcelItem.getParcelCarrierNumber()));
        } else {
            holder.courierIcon.setVisibility(View.GONE);
        }

        // If parcel is unpaid show icon
        if (!parcelItem.getParcelPaid()) {
            holder.unpaidIcon.setImageResource(R.mipmap.unpaid);
            holder.unpaidIcon.setVisibility(View.VISIBLE);
        } else {
            holder.unpaidIcon.setVisibility(View.GONE);
        }

        setAnimation(holder.itemView, position);
    }

    /**
     * Run animation if new view
     *
     * @param view     for animation
     * @param position position
     */
    private void setAnimation(View view, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastAnimPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down);
            view.startAnimation(animation);

            lastAnimPosition = position;
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return parcelItems.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder {

        // Find views
        TextView firstLineBold, secondLineNormal, thirdLineNormal, fourthLineNormal, fourthLineTitle,
                fifthLineNormal, fifthLineTitle, sixthLineNormal, sixthLineTitle, seventhLineNormal,
                parcelUpdateStatusTV, parcelLastMovementStatusTV;
        ImageView statusImageView, courierIcon, unpaidIcon;


        ViewHolder(View itemView) {
            super(itemView);
            // Find views
            firstLineBold = itemView.findViewById(R.id.firstLineBold);
            secondLineNormal = itemView.findViewById(R.id.secondLineNormal);
            thirdLineNormal = itemView.findViewById(R.id.thirdLineNormal);
            fourthLineNormal = itemView.findViewById(R.id.fourthLineNormal);
            fourthLineTitle = itemView.findViewById(R.id.fourthLineTitle);
            fifthLineNormal = itemView.findViewById(R.id.fifthLineNormal);
            fifthLineTitle = itemView.findViewById(R.id.fifthLineTitle);
            sixthLineNormal = itemView.findViewById(R.id.sixthLineNormal);
            sixthLineTitle = itemView.findViewById(R.id.sixthLineTitle);
            seventhLineNormal = itemView.findViewById(R.id.seventhLineNormal);
            parcelUpdateStatusTV = itemView.findViewById(R.id.parcelUpdateStatusTV);
            parcelLastMovementStatusTV = itemView.findViewById(R.id.parcelLastMovementStatusTV);
            statusImageView = itemView.findViewById(R.id.statusImageView);
            courierIcon = itemView.findViewById(R.id.courierIcon);
            unpaidIcon = itemView.findViewById(R.id.unpaidIcon);

        }
    }


}
