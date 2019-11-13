package com.nitramite.paketinseuranta;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    @StringRes
    private int[] TAB_TITLES = null;

    // Variables
    private final Context mContext;


    // Fragments
    private FragmentTrackedDelivery fragmentTrackedDelivery = new FragmentTrackedDelivery();
    private FragmentNumberList fragmentNumberList = new FragmentNumberList();

    // Constructor
    SectionsPagerAdapter(Context context, FragmentManager fm, FragmentTrackedDelivery.TrackedDeliveryType trackedDeliveryType) {
        super(fm);
        mContext = context;
        if (trackedDeliveryType == FragmentTrackedDelivery.TrackedDeliveryType.NEW_PACKAGE) {
            TAB_TITLES = new int[]{R.string.parcel_editor_tab_one, R.string.parcel_editor_tab_two};
        } else {
            TAB_TITLES = new int[]{R.string.parcel_editor_tab_edit_existing};
        }
    }

    // Returns the fragment to display for that page
    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment = fragmentTrackedDelivery;
                break;
            case 1:
                fragment = fragmentNumberList;
                break;
        }
        return fragment;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        return TAB_TITLES.length;
    }
}