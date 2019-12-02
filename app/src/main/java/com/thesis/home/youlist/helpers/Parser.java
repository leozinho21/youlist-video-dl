package com.thesis.home.youlist.helpers;

import com.thesis.home.youlist.exceptions.ParsingException;
import com.thesis.home.youlist.model.Log;
import com.thesis.home.youlist.model.VideoInfo;
import com.thesis.home.youlist.model.YouTubeAudio;
import com.thesis.home.youlist.model.YouTubeVideo;
import com.thesis.home.youlist.services.VideoInfoCreator;
import com.thesis.home.youlist.services.VideoPreviewInfoCollector;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    private Parser() {
    }

    public static class RegexException extends ParsingException {
        public RegexException(String message) {
            super(message);
        }
    }

    public static String matchGroup1(String pattern, String input) throws RegexException {
        Pattern pat = Pattern.compile(pattern);
        Matcher mat = pat.matcher(input);
        boolean foundMatch = mat.find();
        if (foundMatch) {
            return mat.group(1);
        }
        else {
            throw new RegexException("failed to find pattern \""+pattern+" inside of "+input+"\"");
        }
    }

    public static Map<String, String> compatParseMap(final String input) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        for(String arg : input.split("&")) {
            String[] splitArg = arg.split("=");
            if(splitArg.length > 1) {
                map.put(splitArg[0], URLDecoder.decode(splitArg[1], "UTF-8"));
            } else {
                map.put(splitArg[0], "");
            }
        }
        return map;
    }


    static JSONObject getPlayerConfig(String pageContent,Document doc) throws ParsingException {
        try {
            String ytPlayerConfigRaw =
                    Parser.matchGroup1("ytplayer.config\\s*=\\s*(\\{.*?\\});", pageContent);
            return new JSONObject(ytPlayerConfigRaw);
        } catch (Parser.RegexException e) {
            String errorReason = findErrorReason(doc);
            switch(errorReason) {
                case "":
                    Log.d("Content not available: player config empty", e);
                default:
                    Log.d("Content not available", e);

            }
            return new JSONObject();
        } catch (JSONException e) {
            throw new ParsingException("Could not parse yt player config", e);
        }
    }

    static JSONObject getPlayerArgs(JSONObject playerConfig) throws ParsingException {
        JSONObject playerArgs;

        //attempt to load the youtube js player JSON arguments
        boolean isLiveStream = false; //used to determine if this is a livestream or not
        try {
            playerArgs = playerConfig.getJSONObject("args");

            // check if we have a live stream. We need to filter it, since its not yet supported.
            if((playerArgs.has("ps") && playerArgs.get("ps").toString().equals("live"))
                    || (playerArgs.get("url_encoded_fmt_stream_map").toString().isEmpty())) {
                isLiveStream = true;
            }
        }  catch (JSONException e) {
            throw new ParsingException("Could not parse yt player config", e);
        }
        if (isLiveStream) {
            Log.d("This is a Live stream. Can't view those.");
        }

        return playerArgs;
    }

    static String getPlayerUrl(JSONObject playerConfig) throws ParsingException {
        try {
            // The Youtube service needs to be initialized by downloading the
            // js-Youtube-player. This is done in order to get the algorithm
            // for decrypting cryptic signatures inside certain stream urls.
            String playerUrl = "";

            JSONObject ytAssets = playerConfig.getJSONObject("assets");
            playerUrl = ytAssets.getString("js");

            if (playerUrl.startsWith("//")) {
                playerUrl = "https:" + playerUrl;
            }
            return playerUrl;
        } catch (JSONException e) {
            throw new ParsingException(
                    "Could not load decryption code for the Youtube service.", e);
        }
    }

    static String getPlayerUrlFromRestrictedVideo(String pageUrl,Downloader downloader) throws ParsingException {
        try {
            String playerUrl = "";
            String videoId = YoutubeHelper.getVideoId(pageUrl);
            String embedUrl = "https://www.youtube.com/embed/" + videoId;
            String embedPageContent = downloader.download(embedUrl);
            Pattern assetsPattern = Pattern.compile("\"assets\":.+?\"js\":\\s*(\"[^\"]+\")");
            Matcher patternMatcher = assetsPattern.matcher(embedPageContent);
            while (patternMatcher.find()) {
                playerUrl = patternMatcher.group(1);
            }
            playerUrl = playerUrl.replace("\\", "").replace("\"", "");

            if (playerUrl.startsWith("//")) {
                playerUrl = "https:" + playerUrl;
            }
            return playerUrl;
        } catch (IOException e) {
            throw new ParsingException(
                    "Could load decryption code form restricted video for the Youtube service.", e);
        }
    }
    public static VideoInfo.StreamType getStreamType() throws ParsingException {
        return VideoInfo.StreamType.VIDEO_STREAM;
    }

    /**Provides information about links to other videos on the video page, such as related videos.
     * This is encapsulated in a VideoPreviewInfo object,
     * which is a subset of the fields in a full VideoInfo.*/
    static VideoInfoCreator extractVideoPreviewInfo(final Element li) {
        VideoInfoCreator ytVideoInfo = new VideoInfoCreator(li);
        try {
            ytVideoInfo.setDuration(YoutubeHelper.parseDurationString(
                    li.select("span.video-time").first().text()));
        } catch (ParsingException e) {
            e.printStackTrace();
            ytVideoInfo.setDuration(0);
        }
        ytVideoInfo.setThumbnailUrl(YoutubeVideoExtractor.getThumbnailUrl(li));
        ytVideoInfo.setTitle(li.select("span.title").first().text());
        ytVideoInfo.setUploader(li.select("span.g-hovercard").first().text());
        ytVideoInfo.setWebPageUrl(li.select("a.content-link").first().attr("abs:href"));
        try {
            ytVideoInfo.setViewCount( Long.parseLong(li.select("span.view-count")
                    .first().text().replaceAll("[^\\d]", "")));
        } catch (Exception e) {
            //related videos sometimes have no view count
            ytVideoInfo.setViewCount(0);
        }

        return ytVideoInfo;
    }

    static String loadDecryptionCode(String playerUrl,Downloader downloader) throws DecryptException {
        String decryptionFuncName;
        String decryptionFunc;
        String helperObjectName;
        String helperObject;
        String callerFunc = "function decrypt(a){return %%(a);}";
        String decryptionCode;

        try {
            String playerCode = downloader.download(playerUrl);

            decryptionFuncName =
                    Parser.matchGroup1("\\.sig\\|\\|([a-zA-Z0-9$]+)\\(", playerCode);

            String functionPattern = "("
                    + decryptionFuncName.replace("$", "\\$")
                    + "=function\\([a-zA-Z0-9_]+\\)\\{.+?\\})";
            decryptionFunc = "var " + Parser.matchGroup1(functionPattern, playerCode) + ";";

            helperObjectName = Parser
                    .matchGroup1(";([A-Za-z0-9_\\$]{2})\\...\\(", decryptionFunc);

            String helperPattern = "(var "
                    + helperObjectName.replace("$", "\\$") + "=\\{.+?\\}\\};)";
            helperObject = Parser.matchGroup1(helperPattern, playerCode);


            callerFunc = callerFunc.replace("%%", decryptionFuncName);
            decryptionCode = helperObject + decryptionFunc + callerFunc;
        } catch(IOException ioe) {
            throw new DecryptException("Could not load decrypt function", ioe);
        } catch(Exception e) {
            throw new DecryptException("Could not parse decrypt function ", e);
        }

        return decryptionCode;
    }

    static String decryptSignature(String encryptedSig, String decryptionCode)
            throws DecryptException {
        Context context = Context.enter();
        context.setOptimizationLevel(-1);
        Object result = null;
        try {
            ScriptableObject scope = context.initStandardObjects();
            context.evaluateString(scope, decryptionCode, "decryptionCode", 1, null);
            Function decryptionFunc = (Function) scope.get("decrypt", scope);
            result = decryptionFunc.call(context, scope, scope, new Object[]{encryptedSig});
        } catch (Exception e) {
            throw new DecryptException("could not get decrypt signature", e);
        } finally {
            Context.exit();
        }
        return result == null ? "" : result.toString();
    }

    static String findErrorReason(Document doc) {
        return doc.select("h1[id=\"unavailable-message\"]").first().text();
    }

    public static class DecryptException extends ParsingException {
        DecryptException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
