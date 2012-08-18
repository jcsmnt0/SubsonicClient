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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;

import java.util.List;
import java.util.Stack;

import static android.widget.AdapterView.OnItemClickListener;
import static com.casamento.subsonicclient.SubsonicCaller.*;

// TODO: use xml for layout
public class ServerBrowserFragment extends SherlockListFragment {
	private static final String logTag = "ServerBrowserFragment";
	private ListView mListView;
	private Cursor mCursor;

	private ActivityCallbackInterface mActivity;

	// containing Activity must implement this interface; enforced in onAttach
	interface ActivityCallbackInterface {
		void initiateDownload(DownloadTask downloadTask);
		void showDialogFragment(DialogFragment dialogFragment);
		void connectToServer() throws SubsonicClientActivity.ServerNotSetUpException;
		RetrieveCursorTask getRetrieveCursorTask(OnCursorRetrievedListener callbackListener) throws SubsonicClientActivity.ServerNotSetUpException;
		RetrieveCursorTask getRetrieveCursorTask(FilesystemEntry.Folder folder, OnCursorRetrievedListener callbackListener) throws SubsonicClientActivity.ServerNotSetUpException;
		void showProgressSpinner();
		void hideProgressSpinner();
		void pushServerBrowserFragment(FilesystemEntry.Folder folder);
	}

	ServerBrowserFragment(Cursor cursor) {
		mCursor = cursor;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// ensure activity implements the interface this Fragment needs
		try {
			mActivity = (ActivityCallbackInterface)activity;
			setListAdapter(new FilesystemEntryCursorAdapter(getSherlockActivity(), mCursor, false));
		} catch (ClassCastException e) {
			throw new ClassCastException(mActivity.toString() + " must implement " +
					"ServerBrowserFragment.ActivityCallbackInterface");
		}
	}

	// TODO: free references for GC to claim
	@Override
	public void onDetach() {
		super.onDetach();
	}

	// TODO: save list state
	@Override
	public void onPause() {
		super.onPause();
		Log.d(logTag, "PAUSE");
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(logTag, "RESUME");
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);

		mListView = getListView();
		mListView.setOnItemClickListener(filesystemEntryClickListener);
		registerForContextMenu(mListView);
		mListView.setFastScrollEnabled(true);
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
		FilesystemEntry entry = (FilesystemEntry)getListAdapter().getItem(info.position);

		menu.setHeaderTitle(entry.name);

		if (entry.isFolder) {
			getSherlockActivity().getMenuInflater().inflate(R.menu.contextmenu_folder, menu);
		} else {
			FilesystemEntry.MediaFile mediaFile = (FilesystemEntry.MediaFile)entry;
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
		FilesystemEntry entry = (FilesystemEntry)mListView.getAdapter().getItem(info.position);

		if (entry.isFolder) {
			FilesystemEntry.Folder folder = (FilesystemEntry.Folder)entry;

			switch (item.getItemId()) {
				case R.id.folderContextMenu_downloadOriginal:
					//downloadFolder(folder, false);
					break;

				case R.id.folderContextMenu_downloadTranscoded:
					//downloadFolder(folder, true);
					break;
			}
		} else {
			FilesystemEntry.MediaFile mediaFile = (FilesystemEntry.MediaFile)entry;

			switch(item.getItemId()) {
				case R.id.fileContextMenu_downloadOriginalFile:
					try {
						mActivity.initiateDownload(SubsonicCaller.getOriginalDownloadTask(mediaFile));
					} catch (Exception e) {
						// TODO: better exception handling
						mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(),
								getString(R.string.error), e.getLocalizedMessage()));
						Log.e(logTag, e.toString());
					}
					break;

				case R.id.fileContextMenu_downloadTranscodedFile:
					try {
						mActivity.initiateDownload(SubsonicCaller.getTranscodedDownloadTask(mediaFile, 0,
								null, 0, null, false));
					} catch (Exception e) {
						mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(),
								getString(R.string.error), e.getLocalizedMessage()));
						Log.e(logTag, e.toString());
					}
					break;

				case R.id.fileContextMenu_streamFile:
					try {
						SubsonicCaller.stream(mediaFile, 0, null, 0, null, false);
					} catch (Exception e) {
						mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(),
								getString(R.string.error), e.getLocalizedMessage()));
						Log.e(logTag, e.toString());
					}
					break;
			}
		}

		return true;
	}

	@Override
	public void onPrepareOptionsMenu(final Menu menu) {
		// TODO: if this check isn't here the menu items get added every time the menu is shown, find out why
		if (menu.findItem(R.id.option_selectMediaFolder) == null)
			getSherlockActivity().getSupportMenuInflater().inflate(R.menu.optionsmenu_server_browser, menu);
	}

//	@Override
//	public boolean onOptionsItemSelected(final MenuItem item) {
//		switch (item.getItemId()) {
//			case android.R.id.home:
//				if (mCurrentFolder != null) {
//					try {
////						if (mCurrentFolder.parent != null) {
////							setCurrentFolder(mCurrentFolder.parent);
////						} else {
////							setCurrentMediaFolder(mCurrentMediaFolder);
////						}
//						mListView.setSelectionFromTop(mSavedScrollPositions.pop(), 0);
//					} catch (SubsonicClientActivity.ServerNotSetUpException e) {
//						AlertDialogFragment alert = new AlertDialogFragment.Builder(getSherlockActivity())
//							.setTitle(R.string.error)
//							.setMessage("Please set up your server in the settings.")
//							.setNeutralButton(R.string.ok)
//							.create();
//						mActivity.showDialogFragment(alert);
//					}
//				}
//				return true;
//
//			case R.id.option_selectMediaFolder:
////				showSelectMediaFolderDialog();
//				return true;
//
//			default:
//				return super.onOptionsItemSelected(item);
//
//		}
//	}

//	private void downloadFolder(final Folder folder, final boolean transcoded) {
//		// get folder contents list from the server if needed
//		if (folder.contents == null) {
//			try {
//				SubsonicCaller.listFolderContents(folder, new SubsonicCaller.OnFolderContentsResponseListener() {
//					@Override
//					public void onFolderContentsResponse(List<FilesystemEntry> contents) {
//						// try download again
//						folder.contents = contents;
//						downloadFolder(folder, transcoded);
//					}
//
//					@Override
//					public void onException(Exception e) {
//						mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(),
//								R.string.error, e.getLocalizedMessage()));
//						Log.e(logTag, e.toString());
//					}
//				});
//			} catch (Exception e) {
//				mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(),
//						R.string.error, e.getLocalizedMessage()));
//				Log.e(logTag, e.toString());
//			}
//			return;
//		}
//
//		for (FilesystemEntry entry : folder.contents) {
//			if (entry.isFolder)
//				downloadFolder((Folder)entry, transcoded);
//			else {
//				if (transcoded) {
//					try {
//						mActivity.initiateDownload(SubsonicCaller.getTranscodedDownloadTask((MediaFile)entry,
//								0, null, 0, null, false));
//					} catch (Exception e) {
//						mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(), getString(R.string.error), e.getLocalizedMessage()));
//						Log.e(logTag, e.toString());
//					}
//				} else {
//					try {
//						mActivity.initiateDownload(SubsonicCaller.getOriginalDownloadTask((MediaFile)entry));
//					} catch (Exception e) {
//						mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(), getString(R.string.error), e.getLocalizedMessage()));
//						Log.e(logTag, e.toString());
//					}
//				}
//			}
//		}
//	}

//	private void showSelectMediaFolderDialog() {
//		if (mMediaFolders == null || mMediaFolders.size() == 0) {
//			mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(), getString(R.string.error), "There's nothing here!"));
//			return;
//		}
//
//		// populate list of media folders
//		final List<String> entryList = new ArrayList<String>();
////		entryList.add("Everything");
//
//		for (MediaFolder mediaFolder : mMediaFolders) {
//			entryList.add(mediaFolder.name);
//		}
//		final String[] entryArray = new String[entryList.size()];
//		entryList.toArray(entryArray);
//
//		final int currentEntry = mMediaFolders.indexOf(mCurrentMediaFolder);
//
//		final RadioDialogFragment dialog = new RadioDialogFragment("Select media folder:", entryArray, currentEntry, new RadioDialogFragment.OnSelectionListener() {
//			@Override
//			public void onSelection(final int selection) {
////				if (selection == 0)
////					setCurrentMediaFolder(null);
////				else
////					setCurrentMediaFolder(mMediaFolders.get(selection - 1));
//				try {
//					setCurrentMediaFolder(mMediaFolders.get(selection));
//				} catch (SubsonicClientActivity.ServerNotSetUpException e) {
//					AlertDialogFragment alert = new AlertDialogFragment.Builder(getSherlockActivity())
//						.setTitle(R.string.error)
//						.setMessage("Please set up your server in the settings.")
//						.setNeutralButton(R.string.ok)
//						.create();
//					mActivity.showDialogFragment(alert);
//				}
//			}
//		});
//
//		mActivity.showDialogFragment(dialog);
//	}

//	// TODO: push new fragment instead (so OS handles back button)
//	private void setCurrentMediaFolder(final MediaFolder mediaFolder) {
//		mCurrentMediaFolder = mediaFolder;
//		mCurrentFolder = null;
//
//		getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(false);
//
//		if (mCurrentMediaFolder == null || mCurrentMediaFolder.contents == null) {
//			final ServerBrowserFragment self = this;
//			try {
//				final LoadingDialogFragment dialog = new LoadingDialogFragment("Loading folder contents...");
//				mActivity.showDialogFragment(dialog);
//				SubsonicCaller.listMediaFolderContents(mCurrentMediaFolder, null, new SubsonicCaller.OnMediaFolderContentsResponseListener() {
//					@Override
//					public void onMediaFolderContentsResponse(List<FilesystemEntry> contents) {
////						if (self.currentMediaFolder == null)
////							self.currentMediaFolder = new MediaFolder("Everything");
////						self.currentMediaFolder.contents = contents;
////						self.showFolderContents(self.currentMediaFolder);
////						self.getSherlockActivity().setTitle(self.currentMediaFolder.name);
////						dialog.dismiss();
//
//					}
//
//					@Override
//					public void onException(Exception e) {
//						mActivity.showDialogFragment(new AlertDialogFragment(self.getSherlockActivity(), self.getString(R.string.error), e.getLocalizedMessage()));
//						dialog.dismiss();
//					}
//				});
//			} catch (Exception e) {
//				mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(), getString(R.string.error), e.getLocalizedMessage()));
//			}
//		} else {
//			showFolderContents(mCurrentMediaFolder);
//			getSherlockActivity().setTitle(mediaFolder.name);
//		}
//	}

	private OnItemClickListener filesystemEntryClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
			FilesystemEntry entry = SubsonicCaller.getFilesystemEntry((Integer) view.getTag());
			if (entry.isFolder)
				try {
					setCurrentFolder((FilesystemEntry.Folder)entry);
				} catch (SubsonicClientActivity.ServerNotSetUpException e) {
					AlertDialogFragment alert = new AlertDialogFragment.Builder(getSherlockActivity())
						.setTitle(R.string.error)
						.setMessage("Please set up your server in the settings.")
						.setNeutralButton(R.string.ok)
						.create();
					mActivity.showDialogFragment(alert);
				}
		}
	};
//
//	private void setCurrentMediaFolder(final MediaFolder mediaFolder) throws SubsonicClientActivity.ServerNotSetUpException {
//		final LoadingDialogFragment loading = new LoadingDialogFragment("Loading media folder contents...");
//		loading.setCancelable(false);
//		mActivity.showDialogFragment(loading);
//
//		RetrieveCursorTask task = mActivity.getRetrieveCursorTask(mediaFolder, new OnCursorRetrievedListener() {
//			@Override
//			public void onCursorRetrieved(Cursor cursor) {
//				setListAdapter(new FilesystemEntryCursorAdapter(getSherlockActivity(), cursor, false));
//				loading.dismiss();
//			}
//
//			@Override
//			public void onException(Exception e) {
//				loading.dismiss();
//				mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(), R.string.error,
//						e.getLocalizedMessage()));
//			}
//		});
//		task.execute();
//	}

	private void setCurrentFolder(final FilesystemEntry.Folder folder) throws SubsonicClientActivity.ServerNotSetUpException {
		mActivity.pushServerBrowserFragment(folder);
//		mActivity.showProgressSpinner();
//		RetrieveCursorTask task = mActivity.getRetrieveCursorTask(folder, new OnCursorRetrievedListener() {
//			@Override
//			public void onCursorRetrieved(Cursor cursor) {
//				setListAdapter(new FilesystemEntryCursorAdapter(getSherlockActivity(), cursor, false));
//				mActivity.hideProgressSpinner();
//			}
//
//			@Override
//			public void onException(Exception e) {
//				mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(), R.string.error,
//						e.getLocalizedMessage()));
//				mActivity.hideProgressSpinner();
//			}
//		});
//		task.execute();
	}

	private static class FilesystemEntryCursorAdapter extends CursorAdapter {
		private final LayoutInflater inflater;
		private final int idCol, nameCol;

		public FilesystemEntryCursorAdapter(Context context, Cursor c, boolean autoRequery) {
			super(context, c, autoRequery);
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
	}

	// TODO: push new fragment instead (so OS handles back button)
//	private void setCurrentFolder(final Folder folder) {
//		mCurrentFolder = folder;
//
//		if (mCurrentFolder.contents == null) {
//			final ServerBrowserFragment self = this;
//			try {
//				SubsonicCaller.listFolderContents(folder, new SubsonicCaller.OnFolderContentsResponseListener() {
//					@Override
//					public void onFolderContentsResponse(List<FilesystemEntry> contents) {
//						self.currentFolder.contents = contents;
//						self.showFolderContents(self.currentFolder);
//						self.getSherlockActivity().setTitle(self.currentFolder.name);
//						self.getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//					}
//
//					@Override
//					public void onException(Exception e) {
//						try {
//							throw e;
//						} catch (Exception e1) {
//							mActivity.showDialogFragment(new AlertDialogFragment(self.getSherlockActivity(), self.getString(R.string.error), e.getLocalizedMessage()));
//						}
//					}
//				});
//			} catch (Exception e) {
//				mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(), getString(R.string.error), e.getLocalizedMessage()));
//			}
//		} else {
//			showFolderContents(mCurrentFolder);
//			getSherlockActivity().setTitle(folder.name);
//			getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//		}
//	}



	// TODO: push new fragment instead (so OS handles back button)
//	private void showFolderContents(final Folder folder) {
//		setListAdapter(new FilesystemEntryArrayAdapter(getSherlockActivity(), folder.contents));
//
//		mListView.setOnItemClickListener(new OnItemClickListener() {
//			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//
//				FilesystemEntry clickedEntry = folder.contents.get(position);
//
//				if (clickedEntry.isFolder) {
//					mSavedScrollPositions.push(mListView.getFirstVisiblePosition());
//					Folder clickedFolder = (Folder)clickedEntry;
//					setCurrentFolder(clickedFolder);
//				} else {
//					try {
//						SubsonicCaller.stream((MediaFile)clickedEntry, 0, null, 0, null, true);
//					} catch (Exception e) {
//						mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(),
//								getString(R.string.error), e.getLocalizedMessage()));
//						Log.e(logTag, e.toString());
//					}
//				}
//			}
//		});
//
//		// TODO: maybe scroll (marquee) text on swipe/some other action via view.setSelected(true)
//	}
}
