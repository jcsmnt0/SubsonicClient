package com.casamento.subsonicclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.util.ArrayList;
import java.util.List;

public class SubsonicClientActivity extends SherlockFragmentActivity {
	protected final static String logTag = "SubsonicClientActivity";
	protected final static String apiVersion = "1.4.0"; // 1.4.0+ required for JSON
	protected final static String clientId = "Android Subsonic Client";

	protected ServerBrowserFragment serverBrowserFragment;
	protected DownloadManagerFragment downloadManagerFragment;

	protected List<DownloadTask> downloadTasks;

	protected void addAndExecuteDownloadTask(final DownloadTask downloadTask) {
		this.downloadTasks.add(downloadTask);
		downloadTask.execute();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main);

		this.downloadTasks = new ArrayList<DownloadTask>();

		ActionBar actionBar = this.getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		ActionBar.Tab serverBrowserTab = actionBar.newTab().setText("Server");
		serverBrowserFragment = new ServerBrowserFragment();
		serverBrowserTab.setTabListener(new OnTabActionListener(serverBrowserFragment));
		actionBar.addTab(serverBrowserTab);

		ActionBar.Tab downloadManagerTab = actionBar.newTab().setText("Downloads");
		downloadManagerFragment = new DownloadManagerFragment();
		downloadManagerTab.setTabListener(new OnTabActionListener(downloadManagerFragment));
		actionBar.addTab(downloadManagerTab);
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		this.getSupportMenuInflater().inflate(R.menu.optionsmenu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
			case R.id.option_preferences:
				Intent settingsActivity = new Intent(this.getBaseContext(), PreferenceActivity.class);
				startActivity(settingsActivity);
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private class OnTabActionListener implements ActionBar.TabListener {
		public Fragment fragment;

		public OnTabActionListener(Fragment fragment) {
			this.fragment = fragment;
		}

		@Override
		public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
			// can do something on tab reselection (navigate to root maybe?)
		}

		@Override
		public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
			ft.replace(R.id.fragment_container, fragment);
		}

		@Override
		public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
			ft.remove(fragment);
		}
	}

// TODO: can't do this within fragments apparently, so detect active fragment and implement back handling as necessary in here
//    @Override
//    public void onBackPressed() {
//    	if (this.currentFolder != null) {
//	    	if (this.currentFolder.parent != null) {
//	    		this.setCurrentFolder(this.currentFolder.parent);
//	    	} else {
//	    		this.setCurrentMediaFolder(this.currentMediaFolder);
//	    	}
//	    	this.listView.setSelectionFromTop(this.savedScrollPositions.pop(), 0);
//	    	return;
//    	}
//    	super.onBackPressed();
//    }
}