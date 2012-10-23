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
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

public class DownloadService extends Service {
    private static final Collection<Listener> mListeners = new ArrayList<Listener>();
    private static final LinkedList<Download> mPendingDownloads = new LinkedList<Download>();

    private static DownloadTask mCurrentTask;
    private Notification mNotification;

    interface Listener {
        void onAddition(Download download);
        void onStart(Download download);
        void onProgressUpdate(Download download);
        void onCompletion(Download download);
        void onCancellation(Download download);
    }

    static class Adapter implements Listener {
        @Override public void onAddition(final Download download)       { /* do nothing */ }
        @Override public void onStart(final Download download)          { /* do nothing */ }
        @Override public void onProgressUpdate(final Download download) { /* do nothing */ }
        @Override public void onCompletion(final Download download)     { /* do nothing */ }
        @Override public void onCancellation(final Download download)   { /* do nothing */ }
    }

    class ServiceBinder extends Binder {
        DownloadService getService() { return DownloadService.this; }
    }

    private final IBinder mBinder = new ServiceBinder();

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        return START_STICKY;
    }

    void registerListener(final Listener listener) {
        mListeners.add(listener);
    }

    void unregisterListener(final Listener listener) {
        mListeners.remove(listener);
    }

    void queue(final String name, final String url, final String savePath, final String username,
            final String password) {
        final Download d = new Download(name, url, savePath, username, password);

        if (!mPendingDownloads.contains(d)) {
            for (final Listener l : mListeners)
                l.onAddition(d);

            mPendingDownloads.addLast(d);

            if (!isDownloading())
                startDownload(d);
        }
    }

    // TODO: fix this
    void cancel(final Download d) {
        d.setCancelled();

        for (final Listener l : mListeners)
            l.onCancellation(d);

        if (mCurrentTask.getDownload().equals(d)) {
            mCurrentTask.cancel(true);
            startNextOrSleep();
        } else
            mPendingDownloads.remove(d);
    }

    boolean isDownloading() {
        return mCurrentTask != null;
    }

    Collection<Download> getPendingDownloads() {
        return mPendingDownloads;
    }

    private void startNextOrSleep() {
        mPendingDownloads.removeFirst();

        if (mPendingDownloads.isEmpty()) {
            mCurrentTask = null;
            cancelNotification();
        } else
            startDownload(mPendingDownloads.getFirst());
    }

    private final Listener mDownloadTaskListener = new Adapter() {
	    private static final int UPDATE_RATE = 1000;
        private long lastUpdateTime = 0;

        @Override
        public void onStart(final Download d) {
            updateNotification(d.getName(), d.getSavePath());
        }

        @Override
        public void onProgressUpdate(final Download d) {
            // SystemUI chokes if a notification is updated too frequently
            final long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime > UPDATE_RATE) {
                updateNotification(d.getProgressString());
                lastUpdateTime = currentTime;
            }
        }

        @Override
        public void onCompletion(final Download d) {
            startNextOrSleep();
        }

        @Override
        public void onCancellation(final Download d) {
            startNextOrSleep();
        }
    };

    private void startDownload(final Download d) {
        mCurrentTask = (DownloadTask) new DownloadTask(d).execute();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mNotification = createNotification();
        mListeners.add(mDownloadTaskListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mCurrentTask != null)
            mCurrentTask.cancel(true);

        mPendingDownloads.clear();

        cancelNotification();
    }

    private void updateNotification(final CharSequence progress) {
        mNotification.contentView.setTextViewText(R.id.progress, progress);

        startForeground(R.string.download_service, mNotification);
    }

    private void updateNotification(final CharSequence name, final CharSequence path) {
        mNotification.contentView.setTextViewText(R.id.name, name);
        mNotification.contentView.setTextViewText(R.id.path, path);

        // clear progress text view
        updateNotification("");
    }

    private void cancelNotification() {
        stopForeground(true);
    }

    private Notification createNotification() {
        final Intent notificationIntent = new Intent(this, MainActivity.class);
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

    // A thread that performs the actual downloading
    private static class DownloadTask extends AsyncTask<Void, Long, Download> {
        private static final int BUFFER_SIZE = 1024;

        private final Download mDownload;

        private DownloadTask(final Download download) {
            mDownload = download;
        }

        Download getDownload() {
            return mDownload;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mDownload.setStarted();

            for (final Listener dl : mListeners)
                dl.onStart(mDownload);
        }

        @Override
        protected void onProgressUpdate(final Long... progress) {
            super.onProgressUpdate(progress);

            mDownload.setProgress(progress[0]);

            for (final Listener dl : mListeners)
                dl.onProgressUpdate(mDownload);
        }

        @Override
        protected Download doInBackground(final Void... params) {
            try {
                // Get an InputStream for the file on the server
                final InputStream input = Util.getStream(mDownload.getUrl(), mDownload.getUsername(),
                        mDownload.getPassword());

                // Create the file on the local disk
                final String savePath = mDownload.getSavePath();
                final File outDir = new File(savePath.substring(0, savePath.lastIndexOf("/")));

                outDir.mkdirs();

                final OutputStream output = new FileOutputStream(savePath);

                final byte[] data = new byte[BUFFER_SIZE];
                long total = 0;
                int count;

                // Read and save data until cancellation or end of file
                while (!isCancelled() && (count = input.read(data)) != -1) {
                    total += count;
                    publishProgress(total);
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
            } catch (final Exception e) {
                // TODO: exception handling
                Log.e(getClass().getSimpleName(), "Error", e);
            }

            return mDownload;
        }

        @Override
        protected void onPostExecute(final Download d) {
            super.onPostExecute(d);

            d.setCompleted();

            for (final Listener dl : mListeners)
                dl.onCompletion(d);
        }

        @Override
        protected void onCancelled(final Download d) {
            new File(d.getSavePath()).delete();
        }
    }

    static class Download {
        private boolean mStarted = false, mCompleted = false, mCancelled = false;
        private long mProgress;

        private final String mName, mUrl, mSavePath, mUsername, mPassword;

        String getName()     { return mName;     }
        String getUrl()      { return mUrl;      }
        String getSavePath() { return mSavePath; }
        String getUsername() { return mUsername; }
        String getPassword() { return mPassword; }

        @Override
        public boolean equals(final Object o) {
            return (o instanceof Download && ((Download) o).getUrl().equals(mUrl));
        }

        private void setStarted()   { mStarted = true;   }
        private void setCompleted() { mCompleted = true; }
        private void setCancelled() { mCancelled = true; }

        private void setProgress(final long progress) {
            mProgress = progress;
        }

        long getProgress() {
            return mProgress;
        }

        boolean isStarted()   { return mStarted;   }
        boolean isCompleted() { return mCompleted; }
        boolean isCancelled() { return mCancelled; }

        private Download(final String name, final String url, final String savePath, final String username,
                final String password) {
            mName = name;
            mUrl = url;
            mSavePath = savePath;
            mUsername = username;
            mPassword = password;
        }

        private final long KB = 1L << 10, MB = KB << 10, GB = MB << 10, TB = GB << 10;

        String getProgressString() {
            if (mProgress >= TB)
                    return String.format("%.2fTB", (double) mProgress / TB);
                else if (mProgress >= GB)
                    return String.format("%.2fGB", (double) mProgress / GB);
                else if (mProgress >= MB)
                    return String.format("%.2fMB", (double) mProgress / MB);
                else if (mProgress >= KB)
                    return String.format("%.2fKB", (double) mProgress / KB);
                else
                    return String.format("%dB", mProgress);
        }
    }
}