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

final class Folder extends FilesystemEntry {
    @Override
    public boolean equals(final Object o) {
        return o instanceof Folder && ((Folder) o).id.equals(id);
    }

    static final Folder ROOT_FOLDER = new Folder(Integer.MIN_VALUE, null, "Root");

    final Integer coverArtId;
    final String artist, album;
    final Calendar created;
    final boolean isTopLevel;

    // Constructor for top-level folder
    Folder(final Integer id, final Integer parentId, final String name) {
        this(id, parentId, name, null, null, null, null, true);
    }

    Folder(final Integer id, final Integer parentId, final String name, final Integer coverArtId, final String artist,
            final String album, final Calendar created, final boolean isTopLevel) {
        super(id, parentId, true, name);

        this.coverArtId = coverArtId;
        this.artist = artist;
        this.album = album;
        this.created = created;
        this.isTopLevel = isTopLevel;
    }

    Folder(final Cursor c) {
        super(c);
        isTopLevel = c.getInt(c.getColumnIndex(DatabaseHelper.IS_TOP_LEVEL.name)) == 1;

        artist = c.getString(c.getColumnIndex(DatabaseHelper.ARTIST.name));
        album = c.getString(c.getColumnIndex(DatabaseHelper.ALBUM.name));
        coverArtId = c.getInt(c.getColumnIndex(DatabaseHelper.COVER_ART_ID.name));
        created = Util.getDateFromISOString(c.getString(c.getColumnIndex(DatabaseHelper.CREATED.name)));
    }

    // Get a ContentValues object representing this folder's members, to insert into a database
    ContentValues getContentValues() {
        final ContentValues cv = new ContentValues();
        cv.putAll(super.getContentValues());

        cv.put(DatabaseHelper.COVER_ART_ID.name, coverArtId);
        cv.put(DatabaseHelper.ARTIST.name, artist);
        cv.put(DatabaseHelper.ALBUM.name, album);
        cv.put(DatabaseHelper.IS_TOP_LEVEL.name, isTopLevel ? 1 : 0);

        if (created != null)
            cv.put(DatabaseHelper.CREATED.name, Util.getISOStringFromDate(created));

        return cv;
    }

    // Called when the object is serialized to a Parcel (for inter-process communication)
    @Override
    public void writeToParcel(final Parcel out, final int flags) {
        super.writeToParcel(out, flags);
        writeParcelPart(out);
    }

    // Serialize the members that are particular to this object
    void writeParcelPart(final Parcel out) {
        out.writeValue(coverArtId);
        out.writeString(artist);
        out.writeString(album);
        out.writeString(Util.getISOStringFromDate(created));
        out.writeInt(isTopLevel ? 1 : 0);
    }

    public static final Creator<Folder> CREATOR = new Creator<Folder>() {
        @Override
        public Folder createFromParcel(final Parcel in) {
            return (Folder) getInstance(in);
        }

        @Override
        public Folder[] newArray(final int size) {
            return new Folder[size];
        }
    };

    Folder(final Integer id, final Integer parentId, final String name, final Parcel in) {
        this(
            id,
            parentId,
            name,
            (Integer) in.readValue(ClassLoader.getSystemClassLoader()),
            in.readString(),
            in.readString(),
            Util.getDateFromISOString(in.readString()),
            in.readInt() == 1
        );
    }
}
