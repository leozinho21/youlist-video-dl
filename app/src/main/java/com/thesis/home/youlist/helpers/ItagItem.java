package com.thesis.home.youlist.helpers;

import com.thesis.home.youlist.exceptions.ParsingException;
import com.thesis.home.youlist.model.MediaFormat;

/**
 * Created by HOME on 31/8/2016.
 */
public class ItagItem {

    public enum ItagType {
        AUDIO,
        VIDEO,
        VIDEO_ONLY
    }

    public int id;
    public ItagType itagType;
    public int mediaFormatId;
    public String resolutionString = null;
    public int fps = -1;
    public int samplingRate = -1;
    public int bandWidth = -1;


    public ItagItem(int id, ItagType type, MediaFormat format, String res, int fps) {
        this.id = id;
        this.itagType = type;
        this.mediaFormatId = format.id;
        this.resolutionString = res;
        this.fps = fps;
    }
    public ItagItem(int id, ItagType type, MediaFormat format, int samplingRate, int bandWidth) {
        this.id = id;
        this.itagType = type;
        this.mediaFormatId = format.id;
        this.samplingRate = samplingRate;
        this.bandWidth = bandWidth;
    }

    private static final ItagItem[] itagList = {
            // video streams
            //           id, ItagType,       MediaFormat,    Resolution,    fps
            new ItagItem(17, ItagType.VIDEO, MediaFormat.v3GPP, "144p",     12),
            new ItagItem(18, ItagType.VIDEO, MediaFormat.MPEG_4,"360p",     24),
            new ItagItem(22, ItagType.VIDEO, MediaFormat.MPEG_4,"720p",     24),
            new ItagItem(36, ItagType.VIDEO, MediaFormat.v3GPP, "240p",     24),
            new ItagItem(37, ItagType.VIDEO, MediaFormat.MPEG_4,"1080p",    24),
            new ItagItem(38, ItagType.VIDEO, MediaFormat.MPEG_4,"1080p",    24),
            new ItagItem(43, ItagType.VIDEO, MediaFormat.WEBM,  "360p",     24),
            new ItagItem(44, ItagType.VIDEO, MediaFormat.WEBM,  "480p",     24),
            new ItagItem(45, ItagType.VIDEO, MediaFormat.WEBM,  "720p",     24),
            new ItagItem(46, ItagType.VIDEO, MediaFormat.WEBM,  "1080p",    24),
            // audio streams
            //           id, ItagType,       MediaFormat,    samplingR, bandwidth
            new ItagItem(249, ItagType.AUDIO, MediaFormat.WEBMA, 0, 0),  // bandwith/samplingR 0 because not known
            new ItagItem(250, ItagType.AUDIO, MediaFormat.WEBMA, 0, 0),
            new ItagItem(171, ItagType.AUDIO, MediaFormat.WEBMA, 0, 0),
            new ItagItem(140, ItagType.AUDIO, MediaFormat.M4A,   0, 0),
            new ItagItem(251, ItagType.AUDIO, MediaFormat.WEBMA, 0, 0),
            // video only streams
            new ItagItem(160, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "144p", 24),
            new ItagItem(133, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "240p", 24),
            new ItagItem(134, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "360p", 24),
            new ItagItem(135, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "480p", 24),
            new ItagItem(136, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "720p", 24),
            new ItagItem(137, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "1080p", 24),
    };

    /**These lists only contain itag formats that are supported by the common Android Video player.
     However if you are looking for a list showing all itag formats, look at
     https://github.com/rg3/youtube-dl/issues/1687 */

    public static boolean itagIsSupported(int itag) {
        for(ItagItem item : itagList) {
            if(itag == item.id) {
                return true;
            }
        }
        return false;
    }

    public static ItagItem getItagItem(int itag) throws ParsingException {
        for(ItagItem item : itagList) {
            if(itag == item.id) {
                return item;
            }
        }
        throw new ParsingException("itag=" + Integer.toString(itag) + " not supported");
    }
}
