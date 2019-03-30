package com.musicplayer.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.musicplayer.player.MusicPlayer;
import com.musicplayer.player.PlayerState;
import com.musicplayer.utils.AudioFocusHelper;
import com.musicplayer.utils.MusicFocusable;

//This class allows the app to play music in background, even if app was closed via wiping gesture

public class MusicController extends Service implements MediaPlayer.OnErrorListener, MusicFocusable {

    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    AudioFocusHelper mAudioFocusHelper = null;


    private static final String TAG = "MusicController";

    public static final String CHANNEL_ID = "my_music_player";

    MusicPlayer player = null;

    private int noteId = 1;

    PlayerState mState;
    AudioFocus mAudioFocus;


    // the Binder that will be returned on call to onBind()
    private final IBinder mBinder = new ServiceBinder();

    @Override
    public void onGainedAudioFocus() {
        mAudioFocus = AudioFocus.Focused;
        // restart media player with new focus settings
        if (mState == PlayerState.Playing)
            configAndStartMediaPlayer();
    }

    @Override
    public void onLostAudioFocus(boolean canDuck) {
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (player != null && player.isPlaying())
            configAndStartMediaPlayer();

    }


    //Extend the Binder class
    public class ServiceBinder extends Binder{

        public MusicController getService(){
            //return a reference to this service
            return MusicController.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Pops up a message on the screen to show the service is started.
        Log.i(TAG, "onCreate: Service");

        player = MusicPlayer.getMusicPlayer();
        player.setContext(this);

        //get a referende to the notification manager

        //putNotification();
        mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(),this);

        registerBecomingNoisyReceiver();

    }

    //initialize the player to play a specific song
    public void startSong(long index) {

        Log.i(TAG, "startSong: Service");


        if (player != null) {

            player.start(index);

        }else {
            Log.i(TAG, "startSong: Service Null Player");
        }
    }

    @Override
    public void onDestroy() {

        Log.i(TAG, "onDestroy: Service");
        player.reset();
        player = null;
        //cancelNotification();

        super.onDestroy();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service: onStartCommand()");
        return START_NOT_STICKY;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {

        Toast.makeText(this, "MusicController Music Player Fehler", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onError: Music Player failed");

        if(mp!= null){
            try{
                mp.stop();
                mp.release();
                //cancelNotification();
            }
            finally {
                mp = null;
            }
        }
        return false;
    }

    public void play_pause(){
        if(player != null){
            Log.i(TAG, "Service: play_pause()");
            player.play_pause();
        }else{
            Log.i(TAG, "Service: play_pause() Null Player");
        }
    }

    public void resume(){
        if(player != null){
            Log.i(TAG, "Service: resume()");
            player.resume();
        }else{
            Log.i(TAG, "Service: resume() Null Player");
        }
    }

    public void pause(){
        if(player != null){
            Log.i(TAG, "Service: pause()");
            player.pause();
        }else{
            Log.i(TAG, "Service: pause() Null Player");
        }
    }

    public void rewind(){
        if(player != null){
            Log.i(TAG, "Service: rewind()");
            player.rewind();
        }else{
            Log.i(TAG, "Service: rewind() Null Player");
        }
    }

    public void previous() {
        if(player != null){
            Log.i(TAG, "Service: previous()");
            player.previous();
        }else{
            Log.i(TAG, "Service: previous() Null Player");
        }


    }

    public void forward(){
        if(player != null){
            Log.i(TAG, "Service: forward()");
            player.forward();
        }else{
            Log.i(TAG, "Service: forward() Null Player");
        }
    }

    public void next() {

        if(player != null){
            Log.i(TAG, "Service: next()");
            player.next();
        }else{
            Log.i(TAG, "Service: next() Null Player");
        }
    }

    public void shuffle() {
            Log.i(TAG, "Service: shuffle is activated. ");
            player.shuffle();
    }

    public void repeat() {
        if(player != null){
            Log.i(TAG, "Service: repeat is activated. ");
            player.repeat();
        }

    }


    public void stop(){
        if(player != null){
            Log.i(TAG, "Service: stop()");
            player.stop();
            //cancelNotification();
        }else{
            Log.i(TAG, "Service: stop() Null Player");
        }
    }

    public void reset(){
        if(player != null){
            Log.i(TAG, "Service: reset()");
            player.reset();
            //cancelNotification();
        }else{
            Log.i(TAG, "Service: reset() Null Player");
        }
    }

    public void reposition(int position){
        if(player != null){
            Log.i(TAG, "Service: reposition()");
            player.reposition(position);
        }else{
            Log.i(TAG, "Service: reposition() Null Player");
        }
    }


    void configAndStartMediaPlayer() {
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (player.isPlaying()) player.pause();
            return;
        }
        else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
            player.setVolume(0.1f, 0.1f);  // we'll be relatively quiet
        else
            player.setVolume(1.0f, 1.0f); // we can be loud

        if (!player.isPlaying()) player.restart();
    }

    private void putNotification(){
    }

    public void cancelNotification() {
        stopForeground(true);
    }

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio if user removes his headphones from device
            player.pause();
        }
    };
    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    /*private void putNotification(){
        Bitmap largeIcon;
        largeIcon = BitmapFactory.decodeResource(getResources(),R.mipmap.ic_play);
        largeIcon.createScaledBitmap(largeIcon,140,140,false);

        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setSmallIcon(0)
                        .setLargeIcon(largeIcon)
                        .setOngoing(true)
                        .setContentTitle("Music Player")
                        .setContentText(player.getSongTitle())
                        .setSubText(player.getSongArtist())
                        .setAutoCancel(true);

        // Create an explicit pending Intent to the Music UI activity(MusicActivity)
        Intent resultIntent = new Intent(this, MusicActivity.class);

        //create a pending intent so that when the user clicks on the notification, the intent is executed
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);

        //supply the pending intent to the notification
        mBuilder.setContentIntent(resultPendingIntent);

        Log.i(TAG, "Service: putNotification() ");

        startForeground(noteId, mBuilder.build());
    }*/


   /* public static void cancelNotification(Context ctx, int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancel(notifyId);
    }*/
}

