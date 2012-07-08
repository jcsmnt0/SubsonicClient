package com.casamento.subsonicclient;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

class DownloadArrayAdapter extends ArrayAdapter<Download> {
	private final String logTag = "DownloadArrayAdapter";
	private final Context context;
	private final List<Download> downloads;
	private final LayoutInflater inflater;

	private static final class ViewHolder {
		TextView title, path;
		ProgressBar progress;
	}

	protected DownloadArrayAdapter(Context context, List<Download> downloads) {
		super(context, R.layout.download_row_layout, downloads);
		this.context = context;
		this.downloads = downloads;
		this.inflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Download download = this.downloads.get(position);

		ViewHolder holder;

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.download_row_layout, parent, false);
			holder = new ViewHolder();
			holder.title = (TextView)convertView.findViewById(R.id.listrow_download_title);
			holder.path = (TextView)convertView.findViewById(R.id.listrow_download_path);
			holder.progress = (ProgressBar)convertView.findViewById(R.id.listrow_download_progress);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder)convertView.getTag();
		}

		holder.title.setText(download.title);
		holder.path.setText(download.path);
		holder.progress.setProgress((int)download.progress);

		return convertView;
	}
}
