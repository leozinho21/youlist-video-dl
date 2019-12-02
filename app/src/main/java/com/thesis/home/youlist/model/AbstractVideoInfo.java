package com.thesis.home.youlist.model;

/**
 * Created by HOME on 11/5/2016.
 */

import android.graphics.Bitmap;

/**Common properties between VideoInfo and VideoPreviewInfo.*/
public abstract class AbstractVideoInfo {
    public enum StreamType {
        NONE,   // placeholder to check if stream type was checked or not
        VIDEO_STREAM,
        AUDIO_STREAM,
        LIVE_STREAM,
        AUDIO_LIVE_STREAM,
        FILE
    }

    public StreamType stream_type;
    public String id = "";
    public String title = "";
    public String uploader = "";
    public String thumbnail_url = "";
    public Bitmap thumbnail = null;
    public String webpage_url = "";
    public String upload_date = "";
    public long view_count = -1;
}
