/*
 * Copyright (c) 2012, Joseph Casamento
 * All rights reserved.
 *
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

import java.text.ParseException;
import java.util.Calendar;

import static com.casamento.subsonicclient.SubsonicCaller.DatabaseHelper;

abstract class FilesystemEntry {
	int id;
	boolean isFolder;
	int parentId;

	// name is "title" attribute of files, "name" attribute of directories/indices in Subsonic's terminology
	String name;

	static FilesystemEntry getInstance(JSONObject j) throws JSONException, ParseException {
		return getInstance(j, j.optBoolean("isDir"));
	}

	static FilesystemEntry getInstance(JSONObject j, Boolean isFolder) throws JSONException, ParseException {
		FilesystemEntry newEntry;
		if (isFolder) newEntry = new Folder(j);
		else newEntry = new MediaFile(j);

		newEntry.id = j.getInt("id");
		newEntry.parentId = j.optInt("parentId", Integer.MIN_VALUE);
		newEntry.isFolder = isFolder;

		newEntry.name = j.optString("name", null);
		if (newEntry.name  == null) newEntry.name = j.getString("title");

		return newEntry;
	}

	static FilesystemEntry getInstance(Cursor c) {
		boolean isFolder = c.getInt(c.getColumnIndex(DatabaseHelper.IS_FOLDER.name)) == 1;

		FilesystemEntry newEntry;
		if (isFolder) newEntry = new Folder(c);
		else newEntry = new MediaFile(c);

		newEntry.id = c.getInt(c.getColumnIndex(DatabaseHelper.ID.name));
		newEntry.parentId = c.getInt(c.getColumnIndex(DatabaseHelper.PARENT_FOLDER.name));
		newEntry.isFolder = isFolder;
		newEntry.name = c.getString(c.getColumnIndex(DatabaseHelper.NAME.name));

		return newEntry;
	}

	public String toString() {
		return this.name;
	}

	ContentValues getContentValues() {
		ContentValues cv = new ContentValues();

		cv.put(DatabaseHelper.ID.name, id);
		cv.put(DatabaseHelper.NAME.name, name);
		cv.put(DatabaseHelper.PARENT_FOLDER.name, parentId);
		cv.put(DatabaseHelper.IS_FOLDER.name, isFolder ? 1 : 0);

		return cv;
	}

	static class Folder extends FilesystemEntry {
		final int coverArtId;
		final String artist, album;
		final Calendar created;
		final boolean isTopLevel; // MusicFolder, in Subsonic's weird terminology

		Folder(int id, String name, int parentId) {
			this.id = id;
			this.name = name;
			this.isFolder = true;
			this.parentId = parentId;

			coverArtId = -1;
			artist = album = null;
			created = null;
			isTopLevel = this.id <= 0;
		}

		private Folder(JSONObject jFolder) throws JSONException, ParseException {
			isTopLevel = false;

			artist	= Util.fixHTML(jFolder.optString("artist", null));
			album	= Util.fixHTML(jFolder.optString("album", null));

			parentId	= jFolder.optInt("parent", Integer.MIN_VALUE);
			coverArtId	= jFolder.optInt("coverArt", Integer.MIN_VALUE);

			created = Util.getDateFromString(jFolder.optString("created", null));
		}

		private Folder(Cursor c) {
			isTopLevel = c.getInt(c.getColumnIndex(DatabaseHelper.IS_TOP_LEVEL.name)) == 1;

			artist = c.getString(c.getColumnIndex(DatabaseHelper.ARTIST.name));
			album = c.getString(c.getColumnIndex(DatabaseHelper.ALBUM.name));
			coverArtId = c.getInt(c.getColumnIndex(DatabaseHelper.COVER_ART_ID.name));
			created = Util.getDateFromString(c.getString(c.getColumnIndex(DatabaseHelper.CREATED.name)));
		}

		ContentValues getContentValues() {
			ContentValues cv = new ContentValues();

			cv.putAll(super.getContentValues());
			cv.put(DatabaseHelper.COVER_ART_ID.name, coverArtId);
			cv.put(DatabaseHelper.ARTIST.name, artist);
			cv.put(DatabaseHelper.ALBUM.name, album);
			cv.put(DatabaseHelper.IS_TOP_LEVEL.name, isTopLevel ? 1 : 0);

			if (created != null)
				cv.put(DatabaseHelper.CREATED.name, Util.getStringFromDate(created));

			return cv;
		}

		Cursor getContentsCursor(DatabaseHelper dbHelper) {
			return dbHelper.query(DatabaseHelper.PARENT_FOLDER.name + "=" + id);
		}
	}

	static class MediaFile extends FilesystemEntry {
		final String contentType, suffix, transcodedSuffix, transcodedContentType, path, album, artist, type;
		final int duration, bitRate, albumId, artistId, year;
		final long size;
		final boolean isVideo;
		final Calendar created;

		private MediaFile(JSONObject jFile) throws JSONException, ParseException {
			this.path	= jFile.getString("path");
			this.suffix	= Util.fixHTML(jFile.getString("suffix"));

			this.contentType			= Util.fixHTML(jFile.optString("contentType", null));
			this.transcodedSuffix		= Util.fixHTML(jFile.optString("transcodedSuffix", null));
			this.transcodedContentType	= Util.fixHTML(jFile.optString("transcodedContentType", null));
			this.album					= Util.fixHTML(jFile.optString("album", null));
			this.artist					= Util.fixHTML(jFile.optString("artist", null));
			this.type					= Util.fixHTML(jFile.optString("type", null));

			this.size = jFile.optLong("size", -1);

			this.isVideo = jFile.optBoolean("isVideo", false);

			this.duration	= jFile.optInt("duration", -1);
			this.bitRate	= jFile.optInt("bitRate", -1);
			this.albumId	= jFile.optInt("albumId", -1);
			this.artistId	= jFile.optInt("artistId", -1);
			this.year		= jFile.optInt("year", -1);

			this.created = Util.getDateFromString(jFile.optString("created", null));
		}

		private MediaFile(Cursor c) {
			this.path = c.getString(c.getColumnIndex(DatabaseHelper.PATH.name));
			this.suffix = c.getString(c.getColumnIndex(DatabaseHelper.SUFFIX.name));

			this.contentType = c.getString(c.getColumnIndex(DatabaseHelper.CONTENT_TYPE.name));
			this.transcodedContentType = c.getString(c.getColumnIndex(DatabaseHelper.TRANSCODED_CONTENT_TYPE.name));
			this.transcodedSuffix = c.getString(c.getColumnIndex(DatabaseHelper.TRANSCODED_SUFFIX.name));
			this.album = c.getString(c.getColumnIndex(DatabaseHelper.ALBUM.name));
			this.artist = c.getString(c.getColumnIndex(DatabaseHelper.ARTIST.name));
			this.type = c.getString(c.getColumnIndex(DatabaseHelper.TYPE.name));

			this.size = c.getLong(c.getColumnIndex(DatabaseHelper.SIZE.name));

			this.isVideo = c.getInt(c.getColumnIndex(DatabaseHelper.IS_VIDEO.name)) == 1;

			this.duration = c.getInt(c.getColumnIndex(DatabaseHelper.DURATION.name));
			this.bitRate = c.getInt(c.getColumnIndex(DatabaseHelper.BIT_RATE.name));
			this.albumId = c.getInt(c.getColumnIndex(DatabaseHelper.ALBUM_ID.name));
			this.artistId = c.getInt(c.getColumnIndex(DatabaseHelper.ARTIST_ID.name));
			this.year = c.getInt(c.getColumnIndex(DatabaseHelper.YEAR.name));

			this.created = Util.getDateFromString(c.getString(c.getColumnIndex(DatabaseHelper.CREATED.name)));
		}

		ContentValues getContentValues() {
			ContentValues cv = new ContentValues();
			cv.putAll(super.getContentValues());

			cv.put(DatabaseHelper.PATH.name, path);
			cv.put(DatabaseHelper.SUFFIX.name, suffix);
			cv.put(DatabaseHelper.CONTENT_TYPE.name, contentType);
			cv.put(DatabaseHelper.TRANSCODED_CONTENT_TYPE.name, transcodedContentType);
			cv.put(DatabaseHelper.TRANSCODED_SUFFIX.name, transcodedSuffix);
			cv.put(DatabaseHelper.ARTIST.name, artist);
			cv.put(DatabaseHelper.ALBUM.name, album);
			cv.put(DatabaseHelper.TYPE.name, type);
			cv.put(DatabaseHelper.SIZE.name, size);
			cv.put(DatabaseHelper.IS_VIDEO.name, isVideo);
			cv.put(DatabaseHelper.DURATION.name, duration);
			cv.put(DatabaseHelper.BIT_RATE.name, bitRate);
			cv.put(DatabaseHelper.ALBUM_ID.name, albumId);
			cv.put(DatabaseHelper.ARTIST_ID.name, artistId);
			cv.put(DatabaseHelper.YEAR.name, year);
			cv.put(DatabaseHelper.CREATED.name, Util.getStringFromDate(created));

			return cv;
		}
	}
}
