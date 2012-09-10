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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
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

import static com.casamento.subsonicclient.MainActivity.FOLDER_CONTENTS_LOADER;
import static com.casamento.subsonicclient.SubsonicCaller.DatabaseHelper.*;

public class ServerBrowserFragment extends SherlockListFragment {
    private static final String logTag = "ServerBrowserFragment";
    private final Cursor mCursor;
    private final Folder mFolder;
    private CursorAdapter mAdapter;
    private boolean mLoading;

    void setLoading(final boolean loading) {
        mLoading = loading;
    }

    Folder getFolder() { return mFolder; }

    private ActivityCallback mActivity;

    // containing Activity must implement this interface; enforced in onAttach
    interface ActivityCallback {
        void showFolderContents(Folder folder, boolean addToBackStack);
        void popServerBrowserFragment();
        ActionBar getSupportActionBar();
        void download(FilesystemEntry entry, boolean transcoded);
        void refresh(ServerBrowserFragment sbf);
        Uri getFilesystemEntryUri(int id);
        String[] getAllColumns();
        SubsonicLoaders.FolderContentsCursorLoader getFolderContentsCursorLoader(int folderId);
    }

    public ServerBrowserFragment() {
        this(null, null);
    }

    ServerBrowserFragment(final Cursor cursor, final Folder folder) {
        mCursor = cursor;
        mFolder = folder;
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        // ensure activity implements the interface this Fragment needs
        try {
            mActivity = (ActivityCallback)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(mActivity + " must implement ServerBrowserFragment.ActivityCallback");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final boolean homeButtonEnabled = (mFolder != null);
        final ActionBar ab = mActivity.getSupportActionBar();

        ab.setHomeButtonEnabled(homeButtonEnabled);
        ab.setDisplayHomeAsUpEnabled(homeButtonEnabled);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        final ListView lv = getListView();
        lv.setOnItemClickListener(mFilesystemEntryClickListener);
        registerForContextMenu(lv);
        lv.setFastScrollEnabled(true);

        if (mCursor != null && !mLoading) {
            mAdapter = new FilesystemEntryCursorAdapter(getSherlockActivity(), mCursor, false);
            setListAdapter(mAdapter);
        }
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
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
        final FilesystemEntry entry = FilesystemEntry.getInstance((Cursor) getListAdapter().getItem(info.position));

        menu.setHeaderTitle(entry.name);

        if (entry.isFolder) {
            getSherlockActivity().getMenuInflater().inflate(R.menu.contextmenu_folder, menu);
        } else {
            final MediaFile mediaFile = (MediaFile)entry;
            getSherlockActivity().getMenuInflater().inflate(R.menu.contextmenu_file, menu);

            final CharSequence originalText = TextUtils.expandTemplate("Download Original File (^1)", mediaFile.suffix);
            menu.findItem(R.id.download_original_file).setTitle(originalText);

            final CharSequence transcodedText = TextUtils.expandTemplate("Download Transcoded File (^1)",
                    mediaFile.transcodedSuffix != null ? mediaFile.transcodedSuffix : mediaFile.suffix);
            menu.findItem(R.id.download_transcoded_file).setTitle(transcodedText);
        }
    }

    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final FilesystemEntry entry = FilesystemEntry.getInstance((Cursor) getListAdapter().getItem(info.position));

        if (entry.isFolder) {
            final Folder folder = (Folder)entry;

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
                        // TODO: better exception handling
                        Log.e(logTag, e.toString());
                    }
	                return true;

                case R.id.download_transcoded_file:
                    try {
                        mActivity.download(mediaFile, true);
                    } catch (Exception e) {
                        Log.e(logTag, e.toString());
                    }
	                return true;

                case R.id.stream_file:
//					try {
//						SubsonicCaller.stream(mediaFile, 0, null, 0, null, false);
//					} catch (Exception e) {
//						mActivity.showDialogFragment(new AlertDialogFragment(getSherlockActivity(),
//								getString(R.string.error), e.getLocalizedMessage()));
//						Log.e(logTag, e.toString());
//					}
	                return true;

                default:
                    return super.onContextItemSelected(item);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        if (menu.findItem(R.id.refresh) == null)
            getSherlockActivity().getSupportMenuInflater().inflate(R.menu.optionsmenu_server_browser, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mActivity.popServerBrowserFragment();
                return true;

            case R.id.refresh:
                mActivity.refresh(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private final OnItemClickListener mFilesystemEntryClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
            final int folderId = (Integer) view.getTag();

            getLoaderManager().initLoader(FOLDER_CONTENTS_LOADER, null, new LoaderCallbacks<Cursor>() {
                @Override
                public Loader<Cursor> onCreateLoader(final int id, final Bundle data) {
                    return mActivity.getFolderContentsCursorLoader(folderId);
                }

                @Override
                public void onLoadFinished(final Loader<Cursor> cursorLoader, final Cursor cursor) {
                    mAdapter.swapCursor(cursor);
                    getLoaderManager().destroyLoader(FOLDER_CONTENTS_LOADER);
                }

                @Override
                public void onLoaderReset(final Loader<Cursor> cursorLoader) {
                }
            });
        }
    };

    private static class FilesystemEntryCursorAdapter extends CursorAdapter implements SectionIndexer {
        private final LayoutInflater mInflater;
        private final int idCol, nameCol, trackNumCol, cachedCol;
        private AlphabetIndexer mIndexer;

        private FilesystemEntryCursorAdapter(final Context context, final Cursor c, final boolean autoRequery) {
            super(context, c, autoRequery);

            mIndexer = new AlphabetIndexer(c, c.getColumnIndex(NAME.name), "ABCDEFGHIJKLMNOPQRSTUVWXYZ");

            mInflater = LayoutInflater.from(context);

            idCol       = c.getColumnIndex(ID.name);
            nameCol     = c.getColumnIndex(NAME.name);
            cachedCol   = c.getColumnIndex(CACHED.name);
            trackNumCol = c.getColumnIndex(TRACK_NUMBER.name);
        }

        @Override
        public View newView(final Context context, final Cursor cursor, final ViewGroup viewGroup) {
            return mInflater.inflate(R.layout.music_folder_row_layout, viewGroup, false);
        }

        @Override
        public Cursor swapCursor(final Cursor c) {
            mIndexer = new AlphabetIndexer(c, c.getColumnIndex(NAME.name), "ABCDEFGHIJKLMNOPQRSTUVWXYZ");

            return super.swapCursor(c);
        }

        @Override
        public void bindView(final View view, final Context context, final Cursor cursor) {
            final TextView tv = (TextView)view.findViewById(R.id.label);

            final String name = cursor.getString(nameCol);
            final int trackNumber = cursor.getInt(trackNumCol);

            final String displayName = trackNumber > 0 ? Integer.toString(trackNumber) + ". " + name : name;

            tv.setText(displayName);

            // set the subsonic ID as the view's tag
            view.setTag(cursor.getInt(idCol));

            view.setBackgroundResource(cursor.getInt(cachedCol) == 1 ?
                    R.color.abs__holo_blue_light : R.color.abs__background_holo_dark);
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
