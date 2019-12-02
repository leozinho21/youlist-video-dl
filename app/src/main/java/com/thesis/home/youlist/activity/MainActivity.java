package com.thesis.home.youlist.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.support.v7.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.Plus;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.squareup.picasso.Picasso;
import com.thesis.home.youlist.R;
import com.thesis.home.youlist.adapters.NewVideoListAdapter;
import com.thesis.home.youlist.adapters.SuggestionListAdapter;
import com.thesis.home.youlist.exceptions.ExtractionException;
import com.thesis.home.youlist.fragments.AbstractVideoFragment;
import com.thesis.home.youlist.fragments.UserFeedsTabFragment;
import com.thesis.home.youlist.fragments.VideoItemDetailFragment;
import com.thesis.home.youlist.fragments.VideoItemListFragment;
import com.thesis.home.youlist.fragments.homeTabFragments.MostPopularVidFragment;
import com.thesis.home.youlist.helpers.AndroidUtils;
import com.thesis.home.youlist.helpers.CircularTransform;
import com.thesis.home.youlist.helpers.Downloader;
import com.thesis.home.youlist.helpers.ListUtils;
import com.thesis.home.youlist.helpers.YoutubeContent;
import com.thesis.home.youlist.helpers.YoutubeHelper;
import com.thesis.home.youlist.model.Constants;
import com.thesis.home.youlist.services.YoutubeSearchEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener,
        AbstractVideoFragment.Callbacks {

    NavigationView navigationView;

    private GoogleApiClient mGoogleApiClient;
    private ProgressDialog mProgressDialog;

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    /**
     * List of presented account on device.
     */
    List<Account> accountList;
    GoogleSignInAccount acct;

    private static YouTube youtube;
    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = new GsonFactory();
    GoogleAccountCredential credential;
    private String mChosenAccountName;
    public static final String ACCOUNT_KEY = "accountName";
    /**
     * Authenticated user token.
     */
    static final String GOOGLE_ACCOUNT_TYPE = "com.google";
    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1001;
    static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1002;
    final static String YOUTUBE_SCOPE
            = "oauth2:https://www.googleapis.com/auth/youtube";
    static String[] scopes = new String[0];

    // savedInstanceBundle arguments
    private static final String QUERY = "query";
    private static final String STREAMING_SERVICE = "streaming_service";

    private static final int SEARCH_MODE = 0;
    private static final int PRESENT_VIDEOS_MODE = 1;

    private String searchQuery = "";
    private int mode = SEARCH_MODE;
    private SuggestionListAdapter       suggestionListAdapter;
    private SuggestionSearchRunnable    suggestionSearchRunnable;
    private VideoItemDetailFragment     videoFragment = null;
    private Menu menu = null;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private AbstractVideoFragment listFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            Intent intent = getIntent();
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                searchQuery = intent.getStringExtra(SearchManager.QUERY);
            }

        if(savedInstanceState == null){
            setContentView(R.layout.activity_main);

            // Configure sign-in to request the user's ID, email address, and basic profile. ID and
            // basic profile are included in DEFAULT_SIGN_IN.
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            // [START build_client]
            // Build a GoogleApiClient with access to the Google Sign-In API and the
            // options specified by gso.
            // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
            // See https://g.co/AppIndexing/AndroidStudio for more information.
            if(mGoogleApiClient == null)
                mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                        .enableAutoManage(this /* FragmentActivity */, this/* OnConnectionFailedListener */)
                        .addApi(Plus.API)
                        .addApi(Auth.CREDENTIALS_API)
                        .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                        .build();
            // [END build_client]

            /**
             * The toolbar
             */
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();

            if(navigationView==null)
            {
                navigationView = (NavigationView) findViewById(R.id.nav_view);
            }
            navigationView.setNavigationItemSelectedListener(MainActivity.this);

            // Button listeners
            SignInButton signInButton = (SignInButton) navigationView.getHeaderView(0).findViewById(R.id.sign_in_button);
            signInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signIn(v);
                }
            });
            signInButton.setSize(SignInButton.SIZE_STANDARD);
            signInButton.setScopes(gso.getScopeArray());
            Button signoutButton = (Button) navigationView.getHeaderView(0).findViewById(R.id.sign_out_button);
            signoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signOut(v);
                }
            });
        }

            if(AndroidUtils.isDeviceOnline(this)){
                if (savedInstanceState != null) {
                    mChosenAccountName = savedInstanceState.getString(ACCOUNT_KEY);
                } else {
//                    loadAccount();
                }
                if (mChosenAccountName != null){
                    if(credential == null){
                        initCredentials(mChosenAccountName);
                    }
                    if(youtube == null){
                        initYoutube();
                        Snackbar.make(findViewById(R.id.container_body), "Logged as " + mChosenAccountName, Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }
            }
            else{
                Snackbar.make(findViewById(R.id.container_body), "No connection.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }


            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            if(!searchQuery.isEmpty() ) {
                if(listFragment == null) {
                    listFragment = new VideoItemListFragment();
//                    ((VideoItemListFragment) listFragment).setStreamingService(youtubeService);
                }
                ((VideoItemListFragment)listFragment).search(searchQuery);
                fragmentTransaction.replace(R.id.container_body, listFragment);
                fragmentTransaction.commit();
            }

            else if(mChosenAccountName != null){
                initCredentials(mChosenAccountName);
                initYoutube();
                listFragment = new MostPopularVidFragment();
                fragmentTransaction.replace(R.id.container_body, listFragment);
                fragmentTransaction.commit();
            }

    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mGoogleApiClient != null) mGoogleApiClient.disconnect();
    }

    @Override
    protected void onDestroy() {
        youtube         = null;
        mProgressDialog = null;

        super.onDestroy();
    }

    private void initCredentials(String mChosenAccountName){

        scopes = ListUtils.toArray(YouTubeScopes.all());

        credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(scopes)).setSelectedAccountName(mChosenAccountName);
        // set exponential backoff policy
        credential.setBackOff(new ExponentialBackOff());
    }

    private void initYoutube(){

        youtube = new YouTube.Builder(transport, jsonFactory,
                credential).setApplicationName(Constants.APP_NAME)
                .build();
    }

    @Override
    public void onItemSelected(String id,String videoType) {

        commitResult(id,videoType);
    }

    private void commitResult(String id,String videoType){

        String webpageUrl;
        if(YoutubeHelper.UPLOAD_VIDEO_TYPE.equals(videoType)){
            webpageUrl = YoutubeContent.ITEM_MAP.get(YoutubeHelper.UPLOAD_VIDEO_TYPE).get((int) Long.parseLong(id)).webpage_url;
        }
        else if(YoutubeHelper.WATCH_HISTORY_VIDEO_TYPE.equals(videoType)){
            webpageUrl = YoutubeContent.ITEM_MAP.get(YoutubeHelper.WATCH_HISTORY_VIDEO_TYPE).get((int) Long.parseLong(id)).webpage_url;
        }
        else if(YoutubeHelper.WATCH_LATER_VIDEO_TYPE.equals(videoType)){
            webpageUrl = YoutubeContent.ITEM_MAP.get(YoutubeHelper.WATCH_LATER_VIDEO_TYPE).get((int) Long.parseLong(id)).webpage_url;
        }
        else if(YoutubeHelper.TRENDING_VIDEO_TYPE.equals(videoType)){
            webpageUrl = YoutubeContent.ITEM_MAP.get(YoutubeHelper.TRENDING_VIDEO_TYPE).get((int) Long.parseLong(id)).webpage_url;
        }
        else{
            NewVideoListAdapter listAdapter = (NewVideoListAdapter) (listFragment)
                    .getListAdapter();
            webpageUrl = listAdapter.getVideoList().get((int) Long.parseLong(id)).webpage_url;
        }

        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(VideoItemDetailFragment.VIDEO_URL, webpageUrl);
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, VideoDetailActivity.class);
//            detailIntent.putExtra(VideoItemDetailFragment.ARG_ITEM_ID, id);
            detailIntent.putExtra(VideoItemDetailFragment.VIDEO_URL, webpageUrl);
            startActivity(detailIntent);
        }
        hideProgressDialog();
    }

    private void saveAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        sp.edit().putString(ACCOUNT_KEY, mChosenAccountName).commit();
    }

    private void loadAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        mChosenAccountName = sp.getString(ACCOUNT_KEY, null);
        invalidateOptionsMenu();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
       if(mGoogleApiClient != null){

           mGoogleApiClient.connect();

           OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
           if (opr.isDone()) {
               // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
               // and the GoogleSignInResult will be available instantly.
               Log.d(TAG, "Got cached sign-in");
               GoogleSignInResult result = opr.get();
               handleSignInResult(result);
           } else {
               // If the user has not previously signed in on this device or the sign-in has expired,
               // this asynchronous branch will attempt to sign in the user silently.  Cross-device
               // single sign-on will occur in this branch.
               showProgressDialog();
               opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                   @Override
                   public void onResult(GoogleSignInResult googleSignInResult) {
                       hideProgressDialog();
                       handleSignInResult(googleSignInResult);
                   }
               });
           }
       }
    }


    // [START handleSignInResult]
    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            acct = result.getSignInAccount();
            mChosenAccountName = acct.getEmail();

            saveAccount();

            updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }
    // [END handleSignInResult]

    // [START signIn]
    private void signIn(View v) {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signIn]

    // [START signOut]
    private void signOut(View v) {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        updateUI(false);
                        // [END_EXCLUDE]
                    }
                });

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        sp.edit().putString(ACCOUNT_KEY, null).commit();
    }
    // [END signOut]

    /**
     * This method is a hook for background threads and async tasks that need to
     * provide the user a response UI when an exception occurs.
     */
    public void handleException(final Exception e) {
        // Because this call comes from the AsyncTask, we must ensure that the following
        // code instead executes on the UI thread.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException) e)
                            .getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                            MainActivity.this,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException) e).getIntent();
                    startActivityForResult(intent,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            // Receiving a result from the AccountPicker
            if (resultCode == RESULT_OK) {
                // With the account name acquired, go get the auth token
               mChosenAccountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                // With the account name acquired, go get the auth token
//                getUsername();

            } else if (resultCode == RESULT_CANCELED) {
                // The account picker dialog closed without selecting an account.
                // Notify users that they must pick an account to proceed.
                Toast.makeText(this, R.string.pick_account, Toast.LENGTH_SHORT).show();
            }
        } else if ((requestCode == REQUEST_CODE_RECOVER_FROM_AUTH_ERROR ||
                requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR)
                && resultCode == RESULT_OK) {
            // Receiving a result that follows a GoogleAuthException, try auth again
        }

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
       if(mGoogleApiClient != null) {
           mGoogleApiClient.connect();
       }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();

        this.menu = menu;
//        inflater.inflate(R.menu.main, menu);

        suggestionListAdapter = new SuggestionListAdapter(this);

        // Get the SearchView and set the searchable configuration
        if(mode != PRESENT_VIDEOS_MODE &&
                findViewById(R.id.videoitem_detail_container) == null) {
            inflater.inflate(R.menu.videoitem_list, menu);
            MenuItem searchItem = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setFocusable(false);
            searchView.setOnQueryTextListener(
                    new SearchVideoQueryListener());
            suggestionListAdapter = new SuggestionListAdapter(this);
            searchView.setSuggestionsAdapter(suggestionListAdapter);
            searchView.setOnSuggestionListener(new SearchSuggestionListener(searchView));
            if(!searchQuery.isEmpty()) {
                searchView.setQuery(searchQuery,false);
                searchView.setIconifiedByDefault(false);
            }
        } else if (videoFragment != null){
            videoFragment.onCreateOptionsMenu(menu, inflater);
        } else {
            inflater.inflate(R.menu.videoitem_two_pannel, menu);
        }
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, ConfigActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    private void updateUI(boolean signedIn) {
        ImageView loggedUserProfilePic = (ImageView) navigationView.getHeaderView(0).findViewById(R.id.imgProfilePic);
        if (signedIn) {
            if(acct.getPhotoUrl() != null){
                Picasso.with(getApplicationContext()).load(acct.getPhotoUrl().toString()).resize(100,100).transform(new CircularTransform()).into(loggedUserProfilePic);

            }


            navigationView.getHeaderView(0).findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            navigationView.getHeaderView(0).findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);

            initCredentials(mChosenAccountName);
            initYoutube();
        } else {

            loggedUserProfilePic.setImageBitmap(null);
            loggedUserProfilePic.setImageResource(R.drawable.user_group_128);
            navigationView.getHeaderView(0).findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            navigationView.getHeaderView(0).findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_SHORT).show();

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.user_feeds) {

            if (mChosenAccountName == null) {
                    signIn(null);
            }
            else{
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.container_body,new UserFeedsTabFragment());// UserFeedsTabFragment.newInstance(mChosenAccountName));
                fragmentTransaction.commit();
            }

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putString(QUERY, searchQuery);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            Toast.makeText(MainActivity.this,
                    R.string.connection_to_google_play_failed, Toast.LENGTH_SHORT)
                    .show();

            Log.e(TAG,
                    String.format(
                            "Connection to Play Services Failed, error: %d, reason: %s",
                            connectionResult.getErrorCode(),
                            connectionResult.toString()));
            try {
                connectionResult.startResolutionForResult(MainActivity.this, 0);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, e.toString(), e);
            }
        }
    }

    @Override
    public boolean onSearchRequested() {

        return super.onSearchRequested();
    }
    public static YouTube getYoutube() {
        return youtube;
    }


    private class SearchVideoQueryListener implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            try {
                searchQuery = query;

                // Ensure list fragment is video item list fragment
                if( !(listFragment instanceof VideoItemListFragment)){
                    listFragment = new VideoItemListFragment();
//                    ((VideoItemListFragment)listFragment).setStreamingService(youtubeService);
                }

                if(!listFragment.isAdded()){
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.replace(R.id.container_body,listFragment);
                    fragmentTransaction.commit();
                    fragmentManager.executePendingTransactions();
                }
                if(AndroidUtils.isDeviceOnline(MainActivity.this)) {
                    ((VideoItemListFragment) listFragment).search(query);
                    // hide virtual keyboard
                    InputMethodManager inputManager =
                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    try {
                        //noinspection ConstantConditions
                        inputManager.hideSoftInputFromWindow(
                                getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    } catch (NullPointerException e) {
                        Log.e(TAG, "Could not get widget with focus");
                        Toast.makeText(MainActivity.this, "Could not get widget with focus",
                                Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);

//                commitVideoItemDetailFragment();
                // clear focus
                // 1. to not open up the keyboard after switching back to this
                // 2. It's a workaround to a seeming bug by the Android OS it self, causing
                //    onQueryTextSubmit to trigger twice when focus is not cleared.
                // See: http://stackoverflow.com/questions/17874951/searchview-onquerytextsubmit-runs-twice-while-i-pressed-once
                getCurrentFocus().clearFocus();

            } catch(Exception e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if(!newText.isEmpty()) {
                searchSuggestions(newText);
            }
            return true;
        }

    }
    private class SearchSuggestionListener implements SearchView.OnSuggestionListener{

        private SearchView searchView;

        private SearchSuggestionListener(SearchView searchView) {
            this.searchView = searchView;
        }

        @Override
        public boolean onSuggestionSelect(int position) {
            String suggestion = suggestionListAdapter.getSuggestion(position);
            searchView.setQuery(suggestion,true);
            return false;
        }

        @Override
        public boolean onSuggestionClick(int position) {
            String suggestion = suggestionListAdapter.getSuggestion(position);
            searchView.setQuery(suggestion,true);
            return false;
        }
    }

    private class SuggestionResultRunnable implements Runnable{

        private ArrayList<String>suggestions;

        private SuggestionResultRunnable(ArrayList<String> suggestions) {
            this.suggestions = suggestions;
        }

        @Override
        public void run() {
            suggestionListAdapter.updateAdapter(suggestions);
        }
    }

    private class SuggestionSearchRunnable implements Runnable{
        private final String query;
        final Handler h = new Handler();
        private Context context;
        private SuggestionSearchRunnable(String query) {
            this.query = query;
            context = MainActivity.this;
        }

        @Override
        public void run() {
            try {
                YoutubeSearchEngine engine = new YoutubeSearchEngine();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                String searchLanguageKey = context.getString(R.string.language_key);
                String searchLanguage = sp.getString(searchLanguageKey,
                        getString(R.string.default_language_value));
                ArrayList<String>suggestions = engine.suggestionList(query,searchLanguage,new Downloader());
                h.post(new SuggestionResultRunnable(suggestions));

            } catch (ExtractionException e) {
                e.printStackTrace();
            } catch (IOException e) {
                postNewErrorToast(h, R.string.network_error);
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void postNewErrorToast(Handler h, final int stringResource) {
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, getString(stringResource),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void searchSuggestions(String query) {
        suggestionSearchRunnable =
                new SuggestionSearchRunnable(query);
        Thread searchThread;
        searchThread = new Thread(suggestionSearchRunnable);
        searchThread.start();

    }
}