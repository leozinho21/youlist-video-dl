package com.thesis.home.youlist.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.thesis.home.youlist.helpers.CircularTransform;
import com.thesis.home.youlist.helpers.Localization;
import com.thesis.home.youlist.helpers.StringUtils;
import com.thesis.home.youlist.R;
import com.thesis.home.youlist.activity.VideoDetailActivity;
import com.thesis.home.youlist.helpers.VideoInfoViewCreator;
import com.thesis.home.youlist.model.VideoInfo;
import com.thesis.home.youlist.model.VideoPreviewInfo;
import com.thesis.home.youlist.model.YouTubeVideo;

import java.util.ArrayList;
import java.util.Vector;

public class VideoItemDetailFragment extends Fragment {

    private static final String TAG = VideoItemDetailFragment.class.toString();

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String VIDEO_URL = "video_url";
    public static final String AUTO_PLAY = "auto_play";

    private VideoDetailActivity activity;
    private View rootView;

    private VideoInfoViewCreator videoItemViewCreator    = null;
    private RelativeLayout              textContentLayout       = null;
    private TextView                    videoTitleView          = null;
    private TextView                    uploaderView            = null;
    private TextView                    viewCountView           = null;
    private TextView                    thumbsUpView            = null;
    private TextView                    thumbsDownView          = null;
    private TextView                    uploadDateView          = null;
    private TextView                    descriptionView         = null;
    private FrameLayout                 nextVideoFrame          = null;
    private RelativeLayout              nextVideoRootFrame      = null;
    private Button                      nextVideoButton         = null;
    private TextView                    similarTitle            = null;
    private View                        nextVideoView           = null;
    private View                        topView                 = null;
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        void onItemSelected(String webpage_url);
    }


    private boolean showNextVideoItem = false;

    public void updateInfo(final VideoInfo info, View fragmentView, Activity activity ) {
        try {
            Context c = activity;

            if(videoItemViewCreator == null) {
                videoItemViewCreator =  new VideoInfoViewCreator(LayoutInflater.from(getActivity()));
            }
                textContentLayout   = (RelativeLayout) rootView.findViewById(R.id.detailTextContentLayout);
                videoTitleView      = (TextView) rootView.findViewById(R.id.detailVideoTitleView);
                uploaderView        = (TextView) rootView.findViewById(R.id.detailUploaderView);
                viewCountView       = (TextView) rootView.findViewById(R.id.detailViewCountView);
                thumbsUpView        = (TextView) rootView.findViewById(R.id.detailThumbsUpCountView);
                thumbsDownView      = (TextView) rootView.findViewById(R.id.detailThumbsDownCountView);
                uploadDateView      = (TextView) rootView.findViewById(R.id.detailUploadDateView);
                descriptionView     = (TextView) rootView.findViewById(R.id.detailDescriptionView);
                nextVideoFrame      = (FrameLayout) rootView.findViewById(R.id.detailNextVideoFrame);
                nextVideoRootFrame  = (RelativeLayout) rootView.findViewById(R.id.detailNextVideoRootLayout);
                nextVideoButton     = (Button)   rootView.findViewById(R.id.detailNextVideoButton);
                similarTitle        = (TextView) rootView.findViewById(R.id.detailSimilarTitle);
                topView             = rootView.findViewById(R.id.detailTopView);

            if(info.next_video != null) {
                nextVideoView = videoItemViewCreator
                        .getViewFromVideoInfoItem(null, nextVideoFrame, info.next_video,activity);
            } else {
                rootView.findViewById(R.id.detailNextVidButtonAndContentLayout).setVisibility(View.GONE);
                rootView.findViewById(R.id.detailNextVideoTitle).setVisibility(View.GONE);
                rootView.findViewById(R.id.detailNextVideoButton).setVisibility(View.GONE);
            }

            if(nextVideoView != null) {
                nextVideoFrame.addView(nextVideoView);
            }

            initThumbnailViews(info, nextVideoFrame, rootView, c);

            textContentLayout.setVisibility(View.VISIBLE);

            if (!showNextVideoItem) {
                nextVideoRootFrame.setVisibility(View.GONE);
                similarTitle.setVisibility(View.GONE);
            }

            videoTitleView.setText(info.title);

            topView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        ImageView arrow = (ImageView) rootView.findViewById(R.id.toggleDescriptionView);
                        View extra = rootView.findViewById(R.id.detailExtraView);
                        if (extra.getVisibility() == View.VISIBLE) {
                            extra.setVisibility(View.GONE);
                            arrow.setImageResource(R.drawable.ic_keyboard_arrow_down);
                        } else {
                            extra.setVisibility(View.VISIBLE);
                            arrow.setImageResource(R.drawable.ic_keyboard_arrow_up);
                        }
                    }
                    return true;
                }
            });

            // the UI has to react on missing information.
            videoTitleView.setText(info.title);
            if(!info.uploader.isEmpty()) {
                uploaderView.setText(info.uploader);
            } else {
                rootView.findViewById(R.id.detailUploaderWrapView).setVisibility(View.GONE);
            }
            if(info.view_count >= 0) {
                viewCountView.setText(Localization.localizeViewCount(info.view_count, c));
            } else {
                viewCountView.setVisibility(View.GONE);
            }
            if(info.dislike_count >= 0) {
                thumbsDownView.setText(Localization.localizeNumber(info.dislike_count, c));
            } else {
                thumbsDownView.setVisibility(View.INVISIBLE);
                rootView.findViewById(R.id.detailThumbsDownImgView).setVisibility(View.GONE);
            }
            if(info.like_count >= 0) {
                thumbsUpView.setText(Localization.localizeNumber(info.like_count, c));
            } else {
                thumbsUpView.setVisibility(View.GONE);
                rootView.findViewById(R.id.detailThumbsUpImgView).setVisibility(View.GONE);
                thumbsDownView.setVisibility(View.GONE);
                rootView.findViewById(R.id.detailThumbsDownImgView).setVisibility(View.GONE);
            }
            if(!info.upload_date.isEmpty()) {
                uploadDateView.setText(Localization.localizeDate(info.upload_date, c));
            } else {
                uploadDateView.setVisibility(View.GONE);
            }
            if(!info.description.isEmpty()) {
                descriptionView.setText(Html.fromHtml(info.description));
            } else {
                descriptionView.setVisibility(View.GONE);
            }

            descriptionView.setMovementMethod(LinkMovementMethod.getInstance());

            // parse streams
            Vector<YouTubeVideo> streamsToUse = new Vector<>();
            for (YouTubeVideo i : info.video_streams) {
                if (useStream(i, streamsToUse)) {
                    streamsToUse.add(i);
                }
            }

            nextVideoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ((Callbacks)getActivity()).onItemSelected(info.next_video.webpage_url);
                }
            });
            textContentLayout.setVisibility(View.VISIBLE);

            if(info.related_videos != null && !info.related_videos.isEmpty()) {
                initSimilarVideos(info, videoItemViewCreator,rootView,c);

            } else {
                rootView.findViewById(R.id.detailSimilarTitle).setVisibility(View.GONE);
                rootView.findViewById(R.id.similarVideosView).setVisibility(View.GONE);
            }

        } catch (NullPointerException e) {
            Log.w(TAG, "updateInfo(): Fragment closed before thread ended work... or else");
            e.printStackTrace();
        }
    }

    private void initThumbnailViews(VideoInfo info, View nextVideoFrame, View rootView, Context context) {

        ImageView uploaderThumb
                = (ImageView) rootView.findViewById(R.id.detailUploaderThumbnailView);
        ImageView nextVideoThumb =
                (ImageView) nextVideoFrame.findViewById(R.id.itemThumbnailView);

        if(StringUtils.notEmpty(info.uploader_thumbnail_url)) {

            Picasso.with(context).load(info.uploader_thumbnail_url).transform(new CircularTransform()).into(uploaderThumb);
        }
        if(StringUtils.notEmpty(info.thumbnail_url) && info.next_video != null) {

            Picasso.with(context).load(info.next_video.thumbnail_url).into(nextVideoThumb);
        }
    }

    private void initSimilarVideos(final VideoInfo info, VideoInfoViewCreator videoItemViewCreator, View rootView, Context context) {
        LinearLayout similarLayout = (LinearLayout) rootView.findViewById(R.id.similarVideosView);
        ArrayList<VideoPreviewInfo> similar = new ArrayList<>(info.related_videos);

        for (final VideoPreviewInfo item : similar) {
            View similarView = videoItemViewCreator
                    .getViewFromVideoInfoItem(null, similarLayout, item , context);

            similarView.setClickable(true);
            similarView.setFocusable(true);
            similarView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {

                        ((Callbacks)getActivity()).onItemSelected(item.webpage_url);

                        return true;
                    }
                    return false;
                }
            });

            similarLayout.addView(similarView);
            ImageView rthumb = (ImageView)similarView.findViewById(R.id.itemThumbnailView);

            Picasso.with(context).load(item.thumbnail_url).into(rthumb);
        }
    }

    private boolean useStream(YouTubeVideo stream, Vector<YouTubeVideo> streams) {
        for(YouTubeVideo i : streams) {
            if(i.resolution.equals(stream.resolution)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (VideoDetailActivity) getActivity();
        showNextVideoItem = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getBoolean(activity.getString(R.string.show_next_video_key),true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_videoitem_detail, container, false);

        updateInfo(activity.getVideoInfo(),null,activity);

        return rootView;
    }

    public View getRootView() {
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceBundle) {
        super.onActivityCreated(savedInstanceBundle);
    }
}