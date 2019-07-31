package com.musicplayer.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.musicplayer.R;
import com.musicplayer.activities.MusicActivity;

public class FragmentAlbum extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>{
    View view;

    private SimpleCursorAdapter mAdapter;
    private Context mContext;


    public FragmentAlbum() {
        //required empty public Constructor
    }


    //---------------------------BEGIN ONCREATE METHOD----------------------------------------------

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_titel,container,false);

        mContext = getActivity();

        ListView listView = view.findViewById(android.R.id.list);
        listView.getAdapter();
        listView.setFastScrollEnabled(true);
        listView.setFastScrollAlwaysVisible(true);
        listView.setFastScrollStyle(5);


        String[] mString = new String[] {MediaStore.Audio.AlbumColumns.ALBUM, MediaStore.Audio.Media.ARTIST};

        mAdapter = new SimpleCursorAdapter(getActivity(),R.layout.content_playlist_title,null,
                mString, new int[] {R.id.songlist,R.id.songArtist},0);
        setListAdapter(mAdapter);
        getLoaderManager().initLoader(0,null,(LoaderManager.LoaderCallbacks<Cursor>)this);

        //inflate the layout for this fragment
        return view;
    }
    //---------------------------END ONCREATE METHOD------------------------------------------------

    //get METHODS TO CALL


    //If the user selects a song, Fragment sends data to onActivityResult() at MusicActivity class
    @Override
    public void onListItemClick(ListView l, View v, int position, long id){
        super.onListItemClick(l, v, position, id);

        String artist = MediaStore.Audio.Media.ARTIST;
        //return the information about the selected song to MusicActivity
        Intent in = new Intent(getContext(), MusicActivity.class);
        in.putExtra("songIndex", id);
        in.putExtra("artist",artist);


        //return the same return code 100 that MusicActivity used to start this activity.
        getActivity().setResult(100,in);
        getActivity().finish();

    }


    // These are the MediaStore columns that we will retrieve
    static final String[] MUSIC_SUMMARY_PROJECTION = new String[]{
            MediaStore.Audio.Media.ARTIST, //Artist's name
            MediaStore.Audio.AlbumColumns.ALBUM,
            MediaStore.Audio.Media._ID   //Album Art
    };



    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //This is called when a new loader needs to be loaded. This case has only one loader
        //so we don't care about loader ID
        //First pick the base ID to use

        Uri baseUri;
        String select;
        baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        // now create the cursorLoader and return it so that the loader will take care of
        // creating the Cursor for the data being displayed

        select = "((" +  MediaStore.Audio.Media.DISPLAY_NAME + " NOTNULL) AND ("
                + MediaStore.Audio.Media.DISPLAY_NAME + " != '' ) AND ("
                + MediaStore.Audio.Media.IS_MUSIC + "!= 0))";

        return new CursorLoader(getActivity(),baseUri,MUSIC_SUMMARY_PROJECTION,select,
                null,MediaStore.Audio.AlbumColumns.ALBUM + " COLLATE LOCALIZED ASC" );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}
