package com.musicplayer.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.musicplayer.R;
import com.musicplayer.player.MusicPlayer;
import com.musicplayer.player.PlayerState;
import com.musicplayer.service.MusicController;

import java.util.Observable;
import java.util.Observer;

//imported from hkust.comp4521.audio;

public class MusicActivity extends AppCompatActivity implements View.OnClickListener , SeekBar.OnSeekBarChangeListener, Observer {
    private int STORAGE_PERMISSION_CODE= 1;

    private static final String TAG = "MusicActivity";
    private ImageView playerButton, rewindButton, forwardButton, pauseButton, btnPlaylist;
    private ImageView albumArt, shuffleButton, repeatButton;;
    public static Handler handler;
    private TextView songTitleText,songTitleArtist;
    private Context mContext = MusicActivity.this;

    /*
     * Class Name: MusicController
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
     *
     * Class Name: MusicPlayer
     *
     * progress(): returns the percentage of the playback completed, useful to update the progressbar
     * comptletedTime(): Amount of the song time completed playing
     * remainingTime(): Remaining time of the song being played, not used in this app
     *
     *
     * You should use these methods to manage the playing of the song.
     */
    public MusicPlayer player;
    private SeekBar songProgressBar;
    private TextView complTime, remTime;

    private boolean isShuffle = false;
    private boolean isRepeat = false;

    //indicates if the activity is bound to the MusicController Service
    private boolean mIsBound = false;
    private MusicController mCont;
    private GestureDetector mDetector;
    private ServiceConnection Scon = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mCont = ((MusicController.ServiceBinder) binder).getService();
            mIsBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCont = null;
        }
    };

    void doBindService(){
        Intent in = new Intent(this, MusicController.class);
        bindService(in,Scon, Context.BIND_AUTO_CREATE);
    }

    void doUnBindServie(){
        if(mIsBound){
            unbindService(Scon);
            mIsBound = false;
        }
    }

    public void setSongTitle(String title) {
        songTitleText.setText(title);
    }
    public void setSongArtist(String artist) {
        songTitleArtist.setText(artist);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Create a layout for the Activity with four buttons:
        //Rewind,Pause, Play, and Forward and set it to the view of this activity
        setContentView(R.layout.main);

        Log.i(TAG, "Activity: onCreate()");

        //Get the references to the buttons from the layout of the activity
        playerButton = findViewById(R.id.play);
        playerButton.setOnClickListener(this);

        rewindButton = findViewById(R.id.rewind);
        rewindButton.setOnClickListener(this);

        forwardButton = findViewById(R.id.forward);
        forwardButton.setOnClickListener(this);

        shuffleButton = findViewById(R.id.btn_shuffle);
        shuffleButton.setOnClickListener(this);

        repeatButton = findViewById(R.id.btn_repeat);
        repeatButton.setOnClickListener(this);

        //get a reference to the song title TextView in the UI
        songTitleText = findViewById(R.id.songTitle);
        songTitleArtist = findViewById(R.id.songArtist);
        albumArt = findViewById(R.id.album_image);


        //get reference to the seekbar , completion time and remaining time textviews
        songProgressBar = findViewById(R.id.songProgressBar);
        songProgressBar.setMax(100);
        songProgressBar.setOnSeekBarChangeListener(this);
        complTime = findViewById(R.id.songCurrentDurationLabel);
        remTime = findViewById(R.id.songRemainingDurationLabel);

        //If the song title is longer than the given layout_width, animation moves title right to left
        // new TranslateAnimation (float fromXDelta,float toXDelta, float fromYDelta, float toYDelta)




        // The music player is implemented as a Java Singleton class so that only one
        // instance of the player is present within the application. The getMusicPlayer()
        // method returns the reference to the instance of the music player class

        // 1. get a reference to the instance of the music player
        // 2. set the context for the music player to be this activity
        // 3. add this activity as an observer
        player = MusicPlayer.getMusicPlayer();
        player.addObserver(this);

        // For API 23 and later Developers need to include a runtime permission request
        // for sd_card AND internal storage of the device
        if (ContextCompat.checkSelfPermission(MusicActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            startService(new Intent(this, MusicController.class));
        } else {
            requestStoragePermission();
        }


        //bind to the service
        doBindService();
        Log.i(TAG, "Activity: After Bind to Service");

        handler = new Handler();

        songProgressBar.setProgress(player.progress());
        complTime.setText(player.completedTime());
        remTime.setText(player.songDuration());

        setSongTitle(player.currentSongTitle());
        setSongArtist(player.getSongArtist());
        if(player.albumUri() != null){
            albumArt.setImageURI(player.albumUri());
        }else{
            albumArt.setImageResource(R.mipmap.ic_play);
        }

        if (player.isPlaying()) {
            updateSongProgress();
            playerButton.setImageResource(R.mipmap.ic_pause);
        }

        //set an ActionBar access at MusicActivity
        //if this doesn't work, find a Toolbar code at NavigationDrawer app...
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDetector = new GestureDetector(this, new MyGestureListener());


    }

    // If the user denies permissions first and try to open the app again,
    // user will get explained why permission is necessary
    private void requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,Manifest.permission.READ_EXTERNAL_STORAGE)){

            new AlertDialog.Builder(this)
                    .setTitle("Zugangsberechtigung zu Deinem Speicher")
                    .setMessage("Diese Berechtigungsanfrage ist nötig, damit Deine Musik" +
                            " von Deinen Speichern gelesen werden kann.")
                    .setPositiveButton("Na gut.", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(
                                    MusicActivity.this,
                                    new String[]{
                                            Manifest.permission.READ_EXTERNAL_STORAGE},STORAGE_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("NÖÖÖ.", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .create().show();
        }else{
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == STORAGE_PERMISSION_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Zugang wurde erteilt.", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Leider kann Deine neue Musik-App" +
                        " nicht von Deinen Speicherkarten lesen...", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == 100){
            long songIndex = data.getExtras().getLong("songIndex");
            long artist = data.getExtras().getLong("Artist");

            //stops the current song that is played before
            mCont.reset();

            //player starts the song the user selected from the list

            mCont.startSong(songIndex);
            Log.i(TAG, "onActivityResult: Service starts song user chose from listview");
            mCont.play_pause();
        }else{
            Log.i(TAG, "onActivityResult: Resultcode != 100");
            Toast.makeText(mContext, "Result Code was not received!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "Activity: onDestroy()");
        //reset the music player and release the music player

        if(mIsBound){
            doUnBindServie();
        }
        super.onDestroy();
        //mCont.cancelNotification();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        //mCont.cancelNotification();
        super.onStop();
    }


    // To get the (right top) menu button (playlist) at the toolbar of MusicActivity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        //Inflate the menu items for use the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu,menu);
        return true;
    }

    //To get the menu button be clicked by the user (Brandenburg Gate)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle the presses of the action bar items
        switch(item.getItemId()){

            case(R.id.btnPlaylist):
                Intent intent = new Intent(getApplicationContext(), PlaylistActivity.class);

                /* start the playlist activity once the user selects a song
                 *  from the list, return the information about the selected song
                 *  to MusicActivity
                 */
                startActivityForResult(intent,100);

            default:
                return super.onOptionsItemSelected(item);
        }

    }




    @Override
    public void onClick(View v) {
        switch(v.getId()){

            case(R.id.play):
                mCont.play_pause();
                break;

            case(R.id.forward):
                mCont.next();
                break;

            case(R.id.rewind):
                mCont.previous();
                break;

            case(R.id.btn_shuffle):
                if(isShuffle){
                    Log.i(TAG, "onClick Shuffle-Button: Shuffle is OFF now. ");
                    isShuffle = false;
                    player.isShuffle = false;
                    shuffleButton.setImageResource(R.drawable.ic_shuffle);
                }else{
                    Log.i(TAG, "onClick Shuffle-Button: Shuffle is ON now. ");
                    player.isShuffle = true;
                    isShuffle = true;
                    player.isRepeat = false;
                    isRepeat = false;
                    shuffleButton.setImageResource(R.drawable.ic_shuffle_green);
                    repeatButton.setImageResource(R.drawable.ic_repeat);
                }
                break;

            case(R.id.btn_repeat):
                if(isRepeat){
                    Log.i(TAG, "onClick Repeat-Button: Repeat is OFF now. ");
                    player.isRepeat = false;
                    isRepeat = false;
                    repeatButton.setImageResource(R.drawable.ic_repeat);
                }else{
                    Log.i(TAG, "onClick Repeat-Button: Repeat is ON now. ");
                    player.isRepeat = true;
                    isRepeat = true;
                    player.isShuffle = false;
                    isShuffle = false;
                    shuffleButton.setImageResource(R.drawable.ic_shuffle);
                    repeatButton.setImageResource(R.drawable.ic_repeat_red);																	 
                }
                break;
            default:
                break;
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public void update(Observable o, Object arg) {

        switch ((PlayerState)arg){

            case Ready:
                Log.i(TAG,"Activity: Player State Changed to Ready");
                playerButton.setImageResource(R.mipmap.ic_play);
                songProgressBar.setProgress(player.progress());
                complTime.setText(player.completedTime()); //left side of seekbar
                remTime.setText(player.songDuration()); // right side of seekbar
                setSongTitle(player.currentSongTitle());
                setSongArtist(player.getSongArtist());
                albumArt.setImageURI(player.albumUri());
                break;

            case Paused:
                Log.i(TAG,"Activity: Player State Changed to Paused");
                playerButton.setImageResource(R.mipmap.ic_play);
                cancelUpdateSongProgress();
                songProgressBar.setProgress(player.progress());
                complTime.setText(player.completedTime());
                remTime.setText(player.songDuration());
                break;

            case Stopped:
                Log.i(TAG,"Activity: Player State Changed to Stopped");
                playerButton.setImageResource(R.mipmap.ic_play);
                cancelUpdateSongProgress();
                //mCont.cancelNotification();
                break;

            case Playing:
                Log.i(TAG,"Activity: Player State Changed to Playing");
                playerButton.setImageResource(R.mipmap.ic_pause);
                updateSongProgress();
                break;

            case Reset:
                Log.i(TAG,"Activity: Player State Changed to Reset");
                playerButton.setImageResource(R.mipmap.ic_play);
                cancelUpdateSongProgress();
                break;

            default:
                break;
        }

    }
    // updating seekbar, time elapsed and position of seekbar thumb
    public void updateSongProgress(){
        handler.postDelayed(songProgressUpdate,500);
    }

    private Runnable songProgressUpdate = new Runnable() {
        @Override
        public void run() {
            // Initialize the progressBar and the status TextViews
            // We want to modify the progressBar. But we can do it only from
            // the UI thread. To do this we make use of the handler.

            songProgressBar.setProgress(player.progress());
            complTime.setText(player.completedTime());
            remTime.setText(player.songDuration());

            //schedule another update 500 ms later
            handler.postDelayed(songProgressUpdate, 500);


        }
    };

    public void cancelUpdateSongProgress(){

        // Cancel all callbacks that are already in the handler queue
        handler.removeCallbacks(songProgressUpdate);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        cancelUpdateSongProgress();

        if(fromUser && player.isPlaying()){
            mCont.reposition(progress);
            updateSongProgress();
        }else if (fromUser && player.isStopped())
            mCont.reposition(progress);
        updateSongProgress();

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

        //the user touched and holds down the seekbar, so stop updating
        cancelUpdateSongProgress();

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {



    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener{

        @Override
        public boolean onDown(MotionEvent e) {

            return true;
        }


//       /* @Override    // STRG + / removes Comment signs
//        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
//
//            // if user flings finger from right to left on the screen, TitleActivity.class will be started by the device
//
//            if(velocityX > 0) {
//
//                //TODO play next song instead of opening playlist
//                Intent i = new Intent(getApplicationContext(), TitleActivity.class);
//
//                /* start the playlist activity once the user selects a song
//                 *  from the list, return the information about the selected song
//                 *  to MusicActivity
//                 */
//                startActivityForResult(i, 100);
//            }else{
//                //TODO play previous song
//                Intent i = new Intent(getApplicationContext(), TitleActivity.class);
//
//                /* start the playlist activity once the user selects a song
//                 *  from the list, return the information about the selected song
//                 *  to MusicActivity
//                 */
//                startActivityForResult(i, 100);
//            }
//            return true;
//        }*/
    }
}
