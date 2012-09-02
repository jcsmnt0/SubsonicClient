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
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import java.util.*;

import static com.casamento.subsonicclient.FilesystemEntry.Folder;
import static com.casamento.subsonicclient.FilesystemEntry.MediaFile;
import static com.casamento.subsonicclient.SubsonicCaller.*;

public class SubsonicClientActivity extends SherlockFragmentActivity
		implements ServerBrowserFragment.ActivityCallback, DownloadManagerFragment.ActivityCallback {
	protected static final String logTag = "SubsonicClientActivity";

	static boolean serverConnected = false;

	// DownloadService stuff

	private final Collection<DownloadListener> mDownloadListeners = new ArrayList<DownloadListener>();
	private final Map<String, Download> mDownloads = new HashMap<String, Download>();

	private void bindDownloadService() {
		bindService(new Intent(this, DownloadService.class), mDownloadServiceConnection,
				Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);
	}

	private void unbindDownloadService() {
		if (DownloadServiceOutbox.service != null) {
			DownloadServiceOutbox.postClientUnregistration(mDownloadServiceInbox);
		}

		unbindService(mDownloadServiceConnection);
	}

	@Override
	public void registerListener(final DownloadListener dl) {
		mDownloadListeners.add(dl);
	}

	@Override
	public void unregisterListener(final DownloadListener dl) {
		mDownloadListeners.remove(dl);
	}

	@Override
	public Collection<Download> getDownloadList(final DownloadListener dl) {
		return mDownloads.values();
	}

	interface DownloadListener {
		void onAddition(Download download);
		void onStart(Download download);
		void onProgressUpdate(Download download);
		void onCompletion(Download download);
	}

	private final ServiceConnection mDownloadServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(final ComponentName className, final IBinder service) {
			DownloadServiceOutbox.service = new Messenger(service);
			DownloadServiceOutbox.postClientRegistration(mDownloadServiceInbox);
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
		public void handleMessage(final Message msg) {
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
		private static Messenger service;

		private static void postClientRegistration(final Messenger client) {
			try {
				final Message msg = Message.obtain(null, DownloadService.IncomingMessages.REGISTER_CLIENT.ordinal());
				msg.replyTo = client;
				service.send(msg);
			} catch (RemoteException e) {
				// Service has crashed and will be automatically reconnected
				// This message will be automatically resent in mDownloadServiceConnection.onServiceConnected()
			}
		}

		private static void postClientUnregistration(final Messenger client) {
			try {
				final Message msg = Message.obtain(null, DownloadService.IncomingMessages.UNREGISTER_CLIENT.ordinal());
				msg.replyTo = client;
				service.send(msg);
			} catch (RemoteException e) {
				// Service has crashed and will be automatically reconnected
				// This message doesn't need to be resent, since if the service has crashed it's lost its list
				// of registered clients already
			}
		}

		private static void postDownload(final MediaFile mediaFile, final boolean transcoded) {
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

				postMessage(DownloadService.IncomingMessages.INITIATE_DOWNLOAD, msgData);
			} catch (final RemoteException e) {
				// Service has crashed and will be automatically restarted
				// TODO: queue message and resend once service is back up
			} catch (final Throwable t) {
				Log.e(logTag, "Error", t);
			}
		}

		private static void postMessage(final DownloadService.IncomingMessages messageType, final Bundle msgData) throws RemoteException {
			final Message msg = Message.obtain(null, messageType.ordinal());
			msg.setData(msgData);

			service.send(msg);
		}
	}

	@Override
	public void download(final FilesystemEntry entry, final boolean transcoded) {
		if (entry.isFolder) {
			final Folder f = (Folder) entry;
			try {
				getRetrieveCursorTask(f, new CursorTaskListener() {
					@Override
					public void onPreExecute() {
					}

					@Override
					public void onProgressUpdate(final Integer... p) {
					}

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
			DownloadServiceOutbox.postDownload((MediaFile) entry, transcoded);
		}
	}

	// for fragment management
	private ServerBrowserFragment mServerBrowserFragment;
	private ActionBar.Tab mServerBrowserTab;

	private DownloadManagerFragment mDownloadManagerFragment;
	private ActionBar.Tab mDownloadManagerTab;

	private Stack<ServerBrowserFragment> serverBrowserFragmentStack = new Stack<ServerBrowserFragment>();

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

	private CursorTask getRetrieveCursorTask(final Folder folder, final CursorTaskListener callbackListener) throws ServerNotSetUpException {
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

	private void showServerBrowserFragment(final ServerBrowserFragment sbf, final boolean addToStack) {
		if (addToStack)
			serverBrowserFragmentStack.push(mServerBrowserFragment);

		mServerBrowserFragment = sbf;
		mServerBrowserTab.setTabListener(new TabActionListener(mServerBrowserFragment));

		showFragment(mServerBrowserFragment);
	}

	@Override
	public void popServerBrowserFragment() {
		mServerBrowserFragment = serverBrowserFragmentStack.pop();
		mServerBrowserTab.setTabListener(new TabActionListener(mServerBrowserFragment));

		showFragment(mServerBrowserFragment);
	}

	@Override
	public void showFolderContents(final Folder folder, final boolean addToStack) {
		showProgressSpinner();

		try {
			getRetrieveCursorTask(folder, new CursorTaskListener() {
				@Override
				public void onPreExecute() {
					try {
						mServerBrowserFragment.setListAdapter(null);
						mServerBrowserFragment.setEmptyText("Downloading information from server...");
					} catch (final IllegalStateException e) {
						// sometimes mServerBrowserFragment hasn't had its content view created, even if it's on screen.
						// this is pretty harmless, but, TODO: find out why and fix it instead of ignoring the exception
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
					showServerBrowserFragment(new ServerBrowserFragment(cursor, folder), addToStack);
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