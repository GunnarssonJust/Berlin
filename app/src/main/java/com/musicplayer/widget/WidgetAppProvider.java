package com.musicplayer.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;


import com.musicplayer.R;
import com.musicplayer.activities.MusicActivity;

public class WidgetAppProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for(int appWidgetId : appWidgetIds){
            Intent intent = new Intent(context, MusicActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,0);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_standard);
            views.setOnClickPendingIntent(R.id.imageViewAlbumArt_widget_std,pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId,views);
        }
    }
}