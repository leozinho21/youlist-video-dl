package com.thesis.home.youlist.services;

import com.thesis.home.youlist.model.SearchResult;

/**
 * Created by HOME on 25/5/2016.
 */
public class VideoPreviewInfoSearchCollector extends VideoPreviewInfoCollector {

    private String suggestion = "";

    public VideoPreviewInfoSearchCollector() {
        super();
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public SearchResult getSearchResult() {
        SearchResult result = new SearchResult();
        result.suggestion = suggestion;
        result.errors = getErrors();
        result.resultList = getItemList();
        return result;
    }
}
