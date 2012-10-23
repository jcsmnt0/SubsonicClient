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
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import java.util.Stack;

import static com.casamento.subsonicclient.MainActivity.FOLDER_CONTENTS_LOADER;
import static com.casamento.subsonicclient.MainActivity.TOP_LEVEL_FOLDERS_LOADER;

public class ServerBrowserFragment extends SherlockListFragment {
    private final Stack<Cursor> mCursorStack = new Stack<Cursor>();
    private final Stack<Folder> mFolderStack = new Stack<Folder>();
    private CursorAdapter mAdapter;

    private ActivityCallback mActivity;

    // containing Activity must implement this interface; enforced in onAttach
    interface ActivityCallback {
        ActionBar getSupportActionBar();
        void download(FilesystemEntry entry, boolean transcoded);
        void showProgressSpinner();
        void hideProgressSpinner();
        DataSource getDataSource();
        void runOnUiThread(Runnable r);
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        // ensure activity implements the interface this Fragment needs and get a handle to it
        try {
            mActivity = (ActivityCallback)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(mActivity + " must implement ServerBrowserFragment.ActivityCallback");
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tell the OS this fragment has its own options to add to the Activity's options
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set up the list view to handle clicks and long-presses
        final ListView lv = getListView();
        lv.setOnItemClickListener(mFilesystemEntryClickListener);
        registerForContextMenu(lv);

        // Register for the scroll bar handle for fast scrolling
        lv.setFastScrollEnabled(true);

        // Initialize the fragment if nothing is currently shown
        if (mFolderStack.isEmpty())
            initFragment();
    }

    // Performs one-time initialization when this Fragment is created
    private void initFragment() {
        setListAdapter(mAdapter);

        // Load and show the top level folders from the server
        getLoaderManager().restartLoader(TOP_LEVEL_FOLDERS_LOADER, null, new LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(final int id, final Bundle data) {
                // Start the loading
                mActivity.showProgressSpinner();
                return mActivity.getDataSource().getFolderContentsCursorLoader(Folder.ROOT_FOLDER, false);
            }

            @Override
            public void onLoadFinished(final Loader<Cursor> cursorLoader, final Cursor cursor) {
                // Display the loaded data
                mAdapter = new FilesystemEntryCursorAdapter(getSherlockActivity(), cursor, false);
                setListAdapter(mAdapter);

                mFolderStack.push(Folder.ROOT_FOLDER);

                mAdapter.changeCursor(cursor);
                mActivity.hideProgressSpinner();
            }

            @Override
            public void onLoaderReset(final Loader<Cursor> cursorLoader) { /* Do nothing (for now) */ }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshHomeButton();
    }

    // Set the home button in the action bar to go up one level if there is a level to go up, and to not be tappable
    // otherwise
    private void refreshHomeButton() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ActionBar ab = mActivity.getSupportActionBar();

                final boolean enabled = (mFolderStack.size() > 1);
                ab.setHomeButtonEnabled(enabled);
                ab.setDisplayHomeAsUpEnabled(enabled);
            }
        });
    }

    // bug fix as per https://code.google.com/p/android/issues/detail?id=19917
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug:fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Loads the contents of a Folder and displays them in the main ListView.
     * @param folder The Folder to load the contents of
     * @param keepOldCursor If true, the Cursor pointing to the currently visible data will be kept in memory
     * @param reloadData If true, the data will be forced to refresh from the server; otherwise it'll be loaded
     *                   from the database cache if possible
     */
    private void loadAndShowFolderContents(final Folder folder, final boolean keepOldCursor, final boolean reloadData) {
        getLoaderManager().restartLoader(FOLDER_CONTENTS_LOADER, null, new LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(final int id, final Bundle data) {
                mActivity.showProgressSpinner();
                return mActivity.getDataSource().getFolderContentsCursorLoader(folder, reloadData);
            }

            @Override
            public void onLoadFinished(final Loader<Cursor> cursorLoader, final Cursor cursor) {
                if (keepOldCursor)
                    mAdapter.swapCursor(cursor);
                else
                    mAdapter.changeCursor(cursor);

                mActivity.hideProgressSpinner();
                getLoaderManager().destroyLoader(FOLDER_CONTENTS_LOADER);
            }

            @Override
            public void onLoaderReset(final Loader<Cursor> cursorLoader) {}
        });
    }

    // Push a folder onto the stack and display its contents
    private void pushFolder(final Folder folder) {
        mFolderStack.push(folder);
        loadAndShowFolderContents(folder, true, false);
    }

    // Pop a folder off the stack and display its parent's contents
    private void popFolder() {
        mFolderStack.pop();
        loadAndShowFolderContents(mFolderStack.peek(), false, false);
    }

    // TODO: the delete operation fails
    private void refreshFolder() {
        loadAndShowFolderContents(mFolderStack.peek(), false, true);
    }

    // Add this Fragment's menu options to the context menu dynamically
    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
        final FilesystemEntry entry = FilesystemEntry.getInstance((Cursor) getListAdapter().getItem(info.position));

        menu.setHeaderTitle(entry.name);

        final int menuRes = entry.isFolder ? R.menu.contextmenu_folder : R.menu.contextmenu_file;
        getSherlockActivity().getMenuInflater().inflate(menuRes, menu);
    }

    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final FilesystemEntry entry = FilesystemEntry.getInstance((Cursor) getListAdapter().getItem(info.position));

        if (entry.isFolder) {
            final Folder folder = (Folder) entry;

            switch (item.getItemId()) {
                case R.id.folderContextMenu_downloadOriginal:
                    mActivity.download(folder, false);
	                return true;

                case R.id.folderContextMenu_downloadTranscoded:
                    mActivity.download(folder, true);
	                return true;

                default:
                    return super.onContextItemSelected(item);
            }
        } else {
            final MediaFile mediaFile = (MediaFile)entry;

            switch (item.getItemId()) {
                case R.id.download_original_file:
                    try {
                        mActivity.download(mediaFile, false);
                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), e.toString());
                    }
	                return true;

                case R.id.download_transcoded_file:
                    try {
                        mActivity.download(mediaFile, true);
                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), e.toString());
                    }
	                return true;

                default:
                    return super.onContextItemSelected(item);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        // For some reason, this adds menu items every time it's called; so check to see if this Fragment's items
        // already exist before adding them
        if (menu.findItem(R.id.refresh) == null)
            getSherlockActivity().getSupportMenuInflater().inflate(R.menu.optionsmenu_server_browser, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            // When the home button is pressed, go up one level
            case android.R.id.home:
                popFolder();
                return true;

            case R.id.refresh:
                refreshFolder();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private final OnItemClickListener mFilesystemEntryClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(final AdapterView<?> adapterView, final View view, final int position, final long id) {
            final FilesystemEntry entry = FilesystemEntry.getInstance((Cursor) getListAdapter().getItem(position));

            // Show the contents of the tapped folder (if it is a folder)
            if (entry.isFolder)
                pushFolder((Folder) entry);
        }
    };

    private class FilesystemEntryCursorAdapter extends CursorAdapter implements SectionIndexer {
        private final LayoutInflater mInflater;
        private final int nameCol, trackNumCol;
        private AlphabetIndexer mIndexer;

        private static final String INDEXES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        private FilesystemEntryCursorAdapter(final Context context, final Cursor c, final boolean autoRequery) {
            super(context, c, autoRequery);

            mIndexer = new AlphabetIndexer(c, c.getColumnIndex(DatabaseHelper.NAME.name), INDEXES);

            mInflater = LayoutInflater.from(context);

            nameCol     = c.getColumnIndex(DatabaseHelper.NAME.name);
            trackNumCol = c.getColumnIndex(DatabaseHelper.TRACK_NUMBER.name);
        }

        @Override
        public View newView(final Context context, final Cursor cursor, final ViewGroup viewGroup) {
            final View v = mInflater.inflate(R.layout.music_folder_row_layout, viewGroup, false);
            v.setTag(R.id.label, v.findViewById(R.id.label));

            return v;
        }

        // Changes the current cursor to a new one, but keeps the old one in memory
        @Override
        public Cursor swapCursor(final Cursor c) {
            refreshIndexer(c);
            refreshHomeButton();

            return super.swapCursor(c);
        }

        // Changes the cursor to a new one and allows the old one to be garbage collected
        @Override
        public void changeCursor(final Cursor c) {
            refreshIndexer(c);
            refreshHomeButton();

            super.changeCursor(c);
        }

        private void refreshIndexer(final Cursor c) {
            mIndexer = new AlphabetIndexer(c, c.getColumnIndex(DatabaseHelper.NAME.name), INDEXES);
        }

        @Override
        public void bindView(final View view, final Context context, final Cursor cursor) {
            final TextView tv = (TextView) view.getTag(R.id.label);

            final String name = cursor.getString(nameCol);
            final int trackNumber = cursor.getInt(trackNumCol);

            final String displayName = trackNumber > 0 ? Integer.toString(trackNumber) + ". " + name : name;

            tv.setText(displayName);
        }

        @Override
        public int getPositionForSection(final int section) {
            return mIndexer.getPositionForSection(section);
        }

        @Override
        public int getSectionForPosition(final int section) {
            return mIndexer.getSectionForPosition(section);
        }

        @Override
        public Object[] getSections() {
            return mIndexer.getSections();
        }
    }
}
