package com.thesis.home.youlist.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import com.thesis.home.youlist.R;
import com.thesis.home.youlist.exceptions.ExtractionException;
import com.thesis.home.youlist.helpers.Downloader;
import com.thesis.home.youlist.model.VideoPreviewInfo;
import com.thesis.home.youlist.model.SearchResult;
import com.thesis.home.youlist.services.YoutubeSearchEngine;

import java.io.IOException;
import java.util.List;

public class VideoItemListFragment extends AbstractVideoFragment {

    private static final String TAG = VideoItemListFragment.class.toString();
    SearchRunnable searchRunnable = null;
    private class ResultRunnable implements Runnable {
        private final SearchResult result;
        private final int requestId;
        public ResultRunnable(SearchResult result, int requestId) {
            this.result = result;
            this.requestId = requestId;
        }
        @Override
        public void run() {
            updateListOnResult(result, requestId);
            if (android.os.Build.VERSION.SDK_INT >= 19) {
                getListView().removeFooterView(footer);
            }
        }
    }

    private class SearchRunnable implements Runnable {
        private final YoutubeSearchEngine engine;
        private final String query;
        private final int page;
        final Handler h = new Handler();
        private volatile boolean runs = true;
        private final int requestId;
        public SearchRunnable(YoutubeSearchEngine engine, String query, int page, int requestId) {
            this.engine = engine;
            this.query = query;
            this.page = page;
            this.requestId = requestId;
        }
        void terminate() {
            runs = false;
        }
        @Override
        public void run() {
            SearchResult result;
            try {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
                String searchLanguageKey = getContext().getString(R.string.language_key);
                String searchLanguage = sp.getString(searchLanguageKey,
                        getString(R.string.default_language_value));
                result = SearchResult
                        .getSearchResult(engine, query, page, searchLanguage, new Downloader());

                if(runs) {
                    h.post(new ResultRunnable(result, requestId));
                }

                // soft errors:
                if(result != null &&
                        !result.errors.isEmpty()) {
                    Log.e(TAG, "OCCURRED ERRORS DURING SEARCH EXTRACTION:");
                    for(Exception e : result.errors) {
                        e.printStackTrace();
                        Log.e(TAG, "------");
                    }


                }
                // hard errors:
            } catch(IOException e) {
                postNewNothingFoundToast(h, R.string.network_error);
                e.printStackTrace();
            } catch(ExtractionException e) {
                e.printStackTrace();

            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void search(String query) {
        mode = SEARCH_MODE;
        this.query = query;
        this.lastPage = 1;
        videoListAdapter.clearVideoList();
        setListShown(false);
        startSearch(query, lastPage);
        getListView().smoothScrollToPosition(0);
    }

    private void nextPage() {
        loadingNextPage = true;
        lastPage++;
        Log.d(TAG, getString(R.string.search_page) + Integer.toString(lastPage));
        startSearch(query, lastPage);
    }

    private void startSearch(String query, int page) {
        currentRequestId++;
        terminateThreads();
        searchRunnable = new SearchRunnable(new YoutubeSearchEngine(),query, page, currentRequestId);
        searchThread = new Thread(searchRunnable);
        searchThread.start();
    }

    private void updateListOnResult(SearchResult result, int requestId) {
        if(requestId == currentRequestId) {
            setListShown(true);
            updateList(result.resultList);
            if(!result.suggestion.isEmpty()) {
                Toast.makeText(getActivity(),
                        String.format(getString(R.string.did_you_mean), result.suggestion),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void updateList(List<VideoPreviewInfo> list) {
        try {
            videoListAdapter.addVideoList(list);
            terminateThreads();
            hideProgressDialog();
        } catch(IllegalStateException e) {
            Toast.makeText(getActivity(), "Trying to set value while activity doesn't exist anymore.",
                    Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Trying to set value while activity doesn't exist anymore.");
        } catch(Exception e) {
            Toast.makeText(getActivity(), getString(R.string.general_error),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            loadingNextPage = false;
        }
    }

    private void terminateThreads() {
        if(searchThread != null) {
            searchRunnable.terminate();
            // No need to join, since we don't really terminate the thread. We just demand
            // it to post its result runnable into the gui main loop.
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            long lastScrollDate = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (mode != PRESENT_VIDEOS_MODE
                        && list.getChildAt(0) != null
                        && list.getLastVisiblePosition() == list.getAdapter().getCount() - 1
                        && list.getChildAt(list.getChildCount() - 1).getBottom() <= list.getHeight()) {
                    long time = System.currentTimeMillis();
                    if ((time - lastScrollDate) > 200
                            && !loadingNextPage) {
                        lastScrollDate = time;
                        getListView().addFooterView(footer);
                        nextPage();
                    }
                }
            }

        });
    }

    private void postNewNothingFoundToast(Handler h, final int stringResource) {
        h.post(new Runnable() {
            @Override
            public void run() {
                setListShown(true);
                Toast.makeText(getActivity(), getString(stringResource),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        setActivatedPosition(position);
        this.mCallbacks.onItemSelected(Long.toString(id), null);
    }
}
