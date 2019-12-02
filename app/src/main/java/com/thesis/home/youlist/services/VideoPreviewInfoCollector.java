package com.thesis.home.youlist.services;

import com.thesis.home.youlist.exceptions.ParsingException;
import com.thesis.home.youlist.helpers.YoutubeHelper;
import com.thesis.home.youlist.model.VideoPreviewInfo;

import java.util.List;
import java.util.Vector;

public class VideoPreviewInfoCollector {
    private List<VideoPreviewInfo> itemList = new Vector<>();
    private List<Exception> errors = new Vector<>();

    public VideoPreviewInfoCollector() {
    }

    public List<VideoPreviewInfo> getItemList() {
        return itemList;
    }

    public List<Exception> getErrors() {
        return errors;
    }

    public void addError(Exception e) {
        errors.add(e);
    }

    public void commit(VideoInfoCreator extractor) throws ParsingException {
        try {
            VideoPreviewInfo resultItem = new VideoPreviewInfo();

            resultItem.webpage_url = extractor.getWebPageUrl();
            resultItem.id       = YoutubeHelper.getVideoId(resultItem.webpage_url);
            resultItem.title    = extractor.getTitle();
            resultItem.stream_type = extractor.getStreamType();

            // optional iformation
            try {
                resultItem.duration = extractor.getDuration();
            } catch (Exception e) {
                addError(e);
            }
            try {
                resultItem.uploader = extractor.getUploader();
            } catch (Exception e) {
                addError(e);
            }
            try {
                resultItem.upload_date = extractor.getUploadDate();
            } catch (Exception e) {
                addError(e);
            }
            try {
                resultItem.view_count = extractor.getViewCount();
            } catch (Exception e) {
                addError(e);
            }
            try {
                resultItem.thumbnail_url = extractor.getThumbnailUrl();
            } catch (Exception e) {
                addError(e);
            }
            itemList.add(resultItem);
        } catch (Exception e) {
            addError(e);
        }
    }
}
