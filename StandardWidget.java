package com.musicplayer.widgets;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.musicplayer.R;
import com.musicplayer.service.MusicController;

import static com.musicplayer.utils.Constants.ACTION_NEXT;
import static com.musicplayer.utils.Constants.ACTION_PLAY_PAUSE;
import static com.musicplayer.utils.Constants.ACTION_PREVIOUS;

public class StandardWidget extends BaseWidget {

    private static final String TAG = "StandardWidget " ;
    private static final String ACTION_MENU_CLICKED = "MenuClicked";
    private MusicController mCont;
    private boolean D = true;

    @Override
    int getLayoutRes() {
        return R.layout.widget_layout_standard;
    }

    @Override
    protected void onViewsUpdate(Context context, RemoteViews remoteViews, ComponentName serviceName, Bundle extras) {

        remoteViews.setOnClickPendingIntent(R.id.btnNext, PendingIntent.getService(
                context,
                REQUEST_NEXT,
                new Intent(context, MusicController.class)
                        .setAction(ACTION_NEXT)
                        .setComponent(serviceName),
                0
        ));
        remoteViews.setOnClickPendingIntent(R.id.btnPlay, PendingIntent.getService(
                context,
                REQUEST_PLAY_PAUSE,
                new Intent(context, MusicController.class)
                        .setAction(ACTION_PLAY_PAUSE)
                        .setComponent(serviceName),
                0
        ));
        remoteViews.setOnClickPendingIntent(R.id.btnPrevious, PendingIntent.getService(
                context,
                REQUEST_PREVIEW,
                new Intent(context, MusicController.class)
                        .setAction(ACTION_PREVIOUS)
                        .setComponent(serviceName),
                0
        ));
    }

    @Override
    public void onReceive(Context context, Intent intent) {

       super.onReceive(context,intent);
    }


}