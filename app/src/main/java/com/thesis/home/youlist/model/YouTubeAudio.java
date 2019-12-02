package com.thesis.home.youlist.model;

public class YouTubeAudio {
    public String url = "";
    public int format = -1;
    public int bandwidth = -1;
    public int sampling_rate = -1;

    public YouTubeAudio(String url, int format, int bandwidth, int samplingRate) {
        this.url = url; this.format = format;
        this.bandwidth = bandwidth; this.sampling_rate = samplingRate;
    }
}
