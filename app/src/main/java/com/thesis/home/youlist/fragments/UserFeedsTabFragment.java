package com.thesis.home.youlist.fragments;

import com.thesis.home.youlist.R;
import com.thesis.home.youlist.fragments.homeTabFragments.UploadFragment;
import com.thesis.home.youlist.fragments.homeTabFragments.WatchHistoryFragment;
import com.thesis.home.youlist.fragments.homeTabFragments.WatchLaterFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class UserFeedsTabFragment extends Fragment{

	private FragmentTabHost mTabHost;
	 //Mandatory Constructor
     public UserFeedsTabFragment() {
     }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mTabHost = new FragmentTabHost(getActivity());//(FragmentTabHost)rootView.findViewById(R.id.tabhost);
        mTabHost.setup(getActivity(), getChildFragmentManager(),R.layout.fragment_home);
        mTabHost.clearAllTabs();
        mTabHost.addTab(mTabHost.newTabSpec("upload-fraagment").setIndicator("Upload"),
        		UploadFragment.class, getArguments());
        mTabHost.addTab(mTabHost.newTabSpec("watch-history-fragment").setIndicator("Watch History"),
                WatchHistoryFragment.class, getArguments());
        mTabHost.addTab(mTabHost.newTabSpec("watch-later-fragment").setIndicator("Watch Later"),
                WatchLaterFragment.class, getArguments());

        return mTabHost;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }
}
