package com.nitramite.paketinseuranta;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

@SuppressWarnings("HardCodedStringLiteral")
public class Widget extends AppWidgetProvider {

    private static final String TAG = Widget.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), Widget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        try {
            for (int i = 0; i < appWidgetIds.length; i++) {
                Intent intent = new Intent(context, MainMenu.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                DatabaseHelper dbHelper = new DatabaseHelper(context);
                String status = dbHelper.getWidgetStatusInformation();
                RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
                rv.setTextViewText(R.id.statusText, status);

                Log.i(TAG, "Widget on update event");
                Intent updateIntent = new Intent(context, Widget.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                PendingIntent pendingUpdate = PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                rv.setOnClickPendingIntent(R.id.refreshPackagesBtn, pendingUpdate);

                // Finish
                rv.setOnClickPendingIntent(R.id.widgetBg, pendingIntent);
                appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

}