package com.thesis.home.youlist.model;

import com.thesis.home.youlist.exceptions.ExtractionException;
import com.thesis.home.youlist.helpers.Downloader;
import com.thesis.home.youlist.services.YoutubeSearchEngine;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

public class SearchResult {
    public static SearchResult getSearchResult(YoutubeSearchEngine engine, String query,
                                               int page, String languageCode, Downloader dl)
            throws ExtractionException, IOException {

        SearchResult result = engine.search(query, page, languageCode, dl).getSearchResult();
        if(result.resultList.isEmpty()) {
            if(result.suggestion.isEmpty()) {
                throw new ExtractionException("Empty result despite no error");
            } else {
                Log.d(result.suggestion);
            }
        }
        return result;
    }

    public String suggestion = "";
    public List<VideoPreviewInfo> resultList = new Vector<>();
    public List<Exception> errors = new Vector<>();
}
