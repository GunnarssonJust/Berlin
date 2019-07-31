package com.musicplayer.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.musicplayer.R;
import com.musicplayer.models.Audio;
import com.musicplayer.player.MusicPlayer;
import com.musicplayer.player.PlayerState;
import com.musicplayer.receiver.NoisyAudioStreamReceiver;
import com.musicplayer.utils.AudioFocusHelper;
import com.musicplayer.utils.Constants;
import com.musicplayer.utils.NavigationUtils;
import com.musicplayer.widgets.BigWidget;

import java.util.ArrayList;

import static com.musicplayer.utils.Constants.ACTION_NEXT;
import static com.musicplayer.utils.Constants.ACTION_PAUSE;
import static com.musicplayer.utils.Constants.ACTION_PLAY;
import static com.musicplayer.utils.Constants.ACTION_PLAY_PAUSE;
import static com.musicplayer.utils.Constants.ACTION_PREVIOUS;
import static com.musicplayer.utils.Constants.ACTION_REPEAT;
import static com.musicplayer.utils.Constants.ACTION_SHUFFLE;
import static com.musicplayer.utils.Constants.ACTION_STOP;

/* Class Name: MusicController
        *
        * This class implements support for playing a music file using the MediaPlayer class in Android.
        * It supports the following methods:
        *
        * play_pause() toggles the player between playing and paused states
        * resume(): resume playing the current song
        * pause(): pause the currently playing song
        * rewind(): rewind the currently playing song by one step
        * forward(): forward the currently playing song by one step
        * stop(): stop the currently playing song
        * reset(): reset the music player and release the MusicPlayer associated with it
        * reposition(value): repositions the playing position of the song to value% and resumes playing

         This class allows the app to play music in background
*/

public class MusicController extends Service implements MediaPlayer.OnErrorListener{

    //boolean D for enabling/disabling LOG TAGS
    boolean D = true;

    private static final String TAG = "MusicController";

    public static final String UPDATE_PREFERENCES = "updatepreferences";
    public static final String SERVICECMD = "com.musicplayer.musicservicecommand";

    private NotificationManagerCompat mNotificationManager;
    private NotificationManager notificationManager;

    private int noteId = 150;
    MusicPlayer player = null;

    private Context mContext;
    private PowerManager.WakeLock mWakeLock;
    private static long songIndex = 0;

    String CHANNEL_ID = "my_channel_01";

    AudioManager mAudioManager;
    public MediaSessionCompat mMediaSession;
    private AudioFocusHelper mAudioFocusHelper;

    NoisyAudioStreamReceiver mNoisyAudioStreamReceiver;
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            player.pause();
        }
    };

    PlayerState mState;

    // the Binder that will be returned on call to onBind()
    private final IBinder mBinder = new ServiceBinder();

    // handle incoming phone calls
    private boolean ongoingCall = false;
    private boolean onAudioFocus = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private ArrayList<Audio> audioList;
    
    // constructing audiofocus helper object
    public MusicController(AudioFocusHelper mAudioFocusHelper){
        super();
        this.mAudioFocusHelper = mAudioFocusHelper;
    }


    // empty Constructor

    public MusicController(){}

    //Extend the Binder class
    public class ServiceBinder extends Binder {

        public MusicController getService() {
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
        // Pops up a message on the log screen to show the service is started.
        if(D)Log.i(TAG, "onCreate: Service");

        mContext = MusicController.this;

        //loading all audio files from device into an ArrayList<Audio> object
        //loadAudio();

        // The music player is implemented as a Java Singleton class so that only one
        // instance of the player is present within the application. The getMusicPlayer()
        // method returns the reference to the instance of the music player class
        // get a reference to the instance of the music player
        // set the context for the music player to be this service

        player = MusicPlayer.getMusicPlayer();
        player.setContext(this);

        mMediaSession = new MediaSessionCompat(mContext, TAG);
        //get access to the notification manager
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNoisyAudioStreamReceiver = new NoisyAudioStreamReceiver();

        // Initialize the intent filter and each action
        final IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICECMD);
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_STOP);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_REPEAT);
        filter.addAction(ACTION_SHUFFLE);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        filter.addAction(Intent.ACTION_SCREEN_ON);

        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );
        mMediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                resume();
                putNotification();
                if(D)Log.d(TAG, "onPlay: putNotification");
            }

            @Override
            public void onPause() {player.pause();}

            @Override
            public void onSkipToNext() {
                next();
                updateNotification();
            }

            @Override
            public void onSkipToPrevious() {
                previous();
                updateNotification();
            }
        });

        Bitmap largeIcon;
        if (player.albumUri == null) {
            largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_play_pressed);
        } else {
            largeIcon = BitmapFactory.decodeFile(player.albumUri);
        };

        mMediaSession.setMetadata(new MediaMetadataCompat.Builder()
            .putBitmap(MediaMetadata.METADATA_KEY_ART,largeIcon)
            .putString(MediaMetadata.METADATA_KEY_ARTIST,player.getSongArtist())
            .putString(MediaMetadata.METADATA_KEY_ALBUM,player.getAlbumName())
            .putString(MediaMetadata.METADATA_KEY_TITLE,player.getSongTitle())
            .build());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "Main", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
        }

        // get access to the AudioManager
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // switches player off if user removes earphones from the device
        registerBecomingNoisyReceiver();

        // handle incoming phone calls please
        callStateListener();
    }


    //initialize the player to play a specific song
    public void startSong(long index) {
        int result = mAudioManager.requestAudioFocus(focusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        // wait until You get focus of the audio stream
        while(result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED);

        if(D)Log.i(TAG, "startSong: Service");

        if (player != null) {
            songIndex = index;
            player.start(index);
        } else {
            if(D)Log.i(TAG, "startSong: Service Null Player");
        }
    }

    @Override
    public void onDestroy() {

        //unregister the NoisyReceiver (Receiver disables player if user removes headphones)
        unregisterNoisyReceiver();
        //disable call state listener (listener for incoming calls)
        disableCallStateListener();

        if(D)Log.i(TAG, "onDestroy: Service");
        cancelNotification();
        updateWidget();
        player.reset();
        player = null;
        mMediaSession.release();
        super.onDestroy();
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(D)Log.i(TAG, "Service: onStartCommand()");

        // This requests AudioFocus everytime Service will start, for phone calls
        int result = mAudioManager.requestAudioFocus(focusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        // Requests audiofocus for any other audio like skype or whatsapp calls
        /*int result2 = mAudioManager.requestAudioFocus(whatsappListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN );*/
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Any time startService() is called, the intent is delivered here and can be handled.
            handleIntent(intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    // handle the intent delivered to onStartCommand().
    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;

        // get the action specified in the intent. The actions are given in Constants.java.
        String action = intent.getAction();

        if (action.equalsIgnoreCase(Constants.ACTION_PLAY_PAUSE)) {
            player.play_pause();
        } else if (action.equalsIgnoreCase(ACTION_PLAY)) {
            player.resume();
        } else if (action.equalsIgnoreCase(Constants.ACTION_PAUSE)) {
            pause();
        } else if (action.equalsIgnoreCase(Constants.ACTION_FORWARD)) {
            forward();
        } else if (action.equalsIgnoreCase(Constants.ACTION_REWIND)) {
            rewind();
        } else if (action.equalsIgnoreCase(Constants.ACTION_PREVIOUS)) {
            previous();
        } else if (action.equalsIgnoreCase(Constants.ACTION_NEXT)) {
            next();
        } else if (action.equalsIgnoreCase(Constants.ACTION_STOP)) {
            stop();
        } else if (action.equalsIgnoreCase(Constants.ACTION_RESET)) {
            reset();
        } else if (action.equalsIgnoreCase(Constants.ACTION_SONG)) {
            reset();
            long id = intent.getLongExtra("Song", 0);
            startSong(id);
        } else if (action.equalsIgnoreCase(Constants.ACTION_REPOSITION)) {
            int position = intent.getIntExtra("Position", 0);
            reposition(position);
        } else if (action.equalsIgnoreCase(Constants.ACTION_COMPLETED)) {
            reset();
            startSong(songIndex);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {

        Toast.makeText(this, "MusicController Music Player Fehler", Toast.LENGTH_SHORT).show();
        if(D)Log.i(TAG, "onError: Music Player failed");

        if (mp != null) {
            try {
                mp.stop();
                mp.release();
                mMediaSession.release();
                cancelNotification();updateWidget();
                unregisterNoisyReceiver();
                updateWidget();
                stopSelf();

            } finally {
                mp = null;
            }
        }
        return false;
    }

    public void play_pause() {
        if (player != null) {
            if(D)Log.i(TAG, "Service: play_pause()");
            player.play_pause();
        } else {
            if(D)Log.i(TAG, "Service: play_pause() Null Player");
        }
    }

    public void resume() {
        if (player != null) {
            if(D)Log.i(TAG, "Service: resume()");
            player.resume();
            // update the pause button in the notification to pause button
        } else {
            if(D)Log.i(TAG, "Service: resume() Null Player");
        }
    }

    public void pause() {
        if (player != null) {
            if(D)Log.i(TAG, "Service: pause()");
            player.pause();
            mAudioManager.abandonAudioFocus((AudioManager.OnAudioFocusChangeListener) this);
            // update the pause button in the notification to pause button
        } else {
            if(D)Log.i(TAG, "Service: pause() Null Player");
        }
    }

    public void rewind() {
        if (player != null) {
            if(D)Log.i(TAG, "Service: rewind()");
            player.rewind();
        } else {
            if(D)Log.i(TAG, "Service: rewind() Null Player");
        }
    }

    public void previous() {
        if (player != null) {
            if(D)Log.i(TAG, "Service: previous()");
            player.previous();
        } else {
            if(D)Log.i(TAG, "Service: previous() Null Player");
        }


    }

    public void forward() {
        if (player != null) {
            if(D)Log.i(TAG, "Service: forward()");
            player.forward();
        } else {
            if(D)Log.i(TAG, "Service: forward() Null Player");
        }
    }

    public void next() {
        if (player != null) {
            if(D)Log.i(TAG, "Service: next()");
            player.next();
        } else {
            if(D) Log.i(TAG, "Service: next() Null Player");
        }
    }

    public void shuffle() {
        if(D)Log.i(TAG, "Service: shuffle is activated. ");
        player.shuffle();
    }

    public void repeat() {
        if (player != null) {
            if(D)Log.i(TAG, "Service: repeat is activated. ");
            player.repeat();
        }
    }

    public void stop() {
        if(player != null){
            if(D) Log.i(TAG, "Service: stop()");
            player.stop();
            mMediaSession.release();
            cancelNotification();
        }else{
            if(D) Log.i(TAG, "Service: stop() Null Player");
        }
    }

    public void reset() {
        if (player != null) {
            if(D)Log.i(TAG, "Service: reset()");
            // abandon focus of the audio stream
            //while(!abandonFocus());
            player.reset();
        } else {
            if(D)Log.i(TAG, "Service: reset() Null Player");
        }
    }

    public void reposition(int position) {
        if (player != null) {
            if(D) Log.i(TAG, "Service: reposition()");
            player.reposition(position);
        } else {
            if(D)Log.i(TAG, "Service: reposition() Null Player");
        }
    }



    // put the notification into the notification bar. This method is called when the song is first
    // initialized. It will be updated automatically with control buttons by updateNotification().
    public void putNotification() {

        if(D) Log.i(TAG, "putNotification(): Service puts notification on");

        Intent nowPlayingIntent = NavigationUtils.getNowPlayingIntent(this);
        PendingIntent clickIntent = PendingIntent.getActivity(this, 0, nowPlayingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // set current album art to integrated notification image
        Bitmap largeIcon;
        int playButtonResId = player.isPlaying() ? R.drawable.ic_notif_pause : R.drawable.ic_notif_play;
        if (player.albumUri == null) {
            largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_play_pressed);
        } else {
            largeIcon = BitmapFactory.decodeFile(player.albumUri);
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setPriority(0)
                        .setSmallIcon(R.mipmap.ic_play)
                        .setLargeIcon(largeIcon)
                        .setContentIntent(clickIntent)
                        .setContentTitle(player.getSongArtist())
                        .setContentText(player.getSongTitle())
                        .setSubText(player.getAlbumName())
                        .setShowWhen(false)
                        .addAction(R.drawable.ic_notif_backward, "", retrievePlaybackAction(ACTION_PREVIOUS))
                        .addAction(playButtonResId, "", retrievePlaybackAction(ACTION_PLAY_PAUSE))
                        .addAction(R.drawable.ic_notif_forward, "", retrievePlaybackAction(ACTION_NEXT));

        android.support.v4.media.app.NotificationCompat.MediaStyle style =
                new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mMediaSession.getSessionToken())
                        .setShowActionsInCompactView(0,1,2,3);
        mBuilder.setStyle(style);

        //noteId allows You to update the notification later on.
        //set the service as a foreground service
        startForeground(noteId, mBuilder.build());

    }

    // updatenotification() updates the information in the notification and adds the player
    // control buttons
    public void updateNotification() {

        if(D)Log.i(TAG, "Service: updateNotification()");
        //opens App's UI if user clicks on notification
        Intent nowPlayingIntent = NavigationUtils.getNowPlayingIntent(this);
        PendingIntent clickIntent = PendingIntent.getActivity(this, 0, nowPlayingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // update album art at notification image
        Bitmap largeIcon;
        final boolean isPlaying = player.isPlaying();
        int playButtonResId = isPlaying ? R.drawable.ic_notif_pause : R.drawable.ic_notif_play;

        if (player.albumUri == null) {
            largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_play_pressed);
        } else {
            largeIcon = BitmapFactory.decodeFile(player.albumUri);
        }

        NotificationCompat.Builder mBuilder;
        mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setPriority(0)
                .setContentIntent(clickIntent)
                .setSmallIcon(R.mipmap.ic_play)
                .setLargeIcon(largeIcon)
                .setContentTitle(player.getSongArtist())
                .setContentText(player.getSongTitle())
                .setSubText(player.getAlbumName())
                .setShowWhen(false)
                .addAction(R.drawable.ic_notif_backward, "", retrievePlaybackAction(ACTION_PREVIOUS))
                .addAction(playButtonResId, "", retrievePlaybackAction(ACTION_PLAY_PAUSE))
                .addAction(R.drawable.ic_notif_forward, "", retrievePlaybackAction(ACTION_NEXT));

        //MediaStyle class written by Google as default NOTIFICATION for Android devices
        android.support.v4.media.app.NotificationCompat.MediaStyle style =
                new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mMediaSession.getSessionToken())
                        .setShowActionsInCompactView(0,1,2,3);
        mBuilder.setStyle(style);

        // noteId allows you to update the notification with this ID later on.
        notificationManager.notify(noteId, mBuilder.build());
    }


    public void cancelNotification() {
        notificationManager.cancel(noteId);
        notificationManager.cancelAll();
    }

    public void updateWidget() {

        if (D) Log.d(TAG, "updateWidget: Widget will be updated.");
        AppWidgetManager manager = AppWidgetManager.getInstance(this);

        Intent to_widget = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        if (player != null) {

            RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.widget_layout_big);
            Bitmap largeIcon;
            boolean playing = player.isPlaying();

            // set albumart, if file has no albumart, default app icon is shown


            if (player.albumUri == null) {
                largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_play);
            } else {
                largeIcon = BitmapFactory.decodeFile(player.albumUri);
            }


            int[] ids = manager.getAppWidgetIds(
                    new ComponentName(mContext, BigWidget.class));

            //send the id to widget, so it is noted by homescreen app
            to_widget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

            //updating the music file's metadata
            remoteViews.setTextViewText(R.id.widget_big_SongName, player.getSongTitle());
            remoteViews.setTextViewText(R.id.widget_big_artist, player.getSongArtist());

            remoteViews.setImageViewBitmap(R.id.widget_big_albumart, largeIcon);

            //changes player button play/pause
            if(playing){
                remoteViews.setImageViewResource(R.id.btnPlay,R.drawable.ic_notif_pause);
                remoteViews.setViewVisibility(R.id.relLayout_widget_big_pink, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.relLayout_widget_big_black,View.INVISIBLE);
            }else{
                remoteViews.setImageViewResource(R.id.btnPlay,R.drawable.ic_notif_play);
                remoteViews.setViewVisibility(R.id.relLayout_widget_big_pink,View.INVISIBLE);
                remoteViews.setViewVisibility(R.id.relLayout_widget_big_black,View.VISIBLE);
                remoteViews.setTextViewText(R.id.widget_big_SongName_black,player.getSongTitle());
                remoteViews.setTextViewText(R.id.widget_big_artist_black,player.getSongArtist());
                remoteViews.setImageViewBitmap(R.id.widget_big_albumart, largeIcon);
            }
            manager.updateAppWidget(ids, remoteViews);
        }
    }


    // callback method invoked when any change in audio focus is detected
    // written by Reto Meier, Professionelle Android App-Entwicklung, Seite 863f.
    // if condition in AUDIOFOCUS_GAIN taken from Mitch Tabian's "Spotify Clone App"
    private AudioManager.OnAudioFocusChangeListener focusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    AudioManager audioManager =
                            (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                    switch (focusChange) {
                        case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK):
                            //reduce volume while other apps gain focus for a short time
                            player.setVolume(0.2f, 0.2f);
                            break;

                        case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT):
                            if(player.isPlaying()){
                                player.pause();
                                updateNotification();
                                updateWidget();
                            }
                            onAudioFocus = true;
                            break;

                        case (AudioManager.AUDIOFOCUS_LOSS):
                            mMediaSession.setActive(false);
                            mMediaSession.release();
                            audioManager.abandonAudioFocus(this);
                            unregisterNoisyReceiver();
                            cancelNotification();
                            updateWidget();
                            stop();
                            onAudioFocus = false;
                            break;

                        case(AudioManager.MODE_IN_CALL):
                        case(AudioManager.MODE_IN_COMMUNICATION):
                            if(player!=null)player.play_pause();
                            mMediaSession.setActive(false);
                            updateWidget();
                            updateNotification();
                            break;

                        case(AudioManager.MODE_NORMAL):
                            if(player != null)player.resume();
                            mMediaSession.setActive(true);
                            updateWidget();
                            updateNotification();
                            break;

                        case (AudioManager.AUDIOFOCUS_GAIN):
                            //set volume back and continue playing the song
                            if(onAudioFocus && !player.isPlaying()) {
                                player.setVolume(1.0f, 1.0f);
                                mMediaSession.setActive(true);
                                updateNotification();
                                updateWidget();
                                player.play_pause();
                            }else if(player.isPlaying()){
                                player.setVolume(1.0f,1.0f);
                            }
                            onAudioFocus = false;
                            break;

                        default:
                            break;
                    }
                }
            };


    private AudioManager.OnAudioFocusChangeListener whatsappListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int whatsappfocusChange) {
                    AudioManager whatsappManager =
                            (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                    switch (whatsappfocusChange) {
                        case(AudioManager.MODE_IN_CALL):
                        case(AudioManager.MODE_IN_COMMUNICATION):
                        case(AudioManager.MODE_RINGTONE):
                            if(player!=null)player.play_pause();
                            break;
                        case(AudioManager.MODE_NORMAL):
                            if(player != null)player.resume();
                    }
                }
            };

    private void registerBecomingNoisyReceiver() {
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    public void unregisterNoisyReceiver() {
        if(becomingNoisyReceiver != null) {
            unregisterReceiver(becomingNoisyReceiver);
            becomingNoisyReceiver = null;
            cancelNotification();
        }
    }


   /// method to save all audio files in an array, these tensors can be read by service class
    private void loadAudio(){
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri,null,selection,null,sortOrder);

        if (cursor!= null && cursor.getCount() > 0){
            audioList = new ArrayList<>();
            while(cursor.moveToNext()){
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                //while loop saves all strings into ArrayList "audioList"
                audioList.add(new Audio(data,title,album,artist));
            }
            cursor.close();
        }
    }


    private PendingIntent retrievePlaybackAction(final String action) {
        final ComponentName serviceName = new ComponentName(this, MusicController.class);
        Intent intent = new Intent(action);
        intent.setComponent(serviceName);
        return PendingIntent.getService(this, 0, intent, 0);
    }


    //Handle incoming phone calls, stops player in case of a call
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (player != null) {
                            pause();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (player != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resume();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void disableCallStateListener() {
        telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_NONE);
    }
}


