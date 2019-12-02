package com.thesis.home.youlist.helpers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.thesis.home.youlist.R;
import com.thesis.home.youlist.activity.MainActivity;
import com.thesis.home.youlist.exceptions.ExtractionException;
import com.thesis.home.youlist.exceptions.ParsingException;
import com.thesis.home.youlist.model.AbstractVideoInfo;
import com.thesis.home.youlist.model.VideoInfo;
import com.thesis.home.youlist.services.VideoPreviewInfoCollector;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by HOME on 9/3/2016.
 */
public class YoutubeHelper {

    public static final String UPLOAD_VIDEO_TYPE        = "upload_video_type";
    public static final String WATCH_HISTORY_VIDEO_TYPE = "watch_history_video_type";
    public static final String WATCH_LATER_VIDEO_TYPE   = "watch_later_video_type";
    public static final String TRENDING_VIDEO_TYPE      = "trending_video_type";

    public static String getDataSource(String path) throws IOException {
        if (!URLUtil.isNetworkUrl(path)) {
            return path;
        } else {
            URL url = new URL(path);
            URLConnection cn = url.openConnection();
            cn.connect();
            InputStream stream = cn.getInputStream();
            if (stream == null)
                throw new RuntimeException("stream is null");
            File temp = File.createTempFile("mediaplayertmp", "dat");
            temp.deleteOnExit();
            String tempPath = temp.getAbsolutePath();
            FileOutputStream out = new FileOutputStream(temp);
            byte buf[] = new byte[128];
            do {
                int numread = stream.read(buf);
                if (numread <= 0)
                    break;
                out.write(buf, 0, numread);
            } while (true);
            try {
                stream.close();
                out.close();
            } catch (IOException ex) {
                  Log.e(MainActivity.class.getName(), "error: " + ex.getMessage(), ex);
            }
            return tempPath;
        }
    }

    public String getRTSPVideoUrl(String urlYoutube) {
        try {
            String gdy = "http://ssyoutube.com/watch?v=";
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
            String id = extractYoutubeId(urlYoutube);
            URL url = new URL(gdy + id);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            Document doc = dBuilder.parse(connection.getInputStream());
            Element el = doc.getDocumentElement();
            NodeList list = el.getElementsByTagName("media:content");
            String cursor = urlYoutube;
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (node != null) {
                    NamedNodeMap nodeMap = node.getAttributes();
                    HashMap<String, String> maps = new HashMap<String, String>();
                    for (int j = 0; j < nodeMap.getLength(); j++) {
                        Attr att = (Attr) nodeMap.item(j);
                        maps.put(att.getName(), att.getValue());
                    }
                    if (maps.containsKey("yt:format")) {
                        String f = maps.get("yt:format");
                        if (maps.containsKey("url"))
                            cursor = maps.get("url");
                        if (f.equals("1"))
                            return cursor;
                    }
                }
            }
            return cursor;
        } catch (Exception ex) {
            return urlYoutube;
        }
    }
    public static String extractYoutubeId(String url) throws MalformedURLException
    {
        String id = null;
        try
        {
            String query = new URL(url).getQuery();
            if (query != null)
            {
                String[] param = query.split("&");
                for (String row : param)
                {
                    String[] param1 = row.split("=");
                    if (param1[0].equals("v"))
                    {
                        id = param1[1];
                    }
                }
            }
            else
            {
                if (url.contains("embed"))
                {
                    id = url.substring(url.lastIndexOf("/") + 1);
                }
            }
        }
        catch (Exception ex)
        {
        }
        return id;
    }
    public static Bitmap getRoundedShape(Bitmap scaleBitmapImage) {
        int targetWidth = 100;
        int targetHeight = 100;
        Bitmap targetBitmap = Bitmap.createBitmap(targetWidth,
                targetHeight, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(targetBitmap);
        Path path = new Path();
        path.addCircle(((float) targetWidth - 1) / 2,
                ((float) targetHeight - 1) / 2,
                (Math.min(((float) targetWidth),
                        ((float) targetHeight)) / 2),
                Path.Direction.CCW);

        canvas.clipPath(path);
        Bitmap sourceBitmap = scaleBitmapImage;
        canvas.drawBitmap(sourceBitmap,
                new Rect(0, 0, sourceBitmap.getWidth(),
                        sourceBitmap.getHeight()),
                new Rect(0, 0, targetWidth, targetHeight), null);
        return targetBitmap;
    }

    public void setProfileInfo(GoogleApiClient mGoogleApiClient,View v) {
        //not sure if mGoogleapiClient.isConnect is appropriate...
        if (!mGoogleApiClient.isConnected() || Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) == null) {
            ((ImageView) v.findViewById(R.id.imgProfilePic))
                    .setImageDrawable(null);
            ((TextView) v.findViewById(R.id.display_name))
                    .setText(R.string.not_signed_in);
        } else {
            Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            if (currentPerson.hasImage()) {
                // Set the URL of the image that should be loaded into this view, and
                // specify the ImageLoader that will be used to make the request.
            }
            if (currentPerson.hasDisplayName()) {
                ((TextView) v.findViewById(R.id.display_name))
                        .setText(currentPerson.getDisplayName());
            }
        }
    }
    public static int parseDurationString(String input)
            throws ParsingException, NumberFormatException {
        String[] splitInput = input.split(":");
        String days = "0";
        String hours = "0";
        String minutes = "0";
        String seconds;

        switch(splitInput.length) {
            case 4:
                days = splitInput[0];
                hours = splitInput[1];
                minutes = splitInput[2];
                seconds = splitInput[3];
                break;
            case 3:
                hours = splitInput[0];
                minutes = splitInput[1];
                seconds = splitInput[2];
                break;
            case 2:
                minutes = splitInput[0];
                seconds = splitInput[1];
                break;
            case 1:
                seconds = splitInput[0];
                break;
            default:
                throw new ParsingException("Error duration string with unknown format: " + input);
        }
        return ((((Integer.parseInt(days) * 24)
                + Integer.parseInt(hours) * 60)
                + Integer.parseInt(minutes)) * 60)
                + Integer.parseInt(seconds);
    }

    public static String getVideoUrl(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    public static String getVideoId(String url) throws ParsingException {
        String id;

        if(url.contains("youtube")) {
            if(url.contains("attribution_link")) {
                try {
                    String escapedQuery = Parser.matchGroup1("u=(.[^&|$]*)", url);
                    String query = URLDecoder.decode(escapedQuery, "UTF-8");
                    id = Parser.matchGroup1("v=([\\-a-zA-Z0-9_]{11})", query);
                } catch(UnsupportedEncodingException uee) {
                    throw new ParsingException("Could not parse attribution_link", uee);
                }
            } else {
                id = Parser.matchGroup1("[?&]v=([\\-a-zA-Z0-9_]{11})", url);
            }
        }
        else if(url.contains("youtu.be")) {
            if(url.contains("v=")) {
                id = Parser.matchGroup1("v=([\\-a-zA-Z0-9_]{11})", url);
            } else {
                id = Parser.matchGroup1("youtu\\.be/([a-zA-Z0-9_-]{11})", url);
            }
        }
        else {
            throw new ParsingException("Error no suitable url: " + url);
        }


        if(!id.isEmpty()){
            return id;
        } else {
            throw new ParsingException("Error could not parse url: " + url);
        }
    }

    public static String cleanUrl(String complexUrl) throws ParsingException {
        return getVideoUrl(getVideoId(complexUrl));
    }

    public static boolean acceptUrl(String videoUrl) {
        return videoUrl.contains("youtube") ||
                videoUrl.contains("youtu.be");
    }

    public static String getWebPageUrl(org.jsoup.nodes.Element item) throws ParsingException {

        try {
            org.jsoup.nodes.Element el = item.select("div[class*=\"yt-lockup-video\"").first();
            org.jsoup.nodes.Element dl = el.select("h3").first().select("a").first();
            return dl.attr("abs:href");
        } catch (Exception e) {
            throw new ParsingException("Could not get web page url for the video", e);
        }
    }

    public static String getTitle(org.jsoup.nodes.Element item) throws ParsingException {
        try {
            org.jsoup.nodes.Element el = item.select("div[class*=\"yt-lockup-video\"").first();
            org.jsoup.nodes.Element dl = el.select("h3").first().select("a").first();
            return dl.text();
        } catch (Exception e) {
            throw new ParsingException("Could not get title", e);
        }
    }

    public static int getDuration(org.jsoup.nodes.Element item) throws ParsingException {

        try {
            return YoutubeHelper.parseDurationString(
                    item.select("span[class=\"video-time\"]").first().text());
        } catch(Exception e) {
            if(isLiveStream(item)) {
                // -1 for no duration
                return -1;
            } else {
                throw new ParsingException("Could not get Duration: " + getTitle(item), e);
            }


        }
    }

    public static String getUploader(org.jsoup.nodes.Element item) throws ParsingException {

        try {
            return item.select("div[class=\"yt-lockup-byline\"]").first()
                    .select("a").first()
                    .text();
        } catch (Exception e) {
            throw new ParsingException("Could not get uploader", e);
        }
    }

    public static String getUploadDate(org.jsoup.nodes.Element item) throws ParsingException {

        try {
            String uploadDate = "";
            if(item.select("div[class=\"yt-lockup-meta\"]").first() != null){
                uploadDate = item.select("div[class=\"yt-lockup-meta\"]").first()
                        .select("li").first()
                        .text();
            }
            return uploadDate;
        } catch(Exception e) {
            throw new ParsingException("Could not get uplaod date", e);
        }
    }

    public static long getViewCount(org.jsoup.nodes.Element item) throws ParsingException {

        String output;
        String input;
        try {
            input = item.select("div[class=\"yt-lockup-meta\"]").first()
                    .select("li").get(1)
                    .text();
        } catch (IndexOutOfBoundsException e) {
            if(isLiveStream(item)) {
                // -1 for no view count
                return -1;
            } else {
                throw new ParsingException(
                        "Could not parse yt-lockup-meta although available: " + getTitle(item), e);
            }
        }

        output = Parser.matchGroup1("([0-9,\\. ]*)", input)
                .replace(" ", "")
                .replace(".", "")
                .replace(",", "");

        try {
            return Long.parseLong(output);
        } catch (NumberFormatException e) {
            // if this happens the video probably has no views
            if(!input.isEmpty()) {
                return 0;
            } else {
                throw new ParsingException("Could not handle input: " + input, e);
            }
        }
    }

    public static String getThumbnailUrl(org.jsoup.nodes.Element item) throws ParsingException {

        try {
            String url;
            org.jsoup.nodes.Element te = item.select("div[class=\"yt-thumb video-thumb\"]").first()
                    .select("img").first();
            url = te.attr("abs:src");

            if (url.contains(".gif")) {
                url = te.attr("abs:data-thumb");
            }
            return url;
        } catch (Exception e) {
            throw new ParsingException("Could not get thumbnail url", e);
        }
    }

    public static AbstractVideoInfo.StreamType getStreamType(org.jsoup.nodes.Element item) {

        if(isLiveStream(item)) {
            return AbstractVideoInfo.StreamType.LIVE_STREAM;
        } else {
            return AbstractVideoInfo.StreamType.VIDEO_STREAM;
        }
    }

    private static boolean isLiveStream(org.jsoup.nodes.Element item) {
        org.jsoup.nodes.Element bla = item.select("span[class*=\"yt-badge-live\"]").first();

        if(bla == null) {
            if(item.select("span[class*=\"video-time\"]").first() == null) {
                return true;
            }
        }
        return bla != null;
    }

    public static String shortViewCount(Long viewCount){
        if(viewCount >= 1000000000){
            return Long.toString(viewCount/1000000000)+"B views";
        }else if(viewCount>=1000000){
            return Long.toString(viewCount/1000000)+"M views";
        }else if(viewCount>=1000){
            return Long.toString(viewCount/1000)+"K views";
        }else {
            return Long.toString(viewCount)+" views";
        }
    }

    public static String getDurationString(int duration) {
        String output = "";
        int days = duration / (24 * 60 * 60); /* greater than a day */
        duration %= (24 * 60 * 60);
        int hours = duration / (60 * 60); /* greater than an hour */
        duration %= (60 * 60);
        int minutes = duration / 60;
        int seconds = duration % 60;

        //handle days
        if(days > 0) {
            output = Integer.toString(days) + ":";
        }
        // handle hours
        if(hours > 0 || !output.isEmpty()) {
            if(hours > 0) {
                if(hours >= 10 || output.isEmpty()) {
                    output += Integer.toString(minutes);
                } else {
                    output += "0" + Integer.toString(minutes);
                }
            } else {
                output += "00";
            }
            output += ":";
        }
        //handle minutes
        if(minutes > 0 || !output.isEmpty()) {
            if(minutes > 0) {
                if(minutes >= 10 || output.isEmpty()) {
                    output += Integer.toString(minutes);
                } else {
                    output += "0" + Integer.toString(minutes);
                }
            } else {
                output += "00";
            }
            output += ":";
        }

        //handle seconds
        if(output.isEmpty()) {
            output += "0:";
        }

        if(seconds >= 10) {
            output += Integer.toString(seconds);
        } else {
            output += "0" + Integer.toString(seconds);
        }

        return output;
    }

}
