package com.casamento.subsonicclient;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FilesystemEntryArrayAdapter extends ArrayAdapter<FilesystemEntry> {
	private final String logTag = "FilesystemEntryArrayAdapter";
	private final Context context;
	private final List<FilesystemEntry> entries;
	private LayoutInflater inflater;
	
	private static final class ViewHolder {
		TextView label;
	}
	
	public FilesystemEntryArrayAdapter(Context context, List<FilesystemEntry> entries) {
		super(context, R.layout.music_folder_row_layout, entries);
		this.context = context;
		this.entries = entries;
		this.inflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		FilesystemEntry entry = this.entries.get(position);
		
		ViewHolder holder;
		
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.music_folder_row_layout, parent, false);
			holder = new ViewHolder();
			holder.label = (TextView)convertView.findViewById(R.id.label);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder)convertView.getTag();
		}

		holder.label.setText(entry.name);
		
		Drawable icon = null;
		if (entry.isFolder) {
			icon = this.context.getResources().getDrawable(R.drawable.ic_action_folder_open);
		} else {
			MediaFile entryFile = (MediaFile)entry;
			if (entryFile.isVideo)
				icon = this.context.getResources().getDrawable(R.drawable.ic_action_tv);
			else if (entryFile.type != null) {
				if (entryFile.type.equalsIgnoreCase("music"))
					icon = this.context.getResources().getDrawable(R.drawable.ic_action_music_1);
			}
		}
		holder.label.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
		
		return convertView;
	}
}
