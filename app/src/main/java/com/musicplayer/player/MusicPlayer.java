package com.musicplayer.player;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.musicplayer.R;

import java.io.IOException;
import java.util.Observable;
import java.util.Random;

public class MusicPlayer extends Observable implements MediaPlayer.OnErrorListener {

    // Enables/Disables Logcat text for debugging
    private boolean D = true;
    private static final String TAG = "Music Player";
    private MediaPlayer player = null;
    private int position = 0;
    private long mSong = -1;
    private String mSongTitle = null;
    private String mSongArtist = null;
    private String mAlbumName = null;
    private String mSongFile = null;
    public String albumUri = null;
    private long albumID;

    private int rewforwTime = 5000; //ms

    public boolean isShuffle = false;
    public boolean isRepeat = false;

    private int currentDuration, totalDuration;

    private PlayerState mState = PlayerState.Reset;
    private NextState nxState = NextState.Next;

    private Context mContext;
    private Random random,mRandom;


    private static final MusicPlayer _instance = new MusicPlayer();

    public static boolean T = false;



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
        mAlbumName = prefs.getString("Album Name",null);
        mSongFile = prefs.getString("File Name", null);
        albumUri = prefs.getString("Album Art", "android.resource://com.musicplayer/" + R.mipmap.ic_play);
        albumID = prefs.getLong("Album ID",0);
        if(mSongFile != null)
            start(-1);

        //get a reference to the notification manager, this is deprecated for API > 26
        //mNotificationManager = (NotificationManager)mContext.getSystemService(mContext.NOTIFICATION_SERVICE);

    }

    private void setState(PlayerState m){
        mState = m;
        setChanged();
        notifyObservers(mState);
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

        if(D)Log.i(TAG, "method start(long song) is started");

        if (mState == PlayerState.Reset) {

            if (song >= 0) {
                getSongInfo(song);
            }

            player = new MediaPlayer();

            if (android.os.Build.VERSION.SDK_INT < 26) {
                // setAudioStreamType deprecated since v26:
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            } else {
                // we use audioattributes instead for API > 26:
                AudioAttributes attr = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                player.setAudioAttributes(attr);
            }
            try {
                player.setDataSource(mSongFile);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            player.setLooping(true);

            try {
                player.prepare();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
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
                    next();
                }
            });
            setState(PlayerState.Ready);
        }
    }


    public void play_pause(){
        if(D)Log.i(TAG, "MusicPlayer: play_pause started");
        if(mState == PlayerState.Paused || mState == PlayerState.Ready){
            resume();
        }else if(mState == PlayerState.Playing){
            pause();
        }else{
            if(D)Log.i(TAG, "play_pause: Wrong Player State ");
        }
    }

    public void pause(){
        if(D)Log.i(TAG, "Pause");

        if(mState == PlayerState.Playing){
            player.pause();
            position = player.getCurrentPosition();
            savePosition(position);
            setState(PlayerState.Paused);
        }else{
            if(D)Log.i(TAG, "pause:  Wrong Player State");
        }
    }

    public void reset() {
        if(player != null){
            if(D)Log.i(TAG, "reset() is started. MediaPlayer is reset now.");
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
        if(D)Log.i(TAG, "Forward");

        if(mState == PlayerState.Playing){
            player.pause();
            position = player.getCurrentPosition();
            player.seekTo((position + rewforwTime) > totalDuration ? totalDuration-1000: (position+rewforwTime));
            player.start();
        }else{
            if(D)Log.i(TAG, "forward:  Wrong Player State");
        }
    }

    public void shuffle() {
        if(D)Log.i(TAG, "Shuffle started.");
        random = new Random();
        int newSong = 0;

        while(newSong == 0 ){
            newSong = random.nextInt(14300 - 1) + 1;
            if (newSong > 14300)newSong -= 14300;

            if(String.valueOf(newSong).contains("0")){
                newSong += 353;
            }else if(String.valueOf(newSong).contains("1")){
                newSong +=485;
            }else if(String.valueOf(newSong).contains("2")){
                newSong *=2;
            }else if(String.valueOf(newSong).contains("3")){
                newSong +=1450;
            }else if(String.valueOf(newSong).contains("4")){
                newSong %= 35;
            }else if(String.valueOf(newSong).contains("5")){
                newSong /= (newSong + 252);
            }else newSong += 2457;
        }
        start(newSong);
    }

    public void rewind(){
        if(D)Log.i(TAG, "Rewind");
        if(mState == PlayerState.Playing){
            player.pause();
            position = player.getCurrentPosition();
            player.seekTo((position - rewforwTime) < 0 ? 0:(position-rewforwTime));
            player.start();
        }else{
            if(D)Log.i(TAG, "rewind: Wrong PLayer State");
        }
    }

    public void repeat() {
        if(D)Log.i(TAG, "MusicPlayer: repeat is activated.  ");
        start(mSong);
    }

    public void resume() {
        if(D)Log.i(TAG, "resume: resume");

        if(mState == PlayerState.Paused || mState == PlayerState.Ready){
            player.seekTo(position);
            player.start();
            //putNotification(mContext,currentSongTitle(), currentSongArtist(),"");
            setState(PlayerState.Playing);
        }else
        if(D)Log.i(TAG, "resume: Wrong Player State");
    }

    public void stop(){
        if(mState == PlayerState.Playing){
            if(D)Log.i(TAG, "MediaPlayer was Stopped: PlayerState = " + mState);
            position = player.getCurrentPosition();
            savePosition(position);
            player.stop();
            player.release();
            player = null;
            setState(PlayerState.Stopped);
        }
    }

    public void reposition(int value){
        if(D)Log.i(TAG, "Reposition" + value + "%");

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
        if(D)Log.i(TAG, "Restart");

        // if(mState ==PlayerState.Reset){
    }

    private void savePosition(int pos){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor prefed = prefs.edit();
        prefed.putInt("Position", pos);
        prefed.apply();
    }

    public Uri albumUri(){
        return (albumUri == null) ? Uri.parse("android.resource://com.musicplayer/" + R.mipmap.ic_play)
                : Uri.parse(albumUri);
    }

    public void next() {
        if(T) Toast.makeText(mContext, "PlayerState = " + mState,Toast.LENGTH_LONG).show();
            if(mState != PlayerState.Reset) {
                reset();
                savePosition(0);
            }
            if(isShuffle){
                shuffle();
            }else if(isRepeat){
                start(mSong);
            }else{
                start(mSong + 1);
                player.setLooping(true);
                if(D)Log.i(TAG, "next: Play song No." + mSong + " now");
            }
            play_pause();
    }


    public void previous() {
        if (mState == PlayerState.Playing || mState == PlayerState.Ready) {
            if(isShuffle){
                reset();
                savePosition(0);
                mSong = 0;
                shuffle();
            } else if(isRepeat){
                reset();
                start(mSong);
            }else{
                reset();
                start(mSong - 1);
                if(D)Log.i(TAG, "next: Play song No." + mSong + " now");
            }
            play_pause();
        }else if(mState == PlayerState.Reset || mState == PlayerState.Paused) {
            reset();
            start(mSong - 1);
            play_pause();
        }else
        if(D)Log.i(TAG, "next: PLAYER WRONG STATE");
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
                MediaStore.Audio.Media.ARTIST, //artist's name
                MediaStore.Audio.Media.ALBUM_ID, //album id to retrieve album art
                MediaStore.Audio.Albums.ALBUM //album name
        };

        Uri baseUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, index);
        String select = "((" + MediaStore.Audio.Media.DISPLAY_NAME + " NOTNULL) AND ("
                + MediaStore.Audio.Media.DATA + " NOTNULL) AND ("
                + MediaStore.Audio.Media.DISPLAY_NAME + " != '' ) AND ("
                + MediaStore.Audio.Media.IS_MUSIC + "!= 0))";

        musiccursor = mContext.getContentResolver().query(baseUri, MUSIC_SUMMARY_PROJECTION,
                select,null, MediaStore.Audio.Media.DISPLAY_NAME + " COLLATE NOCASE ASC");

        mSong = index;
        //necessary if condition here, otherwise app crashes if any new song will be played
        if (musiccursor != null && musiccursor.getCount() != 0) {
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
            albumID = musiccursor.getLong(music_column_index);

            //5. get the album name for displaying it in notification
            music_column_index = musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
            mAlbumName = musiccursor.getString(music_column_index);
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
            if(D)Log.i(TAG, "Song Selected: " + mSong + " " + mSongFile + " " + mSongTitle + " " + " albumID: " + albumID);


            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor prefed = prefs.edit();
            prefed.putLong("SongID", mSong);
            prefed.putString("Song Title", mSongTitle);
            prefed.putString("Artist", mSongArtist);
            prefed.putString("Album Name",mAlbumName);
            prefed.putString("File Name", mSongFile);
            prefed.putString("Album Art", albumUri);
            if(mSong != -1)
                prefed.putInt("Position", 0);
            prefed.apply();
        }
    }

    private int getPosition(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getInt("Position", 0);
    }

    public String getSongTitle() { return mSongTitle; }

    public long getSongId() {return mSong;}

    public String getSongArtist(){ return mSongArtist; }

    public String getAlbumName() {return mAlbumName;
    }
    public long getAlbumId() {
        return albumID;
    }

    public boolean getShuffle(){
        return isShuffle;
    }
    public void setShuffle(boolean shuffle){
        if(D){
            if(D)Log.d(TAG, "MusicPlayer: setShuffle(): shuffle changed to " + shuffle);
        };
        isShuffle = shuffle;
    }
    public boolean getRepeat(){
        return isRepeat;
    }
    public void setRepeat(boolean repeat){
        isRepeat = repeat;
    }

    public String currentSongTitle(){
        return mSongTitle;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(mContext, "MusicPlayer Fehler", Toast.LENGTH_SHORT).show();
        if(D) Log.i(TAG, "onError: Music Player failed");

        if(mp!= null){
            try{
                mp.stop();
                mp.release();
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




    //---------------------------End(e)---------------------------------------------------------------


}