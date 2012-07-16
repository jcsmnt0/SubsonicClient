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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.util.ArrayList;
import java.util.List;

public class SubsonicClientActivity extends SherlockFragmentActivity implements ServerBrowserFragment.ActivityCallbackInterface,
																				DownloadManagerFragment.ActivityCallbackInterface {
	protected static final String logTag = "SubsonicClientActivity";

	// for fragment management
	private ServerBrowserFragment serverBrowserFragment;
	private DownloadManagerFragment downloadManagerFragment;

	// for com.casamento.subsonicclient.DownloadManagerFragment
	private List<DownloadTask> downloadTasks;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main);

		ActionBar actionBar = this.getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		class OnTabActionListener implements ActionBar.TabListener {
			private Fragment fragment;

			public OnTabActionListener(Fragment fragment) {
				super();
				this.fragment = fragment;
			}

			@Override
			public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
				ft.replace(R.id.fragment_container, fragment);
			}

			@Override
			public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
				ft.remove(fragment);
			}

			@Override
			public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
				// do something?
			}
		}

		ActionBar.Tab serverBrowserTab = actionBar.newTab().setText("Server");
		serverBrowserFragment = new ServerBrowserFragment();
		serverBrowserTab.setTabListener(new OnTabActionListener(serverBrowserFragment));
		actionBar.addTab(serverBrowserTab);

		ActionBar.Tab downloadManagerTab = actionBar.newTab().setText("Downloads");
		downloadManagerFragment = new DownloadManagerFragment();
		downloadManagerTab.setTabListener(new OnTabActionListener(downloadManagerFragment));
		actionBar.addTab(downloadManagerTab);

		// for com.casamento.subsonicclient.DownloadManagerFragment
		this.downloadTasks = new ArrayList<DownloadTask>();
	}

	@Override
	public boolean onCreateOptionsMenu(final com.actionbarsherlock.view.Menu menu) {
		this.getSupportMenuInflater().inflate(R.menu.optionsmenu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
			case R.id.option_preferences:
				Intent settingsActivity = new Intent(this.getBaseContext(), PreferenceActivity.class);
				startActivity(settingsActivity);
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}


	// ServerBrowserInterface methods

	@Override
	public void initiateDownload(final DownloadTask downloadTask) {
		this.downloadTasks.add(downloadTask);
		downloadTask.execute();
	}

	@Override
	public SubsonicCaller getSubsonicCaller() throws IllegalStateException {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		String serverUrl, username, password;
		if ((serverUrl = prefs.getString("serverUrl", "")).equals("") ||
				(username = prefs.getString("username", "")).equals("") ||
				(password = prefs.getString("password", "")).equals(""))
			throw new IllegalStateException("The server has not been fully set up.");

		return new SubsonicCaller(serverUrl, username, password, this);
	}

	@Override
	public void showDialogFragment(DialogFragment dialogFragment) {
		dialogFragment.show(this.getSupportFragmentManager(), "dialog");
	}


	// DownloadManagerInterface methods

	@Override
	public List<DownloadTask> getDownloadTasks() {
		return this.downloadTasks;
	}
}