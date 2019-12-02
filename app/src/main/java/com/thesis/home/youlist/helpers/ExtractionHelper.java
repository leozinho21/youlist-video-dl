package com.thesis.home.youlist.helpers;

import com.thesis.home.youlist.exceptions.ExtractionException;
import com.thesis.home.youlist.model.AbstractVideoInfo;
import com.thesis.home.youlist.model.VideoInfo;
import com.thesis.home.youlist.services.VideoPreviewInfoCollector;

import java.io.IOException;

/**
 * Created by HOME on 2/9/2016.
 */
public class ExtractionHelper {

    public static class StreamExctractException extends ExtractionException {
        StreamExctractException(String message) {
            super(message);
        }
    }

    public static void setupOptionalData(
            VideoInfo videoInfo, YoutubeVideoExtractor extractor) {
        try {
            videoInfo.thumbnail_url = extractor.getThumbnailUrl();
        } catch(Exception e) {
            videoInfo.addException(e);
        }
        try {
            videoInfo.duration = extractor.getLength();
        } catch(Exception e) {
            videoInfo.addException(e);
        }
        try {
            videoInfo.uploader = extractor.getUploader();
        } catch(Exception e) {
            videoInfo.addException(e);
        }
        try {
            videoInfo.description = extractor.getDescription();
        } catch(Exception e) {
            videoInfo.addException(e);
        }
        try {
            videoInfo.view_count = extractor.getViewCount();
        } catch(Exception e) {
            videoInfo.addException(e);
        }
        try {
            videoInfo.upload_date = extractor.getUploadDate();
        } catch(Exception e) {
            videoInfo.addException(e);
        }
        try {
            videoInfo.uploader_thumbnail_url = extractor.getUploaderThumbnailUrl();
        } catch(Exception e) {
            videoInfo.addException(e);
        }
        try {
            videoInfo.start_position = extractor.getTimeStamp();
        } catch(Exception e) {
            videoInfo.addException(e);
        }
        try {
            videoInfo.average_rating = extractor.getAverageRating();
        } catch(Exception e) {
            videoInfo.addException(e);
        }
        try {
            videoInfo.like_count = extractor.getLikeCount();
        } catch(Exception e) {
            videoInfo.addException(e);
        }
        try {
            videoInfo.dislike_count = extractor.getDislikeCount();
        } catch(Exception e) {
            videoInfo.addException(e);
        }
        try {
            // get next video
            VideoPreviewInfoCollector c = new VideoPreviewInfoCollector();
            c.commit(extractor.getNextVideo());
            if(c.getItemList().size() != 0) {
                videoInfo.next_video = c.getItemList().get(0);
            }
            videoInfo.errors.addAll(c.getErrors());
        } catch(Exception e) {
            videoInfo.addException(e);
        }
        try {
            // get related videos
            VideoPreviewInfoCollector c = extractor.getRelatedVideos();
            videoInfo.related_videos = c.getItemList();
            videoInfo.errors.addAll(c.getErrors());
        } catch(Exception e) {
            videoInfo.addException(e);
        }
    }

    public static void setupImportantData(
            VideoInfo videoInfo, YoutubeVideoExtractor extractor)
            throws ExtractionException, IOException {

        videoInfo.webpage_url   = extractor.getPageUrl();
        videoInfo.stream_type   = Parser.getStreamType();
        videoInfo.id            = YoutubeHelper.getVideoId(extractor.getPageUrl());
        videoInfo.title         = extractor.getTitle();
        videoInfo.age_limit     = extractor.getAgeLimit();

        if((videoInfo.stream_type == AbstractVideoInfo.StreamType.NONE)
                || (videoInfo.webpage_url == null || videoInfo.webpage_url.isEmpty())
                || (videoInfo.id == null || videoInfo.id.isEmpty())
                || (videoInfo.title == null /* videoInfo.title can be empty of course */)
                || (videoInfo.age_limit == -1)) {
            throw new ExtractionException("Some important stream information was not given.");
        }

         /*  Load and extract audio */
        try {
            videoInfo.audio_streams = extractor.getAudioStreams();
        } catch(Exception e) {
            videoInfo.addException(new ExtractionException("Couldn't get audio streams", e));
        }
        /* Extract video stream url*/
        try {
            videoInfo.video_streams = extractor.getVideoStreams();
        } catch (Exception e) {
            videoInfo.addException(
                    new ExtractionException("Couldn't get video streams", e));
        }
        // we didn't get a stream,
        if((videoInfo.video_streams == null || videoInfo.video_streams.isEmpty())
                && (videoInfo.audio_streams == null || videoInfo.audio_streams.isEmpty())) {
            throw new StreamExctractException(
                    "Could not get any stream. See error variable to get further details.");
        }
    }
}
