package com.thesis.home.youlist.model;

public enum MediaFormat {
    //video and audio combined formats
    //           id      name    suffix  mime type
    MPEG_4      (0x0,   "MPEG-4", "mp4", "video/mp4"),
    v3GPP       (0x1,   "3GPP",   "3gp", "video/3gpp"),
    WEBM        (0x2,   "WebM",  "webm", "video/webm"),
    // audio formats
    M4A         (0x3,   "m4a",   "m4a",  "audio/mp4"),
    WEBMA       (0x4,   "WebM",  "webm", "audio/webm");

    public final int id;
    @SuppressWarnings("WeakerAccess")
    public final String name;
    @SuppressWarnings("WeakerAccess")
    public final String suffix;
    public final String mimeType;

    MediaFormat(int id, String name, String suffix, String mimeType) {
        this.id = id;
        this.name = name;
        this.suffix = suffix;
        this.mimeType = mimeType;
    }

    public static String getNameById(int ident) {
        for (MediaFormat vf : MediaFormat.values()) {
            if(vf.id == ident) return vf.name;
        }
        return "";
    }

    public static String getSuffixById(int ident) {
        for (MediaFormat vf : MediaFormat.values()) {
            if(vf.id == ident) return vf.suffix;
        }
        return "";
    }

    public static String getMimeById(int ident) {
        for (MediaFormat vf : MediaFormat.values()) {
            if(vf.id == ident) return vf.mimeType;
        }
        return "";
    }
}
