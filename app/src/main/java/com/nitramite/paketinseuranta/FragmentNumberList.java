package com.nitramite.paketinseuranta;

import android.content.ClipboardManager;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nitramite.courier.ParcelObject;
import com.nitramite.utils.CarrierUtils;

import static android.content.Context.CLIPBOARD_SERVICE;

public class FragmentNumberList extends Fragment {

    // Logging
    @SuppressWarnings("HardCodedStringLiteral")
    private static final String TAG = FragmentNumberList.class.getSimpleName();

    // Activity components
    private EditText addTrackingCodesInput;

    // Variables
    private String multiLines = "";


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_number_list, container, false);

        // Find views
        addTrackingCodesInput = view.findViewById(R.id.addTrackingCodesInput);
        Button addBtn = view.findViewById(R.id.addAddBtn);
        Button pasteBtn = view.findViewById(R.id.addPasteBtn);


        addTrackingCodesInput.setFilters(new InputFilter[]{new InputFilter.AllCaps()});

        addBtn.setOnClickListener(v1 -> {
            multiLines = addTrackingCodesInput.getText().toString();
            onAddItems();
        });

        pasteBtn.setOnClickListener(v12 -> {
            StringBuilder clipStringBuilder = new StringBuilder();
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
                    for (int i = 0; i < clipboard.getPrimaryClip().getItemCount(); i++) {
                        clipStringBuilder.append(clipboard.getPrimaryClip().getItemAt(i).getText());
                    }
                    addTrackingCodesInput.setText(clipStringBuilder.toString());
                } else {
                    Toast.makeText(getContext(), R.string.main_menu_add_tracking_codes_your_clipboard_is_empty, Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }


    // Adds new item on list, before adding check for it's existence
    public void onAddItems() {
        try {
            if ((getActivity() != null)) {
                if (multiLines.length() <= 1) {
                    Toast.makeText(getContext(), R.string.main_menu_field_is_empty, Toast.LENGTH_LONG).show();
                } else {
                    String delimiter = "\n";
                    final String[] parcelLines = multiLines.split(delimiter);
                    for (String parcelLine : parcelLines) {
                        boolean write = true;
                        if (((ParcelEditor) getActivity()).databaseHelper.checkForPackageExistence(parcelLine)) {
                            write = false;
                        }
                        if (write) {
                            String carrierCode = String.valueOf(CarrierUtils.detectCarrier(parcelLine));

                            ParcelObject parcelObject = new ParcelObject(parcelLine);
                            parcelObject.setCarrier(carrierCode);
                            parcelObject.setTrackingCode(parcelLine);
                            parcelObject.setCarrierStatus("0");
                            parcelObject.setOriginalTrackingCode(parcelLine);

                            ((ParcelEditor) getActivity()).databaseHelper.insertData(parcelObject);

                        } else {
                            Toast.makeText(getContext(), parcelLine + " " + getString(R.string.main_menu_item_not_added_because_it_already_existed_on_list), Toast.LENGTH_LONG).show();
                        }
                    }
                    getActivity().finish();
                }
            } else {
                Toast.makeText(getContext(), R.string.reference_to_database_is_null_cannot_add_items, Toast.LENGTH_LONG).show();
            }
        } catch (StringIndexOutOfBoundsException ignored) {
        }
    }


} // End of class