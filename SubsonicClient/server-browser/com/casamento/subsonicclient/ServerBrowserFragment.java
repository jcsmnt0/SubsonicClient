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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import static android.widget.AdapterView.OnItemClickListener;
import static com.casamento.subsonicclient.FilesystemEntry.Folder;
import static com.casamento.subsonicclient.FilesystemEntry.MediaFile;
import static com.casamento.subsonicclient.SubsonicCaller.DatabaseHelper;

// TODO: use xml for layout
public class ServerBrowserFragment extends SherlockListFragment {
	private static final String logTag = "ServerBrowserFragment";
	private final Cursor mCursor;

	private ActivityCallback mActivity;

	// containing Activity must implement this interface; enforced in onAttach
	interface ActivityCallback {
		void pushServerBrowserFragment(Folder folder);
		void initiateDownload(MediaFile mediaFile, boolean transcoded);
	}

	public ServerBrowserFragment() {
		mCursor = null;
	}

	ServerBrowserFragment(Cursor cursor) {
		mCursor = cursor;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (mCursor != null)
			setListAdapter(new FilesystemEntryCursorAdapter(getSherlockActivity(), mCursor, false));

		// ensure activity implements the interface this Fragment needs
		try {
			mActivity = (ActivityCallback)activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(mActivity.toString() + " must implement " +
					"ServerBrowserFragment.ActivityCallback");
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);

		final ListView lv = getListView();
		lv.setOnItemClickListener(filesystemEntryClickListener);
		registerForContextMenu(lv);
		lv.setFastScrollEnabled(true);
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
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		FilesystemEntry entry = FilesystemEntry.getInstance((Cursor) getListAdapter().getItem(info.position));

		menu.setHeaderTitle(entry.name);

		if (entry.isFolder) {
			getSherlockActivity().getMenuInflater().inflate(R.menu.contextmenu_folder, menu);
		} else {
			MediaFile mediaFile = (MediaFile)entry;
			getSherlockActivity().getMenuInflater().inflate(R.menu.contextmenu_file, menu);
			menu.findItem(R.id.fileContextMenu_downloadOriginalFile).setTitle("Download Original File (" +
					mediaFile.suffix + ")");
			menu.findItem(R.id.fileContextMenu_downloadTranscodedFile).setTitle("Download Transcoded File (" +
					(mediaFile.transcodedSuffix != null ? mediaFile.transcodedSuffix : mediaFile.suffix) + ")");
		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		FilesystemEntry entry = FilesystemEntry.getInstance((Cursor) getListAdapter().getItem(info.position));

		if (entry.isFolder) {
			Folder folder = (Folder)entry;

			switch (item.getItemId()) {
				case R.id.folderContextMenu_downloadOriginal:
					//downloadFolder(folder, false);
					break;

				case R.id.folderContextMenu_downloadTranscoded:
					//downloadFolder(folder, true);
					break;
			}
		} else {
			MediaFile mediaFile = (MediaFile)entry;

			switch(item.getItemId()) {
				case R.id.fileContextMenu_downloadOriginalFile:
					try {
						mActivity.initiateDownload(mediaFile, false);
					} catch (Exception e) {
						// TODO: better exception handling
						Log.e(logTag, e.toString());
					}
					break;

				case R.id.fileContextMenu_downloadTranscodedFile:
					try {
						mActivity.initiateDownload(mediaFile, true);
					} catch (Exception e) {
						Log.e(logTag, e.toString());
					}
					break;

				case R.id.fileContextMenu_streamFile:
//					try {
//						SubsonicCaller.stream(mediaFile, 0, null, 0, null, false);
//					} catch (Exception e) {
//						mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(),
//								getString(R.string.error), e.getLocalizedMessage()));
//						Log.e(logTag, e.toString());
//					}
					break;
			}
		}

		return true;
	}

	@Override
	public void onPrepareOptionsMenu(final Menu menu) {
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				// TODO: make the home button go up one folder
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private OnItemClickListener filesystemEntryClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
			FilesystemEntry entry = SubsonicCaller.getFilesystemEntry((Integer) view.getTag());
			if (entry.isFolder)
				mActivity.pushServerBrowserFragment((Folder) entry);
		}
	};

	private static class FilesystemEntryCursorAdapter extends CursorAdapter implements SectionIndexer {
		private final LayoutInflater inflater;
		private final int idCol, nameCol;
		private final AlphabetIndexer indexer;

		public FilesystemEntryCursorAdapter(Context context, Cursor c, boolean autoRequery) {
			super(context, c, autoRequery);

			indexer = new AlphabetIndexer(c, c.getColumnIndex(DatabaseHelper.NAME.name), "ABCDEFGHIJKLMNOPQRSTUVWXYZ");

			inflater = LayoutInflater.from(context);
			idCol = c.getColumnIndex(DatabaseHelper.ID.name);
			nameCol = c.getColumnIndex(DatabaseHelper.NAME.name);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
			return inflater.inflate(R.layout.music_folder_row_layout, viewGroup, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView tv = (TextView)view.findViewById(R.id.label);
			tv.setText(cursor.getString(nameCol));
			// set the subsonic ID as the view's tag
			view.setTag(cursor.getInt(idCol));
		}

		@Override
		public int getPositionForSection(int section) {
			return indexer.getPositionForSection(section);
		}

		@Override
		public int getSectionForPosition(int section) {
			return indexer.getSectionForPosition(section);
		}

		@Override
		public Object[] getSections() {
			return indexer.getSections();
		}
	}
}
