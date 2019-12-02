package com.thesis.home.youlist.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.thesis.home.youlist.R;

public class ConfigActivity extends PreferenceActivity  {

    private AppCompatDelegate mDelegate = null;

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceBundle);
        super.onCreate(savedInstanceBundle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

    }

    public static class SettingsFragment extends PreferenceFragment{
        SharedPreferences.OnSharedPreferenceChangeListener prefListener;

        // get keys
        String DEFAULT_RESOLUTION_PREFERENCE;
        String DEFAULT_AUDIO_FORMAT_PREFERENCE;
        String SEARCH_LANGUAGE_PREFERENCE;
        String DOWNLOAD_PATH_PREFERENCE;
        String DOWNLOAD_PATH_AUDIO_PREFERENCE;

        private ListPreference defaultResolutionPreference;
        private ListPreference defaultAudioFormatPreference;
        private ListPreference searchLanguagePreference;
        private EditTextPreference downloadPathPreference;
        private EditTextPreference downloadPathAudioPreference;
        private SharedPreferences defaultPreferences;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.config);

            final Activity activity = getActivity();

            defaultPreferences = PreferenceManager.getDefaultSharedPreferences(activity);

            // get keys
            DEFAULT_RESOLUTION_PREFERENCE =getString(R.string.def_res);
            DEFAULT_AUDIO_FORMAT_PREFERENCE =getString(R.string.default_audio_format_key);
            SEARCH_LANGUAGE_PREFERENCE =getString(R.string.language_key);
            DOWNLOAD_PATH_PREFERENCE = getString(R.string.dl_path_video);
            DOWNLOAD_PATH_AUDIO_PREFERENCE = getString(R.string.dl_path_audio);

            // get pref objects
            defaultResolutionPreference =
                    (ListPreference) findPreference(DEFAULT_RESOLUTION_PREFERENCE);
            defaultAudioFormatPreference =
                    (ListPreference) findPreference(DEFAULT_AUDIO_FORMAT_PREFERENCE);
            searchLanguagePreference =
                    (ListPreference) findPreference(SEARCH_LANGUAGE_PREFERENCE);
            downloadPathPreference =
                    (EditTextPreference) findPreference(DOWNLOAD_PATH_PREFERENCE);
            downloadPathAudioPreference =
                    (EditTextPreference) findPreference(DOWNLOAD_PATH_AUDIO_PREFERENCE);

            prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                      String key) {
                    Activity a = getActivity();
                    if(a != null) {
                        updateSummary();
                    }
                }
            };
            defaultPreferences.registerOnSharedPreferenceChangeListener(prefListener);

            updateSummary();
        }

        // This is used to show the status of some preference in the description
        private void updateSummary() {
            defaultResolutionPreference.setSummary(
                    defaultPreferences.getString(DEFAULT_RESOLUTION_PREFERENCE,
                            getString(R.string.def_res_value)));
            defaultAudioFormatPreference.setSummary(
                    defaultPreferences.getString(DEFAULT_AUDIO_FORMAT_PREFERENCE,
                            getString(R.string.default_audio_format_value)));
            searchLanguagePreference.setSummary(
                    defaultPreferences.getString(SEARCH_LANGUAGE_PREFERENCE,
                            getString(R.string.default_language_value)));
            downloadPathPreference.setSummary(
                    defaultPreferences.getString(DOWNLOAD_PATH_PREFERENCE,
                            getString(R.string.dl_path_summary)));
            downloadPathAudioPreference.setSummary(
                    defaultPreferences.getString(DOWNLOAD_PATH_AUDIO_PREFERENCE,
                            getString(R.string.download_path_audio_summary)));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    private ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    @NonNull
    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            finish();
        }
        return true;
    }

}
