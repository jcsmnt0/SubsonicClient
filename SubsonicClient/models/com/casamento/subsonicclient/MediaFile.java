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
import android.os.Parcel;

import java.util.Calendar;

final class MediaFile extends FilesystemEntry {
    final String suffix, transcodedSuffix, path, album, artist;
    final Integer duration, trackNumber, coverArtId;
    final Long size;
    final Calendar created;

    MediaFile(final Integer id, final Integer parentId, final String name, final Integer coverArtId,
            final String path, final String suffix, final String transcodedSuffix, final String artist,
            final String album, final Long size, final Integer duration, final Integer trackNumber,
            final Calendar created) {
        super(id, parentId, false, name);

        this.coverArtId = coverArtId;
        this.path = path;
        this.suffix = suffix;
        this.transcodedSuffix = transcodedSuffix;
        this.artist = artist;
        this.album = album;
        this.size = size;
        this.duration = duration;
        this.trackNumber = trackNumber;
        this.created = created;
    }

    MediaFile(final Cursor c) {
        super(c);
        path = c.getString(c.getColumnIndex(DatabaseHelper.PATH.name));
        suffix = c.getString(c.getColumnIndex(DatabaseHelper.SUFFIX.name));

        transcodedSuffix = c.getString(c.getColumnIndex(DatabaseHelper.TRANSCODED_SUFFIX.name));
        album = c.getString(c.getColumnIndex(DatabaseHelper.ALBUM.name));
        artist = c.getString(c.getColumnIndex(DatabaseHelper.ARTIST.name));

        size = c.getLong(c.getColumnIndex(DatabaseHelper.SIZE.name));

        duration = c.getInt(c.getColumnIndex(DatabaseHelper.DURATION.name));
        trackNumber = c.getInt(c.getColumnIndex(DatabaseHelper.TRACK_NUMBER.name));
        coverArtId = c.getInt(c.getColumnIndex(DatabaseHelper.COVER_ART_ID.name));

        created = Util.getDateFromISOString(c.getString(c.getColumnIndex(DatabaseHelper.CREATED.name)));
    }

    ContentValues getContentValues() {
        final ContentValues cv = new ContentValues();
        cv.putAll(super.getContentValues());

        cv.put(DatabaseHelper.PATH.name, path);
        cv.put(DatabaseHelper.SUFFIX.name, suffix);
        cv.put(DatabaseHelper.TRACK_NUMBER.name, trackNumber);
        cv.put(DatabaseHelper.TRANSCODED_SUFFIX.name, transcodedSuffix);
        cv.put(DatabaseHelper.ARTIST.name, artist);
        cv.put(DatabaseHelper.ALBUM.name, album);
        cv.put(DatabaseHelper.SIZE.name, size);
        cv.put(DatabaseHelper.DURATION.name, duration);
        cv.put(DatabaseHelper.COVER_ART_ID.name, coverArtId);
        cv.put(DatabaseHelper.CREATED.name, Util.getISOStringFromDate(created));

        return cv;
    }

    @Override
    public void writeToParcel(final Parcel out, final int flags) {
        super.writeToParcel(out, flags);
        writeParcelPart(out);
    }

    void writeParcelPart(final Parcel out) {
        out.writeValue(coverArtId);

        out.writeString(path);
        out.writeString(suffix);
        out.writeString(transcodedSuffix);
        out.writeString(artist);
        out.writeString(album);

        out.writeValue(size);

        out.writeValue(duration);
        out.writeValue(trackNumber);

        out.writeString(Util.getISOStringFromDate(created));
    }

    public static final Creator<MediaFile> CREATOR = new Creator<MediaFile>() {
        @Override
        public MediaFile createFromParcel(final Parcel in) {
            return (MediaFile) getInstance(in);
        }

        @Override
        public MediaFile[] newArray(final int size) {
            return new MediaFile[size];
        }
    };

    MediaFile(final Integer id, final Integer parentId, final String name, final Parcel in) {
        // TODO: this is really ugly and redundant
        this(
                id,
                parentId,
                name,
                (Integer) in.readValue(ClassLoader.getSystemClassLoader()),
                in.readString(),
                in.readString(),
                in.readString(),
                in.readString(),
                in.readString(),
                (Long) in.readValue(ClassLoader.getSystemClassLoader()),
                (Integer) in.readValue(ClassLoader.getSystemClassLoader()),
                (Integer) in.readValue(ClassLoader.getSystemClassLoader()),
                Util.getDateFromISOString(in.readString())
        );
    }
}
