package com.musicplayer.activities;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.musicplayer.R;
import com.musicplayer.adapter.ViewPagerAdapter;
import com.musicplayer.fragments.FragmentAlbum;
import com.musicplayer.fragments.FragmentArtist;
import com.musicplayer.fragments.FragmentTitel;

/* This class is parent for < 3 > Fragments:
 *  FragmentAlbum
 *  FragmentArtist
 *  FragmentTitel
 *
 *  where the user can search and or select his song to play
 *   Created on 24/02/19  by Gunnar Just
 */


public class PlaylistActivity extends AppCompatActivity implements View.OnClickListener{

    private TabLayout tabLayout;
    private AppBarLayout appBarLayout;
    private ViewPager viewPager;
    private ImageView backarrow, search;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        tabLayout = findViewById(R.id.tablayout_playlist);
        appBarLayout = findViewById(R.id.appbar_playlist);
        viewPager = findViewById(R.id.viewpager_id);
        backarrow = findViewById(R.id.backarrow_playlist);
        backarrow.setOnClickListener(this);
        search = findViewById(R.id.search_playlist);
        search.setOnClickListener(this);

        //Adding Fragments to Activity
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new FragmentTitel(), "Titel");
        adapter.addFragment(new FragmentAlbum(),"Album");
        adapter.addFragment(new FragmentArtist(),"K\u00FCnstler");


        //adapter Setup
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

    }


    @Override
    public void onClick(View v) {
        switch(v.getId()){

            case(R.id.backarrow_playlist):
                Intent intent = new Intent(this,MusicActivity.class);
                startActivity(intent);
                finish();
                break;
            case(R.id.search_playlist):
                Intent intent_search = new Intent(this, SearchActivity.class);
                startActivity(intent_search);
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //Ende------------------------------------------------------------------------------------------
}