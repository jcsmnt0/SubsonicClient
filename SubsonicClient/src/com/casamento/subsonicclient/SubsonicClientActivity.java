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
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

import java.util.*;

import static com.casamento.subsonicclient.FilesystemEntry.MediaFile;
import static com.casamento.subsonicclient.SubsonicCaller.*;

public class SubsonicClientActivity extends SherlockFragmentActivity
		implements ServerBrowserFragment.ActivityCallback, DownloadManagerFragment.ActivityCallback {
	protected static final String logTag = "SubsonicClientActivity";

	static boolean serverConnected = false;

	// DownloadService stuff

	private final List<DownloadListener> mDownloadListeners = new ArrayList<DownloadListener>();
	private final Map<String, Download> mDownloads = new HashMap<String, Download>();

	private void bindDownloadService() {
		bindService(new Intent(this, DownloadService.class), mDownloadServiceConnection,
				Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);
	}

	private void unbindDownloadService() {
		if (DownloadServiceOutbox.service != null) {
			DownloadServiceOutbox.postClientUnregistrationRequest(mDownloadServiceInbox);
		}

		unbindService(mDownloadServiceConnection);
	}

	@Override
	public void registerListener(DownloadListener dl) {
		mDownloadListeners.add(dl);
	}

	@Override
	public void unregisterListener(DownloadListener dl) {
		mDownloadListeners.remove(dl);
	}

	@Override
	public Collection<Download> getDownloadList(DownloadListener dl) {
		return mDownloads.values();
	}

	static interface DownloadListener {
		void onAddition(Download download);
		void onStart(Download download);
		void onProgressUpdate(Download download);
		void onCompletion(Download download);
	}

	private final ServiceConnection mDownloadServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(final ComponentName className, final IBinder service) {
			DownloadServiceOutbox.service = new Messenger(service);
			DownloadServiceOutbox.postClientRegistrationRequest(mDownloadServiceInbox);
		}

		@Override
		public void onServiceDisconnected(final ComponentName componentName) {
			// The service's process has crashed
			DownloadServiceOutbox.service = null;
		}
	};

	// TODO: refactor to use Intents for fun and profit
	private final Messenger mDownloadServiceInbox = new Messenger(new Handler() {
		@Override
		public void handleMessage(Message msg) {
			final Bundle msgData = msg.getData();
			msgData.setClassLoader(getClassLoader());

			switch (DownloadService.OutgoingMessages.values()[msg.what]) {
				case DOWNLOAD_ADDED: {
					final Download download = (Download) msgData.getParcelable("download");
					mDownloads.put(download.url, download);

					for (final DownloadListener dl : mDownloadListeners) {
						dl.onAddition(download);
					}
					break;
				}

				case DOWNLOAD_STARTED: {
					final Download d = mDownloads.get(msgData.getString("url"));
					d.setStarted();

					for (final DownloadListener dl : mDownloadListeners) {
						dl.onStart(d);
					}
					break;
				}

				case DOWNLOAD_PROGRESS_UPDATED: {
					final Download d = mDownloads.get(msgData.getString("url"));
					d.progress = msgData.getLong("progress");

					for (final DownloadListener dl : mDownloadListeners) {
						dl.onProgressUpdate(d);
					}
					break;
				}

				case DOWNLOAD_COMPLETED:
					final Download d = mDownloads.get(msgData.getString("url"));
					d.setCompleted();

					for (final DownloadListener dl : mDownloadListeners) {
						dl.onCompletion(d);
					}
					break;

				default:
					super.handleMessage(msg);
			}
		}
	});

	private static class DownloadServiceOutbox {
		private static Messenger service = null;

		private static void postClientRegistrationRequest(final Messenger replyTo) {
			try {
				final Message msg = Message.obtain(null, DownloadService.IncomingMessages.REGISTER_CLIENT.ordinal());
				msg.replyTo = replyTo;
				service.send(msg);
			} catch (RemoteException e) {
				// Service has crashed and will be automatically reconnected
				// This message will be automatically resent in mDownloadServiceConnection.onServiceConnected()
			}
		}

		private static void postClientUnregistrationRequest(final Messenger replyTo) {
			try {
				final Message msg = Message.obtain(null, DownloadService.IncomingMessages.UNREGISTER_CLIENT.ordinal());
				msg.replyTo = replyTo;
				service.send(msg);
			} catch (RemoteException e) {
				// Service has crashed and will be automatically reconnected
				// This message doesn't need to be resent, since if the service has crashed it's lost its list
				// of registered clients already
			}
		}

		private static void postDownloadInitiationRequest(final MediaFile mediaFile, final boolean transcoded) {
			final Bundle msgData = new Bundle();

			try {
				final String url = SubsonicCaller.getDownloadUrl(mediaFile, transcoded);
				final String savePath = Environment.getExternalStorageDirectory().toString() + "/SubsonicClient/" +
						mediaFile.path.substring(0, mediaFile.path.lastIndexOf('.') + 1) +
						(transcoded && mediaFile.transcodedSuffix != null ?
								mediaFile.transcodedSuffix : mediaFile.suffix);

				msgData.putString("url", url);
				msgData.putString("name", mediaFile.name);
				msgData.putString("savePath", savePath);
				msgData.putString("username", SubsonicCaller.getUsername());
				msgData.putString("password", SubsonicCaller.getPassword());

				try {
					postMessage(DownloadService.IncomingMessages.INITIATE_DOWNLOAD, msgData);
				} catch (RemoteException e) {
					// Service has crashed and will be automatically restarted
					// TODO: queue message and resend once service is back up
				}
			} catch (Throwable t) {
				Log.e(logTag, "Error", t);
			}
		}

		private static void postMessage(DownloadService.IncomingMessages messageType, Bundle msgData) throws RemoteException {
			final Message msg = Message.obtain(null, messageType.ordinal());
			msg.setData(msgData);

			service.send(msg);
		}
	}

	@Override
	public void initiateDownload(MediaFile mediaFile, boolean transcoded) {
		DownloadServiceOutbox.postDownloadInitiationRequest(mediaFile, transcoded);
	}

	// for fragment management
	private ServerBrowserFragment mServerBrowserFragment;
	private DownloadManagerFragment mDownloadManagerFragment;

	private void pushFragment(Fragment fragment) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		transaction.replace(R.id.fragment_container, fragment);
		transaction.addToBackStack(null);
		transaction.commit();
	}

	@Override
	protected void onDestroy() {
		unbindDownloadService();
	}

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

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminateVisibility(false);

		setContentView(R.layout.main);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

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
						hideProgressSpinner();
					} else {
						hideProgressSpinner();
						showDialogFragment(new AlertDialogFragment.Builder(getApplicationContext())
								.setTitle(R.string.error)
								.setMessage("Something bad happened when getting the data.")
								.setNeutralButton(R.string.ok)
								.create());
					}
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

		bindDownloadService();
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

	class ServerNotSetUpException extends Exception {
		ServerNotSetUpException(String message) { super(message); }
	}

	private void connectToServer() throws ServerNotSetUpException {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		String serverUrl, username, password;
		if (TextUtils.isEmpty(serverUrl = prefs.getString("serverUrl", "")) ||
				TextUtils.isEmpty(username = prefs.getString("username", "")) ||
				TextUtils.isEmpty(password = prefs.getString("password", "")))
			throw new ServerNotSetUpException("The server has not been fully set up.");

		setServerDetails(serverUrl, username, password, this);
		serverConnected = true;
	}

	private RetrieveCursorTask getRetrieveCursorTask(OnCursorRetrievedListener callbackListener) throws ServerNotSetUpException {
		if (!serverConnected) connectToServer();
		return new RetrieveCursorTask(callbackListener);
	}

	private RetrieveCursorTask getRetrieveCursorTask(FilesystemEntry.Folder folder, OnCursorRetrievedListener callbackListener) throws ServerNotSetUpException {
		if (!serverConnected) connectToServer();
		return new RetrieveCursorTask(folder, callbackListener);
	}

	private void showDialogFragment(final DialogFragment dialogFragment) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dialogFragment.show(getSupportFragmentManager(), "dialog");
			}
		});
	}

	private void dismissDialogFragment(final DialogFragment dialogFragment) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dialogFragment.dismiss();
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

	@Override
	public void pushServerBrowserFragment(FilesystemEntry.Folder folder) {
		showProgressSpinner();

		try {
			getRetrieveCursorTask(folder, new OnCursorRetrievedListener() {
				@Override
				public void onCursorRetrieved(Cursor cursor) {
					mServerBrowserFragment = new ServerBrowserFragment(cursor);
					pushFragment(mServerBrowserFragment);
					hideProgressSpinner();
				}

				@Override
				public void onException(Exception e) {
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
}