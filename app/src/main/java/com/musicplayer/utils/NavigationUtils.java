package com.musicplayer.utils;

import android.content.Context;
import android.content.Intent;

import com.musicplayer.activities.MusicActivity;

public class NavigationUtils {
    public static Intent getNowPlayingIntent(Context context) {

        final Intent intent = new Intent(context, MusicActivity.class);
        intent.setAction(Constants.NAVIGATE_NOWPLAYING);
        return intent;
    }
}
