package com.thesis.home.youlist.fragments;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.github.rtoshiro.view.video.FullscreenVideoLayout;
import com.squareup.picasso.Picasso;
import com.thesis.home.youlist.R;
import com.thesis.home.youlist.activity.VideoDetailActivity;
import com.thesis.home.youlist.helpers.StringUtils;
import com.thesis.home.youlist.model.Constants;

import java.io.IOException;

/**
 * Created by Scott on 19/04/15.
 */
public class PlayVideoFragment extends Fragment {

    public static final     String STREAM_URL       = "stream_url";
    public static final     String START_POSITION   = "start_position";
    private static final    String POSITION         = "position";

    private static final long HIDING_DELAY = 3000;

    private FullscreenVideoLayout videoLayout;
    private ImageView videoThumbnailView;
    private View detailVideoThumb;
    private int position = 0;
    private ProgressBar progressBar;
    private Bitmap videoThumbnail;

    private View decorView;
    private boolean uiIsHidden = false;
    private static long lastUiShowTime = 0;
    private boolean hasSoftKeys = false;

    private static final String PREF_IS_LANDSCAPE = "is_landscape";
    private boolean isLandscape = true;
    private SharedPreferences prefs;
    private VideoDetailActivity a;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        a = (VideoDetailActivity) getActivity();
        a.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View fragment = inflater.inflate(R.layout.play_video_fragment, container, false);

        String thumbnail_url = getArguments().getString(Constants.THUMB_URL);

        videoThumbnailView = (ImageView) fragment.findViewById(R.id.detailThumbnailView);
        if(StringUtils.notEmpty(thumbnail_url)) {

            Picasso.with(a).load(thumbnail_url).into(videoThumbnailView);

            Button backgroundButton = (Button)fragment.findViewById(R.id.detailVideoThumbnailWindowBackgroundButton);

            backgroundButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playVideo(fragment);
                }
            });
            ImageView playArrowView = (ImageView) fragment.findViewById(R.id.playArrowView);
            playArrowView.setVisibility(View.VISIBLE);

        } else {
            videoThumbnailView.setImageResource(R.drawable.dummy_thumbnail);
        }

        if(savedInstanceState != null){
            position = savedInstanceState.getInt(POSITION);
        }
        else{
            position = getArguments().getInt(Constants.START_POSITION);
        }
        isLandscape = checkIfLandscape();
        hasSoftKeys = checkIfHasSoftKeys();

        position = getArguments().getInt(START_POSITION, 0)*1000;//convert from seconds to milliseconds

        videoLayout = (FullscreenVideoLayout) fragment.findViewById(R.id.videoview);
        videoLayout.setActivity(a);
        videoLayout.seekTo(position);
        progressBar = (ProgressBar) fragment.findViewById(R.id.detailProgressBar);
        try {
            videoLayout.setVideoURI(Uri.parse(getArguments().getString(STREAM_URL)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Button button = (Button) fragment.findViewById(R.id.content_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(uiIsHidden) {
                    showUi();
                } else {
                    if(isLandscape)
                    {
                        hideUi();
                    }
                }
            }
        });
        decorView = a.getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if (visibility == View.VISIBLE && uiIsHidden) {
                    showUi();
                }
            }
        });

        prefs = a.getPreferences(Context.MODE_PRIVATE);
        if(prefs.getBoolean(PREF_IS_LANDSCAPE, false) && !isLandscape) {
            toggleOrientation();
        }
        return fragment;
    }

    public void refreshVideo(String videoUrl,String thumbnail_url) throws IOException {

        if(detailVideoThumb != null) detailVideoThumb.setVisibility(View.VISIBLE);

        Picasso.with(a).load(thumbnail_url).into(videoThumbnailView);

       if(videoLayout != null) {
           videoLayout.reset();
           videoLayout.setVideoURI(Uri.parse(videoUrl));
       }
    }

    @Override
    public void onPause() {
        super.onPause();
        videoLayout.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        videoLayout.seekTo(a.getStart_position());
//        videoView.seekTo(a.getStart_position());
    }

    @Override
    public void onDestroy() {
        videoLayout = null;
        super.onDestroy();
        prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        videoLayout.pause();
    }

    public void playVideo(View fragment) {

        if(detailVideoThumb == null)
        {
            detailVideoThumb = fragment.findViewById(R.id.detailVideoThumbnailWindowLayout);
        }

        detailVideoThumb.setVisibility(View.GONE);
        videoLayout.start();

        if(isLandscape) {
             Handler handler = new Handler();
             handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                      if ((System.currentTimeMillis() - lastUiShowTime) >= HIDING_DELAY) {
                           hideUi();
                      }
                    }}, HIDING_DELAY);
               lastUiShowTime = System.currentTimeMillis();
            }

    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);

        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;
            adjustMediaControlMetrics();
            hideUi();
        } else if (config.orientation == Configuration.ORIENTATION_PORTRAIT){
            isLandscape = false;
            adjustMediaControlMetrics();
            showUi();
        }
    }
    private void showUi() {
        try {
            uiIsHidden = false;
            adjustMediaControlMetrics();
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            if(isLandscape){
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if ((System.currentTimeMillis() - lastUiShowTime) >= HIDING_DELAY) {
                            hideUi();
                        }
                    }
                }, HIDING_DELAY);
                lastUiShowTime = System.currentTimeMillis();
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void hideUi() {
        uiIsHidden = true;
        if (Build.VERSION.SDK_INT >= 17) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    private void adjustMediaControlMetrics() {
        videoLayout.setLeft(0);
        videoLayout.setTop(0);
    }
    private boolean checkIfLandscape() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        a.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels < displayMetrics.widthPixels;
    }
    private boolean checkIfHasSoftKeys(){
        return Build.VERSION.SDK_INT >= 17 ||
                getNavigationBarHeight() != 0 ||
                getNavigationBarWidth() != 0;
    }

    private void toggleOrientation() {
        if(isLandscape)  {
            isLandscape = false;
            a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            isLandscape = true;
            hideUi();
            a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_IS_LANDSCAPE, isLandscape);
        editor.apply();
    }
    private int getNavigationBarHeight() {
        if(Build.VERSION.SDK_INT >= 17) {
            Display d = getActivity().getWindowManager().getDefaultDisplay();

            DisplayMetrics realDisplayMetrics = new DisplayMetrics();
            d.getRealMetrics(realDisplayMetrics);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            d.getMetrics(displayMetrics);

            int realHeight = realDisplayMetrics.heightPixels;
            int displayHeight = displayMetrics.heightPixels;
            return realHeight - displayHeight;
        } else {
            return 50;
        }
    }

    private int getNavigationBarWidth() {
        if(Build.VERSION.SDK_INT >= 17) {
            Display d = getActivity().getWindowManager().getDefaultDisplay();

            DisplayMetrics realDisplayMetrics = new DisplayMetrics();
            d.getRealMetrics(realDisplayMetrics);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            d.getMetrics(displayMetrics);

            int realWidth = realDisplayMetrics.widthPixels;
            int displayWidth = displayMetrics.widthPixels;
            return realWidth - displayWidth;
        } else {
            return 50;
        }
    }
}
