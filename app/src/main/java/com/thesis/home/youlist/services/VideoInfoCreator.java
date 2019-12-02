package com.thesis.home.youlist.services;

import com.thesis.home.youlist.exceptions.ParsingException;
import com.thesis.home.youlist.helpers.Parser;
import com.thesis.home.youlist.helpers.YoutubeHelper;
import com.thesis.home.youlist.model.AbstractVideoInfo;

import org.jsoup.nodes.Element;

public class VideoInfoCreator {

    private final Element item;

    private String  webPageUrl;
    private String  title;
    private int     duration = -1;
    private String  uploader;
    private String  thumbnailUrl;
    private long    viewCount = -1;

    public VideoInfoCreator(Element item) {
        this.item = item;
    }

    public String getWebPageUrl() throws ParsingException {
        if ( webPageUrl != null ) return webPageUrl;
        return YoutubeHelper.getWebPageUrl(item);
    }

    public String getTitle() throws ParsingException {
        if ( title != null ) return title;
        return YoutubeHelper.getTitle(item);
    }

    public int getDuration() throws ParsingException {
        if ( duration != -1) return duration;
        return YoutubeHelper.getDuration(item);
    }

    public String getUploader() throws ParsingException {
        if ( uploader != null ) return uploader;
        return YoutubeHelper.getUploader(item);
    }

    public String getUploadDate() throws ParsingException {
        return YoutubeHelper.getUploadDate(item);
    }

    public long getViewCount() throws ParsingException {
        if ( viewCount != -1 ) return viewCount;
        return YoutubeHelper.getViewCount(item);
    }

    public String getThumbnailUrl() throws ParsingException {
        if ( thumbnailUrl != null ) return thumbnailUrl;
        return YoutubeHelper.getThumbnailUrl(item);
    }

    public AbstractVideoInfo.StreamType getStreamType() {
        return YoutubeHelper.getStreamType(item);
    }

    public void setWebPageUrl(String webPageUrl) {
        this.webPageUrl = webPageUrl;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }
}
