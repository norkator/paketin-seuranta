package com.nitramite.utils;

import android.app.AlertDialog;
import android.content.Context;

import com.nitramite.paketinseuranta.R;

public class dialogUtils {

    public static void genericErrorDialog(Context context, boolean isFinishing, final String title, final String description) {
        if (!isFinishing) {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(description)
                    .setPositiveButton(R.string.main_menu_close, (dialog, which) -> {
                    })
                    .setIcon(R.mipmap.ps_logo_round)
                    .show();
        }
    }


}
