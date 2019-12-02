package com.thesis.home.youlist.players;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;


import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.thesis.home.youlist.BuildConfig;
import com.thesis.home.youlist.R;
import com.thesis.home.youlist.activity.VideoDetailActivity;
import com.thesis.home.youlist.fragments.VideoItemDetailFragment;
import com.thesis.home.youlist.model.Constants;

import java.io.IOException;

public class BackgroundPlayer extends Service{

    private static final String TAG = BackgroundPlayer.class.toString();
    private static final String ACTION_STOP = TAG + ".STOP";
    private static final String ACTION_PLAYPAUSE = TAG + ".PLAYPAUSE";

    private volatile String webUrl = "";
    private volatile String channelName = "";
    private volatile String thumbnailUrl ;

    // Determines if the service is already running.
    // Prevents launching the service twice.
    public static volatile boolean isRunning = false;

    public BackgroundPlayer() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, R.string.background_player_playing_toast,
                Toast.LENGTH_SHORT).show();

        String source = intent.getDataString();
        //Log.i(TAG, "backgroundPLayer source:"+source);
        String videoTitle = intent.getStringExtra(Constants.TITLE);
        webUrl          = intent.getStringExtra(Constants.WEB_URL);
        channelName     = intent.getStringExtra(Constants.CHANNEL_NAME);
        thumbnailUrl    = intent.getStringExtra(Constants.THUMB_URL);

        //do nearly everything in a separate thread
        PlayerThread player = new PlayerThread(source, videoTitle, this);
        player.start();

        isRunning = true;

        // If we get killed after returning here, don't restart
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        isRunning = false;
    }

    private class PlayerThread extends Thread {
        MediaPlayer mediaPlayer;
        private String source;
        private String title;
        private int noteID = TAG.hashCode();
        private BackgroundPlayer owner;
        private NotificationManager noteMgr;
        private WifiManager.WifiLock wifiLock;
        private Bitmap videoThumbnail = null;
        private NotificationCompat.Builder noteBuilder;
        private Notification note;

        public PlayerThread(String src, String title, BackgroundPlayer owner) {
            this.source = src;
            this.title = title;
            this.owner = owner;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        @Override
        public void run() {
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);//cpu lock
            try {
                mediaPlayer.setDataSource(source);
                mediaPlayer.prepare();

            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.e(TAG, "video source:" + source);
                Log.e(TAG, "video title:" + title);
                //can't do anything useful without a file to play; exit early
                return;
            }

            WifiManager wifiMgr = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            wifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);

            //listen for end of video
            mediaPlayer.setOnCompletionListener(new EndListener(wifiLock));

            wifiLock.acquire();
            mediaPlayer.start();

            IntentFilter filter = new IntentFilter();
            filter.setPriority(Integer.MAX_VALUE);
            filter.addAction(ACTION_PLAYPAUSE);
            filter.addAction(ACTION_STOP);
            registerReceiver(broadcastReceiver, filter);

            note = buildNotification();

            startForeground(noteID, note);

            noteMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        }

        /**Handles button presses from the notification. */
        private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                //Log.i(TAG, "received broadcast action:"+action);
                if(action.equals(ACTION_PLAYPAUSE)) {
                    if(mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        note.contentView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_play_circle_filled_white);
                        if(android.os.Build.VERSION.SDK_INT >=16){
                            note.bigContentView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_play_circle_filled_white);
                        }
                        noteMgr.notify(noteID, note);
                    }
                    else {
                        //reacquire CPU lock after auto-releasing it on pause
                        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                        mediaPlayer.start();
                        note.contentView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_pause_circle_filled);
                        if(android.os.Build.VERSION.SDK_INT >=16){
                            note.bigContentView.setImageViewResource(R.id.notificationPlayPause, R.drawable.ic_pause_circle_filled);
                        }
                        noteMgr.notify(noteID, note);
                    }
                }
                else if(action.equals(ACTION_STOP)) {
                    //this auto-releases CPU lock
                    mediaPlayer.stop();
                    afterPlayCleanup();
                }
            }
        };

        private void afterPlayCleanup() {
            //remove progress bar
            //noteBuilder.setProgress(0, 0, false);

            //remove notification
            noteMgr.cancel(noteID);
            unregisterReceiver(broadcastReceiver);
            //release mediaPlayer's system resources
            mediaPlayer.release();

            //release wifilock
            wifiLock.release();
            //remove foreground status of service; make BackgroundPlayer killable
            stopForeground(true);

            stopSelf();
        }

        private class EndListener implements MediaPlayer.OnCompletionListener {
            private WifiManager.WifiLock wl;
            public EndListener(WifiManager.WifiLock wifiLock) {
                this.wl = wifiLock;
            }

            @Override
            public void onCompletion(MediaPlayer mp) {
                afterPlayCleanup();
            }
        }

        private Notification buildNotification() {
            Notification note;
            Resources res = getApplicationContext().getResources();
            noteBuilder = new NotificationCompat.Builder(owner);

            PendingIntent playPI = PendingIntent.getBroadcast(owner, noteID,
                    new Intent(ACTION_PLAYPAUSE), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent stopPI = PendingIntent.getBroadcast(owner, noteID,
                    new Intent(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT);

            //build intent to return to video, on tapping notification
            Intent openDetailViewIntent = new Intent(getApplicationContext(),
                    VideoDetailActivity.class);
            openDetailViewIntent.putExtra(VideoItemDetailFragment.VIDEO_URL, webUrl);
            openDetailViewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent openDetailView = PendingIntent.getActivity(owner, noteID,
                    openDetailViewIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            noteBuilder
                    .setOngoing(true)
                    .setDeleteIntent(stopPI)
                    .setSmallIcon(R.drawable.ic_play_circle_filled_white)
                    .setTicker(
                            String.format(res.getString(
                                    R.string.background_player_time_text), title))
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(),
                            noteID, openDetailViewIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentIntent(openDetailView);


            Target target = new Target() {

                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    videoThumbnail = bitmap;
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }

            };
            loadBitmap(thumbnailUrl,target);

            RemoteViews view =
                    new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.player_notification);
            view.setImageViewBitmap(R.id.notificationCover, videoThumbnail);
            view.setTextViewText(R.id.notificationSongName, title);
            view.setTextViewText(R.id.notificationArtist, channelName);
            view.setOnClickPendingIntent(R.id.notificationStop, stopPI);
            view.setOnClickPendingIntent(R.id.notificationPlayPause, playPI);
            view.setOnClickPendingIntent(R.id.notificationContent, openDetailView);

            RemoteViews expandedView =
                    new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.player_notification_expanded);
                expandedView.setImageViewBitmap(R.id.notificationCover, videoThumbnail);
            expandedView.setTextViewText(R.id.notificationSongName, title);
                expandedView.setTextViewText(R.id.notificationArtist, channelName);
            expandedView.setOnClickPendingIntent(R.id.notificationStop, stopPI);
            expandedView.setOnClickPendingIntent(R.id.notificationPlayPause, playPI);
            expandedView.setOnClickPendingIntent(R.id.notificationContent, openDetailView);

            noteBuilder.setCategory(Notification.CATEGORY_TRANSPORT);

            //Make notification appear on lockscreen
            noteBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);

            note = noteBuilder.build();
            note.contentView = view;

            if (android.os.Build.VERSION.SDK_INT > 16) {
                note.bigContentView = expandedView;
            }

            return note;
        }
        private void loadBitmap(String url,Target target) {
            Picasso.with(getApplicationContext()).load(url).into(target);
        }
    }
}
