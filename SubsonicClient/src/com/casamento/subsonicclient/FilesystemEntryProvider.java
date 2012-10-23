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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.List;

public class FilesystemEntryProvider extends ContentProvider {
    private DatabaseHelper mDatabaseHelper;

    private List<String> mTables;

    private void createTable(final SQLiteDatabase db, final String table) {
        String createCommand = "create table if not exists " + table + "(";
        for (final DatabaseHelper.Column column : DatabaseHelper.COLUMNS) {
            createCommand += column.name + " " + column.type + ",";
        }

        // Trim trailing ',' and terminate command
        createCommand = createCommand.substring(0, createCommand.length()-1) + ");";

        db.execSQL(createCommand);
    }

    private void createTableIfNecessary(final SQLiteDatabase db, final String table) {
        if (!mTables.contains(table)) {
            createTable(db, table);
            mTables.add(table);
        }
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        mTables = mDatabaseHelper.getTables();
        return true;
    }

    private final String ordering = "coalesce(" + DatabaseHelper.TRACK_NUMBER.name + "," + DatabaseHelper.NAME.name
            + ") collate nocase";

    @Override
    public Cursor query(final Uri uri, final String[] columns, final String _ignoredSelection,
            final String[] _ignoredSelectionArgs, final String _ignoredOrderBy) {
        final SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        final String table = uri.getPathSegments().get(DataSource.URI_SEGMENT_TABLE_NAME);

        createTableIfNecessary(db, table);

        switch (DataSource.getCommandType(uri)) {
            case TOP_LEVEL_FOLDERS: {
                final String selection = DatabaseHelper.IS_TOP_LEVEL.name + "=1";
                return db.query(table, columns, selection, null, null, null, ordering);
            }

            case FOLDER_CONTENTS: {
                final int id = Integer.parseInt(uri.getLastPathSegment());
                final String selection = DatabaseHelper.PARENT_FOLDER.name + "=" + id;
                return db.query(table, columns, selection, null, null, null, ordering);
            }

            case FILESYSTEM_ENTRY: {
                final int id = Integer.parseInt(uri.getLastPathSegment());
                final String selection = DatabaseHelper.ID.name + "=" + id;
                return db.query(table, columns, selection, null, null, null, ordering);
            }

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public String getType(final Uri uri) {
        return DataSource.CommandType.values()[DataSource.mUriMatcher.match(uri)].type;
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final String table = uri.getPathSegments().get(DataSource.URI_SEGMENT_TABLE_NAME);

        createTableIfNecessary(db, table);

        switch (DataSource.getCommandType(uri)) {
            case INSERT: {
                final long row = db.insertOrThrow(table, null, values);
                return DataSource.buildUri(table, DataSource.CommandType.FILESYSTEM_ENTRY, row);
            }

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    private static int deleteTopLevelFolders(final SQLiteDatabase db, final String table) {
        // First delete top-level folders, to get a valid count...
        final int topLevelFolderCount = db.delete(table, DatabaseHelper.IS_TOP_LEVEL.name + "=1", null);

        // then delete everything else, since everything is a child of a top-level folder
        db.delete(table, null, null);

        return topLevelFolderCount;
    }

    /**
     * Recursively deletes the contents of a folder from the given database/table
     * @param db The database that contains the table
     * @param table The table that contains the folder
     * @param folder The folder to delete
     * @return The number of immediate children that the deleted folder contained
     */
    private int deleteFolderContents(final SQLiteDatabase db, final String table, final Folder folder) {
        // Get a Cursor for the folder contents
        final Uri folderContentsUri = DataSource.buildUri(table, DataSource.CommandType.FOLDER_CONTENTS, folder);
        final String[] columns = { DatabaseHelper.ID.name, DatabaseHelper.IS_FOLDER.name };
        final Cursor folderContentsCursor = query(folderContentsUri, columns, null, null, null);

        int deletedEntries = 0;

        while (folderContentsCursor.moveToNext()) {
            final Folder child = new Folder(folderContentsCursor);

            // If the current entry is a folder, recursively delete all of its children
            if (child.isFolder)
                deleteFolderContents(db, table, child);

            // Delete the entry itself
            if (!folder.equals(Folder.ROOT_FOLDER)) {
                final Uri deleteUri = DataSource.buildUri(table, DataSource.CommandType.FILESYSTEM_ENTRY, folder);
                delete(deleteUri, null, null);
            }

            deletedEntries++;
        }

        return deletedEntries;
    }

    @Override
    public int delete(final Uri uri, final String _ignoredSelection, final String[] _ignoredSelectionArgs) {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final String table = uri.getPathSegments().get(DataSource.URI_SEGMENT_TABLE_NAME);

        createTableIfNecessary(db, table);

        switch (DataSource.getCommandType(uri)) {
            case TOP_LEVEL_FOLDERS: {
                return deleteTopLevelFolders(db, table);
            }

            case FOLDER_CONTENTS: {
                final int folderId = Integer.parseInt(uri.getLastPathSegment());
                final Uri folderUri = DataSource.buildUri(table, DataSource.CommandType.FILESYSTEM_ENTRY, folderId);
                final Folder f = new Folder(query(folderUri, DatabaseHelper.getColumnNames(), null, null, null));
                return deleteFolderContents(db, table, f);
            }

            case FILESYSTEM_ENTRY: {
                return db.delete(table, DatabaseHelper.ID.name + "=" + uri.getLastPathSegment(), null);
            }

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public int update(final Uri uri, final ContentValues values, final String _ignoredSelection,
            final String[] _ignoredSelectionArgs) {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final String table = uri.getPathSegments().get(DataSource.URI_SEGMENT_TABLE_NAME);

        createTableIfNecessary(db, table);

        switch (DataSource.getCommandType(uri)) {
            case UPDATE:
                final String entryId = uri.getLastPathSegment();
                return db.update(table, values, DatabaseHelper.ID.name + "=" + entryId, null);

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }
}
