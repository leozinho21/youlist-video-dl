package com.thesis.home.youlist.model;

import com.thesis.home.youlist.exceptions.ExtractionException;
import com.thesis.home.youlist.helpers.ExtractionHelper;
import com.thesis.home.youlist.helpers.YoutubeVideoExtractor;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * Created by HOME on 11/5/2016.
 */
public class VideoInfo extends AbstractVideoInfo {

    public String uploader_thumbnail_url = "";
    public String description = "";
    public List<YouTubeVideo> video_streams = null;
    public List<YouTubeAudio> audio_streams = null;

    public int duration = -1;

    public int age_limit = -1;
    public int like_count = -1;
    public int dislike_count = -1;
    public String average_rating = "";
    public VideoPreviewInfo next_video = null;
    public List<VideoPreviewInfo> related_videos = null;
    public int start_position = 0;

    public List<Exception> errors = new Vector<>();

    public VideoInfo() {}

    public void addException(Exception e) {
        errors.add(e);
    }

    public static VideoInfo getVideoInfo(YoutubeVideoExtractor extractor)
            throws ExtractionException, IOException {
        VideoInfo videoInfo = new VideoInfo();

        ExtractionHelper.setupImportantData(videoInfo, extractor);
        ExtractionHelper.setupOptionalData(videoInfo, extractor);

        return videoInfo;
    }

}