package com.nitramite.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.nitramite.utils.CarrierUtils;
import com.nitramite.paketinseuranta.R;

public class CustomCarrierSpinnerAdapter extends ArrayAdapter<String> {

    private int groupId;
    private final Activity context;
    private final String[] carrierItems;
    private LayoutInflater inflater;

    public CustomCarrierSpinnerAdapter(Activity context, int groupId, String[] carrierItems) {
        super(context, R.layout.carrier_adapter, carrierItems);
        // TODO Auto-generated constructor stub

        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.context = context;
        this.groupId = groupId;
        this.carrierItems = carrierItems;
    }


    @SuppressWarnings({"NullableProblems", "HardCodedStringLiteral"})
    public View getView(int position, View convertView, ViewGroup parent ){
        @SuppressLint("ViewHolder") View itemView = inflater.inflate(groupId, parent, false);

        // Find views
        ImageView carrierLogo = itemView.findViewById(R.id.carrierLogo);
        TextView carrierName = itemView.findViewById(R.id.carrierName);

        // Set package info view
        carrierName.setText(carrierItems[position]);

        // Get icon resource
        carrierLogo.setBackgroundResource(CarrierUtils.getCarrierIconResourceForCarrierName(carrierItems[position]));

        return itemView;
    }



    @SuppressWarnings("NullableProblems")
    public View getDropDownView(int position, View convertView, ViewGroup parent){
        return getView(position, convertView, parent);

    }


} 
