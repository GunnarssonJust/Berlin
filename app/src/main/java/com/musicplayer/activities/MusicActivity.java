package com.musicplayer.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import android.widget.Toast;

import com.musicplayer.R;
import com.musicplayer.player.MusicPlayer;
import com.musicplayer.player.NextState;
import com.musicplayer.player.PlayerState;
import com.musicplayer.service.MusicController;

import java.util.Observable;
import java.util.Observer;

//imported from hkust.comp4521.audio;

public class MusicActivity extends AppCompatActivity implements View.OnClickListener , SeekBar.OnSeekBarChangeListener, Observer {
    private int STORAGE_PERMISSION_CODE = 1;

    private static final String TAG = "MusicActivity";
    //Handling Logs on debug window
    private boolean D = true;

    private ImageView playerButton, rewindButton, forwardButton, pauseButton, btnPlaylist;
    private ImageView albumArt, shuffleButton, repeatButton;
    private TextView songTitleText, songTitleArtist;
    private Context mContext = MusicActivity.this;
    private SeekBar songProgressBar;
    private TextView complTime, remTime;

    //indicates if the activity is bound to the MusicController Service
    private boolean mIsBound = false;

    private MusicController mCont;
    MusicPlayer player;
    NextState nxState;

    public static Handler handler;

    private GestureDetector mDetector;
    private static final String MUSIC_CHANNEL = "musicchannel";
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

    void doBindService() {
        Intent in = new Intent(this, MusicController.class);
        bindService(in, Scon, Context.BIND_AUTO_CREATE);
    }

    void doUnBindServie() {
        if (mIsBound) {
            unbindService(Scon);
            mIsBound = false;
            mCont.stopSelf();
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
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // For API 23 and later Developers need to include a runtime permission request
        // for sd_card AND internal storage of the device
        if (ContextCompat.checkSelfPermission(MusicActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            startService(new Intent(this, MusicController.class));
        } else {
            requestStoragePermission();
        }

        if(D)Log.i(TAG, "Activity: onCreate()");

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

        //get a reference to the song title, artist, and albumArt TextView resp. ImageView in the UI
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
        //new TranslateAnimation(float fromXDelta,float toXDelta, float fromYDelta, float toYDelta);

        // The music player is implemented as a Java Singleton class so that only ONE
        // instance of the player is present within the application. The getMusicPlayer()
        // method returns the reference to the instance of the music player class

        // 1. get a reference to the instance of the music player
        // 2. set the context for the music player to be this activity
        // 3. add this activity as an observer
        player = MusicPlayer.getMusicPlayer();
        player.addObserver(this);

        // For API 23 and later Developers need to include a runtime permission request
        // for sd_card AND internal storage of the device


        //bind to the service
        doBindService();
        if(D)Log.i(TAG, "Activity: Service is binded now");

        handler = new Handler();

        songProgressBar.setProgress(player.progress());
        complTime.setText(player.completedTime());
        remTime.setText(player.songDuration());

        setSongTitle(player.currentSongTitle());
        setSongArtist(player.getSongArtist());
        if (player.albumUri() != null) {
            albumArt.setImageURI(player.albumUri());
        } else {
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
    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_granted)
                    .setMessage(R.string.permission_granting)
                    .setPositiveButton(R.string.na_gut, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(
                                    MusicActivity.this,
                                    new String[]{
                                            Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton(R.string.nein, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Zugang wurde erteilt.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Leider kann Deine neue Musik-App" +
                        " nicht von Deinen Speicherkarten lesen...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Chosen song from playlist activity arrives here and will be noticed
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == 100) {
            long songIndex = data.getExtras().getLong("songIndex");
            long artist = data.getExtras().getLong("Artist");

            //stops the current song that is played before
            mCont.reset();

            //player starts the song the user selected from the list

            mCont.startSong(songIndex);
            if(D)Log.i(TAG, "onActivityResult: Service starts song user chose from listview");
            mCont.play_pause();
        } else {
            if(D)Log.i(TAG, "onActivityResult: Resultcode != 100");
            Toast.makeText(mContext, "Result Code was not received!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if(D)Log.i(TAG, "Activity: onDestroy()");
        //reset the music player and release the music player
        if (mIsBound) {
            Intent serviceIntent = new Intent(this, MusicController.class);
            stopService(serviceIntent);
            doUnBindServie();
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
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
    protected void onStop() {
        //mCont.cancelNotification();
        super.onStop();
    }

    // To get the (right top) menu button (playlist) at the toolbar of MusicActivity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        //Inflate the menu items for use the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    //To get the menu button be clicked by the user (Brandenburg Gate, right top of player's UI)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle the presses of the action bar items
        switch (item.getItemId()) {

            case (R.id.btnPlaylist):
                Intent intent = new Intent(getApplicationContext(), PlaylistActivity.class);

                /* start the playlist activity once the user selects a song
                 *  from the list, return the information about the selected song
                 *  to MusicActivity
                 */
                startActivityForResult(intent, 100);
                break;

            case (R.id.search_playlist):
                Intent searchIntent = new Intent(getApplicationContext(), SearchActivity.class);
                //if code for SearchActivity is finished, startActivityforResult(intent,requestCode)
                //startActivityForResult();
                //but otherwise searchIntent(intent) will be used
                startActivity(searchIntent);
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case (R.id.play):
                mCont.play_pause();
                break;

            case (R.id.forward):
                mCont.next();
                break;

            case (R.id.rewind):
                mCont.previous();
                break;

            case (R.id.btn_shuffle): {
                if (player.isShuffle) {
                    if(D)Log.i(TAG, "onClick Shuffle-Button: Shuffle is OFF now. ");
                    player.setShuffle(false);
                    shuffleButton.setImageResource(R.drawable.ic_shuffle);
                } else {
                    if(D)Log.i(TAG, "onClick Shuffle-Button: Shuffle is ON now. ");
                    player.setShuffle(true);
                    player.setRepeat(false);
                    shuffleButton.setImageResource(R.drawable.ic_shuffle_green);
                    repeatButton.setImageResource(R.drawable.ic_repeat);
                }
                break;
            }

            case (R.id.btn_repeat): {
                if (player.isRepeat) {
                    if(D)Log.i(TAG, "onClick Repeat-Button: Repeat is OFF now. ");
                    player.setRepeat(false);
                    repeatButton.setImageResource(R.drawable.ic_repeat);
                } else {
                    if(D)Log.i(TAG, "onClick Repeat-Button: Repeat is ON now. ");
                    player.setRepeat(true);
                    player.setShuffle(false);
                    shuffleButton.setImageResource(R.drawable.ic_shuffle);
                    repeatButton.setImageResource(R.drawable.ic_repeat_red);
                }
                break;
            }
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

        switch ((PlayerState) arg) {

            case Ready:
                if(D)Log.i(TAG, "Activity: Player State Changed to Ready");
                playerButton.setImageResource(R.mipmap.ic_play);
                songProgressBar.setProgress(player.progress());
                complTime.setText(player.completedTime()); //left side of seekbar
                remTime.setText(player.songDuration()); // right side of seekbar
                setSongTitle(player.currentSongTitle());
                setSongArtist(player.getSongArtist());
                albumArt.setImageURI(player.albumUri());
                break;

            case Paused:
                if(D)Log.i(TAG, "Activity: Player State Changed to Paused");
                playerButton.setImageResource(R.mipmap.ic_play);
                cancelUpdateSongProgress();
                songProgressBar.setProgress(player.progress());
                complTime.setText(player.completedTime());
                remTime.setText(player.songDuration());
                mCont.updateNotification();
                mCont.updateWidget();
                break;

            case Stopped:
                if(D)Log.i(TAG, "Activity: Player State Changed to Stopped");
                playerButton.setImageResource(R.mipmap.ic_play);
                cancelUpdateSongProgress();
                mCont.cancelNotification();
                mCont.updateWidget();
                break;

            case Playing:
                if(D)Log.i(TAG, "Activity: Player State Changed to Playing");
                playerButton.setImageResource(R.mipmap.ic_pause);
                updateSongProgress();
                mCont.updateNotification();
                mCont.updateWidget();
                break;

            case Reset:
                if(D)Log.i(TAG, "Activity: Player State Changed to Reset");
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
//                Intent i = new Intent(getApplicationContext(), PlaylistActivity.class);
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
