package com.nitramite.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nitramite.paketinseuranta.R;

import java.util.ArrayList;

/**
 * Created by Martin on 15.1.2016.
 * Things to note!
 * custom_events_recycler_view contains recycler view component
 * This file is custom recycler view adapter which uses layout named custom_events_adapter_cardview
 */
public class CustomEventsRecyclerViewAdapter extends RecyclerView.Adapter<CustomEventsRecyclerViewAdapter.MyViewHolder> {

    private ArrayList<String> parcelEventsArray_FIS;
    private ArrayList<String> parcelEventsArray_TIMESTAMPS;
    private ArrayList<String> parcelEventsArray_LOCATIONCODES;
    private ArrayList<String> parcelEventsArray_LOCATIONNAMES;

    public CustomEventsRecyclerViewAdapter(ArrayList<String> parcelEventsArray_FIS, ArrayList<String> parcelEventsArray_TIMESTAMPS,
                                           ArrayList<String> parcelEventsArray_LOCATIONCODES, ArrayList<String> parcelEventsArray_LOCATIONNAMES) {
        this.parcelEventsArray_FIS = parcelEventsArray_FIS;
        this.parcelEventsArray_TIMESTAMPS = parcelEventsArray_TIMESTAMPS;
        this.parcelEventsArray_LOCATIONCODES = parcelEventsArray_LOCATIONCODES;
        this.parcelEventsArray_LOCATIONNAMES = parcelEventsArray_LOCATIONNAMES;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_events_adapter_cardview, parent, false);
        return new MyViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.fiOutput.setText(parcelEventsArray_FIS.get(position));
        holder.timestampOutput.setText(parcelEventsArray_TIMESTAMPS.get(position));
        String locationCodeFix;
        locationCodeFix = parcelEventsArray_LOCATIONCODES.get(position);
        if (locationCodeFix.length() == 4) {
            locationCodeFix = "-";
        }
        holder.locationcodenameOutput.setText(locationCodeFix + ", " + parcelEventsArray_LOCATIONNAMES.get(position));
    }

    @Override
    public int getItemCount() {
        return parcelEventsArray_FIS.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView fiOutput;
        private TextView timestampOutput;
        private TextView locationcodenameOutput;
        MyViewHolder(View itemView) {
            super(itemView);
            fiOutput = (TextView)itemView.findViewById(R.id.fiOutput);
            timestampOutput = (TextView)itemView.findViewById(R.id.timestampOutput);
            locationcodenameOutput = (TextView)itemView.findViewById(R.id.locationcodenameOutput);
        }
    }

}