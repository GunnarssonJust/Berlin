package com.musicplayer.player;

public enum NextState {
    Shuffle,  //Shuffle-Mode is on, all titles will be played randomly
    ShuffleAlbum, // Shuffle-Mode for Album, all titles of album will be played randomly
    ShuffleArtist, // Shuffle-Mode for Artists
    RepeatOne,  // Repeat-Mode on, current title will be repeated for one time only
    Repeat,  // Repeat-Mode on, current title will be repeated infinitely
    Next  // Next-Mode on, songindex = ++songindex
}
