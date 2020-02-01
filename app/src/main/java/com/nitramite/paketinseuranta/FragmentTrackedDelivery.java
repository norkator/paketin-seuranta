package com.nitramite.paketinseuranta;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.integration.android.IntentIntegrator;
import com.nitramite.adapters.AutoCompleteScenario;
import com.nitramite.adapters.CustomAutoCompleteAdapter;
import com.nitramite.adapters.CustomCarrierSpinnerAdapter;
import com.nitramite.utils.CarrierUtils;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@SuppressWarnings("ConstantConditions")
public class FragmentTrackedDelivery extends Fragment implements DatePickerDialog.OnDateSetListener, FragmentTrackedDeliveryInterface {

    // Logging
    private static final String TAG = "FragmentTrackedDelivery";

    // Date picker scenario
    enum DatePickerScenario {
        ORDER_DATE,
        MANUAL_DELIVERY_DATE,
    }

    enum TrackedDeliveryType {
        NEW_PACKAGE,
        EXISTING_PACKAGE
    }

    // Time parsing
    private @SuppressLint("SimpleDateFormat")
    DateFormat pickerDateFormat = new SimpleDateFormat("yyyy-M-dd");
    private @SuppressLint("SimpleDateFormat")
    DateFormat showingDateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private @SuppressLint("SimpleDateFormat")
    DateFormat SQLiteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Activity components
    private EditText trackingCodeET;
    private TextView orderDateTV, manualDeliveryDateTV;
    private EditText packageNameET;
    private EditText lockerCodeET;
    private EditText additionalNoteET;
    private EditText productPageET;
    private AutoCompleteTextView senderET;
    private AutoCompleteTextView deliveryMethodET;
    private String orderDateSqliteFormatStr = null;
    private String manualDeliveryDateSqliteFormatStr = null;


    // Variables
    private DatePickerScenario datePickerScenario;


    @SuppressWarnings("AccessStaticViaInstance")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tracked_delivery, container, false);

        // Connect interface for barcode result
        ((ParcelEditor)getActivity()).setFragmentTrackedDeliveryInterface(this);

        // Find components
        Button positiveBtn = view.findViewById(R.id.positiveBtn);
        if (((ParcelEditor) getActivity()).trackedDeliveryType == TrackedDeliveryType.NEW_PACKAGE) {
            positiveBtn.setText(R.string.add);
        }
        Button negativeBtn = view.findViewById(R.id.negativeBtn);
        packageNameET = view.findViewById(R.id.packageNameET);
        lockerCodeET = view.findViewById(R.id.lockerCodeET);
        trackingCodeET = view.findViewById(R.id.trackingCodeET);
        senderET = view.findViewById(R.id.senderET);
        deliveryMethodET = view.findViewById(R.id.deliveryMethodET);
        Spinner selectCarrierSpinner = view.findViewById(R.id.selectCarrierSpinner);
        additionalNoteET = view.findViewById(R.id.additionalNoteET);
        productPageET = view.findViewById(R.id.productPageET);
        orderDateTV = view.findViewById(R.id.orderDateTV);
        manualDeliveryDateTV = view.findViewById(R.id.manualDeliveryDateTV);
        Button selectOrderDateBtn = view.findViewById(R.id.selectOrderDateBtn);
        Button selectManualDeliveryDateBtn = view.findViewById(R.id.selectManualDeliveryDateBtn);
        Button pasteTrackingNumberBtn = view.findViewById(R.id.pasteTrackingNumberBtn);
        Button scanTrackingNumberBtn = view.findViewById(R.id.scanTrackingNumberBtn);

        /*
         * Force all caps for tracking code
         * https://stackoverflow.com/questions/15961813/in-android-edittext-how-to-force-writing-uppercase
         */
        trackingCodeET.setFilters(new InputFilter[]{new InputFilter.AllCaps()});


        // Set data into views
        Cursor res = ((ParcelEditor) getActivity()).databaseHelper.getEditPackageData(((ParcelEditor) getActivity()).parcelId);
        res.moveToFirst();
        packageNameET.setText(res.getString(0));
        lockerCodeET.setText((res.getString(1).equals("null") ? "" : res.getString(1)));
        final int carrierNumber = res.getInt(2);
        trackingCodeET.setText(res.getString(3));
        senderET.setText(res.getString(4));
        deliveryMethodET.setText(res.getString(5));
        additionalNoteET.setText(res.getString(6));
        productPageET.setText(res.getString(7));
        if (res.getString(8) == null) {
            orderDateTV.setText(getContext().getString(R.string.add_edit_dialog_order_date_not_selected));
        } else {
            orderDateSqliteFormatStr = res.getString(8);
            orderDateTV.setText(formatSqLiteDateToShowingDate(res.getString(8)));
        }
        if (res.getString(9) == null) {
            manualDeliveryDateTV.setText(getContext().getString(R.string.add_edit_dialog_order_date_not_selected));
        } else {
            manualDeliveryDateSqliteFormatStr = res.getString(9);
            manualDeliveryDateTV.setText(formatSqLiteDateToShowingDate(res.getString(9)));
        }

        // Set carrier spinner, carrier changing adapter
        setCarrierSpinnerData(getActivity(), selectCarrierSpinner, carrierNumber, ((ParcelEditor) getActivity()).databaseHelper, ((ParcelEditor) getActivity()).parcelId);

        // Construct auto complete word list for sender field
        CustomAutoCompleteAdapter senderAutoCompleteAdapter = new CustomAutoCompleteAdapter(getActivity(), ((ParcelEditor) getActivity()).databaseHelper, AutoCompleteScenario.SENDER);
        senderET.setThreshold(1); // Will start working from first character
        senderET.setAdapter(senderAutoCompleteAdapter);

        // Construct auto complete word list for delivery method field
        CustomAutoCompleteAdapter deliveryMethodAutoCompleteAdapter = new CustomAutoCompleteAdapter(getActivity(), ((ParcelEditor) getActivity()).databaseHelper, AutoCompleteScenario.DELIVERY_METHOD);
        deliveryMethodET.setThreshold(1); // Will start working from first character
        deliveryMethodET.setAdapter(deliveryMethodAutoCompleteAdapter);


        packageNameET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (packageNameET.getText().length() == 0 && trackingCodeET.getText().length() == 0) {
                    positiveBtn.setEnabled(false);
                    positiveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.buttonDisabled));
                } else {
                    positiveBtn.setEnabled(true);
                    positiveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.bootStrapPrimary));
                }
                ((ParcelEditor)getActivity()).toggleSaveActionBtnVisibility(positiveBtn.isEnabled());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        trackingCodeET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (packageNameET.getText().length() == 0 && trackingCodeET.getText().length() == 0) {
                    positiveBtn.setEnabled(false);
                    positiveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.buttonDisabled));
                } else {
                    positiveBtn.setEnabled(true);
                    positiveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.bootStrapPrimary));
                }
                ((ParcelEditor)getActivity()).toggleSaveActionBtnVisibility(positiveBtn.isEnabled());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });


        selectOrderDateBtn.setOnClickListener(view_ -> {
            datePickerScenario = DatePickerScenario.ORDER_DATE;
            Calendar c = Calendar.getInstance();
            int mYear = c.get(Calendar.YEAR);
            int mMonth = c.get(Calendar.MONTH);
            int mDay = c.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog =
                    new DatePickerDialog(getContext(), this, mYear, mMonth, mDay);
            datePickerDialog.show();
        });


        selectManualDeliveryDateBtn.setOnClickListener(view_ -> {
            datePickerScenario = DatePickerScenario.MANUAL_DELIVERY_DATE;
            Calendar c = Calendar.getInstance();
            int mYear = c.get(Calendar.YEAR);
            int mMonth = c.get(Calendar.MONTH);
            int mDay = c.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog =
                    new DatePickerDialog(getActivity(), this, mYear, mMonth, mDay);
            datePickerDialog.show();
        });


        // Button initial validation
        if (packageNameET.getText().length() == 0 && trackingCodeET.getText().length() == 0) {
            positiveBtn.setEnabled(false);
            positiveBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.buttonDisabled));
            ((ParcelEditor)getActivity()).toggleSaveActionBtnVisibility(positiveBtn.isEnabled());
        } else {
            ((ParcelEditor)getActivity()).saveBtnMenuItemVisibility = true;
        }

        res.close();


        positiveBtn.setOnClickListener(v -> {
            onPositiveBtnClick();
        });

        negativeBtn.setOnClickListener(v -> {
            switch (((ParcelEditor) getActivity()).trackedDeliveryType) {
                case NEW_PACKAGE:
                    ((ParcelEditor) getActivity()).databaseHelper.deletePackageData(((ParcelEditor) getActivity()).parcelId);
                    break;
                case EXISTING_PACKAGE:
                    break;
            }
            getActivity().setResult(getActivity().RESULT_OK);
            getActivity().finish();
        });


        pasteTrackingNumberBtn.setOnClickListener(view1 -> {
            try {
                ClipData.Item clipData = ((ParcelEditor) getActivity()).clipboard.getPrimaryClip().getItemAt(0);
                trackingCodeET.setText(clipData.getText().toString());
            } catch (NullPointerException e) {
                Toast.makeText(getContext(), getString(R.string.empty_clipboard), Toast.LENGTH_SHORT).show();
            }
        });

        scanTrackingNumberBtn.setOnClickListener(view12 -> {
            if (((ParcelEditor) getActivity()).hasCameraPermission()) {
                IntentIntegrator integrator = new IntentIntegrator(getActivity());
                integrator.setCaptureActivity(BarcodeCaptureActivity.class);
                integrator.setOrientationLocked(false);
                integrator.setPrompt("");
                integrator.initiateScan();
            }
        });


        return view;
    }


    public void onPositiveBtnClick() {
        String givenTrackingCode = null;
        switch (((ParcelEditor) getActivity()).trackedDeliveryType) {
            case EXISTING_PACKAGE:
                // Update
                if (((ParcelEditor) getActivity()).databaseHelper.updateEditPackageData(((ParcelEditor) getActivity()).parcelId, packageNameET.getText().toString(), lockerCodeET.getText().toString(),
                        trackingCodeET.getText().toString(), senderET.getText().toString(), deliveryMethodET.getText().toString(), additionalNoteET.getText().toString(),
                        null, productPageET.getText().toString(), orderDateSqliteFormatStr, manualDeliveryDateSqliteFormatStr)) {
                    Toast.makeText(getActivity(), R.string.edit_package_utils_changes_saved, Toast.LENGTH_SHORT).show();
                    getActivity().setResult(getActivity().RESULT_OK);
                    getActivity().finish();
                    break;
                }
                break;
            case NEW_PACKAGE:
                // Check for existence, if not exists continue. Remember that empty field is allowed
                givenTrackingCode = trackingCodeET.getText().toString();
                if (((ParcelEditor) getActivity()).databaseHelper.checkForPackageExistence(givenTrackingCode) && givenTrackingCode.length() != 0) {
                    Toast.makeText(getActivity(), trackingCodeET.getText().toString() + " " + getActivity().getString(R.string.main_menu_item_not_added_because_it_already_existed_on_list), Toast.LENGTH_SHORT).show();
                } else {
                    if (((ParcelEditor) getActivity()).databaseHelper.updateEditPackageData(((ParcelEditor) getActivity()).parcelId, packageNameET.getText().toString(), lockerCodeET.getText().toString(),
                            trackingCodeET.getText().toString(), senderET.getText().toString(), deliveryMethodET.getText().toString(), additionalNoteET.getText().toString(),
                            trackingCodeET.getText().toString(), productPageET.getText().toString(), orderDateSqliteFormatStr, manualDeliveryDateSqliteFormatStr)) {
                        Toast.makeText(getActivity(), R.string.edit_package_utils_tracked_delivery_created, Toast.LENGTH_SHORT).show();
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("PARCEL_ID", ((ParcelEditor) getActivity()).parcelId);
                        getActivity().setResult(Activity.RESULT_OK, returnIntent);
                        getActivity().finish();
                        break;
                    }
                }
                break;
        }
    }


    // Populates carrier spinner with selectable carrier and preselects current selected carrier
    public void setCarrierSpinnerData(Activity activityContext, Spinner selectCarrierSpinner, int currentCarrierNumber, DatabaseHelper databaseHelper, String parcelDatabaseId) {
        // Parcel carries (User can change this if incorrect)
        String[] carriers = {
                CarrierUtils.CARRIER_POSTI_STR,
                //CarrierUtils.CARRIER_POSTI_CHINA_STR,
                CarrierUtils.CARRIER_MATKAHUOLTO_STR,
                CarrierUtils.CARRIER_DHL_EXPRESS_STR,
                CarrierUtils.CARRIER_DHL_AMAZON_STR,
                CarrierUtils.CARRIER_DHL_ACTIVE_TRACKING_STR,
                CarrierUtils.CARRIER_UPS_STR,
                CarrierUtils.CARRIER_FEDEX_STR,
                CarrierUtils.CARRIER_POSTNORD_STR,
                CarrierUtils.CARRIER_ARRA_PAKETTI_STR,
                CarrierUtils.CARRIER_USPS_STR,
                CarrierUtils.CARRIER_YANWEN_STR,
                CarrierUtils.CARRIER_GLS_STR,
                CarrierUtils.CARRIER_CAINIAO_STR,
                // CarrierUtils.CARRIER_CPRAM_STR,
                CarrierUtils.CARRIER_OTHER_STR
        };
        Integer[] carrierCodes = {
                CarrierUtils.CARRIER_POSTI,
                //CarrierUtils.CARRIER_CHINA,
                CarrierUtils.CARRIER_MATKAHUOLTO,
                CarrierUtils.CARRIER_DHL_EXPRESS,
                CarrierUtils.CARRIER_DHL_AMAZON,
                CarrierUtils.CARRIER_DHL_ACTIVE_TRACKING,
                CarrierUtils.CARRIER_UPS,
                CarrierUtils.CARRIER_FEDEX,
                CarrierUtils.CARRIER_POSTNORD,
                CarrierUtils.CARRIER_ARRA_PAKETTI,
                CarrierUtils.CARRIER_USPS,
                CarrierUtils.CARRIER_YANWEN,
                CarrierUtils.CARRIER_GLS,
                CarrierUtils.CARRIER_CAINIAO,
                // CarrierUtils.CARRIER_CPRAM,
                CarrierUtils.CARRIER_OTHER
        };
        CustomCarrierSpinnerAdapter customCarrierAdapter = new CustomCarrierSpinnerAdapter(activityContext, R.layout.carrier_adapter, carriers);
        selectCarrierSpinner.setAdapter(customCarrierAdapter);
        switch (currentCarrierNumber) {
            case CarrierUtils.CARRIER_POSTI:
                selectCarrierSpinner.setSelection(0);
                break;
                /*
            case CarrierUtils.CARRIER_CHINA:
                selectCarrierSpinner.setSelection(1);
                break;
                */
            case CarrierUtils.CARRIER_MATKAHUOLTO:
                selectCarrierSpinner.setSelection(1);
                break;
            case CarrierUtils.CARRIER_DHL_EXPRESS:
                selectCarrierSpinner.setSelection(2);
                break;
            case CarrierUtils.CARRIER_DHL_AMAZON:
                selectCarrierSpinner.setSelection(3);
                break;
            case CarrierUtils.CARRIER_DHL_ACTIVE_TRACKING:
                selectCarrierSpinner.setSelection(4);
                break;
            case CarrierUtils.CARRIER_UPS:
                selectCarrierSpinner.setSelection(5);
                break;
            case CarrierUtils.CARRIER_FEDEX:
                selectCarrierSpinner.setSelection(6);
                break;
            case CarrierUtils.CARRIER_POSTNORD:
                selectCarrierSpinner.setSelection(7);
                break;
            case CarrierUtils.CARRIER_ARRA_PAKETTI:
                selectCarrierSpinner.setSelection(8);
                break;
            case CarrierUtils.CARRIER_USPS:
                selectCarrierSpinner.setSelection(9);
                break;
            case CarrierUtils.CARRIER_YANWEN:
                selectCarrierSpinner.setSelection(10);
                break;
            case CarrierUtils.CARRIER_GLS:
                selectCarrierSpinner.setSelection(11);
                break;
            case CarrierUtils.CARRIER_CAINIAO:
                selectCarrierSpinner.setSelection(12);
                break;
                /*
            case CarrierUtils.CARRIER_CPRAM:
                selectCarrierSpinner.setSelection(13);
                break;
                */
            case CarrierUtils.CARRIER_OTHER:
                selectCarrierSpinner.setSelection(13);
                break;
        }
        selectCarrierSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (!databaseHelper.updateCarrierCode(parcelDatabaseId, carrierCodes[i])) {
                    Toast.makeText(activityContext, R.string.edit_package_utils_changing_courier_failed, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }


    @Override
    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
        try {
            String timeStamp = datePicker.getYear() + "-" + (datePicker.getMonth() + 1) + "-" + datePicker.getDayOfMonth();
            Date parseTimeDate = pickerDateFormat.parse(timeStamp);
            final String showingDate = showingDateFormat.format(parseTimeDate);
            switch (datePickerScenario) {
                case ORDER_DATE:
                    orderDateTV.setText(showingDate);
                    orderDateSqliteFormatStr = SQLiteDateFormat.format(parseTimeDate);
                    break;
                case MANUAL_DELIVERY_DATE:
                    manualDeliveryDateTV.setText(showingDate);
                    manualDeliveryDateSqliteFormatStr = SQLiteDateFormat.format(parseTimeDate);
                    break;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    /**
     * Format sqlite date to showing date
     *
     * @param sqliteDate string
     * @return str
     */
    private String formatSqLiteDateToShowingDate(String sqliteDate) {
        try {
            Date date = SQLiteDateFormat.parse(sqliteDate);
            return showingDateFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Set tracking code from activity hosting this fragment
     * @param trackingCode str
     */
    public void setTrackingCodeToFragment(final String trackingCode) {
        trackingCodeET.setText(trackingCode);
    }

    @Override
    public void onBarcodeScanResult(String barcode) {
        trackingCodeET.setText(barcode);
    }

    @Override
    public void onSaveChangesActionBtnClick() {
        onPositiveBtnClick();
    }

} // End of class