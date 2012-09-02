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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DownloadService extends Service {
	private static final String logTag = "SubsonicClient/DownloadService";
	private static final List<Messenger> mClients = new ArrayList<Messenger>();

	// TODO: there's probably a more efficient data structure for this purpose
	private static final ArrayList<Download> mDownloads = new ArrayList<Download>();

	private static AsyncTask mCurrentTask;

	private Notification mNotification;

	static enum IncomingMessages {
		REGISTER_CLIENT,
		UNREGISTER_CLIENT,
		INITIATE_DOWNLOAD,
	}

	static enum OutgoingMessages {
		DOWNLOAD_ADDED,
		DOWNLOAD_STARTED,
		DOWNLOAD_PROGRESS_UPDATED,
		DOWNLOAD_COMPLETED,
	}

	private final DownloadTask.Listener mDownloadTaskListener = new DownloadTask.Listener() {
		@Override
		public void onStart(final Download d) {
			Outbox.postStart(d);
			updateNotification(d.name, d.savePath);
		}

		@Override
		public void onProgressUpdate(final Download d) {
			Outbox.postProgressUpdate(d);

			// SystemUI chokes if a notification is updated too frequently
			if (System.currentTimeMillis() % 1000 == 0)
				updateNotification(d.getProgressString());
		}

		@Override
		public void onCompletion(final Download d) {
			Outbox.postCompletion(d);
			mDownloads.remove(d);

			// start the next download, if there is one
			if (mDownloads.size() > 0)
				startDownload(mDownloads.get(0));
			else {
				mCurrentTask = null;
				cancelNotification();
			}
		}
	};

	private static class Outbox {
		private static void postAddition(final Download d) {
			final Bundle data = new Bundle();
			data.putParcelable("download", d);

			postMessage(OutgoingMessages.DOWNLOAD_ADDED, data);
		}

		private static void postStart(final Download d) {
			final Bundle data = new Bundle();
			//data.putParcelable("download", d);
			data.putString("url", d.url);

			postMessage(OutgoingMessages.DOWNLOAD_STARTED, data);
		}

		private static void postProgressUpdate(final Download d) {
			final Bundle data = new Bundle();
			//data.putParcelable("download", d);
			data.putString("url", d.url);
			data.putLong("progress", d.progress);

			postMessage(OutgoingMessages.DOWNLOAD_PROGRESS_UPDATED, data);
		}

		private static void postCompletion(final Download d) {
			final Bundle data = new Bundle();
			//data.putParcelable("download", d);
			data.putString("url", d.url);

			postMessage(OutgoingMessages.DOWNLOAD_COMPLETED, data);
		}

		private static void postMessage(OutgoingMessages messageType, Bundle msgData) {
			final Message msg = Message.obtain(null, messageType.ordinal());
			msg.setData(msgData);

			for (final Messenger client : mClients) {
				try {
					client.send(msg);
				} catch (RemoteException e) {
					mClients.remove(client);
				}
			}
		}
	}

	private final Messenger mInbox = new Messenger(new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			final Bundle msgData = msg.getData();

			switch (IncomingMessages.values()[msg.what]) {
				case REGISTER_CLIENT:
					mClients.add(msg.replyTo);
					break;

				case UNREGISTER_CLIENT:
					mClients.remove(msg.replyTo);
					break;

				case INITIATE_DOWNLOAD:
					final String url = msgData.getString("url");
					final String name = msgData.getString("name");
					final String savePath = msgData.getString("savePath");
					final String username = msgData.getString("username");
					final String password = msgData.getString("password");

					final Download d = new Download(url, name, savePath, username, password);

					mDownloads.add(d);
					Outbox.postAddition(d);

					// if d is the only download in the queue, start it; otherwise, it'll be started when the
					// current download is finished
					if (mDownloads.size() == 1)
						startDownload(d);

					break;

				default:
					super.handleMessage(msg);
			}
		}
	});

	private void startDownload(final Download d) {
		mCurrentTask = new DownloadTask(d, mDownloadTaskListener).execute();
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mNotification = createNotification();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mCurrentTask != null)
			mCurrentTask.cancel(true);

		mDownloads.clear();

		cancelNotification();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mInbox.getBinder();
	}

	private NotificationManager getNotificationManager() {
		return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	private void updateNotification(final String progress) {
		mNotification.contentView.setTextViewText(R.id.progress, progress);

		getNotificationManager().notify(R.string.download_service, mNotification);
	}

	private void updateNotification(String name, String path) {
		mNotification.contentView.setTextViewText(R.id.name, name);
		mNotification.contentView.setTextViewText(R.id.path, path);

		// clear progress text view
		updateNotification("");

		getNotificationManager().notify(R.string.download_service, mNotification);
	}

	private void cancelNotification() {
		getNotificationManager().cancel(R.string.download_service);
	}

	private Notification createNotification() {
		final Intent notificationIntent = new Intent(this, SubsonicClientActivity.class);
		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		final RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.download_notification_layout);
		contentView.setImageViewResource(R.id.icon, R.drawable.ic_action_download);

		return new NotificationCompat.Builder(this)
				.setContentIntent(contentIntent)
				.setSmallIcon(R.drawable.ic_action_download)
				.setWhen(System.currentTimeMillis())
				.setAutoCancel(false)
				.setOngoing(true)
				.setContent(contentView)
				.getNotification();
	}

	static class DownloadTask extends AsyncTask<Void, Long, String> {
		private final static String logTag = "DownloadTask";
		private final static int UPDATE_INTERVAL = 1 * (int) Math.pow(1024, 2);

		private final Download mDownload;
		private final Listener mListener;

		private interface Listener {
			void onStart(Download d);
			void onProgressUpdate(Download d);
			void onCompletion(Download d);
		}

		private DownloadTask(Download download, Listener listener) {
			mDownload = download;
			mListener = listener;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			mDownload.setStarted();
			mListener.onStart(mDownload);
		}

		@Override
		protected void onProgressUpdate(Long... progress) {
			super.onProgressUpdate(progress);

			Log.v(logTag, Long.toString(progress[0]));

			mDownload.progress = progress[0];
			mListener.onProgressUpdate(mDownload);
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				final HttpClient httpClient = new DefaultHttpClient();
				final HttpGet httpGet = new HttpGet(mDownload.url);

				UsernamePasswordCredentials creds = new UsernamePasswordCredentials(mDownload.username,
						mDownload.password);
				httpGet.addHeader(new BasicScheme().authenticate(creds, httpGet));

				final HttpResponse response = httpClient.execute(httpGet);
				final HttpEntity entity = response.getEntity();

				if (entity != null) {
					String savePath = mDownload.savePath;

					InputStream input = new BufferedInputStream(entity.getContent());
					File outDir = new File(savePath.substring(0, savePath.lastIndexOf("/")));
					outDir.mkdirs();
					OutputStream output = new FileOutputStream(savePath);

					final byte data[] = new byte[1024];
					long total = 0;
					int count;

					while ((count = input.read(data)) != -1) {
						total += count;

						publishProgress(total);

						output.write(data, 0, count);
					}
					output.flush();
					output.close();
					input.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(String s) {
			super.onPostExecute(s);

			mDownload.setCompleted();

			if (mListener != null)
				mListener.onCompletion(mDownload);
		}
	}
}
