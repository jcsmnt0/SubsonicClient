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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class SubsonicProvider extends ContentProvider {
    private SubsonicDatabaseHelper mDatabaseHelper;
    private UriMatcher mUriMatcher;

    private static final String AUTHORITY = "com.casamento.subsonicclient.SubsonicProvider";

    enum Queries {
        TOP_LEVEL_FOLDERS("top_level_folders", true),
        FOLDER_CONTENTS("folder_contents", false),
        FILESYSTEM_ENTRY("filesystem_entry", false),
        INSERT("insert", true),
        UPDATE("update", false);

        private final String CONTENT_TYPE = "filesystem_entry";

        final Uri uri;
        final String basePath, type;

        Queries(final String uriPart, final boolean isDir) {
            final String finalSegment = isDir ? "" : "/*";
            basePath = uriPart + finalSegment;
            uri = Uri.parse("content://" + AUTHORITY + "/" + uriPart);
            type = (isDir ? ContentResolver.CURSOR_DIR_BASE_TYPE : ContentResolver.CURSOR_ITEM_BASE_TYPE) + "/" +
                    CONTENT_TYPE + finalSegment;
        }
    }

    private UriMatcher getUriMatcher() {
        final UriMatcher u = new UriMatcher(UriMatcher.NO_MATCH);

        for (final Queries q : Queries.values())
            u.addURI(AUTHORITY, q.basePath, q.ordinal());

        return u;
    }

    private Queries getQueryType(final Uri uri) {
        return Queries.values()[mUriMatcher.match(uri)];
    }


    @Override
    public boolean onCreate() {
        mDatabaseHelper = new SubsonicDatabaseHelper(getContext());
        mUriMatcher = getUriMatcher();
        return true;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection, final String[] selectionArgs,
            final String orderBy) {
        switch (getQueryType(uri)) {
            case TOP_LEVEL_FOLDERS:
                return mDatabaseHelper.getTopLevelCursor();

            case FOLDER_CONTENTS:
                return mDatabaseHelper.getFolderContentsCursor(Integer.parseInt(uri.getLastPathSegment()));

            case FILESYSTEM_ENTRY:
                return mDatabaseHelper.getFilesystemEntry(Integer.parseInt(uri.getLastPathSegment()));

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public String getType(final Uri uri) {
        return Queries.values()[mUriMatcher.match(uri)].type;
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues contentValues) {
        switch (getQueryType(uri)) {
            case INSERT:
                final String row = Long.toString(mDatabaseHelper.insert(contentValues));
                return Uri.withAppendedPath(Queries.FILESYSTEM_ENTRY.uri, row);

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        switch (getQueryType(uri)) {
            case TOP_LEVEL_FOLDERS:
                return mDatabaseHelper.deleteTopLevelFolders();

            case FOLDER_CONTENTS:
                return mDatabaseHelper.deleteFolder(Integer.parseInt(uri.getLastPathSegment()));

            case FILESYSTEM_ENTRY:
                return mDatabaseHelper.deleteEntry(Integer.parseInt(uri.getLastPathSegment()));

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public int update(final Uri uri, final ContentValues contentValues, final String selection,
            final String[] selectionArgs) {
        switch (getQueryType(uri)) {
            case UPDATE:
                final int row = Integer.parseInt(uri.getLastPathSegment());
                return mDatabaseHelper.update(row, contentValues);

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }
}
