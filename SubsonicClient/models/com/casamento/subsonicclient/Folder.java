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

import android.content.ContentValues;
import android.database.Cursor;
import org.json.JSONObject;

import java.util.Calendar;

class Folder extends FilesystemEntry {
    static final int NULL_ID = Integer.MIN_VALUE;
    final int coverArtId;
    final String artist, album;
    final Calendar created;
    final boolean isTopLevel; // MusicFolder, in Subsonic's weird terminology

    Folder(final int id, final String name, final int parentId) {
        isFolder = true;

        this.id = id;
        this.name = name;
        this.parentId = parentId;

        coverArtId = -1;
        artist = album = null;
        created = null;
        isTopLevel = this.id <= 0;
    }

    Folder(final JSONObject jFolder) {
        isTopLevel = false;

        artist	= Util.fixHTML(jFolder.optString("artist", null));
        album	= Util.fixHTML(jFolder.optString("album", null));

        parentId	= jFolder.optInt("parent", NULL_ID);
        coverArtId	= jFolder.optInt("coverArt", NULL_ID);

        created = Util.getDateFromString(jFolder.optString("created", null));
    }

    Folder(final Cursor c) {
        isTopLevel = c.getInt(c.getColumnIndex(SubsonicCaller.DatabaseHelper.IS_TOP_LEVEL.name)) == 1;

        artist = c.getString(c.getColumnIndex(SubsonicCaller.DatabaseHelper.ARTIST.name));
        album = c.getString(c.getColumnIndex(SubsonicCaller.DatabaseHelper.ALBUM.name));
        coverArtId = c.getInt(c.getColumnIndex(SubsonicCaller.DatabaseHelper.COVER_ART_ID.name));
        created = Util.getDateFromString(c.getString(c.getColumnIndex(SubsonicCaller.DatabaseHelper.CREATED.name)));
    }

    ContentValues getContentValues() {
        final ContentValues cv = new ContentValues();

        cv.putAll(super.getContentValues());
        cv.put(SubsonicCaller.DatabaseHelper.COVER_ART_ID.name, coverArtId);
        cv.put(SubsonicCaller.DatabaseHelper.ARTIST.name, artist);
        cv.put(SubsonicCaller.DatabaseHelper.ALBUM.name, album);
        cv.put(SubsonicCaller.DatabaseHelper.IS_TOP_LEVEL.name, isTopLevel ? 1 : 0);

        if (created != null)
            cv.put(SubsonicCaller.DatabaseHelper.CREATED.name, Util.getStringFromDate(created));

        return cv;
    }

    Cursor getContentsCursor(final SubsonicCaller.DatabaseHelper dbHelper) {
        return dbHelper.query(SubsonicCaller.DatabaseHelper.PARENT_FOLDER.name + "=" + id);
    }
}
