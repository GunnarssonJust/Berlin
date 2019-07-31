package com.musicplayer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.musicplayer.player.MusicPlayer;

public class NoisyAudioStreamReceiver extends BroadcastReceiver {
    private MusicPlayer player;

    @Override
    public void onReceive(Context context, Intent intent) {
        player = new MusicPlayer();
        if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())){
            player.pause();
            player.reset();
            System.exit(0);
        }
    }
}
