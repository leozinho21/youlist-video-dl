package com.thesis.home.youlist.helpers;

/**
 * Created by HOME on 11/5/2016.
 */


import com.thesis.home.youlist.exceptions.ExtractionException;
import com.thesis.home.youlist.exceptions.ParsingException;
import com.thesis.home.youlist.model.Log;
import com.thesis.home.youlist.model.MediaFormat;
import com.thesis.home.youlist.model.VideoInfo;
import com.thesis.home.youlist.model.YouTubeAudio;
import com.thesis.home.youlist.model.YouTubeVideo;
import com.thesis.home.youlist.services.VideoPreviewInfoCollector;
import com.thesis.home.youlist.services.VideoInfoCreator;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeVideoExtractor {
    // exceptions

    private String url;
    private VideoPreviewInfoCollector previewInfoCollector;

    public String getUrl() {
        return url;
    }

    // ----------------

    private static final String GET_VIDEO_INFO_URL =
            "https://www.youtube.com/get_video_info?video_id=%%video_id%%$$el_type$$&ps=default&eurl=&gl=US&hl=en";
    // eltype is nececeary for the url aboth
    private static final String EL_INFO = "el=info";

    private final Document doc;
    private JSONObject playerArgs;
    private boolean isAgeRestricted;
    private Map<String, String> videoInfoPage;

    // cached values
    private static volatile String decryptionCode = "";

    String pageUrl = "";

    private Downloader downloader;

    public YoutubeVideoExtractor(String pageUrl, Downloader dl) throws ExtractionException, IOException {
        previewInfoCollector = new VideoPreviewInfoCollector();
        downloader = dl;
        this.pageUrl = pageUrl;
        String pageContent = downloader.download(YoutubeHelper.cleanUrl(pageUrl));
        doc = Jsoup.parse(pageContent, pageUrl);
        JSONObject ytPlayerConfig;
        String playerUrl;

        // Check if the video is age restricted
        if (pageContent.contains("<meta property=\"og:restrictions:age")) {
            String videoInfoUrl = GET_VIDEO_INFO_URL.replace("%%video_id%%",
                    YoutubeHelper.getVideoId(pageUrl)).replace("$$el_type$$", "&" + EL_INFO);
            String videoInfoPageString = downloader.download(videoInfoUrl);
            videoInfoPage = Parser.compatParseMap(videoInfoPageString);
            playerUrl = Parser.getPlayerUrlFromRestrictedVideo(pageUrl,downloader);
            isAgeRestricted = true;
        } else {
            ytPlayerConfig = Parser.getPlayerConfig(pageContent,doc);
            playerArgs = Parser.getPlayerArgs(ytPlayerConfig);
            playerUrl = Parser.getPlayerUrl(ytPlayerConfig);
            isAgeRestricted = false;
        }

        if(decryptionCode.isEmpty()) {
            decryptionCode = Parser.loadDecryptionCode(playerUrl,downloader);
        }
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public String getTitle() throws ParsingException {
        try {
            if (playerArgs == null) {
                return videoInfoPage.get("title");
            }
            //json player args method
            return playerArgs.getString("title");
        } catch(JSONException je) {//html <meta> method
            je.printStackTrace();
            System.err.println("failed to load title from JSON args; trying to extract it from HTML");
            try { // fall through to fall-back
                return doc.select("meta[name=title]").attr("content");
            } catch (Exception e) {
                throw new ParsingException("failed permanently to load title.", e);
            }
        }
    }

    public int getAgeLimit() throws ParsingException {
        if (!isAgeRestricted) {
            return 0;
        }
        try {
            return Integer.valueOf(doc.head()
                    .getElementsByAttributeValue("property", "og:restrictions:age")
                    .attr("content").replace("+", ""));
        } catch (Exception e) {
            throw new ParsingException("Could not get age restriction");
        }
    }

    public List<YouTubeAudio> getAudioStreams() throws ParsingException {
        List<YouTubeAudio> youTubeAudios = new ArrayList<>();
        try{
            String encodedUrlMap;
            // playerArgs could be null if the video is age restricted
            if (playerArgs == null) {
                encodedUrlMap = videoInfoPage.get("adaptive_fmts");
            } else {
                encodedUrlMap = playerArgs.getString("adaptive_fmts");
            }
            for(String url_data_str : encodedUrlMap.split(",")) {
                // This loop iterates through multiple streams, therefor tags
                // is related to one and the same stream at a time.
                Map<String, String> tags = Parser.compatParseMap(
                        org.jsoup.parser.Parser.unescapeEntities(url_data_str, true));

                int itag = Integer.parseInt(tags.get("itag"));

                if (ItagItem.itagIsSupported(itag)) {
                    ItagItem itagItem = ItagItem.getItagItem(itag);
                    if (itagItem.itagType == ItagItem.ItagType.AUDIO) {
                        String streamUrl = tags.get("url");
                        // if video has a signature: decrypt it and add it to the url
                        if (tags.get("s") != null) {
                            streamUrl = streamUrl + "&signature="
                                    + Parser.decryptSignature(tags.get("s"), decryptionCode);
                        }

                        youTubeAudios.add(new YouTubeAudio(streamUrl,
                                itagItem.mediaFormatId,
                                itagItem.bandWidth,
                                itagItem.samplingRate));
                    }
                }
            }
        } catch (Exception e) {
            throw new ParsingException("Could not get audiostreams", e);
        }
        return youTubeAudios;
    }

    public List<YouTubeVideo> getVideoStreams() throws ParsingException {
        List<YouTubeVideo> youTubeVideos = new ArrayList<>();

        try{
            String encodedUrlMap;
            // playerArgs could be null if the video is age restricted
            if (playerArgs == null) {
                encodedUrlMap = videoInfoPage.get("url_encoded_fmt_stream_map");
            } else {
                encodedUrlMap = playerArgs.getString("url_encoded_fmt_stream_map");
            }
            for(String url_data_str : encodedUrlMap.split(",")) {
                try {
                    Map<String, String> tags = Parser.compatParseMap(
                            org.jsoup.parser.Parser.unescapeEntities(url_data_str, true));

                    int itag = Integer.parseInt(tags.get("itag"));

                    if (ItagItem.itagIsSupported(itag)) {
                        ItagItem itagItem = ItagItem.getItagItem(itag);
                        if(itagItem.itagType == ItagItem.ItagType.VIDEO) {
                            String streamUrl = tags.get("url");
                            // if video has a signature: decrypt it and add it to the url
                            if (tags.get("s") != null) {
                                streamUrl = streamUrl + "&signature="
                                        + Parser.decryptSignature(tags.get("s"), decryptionCode);
                            }
                            youTubeVideos.add(new YouTubeVideo(
                                    streamUrl,
                                    itagItem.mediaFormatId,
                                    itagItem.resolutionString));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not get videos.");
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            throw new ParsingException("Failed to get videos", e);
        }

        if(ListUtils.isEmptyList(youTubeVideos)) {
            throw new ParsingException("Failed to get any videos");
        }
        return youTubeVideos;
    }


   public String getThumbnailUrl() throws ParsingException {
        //first attempt getting a small image version
        //in the html extracting part we try to get a thumbnail with a higher resolution
        // Try to get high resolution thumbnail if it fails use low res from the player instead
        try {
            return doc.select("link[itemprop=\"thumbnailUrl\"]").first().attr("abs:href");
        } catch(Exception e) {
            System.err.println("Could not find high res Thumbnail. Using low res instead");
        }
        try { //fall through to fallback
            return playerArgs.getString("thumbnail_url");
        } catch (JSONException je) {
            throw new ParsingException(
                    "failed to extract thumbnail URL from JSON args; trying to extract it from HTML", je);
        } catch (NullPointerException ne) {
            // Get from the video info page instead
            return videoInfoPage.get("thumbnail_url");
        }
    }

    static String getThumbnailUrl(Element li){
        Element img = li.select("img").first();
        String thumbnailUrl = img.attr("abs:src");
        // Sometimes youtube sends links to gif files which somehow seem to not exist
        // anymore. Items with such gif also offer a secondary image source. So we are going
        // to use that if we caught such an item.
        if (thumbnailUrl.contains(".gif")) {
            thumbnailUrl = img.attr("data-thumb");
        }
        if (thumbnailUrl.startsWith("//")) {
            thumbnailUrl = "https:" + thumbnailUrl;
        }
        return thumbnailUrl;
    }

    public int getLength() throws ParsingException {
        try {
            if (playerArgs == null) {
                return Integer.valueOf(videoInfoPage.get("length_seconds"));
            }
            return playerArgs.getInt("length_seconds");
        } catch (JSONException e) {
            throw new ParsingException("failed to load video duration from JSON args", e);
        }
    }

    public String getUploaderThumbnailUrl() throws ParsingException {
        try {
            return doc.select("a[class*=\"yt-user-photo\"]").first()
                    .select("img").first()
                    .attr("abs:data-thumb");
        } catch (Exception e) {
            throw new ParsingException("failed to get uploader thumbnail URL.", e);
        }
    }

    public String getUploader() throws ParsingException {
        try {
            if (playerArgs == null) {
                return videoInfoPage.get("author");
            }
            //json player args method
            return playerArgs.getString("author");
        } catch(JSONException je) {
            je.printStackTrace();
            System.err.println(
                    "failed to load uploader name from JSON args; trying to extract it from HTML");
        } try {//fall through to fallback HTML method
            return doc.select("div.yt-user-info").first().text();
        } catch (Exception e) {
            throw new ParsingException("failed permanently to load uploader name.", e);
        }
    }

    public String getDescription() throws ParsingException {
        try {
            return doc.select("p[id=\"eow-description\"]").first().html();
        } catch (Exception e) {
            throw new ParsingException("failed to load description.", e);
        }
    }

    public long getViewCount() throws ParsingException {
        try {
            String viewCountString = doc.select("meta[itemprop=interactionCount]").attr("content");
            return Long.parseLong(viewCountString);
        } catch (Exception e) {
            throw new ParsingException("failed to get number of views", e);
        }
    }

    public String getUploadDate() throws ParsingException {
        try {
            return doc.select("meta[itemprop=datePublished]").attr("content");
        } catch (Exception e) {
            throw new ParsingException("failed to get upload date.", e);
        }
    }

    public int getTimeStamp() throws ParsingException {
        String timeStamp;
        try {
            timeStamp = Parser.matchGroup1("((#|&|\\?)t=\\d{0,3}h?\\d{0,3}m?\\d{1,3}s?)", pageUrl);
        } catch (Parser.RegexException e) {
            // catch this instantly since an url does not necessarily have to have a time stamp

            // -2 because well the testing system will then know its the regex that failed :/
            // not good i know
            return -2;
        }

        if(!timeStamp.isEmpty()) {
            try {
                String secondsString = "";
                String minutesString = "";
                String hoursString = "";
                try {
                    secondsString = Parser.matchGroup1("(\\d{1,3})s", timeStamp);
                    minutesString = Parser.matchGroup1("(\\d{1,3})m", timeStamp);
                    hoursString = Parser.matchGroup1("(\\d{1,3})h", timeStamp);
                } catch (Exception e) {
                    //it could be that time is given in another method
                    if (secondsString.isEmpty() //if nothing was got,
                            && minutesString.isEmpty()//treat as unlabelled seconds
                            && hoursString.isEmpty()) {
                        secondsString = Parser.matchGroup1("t=(\\d{1,3})", timeStamp);
                    }
                }

                int seconds = secondsString.isEmpty() ? 0 : Integer.parseInt(secondsString);
                int minutes = minutesString.isEmpty() ? 0 : Integer.parseInt(minutesString);
                int hours = hoursString.isEmpty() ? 0 : Integer.parseInt(hoursString);

                return seconds + (60 * minutes) + (3600 * hours);
                //Log.d(TAG, "derived timestamp value:"+ret);
                //the ordering varies internationally
            } catch (ParsingException e) {
                throw new ParsingException("Could not get timestamp.", e);
            }
        } else {
            return 0;
        }
    }

    public String getAverageRating() throws ParsingException {
        try {
            if (playerArgs == null) {
                return videoInfoPage.get("avg_rating");
            }
            return playerArgs.getString("avg_rating");
        } catch (JSONException e) {
            throw new ParsingException("Could not get Average rating", e);
        }
    }

    public int getLikeCount() throws ParsingException {
        String likesString = "";
        try {

            Element button = doc.select("button.like-button-renderer-like-button").first();
            try {
                likesString = button.select("span.yt-uix-button-content").first().text();
            } catch (NullPointerException e) {
                return -1;
            }
            return Integer.parseInt(likesString.replaceAll("[^\\d]", ""));
        } catch (NumberFormatException nfe) {
            throw new ParsingException(
                    "failed to parse likesString \"" + likesString + "\" as integers", nfe);
        } catch (Exception e) {
            throw new ParsingException("Could not get like count", e);
        }
    }

    public int getDislikeCount() throws ParsingException {
        String dislikesString = "";
        try {
            Element button = doc.select("button.like-button-renderer-dislike-button").first();
            try {
                dislikesString = button.select("span.yt-uix-button-content").first().text();
            } catch (NullPointerException e) {
                return -1;
            }
            return Integer.parseInt(dislikesString.replaceAll("[^\\d]", ""));
        } catch(NumberFormatException nfe) {
            throw new ParsingException(
                    "failed to parse dislikesString \"" + dislikesString + "\" as integers", nfe);
        } catch(Exception e) {
            throw new ParsingException("Could not get dislike count", e);
        }
    }

    public VideoInfoCreator getNextVideo() throws ParsingException {
        try {
            return Parser.extractVideoPreviewInfo(doc.select("div[class=\"watch-sidebar-section\"]").first()
                    .select("li").first());
        } catch(Exception e) {
            throw new ParsingException("Could not get next video", e);
        }
    }

    public VideoPreviewInfoCollector getRelatedVideos() throws ParsingException {

        VideoPreviewInfoCollector collector = new VideoPreviewInfoCollector();

        try {
            for (Element li : doc.select("ul[id=\"watch-related\"]").first().children()) {

                if (li.select("a[class*=\"content-link\"]").first() != null) {
                    collector.commit(Parser.extractVideoPreviewInfo(li));
                }
            }
            return collector;
        } catch(Exception e) {
            throw new ParsingException("Could not get related videos", e);
        }
    }
}