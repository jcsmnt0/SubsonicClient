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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListFragment;
import com.casamento.subsonicclient.DownloadService.Download;

import java.util.ArrayList;
import java.util.Collection;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class DownloadManagerFragment extends SherlockListFragment {
    private static final String logTag = "DownloadManagerFragment";
    private Adapter mAdapter;
    private ActivityCallback mActivity;
    private DownloadService mDownloadService;

    interface ActivityCallback {
        DownloadService getDownloadService();
        ActionBar getSupportActionBar();
    }

    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private final DownloadService.Listener mDownloadListener = new DownloadService.Adapter() {
        @Override
        public void onAddition(final Download d) {
            mAdapter.add(d);
        }

        @Override
        public void onStart(final Download d) {
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onProgressUpdate(final Download d) {
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onCompletion(final Download d) {
            mAdapter.remove(d);
        }

        @Override
        public void onCancellation(final Download d) {
            mAdapter.remove(d);
        }
    };

    @Override
    public void onPause() {
        super.onPause();

        mDownloadService.unregisterListener(mDownloadListener);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new Adapter(getActivity());
        setListAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        mDownloadService.registerListener(mDownloadListener);

        mAdapter.clear();
        mAdapter.addAll(mDownloadService.getPendingDownloads());

        final ActionBar ab = mActivity.getSupportActionBar();

        ab.setHomeButtonEnabled(false);
        ab.setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void onAttach(final Activity attachingActivity) {
        super.onAttach(attachingActivity);

        // ensure attaching activity implements this fragment's interface
        try {
            mActivity = (ActivityCallback) attachingActivity;
            mDownloadService = mActivity.getDownloadService();
        } catch (ClassCastException e) {
            throw new ClassCastException(mActivity.toString() + " must implement " +
                    "DownloadManagerFragment.ActivityCallback");
        }
    }

    // bug fix as per https://code.google.com/p/android/issues/detail?id=19917
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug:fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    private class Adapter extends ArrayAdapter<Download> {
        private final String logTag = "DownloadArrayAdapter";

        private class ViewHolder {
            private TextView name, path, progressView;
            private ProgressBar spinner;
        }

        private Adapter(final Context context) {
            //super(context, R.layout.download_row_layout);
            super(context, R.layout.download_row_layout, new ArrayList<Download>());
        }

        // addAll doesn't exist in Android pre-3.0
        @Override
        public void addAll(final Collection<? extends Download> collection) {
            for (final Download d : collection) {
                add(d);
            }
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final ViewHolder holder;
            final View v;

            if (convertView == null) {
                final LayoutInflater li = (LayoutInflater)
                        getSherlockActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = li.inflate(R.layout.download_row_layout, parent, false);

                holder = new ViewHolder();
                holder.name = (TextView) v.findViewById(R.id.listrow_download_name);
                holder.path = (TextView) v.findViewById(R.id.listrow_download_path);
                holder.progressView = (TextView) v.findViewById(R.id.listrow_download_progress);
                holder.spinner = (ProgressBar) v.findViewById(R.id.listrow_download_progress_bar);

                v.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
                v = convertView;
            }

            final Download d = getItem(position);

            // I'm coming to realize that you can never count on anything's existence in Android, even if you
            // explicitly created it like ten seconds ago
            // TODO: figure out what the root problem is here and fix it
            if (holder.name != null)
                holder.name.setText(d.name);

            if (holder.path != null)
                holder.path.setText(d.savePath);

            if (holder.progressView != null)
                holder.progressView.setText(d.getProgressString());

            if (holder.spinner != null)
                holder.spinner.setVisibility(d.isStarted() && !d.isCompleted() && !d.isCancelled() ?
                        VISIBLE : INVISIBLE);

            return v;
        }
    }
}