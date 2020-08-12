package com.nitramite.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.WindowManager;

import com.nitramite.paketinseuranta.R;

public class DialogUtils {

    public void genericErrorDialog(Context context, boolean isFinishing, final String title, final String description) {
        try {
            if (!isFinishing) {
                new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage(description)
                        .setPositiveButton(R.string.main_menu_close, (dialog, which) -> {
                        })
                        .setIcon(R.mipmap.ps_logo_round)
                        .show();
            }
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        }
    }


}
