package com.thesis.home.youlist.model;

/**
 * Created by HOME on 11/5/2016.
 */
public class YouTubeVideo {
    public String url = "";
    public int format = -1;
    public String resolution = "";

    public YouTubeVideo(String url, int format, String res) {
        this.url = url; this.format = format; resolution = res;
    }
}
