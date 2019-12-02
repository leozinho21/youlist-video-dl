package com.thesis.home.youlist.helpers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.thesis.home.youlist.R;
import com.thesis.home.youlist.activity.ConfigActivity;
import com.thesis.home.youlist.model.MediaFormat;
import com.thesis.home.youlist.model.YouTubeVideo;

import java.util.List;


public class ActionBarHandler {
    private static final String TAG = ActionBarHandler.class.toString();

    private AppCompatActivity activity;
    private int selectedVideoStream = -1;

    private SharedPreferences defaultPreferences = null;

    private Menu menu;

    // Only callbacks are listed here
    private OnActionListener onShareListener = null;
    private OnActionListener onOpenInBrowserListener = null;
    private OnActionListener onDownloadListener = null;
    private OnActionListener onPlayAudioListener = null;


    // Triggered when a stream related action is triggered.
    public interface OnActionListener {
        void onActionSelected(int selectedStreamId);
    }

    public ActionBarHandler(AppCompatActivity activity) {
        this.activity = activity;
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public void setupNavMenu(AppCompatActivity activity) {
        this.activity = activity;
        try {
            activity.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void setupStreamList(final List<YouTubeVideo> youTubeVideos) {
        if (activity != null) {
            selectedVideoStream = 0;

            int videos = youTubeVideos != null? youTubeVideos.size() : 0;
            // this array will be shown in the dropdown menu for selecting the stream/resolution.
            String[] itemArray = new String[videos];
            for (int i = 0; i < videos; i++) {
                YouTubeVideo item = youTubeVideos.get(i);
                itemArray[i] = MediaFormat.getNameById(item.format) + " " + item.resolution;
            }
            int defaultResolution = getDefaultResolution(youTubeVideos);

            ArrayAdapter<String> itemAdapter = new ArrayAdapter<>(activity.getBaseContext(),
                    android.R.layout.simple_spinner_dropdown_item, itemArray);

            ActionBar ab = activity.getSupportActionBar();

            assert ab != null : "Could not get actionbar";
            ab.setListNavigationCallbacks(itemAdapter
                    , new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                    selectedVideoStream = (int) itemId;
                    return true;
                }
            });

            ab.setSelectedNavigationItem(defaultResolution);
        }
    }


    private int getDefaultResolution(final List<YouTubeVideo> youTubeVideos) {
        String defaultResolution = defaultPreferences
                .getString(activity.getString(R.string.def_res),
                        activity.getString(R.string.def_res_value));
        int videos = youTubeVideos != null? youTubeVideos.size() : 0;
        for (int i = 0; i < videos; i++) {
            YouTubeVideo item = youTubeVideos.get(i);
            if (defaultResolution.equals(item.resolution)) {
                return i;
            }
        }
        return 0;
    }

    public void setupMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
        defaultPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        inflater.inflate(R.menu.videoitem_detail, menu);

    }

    public boolean onItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_item_share: {
                if(onShareListener != null) {
                    onShareListener.onActionSelected(selectedVideoStream);
                }
                return true;
            }
            case R.id.menu_item_openInBrowser: {
                if(onOpenInBrowserListener != null) {
                    onOpenInBrowserListener.onActionSelected(selectedVideoStream);
                }
            }
            return true;
            case R.id.menu_item_download:
                if(onDownloadListener != null) {
                    onDownloadListener.onActionSelected(selectedVideoStream);
                }
                return true;
            case R.id.action_settings: {
                Intent intent = new Intent(activity, ConfigActivity.class);
                activity.startActivity(intent);
                return true;
            }
            case R.id.menu_item_play_audio:
                if(onPlayAudioListener != null) {
                    onPlayAudioListener.onActionSelected(selectedVideoStream);
                }
                return true;
            default:
                Log.e(TAG, "Menu Item not known");
        }
        return false;
    }

    public int getSelectedVideoStream() {
        return selectedVideoStream;
    }

    public void setOnShareListener(OnActionListener listener) {
        onShareListener = listener;
    }

    public void setOnOpenInBrowserListener(OnActionListener listener) {
        onOpenInBrowserListener = listener;
    }

    public void setOnDownloadListener(OnActionListener listener) {
        onDownloadListener = listener;
    }

    public void setOnPlayAudioListener(OnActionListener listener) {
        onPlayAudioListener = listener;
    }

    public void showAudioAction(boolean visible) {
        menu.findItem(R.id.menu_item_play_audio).setVisible(visible);
    }

}
