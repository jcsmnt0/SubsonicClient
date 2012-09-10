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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SubsonicLoaders {
    private static final String logTag = "SubsonicLoaders";

    private abstract static class SubsonicLoader<T> extends RestLoader<T> {
        private final String mUrl, mUsername, mPassword;
        private final Map<String, String> mRequiredParams = new HashMap<String, String>();

        SubsonicLoader(final Context c, final String url, final String username, final String password) {
            super(c);
            mUrl = url;
            mUsername = username;
            mPassword = password;

            mRequiredParams.put("c", "SubsonicClient");
            mRequiredParams.put("f", "json");
            mRequiredParams.put("v", "1.8.0");
        }

        /*
         * Methods to retrieve data from the database
         */

        Cursor getTopLevelFoldersCursor() {
            return getContext().getContentResolver().query(SubsonicProvider.Queries.TOP_LEVEL_FOLDERS.uri,
                    SubsonicDatabaseHelper.getColumnNames(), null, null, null);
        }

        Cursor getFolderContentsCursor(final int folderId) {
            final Uri uri = Uri.withAppendedPath(SubsonicProvider.Queries.FOLDER_CONTENTS.uri,
                    Integer.toString(folderId));
            return getContext().getContentResolver().query(uri, SubsonicDatabaseHelper.getColumnNames(), null, null,
                    null);
        }

        FilesystemEntry getFilesystemEntry(final int entryId) {
            final Uri uri = Uri.withAppendedPath(SubsonicProvider.Queries.FILESYSTEM_ENTRY.uri,
                    Integer.toString(entryId));
            final Cursor c = getContext().getContentResolver().query(uri, SubsonicDatabaseHelper.getColumnNames(), null,
                    null, null);

            c.moveToFirst();

            return FilesystemEntry.getInstance(c);
        }


        /*
         * Methods to put data into the database
         */

        void insertFilesystemEntries(final FilesystemEntry[] entries) {
            for (final FilesystemEntry entry : entries)
                getContext().getContentResolver().insert(SubsonicProvider.Queries.INSERT.uri, entry.getContentValues());
        }


        /*
         * Methods to retrieve data from the server
         */

        Folder[] retrieveTopLevelFolders() throws AuthenticationException, SubsonicException, JSONException, IOException {
            final String callUrl = buildRestCall(mUrl, "rest/getMusicFolders.view", mRequiredParams);
            final String responseStr = readAll(getStream(callUrl, mUsername, mPassword));
            final JSONObject jResponse = parseSubsonicResponse(responseStr);
            final JSONArray jFolderArr = jResponse.getJSONObject("musicFolders").getJSONArray("musicFolder");

            final int folderCount = jFolderArr.length();
            final Folder[] folders = new Folder[folderCount];

            for (int i = 0; i < folderCount; i++) {
                final JSONObject jFolder = jFolderArr.getJSONObject(i);
                folders[i] = new Folder(-jFolder.getInt("id"), jFolder.getString("name"), Folder.NULL_ID);
            }

            return folders;
        }

        FilesystemEntry[] retrieveFolderContents(final Folder f) throws IOException, AuthenticationException, SubsonicException, JSONException {
            final Map<String, String> params = new HashMap<String, String>(mRequiredParams);
            final String callUrl;

            if (f.isTopLevel) {
                params.put("musicFolderId", Integer.toString(-f.id));
                callUrl = buildRestCall(mUrl, "rest/getIndexes.view", params);
            } else {
                params.put("id", Integer.toString(f.id));
                callUrl = buildRestCall(mUrl, "rest/getMusicDirectory.view", params);
            }

            final String responseStr = readAll(getStream(callUrl, mUsername, mPassword));
            final JSONObject jResponse = parseSubsonicResponse(responseStr);

            JSONObject jContentsObj = jResponse.optJSONObject("indexes");
            if (jContentsObj == null) jContentsObj = jResponse.getJSONObject("directory");

            final List<FilesystemEntry> contents = new ArrayList<FilesystemEntry>();

            final JSONArray jChildArr = jContentsObj.optJSONArray("child");

            if (jChildArr == null) {
                final JSONObject jChild = jContentsObj.optJSONObject("child");
                if (jChild != null)
                    contents.add(FilesystemEntry.getInstance(jChild, f.id));
            } else {
                final int jChildArrLen = jChildArr.length();
                for (int i = 0; i < jChildArrLen; i++)
                    contents.add(FilesystemEntry.getInstance(jChildArr.getJSONObject(i), f.id));
            }

            final JSONArray jIndexArr = jContentsObj.optJSONArray("index");
            if (jIndexArr != null) {
                final int jIndexArrLen = jIndexArr.length();

                for (int i = 0; i < jIndexArrLen; i++) {
                    final JSONObject jIndex = jIndexArr.getJSONObject(i);

                    // artist tag can be either an array or an object (if there's only one)
                    final JSONArray jFolderArr = jIndex.optJSONArray("artist");
                    if (jFolderArr != null) {
                        final int jFolderArrayLength = jFolderArr.length();
                        for (int j = 0; j < jFolderArrayLength; j++)
                            contents.add(FilesystemEntry.getInstance(jFolderArr.getJSONObject(j), true, f.id));
                    } else
                        contents.add(FilesystemEntry.getInstance(jIndex.getJSONObject("artist"), true, f.id));
                }
            }

            final FilesystemEntry[] entryArr = new FilesystemEntry[contents.size()];
            return contents.toArray(entryArr);
        }

        private static JSONObject parseSubsonicResponse(final CharSequence responseStr) throws JSONException, SubsonicException {
            final JSONObject jResponse = new JSONObject((String) responseStr).getJSONObject("subsonic-response");

            // handle Subsonic errors
            if (jResponse != null && jResponse.getString("status").equals("failed")) {
                final JSONObject err = jResponse.getJSONObject("error");
                throw new SubsonicException(err.getInt("code"), err.getString("message"));
            }

            return jResponse;
        }
    }

    static class TopLevelFoldersCursorLoader extends SubsonicLoader<Cursor> {
        // if false, try to get data from the database and get it from the server if it's not there;
        // if true, remove data from the database and pull new data from the server
        final boolean mReloadData;

        TopLevelFoldersCursorLoader(final Context c, final String url, final String username, final String password) {
            this(c, url, username, password, false);
        }

        TopLevelFoldersCursorLoader(final Context c, final String url, final String username, final String password,
                final boolean reload) {
            super(c, url, username, password);
            mReloadData = reload;
        }

        @Override
        public Cursor loadInBackground() {
            // try to load data from the database (if not reloading)
            if (!mReloadData) {
                final Cursor c = getTopLevelFoldersCursor();
                if (c != null && c.getCount() > 0)
                    return c;
            }

            // load it from the server if it can't be found in the database

            // delete old data, if any exists
            getContext().getContentResolver().delete(SubsonicProvider.Queries.TOP_LEVEL_FOLDERS.uri, null, null);

            try {
                insertFilesystemEntries(retrieveTopLevelFolders());
            } catch (Exception e) {
                Log.e(logTag, "Error", e);
            }

            return getTopLevelFoldersCursor();
        }
    }

    static class FolderContentsCursorLoader extends SubsonicLoader<Cursor> {
        final boolean mReloadData;
        final int mFolderId;

        FolderContentsCursorLoader(final Context c, final String url, final String username, final String password,
                final int folderId) {
            this(c, url, username, password, folderId, false);
        }

        FolderContentsCursorLoader(final Context c, final String url, final String username, final String password,
                final int folderId, final boolean reload) {
            super(c, url, username, password);
            mReloadData = reload;
            mFolderId = folderId;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        public Cursor loadInBackground() {
            // try to load data from the database (if not reloading)
            if (!mReloadData) {
                final Cursor c = getFolderContentsCursor(mFolderId);
                if (c != null && c.getCount() > 0)
                    return c;
            }

            // load it from the server if it can't be found in the database

            // delete old data, if any exists
            final Uri deleteUri = Uri.withAppendedPath(SubsonicProvider.Queries.FOLDER_CONTENTS.uri,
                    Integer.toString(mFolderId));

            getContext().getContentResolver().delete(deleteUri, null, null);

            try {
                final Folder f = (Folder) getFilesystemEntry(mFolderId);
                insertFilesystemEntries(retrieveFolderContents(f));
            } catch (Exception e) {
                Log.e(logTag, "Error", e);
            }

            return getFolderContentsCursor(mFolderId);
        }
    }
}
