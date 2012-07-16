package com.casamento.subsonicclient;
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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: use xml for layout
public class ServerBrowserFragment extends SherlockListFragment implements OnSharedPreferenceChangeListener {
	private static final String logTag = "com.casamento.subsonicclient.ServerBrowserFragment";
	private SharedPreferences prefs;
	private List<MediaFolder> mediaFolders; // TODO: use array instead of List for performance (and because there's no reason not to)
	private MediaFolder currentMediaFolder;
	private Folder currentFolder;
	private Stack<Integer> savedScrollPositions;
	private ListView listView;

	private ActivityCallbackInterface activity;
	private SubsonicCaller subsonicCaller;

	// containing Activity must implement this interface; enforced in onAttach
	protected interface ActivityCallbackInterface {
		void initiateDownload(DownloadTask downloadTask);
		SubsonicCaller getSubsonicCaller();
		void showDialogFragment(DialogFragment dialogFragment);
	}

	// TODO: onDetach, disable anything that references activity
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// ensure activity implements the interface this Fragment needs
		try {
			this.activity = (ActivityCallbackInterface)activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ServerBrowserFragment.ActivityCallbackInterface");
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		this.clearList();
		this.connectToServer();
	}

	// TODO: save list state somehow
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

	private void connectToServer() {
    	this.subsonicCaller = activity.getSubsonicCaller();
	    final ServerBrowserFragment self = this;
	    try {
		    // TODO: figure out how to cancel a call on dialog cancel (some kind of reference to the AsyncTask)
		    final LoadingDialogFragment dialog = new LoadingDialogFragment("Loading media folder list...");
		    this.activity.showDialogFragment(dialog);
		    this.subsonicCaller.getMediaFolders(new SubsonicCaller.OnMediaFoldersResponseListener() {
			    @Override
			    public void onMediaFoldersResponse(List<MediaFolder> mediaFolders) {
				    self.mediaFolders = mediaFolders;
				    self.setCurrentMediaFolder(null);
				    dialog.dismiss();
			    }

			    @Override
			    public void onException(Exception e) {
				    AlertDialogFragment alert = new AlertDialogFragment.Builder(self.getActivity())
						    .setTitle(R.string.error) // TODO: null pointer exception ?!?
						    .setMessage(e.getLocalizedMessage())
						    .setNeutralButton(R.string.ok)
						    .create();
				    activity.showDialogFragment(alert);
				    dialog.dismiss();
			    }
		    });
	    } catch (IllegalStateException e) {
		    // this is when the activity tries to show a dialog, but another activity is in the forefront
		    // TODO: fix it somehow - can the activity detect whether it's visible?
		    // something about onPause/onResume is most likely the solution
	    } catch (Exception e) {
		    activity.showDialogFragment(new AlertDialogFragment(this.getActivity(), this.getString(R.string.error), e.getLocalizedMessage()));
	    }
    }
	
	private void clearList() {
		this.listView.setAdapter(null);
		this.mediaFolders = null;
		this.currentMediaFolder = null;
		this.currentFolder = null;
		this.setEmptyText("");
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		this.setHasOptionsMenu(true);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this.getSherlockActivity());
		this.prefs.registerOnSharedPreferenceChangeListener(this);

		try {
			connectToServer();
		} catch (IllegalStateException e) { // thrown if URL, username, or password are missing
			activity.showDialogFragment(new AlertDialogFragment(this.getSherlockActivity(), this.getString(R.string.error), e.getLocalizedMessage()));
		}

		this.mediaFolders = new ArrayList<MediaFolder>();
		this.currentMediaFolder = new MediaFolder();
		this.currentMediaFolder.initContents();
		this.savedScrollPositions = new Stack<Integer>();
		
		this.setListAdapter(new FilesystemEntryArrayAdapter(this.getSherlockActivity(), this.currentMediaFolder.contents));
		
		this.listView = this.getListView();
		this.registerForContextMenu(this.getListView());
		this.listView.setFastScrollEnabled(true);

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
    	FilesystemEntry entry = (FilesystemEntry)this.getListAdapter().getItem(info.position);
    	
    	menu.setHeaderTitle(entry.name);
    	
    	if (entry.isFolder) {
			this.getSherlockActivity().getMenuInflater().inflate(R.menu.contextmenu_folder, menu);
    	} else {
    		MediaFile mediaFile = (MediaFile)entry;
    		this.getSherlockActivity().getMenuInflater().inflate(R.menu.contextmenu_file, menu);
    		menu.findItem(R.id.fileContextMenu_downloadOriginalFile).setTitle("Download Original File (" + mediaFile.suffix + ")");
    		menu.findItem(R.id.fileContextMenu_downloadTranscodedFile).setTitle("Download Transcoded File (" + (mediaFile.transcodedSuffix != null ? mediaFile.transcodedSuffix : mediaFile.suffix) + ")");
    	}
    }
	
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
    	FilesystemEntry entry = (FilesystemEntry)this.listView.getAdapter().getItem(info.position);
    	
    	if (entry.isFolder) {
    	    Folder folder = (Folder)entry;

			switch (item.getItemId()) {
				case R.id.folderContextMenu_downloadOriginal:
					this.downloadFolder(folder, false);
					break;

				case R.id.folderContextMenu_downloadTranscoded:
					this.downloadFolder(folder, true);
					break;
			}
    	} else {
    		MediaFile mediaFile = (MediaFile)entry;
    		
    		switch(item.getItemId()) {
    			case R.id.fileContextMenu_downloadOriginalFile:
    				try {
					    this.activity.initiateDownload(this.subsonicCaller.getOriginalDownloadTask(mediaFile));
					} catch (Exception e) {
					    // TODO: better exception handling
					    activity.showDialogFragment(new AlertDialogFragment(this.getSherlockActivity(), this.getString(R.string.error), e.getLocalizedMessage()));
					    Log.e(logTag, e.toString());
				    }
				    break;
    			
    			case R.id.fileContextMenu_downloadTranscodedFile:
    				try {
					    this.activity.initiateDownload(this.subsonicCaller.getTranscodedDownloadTask(mediaFile, 0, null, 0, null, false));
    				} catch (Exception e) {
					    activity.showDialogFragment(new AlertDialogFragment(this.getSherlockActivity(), this.getString(R.string.error), e.getLocalizedMessage()));
				    }
				    break;
    			
    			case R.id.fileContextMenu_streamFile:
					try {
						this.subsonicCaller.stream(mediaFile, 0, null, 0, null, false);
					} catch (Exception e) {
						activity.showDialogFragment(new AlertDialogFragment(this.getSherlockActivity(), this.getString(R.string.error), e.getLocalizedMessage()));
					}
					break;
    		}
    	}
    	
		return true;
    }
    
    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
    	// TODO: if this check isn't here the menu items get added every time the menu is shown, find out why and figure out how to hide items programmatically
    	if (menu.findItem(R.id.option_selectMediaFolder) == null) {
    		this.getSherlockActivity().getSupportMenuInflater().inflate(R.menu.optionsmenu_server_browser, menu);
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
    	switch (item.getItemId()) {
    		case android.R.id.home:
    			if (this.currentFolder != null) {
    		    	if (this.currentFolder.parent != null) {
    		    		this.setCurrentFolder(this.currentFolder.parent);
    		    	} else {
    		    		this.setCurrentMediaFolder(this.currentMediaFolder);
    		    	}
    		    	this.listView.setSelectionFromTop(this.savedScrollPositions.pop(), 0);
    	    	}
    			return true;
    		
    		case R.id.option_selectMediaFolder:
    			showSelectMediaFolderDialog();
    			return true;
    			
    		default:
    			return super.onOptionsItemSelected(item);
    			
    	}
    }

	// TODO: out of curiosity, find out why it acts like it has a download queue even though I haven't implemented one
	private void downloadFolder(final Folder folder, final boolean transcoded) {
		// get folder contents list from the server if needed
		if (folder.contents == null) {
			final ServerBrowserFragment self = this;
			try {
				this.subsonicCaller.listFolderContents(folder, new SubsonicCaller.OnFolderContentsResponseListener() {
					@Override
					public void onFolderContentsResponse(List<FilesystemEntry> contents) {
						// try download again
						folder.contents = contents;
						self.downloadFolder(folder, transcoded);
					}

					@Override
					public void onException(Exception e) {
						self.activity.showDialogFragment(new AlertDialogFragment(self.getSherlockActivity(), R.string.error, e.getLocalizedMessage()));
					}
				});
			} catch (Exception e) {
				this.activity.showDialogFragment(new AlertDialogFragment(this.getSherlockActivity(), R.string.error, e.getLocalizedMessage()));
			}
			return;
		}

		for (FilesystemEntry entry : folder.contents) {
			if (entry.isFolder)
				downloadFolder((Folder)entry, transcoded);
			else {
				if (transcoded) {
					try {
						this.activity.initiateDownload(this.subsonicCaller.getTranscodedDownloadTask((MediaFile)entry, 0, null, 0, null, false));
					} catch (Exception e) {
						activity.showDialogFragment(new AlertDialogFragment(this.getSherlockActivity(), this.getString(R.string.error), e.getLocalizedMessage()));
					}
				} else {
					try {
						this.activity.initiateDownload(this.subsonicCaller.getOriginalDownloadTask((MediaFile)entry));
					} catch (Exception e) {
						activity.showDialogFragment(new AlertDialogFragment(this.getSherlockActivity(), this.getString(R.string.error), e.getLocalizedMessage()));
					}
				}
			}
		}


	}
    
    private void showSelectMediaFolderDialog() {
    	if (this.mediaFolders == null || this.mediaFolders.size() == 0) {
		    activity.showDialogFragment(new AlertDialogFragment(this.getSherlockActivity(), this.getString(R.string.error), "There's nothing here!"));
    		return;
    	}

    	// populate list of media folders
    	final List<String> entryList = new ArrayList<String>();
    	entryList.add("Everything");

	    for (MediaFolder mediaFolder : this.mediaFolders) {
		    entryList.add(mediaFolder.name);
	    }
    	final String[] entryArray = new String[entryList.size()];
    	entryList.toArray(entryArray);

		final int currentEntry = this.mediaFolders.indexOf(this.currentMediaFolder) + 1;
	    final ServerBrowserFragment self = this;

	    final RadioDialogFragment dialog = new RadioDialogFragment("Select media folder:", entryArray, currentEntry, new RadioDialogFragment.OnSelectionListener() {
		    @Override
		    public void onSelection(final int selection) {
				if (selection == 0)
					self.setCurrentMediaFolder(null);
			    else
					self.setCurrentMediaFolder(self.mediaFolders.get(selection - 1));
		    }
	    });

	    this.activity.showDialogFragment(dialog);
    }

	// TODO: push new fragment instead (so OS handles back button)
    private void setCurrentMediaFolder(final MediaFolder mediaFolder) {
    	this.currentMediaFolder = mediaFolder;
    	this.currentFolder = null;

		this.getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    	
    	if (this.currentMediaFolder == null || this.currentMediaFolder.contents == null) {
		    final ServerBrowserFragment self = this;
		    try {
			    final LoadingDialogFragment dialog = new LoadingDialogFragment("Loading folder contents...");
			    this.activity.showDialogFragment(dialog);
			    this.subsonicCaller.listMediaFolderContents(this.currentMediaFolder, null, new SubsonicCaller.OnMediaFolderContentsResponseListener() {
				    @Override
				    public void onMediaFolderContentsResponse(List<FilesystemEntry> contents) {
					    if (self.currentMediaFolder == null)
						    self.currentMediaFolder = new MediaFolder("Everything");
					    self.currentMediaFolder.contents = contents;
					    self.showFolderContents(self.currentMediaFolder);
					    self.getSherlockActivity().setTitle(self.currentMediaFolder.name);
					    dialog.dismiss();
				    }

				    @Override
				    public void onException(Exception e) {
					    activity.showDialogFragment(new AlertDialogFragment(self.getSherlockActivity(), self.getString(R.string.error), e.getLocalizedMessage()));
					    dialog.dismiss();
				    }
			    });
		    } catch (Exception e) {
			    activity.showDialogFragment(new AlertDialogFragment(this.getSherlockActivity(), this.getString(R.string.error), e.getLocalizedMessage()));
		    }
	    } else {
    		this.showFolderContents(this.currentMediaFolder);
    		this.getSherlockActivity().setTitle(mediaFolder.name);
    	}
    }

	// TODO: push new fragment instead (so OS handles back button)
    private void setCurrentFolder(final Folder folder) {
    	this.currentFolder = folder;
    	    	
    	if (this.currentFolder.contents == null) {
		    final ServerBrowserFragment self = this;
		    try {
			    this.subsonicCaller.listFolderContents(folder, new SubsonicCaller.OnFolderContentsResponseListener() {
				    @Override
				    public void onFolderContentsResponse(List<FilesystemEntry> contents) {
					    self.currentFolder.contents = contents;
					    self.showFolderContents(self.currentFolder);
					    self.getSherlockActivity().setTitle(self.currentFolder.name);
					    self.getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
				    }

				    @Override
				    public void onException(Exception e) {
					    try {
						    throw e;
					    } catch (Exception e1) {
						    activity.showDialogFragment(new AlertDialogFragment(self.getSherlockActivity(), self.getString(R.string.error), e.getLocalizedMessage()));
					    }
				    }
			    });
		    } catch (Exception e) {
			    activity.showDialogFragment(new AlertDialogFragment(this.getSherlockActivity(), this.getString(R.string.error), e.getLocalizedMessage()));
		    }
	    } else {
    		this.showFolderContents(this.currentFolder);
    		this.getSherlockActivity().setTitle(folder.name);
    		this.getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    	}
    }

	// TODO: push new fragment instead (so OS handles back button)
    private void showFolderContents(final Folder folder) {
    	this.setListAdapter(new FilesystemEntryArrayAdapter(this.getSherlockActivity(), folder.contents));

	    final ServerBrowserFragment self = this;
    	this.listView.setOnItemClickListener(new OnItemClickListener() {
    		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    			
    			FilesystemEntry clickedEntry = folder.contents.get(position);
    			
    			if (clickedEntry.isFolder) {
				    self.savedScrollPositions.push(self.listView.getFirstVisiblePosition());
    				Folder clickedFolder = (Folder)clickedEntry;
				    self.setCurrentFolder(clickedFolder);
    			} else {
    				try {
					    self.subsonicCaller.stream((MediaFile)clickedEntry, 0, null, 0, null, true);
					} catch (Exception e) {
					    activity.showDialogFragment(new AlertDialogFragment(self.getSherlockActivity(), self.getString(R.string.error), e.getLocalizedMessage()));
				    }
    			}
    		}
    	});
    	
    	// TODO: maybe scroll text on swipe/some other action via view.setSelected(true)
    }

	static class FilesystemEntryArrayAdapter extends ArrayAdapter<FilesystemEntry> {
		private final static String logTag = "FilesystemEntryArrayAdapter";
		private final Context context;
		private final List<FilesystemEntry> entries;
		private final LayoutInflater inflater;

		private static final class ViewHolder {
			TextView label;
		}

		protected FilesystemEntryArrayAdapter(Context context, List<FilesystemEntry> entries) {
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
}
	