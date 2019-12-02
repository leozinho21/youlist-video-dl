package com.thesis.home.youlist.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.thesis.home.youlist.R;
import com.thesis.home.youlist.model.AbstractVideoInfo;
import com.thesis.home.youlist.model.VideoPreviewInfo;


public class VideoInfoViewCreator {
    private final LayoutInflater inflater;

    public VideoInfoViewCreator(LayoutInflater inflater) {
        this.inflater = inflater;
    }
    Context context;
    public View getViewFromVideoInfoItem(View convertView, ViewGroup parent, VideoPreviewInfo info, Context context) {
        ViewHolder holder;
        this.context = context;
        // generate holder
        if(convertView == null) {
            convertView = inflater.inflate(R.layout.video_item, parent, false);
            holder = new ViewHolder();
            holder.itemThumbnailView  = (ImageView) convertView.findViewById(R.id.itemThumbnailView);
            holder.itemVideoTitleView = (TextView) convertView.findViewById(R.id.itemVideoTitleView);
            holder.itemUploaderView   = (TextView) convertView.findViewById(R.id.itemUploaderView);
            holder.itemDurationView   = (TextView) convertView.findViewById(R.id.itemDurationView);
            holder.itemUploadDateView = (TextView) convertView.findViewById(R.id.itemUploadDateView);
            holder.itemViewCountView  = (TextView) convertView.findViewById(R.id.itemViewCountView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.itemVideoTitleView.setText(info.title);
        if(info.uploader != null && !info.uploader.isEmpty()) {
            holder.itemUploaderView.setText(info.uploader);
        } else {
            holder.itemUploaderView.setVisibility(View.INVISIBLE);
        }
        if(info.duration > 0) {
            holder.itemDurationView.setText(YoutubeHelper.getDurationString(info.duration));
        } else {
            if(info.stream_type == AbstractVideoInfo.StreamType.LIVE_STREAM) {
                holder.itemDurationView.setText(R.string.duration_live);
            } else {
                holder.itemDurationView.setVisibility(View.GONE);
            }
        }
        if(info.view_count >= 0) {
            holder.itemViewCountView.setText(YoutubeHelper.shortViewCount(info.view_count));
        } else {
            holder.itemViewCountView.setVisibility(View.GONE);
        }
        if(info.upload_date != null && !info.upload_date.isEmpty()) {
            holder.itemUploadDateView.setText(info.upload_date + " • ");
        }

        holder.itemThumbnailView.setImageResource(R.drawable.dummy_thumbnail);
        if(info.thumbnail_url != null && !info.thumbnail_url.isEmpty()) {

            Picasso.with(context).load(info.thumbnail_url).into( holder.itemThumbnailView);
        }

        return convertView;
    }

    private class ViewHolder {
        public ImageView itemThumbnailView;
        public TextView itemVideoTitleView, itemUploaderView, itemDurationView, itemUploadDateView, itemViewCountView;
    }
}
