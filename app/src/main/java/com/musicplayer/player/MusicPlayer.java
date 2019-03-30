package com.musicplayer.player;

import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import android.media.AudioManager.OnAudioFocusChangeListener;

import com.musicplayer.R;
import com.musicplayer.service.MusicController;

import java.io.IOException;
import java.util.Observable;
import java.util.Random;

public class MusicPlayer extends Observable implements MediaPlayer.OnErrorListener{


    private static final String TAG = "Music Player";
    MediaPlayer player = null;
    private int position = 0;
    private long mSong = -1;
    private String mSongTitle = null;
    private String mSongArtist = null;
    private String mSongFile = null;
    private String albumUri = null;
    private int rewforwTime = 5000; //ms

    public boolean isShuffle = false;
    public boolean isRepeat = false;

    private int currentDuration, totalDuration;

    PlayerState mState = PlayerState.Reset;

    Context mContext;
    Random random;


    public static final MusicPlayer _instance = new MusicPlayer();

    private int noteId =1;
    private NotificationManager mNotificationManager;


    public MusicPlayer(){

    }

    public static synchronized MusicPlayer getMusicPlayer(){

        return _instance;

    }

    public void setContext(Context c){
        mContext = c;


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSong = prefs.getLong("SongID", -1);
        mSongTitle = prefs.getString("Song Title", null);
        mSongArtist = prefs.getString("Artist", null);
        mSongFile = prefs.getString("File Name", null);
        albumUri = prefs.getString("Album Art", "android.resource://com.musicplayer/" + R.mipmap.ic_play);
        if(mSongFile != null)
            start(-1);

        //get a reference to the notification manager
        //mNotificationManager = (NotificationManager)mContext.getSystemService(mContext.NOTIFICATION_SERVICE);

    }

    private void setState(PlayerState m){
        mState = m;
        setChanged();
        notifyObservers(mState);
    }

    public static long getAlbumId(){
        return Long.parseLong(MediaStore.Audio.Media.ALBUM_ID);
    }

    public boolean isPlaying(){
        if(player != null){
            return player.isPlaying();
        }else{
            return false;
        }
    }
    public boolean isStopped(){
        return mState == PlayerState.Paused;
    }
    public boolean isPaused(){
        return mState == PlayerState.Paused;

    }

    public void start(long song) {

        Log.i(TAG, "method start(long song) is started");

        if (mState == PlayerState.Reset) {

            if(song >= 0){
                getSongInfo(song);
            }

            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);

            try {
                player.setDataSource(mSongFile);
            }catch(IllegalArgumentException e){
                e.printStackTrace();
            }catch(SecurityException e){
                e.printStackTrace();
            }catch(IllegalStateException e){
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }

            player.setLooping(false);

            try {
                player.prepare();
            }catch (IllegalStateException e) {
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }

            totalDuration = player.getDuration();

            position = getPosition();
            player.seekTo(position);

            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    reset();
                    savePosition(0);
                    //after user selected song from PlaylistActivity ListViews, app starts song automatically
                    start(mSong);
                    if(!isShuffle && !isRepeat){
                        //play next song if shuffle and repeat are off
                        start(mSong + 1);
                        play_pause();
                    }else if(isShuffle){
                        //play random song if user has the shuffle button switched on
                        shuffle();
                    }else{
                        //if user switched repeat button on, same song will be played again
                        start(mSong);
                        play_pause();
                    }
                }
            });
            setState(PlayerState.Ready);

        } else {
            Log.i(TAG, "start: Wrong Player State");
        }
    }

    /**
     *
     * @param index: received from playlist indicating the id of the row from the MediaStore content
     *             provider
     */
    private void getSongInfo(long index) {

        int music_column_index;
        Cursor musiccursor;

        // The specific row and the columns that I wish to retrieve
        final String[] MUSIC_SUMMARY_PROJECTION = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA, //file handle
                MediaStore.Audio.Media.DISPLAY_NAME, //name of the music file
                MediaStore.Audio.Media.TITLE, //title of the song
                MediaStore.Audio.Media.ARTIST, //Artist's name
                MediaStore.Audio.Media.ALBUM_ID //album id to retrieve album art
        };

        Uri baseUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, index);
        String select = "((" + MediaStore.Audio.Media.DISPLAY_NAME + " NOTNULL) AND ("
                + MediaStore.Audio.Media.DATA + " NOTNULL) AND ("
                + MediaStore.Audio.Media.DISPLAY_NAME + " != '' ) AND ("
                + MediaStore.Audio.Media.IS_MUSIC + "!= 0))";

        musiccursor = mContext.getContentResolver().query(baseUri, MUSIC_SUMMARY_PROJECTION,
                select,null, MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED ASC");

        mSong = index;
        //necessary if condition here, otherwise app crashes if any new song will be played
        if (musiccursor != null) {
            musiccursor.moveToFirst();
            // 1.) get the title of the song
            music_column_index = musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            mSongTitle = musiccursor.getString(music_column_index);

            // 2.) get the artist's name and append to the song title
            music_column_index = musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            mSongArtist = musiccursor.getString(music_column_index);

            // 3.) get the file handle
            music_column_index = musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            mSongFile = musiccursor.getString(music_column_index);

            // 4.) get the album ID for displaying album art
            music_column_index = musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            long albumID = musiccursor.getLong(music_column_index);
            musiccursor.close();

            String[] projectionImages = new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART};
            Cursor c = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    projectionImages, MediaStore.Audio.Albums._ID + " = " + albumID,
                    null, null);
            if (c != null){
                c.moveToFirst();
                String coverPath = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART));
                c.close();

                if (coverPath == null) {
                    coverPath = "android.resource://com.musicplayer/" + R.mipmap.ic_play;
                }
                albumUri = coverPath;
            }
            Log.i(TAG, "Song Selected: " + mSong + " " + mSongFile + " " + mSongTitle + " " + " albumID: " + albumID);


            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor prefed = prefs.edit();
            prefed.putLong("SongID", mSong);
            prefed.putString("Song Title", mSongTitle);
            prefed.putString("Artist", mSongArtist);
            prefed.putString("File Name", mSongFile);
            prefed.putString("Album Art", albumUri);
            if(mSong != -1)
                prefed.putInt("Position", 0);
            prefed.commit();
        }
		}

    private int getPosition(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getInt("Position", 0);
    }

    public void play_pause(){
        Log.i(TAG, "start: Play_pause");

        if(mState == PlayerState.Paused || mState == PlayerState.Ready){
            resume();
        }else if(mState == PlayerState.Playing){
            pause();
        }else{
            Log.i(TAG, "play_pause: Wrong Player State ");
        }
    }

    public void pause(){
        Log.i(TAG, "Pause");

        if(mState == PlayerState.Playing){
            player.pause();
            position = player.getCurrentPosition();
            savePosition(position);
            setState(PlayerState.Paused);
        }else{
            Log.i(TAG, "pause:  Wrong Player State");
        }
    }

    public void reset() {
        if(player != null){
            Log.i(TAG, "Reset");
            position = player.getCurrentPosition();
            savePosition(position);
            player.stop();
            player.reset();
            player.release();
            position = 0;
            player = null;
            setState(PlayerState.Reset);
        }
    }

    public void forward() {
        Log.i(TAG, "Forward");

        if(mState == PlayerState.Playing){
            player.pause();
            position = player.getCurrentPosition();
            player.seekTo((position + rewforwTime) > totalDuration ? totalDuration-1000: (position+rewforwTime));
            player.start();
        }else{
            Log.i(TAG, "forward:  Wrong Player State");
        }
    }

    public void shuffle() {
        Log.i(TAG, "Shuffle started.");
        if(mState == PlayerState.Reset){
            Random random = new Random();
            mSong = random.nextInt(14426);//TODO GET THE .size() of list of all songs
            if(mSong > 5) {
                start(mSong);
                play_pause();
            }else{
                mSong = 255;
                start(mSong);
                play_pause();
            }
        }else{
            Log.i(TAG, "SHUFFLE:  Wrong Player State");
        }
    }

    public void rewind(){
        Log.i(TAG, "Rewind");
        if(mState == PlayerState.Playing){
            player.pause();
            position = player.getCurrentPosition();
            player.seekTo((position - rewforwTime) < 0 ? 0:(position-rewforwTime));
            player.start();
        }else{
            Log.i(TAG, "rewind: Wrong PLayer State");
        }
    }

    public void repeat() {
        Log.i(TAG, "MusicPlayer: repeat is activated.  ");
        start(mSong);
        play_pause();
    }

    public void resume() {
        Log.i(TAG, "resume: resume");

        if(mState == PlayerState.Paused || mState == PlayerState.Ready){
            player.seekTo(position);
            player.start();
            //putNotification(mContext,currentSongTitle(), currentSongArtist(),"");
            setState(PlayerState.Playing);
        }else
            Log.i(TAG, "resume: Wrong Player State");
    }

    public void putNotification(Context context,String title,String artist,String body){
        //TODO Notification
    }

    public void stop(){
        if(mState == PlayerState.Playing){
            Log.i(TAG, "Stop");
            position = player.getCurrentPosition();
            savePosition(position);
            player.stop();
            player.release();
            player = null;
            setState(PlayerState.Stopped);
        }
    }


    public void reposition(int value){
        Log.i(TAG, "Reposition" + value + "%");

        if(mState == PlayerState.Playing){
            pause();
            position = (int) ((double) value*totalDuration/100);
            resume();
        }else if (mState == PlayerState.Paused){
            pause();
            position = (int)((double) value*totalDuration/100);
            player.seekTo(position);
        }
    }

    public int progress(){
        Double percentage = (double) 0;

        if(mState == PlayerState.Reset)
            return 0;

        currentDuration = player.getCurrentPosition();

        long currentSeconds = (int) (currentDuration / 1000);
        long totalSeconds = (int) (totalDuration / 1000);

        //calculating percentage
        percentage = (((double)currentSeconds)/totalSeconds)*100;

        //return percentage
        return percentage.intValue();
    }

    public String completedTime(){
        return milliSecondsToTimer(currentDuration);
    }

    public String remainingTime(){
        return milliSecondsToTimer(currentDuration);
    }

    public String songDuration(){
        //TODO display whole song duration instead of remaining time
        return milliSecondsToTimer(totalDuration);
    }

    private String milliSecondsToTimer(long milliseconds){
        String finalTimerString = "";
        String secondsString = "";

        //Convert total duration into time
        int hours = (int)(milliseconds/(1000*60*60));
        int minutes = (int)(milliseconds % (1000*60*60))/(1000*60);
        int seconds = (int) ((milliseconds % (1000*60*60)) % (1000*60) /1000);
        //Add hours if there
        if(hours>0){
            finalTimerString = hours + ":";
        }

        //Prepending 0 to seconds if it is one digit
        if(seconds < 10){
            secondsString = "0" + seconds;
        }else{
            secondsString = "" + seconds;

        }
        finalTimerString = finalTimerString + minutes + ":" + secondsString;
        //return Timerstring
        return finalTimerString;
    }

    public void restart() {
        Log.i(TAG, "Restart");

        // if(mState ==PlayerState.Reset){
    }

    private void savePosition(int pos){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor prefed = prefs.edit();
        prefed.putInt("Position", pos);
        prefed.apply();
    }

    public String getSongTitle() { return mSongTitle; }

    public String getSongArtist(){ return mSongArtist; }

    public String currentSongTitle(){
        return mSongTitle;
    }


    public Uri albumUri(){
        return (albumUri == null) ? Uri.parse("android.resource://com.musicplayer/" + R.mipmap.ic_play)
                : Uri.parse(albumUri);
    }

    public void next() {
        if (mState == PlayerState.Playing || mState == PlayerState.Ready) {
            if(isShuffle) {
                reset();
                shuffle();
                setState(PlayerState.Playing);
						} else if(isRepeat){
                reset();
                start(mSong);
                player.start();
                setState(PlayerState.Playing);

            }else{
                reset();
                start(mSong + 1);
                Log.i(TAG, "next: Play song No." + mSong + " now");
                player.start();
                setState(PlayerState.Playing);
            }
        }else
            Log.i(TAG, "next: PLAYER WRONG STATE");				 
    }

    public void previous() {

        if(mState == PlayerState.Playing || mState == PlayerState.Ready){
            reset();
            start(mSong - 1 );
            play_pause();
        }else{
            Log.i(TAG, "MusicPlayer: previous() WRONG PLAYER STATE ");
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(mContext, "MusicPlayer Fehler", Toast.LENGTH_SHORT).show();
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

    public void setVolume(float v, float v1) {
        player.setVolume(v,v1);
    }


    //Ende------------------------------------------------------------------------------------------


}
