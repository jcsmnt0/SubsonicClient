package com.casamento.subsonicclient;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.SherlockListFragment;

import java.util.List;

public class DownloadManagerFragment extends SherlockListFragment {
	protected final static String logTag = "DownloadManagerFragment";
	protected List<DownloadTask> downloadTasks;

	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	// TODO: restore state on attach
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.downloadTasks = this.getSubsonicActivity().downloadTasks;
		this.setListAdapter(new DownloadTaskArrayAdapter(activity, this.downloadTasks));
	}

	// TODO: save state on detach
	@Override
	public void onDetach() {
		super.onDetach();
		this.setListAdapter(null);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup viewGroup, final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.download_manager_fragment, viewGroup, false);
	}

	private SubsonicClientActivity getSubsonicActivity() {
		return (SubsonicClientActivity)this.getSherlockActivity();
	}
}