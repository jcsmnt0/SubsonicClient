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
import android.os.Parcelable;

abstract class FilesystemEntry implements Parcelable {
    final Integer id;
    final boolean isFolder;
    final Integer parentId;

    // name is "title" attribute of files, "name" attribute of directories/indices in Subsonic's terminology
    final String name;

    static boolean isFolder(final Cursor c) {
        return c.getInt(c.getColumnIndex(DatabaseHelper.IS_FOLDER.name)) == 1;
    }

    protected FilesystemEntry(final Integer id, final Integer parentId, final boolean isFolder, final String name) {
        this.id = id;
        this.parentId = parentId;
        this.isFolder = isFolder;
        this.name = name;
    }

    protected FilesystemEntry(final Cursor c) {
        this(
            c.getInt(c.getColumnIndex(DatabaseHelper.ID.name)),
            c.getInt(c.getColumnIndex(DatabaseHelper.PARENT_FOLDER.name)),
            c.getInt(c.getColumnIndex(DatabaseHelper.IS_FOLDER.name)) == 1,
            c.getString(c.getColumnIndex(DatabaseHelper.NAME.name))
        );
    }

    static FilesystemEntry getInstance(final Cursor c) {
        final boolean isFolder = isFolder(c);

        return isFolder ?
                new Folder(c) :
                new MediaFile(c);
    }

    public String toString() {
        return name;
    }

    ContentValues getContentValues() {
        final ContentValues cv = new ContentValues();

        cv.put(DatabaseHelper.ID.name, id);
        cv.put(DatabaseHelper.NAME.name, name);
        cv.put(DatabaseHelper.PARENT_FOLDER.name, parentId);
        cv.put(DatabaseHelper.IS_FOLDER.name, isFolder ? 1 : 0);

        return cv;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel out, final int flags) {
        out.writeValue(id);
        out.writeString(name);
        out.writeValue(parentId);
        out.writeInt(isFolder ? 1 : 0);
    }

    public static final Creator<FilesystemEntry> CREATOR = new Creator<FilesystemEntry>() {
        @Override
        public FilesystemEntry createFromParcel(final Parcel in) {
            return getInstance(in);
        }

        @Override
        public FilesystemEntry[] newArray(final int size) {
            return new FilesystemEntry[size];
        }
    };

    static FilesystemEntry getInstance(final Parcel in) {
        final ClassLoader loader = ClassLoader.getSystemClassLoader();
        final Integer id = (Integer) in.readValue(loader);
        final String name = in.readString();
        final Integer parentId = (Integer) in.readValue(loader);
        final boolean isFolder = in.readInt() == 1;

        return isFolder ?
                new Folder(id, parentId, name, in) :
                new MediaFile(id, parentId, name, in);
    }
}
