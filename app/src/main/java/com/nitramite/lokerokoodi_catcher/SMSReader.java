/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.lokerokoodi_catcher;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class SMSReader {

    private Context context;

    public SMSReader(Context context) {
        this.context = context;
    }

    public void readSMSConversations() {
        // TODO check for permissions
        ContentResolver contentResolver = context.getContentResolver();
        final String[] projection = new String[]{"*"};
        Uri uri = Uri.parse("content://mms-sms/conversations/");
        Cursor query = contentResolver.query(uri, projection, null, null, null);
    }
}
