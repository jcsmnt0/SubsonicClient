package com.casamento.subsonicclient;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ServerBrowserFragment extends SherlockListFragment implements OnSharedPreferenceChangeListener {
	private final String logTag = "ServerBrowserFragment";
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
    	this.caller = new SubsonicCaller(prefs.getString("serverUrl", null), prefs.getString("username", null), prefs.getString("password", null), SubsonicClientActivity.apiVersion, SubsonicClientActivity.clientId, this.getSherlockActivity());
	    final ServerBrowserFragment self = this;
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
					Util.showSingleButtonAlertBox(self.getSherlockActivity(), e1.getLocalizedMessage(), "OK");
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
		
		this.setHasOptionsMenu(true);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this.getSherlockActivity());
		this.prefs.registerOnSharedPreferenceChangeListener(this);
		
		if (prefs.getString("serverUrl", null) == null || prefs.getString("username", null) == null || prefs.getString("password", null) == null) {
			Util.showSingleButtonAlertBox(this.getSherlockActivity(), "The server is not fully set up.", "You're right");
		} else {
			connectToServer();
		}

		this.mediaFolders = new ArrayList<MediaFolder>();
		this.currentMediaFolder = new MediaFolder();
		this.currentMediaFolder.initContents();
		this.savedScrollPositions = new Stack<Integer>();
		
		this.setListAdapter(new FilesystemEntryArrayAdapter(this.getSherlockActivity(), this.currentMediaFolder.contents));
		
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
    		this.getSherlockActivity().getMenuInflater().inflate(R.menu.context_menu_file, menu);
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
					    this.getSubsonicActivity().addAndExecuteDownloadTask(this.caller.getOriginalDownloadTask(mediaFile));
					} catch (MalformedURLException e) {
						Util.showSingleButtonAlertBox(this.getSherlockActivity(), "URL incorrect or something", "Forgive me");
						e.printStackTrace();
					} catch (UnsupportedEncodingException e) {
						Log.wtf(logTag, e);
					} catch (URISyntaxException e) {
					    e.printStackTrace();
				    } catch (IOException e) {
					    e.printStackTrace();
				    }
				    break;
    			
    			case R.id.fileContextMenu_downloadTranscodedFile:
//    				try {
//					    DownloadTask downloadTask = this.caller.getTranscodedDownloadTask(mediaFile, 0, null, 0, null, false);
//					    downloadTask.execute();
//					    ((SubsonicClientActivity)this.getSherlockActivity()).downloadTasks.add(downloadTask);
//    				} catch (MalformedURLException e) {
//						Util.showSingleButtonAlertBox(this.getSherlockActivity(), "URL incorrect or something", "Forgive me");
//						e.printStackTrace();
//					} catch (UnsupportedEncodingException e) {
//						Log.wtf(logTag, e);
//					} catch (URISyntaxException e) {
//					    e.printStackTrace();
//				    } catch (IOException e) {
//					    e.printStackTrace();
//				    }
				    break;
    			
    			case R.id.fileContextMenu_streamFile:
					try {
						caller.stream(mediaFile, 0, null, 0, null, false);
					} catch (MalformedURLException e) {
						Util.showSingleButtonAlertBox(this.getSherlockActivity(), "URL incorrect or something", "Sure");
						e.printStackTrace();
					} catch (ActivityNotFoundException e) {
						Util.showSingleButtonAlertBox(this.getSherlockActivity(), "You don't have an app to handle files of type " + mediaFile.contentType, "You're right");
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
    
    private void showSelectMediaFolderDialog() {
    	if (this.mediaFolders == null || this.mediaFolders.size() == 0) {
    		Util.showSingleButtonAlertBox(this.getSherlockActivity(), "There's nothing here!", "I see.");
    		return;
    	}
    	
    	AlertDialog.Builder dialog = new AlertDialog.Builder(this.getSherlockActivity());
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

	// TODO: push new fragment instead (so OS handles back button)
    private void setCurrentMediaFolder(final MediaFolder mediaFolder) {
    	this.currentMediaFolder = mediaFolder;
    	this.currentFolder = null;

		this.getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    	
    	if (this.currentMediaFolder == null || this.currentMediaFolder.contents == null) {
		    final ServerBrowserFragment self = this;
    		this.caller.listMediaFolderContents(this.currentMediaFolder, null, new OnMediaFolderContentsResponseListener() {
				@Override
				void onMediaFolderContentsResponse(List<FilesystemEntry> contents) {
					if (self.currentMediaFolder == null)
						self.currentMediaFolder = new MediaFolder("Everything");
					self.currentMediaFolder.contents = contents;
					self.showFolderContents(self.currentMediaFolder);
					self.getSherlockActivity().setTitle(self.currentMediaFolder.name);
				}
				
				@Override
				void onException(Exception e) {
					try {
						throw e;
					} catch (Exception e1) {
						Util.showSingleButtonAlertBox(self.getSherlockActivity(), e1.getLocalizedMessage(), "I forgive you");
					}
				}
    		});
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
    		this.caller.listFolderContents(folder, new OnFolderContentsResponseListener() {
    			@Override
    			public void onFolderContentsResponse(java.util.List<FilesystemEntry> contents) {
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
						Util.showSingleButtonAlertBox(self.getSherlockActivity(), e1.getLocalizedMessage(), "I forgive you");
					}
    			}
    		});
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
					    self.caller.stream((MediaFile)clickedEntry, 0, null, 0, null, true);
					} catch (MalformedURLException e) {
						Util.showSingleButtonAlertBox(self.getSherlockActivity(), "The URL was bad in some way.", "Acceptance");
						e.printStackTrace();
					} catch (ActivityNotFoundException e) {
						Util.showSingleButtonAlertBox(self.getSherlockActivity(), "You don't have an app to handle files of type " + ((MediaFile)clickedEntry).contentType, "You're right");
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    		}
    	});
    	
    	// TODO: maybe scroll text on swipe/some other action via view.setSelected(true)
    }

	// just a little shortcut to avoid some horrible nested parentheses
	private SubsonicClientActivity getSubsonicActivity() {
		return (SubsonicClientActivity)this.getSherlockActivity();
	}
}
	