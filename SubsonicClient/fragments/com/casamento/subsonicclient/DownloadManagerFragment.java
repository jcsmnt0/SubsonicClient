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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;

import java.util.List;

public class DownloadManagerFragment extends SherlockListFragment {
	private final static String logTag = "com.casamento.subsonicclient.DownloadManagerFragment";
	private ActivityCallbackInterface activity;

	protected interface ActivityCallbackInterface {
		List<DownloadTask> getDownloadTasks();
	}

	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	// TODO: restore state on attach
	@Override
	public void onAttach(Activity attachingActivity) {
		super.onAttach(attachingActivity);

		// ensure attaching activity implements this fragment's interface
		try {
			this.activity = (ActivityCallbackInterface)attachingActivity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ServerBrowserFragment.ActivityCallbackInterface");
		}

		this.setListAdapter(new DownloadTaskArrayAdapter(attachingActivity, this.activity.getDownloadTasks()));
	}

	// TODO: save state on detach
	@Override
	public void onDetach() {
		super.onDetach();
		this.setListAdapter(null);
	}

	// bug fix as per https://code.google.com/p/android/issues/detail?id=19917
	@Override
	public void onSaveInstanceState(final Bundle outState) {
		if (outState.isEmpty()) {
			outState.putBoolean("bug:fix", true);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup viewGroup, final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.download_manager_fragment, viewGroup, false);
	}

	private SubsonicClientActivity getSubsonicActivity() {
		return (SubsonicClientActivity)this.getSherlockActivity();
	}

	private static class DownloadTaskArrayAdapter extends ArrayAdapter<DownloadTask> {
		private final String logTag = "DownloadArrayAdapter";
		private final Context context;
		private final List<DownloadTask> downloadTasks;
		private final LayoutInflater inflater;

		private static final class ViewHolder {
			TextView name, path, progressView;
			ProgressBar progressBar;
		}

		protected DownloadTaskArrayAdapter(Context context, List<DownloadTask> downloadTasks) {
			super(context, R.layout.download_row_layout, downloadTasks);
			this.context = context;
			this.downloadTasks = downloadTasks;
			this.inflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			DownloadTask downloadTask = this.downloadTasks.get(position);

			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.download_row_layout, parent, false);
				holder = new ViewHolder();
				holder.name = (TextView)convertView.findViewById(R.id.listrow_download_name);
				holder.path = (TextView)convertView.findViewById(R.id.listrow_download_path);
				holder.progressView = (TextView)convertView.findViewById(R.id.listrow_download_progress);
				holder.progressBar = (ProgressBar)convertView.findViewById(R.id.listrow_download_progress_bar);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder)convertView.getTag();
			}

			holder.path.setText(downloadTask.savePath);

			return convertView;
		}
	}
}