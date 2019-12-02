package com.thesis.home.youlist.helpers;

import com.thesis.home.youlist.model.VideoPreviewInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Helper class for providing mock data to the app.
 */
public class YoutubeContent {

    /**
     * An array of YouTube videos
     */
    public static List<VideoPreviewInfo> ITEMS = new ArrayList<>();

    /**
     * A map of YouTube videos, by ID.
     */
    public static Map<String, List<VideoPreviewInfo>> ITEM_MAP = new HashMap<>();


    public static List<VideoPreviewInfo> RELATED_VIDEOS = new ArrayList<>();

    public static void addItems(String type,List<VideoPreviewInfo> videos) {
        clearItems();
        ITEMS.addAll(videos);
        ITEM_MAP.put(type,  videos );
    }

    public static void clearItems() {
            ITEMS.clear();
            ITEM_MAP.clear();
    }

    public static void addRelatedVideos(List<VideoPreviewInfo> videos) {
        RELATED_VIDEOS.addAll(videos);
    }

    public static void clearRelatedVideos() {
        RELATED_VIDEOS.clear();
    }

    public static List<VideoPreviewInfo> getRelatedVideos() {
        return RELATED_VIDEOS;
    }
}
