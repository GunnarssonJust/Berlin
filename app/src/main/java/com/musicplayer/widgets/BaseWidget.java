package com.musicplayer.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.widget.RemoteViews;

import com.musicplayer.R;
import com.musicplayer.activities.MusicActivity;
import com.musicplayer.service.MusicController;

public abstract class BaseWidget extends AppWidgetProvider {

    protected static final int REQUEST_NEXT =1;
    protected static final int REQUEST_PREVIEW = 2;
    protected static final int REQUEST_PLAY_PAUSE = 3;
    private static final String TAG = "BaseWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        onUpdate(context, appWidgetManager, appWidgetIds,null);
    }

    private void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, Bundle extras){
        ComponentName serviceName = new ComponentName(context, MusicController.class);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getLayoutRes());
        try {
            onViewsUpdate(context, remoteViews, serviceName, extras);
            appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for(int appWidgetId : appWidgetIds){
            Intent intent = new Intent(context, MusicActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,0);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_big);
            views.setOnClickPendingIntent(R.id.widget_big_albumart,pendingIntent);
            views.setOnClickPendingIntent(R.id.widget_big_albumart_black,pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId,views);
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.startsWith("com.musicplayer.")) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), this.getClass().getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, appWidgetIds, intent.getExtras());
        } else {
            super.onReceive(context, intent);
        }
    }

    protected abstract void onViewsUpdate(Context context, RemoteViews remoteViews, ComponentName serviceName, Bundle extras);

    abstract @LayoutRes int getLayoutRes();
}
