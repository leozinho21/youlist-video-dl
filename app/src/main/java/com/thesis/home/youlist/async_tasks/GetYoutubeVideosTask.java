package com.thesis.home.youlist.async_tasks;

import android.os.AsyncTask;
import android.support.v4.app.ListFragment;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.thesis.home.youlist.helpers.YoutubeContent;
import com.thesis.home.youlist.helpers.YoutubeHelper;
import com.thesis.home.youlist.interfaces.IVideo;
import com.thesis.home.youlist.model.AbstractVideoInfo;
import com.thesis.home.youlist.model.Constants;
import com.thesis.home.youlist.model.VideoPreviewInfo;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * Created by HOME on 12/3/2016.
 */
public class GetYoutubeVideosTask extends AsyncTask<String,Void,Void> {

    private static final String webpage = "https://www.youtube.com/watch?v=";

    private static int  VIDEO_ID_PARAM      = 0;
    private static int  QUERY_TEXT_PARAM    = 1;

    private YouTube             youtube ;
    private ListFragment        fragment ;
    private YouTube.Search.List query ;
    List<VideoPreviewInfo> resultList = new Vector<>();

    public GetYoutubeVideosTask(YouTube youtube,ListFragment fragment){
        this.youtube    = youtube;
        this.fragment   = fragment;
    }

    @Override
    protected Void doInBackground(String... params) {
        try{

            com.google.api.services.youtube.YouTube.Channels.List channels = youtube.channels().list("contentDetails");
            channels.setKey(Constants.MY_API_KEY);
            channels.setMine(true);

            ChannelListResponse channelResult = channels.execute();

            if(YoutubeHelper.UPLOAD_VIDEO_TYPE.equals(params[VIDEO_ID_PARAM])){
                fetchVideos( getUploadPlaylistId( channelResult ) );
                YoutubeContent.addItems(YoutubeHelper.UPLOAD_VIDEO_TYPE,resultList);
            }
            else if(YoutubeHelper.WATCH_HISTORY_VIDEO_TYPE.equals(params[VIDEO_ID_PARAM])){
                fetchVideos( getWatchHistoryPlaylistId( channelResult ) );
                YoutubeContent.addItems(YoutubeHelper.WATCH_HISTORY_VIDEO_TYPE,resultList);
            }
            else if(YoutubeHelper.WATCH_LATER_VIDEO_TYPE.equals(params[VIDEO_ID_PARAM])){
                fetchVideos( getWatchLaterPlaylistId( channelResult ) );
                YoutubeContent.addItems(YoutubeHelper.WATCH_LATER_VIDEO_TYPE,resultList);
            }
            else if(YoutubeHelper.TRENDING_VIDEO_TYPE.equals(params[VIDEO_ID_PARAM])){
                fetchPopularVideos();
                YoutubeContent.addItems(YoutubeHelper.TRENDING_VIDEO_TYPE,resultList);
            }


        } catch (UserRecoverableAuthIOException e) {
            fragment.startActivityForResult(e.getIntent(), 1001);
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    private void fetchVideos(String playlistId) throws IOException {

        // Get videos from user's upload playlist with a playlist
        // items list request
        PlaylistItemListResponse pilr = youtube.playlistItems()
                .list("id,contentDetails,snippet")
                .setPlaylistId(playlistId)
                .setMaxResults(new Long(20)).execute();

        YouTube.Videos.List listVideosRequest = youtube.videos().list("contentDetails,snippet,statistics");
        listVideosRequest.setKey(Constants.MY_API_KEY);
        listVideosRequest.setId(getIds(pilr.getItems()));
        addVideos(listVideosRequest.execute());
    }

    private void fetchPopularVideos() throws IOException  {
        YouTube.Videos.List listVideosRequest = youtube.videos().list("contentDetails,snippet,statistics");
        listVideosRequest.setKey(Constants.MY_API_KEY);
        listVideosRequest.setChart("mostPopular");
        listVideosRequest.setMaxResults(new Long(20));

        addVideos(listVideosRequest.execute());
    }
    private void addVideos(VideoListResponse listResponse){
        for(Video video :  listResponse.getItems()){
            VideoPreviewInfo videoPreviewInfo = new VideoPreviewInfo();
            videoPreviewInfo.thumbnail_url   = video.getSnippet().getThumbnails().getDefault().getUrl().toString();
            videoPreviewInfo.title           = video.getSnippet().getTitle();
            videoPreviewInfo.webpage_url     = webpage + video.getId();
            videoPreviewInfo.id              = video.getId();
            videoPreviewInfo.stream_type     = AbstractVideoInfo.StreamType.VIDEO_STREAM;
            videoPreviewInfo.view_count      = video.getStatistics().getViewCount().longValue();
            videoPreviewInfo.upload_date     = video.getSnippet().getPublishedAt().toString();
            videoPreviewInfo.uploader        = video.getSnippet().getChannelTitle();

            resultList.add(videoPreviewInfo);
        }
    }
    private String getIds(List<PlaylistItem> playlistItemList){
        if (playlistItemList == null) return null;

        StringBuffer idbuffer = new StringBuffer();

        for(PlaylistItem item : playlistItemList){
            idbuffer.append( item.getContentDetails().getVideoId()).append(",");
        }
        if(idbuffer.length() > 0)
        {
            idbuffer.deleteCharAt(idbuffer.length()-1);
        }

        return  idbuffer.toString();
    }
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        returnVideosTo(resultList, (IVideo) fragment);
    }


    private void returnVideosTo(List<VideoPreviewInfo> resultList, IVideo fragment){
        fragment.onSuccess(resultList);
    }

    private String getUploadPlaylistId( ChannelListResponse channelResult){

        return channelResult.getItems().get(0)
                .getContentDetails()
                .getRelatedPlaylists()
                .getUploads();
    }

    private String getWatchLaterPlaylistId( ChannelListResponse channelResult){

        return channelResult.getItems().get(0)
                .getContentDetails()
                .getRelatedPlaylists()
                .getWatchLater();
    }

    private String getWatchHistoryPlaylistId( ChannelListResponse channelResult){

        return channelResult.getItems().get(0)
                .getContentDetails()
                .getRelatedPlaylists()
                .getWatchHistory();
    }

}
