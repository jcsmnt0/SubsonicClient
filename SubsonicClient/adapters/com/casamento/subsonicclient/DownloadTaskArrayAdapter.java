package com.casamento.subsonicclient;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

class DownloadTaskArrayAdapter extends ArrayAdapter<DownloadTask> {
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

		holder.name.setText(downloadTask.name);
		holder.path.setText(downloadTask.savePath);

		downloadTask.attachProgressView(holder.progressView);
//		downloadTask.attachProgressBar(progressBar);

		return convertView;
	}
}
