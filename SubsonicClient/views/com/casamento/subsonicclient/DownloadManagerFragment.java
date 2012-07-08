package com.casamento.subsonicclient;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.SherlockListFragment;

import java.util.List;

public class DownloadManagerFragment extends SherlockListFragment {
	protected final static String logTag = "DownloadManagerFragment";
	protected List<Download> downloads;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		downloads = ((SubsonicClientActivity)this.getSherlockActivity()).downloads;

		this.setListAdapter(new DownloadArrayAdapter(this.getSherlockActivity(), this.downloads));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.download_manager_fragment, viewGroup, false);
	}
}