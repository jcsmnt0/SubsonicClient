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
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.casamento.subsonicclient.SubsonicClientActivity.DownloadListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DownloadManagerFragment extends SherlockListFragment {
	private final static String logTag = "DownloadManagerFragment";
	private Adapter mAdapter;
	private ActivityCallback mActivity;

	interface ActivityCallback {
		void registerListener(DownloadListener dl);
		void unregisterListener(DownloadListener dl);
		Collection<Download> getDownloadList(DownloadListener dl);
	}

	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	private final DownloadListener mDownloadListener = new DownloadListener() {
		@Override
		public void onAddition(Download d) {
			mAdapter.add(d);
		}

		@Override
		public void onStart(Download d) {
		}

		@Override
		public void onProgressUpdate(Download d) {
			mAdapter.notifyDataSetChanged();
		}

		@Override
		public void onCompletion(Download d) {
		}
	};

	@Override
	public void onPause() {
		super.onPause();

		mActivity.unregisterListener(mDownloadListener);
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

		mActivity.registerListener(mDownloadListener);
		mAdapter.addAll(mActivity.getDownloadList(mDownloadListener));
	}

	@Override
	public void onAttach(Activity attachingActivity) {
		super.onAttach(attachingActivity);

		// ensure attaching activity implements this fragment's interface
		try {
			mActivity = (ActivityCallback) attachingActivity;
			mActivity.registerListener(mDownloadListener);
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

//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
//		return inflater.inflate(R.layout.download_manager_fragment, viewGroup, false);
//	}

//	private final ArrayAdapter<Download> adapter = new ArrayAdapter<Download>() {
//		final class ViewHolder {
//			final TextView nameView, pathView;
//
//			ViewHolder(TextView nameView, TextView pathView) {
//				this.nameView = nameView;
//				this.pathView = pathView;
//			}
//		}
//
//		@Override
//		public View getView(int position, View convertView, ViewGroup parent) {
//			ViewHolder holder;
//
//			if (convertView == null) {
//				LayoutInflater inflater = (LayoutInflater) getSherlockActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//				convertView = inflater.inflate(R.layout.download_row_layout, parent, false);
//
//				holder = new ViewHolder(
//						(TextView) convertView.findViewById(R.id.listrow_download_name),
//						(TextView) convertView.findViewById(R.id.listrow_download_path)
//				);
//
//				convertView.setTag(holder);
//			}
//		}
//	};

	private class Adapter extends ArrayAdapter<Download> {
		private final String logTag = "DownloadArrayAdapter";
		private final List<Download> mDownloads = new ArrayList<Download>();

		private final int TAG_HOLDER = 0;

		private class ViewHolder {
			private TextView name, path, progressView;
		}

		private Adapter(final Context context) {
			//super(context, R.layout.download_row_layout);
			super(context, R.layout.download_row_layout, new ArrayList<Download>());
		}

		// addAll doesn't exist in Android pre-3.0
		@Override
		public void addAll(Collection<? extends Download> collection) {
			for (final Download d : collection) {
				add(d);
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;

			if (convertView == null) {
				LayoutInflater li = (LayoutInflater)
						getSherlockActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = li.inflate(R.layout.download_row_layout, parent, false);

				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.listrow_download_name);
				holder.path = (TextView) convertView.findViewById(R.id.listrow_download_path);
				holder.progressView = (TextView) convertView.findViewById(R.id.listrow_download_progress);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			final Download d = getItem(position);

			// I'm coming to realize that you can never count on anything's existence in Android, even if you
			// explicitly created it like ten seconds ago

			if (holder.name != null)
				holder.name.setText(d.name);

			if (holder.path != null)
				holder.path.setText(d.savePath);

			if (holder.progressView != null)
				holder.progressView.setText(d.getProgressString());

			return convertView;
		}
	}
}