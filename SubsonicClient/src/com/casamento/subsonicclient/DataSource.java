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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;

import static com.casamento.subsonicclient.DataRetrievalService.*;

class DataSource {
    static final int URI_SEGMENT_TABLE_NAME = 0;

    private final Context mContext;

    private final AccessInformation mAccessInfo;
    private final String mTableName;
    private final Class mDataServiceClass;

    enum CommandType {
        TOP_LEVEL_FOLDERS("folder_contents", false),
        FOLDER_CONTENTS("folder_contents", true),
        FILESYSTEM_ENTRY("filesystem_entry", true),
        INSERT("insert", false),
        UPDATE("update", true);

        private final String CONTENT_TYPE = "filesystem_entry";

        final String basePath, type, uriPart;

        CommandType(final String theUriPart, final boolean requiresId) {
            uriPart = theUriPart;

            final String lastSegment = requiresId ?
                    "/*" :
                    "";

            basePath = uriPart + lastSegment;

            type = requiresId ?
                    ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_TYPE + lastSegment :
                    ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_TYPE + lastSegment;
        }
    }

    DataSource(final Context context, final String tableName, final AccessInformation accessInfo,
            final Class dataServiceClass) {
        mContext = context;
        mTableName = tableName;
        mAccessInfo = accessInfo;
        mDataServiceClass = dataServiceClass;
    }

    // Build a URI to perform a CRUD operation on the database table
    static Uri buildUri(final String table, final CommandType command) {
        return buildUri(table, command, (String) null);
    }

    static Uri buildUri(final String table, final CommandType command, final Folder folder) {
        return Folder.ROOT_FOLDER.equals(folder) ?
                buildUri(table, command) :
                buildUri(table, command, folder.id.toString());
    }

    static Uri buildUri(final String table, final CommandType command, final Long id) {
        return buildUri(table, command, id.toString());
    }

    static Uri buildUri(final String table, final CommandType command, final Integer id) {
        return buildUri(table, command, id.toString());
    }

    static Uri buildUri(final String table, final CommandType command, final String id) {
        Uri uri = Uri.parse("content://" + AUTHORITY);
        uri = Uri.withAppendedPath(uri, table);
        uri = Uri.withAppendedPath(uri, command.uriPart);

        return id == null ?
                uri :
                Uri.withAppendedPath(uri, id);
    }

    static CommandType getCommandType(final Uri uri) {
        return CommandType.values()[mUriMatcher.match(uri)];
    }

    AccessInformation getAccessInformation() {
        return mAccessInfo;
    }

    // TODO: this fails
    private int deleteFolderContents(final Folder f) {
        final Uri uri = buildUri(mTableName, CommandType.FOLDER_CONTENTS, f);
        return mContext.getContentResolver().delete(uri, null, null);
    }

    private Cursor getFolderContentsCursor(final Folder f) {
        final Uri uri = buildUri(mTableName, CommandType.FOLDER_CONTENTS, f);
        return mContext.getContentResolver().query(uri, DatabaseHelper.getColumnNames(), null, null, null);
    }

    // Inserts FilesystemEntries into the database table
    private void insertFilesystemEntries(final FilesystemEntry... entries) {
        final Uri uri = buildUri(mTableName, CommandType.INSERT);

        final ContentResolver cr = mContext.getContentResolver();
        for (final FilesystemEntry entry : entries)
            cr.insert(uri, entry.getContentValues());
    }

    Loader<Cursor> getFolderContentsCursorLoader(final Folder folder, final boolean refresh) {
        if (refresh)
            deleteFolderContents(folder);

        return new AsyncTaskLoader<Cursor>(mContext) {
            @Override
            protected void onStartLoading() {
                super.onStartLoading();

                // Trigger loadInBackground() immediately; no setup to do here
                forceLoad();
            }

            @Override
            public void deliverResult(final Cursor cursor) {
                // A null return value means the result has been deferred; loadInBackground() will be called again once
                // there's data to deliver, and hand off a valid Cursor to this function
                if (cursor != null)
                    super.deliverResult(cursor);
            }

            // N.B. The return value of this method gets immediately handed off to deliverResult()
            @Override
            public Cursor loadInBackground() {
                // If the data is already in the database, return the Cursor immediately
                final Cursor c = getFolderContentsCursor(folder);
                if (c.getCount() > 0)
                    return c;

                // Otherwise, put out an Intent to tell the IntentService to retrieve the data from the server
                final Intent folderContentsRequest = new Intent(mContext, mDataServiceClass);
                folderContentsRequest.setData(buildUri(mTableName, CommandType.FOLDER_CONTENTS, folder));
                folderContentsRequest.putExtra(IN_ACCESS_INFO, mAccessInfo);
                folderContentsRequest.putExtra(IN_FOLDER, folder);

                // Add the callback for when the data is finished being retrieved
                folderContentsRequest.putExtra(IN_RESULT_RECEIVER, new ResultReceiver(null) {
                    @Override
                    protected void onReceiveResult(final int resultCode, final Bundle data) {
                        // TODO: error handling
                        if (resultCode == RESULT_CODE_OK) {
                            // Put the new data into the database
                            final FilesystemEntry[] contents = (FilesystemEntry[]) data.getParcelableArray(OUT_RESULTS);
                            insertFilesystemEntries(contents);

                            // Restart loadInBackground() now that the data is in the database
                            forceLoad();
                        }
                    }
                });

                // Send the Intent on its way
                mContext.startService(folderContentsRequest);

                // A null result is ignored in deliverResult(); when the IntentService finishes processing,
                // loadInBackground() will be restarted and deliver the cursor in the
                return null;
            }
        };
    }

    abstract static class AccessInformation implements Parcelable {
        abstract boolean hasCache();
        abstract String getDownloadUrl(MediaFile mf, boolean transcoded);
        abstract String getUsername();
        abstract String getPassword();
    }

    static final String AUTHORITY = "com.casamento.subsonicclient.FilesystemEntryProvider";
    static UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH) {
        {
            for (final CommandType q : CommandType.values())
                addURI(AUTHORITY, "*/" + q.basePath, q.ordinal());
        }
    };
}
