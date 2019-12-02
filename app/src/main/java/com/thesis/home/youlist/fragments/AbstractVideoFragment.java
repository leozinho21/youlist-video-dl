package com.thesis.home.youlist.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

import com.thesis.home.youlist.R;
import com.thesis.home.youlist.adapters.NewVideoListAdapter;
import com.thesis.home.youlist.interfaces.IVideo;
import com.thesis.home.youlist.model.VideoPreviewInfo;

import java.util.List;

/**
 * Created by HOME on 27/5/2016.
 */
public class AbstractVideoFragment extends ListFragment implements IVideo {

    protected NewVideoListAdapter videoListAdapter;
    private ProgressDialog mProgressDialog;
    // activity modes
    protected static final int SEARCH_MODE = 0;
    protected static final int PRESENT_VIDEOS_MODE = 1;

    protected int mode = SEARCH_MODE;
    protected String query = "";
    protected int lastPage = 0;

    protected Thread searchThread = null;
    // used to track down if results posted by threads ar still valid
    protected int currentRequestId = -1;
    protected ListView list;

    protected View footer;

    // used to suppress request for loading a new page while another page is already loading.
    protected boolean loadingNextPage = true;

    @Override
    public void onSuccess(List<VideoPreviewInfo> resultList) {
        this.videoListAdapter.clearVideoList();
        this.videoListAdapter.addVideoList(resultList);
       if(this.isAdded()) {
           setListShown(true);
       }

        hideProgressDialog();
    }

    @Override
    public void onFailure(Error error) {

    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        void onItemSelected(String id,String videoType);
    }

    protected Callbacks mCallbacks = null;

    public void present(List<VideoPreviewInfo> videoList) {
        this.mode = PRESENT_VIDEOS_MODE;
        setListShown(true);
        getListView().smoothScrollToPosition(0);

        updateList(videoList);
    }

    /**
     * Override it
     * @param list
     */
    protected void updateList(List<VideoPreviewInfo> list){
    }

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    protected static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The current activated item position. Only used on tablets.
     */
    protected int mActivatedPosition = ListView.INVALID_POSITION;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.list = getListView();
        this.videoListAdapter = new NewVideoListAdapter(getActivity(), list);
        footer = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.paginate_footer, null, false);

//        showProgressDialog();

        setListAdapter(this.videoListAdapter);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null

                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    protected void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        this.mActivatedPosition = position;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Activities containing this fragment must implement its callbacks.
        if (!(context instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        this.mCallbacks = (Callbacks) context;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    protected void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    @Override
    public void onDestroy() {
        mProgressDialog = null;
        super.onDestroy();
    }
}
