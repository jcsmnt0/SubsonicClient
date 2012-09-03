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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

class MediaFile extends FilesystemEntry {
    final String contentType, suffix, transcodedSuffix, transcodedContentType, path, album, artist, type;
    final int duration, bitRate, albumId, artistId, year;
    final long size;
    final boolean isVideo;
    final Calendar created;

    MediaFile(final JSONObject jFile) throws JSONException {
        path = jFile.getString("path");
        suffix = Util.fixHTML(jFile.getString("suffix"));

        contentType           = Util.fixHTML(jFile.optString("contentType", null));
        transcodedSuffix      = Util.fixHTML(jFile.optString("transcodedSuffix", null));
        transcodedContentType = Util.fixHTML(jFile.optString("transcodedContentType", null));
        album                 = Util.fixHTML(jFile.optString("album", null));
        artist                = Util.fixHTML(jFile.optString("artist", null));
        type                  = Util.fixHTML(jFile.optString("type", null));

        size = jFile.optLong("size", -1);

        isVideo = jFile.optBoolean("isVideo", false);

        duration	= jFile.optInt("duration", -1);
        bitRate	= jFile.optInt("bitRate", -1);
        albumId	= jFile.optInt("albumId", -1);
        artistId	= jFile.optInt("artistId", -1);
        year		= jFile.optInt("year", -1);

        created = Util.getDateFromString(jFile.optString("created", null));
    }

    MediaFile(final Cursor c) {
        path = c.getString(c.getColumnIndex(SubsonicCaller.DatabaseHelper.PATH.name));
        suffix = c.getString(c.getColumnIndex(SubsonicCaller.DatabaseHelper.SUFFIX.name));

        contentType = c.getString(c.getColumnIndex(SubsonicCaller.DatabaseHelper.CONTENT_TYPE.name));
        transcodedContentType = c.getString(c.getColumnIndex(SubsonicCaller.DatabaseHelper.TRANSCODED_CONTENT_TYPE.name));
        transcodedSuffix = c.getString(c.getColumnIndex(SubsonicCaller.DatabaseHelper.TRANSCODED_SUFFIX.name));
        album = c.getString(c.getColumnIndex(SubsonicCaller.DatabaseHelper.ALBUM.name));
        artist = c.getString(c.getColumnIndex(SubsonicCaller.DatabaseHelper.ARTIST.name));
        type = c.getString(c.getColumnIndex(SubsonicCaller.DatabaseHelper.TYPE.name));

        size = c.getLong(c.getColumnIndex(SubsonicCaller.DatabaseHelper.SIZE.name));

        isVideo = c.getInt(c.getColumnIndex(SubsonicCaller.DatabaseHelper.IS_VIDEO.name)) == 1;

        duration = c.getInt(c.getColumnIndex(SubsonicCaller.DatabaseHelper.DURATION.name));
        bitRate = c.getInt(c.getColumnIndex(SubsonicCaller.DatabaseHelper.BIT_RATE.name));
        albumId = c.getInt(c.getColumnIndex(SubsonicCaller.DatabaseHelper.ALBUM_ID.name));
        artistId = c.getInt(c.getColumnIndex(SubsonicCaller.DatabaseHelper.ARTIST_ID.name));
        year = c.getInt(c.getColumnIndex(SubsonicCaller.DatabaseHelper.YEAR.name));

        created = Util.getDateFromString(c.getString(c.getColumnIndex(SubsonicCaller.DatabaseHelper.CREATED.name)));
    }

    ContentValues getContentValues() {
        final ContentValues cv = new ContentValues();
        cv.putAll(super.getContentValues());

        cv.put(SubsonicCaller.DatabaseHelper.PATH.name, path);
        cv.put(SubsonicCaller.DatabaseHelper.SUFFIX.name, suffix);
        cv.put(SubsonicCaller.DatabaseHelper.CONTENT_TYPE.name, contentType);
        cv.put(SubsonicCaller.DatabaseHelper.TRANSCODED_CONTENT_TYPE.name, transcodedContentType);
        cv.put(SubsonicCaller.DatabaseHelper.TRANSCODED_SUFFIX.name, transcodedSuffix);
        cv.put(SubsonicCaller.DatabaseHelper.ARTIST.name, artist);
        cv.put(SubsonicCaller.DatabaseHelper.ALBUM.name, album);
        cv.put(SubsonicCaller.DatabaseHelper.TYPE.name, type);
        cv.put(SubsonicCaller.DatabaseHelper.SIZE.name, size);
        cv.put(SubsonicCaller.DatabaseHelper.IS_VIDEO.name, isVideo);
        cv.put(SubsonicCaller.DatabaseHelper.DURATION.name, duration);
        cv.put(SubsonicCaller.DatabaseHelper.BIT_RATE.name, bitRate);
        cv.put(SubsonicCaller.DatabaseHelper.ALBUM_ID.name, albumId);
        cv.put(SubsonicCaller.DatabaseHelper.ARTIST_ID.name, artistId);
        cv.put(SubsonicCaller.DatabaseHelper.YEAR.name, year);
        cv.put(SubsonicCaller.DatabaseHelper.CREATED.name, Util.getStringFromDate(created));

        return cv;
    }
}
