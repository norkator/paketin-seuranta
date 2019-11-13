package com.nitramite.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.nitramite.paketinseuranta.DatabaseHelper;
import com.nitramite.utils.Utils;
import java.util.ArrayList;

// Reference: https://gist.github.com/korniltsev/4071915
public class CustomAutoCompleteAdapter extends BaseAdapter implements Filterable {

    //  Logging
    private static final String TAG = "CustomAutoComplete";

    // Variables
    private Context context;
    private DatabaseHelper databaseHelper;
    private AutoCompleteScenario autoCompleteScenario;
    private ArrayList<String> objects;
    private String currentConstraint = "";


    // Constructor
    public CustomAutoCompleteAdapter(Activity context_, DatabaseHelper databaseHelper_, AutoCompleteScenario autoCompleteScenario_) {
        super();
        this.context = context_;
        this.databaseHelper = databaseHelper_;
        this.autoCompleteScenario = autoCompleteScenario_;
    }



    @Override
    public int getCount() {
        if (objects != null) {
            return objects.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        if (objects != null) {
            return objects.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextView beg = new TextView(context); // Beginning part of text
        TextView mid = new TextView(context); // Middle part of text
        TextView end = new TextView(context); // End part of text

        beg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        mid.setTypeface(Typeface.DEFAULT_BOLD);
        mid.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        end.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

        final String item = getItem(position).toString();
        mid.setText(currentConstraint);
        String[] split = item.split("(?i)" + currentConstraint);
        for (int i = 0; i < split.length; i++) {
            if (i == 0) {
                beg.setText(split[i]);
            }
            if (i == 1) {
                end.setText(split[i]);
            }
        }

        // Add views to stack
        linearLayout.addView(beg);
        linearLayout.addView(mid);
        linearLayout.addView(end);
        linearLayout.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
        return linearLayout;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults ret = new FilterResults();

                if (constraint != null) {

                    // Store current typing
                    currentConstraint = constraint.toString();

                    // For now everything had to be words 'test' not clauses 'hello world'
                    // Try overcome this stupid fucking shit limitation

                    if (databaseHelper != null) {
                        ArrayList<String> filtered = databaseHelper.getAutocompleteWordsForScenario(autoCompleteScenario, currentConstraint);
                        ret.values = filtered;
                        ret.count = filtered.size();
                    }

                }

                return ret;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                objects = (ArrayList<String>) results.values;
                notifyDataSetChanged();
            }
        };
    }


} // End of class