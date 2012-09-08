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

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static com.casamento.subsonicclient.SubsonicCaller.DatabaseHelper.getInstance;

class SubsonicCaller extends RestCaller {
    private static final String API_VERSION = "1.4.0"; // 1.4.0+ required for JSON
    private static final String CLIENT_ID = "Android Subsonic Client";
    private static final String logTag = "SubsonicCaller";
    private static String mServerUrl, mUsername, mPassword;
    private static Map<String, String> mRequiredParams;
    private static DatabaseHelper mDatabaseHelper;

    static String getUsername() {
        return mUsername;
    }

    static String getPassword() {
        return mPassword;
    }

    private interface Methods {
        // documentation is mostly taken from http://www.subsonic.org/pages/api.jsp
        // "param: description" is a required parameter, "[param]: description" is optional
        // example outputs can be found at <server>/xsd


        // SYSTEM METHODS:

        /**
         * Get details about the software license.
         */
        String GET_LICENSE = "getLicense.view";

        /**
         * Used to test connectivity with the server.
         */
        String PING = "ping.view";


        // NAVIGATION METHODS:

        /**
         * Returns all configured top-level media folders (confusingly termed "music
         * folders" in the API, but called "media folders" in the Subsonic application
         * itself).
         */
        String GET_MEDIA_FOLDERS = "getMusicFolders.view";

        /**
         * Returns an indexed structure of all artists.
         * <p/>
         * [musicFolderId]:		ID of media mFolder (from GET_MEDIA_FOLDERS)
         * [ifModifiedSince]: 	If specified, only return a result if the
         * artist collection has changed since the given time.
         * format: java.util.Calendar.getTimeInMillis()
         */
        String LIST_MEDIA_FOLDER_CONTENTS = "getIndexes.view";

        /**
         * Returns a listing of all files in a music directory.
         * Typically used to get list of albums for an artist,
         * or list of songs for an album.
         * <p/>
         * id: 	A string which uniquely identifies the music mFolder.
         * Obtained by calls to getIndexes or getMusicDirectory.
         */
        String LIST_FOLDER_CONTENTS = "getMusicDirectory.view";

        /**
         * (1.8.0+)
         * Similar to LIST_MEDIA_FOLDER_CONTENTS, but organizes music
         * according to ID3 tags.
         */
        String LIST_ARTISTS = "getArtists.view";

        /**
         * (1.8.0+)
         * Returns details for an artist, including a list of albums.
         * This method organizes music according to ID3 tags.
         * <p/>
         * id: ID of artist (e.g. from LIST_ARTISTS)
         */
        String GET_ARTIST_DETAILS = "getArtist.view";

        /**
         * (1.8.0+)
         * Returns details for an album, including a list of songs.
         * This method organizes music according to ID3 tags.
         * <p/>
         * id: ID of album (e.g. from GET_ARTIST_DETAILS or LIST_ALBUMS)
         */
        String GET_ALBUM_DETAILS = "getAlbum.view";

        /**
         * (1.8.0+)
         * Returns details for a song.
         * <p/>
         * id: ID of a song (e.g. from GET_RANDOM_SONGS)
         */
        String GET_SONG_DETAILS = "getSong.view";

        /**
         * (1.8.0+)
         * Returns all video files.
         */
        String LIST_VIDEOS = "getVideos.view";

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
        String LIST_ALBUMS = "getAlbumList.view";

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
        String LIST_ALBUMS_ID3 = "getAlbumList2.view";

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
        String GET_RANDOM_SONGS = "getRandomSongs.view";

        String GET_NOW_PLAYING = "getNowPlaying.view";
        String GET_STARRED_ITEMS = "getStarred.view";
        String GET_STARRED_ITEMS_ID3 = "getStarred2.view";


        // SEARCHING

        String OLD_SEARCH = "search.view";
        String SEARCH = "search2.view";
        String SEARCH_ID3 = "search3.view";


        // PLAYLISTS

        String LIST_PLAYLISTS = "getPlaylists.view";
        String GET_PLAYLIST_DETAILS = "getPlaylist.view";
        String CREATE_PLAYLIST = "createPlaylist.view";
        String UPDATE_PLAYLIST = "updatePlaylist.view";
        String DELETE_PLAYLIST = "deletePlaylist.view";


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
        String STREAM = "stream.view";
        String DOWNLOAD = "download.view";
        String GET_COVER_ART = "getCoverArt.view";
        String GET_LYRICS = "getLyrics.view";
        String GET_USER_AVATAR = "getAvatar.view";


        // MEDIA ANNOTATION

        String STAR = "star.view";
        String UNSTAR = "unstar.view";
        String SET_RATING = "setRating.view";
        String SCROBBLE = "scrobble.view";


        // SHARING

        String GET_SHARED_ITEMS = "getShares.view";
        String CREATE_SHARED_ITEM = "createShare.view";
        String UPDATE_SHARED_ITEM = "updateShare.view";
        String DELETE_SHARED_ITEM = "deleteShare.view";


        // PODCASTS

        String LIST_PODCASTS = "getPodcasts.view";


        // JUKEBOX

        String JUKEBOX_CONTROL = "jukeboxControl.view";


        // CHAT

        String LIST_CHAT_MESSAGES = "getChatMessages.view";
        String POST_CHAT_MESSAGE = "addChatMessage.view";


        // USER MANAGEMENT

        String GET_USER_DETAILS = "getUser.view";
        String CREATE_USER = "createUser.view";
        String DELETE_USER = "deleteUser.view";
        String CHANGE_USER_PASSWORD = "changePassword.view";
    }

    protected interface PingResponseListener {
        void onPingResponse(final boolean ok);
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

    static FilesystemEntry getFilesystemEntry(final int id) {
        final Cursor cursor = mDatabaseHelper.getFilesystemEntry(id);
        cursor.moveToFirst();
        return FilesystemEntry.getInstance(cursor);
    }

    static DatabaseHelper getDatabaseHelper() {
        return mDatabaseHelper;
    }

    static void setServerDetails(final String serverUrl, final String username, final String password,
            final Activity activity) {
        mServerUrl = serverUrl + "/rest";
        mUsername = username;
        mPassword = password;

        mDatabaseHelper = getInstance(activity);

        mRequiredParams = new HashMap<String, String>();
        mRequiredParams.put("v", API_VERSION);
        mRequiredParams.put("c", CLIENT_ID);
        mRequiredParams.put("f", "json");
    }

    private static JSONObject parseSubsonicResponse(final CharSequence responseStr) throws JSONException, SubsonicException {
        final JSONObject jResponse = new JSONObject((String) responseStr).getJSONObject("subsonic-response");

        // handle Subsonic errors
        if (jResponse != null && jResponse.getString("status").equals("failed")) {
            final JSONObject err = jResponse.getJSONObject("error");
            throw new SubsonicException(err.getInt("code"), err.getString("message"));
        }

        return jResponse;
    }

    static void ping(final PingResponseListener callbackListener) throws UnsupportedEncodingException, MalformedURLException, URISyntaxException {
        final CharSequence restCall = buildRestCall(mServerUrl, Methods.PING, mRequiredParams);

        new RestResponseTask(restCall, mUsername, mPassword, new RestResponseTask.Adapter() {
            @Override
            public void onResult(final CharSequence responseStr) {
                boolean ok;

                try {
                    final JSONObject jResponse = parseSubsonicResponse(responseStr);
                    ok = (jResponse.getString("status").equals("ok"));
                } catch (final Exception e) {
                    Log.e(logTag, e.toString());
                    ok = false;
                }

                callbackListener.onPingResponse(ok);
            }

            @Override
            public void onException(final Exception e) {
                callbackListener.onPingResponse(false);
            }
        }).execute((Void) null);
    }

    interface AsyncTaskListener<Progress, Result> {
        void onPreExecute();
        void onProgressUpdate(Progress... p);
        void onResult(Result r);
        void onException(Exception e);
    }

    static class AsyncTaskAdapter<Progress, Result> implements AsyncTaskListener<Progress, Result> {
        @Override public void onPreExecute()                        { /* do nothing */ }
        @Override public void onProgressUpdate(final Progress... p) { /* do nothing */ }
        @Override public void onResult(final Result r)              { /* do nothing */ }
        @Override public void onException(final Exception e)        { /* do nothing */ }
    }

    static class RestResponseTask extends DataRetrievalTask<Void, Void, CharSequence> {
        private final CharSequence mUrl, mUsername, mPassword;
        private Listener mCallbackListener;

        interface Listener extends AsyncTaskListener<Void, CharSequence> {}
        static class Adapter extends AsyncTaskAdapter<Void, CharSequence> implements Listener {}

        RestResponseTask(final CharSequence url, final CharSequence username, final CharSequence password,
                 final Listener callbackListener) {
            mUrl = url;
            mUsername = username;
            mPassword = password;
            mCallbackListener = callbackListener;
        }

        @Override
        protected CharSequence doInBackground(final Void... nothing) {
            try {
                return readAll(getStream(mUrl, mUsername, mPassword));
            } catch (final Exception e) {
                mCallbackListener.onException(e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(final CharSequence response) {
            if (response != null)
                mCallbackListener.onResult(response);
        }
    }

    static class CursorTask extends DataRetrievalTask<Void, Integer, Cursor> {
        private static final String logTag = "CursorTask";
        private final Folder mFolder;
        private final Listener mCallbackListener;
        private int entriesInserted = 0;

        interface Listener extends AsyncTaskListener<Integer, Cursor> {}
        static class Adapter extends AsyncTaskAdapter<Integer, Cursor> implements Listener {}

        // if folder is null, the top-level folders are retrieved
        CursorTask(final Folder folder, final Listener callbackListener) {
            mFolder = folder;
            mCallbackListener = callbackListener;
        }

        private void insertJSONObject(final JSONObject jEntry, final int parentId) throws JSONException {
            final FilesystemEntry entry = FilesystemEntry.getInstance(jEntry);
            entry.parentId = parentId;
            mDatabaseHelper.insert(entry.getContentValues());
            publishProgress(++entriesInserted);
        }

        private void insertJSONObject(final JSONObject jEntry, final Boolean isFolder, final int parentId) throws JSONException {
            final FilesystemEntry entry = FilesystemEntry.getInstance(jEntry, isFolder);
            entry.parentId = parentId;
            mDatabaseHelper.insert(entry.getContentValues());
            publishProgress(++entriesInserted);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mCallbackListener.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(final Integer... foldersProcessed) {
            super.onProgressUpdate(foldersProcessed);
            mCallbackListener.onProgressUpdate(foldersProcessed);
        }

        // grabs the list of top level folders from the server and puts them in the database
        private void insertTopLevelFoldersIntoDatabase() throws IOException, AuthenticationException, SubsonicException, JSONException, UnsupportedEncodingException {
            final CharSequence callUrl = buildRestCall(mServerUrl, Methods.GET_MEDIA_FOLDERS, mRequiredParams);
            final CharSequence response = readAll(getStream(callUrl, mUsername, mPassword));

            if (isCancelled()) return;

            final JSONObject jResponse = parseSubsonicResponse(response);

            final JSONArray jFolderArray = jResponse.getJSONObject("musicFolders").getJSONArray("musicFolder");
            final int jFolderArrayLength = jFolderArray.length();
            for (int i = 0; i < jFolderArrayLength; i++) {
                final JSONObject jFolder = jFolderArray.getJSONObject(i);
                final Folder folder = new Folder(-jFolder.getInt("id"),
                        jFolder.getString("name"), mFolder != null ? mFolder.id : Folder.NULL_ID);
                mDatabaseHelper.insert(folder.getContentValues());
                publishProgress(++entriesInserted);
            }
        }

        private void insertFolderContentsIntoDatabase(final Folder f) throws IOException, AuthenticationException, SubsonicException, JSONException, UnsupportedEncodingException {
            final Map<String, String> params = new HashMap<String, String>(mRequiredParams);
            if (mFolder.isTopLevel)
                params.put("musicFolderId", Integer.toString(-mFolder.id));
            else
                params.put("id", Integer.toString(mFolder.id));

            final CharSequence callUrl = mFolder.isTopLevel ?
                    buildRestCall(mServerUrl, Methods.LIST_MEDIA_FOLDER_CONTENTS, params) :
                    buildRestCall(mServerUrl, Methods.LIST_FOLDER_CONTENTS, params);

            final CharSequence response = readAll(getStream(callUrl, mUsername, mPassword));

            if (isCancelled()) return;

            final JSONObject jResponse = parseSubsonicResponse(response);

            JSONObject jIndexesResponse = jResponse.optJSONObject("indexes");
            if (jIndexesResponse == null) jIndexesResponse = jResponse.getJSONObject("directory");

            final JSONArray jChildArray = jIndexesResponse.optJSONArray("child");
            if (jChildArray == null) {
                final JSONObject jChild = jIndexesResponse.optJSONObject("child");
                if (jChild != null)
                    insertJSONObject(jChild, mFolder.id);
            } else {
                final int jChildArrayLength = jChildArray.length();
                for (int i = 0; i < jChildArrayLength; i++)
                    insertJSONObject(jChildArray.getJSONObject(i), mFolder.id);
            }

            final JSONArray jIndexArray = jIndexesResponse.optJSONArray("index");
            if (jIndexArray != null) {
                final int jIndexArrayLength = jIndexArray.length();
                Log.d(logTag, "getting " + Integer.toString(jIndexArrayLength) + " items");

                for (int i = 0; i < jIndexArrayLength; i++) {
                    final JSONObject jIndex = jIndexArray.getJSONObject(i);

                    // artist tag can be either an array or an object (if there's only one)
                    final JSONArray jFolderArray = jIndex.optJSONArray("artist");
                    if (jFolderArray != null) {
                        final int jFolderArrayLength = jFolderArray.length();
                        for (int j = 0; j < jFolderArrayLength; j++)
                            insertJSONObject(jFolderArray.getJSONObject(j), true, mFolder.id);
                    } else
                        insertJSONObject(jIndex.getJSONObject("artist"), true, mFolder.id);
                }
            }
        }

        private Cursor getTopLevelCursor() {
            final Cursor c = mDatabaseHelper.getTopLevelCursor();
            if (c != null && c.getCount() > 0)
                return c;

            try {
                insertTopLevelFoldersIntoDatabase();
                if (isCancelled()) return null;

                return mDatabaseHelper.getTopLevelCursor();
            } catch (final Exception e) {
                mCallbackListener.onException(e);
                return null;
            }
        }

        private Cursor getFolderContentsCursor(final Folder f) {
            // check if there's already data for the media mFolder in the database
            final Cursor cursor = mDatabaseHelper.getFolderContentsCursor(mFolder);
            if (cursor != null && cursor.getCount() > 0)
                return cursor;

            // otherwise, get the data from the server and insert it in the database, then return a new cursor
            try {
                insertFolderContentsIntoDatabase(mFolder);
                if (isCancelled()) return null;

                return mDatabaseHelper.getFolderContentsCursor(mFolder);
            } catch (final Exception e) {
                mCallbackListener.onException(e);
                return null;
            }
        }

        @Override
        protected Cursor doInBackground(final Void... nothing) {
            // if mFolder is null, get the list of top-level folders
            if (mFolder == null) return getTopLevelCursor();

            // otherwise, get the contents of the folder that was passed
            return getFolderContentsCursor(mFolder);
        }

        @Override
        protected void onPostExecute(final Cursor cursor) {
            mCallbackListener.onResult(cursor);
        }
    }

    static String getDownloadUrl(final MediaFile mediaFile, final boolean transcoded) {
        final Map<String, String> params = new HashMap<String, String>(mRequiredParams);
        params.put("id", Integer.toString(mediaFile.id));

        try {
            return buildRestCall(mServerUrl, transcoded ? Methods.STREAM : Methods.DOWNLOAD, params);
        } catch (UnsupportedEncodingException e) {
            // this shouldn't ever happen, Android always supports UTF-8
            return null;
        }
    }
}