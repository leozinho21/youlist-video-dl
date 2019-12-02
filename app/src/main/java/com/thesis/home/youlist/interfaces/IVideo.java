package com.thesis.home.youlist.interfaces;

import com.thesis.home.youlist.model.VideoPreviewInfo;

import java.util.List;

/**
 * Created by HOME on 12/3/2016.
 */
public interface IVideo {
    void onSuccess(List<VideoPreviewInfo> resultList);
    void onFailure(Error error);
}
