/*
 * Copyright (c) 2020
 * Paketin Seuranta
 *
 * @author developerfromjokela
 * @author norkator
 */

package com.nitramite.paketinseuranta.notifier;

import com.google.firebase.messaging.FirebaseMessaging;

@SuppressWarnings("HardCodedStringLiteral")
public class PushUtils {

    public static String TOPIC_UPDATE = "/topics/update_parcels";

    public static void subscribeToTopic(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic);
    }

    public static void unsubscribeFromTopic(String topic) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
    }
}
