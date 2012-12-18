/*
 * Copyright (c) 2012, Joseph Casamento
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.casamento.subsonicclient;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

public class MainActivity extends Activity
        implements ServerBrowserFragment.ActivityCallback, DownloadManagerFragment.ActivityCallback {
    private DataSource mDataSource;

    // TODO: implement preference screen with list of servers, so this isn't hard-coded
    private DataSource.AccessInformation mSubInfo =
            new SubsonicService.SubsonicAccessInformation("http://subsonic.org/demo", "android-guest", "guest");

    static final int
            DOWNLOAD_LOADER          = 0,
            FILESYSTEM_ENTRY_LOADER  = 1,
            FOLDER_CONTENTS_LOADER   = 2,
            TOP_LEVEL_FOLDERS_LOADER = 3;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make the progress spinner on the right side of the action bar available
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(false);

        // Set up the test data source (TODO: remove once the preference screen works)
        mDataSource = new DataSource(this, "test", mSubInfo, SubsonicService.class);

        setContentView(R.layout.main);

        // Get an action bar with tabs
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set up the server browser fragment and its associated tab
        mServerBrowserFragment = new ServerBrowserFragment();

        mServerBrowserTab = actionBar.newTab().setText("Server");
        mServerBrowserTab.setTabListener(new TabActionListener(mServerBrowserFragment));
        actionBar.addTab(mServerBrowserTab);

        // The server browser starts loading as soon as it's added, so show the progress spinner
        showProgressSpinner();

        // Set up the download manager fragment and its associated tab
        mDownloadManagerTab = actionBar.newTab().setText("Downloads");
        mDownloadManagerFragment = new DownloadManagerFragment();
        mDownloadManagerTab.setTabListener(new TabActionListener(mDownloadManagerFragment));
        actionBar.addTab(mDownloadManagerTab);

        // Start the download service (if necessary)
        bindDownloadService();
    }

    @Override
    public DataSource getDataSource() {
        return mDataSource;
    }

    private DownloadService mDownloadService;

    // Starts the DownloadService and then binds it to this Activity, so it won't die when the Activity ends
    private void bindDownloadService() {
        final Intent i = new Intent(this, DownloadService.class);

        startService(i);
        bindService(i, mDownloadServiceConnection, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);
    }

    private void unbindDownloadService() {
        if (mDownloadService != null) {
            unbindService(mDownloadServiceConnection);
        }
    }

    public DownloadService getDownloadService() {
        return mDownloadService;
    }

    // Handle the callback when the DownloadService is bound
    private final ServiceConnection mDownloadServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            mDownloadService = ((DownloadService.ServiceBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(final ComponentName componentName) {
            // The service's process has crashed
            mDownloadService = null;
        }
    };

    @Override
    public void download(final FilesystemEntry entry, final boolean transcoded) {
        if (entry.isFolder) {
            final Folder f = (Folder) entry;

            // Load the contents of the folder
            final Loader<Cursor> contentsLoader = mDataSource.getFolderContentsCursorLoader(f, false);

            // Callback for when the loading is finished
            contentsLoader.registerListener(0, new Loader.OnLoadCompleteListener<Cursor>() {
                @Override
                public void onLoadComplete(final Loader<Cursor> cursorLoader, final Cursor cursor) {
                    // Queue a download for each item in the folder
                    cursor.moveToFirst();

                    for (int i = 0, len = cursor.getCount(); i < len; i++) {
                        download(FilesystemEntry.getInstance(cursor), transcoded);
                        cursor.moveToNext();
                    }
                }
            });

            contentsLoader.startLoading();
        } else {
            final MediaFile mediaFile = (MediaFile) entry;

            try {
                // Construct the path for the file based on its properties
                // TODO: this could stand to be a little less hideous
                final String savePath = Environment.getExternalStorageDirectory().toString() + "/SubsonicClient/" +
                        mediaFile.path.substring(0, mediaFile.path.lastIndexOf('.') + 1) +
                        (transcoded && mediaFile.transcodedSuffix != null ?
                                mediaFile.transcodedSuffix : mediaFile.suffix);

                final DataSource.AccessInformation accessInfo = mDataSource.getAccessInformation();
                final String url = accessInfo.getDownloadUrl(mediaFile, transcoded);
                final String username = accessInfo.getUsername();
                final String password = accessInfo.getPassword();

                mDownloadService.queue(mediaFile.name, url, savePath, username, password);
            } catch (final Exception e) {
                // TODO: better exception handling
                Log.e(getClass().getSimpleName(), "Error", e);
            }
        }
    }

    private ServerBrowserFragment mServerBrowserFragment;
    private ActionBar.Tab mServerBrowserTab;

    private DownloadManagerFragment mDownloadManagerFragment;
    private ActionBar.Tab mDownloadManagerTab;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindDownloadService();
    }

    // Action bar tab callbacks: on selection, show the relevant fragment; on unselection, hide the relevant fragment
    private class TabActionListener implements ActionBar.TabListener {
        private final Fragment mFragment;

        private TabActionListener(final Fragment fragment) {
            mFragment = fragment;
        }

        @Override
        public void onTabSelected(final ActionBar.Tab tab, final FragmentTransaction ft) {
            ft.replace(R.id.fragment_container, mFragment);
        }

        @Override
        public void onTabUnselected(final ActionBar.Tab tab, final FragmentTransaction ft) {
            ft.remove(mFragment);
        }

        @Override
        public void onTabReselected(final ActionBar.Tab tab, final FragmentTransaction ft) {
            // do nothing
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.optionsmenu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_preferences:
                startActivity(new Intent(getBaseContext(), PreferencesActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void showProgressSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setProgressBarIndeterminateVisibility(true);
            }
        });
    }

    @Override
    public void hideProgressSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setProgressBarIndeterminateVisibility(false);
            }
        });
    }

}
