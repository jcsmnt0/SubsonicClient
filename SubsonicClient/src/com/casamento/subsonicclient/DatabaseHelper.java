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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

class DatabaseHelper extends SQLiteOpenHelper {
    static final String DATABASE_NAME = "filesystem_entries.db";
    static final int DATABASE_VERSION = 2;

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
            DURATION = new Column("duration", "integer"),
            BIT_RATE = new Column("bit_rate", "integer"),
            YEAR = new Column("year", "integer"),
            SIZE = new Column("size", "integer"),

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
            DURATION,
            BIT_RATE,
            YEAR,
            SIZE,
            CACHED
    };

    static String[] getColumnNames() {
        final int columnCount = COLUMNS.length;
        final String[] columnNames = new String[columnCount];

        for (int i = 0; i < columnCount; i++)
            columnNames[i] = COLUMNS[i].name;

        return columnNames;
    }

    List<String> getTables() {
        final List<String> tables = new ArrayList<String>();

        final Cursor c = getWritableDatabase().rawQuery("select name from sqlite_master where type='table'", null);
        while (c.moveToNext()) {
            final String table = c.getString(0);

            if (!table.equals("android_metadata"))
                tables.add(table);
        }

        return tables;
    }

    private final Context mContext;

    DatabaseHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        mContext = context;
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {}

    @Override
    public void onDowngrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        mContext.deleteDatabase(DATABASE_NAME);

        onCreate(db);
    }

    // TODO: this works, but crashes on the first launch after an upgrade
    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        mContext.deleteDatabase(DATABASE_NAME);

        onCreate(db);
    }

    static class Column {
        final String name, type;
        Column(final String theName, final String theType) {
            name = theName;
            type = theType;
        }
    }
}
