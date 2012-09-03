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

import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import java.util.Stack;

import static com.casamento.subsonicclient.SubsonicCaller.CursorTask;
import static com.casamento.subsonicclient.SubsonicCaller.setServerDetails;

public class MainActivity extends SherlockFragmentActivity
        implements ServerBrowserFragment.ActivityCallback, DownloadManagerFragment.ActivityCallback {
    private static final String logTag = "MainActivity";

    static boolean serverConnected = false;

    // DownloadService stuff
    private DownloadService mDownloadService;

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
            try {
                getCursorTask(f, new CursorTask.Adapter() {
                    @Override
                    public void onResult(final Cursor cursor) {
                        final int entryCount = cursor.getCount();
                        cursor.moveToFirst();

                        for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
                            download(FilesystemEntry.getInstance(cursor), transcoded);
                            cursor.moveToNext();
                        }
                    }

                    @Override
                    public void onException(final Exception e) {
                        Log.e(logTag, "Error", e);
                    }
                }).execute();
            } catch (ServerNotSetUpException e) {
                showDialogFragment(new AlertDialogFragment.Builder(this)
                        .setMessage(e.getLocalizedMessage())
                        .setTitle("Error")
                        .setNeutralButton("OK")
                        .create());
            }
        } else {
            final MediaFile mediaFile = (MediaFile) entry;

            try {
                final String url = (String) SubsonicCaller.getDownloadUrl(mediaFile, transcoded);
                final String savePath = Environment.getExternalStorageDirectory().toString() + "/SubsonicClient/" +
                        mediaFile.path.substring(0, mediaFile.path.lastIndexOf('.') + 1) +
                        (transcoded && mediaFile.transcodedSuffix != null ?
                                mediaFile.transcodedSuffix : mediaFile.suffix);

                mDownloadService.queue(url, mediaFile.name, savePath, SubsonicCaller.getUsername(),
                        SubsonicCaller.getPassword());
            } catch (final Exception e) {
                Log.e(logTag, "Error", e);
            }
        }
    }

    // fragment management stuff
    private ServerBrowserFragment mServerBrowserFragment;
    private ActionBar.Tab mServerBrowserTab;

    private DownloadManagerFragment mDownloadManagerFragment;
    private ActionBar.Tab mDownloadManagerTab;

    //private Stack<ServerBrowserFragment> serverBrowserFragmentStack = new Stack<ServerBrowserFragment>();
    private Stack<Folder> mFolderStack = new Stack<Folder>();

    private void showFragment(final Fragment fragment) {
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindDownloadService();
    }

    class TabActionListener implements ActionBar.TabListener {
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
            // TODO: do something?
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setSupportProgressBarIndeterminateVisibility(false);

        setContentView(R.layout.main);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mServerBrowserFragment = new ServerBrowserFragment();

        mServerBrowserTab = actionBar.newTab().setText("Server");
        mServerBrowserTab.setTabListener(new TabActionListener(mServerBrowserFragment));
        actionBar.addTab(mServerBrowserTab);

        showProgressSpinner();

        showFolderContents(null, false);

        mDownloadManagerTab = actionBar.newTab().setText("Downloads");
        mDownloadManagerFragment = new DownloadManagerFragment();
        mDownloadManagerTab.setTabListener(new TabActionListener(mDownloadManagerFragment));
        actionBar.addTab(mDownloadManagerTab);

        bindDownloadService();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getSupportMenuInflater().inflate(R.menu.optionsmenu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_preferences:
                startActivity(new Intent(getBaseContext(), PreferenceActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class ServerNotSetUpException extends Exception {
        ServerNotSetUpException() { super(getString(R.string.server_not_set_up)); }
    }

    private void connectToServer() throws ServerNotSetUpException {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final String serverUrl, username, password;
        if (TextUtils.isEmpty(serverUrl = prefs.getString("serverUrl", "")) ||
                TextUtils.isEmpty(username = prefs.getString("username", "")) ||
                TextUtils.isEmpty(password = prefs.getString("password", "")))
            throw new ServerNotSetUpException();

        setServerDetails(serverUrl, username, password, this);
        serverConnected = true;
    }

    // TODO: replace with ContentProvider
    private CursorTask getCursorTask(final Folder folder, final CursorTask.Listener callbackListener) throws ServerNotSetUpException {
        if (!serverConnected) connectToServer();
        return new CursorTask(folder, callbackListener);
    }

    private void showDialogFragment(final DialogFragment dialogFragment) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialogFragment.show(getSupportFragmentManager(), "dialog");
            }
        });
    }

    private void showProgressSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setSupportProgressBarIndeterminateVisibility(true);
            }
        });
    }

    private void hideProgressSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setSupportProgressBarIndeterminateVisibility(false);
            }
        });
    }

    private void setServerBrowserFragment(final ServerBrowserFragment sbf, final boolean addToStack) {
        if (addToStack)
            mFolderStack.push(mServerBrowserFragment.getFolder());

        mServerBrowserFragment = sbf;
        mServerBrowserTab.setTabListener(new TabActionListener(mServerBrowserFragment));

        if (getSupportActionBar().getSelectedTab().equals(mServerBrowserTab))
            showFragment(mServerBrowserFragment);
    }

    @Override
    public void popServerBrowserFragment() {
        showFolderContents(mFolderStack.pop(), false);
    }

    @Override
    public void showFolderContents(final Folder folder, final boolean addToStack) {
        showProgressSpinner();

        try {
            getCursorTask(folder, new CursorTask.Adapter() {
                @Override
                public void onPreExecute() {
                    try {
                        mServerBrowserFragment.setLoading(true);
                        mServerBrowserFragment.setListAdapter(null);
                        mServerBrowserFragment.setEmptyText("Downloading information from server...");
                    } catch (final IllegalStateException e) {
                        // sometimes mServerBrowserFragment hasn't had its content view created, even if it's on screen.
                        // this is pretty harmless, but TODO: find out why and fix it instead of ignoring the exception
                    }
                }

                @Override
                public void onProgressUpdate(final Integer... p) {
                    final CharSequence progressText = TextUtils.expandTemplate("Retrieved ^1 files/folders",
                            Integer.toString(p[0]));

                    try {
                        mServerBrowserFragment.setEmptyText(progressText);
                    } catch (final IllegalStateException e) {
                        // see comment for onPreExecute()
                    }
                }

                @Override
                public void onResult(final Cursor cursor) {
                    setServerBrowserFragment(new ServerBrowserFragment(cursor, folder), addToStack);
                    hideProgressSpinner();
                }

                @Override
                public void onException(final Exception e) {
                    showDialogFragment(new AlertDialogFragment(getApplicationContext(), R.string.error,
                            e.getLocalizedMessage()));
                    hideProgressSpinner();
                }
            }).execute((Void) null);
        } catch (ServerNotSetUpException e) {
            showDialogFragment(new AlertDialogFragment(getApplicationContext(), R.string.error,
                    e.getLocalizedMessage()));
            hideProgressSpinner();
        }
    }

    @Override
    public void refresh(final ServerBrowserFragment sbf) {
        final Folder f = sbf.getFolder();
        SubsonicCaller.delete(f);
        showFolderContents(f, false);
    }
}
