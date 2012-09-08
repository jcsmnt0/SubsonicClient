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
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

class SubsonicProvider extends ContentProvider {
    private DatabaseHelper mDatabaseHelper;

    private static final String AUTHORITY = "com.casamento.subsonicclient.SubsonicProvider";

    enum Queries {
        TOP_LEVEL_FOLDERS(""),
        FOLDER_CONTENTS,
        FILESYSTEM_ENTRY;

        final String uri;

        private Queries(final String theUri) {
            uri = theUri;
        }

        private UriMatcher getUriMatcher() {

        }
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getType(Uri uri) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "subsonic.db";
        private static final int DATABASE_VERSION = 3;
        private static final String TABLE_NAME = "filesystem_entries";
        private static DatabaseHelper mInstance;

        static final Column
                // FilesystemEntry attributes
                ID = new Column("_id", "integer primary key not null"),
                NAME = new Column("name", "text not null"),
                IS_FOLDER = new Column("is_folder", "integer not null"),

        // Folder/MediaFile attributes
        PARENT_FOLDER = new Column("parent_folder", "integer"),
                ARTIST = new Column("artist", "text"),
                ALBUM = new Column("album", "text"),
                COVER_ART_ID = new Column("cover_art_id", "integer"),
                CREATED = new Column("created", "text"),
                IS_TOP_LEVEL = new Column("is_top_level", "integer"),

        // MediaFile attributes
        PATH = new Column("path", "text"),
                SUFFIX = new Column("suffix", "text"),
                TRACK_NUMBER = new Column("track_number", "integer"),
                TRANSCODED_SUFFIX = new Column("transcoded_suffix", "text"),
                CONTENT_TYPE = new Column("content_type", "text"),
                TRANSCODED_CONTENT_TYPE = new Column("transcoded_content_type", "text"),
                TYPE = new Column("type", "text"),
                DURATION = new Column("duration", "integer"),
                BIT_RATE = new Column("bit_rate", "integer"),
                ARTIST_ID = new Column("artist_id", "integer"),
                ALBUM_ID = new Column("album_id", "integer"),
                YEAR = new Column("year", "integer"),
                SIZE = new Column("size", "integer"),
                IS_VIDEO = new Column("is_video", "integer"),

        // other stuff
        CACHED = new Column("cached", "integer");

        static final Column[] COLUMNS = {
                ID,
                NAME,
                IS_FOLDER,
                PARENT_FOLDER,
                ARTIST,
                ALBUM,
                COVER_ART_ID,
                CREATED,
                IS_TOP_LEVEL,
                PATH,
                SUFFIX,
                TRACK_NUMBER,
                TRANSCODED_SUFFIX,
                CONTENT_TYPE,
                TRANSCODED_CONTENT_TYPE,
                TYPE,
                DURATION,
                BIT_RATE,
                ARTIST_ID,
                ALBUM_ID,
                YEAR,
                SIZE,
                IS_VIDEO,
                CACHED
        };

        static String[] getColumnNames() {
            final int columnCount = COLUMNS.length;
            final String[] columnNames = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columnNames[i] = COLUMNS[i].name;
            }
            return columnNames;
        }

        public static String getCreateCommand() {
            String createCommand = "create table if not exists " + TABLE_NAME + "(";
            for (final Column column : COLUMNS) {
                createCommand += column.name + " " + column.type + ",";
            }
            return createCommand.substring(0, createCommand.length() - 1) + ");";
        }

        // ensure only one instance is ever active
        static DatabaseHelper getInstance(final Context context) {
            if (mInstance == null)
                mInstance = new DatabaseHelper(context.getApplicationContext());
            return mInstance;
        }

        private DatabaseHelper(final Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            final SQLiteDatabase db = getWritableDatabase();
            //db.execSQL("drop table if exists " + TABLE_NAME + ";");
            db.execSQL(getCreateCommand());
        }

        Cursor query(final String whereClause, final String orderByClause) {
            return getReadableDatabase().query(TABLE_NAME, getColumnNames(), whereClause, null, null, null,
                    orderByClause);
        }

        Cursor getTopLevelCursor() {
            return query(IS_TOP_LEVEL.name + "= '1'", NAME.name + " collate nocase");
        }

        Cursor getFolderContentsCursor(final Folder f) {
            final String orderBy = "coalesce(" + TRACK_NUMBER.name + "," + NAME.name + ") collate nocase";
            return query(PARENT_FOLDER.name + "=" + f.id, orderBy);
        }

        Cursor getFilesystemEntry(final int id) {
            return query(ID.name + "=" + id, NAME.name + " collate nocase");
        }

        long insert(final ContentValues values) {
            return getWritableDatabase().insertOrThrow(TABLE_NAME, null, values);
        }

        int delete(final Folder f) {
            final String[] id = { Integer.toString(f == null ? Folder.NULL_ID : f.id) };
            return getWritableDatabase().delete(TABLE_NAME, PARENT_FOLDER.name + "=?", id);
        }

        int update(final FilesystemEntry f, final ContentValues values) {
            final String[] id = { Integer.toString(f.id) };
            return getWritableDatabase().update(TABLE_NAME, values, ID.name + "=?", id);
        }

        @Override
        public void onCreate(final SQLiteDatabase database) {
            database.execSQL(getCreateCommand());
        }

        @Override
        public void onUpgrade(final SQLiteDatabase database, final int oldVersion, final int newVersion) {
            database.execSQL("drop table if exists " + TABLE_NAME);
            onCreate(database);
        }

        static class Column {
            final String name, type;
            Column(final String theName, final String theType) {
                name = theName;
                type = theType;
            }
        }
    }
}
