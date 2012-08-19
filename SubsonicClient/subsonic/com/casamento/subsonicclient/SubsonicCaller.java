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

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static com.casamento.subsonicclient.SubsonicCaller.DatabaseHelper.*;

class SubsonicCaller extends RestCaller {
	private static final String API_VERSION = "1.4.0"; // 1.4.0+ required for JSON
	private static final String CLIENT_ID = "Android Subsonic Client";
	private static final String logTag = "SubsonicCaller";
	private static String mServerUrl, mUsername, mPassword;
	private static Map<String, String> mRequiredParams;
	private static Activity mActivity;
	private static DatabaseHelper mDatabaseHelper;

	static String getUsername() {
		return mUsername;
	}

	static String getPassword() {
		return mPassword;
	}

	private static interface Methods {
		// documentation is mostly taken from http://www.subsonic.org/pages/api.jsp
		// "param: description" is a required parameter, "[param]: description" is optional
		// example outputs can be found at <server>/xsd


		// SYSTEM METHODS:

		/**
		 * Get details about the software license.
		 */
		static final String GET_LICENSE = "getLicense.view";

		/**
		 * Used to test connectivity with the server.
		 */
		static final String PING = "ping.view";


		// NAVIGATION METHODS:

		/**
		 * Returns all configured top-level media folders (confusingly termed "music
		 * folders" in the API, but called "media folders" in the Subsonic application
		 * itself).
		 */
		static final String GET_MEDIA_FOLDERS = "getMusicFolders.view";

		/**
		 * Returns an indexed structure of all artists.
		 * <p/>
		 * [musicFolderId]:		ID of media mFolder (from GET_MEDIA_FOLDERS)
		 * [ifModifiedSince]: 	If specified, only return a result if the
		 * artist collection has changed since the given time.
		 * format: java.util.Calendar.getTimeInMillis()
		 */
		static final String LIST_MEDIA_FOLDER_CONTENTS = "getIndexes.view";

		/**
		 * Returns a listing of all files in a music directory.
		 * Typically used to get list of albums for an artist,
		 * or list of songs for an album.
		 * <p/>
		 * id: 	A string which uniquely identifies the music mFolder.
		 * Obtained by calls to getIndexes or getMusicDirectory.
		 */
		static final String LIST_FOLDER_CONTENTS = "getMusicDirectory.view";

		/**
		 * (1.8.0+)
		 * Similar to LIST_MEDIA_FOLDER_CONTENTS, but organizes music
		 * according to ID3 tags.
		 */
		static final String LIST_ARTISTS = "getArtists.view";

		/**
		 * (1.8.0+)
		 * Returns details for an artist, including a list of albums.
		 * This method organizes music according to ID3 tags.
		 * <p/>
		 * id: ID of artist (e.g. from LIST_ARTISTS)
		 */
		static final String GET_ARTIST_DETAILS = "getArtist.view";

		/**
		 * (1.8.0+)
		 * Returns details for an album, including a list of songs.
		 * This method organizes music according to ID3 tags.
		 * <p/>
		 * id: ID of album (e.g. from GET_ARTIST_DETAILS or LIST_ALBUMS)
		 */
		static final String GET_ALBUM_DETAILS = "getAlbum.view";

		/**
		 * (1.8.0+)
		 * Returns details for a song.
		 * <p/>
		 * id: ID of a song (e.g. from GET_RANDOM_SONGS)
		 */
		static final String GET_SONG_DETAILS = "getSong.view";

		/**
		 * (1.8.0+)
		 * Returns all video files.
		 */
		static final String LIST_VIDEOS = "getVideos.view";

		// ALBUM & SONG LISTS

		/**
		 * (1.2.0+)
		 * Returns a list of random, newest, highest rated etc. albums.
		 * Similar to the album lists on the home page of the Subsonic web interface.
		 * <p/>
		 * type:		The list type. Must be one of the following: random, newest,
		 * highest, frequent, recent, alphabeticalByName (1.8.0+),
		 * alphabeticalByArtist (1.8.0+), starred (1.8.0+)
		 * [size]:		The number of albums to return. Default 10, max 500.
		 * [offset]:	The list offset, for paging.
		 */
		static final String LIST_ALBUMS = "getAlbumList.view";

		/**
		 * (1.8.0+)
		 * Similar to LIST_ALBUMS, but organizes music according to ID3 tags.
		 * <p/>
		 * type:		The list type. Must be one of the following: random, newest,
		 * highest, frequent, recent, alphabeticalByName (1.8.0+),
		 * alphabeticalByArtist (1.8.0+), starred (1.8.0+)
		 * [size]:		The number of albums to return. Default 10, max 500.
		 * [offset]:	The list offset, for paging.
		 */
		static final String LIST_ALBUMS_ID3 = "getAlbumList2.view";

		/**
		 * (1.2.0+)
		 * Returns random songs matching the given criteria.
		 * <p/>
		 * [size]:			Songs to return. Default 10, max 500.
		 * [genre]:			Only return songs within genre.
		 * [fromYear]:		Only return songs from year or after.
		 * [toYear]:		Only return songs from year or before.
		 * [musicFolderId]:	Only return songs in the matching root mFolder.
		 */
		static final String GET_RANDOM_SONGS = "getRandomSongs.view";

		static final String GET_NOW_PLAYING = "getNowPlaying.view";
		static final String GET_STARRED_ITEMS = "getStarred.view";
		static final String GET_STARRED_ITEMS_ID3 = "getStarred2.view";


		// SEARCHING

		static final String OLD_SEARCH = "search.view";
		static final String SEARCH = "search2.view";
		static final String SEARCH_ID3 = "search3.view";


		// PLAYLISTS

		static final String LIST_PLAYLISTS = "getPlaylists.view";
		static final String GET_PLAYLIST_DETAILS = "getPlaylist.view";
		static final String CREATE_PLAYLIST = "createPlaylist.view";
		static final String UPDATE_PLAYLIST = "updatePlaylist.view";
		static final String DELETE_PLAYLIST = "deletePlaylist.view";


		// MEDIA RETRIEVAL

		/**
		 * Streams a given media file.
		 * Returns binary data on success, REST response on error.
		 * <p/>
		 * id:						id of the file to stream
		 * [maxBitRate]:			(1.2.0+) 0 symbolizes no limit
		 * [format]:				(1.6.0+) preferred transcoding format (e.g. "mp3", "flv")
		 * [timeOffset]:			start streaming at given offset seconds into video
		 * [size]:					(1.6.0+) request video size as "WxH"
		 * [estimateContentLength]:	(1.8.0+) estimated size for transcoded media in Content-Length header
		 */
		static final String STREAM = "stream.view";
		static final String DOWNLOAD = "download.view";
		static final String GET_COVER_ART = "getCoverArt.view";
		static final String GET_LYRICS = "getLyrics.view";
		static final String GET_USER_AVATAR = "getAvatar.view";


		// MEDIA ANNOTATION

		static final String STAR = "star.view";
		static final String UNSTAR = "unstar.view";
		static final String SET_RATING = "setRating.view";
		static final String SCROBBLE = "scrobble.view";


		// SHARING

		static final String GET_SHARED_ITEMS = "getShares.view";
		static final String CREATE_SHARED_ITEM = "createShare.view";
		static final String UPDATE_SHARED_ITEM = "updateShare.view";
		static final String DELETE_SHARED_ITEM = "deleteShare.view";


		// PODCASTS

		static final String LIST_PODCASTS = "getPodcasts.view";


		// JUKEBOX

		static final String JUKEBOX_CONTROL = "jukeboxControl.view";


		// CHAT

		static final String LIST_CHAT_MESSAGES = "getChatMessages.view";
		static final String POST_CHAT_MESSAGE = "addChatMessage.view";


		// USER MANAGEMENT

		static final String GET_USER_DETAILS = "getUser.view";
		static final String CREATE_USER = "createUser.view";
		static final String DELETE_USER = "deleteUser.view";
		static final String CHANGE_USER_PASSWORD = "changePassword.view";
	}

	protected static interface OnPingResponseListener {
		void onPingResponse(final boolean ok);
	}

	static class DatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "subsonic.db";
		private static final int DATABASE_VERSION = 1;
		private static DatabaseHelper mInstance = null;

		static final String TABLE_NAME = "filesystem_entries";

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
				IS_VIDEO = new Column("is_video", "integer");

		static final Column[] COLUMNS = new Column[] {
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
			IS_VIDEO
		};

		static String[] getColumnNames() {
			int len = COLUMNS.length;
			String[] columnNames = new String[len];
			for (int i = 0; i < len; i++) {
				columnNames[i] = COLUMNS[i].name;
			}
			return columnNames;
		}

		public static String getCreateCommand() {
			String createCommand = "create table if not exists " + TABLE_NAME + "(";
			for (Column column : COLUMNS) {
				createCommand += column.name + " " + column.type + ",";
			}
			return createCommand.substring(0, createCommand.length() - 1) + ");";
		}

		// ensure only one instance is ever active
		static DatabaseHelper getInstance(Context context) {
			if (mInstance == null)
				mInstance = new DatabaseHelper(context.getApplicationContext());
			return mInstance;
		}

		private DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			SQLiteDatabase db = getWritableDatabase();
			db.execSQL("drop table if exists " + TABLE_NAME + ";");
			db.execSQL(getCreateCommand());
		}

		Cursor query(String whereClause) {
			String orderByClause = NAME.name + " collate nocase";
			return getReadableDatabase().query(TABLE_NAME, getColumnNames(), whereClause, null, null, null, orderByClause);
		}

		long insert(ContentValues values) {
			return getWritableDatabase().insertOrThrow(TABLE_NAME, null, values);
		}

		@Override
		public void onCreate(SQLiteDatabase database) {
			database.execSQL(getCreateCommand());
		}

		@Override
		public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
			database.execSQL("drop table if exists " + TABLE_NAME);
			onCreate(database);
		}

		static class Column {
			final String name, type;
			Column(final String name, final String type) {
				this.name = name;
				this.type = type;
			}
		}
	}

	static FilesystemEntry getFilesystemEntry(int id) {
		Cursor cursor = mDatabaseHelper.query(ID.name + "=" + id);
		cursor.moveToFirst();
		return FilesystemEntry.getInstance(cursor);
	}

	static void setServerDetails(final String serverUrl, final String username, final String password,
			final Activity activity) {
		mServerUrl = serverUrl + "/rest";
		mUsername = username;
		mPassword = password;

		mActivity = activity;
		mDatabaseHelper = getInstance(mActivity);

		mRequiredParams = new HashMap<String, String>();
		mRequiredParams.put("v", SubsonicCaller.API_VERSION);
		mRequiredParams.put("c", SubsonicCaller.CLIENT_ID);
		mRequiredParams.put("f", "json");
	}

	private static JSONObject parseSubsonicResponse(String responseStr) throws JSONException, SubsonicException {
		JSONObject jResponse = new JSONObject(responseStr).getJSONObject("subsonic-response");

		// handle Subsonic errors
		if (jResponse != null && jResponse.getString("status").equals("failed")) {
			JSONObject err = jResponse.getJSONObject("error");
			throw new SubsonicException(err.getInt("code"), err.getString("message"));
		}

		return jResponse;
	}

	static void ping(final OnPingResponseListener callbackListener) throws UnsupportedEncodingException, MalformedURLException, URISyntaxException {
		new RetrieveRestResponseTask(buildRestCall(mServerUrl, Methods.PING, mRequiredParams), mUsername, mPassword,
				new OnRestResponseListener() {
			@Override
			public void onRestResponse(String responseStr) {
				boolean ok;

				try {
					JSONObject jResponse = parseSubsonicResponse(responseStr);
					ok = (jResponse.getString("status").equals("ok"));
				} catch (Exception e) {
					Log.e(logTag, e.toString());
					ok = false;
				}

				callbackListener.onPingResponse(ok);
			}

			@Override
			public void onException(Exception e) {
				callbackListener.onPingResponse(false);
			}
		}).execute((Void) null);
	}

	static interface OnCursorRetrievedListener {
		void onCursorRetrieved(Cursor cursor);
		void onException(Exception e);
	}

	static interface OnRestResponseListener {
		void onRestResponse(String responseStr);
		void onException(Exception e);
	}

	static class RetrieveRestResponseTask extends RestCallTask<Void, Void, String> {
		private final String mUrl, mUsername, mPassword;
		private OnRestResponseListener mCallbackListener;

		RetrieveRestResponseTask(final String url, final String username, final String password,
				final OnRestResponseListener callbackListener) {
			mUrl = url;
			mUsername = username;
			mPassword = password;
			mCallbackListener = callbackListener;
		}

		@Override
		protected String doInBackground(Void... nothing) {
			try {
				return getRestResponse(mUrl, mUsername, mPassword);
			} catch (Exception e) {
				mCallbackListener.onException(e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(String response) {
			if (response != null)
				mCallbackListener.onRestResponse(response);
		}
	}

	static class RetrieveCursorTask extends RestCallTask<Void, Integer, Cursor> {
		private final static String logTag = "RetrieveCursorTask";
		private final FilesystemEntry.Folder mFolder;
		private final OnCursorRetrievedListener mCallbackListener;
		private int entriesInserted = 0;

		// this one is to get the list of top-level folders
		RetrieveCursorTask(OnCursorRetrievedListener callbackListener) {
			mFolder = null;
			mCallbackListener = callbackListener;
		}

		RetrieveCursorTask(FilesystemEntry.Folder folder, OnCursorRetrievedListener callbackListener) {
			mFolder = folder;
			mCallbackListener = callbackListener;
		}

		private void insertJSONObject(JSONObject jEntry, int parentId) throws ParseException, JSONException {
			FilesystemEntry entry = FilesystemEntry.getInstance(jEntry);
			entry.parentId = parentId;
			mDatabaseHelper.insert(entry.getContentValues());
			publishProgress(++entriesInserted);
		}

		private void insertJSONObject(JSONObject jEntry, Boolean isFolder, int parentId) throws ParseException, JSONException {
			FilesystemEntry entry = FilesystemEntry.getInstance(jEntry, isFolder);
			entry.parentId = parentId;
			mDatabaseHelper.insert(entry.getContentValues());
			publishProgress(++entriesInserted);
		}

		@Override
		protected Cursor doInBackground(Void... nothing) {
			// if mFolder is null, get the list of top-level folders
			if (mFolder == null) {
				Cursor cursor = mDatabaseHelper.query(IS_TOP_LEVEL.name + "= '1'");
				if (cursor != null && cursor.getCount() > 0)
					return cursor;

				try {
					String response = getRestResponse(
							buildRestCall(mServerUrl, Methods.GET_MEDIA_FOLDERS, mRequiredParams),
							mUsername,
							mPassword
					);

					if (isCancelled()) return null;

					JSONObject jResponse = parseSubsonicResponse(response);

					JSONArray jFolderArray = jResponse.getJSONObject("musicFolders").getJSONArray("musicFolder");
					int jFolderArrayLength = jFolderArray.length();
					for (int i = 0; i < jFolderArrayLength; i++) {
						JSONObject jFolder = jFolderArray.getJSONObject(i);
						FilesystemEntry.Folder folder = new FilesystemEntry.Folder(-jFolder.getInt("id"),
								jFolder.getString("name"), mFolder != null ? mFolder.id : Integer.MIN_VALUE);
						mDatabaseHelper.insert(folder.getContentValues());
						publishProgress(++entriesInserted);
					}

					return mDatabaseHelper.query(IS_TOP_LEVEL.name + "= '1'");
				} catch (Exception e) {
					mCallbackListener.onException(e);
					return null;
				}
			}

			// otherwise, get the contents of the mFolder that was passed

			// check if there's already data for the media mFolder in the database
			Cursor cursor = mFolder.getContentsCursor(mDatabaseHelper);
			if (cursor != null && cursor.getCount() > 0)
				return cursor;

			// otherwise, get the data from the server and insert it in the database, then return a new cursor
			try {
				Map<String, String> params = new HashMap<String, String>(mRequiredParams);
				if (mFolder.isTopLevel)
					params.put("musicFolderId", Integer.toString(-mFolder.id));
				else
					params.put("id", Integer.toString(mFolder.id));

				String response;
				response = getRestResponse(
						mFolder.isTopLevel ?
								buildRestCall(mServerUrl, Methods.LIST_MEDIA_FOLDER_CONTENTS, params) :
								buildRestCall(mServerUrl, Methods.LIST_FOLDER_CONTENTS, params),
						mUsername,
						mPassword
				);
				if (isCancelled()) return null;

				JSONObject jResponse = parseSubsonicResponse(response);

				JSONObject jIndexesResponse = jResponse.optJSONObject("indexes");
				if (jIndexesResponse == null) jIndexesResponse = jResponse.getJSONObject("directory");

				JSONArray jChildArray = jIndexesResponse.optJSONArray("child");
				if (jChildArray == null) {
					JSONObject jChild = jIndexesResponse.optJSONObject("child");
					if (jChild != null)
						insertJSONObject(jChild, mFolder.id);
				} else {
					int jChildArrayLength = jChildArray.length();
					for (int i = 0; i < jChildArrayLength; i++)
						insertJSONObject(jChildArray.getJSONObject(i), mFolder.id);
				}

				JSONArray jIndexArray = jIndexesResponse.optJSONArray("index");
				if (jIndexArray != null) {
					int jIndexArrayLength = jIndexArray.length();
					Log.d(logTag, "getting " + Integer.toString(jIndexArrayLength) + " items");

					for (int i = 0; i < jIndexArrayLength; i++) {
						JSONObject jIndex = jIndexArray.getJSONObject(i);

						// artist tag can be either an array or an object (if there's only one)
						JSONArray jFolderArray = jIndex.optJSONArray("artist");
						if (jFolderArray != null) {
							int jFolderArrayLength = jFolderArray.length();
							for (int j = 0; j < jFolderArrayLength; j++)
								insertJSONObject(jFolderArray.getJSONObject(j), true, mFolder.id);
						} else
							insertJSONObject(jIndex.getJSONObject("artist"), true, mFolder.id);
					}
				}

				return mFolder.getContentsCursor(mDatabaseHelper);
			} catch (Exception e) {
				mCallbackListener.onException(e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(Cursor cursor) {
			mCallbackListener.onCursorRetrieved(cursor);
		}
	}

//	/**
//	 * Streams a given media file to a capable external app via an Intent.
//	 *
//	 * @param mediaFile             The MediaFile to stream.
//	 * @param maxBitRate            (1.2.0+) The maximum bit rate to transcode to; 0 for unlimited.
//	 * @param format                (1.6.0+) The preferred transcoding format (e.g. "mp3", "flv") (can be null).
//	 * @param timeOffset            The offset (in seconds) at which to start streaming a video file.
//	 * @param videoSize             (1.6.0+) The size (in "WIDTHxHEIGHT" format) to request a video in (can be null).
//	 * @param estimateContentLength (1.8.0+) Whether to estimate the content size in the Content-Length HTTP header.
//	 * @throws java.net.MalformedURLException If the URL is somehow bad.
//	 * @throws java.io.UnsupportedEncodingException
//	 *
//	 */
//	static void stream(FilesystemEntry.MediaFile mediaFile, int maxBitRate, String format, int timeOffset, String videoSize, boolean estimateContentLength) throws MalformedURLException, UnsupportedEncodingException, URISyntaxException {
//		Map<String, String> params = new HashMap<String, String>(mRequiredParams);
//		params.put("id", Integer.toString(mediaFile.id));
//		if (maxBitRate > 0)
//			params.put("maxBitRate", Integer.toString(maxBitRate));
//		if (format != null)
//			params.put("format", format);
//		if (timeOffset > 0)
//			params.put("timeOffset", Integer.toString(timeOffset));
//		if (videoSize != null)
//			params.put("size", videoSize);
//		if (estimateContentLength)
//			params.put("estimateContentLength", "true");
//
//		Uri streamUri = Uri.parse(buildRestCall(mServerUrl, Methods.STREAM, params).toString());
//		Intent intent = new Intent(Intent.ACTION_VIEW);
//		Log.d(logTag, Boolean.toString(mediaFile.isVideo));
//		if (mediaFile.isVideo)
//			intent.setDataAndType(streamUri, mediaFile.transcodedContentType != null ? mediaFile.transcodedContentType : "video/*");
//		else
//			intent.setDataAndType(streamUri, mediaFile.transcodedContentType != null ? mediaFile.transcodedContentType : "audio/*");
//		Log.d(logTag, "trying to stream from " + streamUri.toString());
//		mActivity.startActivity(intent);
//	}
//
//	/**
//	 * Returns a DownloadTask for a transcoded file.
//	 *
//	 * @param mediaFile             The MediaFile to stream.
//	 * @param maxBitRate            (1.2.0+) The maximum bit rate to transcode to; 0 for unlimited.
//	 * @param format                (1.6.0+) The preferred transcoding format (e.g. "mp3", "flv") (can be null).
//	 * @param timeOffset            The offset (in seconds) at which to start streaming a video file.
//	 * @param videoSize             (1.6.0+) The size (in "WIDTHxHEIGHT" format) to request a video in (can be null).
//	 * @param estimateContentLength (1.8.0+) Whether to estimate the content size in the Content-Length HTTP header.
//	 * @throws java.net.MalformedURLException If the URL is somehow bad.
//	 * @throws java.io.UnsupportedEncodingException
//	 *
//	 */
//	static DownloadTask getTranscodedDownloadTask(final FilesystemEntry.MediaFile mediaFile, final int maxBitRate, final String format, final int timeOffset, final String videoSize, final boolean estimateContentLength) throws IOException, URISyntaxException {
//		URI downloadURL = buildRestCall(mServerUrl, Methods.STREAM, new HashMap<String, String>(mRequiredParams) {{
//			put("id", Integer.toString(mediaFile.id));
//			if (maxBitRate > 0)
//				put("maxBitRate", Integer.toString(maxBitRate));
//			if (timeOffset > 0)
//				put("timeOffset", Integer.toString(timeOffset));
//			if (videoSize != null)
//				put("videoSize", videoSize);
//			if (estimateContentLength)
//				put("estimateContentLength", "true");
//		}});
//
//		String extStorageDir = Environment.getExternalStorageDirectory().toString() + "/SubsonicClient/";
//		String filePath = extStorageDir + mediaFile.path.substring(0, mediaFile.path.lastIndexOf('.') + 1) + (mediaFile.transcodedSuffix != null ? mediaFile.transcodedSuffix : mediaFile.suffix);
//		return new DownloadTask(downloadURL, mediaFile.name, filePath);
//	}

	static String getDownloadUrl(FilesystemEntry.MediaFile mediaFile, boolean transcoded) throws MalformedURLException, URISyntaxException, UnsupportedEncodingException {
		Map<String, String> params = new HashMap<String, String>(mRequiredParams);
		params.put("id", Integer.toString(mediaFile.id));

		return buildRestCall(
				mServerUrl,
				transcoded ? Methods.STREAM : Methods.DOWNLOAD,
				params
		);
	}

//
//	/**
//	 * Returns a DownloadTask for an original (non-transcoded) file.
//	 *
//	 * @param mediaFile The MediaFile to download.
//	 * @throws java.net.MalformedURLException
//	 * @throws java.io.UnsupportedEncodingException
//	 *
//	 */
//	static DownloadTask getOriginalDownloadTask(final FilesystemEntry.MediaFile mediaFile) throws IOException, URISyntaxException {
//		URI downloadURL = buildRestCall(mServerUrl, Methods.DOWNLOAD, new HashMap<String, String>(mRequiredParams) {{
//			put("id", Integer.toString(mediaFile.id));
//		}});
//
//		String filePath = Environment.getExternalStorageDirectory().toString() + "/SubsonicClient/" + mediaFile.path;
//		return new DownloadTask(downloadURL, mediaFile.name, filePath);
//	}
}