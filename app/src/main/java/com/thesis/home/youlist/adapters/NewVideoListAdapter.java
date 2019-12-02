package com.thesis.home.youlist.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.thesis.home.youlist.R;
import com.thesis.home.youlist.helpers.VideoInfoViewCreator;
import com.thesis.home.youlist.model.VideoPreviewInfo;

import java.util.List;
import java.util.Vector;

public class NewVideoListAdapter extends BaseAdapter {
    private final Context context;
    private final VideoInfoViewCreator viewCreator;
    private Vector<VideoPreviewInfo> videoList = new Vector<>();
    private final ListView listView;

    public NewVideoListAdapter(Context context,ListView listView) {

        viewCreator = new VideoInfoViewCreator(LayoutInflater.from(context));
        this.context = context;
        this.listView = listView;
        this.listView.setDivider(null);
        this.listView.setDividerHeight(0);
    }

    public void addVideoList(List<VideoPreviewInfo> videos) {
        videoList.addAll(videos);
        notifyDataSetChanged();
    }

    public void clearVideoList() {
        videoList = new Vector<>();
        notifyDataSetChanged();
    }

    public Vector<VideoPreviewInfo> getVideoList() {
        return videoList;
    }

    @Override
    public int getCount() {
        return videoList.size();
    }

    @Override
    public Object getItem(int position) {
        return videoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = viewCreator.getViewFromVideoInfoItem(convertView, parent, videoList.get(position),context);

        if(listView.isItemChecked(position)) {
            convertView.setBackgroundColor(ContextCompat.getColor(context, R.color.light_youtube_primary_color));
        } else {
            convertView.setBackgroundColor(0);
        }

        return convertView;
    }
}