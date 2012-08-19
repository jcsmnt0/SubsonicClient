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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DownloadService extends Service implements DownloadTask.DownloadTaskListener {
	private final static String logTag = "SubsonicClient/DownloadService";
	private final List<Messenger> mClients = new ArrayList<Messenger>();
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	private Handler mHandler, mDownloadThreadHandler;
	private DownloadThread mDownloadThread;

	static enum Messages {
		REGISTER_CLIENT,
		UNREGISTER_CLIENT,
		INITIATE_DOWNLOAD,
		DOWNLOAD_PROGRESS_UPDATED,
		DOWNLOAD_STARTED,
		DOWNLOAD_FINISHED
	}

	private class DownloadThread extends Thread {
		@Override
		public void run() {
			try {
				Looper.prepare();
				mDownloadThreadHandler = new Handler();
				Looper.loop();
			} catch (Throwable t) {
				Log.e(logTag, "Error", t);
			}
		}

		public void requestStop() {
			mDownloadThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					try {
						Looper.myLooper().quit();
					} catch (Throwable t) {
						Log.e(logTag, "Error", t);
					}
				}
			});
		}

		public synchronized void initiateDownload(final DownloadTask downloadTask) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					downloadTask.setListener(DownloadService.this);
					downloadTask.execute((Void) null);
				}
			});

			Bundle data = new Bundle();
			data.putString("url", downloadTask.url);
			postMessage(Messages.DOWNLOAD_STARTED, data);
		}
	}

	@Override
	public void onProgressUpdate(long progress) {
		Bundle data = new Bundle();
		data.putLong("progress", progress);
		postMessage(Messages.DOWNLOAD_PROGRESS_UPDATED, data);
	}

	@Override
	public void onDownloadCompletion(DownloadTask task) {
		Bundle data = new Bundle();
		data.putString("url", task.url);

		postMessage(Messages.DOWNLOAD_FINISHED, data);
	}

	private void postMessage(Messages what, Bundle msgData) {
		Message msg = Message.obtain(null, what.ordinal());
		msg.setData(msgData);

		for (Messenger client : mClients) {
			try {
				client.send(msg);
			} catch (RemoteException e) {
				mClients.remove(client);
			}
		}
	}


	final Messenger mMessenger = new Messenger(new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (Messages.values()[msg.what]) {
				case REGISTER_CLIENT:
					mClients.add(msg.replyTo);
					break;

				case UNREGISTER_CLIENT:
					mClients.remove(msg.replyTo);
					break;

				case INITIATE_DOWNLOAD:
					Bundle msgData = msg.getData();
					mDownloadThread.initiateDownload(new DownloadTask(msgData.getString("url"),
							msgData.getString("savePath"), msgData.getString("username"),
							msgData.getString("password")));
					break;

				default:
					super.handleMessage(msg);
			}
		}
	});

	@Override
	public void onCreate() {
		super.onCreate();

		mDownloadThread = new DownloadThread();
		mDownloadThread.start();

		mHandler = new Handler();

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification();
	}

	@Override
	public void onDestroy() {
		mNotificationManager.cancel(R.string.download_service_notification_text);
		mDownloadThread.requestStop();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	private void showNotification() {
		Intent notificationIntent = new Intent(this, SubsonicClientActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		mNotification = new NotificationCompat.Builder(this)
				.setContentIntent(contentIntent)
				.setSmallIcon(R.drawable.ic_action_download)
				.setTicker(getString(R.string.download_service_ticker))
				.setWhen(System.currentTimeMillis())
				.setAutoCancel(false)
				.setOngoing(true)
				.setContentTitle(getString(R.string.download_service_content_title))
				.setContentText(getString(R.string.download_service_content_text))
				.getNotification();

		mNotificationManager.notify(R.string.download_service, mNotification);
	}
}
