package com.thesis.home.youlist.activity;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.thesis.home.youlist.R;

import java.io.File;



/**
 * Created by HOME on 15/5/2016.
 */
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        initSettings(this);
    }

    public static void initSettings(Context context) {
        PreferenceManager.setDefaultValues(context, R.xml.config, false);
        getVideoDownloadFolder(context);
        getAudioDownloadFolder(context);
    }

    public static File getVideoDownloadFolder(Context context) {
        return getFolder(context, R.string.dl_path_video, Environment.DIRECTORY_MOVIES);
    }

    public static File getAudioDownloadFolder(Context context) {
        return getFolder(context, R.string.dl_path_audio, Environment.DIRECTORY_MUSIC);
    }

    private static File getFolder(Context context, int keyID, String defaultDirectoryName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = context.getString(keyID);
        String downloadPath = prefs.getString(key, null);
        if ((downloadPath != null) && (!downloadPath.isEmpty())) return new File(downloadPath.trim());

        final File folder = getFolder(defaultDirectoryName);
        SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.putString(key
                , new File(folder,"YouList").getAbsolutePath());
        spEditor.apply();
        return folder;
    }

    private static File getFolder(String defaultDirectoryName) {
        return new File(Environment.getExternalStorageDirectory(),defaultDirectoryName);
    }
}