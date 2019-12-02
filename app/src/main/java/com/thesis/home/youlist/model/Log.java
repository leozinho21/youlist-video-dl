package com.thesis.home.youlist.model;

import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Message;

public class Log {
 
    private static final boolean live = false;
     
    private static final String USER_FEED_TAG 	= "UserFeedYouTubeTut";
    private static final String ERROR_TAG 		= "ERROR_TAG";
    private static final String JSON_ERROR_TAG 	= "JSON_ERROR_TAG";
    
    public static void d(String msg){
        d(msg, null);
    }
     
    public static void d(String msg, Throwable e){
        if(!live)
            android.util.Log.d(USER_FEED_TAG, Thread.currentThread().getName() +"| "+ msg, e);
    }
     
    public static void i(String msg){
        i(msg, null);
    }
     
    public static void i(String msg, Throwable e){
        if(!live)
            android.util.Log.i(USER_FEED_TAG, Thread.currentThread().getName() +"| "+ msg, e);
    }
     
    public static void e(String msg){
        e(msg, null);
    }
     
    public static void e(String msg, Throwable e){
        if(!live)
            android.util.Log.e(USER_FEED_TAG, Thread.currentThread().getName() +"| "+ msg, e);
    }
}