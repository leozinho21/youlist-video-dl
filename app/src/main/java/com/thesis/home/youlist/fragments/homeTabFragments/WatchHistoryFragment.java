package com.thesis.home.youlist.fragments.homeTabFragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.ListView;

import com.google.api.services.youtube.YouTube;

import com.thesis.home.youlist.activity.MainActivity;
import com.thesis.home.youlist.async_tasks.GetYoutubeVideosTask;
import com.thesis.home.youlist.fragments.AbstractVideoFragment;
import com.thesis.home.youlist.helpers.AndroidUtils;
import com.thesis.home.youlist.helpers.ListUtils;
import com.thesis.home.youlist.helpers.YoutubeHelper;
import com.thesis.home.youlist.model.VideoPreviewInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Scott on 19/04/15.
 */
public class WatchHistoryFragment extends AbstractVideoFragment {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private static YouTube youtube;

    private String          user;
    private Map<String,List<VideoPreviewInfo>> videos = new HashMap<>();
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        this.user    = getArguments().getString("user");
        this.youtube = ((MainActivity)getActivity()).getYoutube();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(AndroidUtils.isDeviceOnline(getContext()))
        {
            getVideos();
        }
        else{
            Snackbar.make(view, "No connection.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    private void getVideos(){
        if(!ListUtils.isEmptyList(videos.get(YoutubeHelper.WATCH_HISTORY_VIDEO_TYPE))) {
            videoListAdapter.addVideoList(videos.get(YoutubeHelper.WATCH_HISTORY_VIDEO_TYPE));
        }
        else{
            new GetYoutubeVideosTask(youtube,this).execute(YoutubeHelper.WATCH_HISTORY_VIDEO_TYPE);
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        setActivatedPosition(position);
        this.mCallbacks.onItemSelected(Long.toString(id), YoutubeHelper.WATCH_HISTORY_VIDEO_TYPE);
    }
}
