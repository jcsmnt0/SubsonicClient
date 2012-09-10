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

import static com.casamento.subsonicclient.SubsonicCaller.DatabaseHelper.*;

abstract class FilesystemEntry {
    int id;
    boolean isFolder;
    int parentId;

    // name is "title" attribute of files, "name" attribute of directories/indices in Subsonic's terminology
    String name;

    static FilesystemEntry getInstance(final JSONObject j) throws JSONException {
        return getInstance(j, j.optBoolean("isDir"));
    }

    static boolean isFolder(final Cursor c) {
        return c.getInt(c.getColumnIndex(IS_FOLDER.name)) == 1;
    }

    static FilesystemEntry getInstance(final JSONObject j, final boolean isFolder) throws JSONException {
        final FilesystemEntry newEntry = isFolder ? new Folder(j) : new MediaFile(j);

        newEntry.id = j.getInt("id");
        newEntry.parentId = j.optInt("parentId", Folder.NULL_ID);
        newEntry.isFolder = isFolder;

        newEntry.name = j.optString("name", null);
        if (newEntry.name  == null) newEntry.name = j.getString("title");

        return newEntry;
    }

    static FilesystemEntry getInstance(final JSONObject j, final int parentId) throws JSONException {
        final FilesystemEntry newEntry = getInstance(j);
        newEntry.parentId = parentId;
        return newEntry;
    }

    static FilesystemEntry getInstance(final JSONObject j, final boolean isFolder, final int parentId) throws JSONException {
        final FilesystemEntry newEntry = getInstance(j, isFolder);
        newEntry.parentId = parentId;
        return newEntry;
    }

    static FilesystemEntry getInstance(final Cursor c) {
        final boolean isFolder = isFolder(c);

        final FilesystemEntry newEntry = isFolder ? new Folder(c) : new MediaFile(c);

        newEntry.id = c.getInt(c.getColumnIndex(ID.name));
        newEntry.parentId = c.getInt(c.getColumnIndex(PARENT_FOLDER.name));
        newEntry.isFolder = isFolder;
        newEntry.name = c.getString(c.getColumnIndex(NAME.name));

        return newEntry;
    }

    public String toString() {
        return name;
    }

    ContentValues getContentValues() {
        final ContentValues cv = new ContentValues();

        cv.put(ID.name, id);
        cv.put(NAME.name, name);
        cv.put(PARENT_FOLDER.name, parentId);
        cv.put(IS_FOLDER.name, isFolder ? 1 : 0);

        return cv;
    }

}
