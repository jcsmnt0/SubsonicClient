package com.casamento.subsonicclient;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class ServerBrowserFragment extends SherlockListFragment implements OnSharedPreferenceChangeListener {
	private final String logTag = "ServerBrowserFragment";
	private final ServerBrowserFragment self = this; // for referencing within anonymous classes
	private SherlockFragmentActivity activity;
	private SharedPreferences prefs;
	private List<MediaFolder> mediaFolders;
	private MediaFolder currentMediaFolder;
	private Folder currentFolder;
	private SubsonicCaller caller;
	private Stack<Integer> savedScrollPositions;
	private ListView listView;
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		this.clearList();
		this.connectToServer();
	}
	
    private void connectToServer() {
    	this.caller = new SubsonicCaller(prefs.getString("serverUrl", null), prefs.getString("username", null), prefs.getString("password", null), SubsonicClientActivity.apiVersion, SubsonicClientActivity.clientId, this.activity);
		this.caller.getMediaFolders(new OnMediaFoldersResponseListener() {
			@Override
			void onMediaFoldersResponse(List<MediaFolder> mediaFolders) {
				self.mediaFolders = mediaFolders;
				self.setCurrentMediaFolder(null);
			}
			
			@Override
			void onException(Exception e) {
				try {
					self.setEmptyText("Set up your server in the preferences");
					throw e;
				} catch (Exception e1) {
					Util.showSingleButtonAlertBox(self.activity, e1.getLocalizedMessage(), "OK");
				}
			}
		});
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
		
		this.activity = this.getSherlockActivity();
		this.setHasOptionsMenu(true);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this.activity);
		this.prefs.registerOnSharedPreferenceChangeListener(this);
		
		if (prefs.getString("serverUrl", null) == null || prefs.getString("username", null) == null || prefs.getString("password", null) == null) {
			Util.showSingleButtonAlertBox(this.activity, "The server is not fully set up.", "You're right");
		} else {
			connectToServer();
		}

		this.mediaFolders = new ArrayList<MediaFolder>();
		this.currentMediaFolder = new MediaFolder();
		this.currentMediaFolder.initContents();
		this.savedScrollPositions = new Stack<Integer>();
		
		this.setListAdapter(new FilesystemEntryArrayAdapter(this.activity, this.currentMediaFolder.contents));
		
		this.listView = this.getListView();
		this.registerForContextMenu(this.getListView());
	}
	
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);

    	AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
    	FilesystemEntry item = (FilesystemEntry)this.getListAdapter().getItem(info.position);
    	
    	menu.setHeaderTitle(item.name);
    	
    	if (item.isFolder) {
    		// show folder menu
    	} else {
    		MediaFile mediaFile = (MediaFile)item;
    		this.activity.getMenuInflater().inflate(R.menu.context_menu_file, menu);
    		menu.findItem(R.id.fileContextMenu_downloadOriginalFile).setTitle("Download Original File (" + mediaFile.suffix + ")");
    		menu.findItem(R.id.fileContextMenu_downloadTranscodedFile).setTitle("Download Transcoded File (" + (mediaFile.transcodedSuffix != null ? mediaFile.transcodedSuffix : mediaFile.suffix) + ")");
    	}
    }
	
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
    	FilesystemEntry entry = (FilesystemEntry)this.listView.getAdapter().getItem(info.position);
    	
    	if (entry.isFolder) {
    		// handle folder menu
    	} else {
    		MediaFile mediaFile = (MediaFile)entry;
    		
    		switch(item.getItemId()) {
    			case R.id.fileContextMenu_downloadOriginalFile:
    				try {
						caller.downloadOriginal(mediaFile);
					} catch (MalformedURLException e) {
						Util.showSingleButtonAlertBox(this.activity, "URL incorrect or something", "Forgive me");
						e.printStackTrace();
					} catch (UnsupportedEncodingException e) {
						Log.wtf(logTag, e);
					}
    				break;
    			
    			case R.id.fileContextMenu_downloadTranscodedFile:
    				try {
    					caller.downloadTranscoded(mediaFile, 0, null, 0, null, false);
    				} catch (MalformedURLException e) {
						Util.showSingleButtonAlertBox(this.activity, "URL incorrect or something", "Forgive me");
						e.printStackTrace();
					} catch (UnsupportedEncodingException e) {
						Log.wtf(logTag, e);
					}
    				break;
    			
    			case R.id.fileContextMenu_streamFile:
					try {
						caller.stream(mediaFile, 0, null, 0, null, false);
					} catch (MalformedURLException e) {
						Util.showSingleButtonAlertBox(this.activity, "URL incorrect or something", "Sure");
						e.printStackTrace();
					} catch (ActivityNotFoundException e) {
						Util.showSingleButtonAlertBox(this.activity, "You don't have an app to handle files of type " + mediaFile.contentType, "You're right");
						e.printStackTrace();
					} catch (UnsupportedEncodingException e) {
						Log.wtf(logTag, e);
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
    		this.activity.getSupportMenuInflater().inflate(R.menu.optionsmenu_server_browser, menu);
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
    
    private void showSelectMediaFolderDialog() {
    	if (this.mediaFolders == null || this.mediaFolders.size() == 0) {
    		Util.showSingleButtonAlertBox(this.activity, "There's nothing here!", "I see.");
    		return;
    	}
    	
    	AlertDialog.Builder dialog = new AlertDialog.Builder(this.activity);
    	dialog.setTitle("Select media folder:");
    	
    	// populate list of media folders
    	List<String> entryList = new ArrayList<String>();
    	entryList.add("Everything");
    	
    	final int mediaFoldersLength = this.mediaFolders.size();
    	for (int i = 0; i < mediaFoldersLength; i++) {
    		entryList.add(this.mediaFolders.get(i).name);
    	}
    	String[] entryArray = new String[entryList.size()];
    	entryList.toArray(entryArray);
    	
		int currentEntry = this.mediaFolders.indexOf(this.currentMediaFolder) + 1;
		
		final ServerBrowserFragment thisFragment = this;
    	
    	dialog.setSingleChoiceItems(entryArray, currentEntry, new OnClickListener() {
			public void onClick(DialogInterface dialog, int index) {
				
				if (index == 0)
					thisFragment.setCurrentMediaFolder(null);
				else
					thisFragment.setCurrentMediaFolder(thisFragment.mediaFolders.get(index-1));
				dialog.dismiss();
			}
    	});
    	
    	dialog.show();
    }
    
    private void setCurrentMediaFolder(final MediaFolder mediaFolder) {
    	this.currentMediaFolder = mediaFolder;
    	this.currentFolder = null;

		this.activity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    	
    	if (this.currentMediaFolder == null || this.currentMediaFolder.contents == null) {
    		this.caller.listMediaFolderContents(this.currentMediaFolder, null, new OnMediaFolderContentsResponseListener() {
				@Override
				void onMediaFolderContentsResponse(List<FilesystemEntry> contents) {
					if (self.currentMediaFolder == null)
						self.currentMediaFolder = new MediaFolder("Everything");
					self.currentMediaFolder.contents = contents;
					self.showFolderContents(self.currentMediaFolder);
					self.activity.setTitle(self.currentMediaFolder.name);
				}
				
				@Override
				void onException(Exception e) {
					try {
						throw e;
					} catch (Exception e1) {
						Util.showSingleButtonAlertBox(self.activity, e1.getLocalizedMessage(), "I forgive you");
					}
				}
    		});
    	} else {
    		this.showFolderContents(this.currentMediaFolder);
    		this.activity.setTitle(mediaFolder.name);
    	}
    }
    
    private void setCurrentFolder(final Folder folder) {
    	this.currentFolder = folder;
    	    	
    	if (this.currentFolder.contents == null) {
    		this.caller.listFolderContents(folder, new OnFolderContentsResponseListener() {
    			@Override
    			public void onFolderContentsResponse(java.util.List<FilesystemEntry> contents) {
    				self.currentFolder.contents = contents;
    				self.showFolderContents(self.currentFolder);
    				self.activity.setTitle(self.currentFolder.name);
    				self.activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    			}
    			
    			@Override
    			public void onException(Exception e) {
    				try {
						throw e;
					} catch (Exception e1) {
						Util.showSingleButtonAlertBox(self.activity, e1.getLocalizedMessage(), "I forgive you");
					}
    			}
    		});
    	} else {
    		this.showFolderContents(this.currentFolder);
    		this.activity.setTitle(folder.name);
    		this.activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    	}
    }
    
    private void showFolderContents(final Folder folder) {
    	this.setListAdapter(new FilesystemEntryArrayAdapter(this.activity, folder.contents));
    	final ServerBrowserFragment thisFragment = this;
    	
    	this.listView.setOnItemClickListener(new OnItemClickListener() {
    		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    			
    			FilesystemEntry clickedEntry = folder.contents.get(position);
    			
    			if (clickedEntry.isFolder) {
    				thisFragment.savedScrollPositions.push(thisFragment.listView.getFirstVisiblePosition());
    				Folder clickedFolder = (Folder)clickedEntry;
    				thisFragment.setCurrentFolder(clickedFolder);
    			} else {
    				try {
    					thisFragment.caller.stream((MediaFile)clickedEntry, 0, null, 0, null, true);
					} catch (MalformedURLException e) {
						Util.showSingleButtonAlertBox(thisFragment.activity, "The URL was bad in some way.", "Acceptance");
						e.printStackTrace();
					} catch (ActivityNotFoundException e) {
						Util.showSingleButtonAlertBox(thisFragment.activity, "You don't have an app to handle files of type " + ((MediaFile)clickedEntry).contentType, "You're right");
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    		}
    	});
    	
    	// TODO: maybe scroll text on swipe/some other action via view.setSelected(true)
    }

	public void onGetMediaFoldersResponse(final List<MediaFolder> mediaFolders) {
		this.mediaFolders = mediaFolders;
		this.mediaFolders.add(0, null);
	}

	public void onListFolderContentsResponse(final List<FilesystemEntry> contents) {
		this.currentFolder.contents = contents;
		this.showFolderContents(this.currentFolder);
		this.activity.setTitle(this.currentFolder.name);
	}
}
	