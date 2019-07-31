package com.musicplayer.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.musicplayer.utils.Constants;

/**
 * This service class enables a notificationlistener
 * if skype enables his notification, service stops mediaplayer object until skype call is finished
 * Authors: Amit and Shazli from stackoverflow.com
 */

public class SkypeNotificationListenerService extends NotificationListenerService {

    private boolean mSkypeConnected;
    private static final String TAG = "NM";
    public SkypeNotificationListenerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.NOTIFICATION_LISTENER");
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(nlServiceReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(nlServiceReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        String packageName = sbn.getPackageName();
        Log.d(TAG, "onNotificationPosted " + packageName);
        if(packageName != null && packageName.equals("com.skype.raider")) {
            Intent intent = new Intent("com.example.NOTIFICATION_LISTENER");
            intent.putExtra("connected", true);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        String packageName = sbn.getPackageName();
        Log.d(TAG, "onNotificationRemoved " + packageName);
        if(packageName != null && packageName.equals("com.skype.raider")) {
            Intent intent = new Intent("com.example.NOTIFICATION_LISTENER");
            intent.putExtra("connected", false);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    @Override
    public StatusBarNotification[] getActiveNotifications() {
        return super.getActiveNotifications();
    }

    BroadcastReceiver nlServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null) {
                boolean connected = intent.getBooleanExtra("connected", false);
                Intent skypeIntent;
                skypeIntent = new Intent(Constants.SKYPE_CONNECTED);
                if(connected && !mSkypeConnected) {
                    mSkypeConnected = true;
                    skypeIntent.putExtra("connected", true);
                } else if(!connected) {
                    mSkypeConnected = false;
                    Log.d(TAG, "send broadcast disconnected");
                    skypeIntent.putExtra("connected", false);
                }
                sendStickyBroadcast(skypeIntent);
            }
        }
    };
}
