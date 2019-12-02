package com.thesis.home.youlist.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.github.pedrovgs.DraggableListener;
import com.github.pedrovgs.DraggablePanel;
import com.thesis.home.youlist.R;
import com.thesis.home.youlist.adapters.NewVideoListAdapter;
import com.thesis.home.youlist.fragments.PlayVideoFragment;
import com.thesis.home.youlist.fragments.VideoItemDetailFragment;
import com.thesis.home.youlist.helpers.ActionBarHandler;
import com.thesis.home.youlist.helpers.Downloader;
import com.thesis.home.youlist.helpers.ListUtils;
import com.thesis.home.youlist.helpers.YoutubeHelper;
import com.thesis.home.youlist.model.MediaFormat;
import com.thesis.home.youlist.helpers.YoutubeVideoExtractor;
import com.thesis.home.youlist.model.Constants;
import com.thesis.home.youlist.model.VideoInfo;
import com.thesis.home.youlist.model.YouTubeVideo;
import com.thesis.home.youlist.players.BackgroundPlayer;
import com.thesis.home.youlist.model.YouTubeAudio;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;

public class VideoDetailActivity extends AppCompatActivity implements VideoItemDetailFragment.Callbacks{

    private static final String TAG = VideoDetailActivity.class.toString();

    @Bind(R.id.draggable_panel)
    DraggablePanel draggablePanel;

    private VideoItemDetailFragment fragment;
    private PlayVideoFragment       playVideoFragment;
    private ActionBarHandler        actionBarHandler;

    private NewVideoListAdapter listAdapter;
    private String videoUrl;
    private boolean isRefresh           = false;
    private ProgressDialog mProgressDialog;

    private VideoInfo videoInfo = null;
    private boolean isLandscape = true;

    private int start_position  = 0;


    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public interface OnInvokeCreateOptionsMenuListener {
        void createOptionsMenu();
    }

    private OnInvokeCreateOptionsMenuListener onInvokeCreateOptionsMenuListener = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videoitem_detail);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        isLandscape = checkIfLandscape();
        try {
            //noinspection ConstantConditions
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch(Exception e) {
            Log.d(TAG, "Could not get SupportActionBar");
            e.printStackTrace();
        }

        Bundle arguments = new Bundle();
        if (savedInstanceState == null) {
            showProgressDialog();

            // this means the video was called though another app
            if (getIntent().getData() != null) {
                videoUrl = getIntent().getData().toString();

                arguments.putString(VideoItemDetailFragment.VIDEO_URL, videoUrl);

                arguments.putBoolean(VideoItemDetailFragment.AUTO_PLAY,
                        PreferenceManager.getDefaultSharedPreferences(this)
                                .getBoolean(getString(R.string.autoplay_through_intent_key), false));
            } else {
                videoUrl = getIntent().getStringExtra(VideoItemDetailFragment.VIDEO_URL);
                arguments.putString(VideoItemDetailFragment.VIDEO_URL, videoUrl);
                arguments.putBoolean(VideoItemDetailFragment.AUTO_PLAY, false);
                initStreamInfo();
            }

        } else {
            videoUrl = savedInstanceState.getString(VideoItemDetailFragment.VIDEO_URL);
        }

        actionBarHandler = new ActionBarHandler(this);
        actionBarHandler.setupNavMenu(this);
        if(onInvokeCreateOptionsMenuListener != null) {
            onInvokeCreateOptionsMenuListener.createOptionsMenu();
        }

    }
    @Override
    public void onItemSelected(String webpage_url) {
        this.videoUrl = webpage_url;
        isRefresh = true;
        initStreamInfo();
    }
    private void initStreamInfo(){
        try {
            Thread videoExtrThread = new Thread(new VideoExtractorRunnable( videoUrl));
            videoExtrThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private class VideoExtractorRunnable implements Runnable {
        private final Handler h = new Handler();
        private YoutubeVideoExtractor streamExtractor;
        private final String videoUrl;

        public VideoExtractorRunnable(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        @Override
        public void run() {
            VideoInfo videoInfo = null;
            try {
                if(YoutubeHelper.acceptUrl(videoUrl)) {
                    streamExtractor =  new YoutubeVideoExtractor(videoUrl, new Downloader());
                }
                else {
                    throw new IllegalArgumentException("String is not a valid Youtube URL");
                }

                videoInfo = VideoInfo.getVideoInfo(streamExtractor);

                h.post(new VideoResultRunnable(videoInfo));

            } catch (IOException e) {
                showError(h, R.string.network_error);
                e.printStackTrace();
            } catch(Exception e) {
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        VideoDetailActivity.this.finish();
                    }
                });
                e.printStackTrace();
            }
        }
    }

    private class VideoResultRunnable implements Runnable {

        public VideoResultRunnable(VideoInfo videoInfo) {
            VideoDetailActivity.this.videoInfo = videoInfo;
        }
        @Override
        public void run() {
            Activity a =  VideoDetailActivity.this;

            setupActionBarHandler(videoInfo);


            if(a != null) {
                boolean showAgeRestrictedContent = PreferenceManager.getDefaultSharedPreferences(a)
                        .getBoolean(a.getString(R.string.show_age_restricted_content), false);
                if (videoInfo.age_limit == 0 || showAgeRestrictedContent) {

                    if(!isRefresh){
                        /**
                         * Draggable panel setup
                         */
                        ButterKnife.bind(a);
                        initializeDraggablePanel();
                        hookDraggablePanelListeners();

                        ListView list = (ListView) findViewById(R.id.detailList);
                        listAdapter = new NewVideoListAdapter(VideoDetailActivity.this,list);

                        listAdapter.addVideoList(videoInfo.related_videos);
                        list.setAdapter(listAdapter);
                        list.setOnItemClickListener(new AdapterView.OnItemClickListener()
                        {
                            @Override
                            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
                            {
                                onItemSelected(videoInfo.related_videos.get(position).webpage_url);
                            }
                        });
                    }
                    else{
                        listAdapter.clearVideoList();
                        listAdapter.addVideoList(videoInfo.related_videos);

                        try {
                            playVideoFragment.refreshVideo(videoInfo.video_streams.get(actionBarHandler.getSelectedVideoStream()).url, videoInfo.thumbnail_url);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        fragment.updateInfo(videoInfo,fragment.getRootView(),a);

                        FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
                        fragTransaction.detach(fragment);
                        fragTransaction.attach(fragment);
                        fragTransaction.commit();
                        fragment.getRootView().forceLayout();
                        draggablePanel.maximize();
                        draggablePanel.bringToFront();
                    }

                    hideProgressDialog();
                } else {
                    // video is age restricted
                }
            }
        }
    }
    private boolean checkIfLandscape() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels < displayMetrics.widthPixels;
    }

    private void initializeDraggablePanel() {

        start_position = videoInfo.start_position;
        Bundle args = new Bundle();
        args.putString(Constants.VIDEO_TITLE,   videoInfo.title);

        String url = ListUtils.isEmptyList(videoInfo.video_streams) ? "" : videoInfo.video_streams.get(actionBarHandler.getSelectedVideoStream()).url;

        args.putString(Constants.STREAM_URL,    url);
        args.putString(Constants.VIDEO_URL,     videoInfo.webpage_url);
        args.putString(Constants.THUMB_URL,     videoInfo.thumbnail_url);
        args.putInt(Constants.START_POSITION,   videoInfo.start_position);

        fragment = new VideoItemDetailFragment();

        playVideoFragment = new PlayVideoFragment();
        playVideoFragment.setArguments(args);
        draggablePanel.setFragmentManager(getSupportFragmentManager());
        draggablePanel.setTopFragment(playVideoFragment);

        fragment.setArguments(args);
        draggablePanel.setBottomFragment(fragment);
        draggablePanel.setKeepScreenOn(true);
        draggablePanel.initializeView();
    }

    private void hideDraggablePanel() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                draggablePanel.setVisibility(View.GONE);
                draggablePanel.closeToRight();
            }
        }, 10);
    }

    /**
     * Hook the DraggableListener to DraggablePanel to pause or resume the video when the
     * DragglabePanel is maximized or closed.
     */
    private void hookDraggablePanelListeners() {
        draggablePanel.setDraggableListener(new DraggableListener() {
            @Override public void onMaximized() {
                playVideo();
            }

            @Override public void onMinimized() {
                //Empty
            }

            @Override public void onClosedToLeft() {
                pauseVideo();
            }

            @Override public void onClosedToRight() {
                pauseVideo();
            }
        });
    }

    private void pauseVideo() {
        playVideoFragment.onDestroy();
    }

    private void playVideo() {
    }

    @Override
    protected void onDestroy() {
        mProgressDialog = null;
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        mProgressDialog = null;
        super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(VideoItemDetailFragment.VIDEO_URL, videoUrl);
        outState.putBoolean(VideoItemDetailFragment.AUTO_PLAY, false);
    }


    private void showError(Handler h, final int stringResource) {
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(VideoDetailActivity.this,
                        stringResource, Toast.LENGTH_LONG).show();
            }
        });
    }


    private void setupActionBarHandler(final VideoInfo info) {
        actionBarHandler.setupStreamList(info.video_streams);

        actionBarHandler.setOnShareListener(new ActionBarHandler.OnActionListener() {
            @Override
            public void onActionSelected(int selectedStreamId) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, info.webpage_url);
                intent.setType("text/plain");
                VideoDetailActivity.this.startActivity(Intent.createChooser(intent, VideoDetailActivity.this.getString(R.string.share_dialog_title)));
            }
        });

        actionBarHandler.setOnOpenInBrowserListener(new ActionBarHandler.OnActionListener() {
            @Override
            public void onActionSelected(int selectedStreamId) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(info.webpage_url));

                VideoDetailActivity.this.startActivity(Intent.createChooser(intent, VideoDetailActivity.this.getString(R.string.choose_browser)));
            }
        });

        actionBarHandler.setOnDownloadListener(new ActionBarHandler.OnActionListener() {
            @Override
            public void onActionSelected(int selectedStreamId) {
                try {
                    Bundle args = new Bundle();

                    if (info.audio_streams != null) {
                        YouTubeAudio youTubeAudio =
                                info.audio_streams.get(getPreferredAudioStreamId(info));

                        String audioSuffix = "." + MediaFormat.getSuffixById(youTubeAudio.format);
                        args.putString(DownloadDialog.AUDIO_URL, youTubeAudio.url);
                        args.putString(DownloadDialog.FILE_SUFFIX_AUDIO, audioSuffix);
                    }

                    if (info.video_streams != null) {
                        YouTubeVideo selectedVideoItem = info.video_streams.get(selectedStreamId);
                        String videoSuffix = "." + MediaFormat.getSuffixById(selectedVideoItem.format);
                        args.putString(DownloadDialog.FILE_SUFFIX_VIDEO, videoSuffix);
                        args.putString(DownloadDialog.VIDEO_URL, selectedVideoItem.url);
                    }

                    args.putString(DownloadDialog.TITLE, info.title);
                    DownloadDialog downloadDialog = new DownloadDialog();
                    downloadDialog.setArguments(args);
                    downloadDialog.show(VideoDetailActivity.this.getSupportFragmentManager(), "downloadDialog");
                } catch (Exception e) {
                    Toast.makeText(VideoDetailActivity.this,
                            R.string.could_not_setup_download_menu, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });

        if(info.audio_streams == null) {
            actionBarHandler.showAudioAction(false);
        } else {
            actionBarHandler.setOnPlayAudioListener(new ActionBarHandler.OnActionListener() {
                @Override
                public void onActionSelected(int selectedStreamId) {

                    Intent intent;
                    YouTubeAudio youTubeAudio = info.audio_streams.get(getPreferredAudioStreamId(info));
                    if ( Build.VERSION.SDK_INT >= 18) {
                        //internal music player: explicit intent
                        if (!BackgroundPlayer.isRunning ){

                            intent = new Intent(VideoDetailActivity.this, BackgroundPlayer.class);

                            intent.setAction(Intent.ACTION_VIEW);
                            Log.i(TAG, "youTubeAudio is null:" + (youTubeAudio == null));
                            Log.i(TAG, "youTubeAudio.url is null:" + (youTubeAudio.url == null));
                            intent.setDataAndType(Uri.parse(youTubeAudio.url),
                                    MediaFormat.getMimeById(youTubeAudio.format));
                            intent.putExtra(Constants.TITLE,         info.title);
                            intent.putExtra(Constants.WEB_URL,       info.webpage_url);
                            intent.putExtra(Constants.CHANNEL_NAME,  info.uploader);
                            intent.putExtra(Constants.THUMB_URL,     info.thumbnail_url);

                            VideoDetailActivity.this.startService(intent);
                        }
                    } else {
                        intent = new Intent();
                        try {
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.parse(youTubeAudio.url),
                                    MediaFormat.getMimeById(youTubeAudio.format));
                            intent.putExtra(Intent.EXTRA_TITLE, info.title);
                            intent.putExtra("title", info.title);

                            VideoDetailActivity.this.startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        actionBarHandler.setupMenu(menu, getMenuInflater());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            NavUtils.navigateUpTo(this, intent);
            return true;
        } else {
            return actionBarHandler.onItemSelected(item) ||
                    super.onOptionsItemSelected(item);
        }
    }
    private int getPreferredAudioStreamId(final VideoInfo info) {
        String preferredFormatString = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(this.getString(R.string.default_audio_format_key), "webm");

        int preferredFormat = MediaFormat.WEBMA.id;
        switch(preferredFormatString) {
            case "webm":
                preferredFormat = MediaFormat.WEBMA.id;
                break;
            case "m4a":
                preferredFormat = MediaFormat.M4A.id;
                break;
            default:
                break;
        }

        for(int i = 0; i < info.audio_streams.size(); i++) {
            if(info.audio_streams.get(i).format == preferredFormat) {
                return i;
            }
        }

        Log.e(TAG, "FAILED to set audioStream value!");
        return 0;
    }

    public int getStart_position() {
        return start_position;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        start_position = savedInstanceState.getInt(Constants.START_POSITION);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }
}
