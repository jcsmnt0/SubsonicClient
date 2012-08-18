/*
 * Copyright (c) 2012, Joseph Casamento
 * All rights reserved.
 *
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

import java.util.ArrayList;
import java.util.List;

import static com.casamento.subsonicclient.SubsonicCaller.*;

public class SubsonicClientActivity extends SherlockFragmentActivity
		implements ServerBrowserFragment.ActivityCallbackInterface, DownloadManagerFragment.ActivityCallbackInterface {

	protected static final String logTag = "SubsonicClientActivity";

	static boolean serverConnected = false;

	// for fragment management
	private ServerBrowserFragment mServerBrowserFragment;
	private DownloadManagerFragment mDownloadManagerFragment;

	// for DownloadManagerFragment
	private List<DownloadTask> mDownloadTasks;

	private void pushFragment(Fragment fragment) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		transaction.replace(R.id.fragment_container, fragment);
		transaction.addToBackStack(null);
		transaction.commit();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminateVisibility(false);

		setContentView(R.layout.main);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		class OnTabActionListener implements ActionBar.TabListener {
			private Fragment mFragment;

			public OnTabActionListener(Fragment fragment) {
				super();
				mFragment = fragment;
			}

			@Override
			public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
				ft.replace(R.id.fragment_container, mFragment);
			}

			@Override
			public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
				ft.remove(mFragment);
			}

			@Override
			public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
				// do something?
			}
		}

		final ActionBar.Tab serverBrowserTab = actionBar.newTab().setText("Server");

		showProgressSpinner();
		try {
			RetrieveCursorTask task = getRetrieveCursorTask(new OnCursorRetrievedListener() {
				@Override
				public void onCursorRetrieved(Cursor cursor) {
					if (cursor != null && cursor.getCount() >= 0) {
						mServerBrowserFragment = new ServerBrowserFragment(cursor);
						serverBrowserTab.setTabListener(new OnTabActionListener(mServerBrowserFragment));
						actionBar.addTab(serverBrowserTab, 0);
						actionBar.selectTab(serverBrowserTab);
					} else {
						showDialogFragment(new AlertDialogFragment.Builder(getApplicationContext())
								.setTitle(R.string.error)
								.setMessage("Something bad happened when getting the data.")
								.setNeutralButton(R.string.ok)
								.create());
					}

					hideProgressSpinner();
				}

				@Override
				public void onException(Exception e) {
					e.printStackTrace();
					hideProgressSpinner();
					showDialogFragment(new AlertDialogFragment.Builder(getApplicationContext())
							.setTitle(R.string.error)
							.setMessage(e.getLocalizedMessage())
							.setNeutralButton(R.string.ok)
							.create());
				}
			});
			task.execute();
		} catch (Exception e) {
			hideProgressSpinner();
			e.printStackTrace();
			Log.e(logTag, e.getLocalizedMessage());
		}

		ActionBar.Tab downloadManagerTab = actionBar.newTab().setText("Downloads");
		mDownloadManagerFragment = new DownloadManagerFragment();
		downloadManagerTab.setTabListener(new OnTabActionListener(mDownloadManagerFragment));
		actionBar.addTab(downloadManagerTab);

		// for DownloadManagerFragment
		mDownloadTasks = new ArrayList<DownloadTask>();
	}

	@Override
	public boolean onCreateOptionsMenu(final com.actionbarsherlock.view.Menu menu) {
		getSupportMenuInflater().inflate(R.menu.optionsmenu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
			case R.id.option_preferences:
				Intent settingsActivity = new Intent(getBaseContext(), PreferenceActivity.class);
				startActivity(settingsActivity);
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}


	// ServerBrowserInterface methods

	@Override
	public void initiateDownload(final DownloadTask downloadTask) {
		mDownloadTasks.add(downloadTask);
		downloadTask.execute();
	}

	class ServerNotSetUpException extends Exception {
		ServerNotSetUpException(String message) { super(message); }
	}

	@Override
	public void connectToServer() throws ServerNotSetUpException {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		String serverUrl, username, password;
		if (TextUtils.isEmpty(serverUrl = prefs.getString("serverUrl", "")) ||
				TextUtils.isEmpty(username = prefs.getString("username", "")) ||
				TextUtils.isEmpty(password = prefs.getString("password", "")))
			throw new ServerNotSetUpException("The server has not been fully set up.");

		setServerDetails(serverUrl, username, password, this);
		serverConnected = true;
	}

	@Override
	public RetrieveCursorTask getRetrieveCursorTask(OnCursorRetrievedListener callbackListener) throws ServerNotSetUpException {
		if (!serverConnected) connectToServer();
		return new RetrieveCursorTask(callbackListener);
	}

	@Override
	public RetrieveCursorTask getRetrieveCursorTask(FilesystemEntry.Folder folder, OnCursorRetrievedListener callbackListener) throws ServerNotSetUpException {
		if (!serverConnected) connectToServer();
		return new RetrieveCursorTask(folder, callbackListener);
	}

	@Override
	public void showDialogFragment(DialogFragment dialogFragment) {
		dialogFragment.show(getSupportFragmentManager(), "dialog");
	}

	@Override
	public void showProgressSpinner() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setSupportProgressBarIndeterminateVisibility(true);
			}
		});
	}

	@Override
	public void hideProgressSpinner() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setSupportProgressBarIndeterminateVisibility(false);
			}
		});
	}

	@Override
	public void pushServerBrowserFragment(FilesystemEntry.Folder folder) {
		showProgressSpinner();
		RetrieveCursorTask task = null;
		try {
			task = getRetrieveCursorTask(folder, new OnCursorRetrievedListener() {
				@Override
				public void onCursorRetrieved(Cursor cursor) {
					pushFragment(new ServerBrowserFragment(cursor));
					hideProgressSpinner();
				}

				@Override
				public void onException(Exception e) {
					showDialogFragment(new AlertDialogFragment(getApplicationContext(), R.string.error,
							e.getLocalizedMessage()));
					hideProgressSpinner();
				}
			});
		} catch (ServerNotSetUpException e) {
			showDialogFragment(new AlertDialogFragment(getApplicationContext(), R.string.error,
					e.getLocalizedMessage()));
			hideProgressSpinner();
		}
		task.execute();
	}

	// DownloadManagerInterface methods

	@Override
	public List<DownloadTask> getDownloadTasks() {
		return mDownloadTasks;
	}
}