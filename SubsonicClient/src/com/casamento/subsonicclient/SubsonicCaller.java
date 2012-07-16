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
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

class SubsonicCaller extends RESTCaller {
	private static final String API_VERSION = "1.4.0"; // 1.4.0+ required for JSON
	private static final String CLIENT_ID = "Android Subsonic Client";
	private static final String logTag = "SubsonicCaller";
	private String serverUrl;
	private Map<String, String> requiredParams;
	private Activity activity;
	private final SubsonicCaller self = this;

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
		 * [musicFolderId]:		ID of media folder (from GET_MEDIA_FOLDERS)
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
		 * id: 	A string which uniquely identifies the music folder.
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
		 * [genre]:			Only return songs within this genre.
		 * [fromYear]:		Only return songs from this year or after.
		 * [toYear]:		Only return songs from this year or before.
		 * [musicFolderId]:	Only return songs in the matching root folder.
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

	protected static interface OnExceptionListener {
		void onException(Exception e);
	}

	protected static interface OnPingResponseListener {
		void onPingResponse(final boolean ok);
	}

	protected static interface OnLicenseResponseListener extends OnExceptionListener {
		void onLicenseResponse(final License license);
	}

	protected static interface OnMediaFolderContentsResponseListener extends OnExceptionListener {
		void onMediaFolderContentsResponse(final List<FilesystemEntry> contents);
	}

	protected static interface OnFolderContentsResponseListener extends OnExceptionListener {
		void onFolderContentsResponse(final List<FilesystemEntry> contents);
	}

	protected static interface OnMediaFoldersResponseListener extends OnExceptionListener {
		void onMediaFoldersResponse(final List<MediaFolder> mediaFolders);
	}

	// TODO: it might make more sense for this to be a UI-less fragment
	protected SubsonicCaller(final String serverUrl, final String username, final String password, final Activity activity) {
		this.serverUrl = serverUrl + "/rest";
		this.activity = activity;

		this.requiredParams = new HashMap<String, String>() {{
			this.put("u", username);
			this.put("p", password);
			this.put("v", SubsonicCaller.API_VERSION);
			this.put("c", SubsonicCaller.CLIENT_ID);
			this.put("f", "json"); // XML isn't supported, for now at least
		}};
	}

	private JSONObject parseSubsonicResponse(String responseStr) throws JSONException, SubsonicException {
		JSONObject jResponse = null;
		jResponse = new JSONObject(responseStr).getJSONObject("subsonic-response");

		// handle Subsonic errors
		if (jResponse != null && jResponse.getString("status").equals("failed")) {
			JSONObject err = jResponse.getJSONObject("error");
			throw new SubsonicException(err.getInt("code"), err.getString("message"));
		}

		return jResponse;
	}

	protected void ping(final OnPingResponseListener callbackListener) throws UnsupportedEncodingException, MalformedURLException, URISyntaxException {
		SubsonicCaller.call(this.serverUrl, Methods.PING, this.requiredParams, new OnRESTResponseListener() {
			@Override
			public void onRESTResponse(String responseStr) {
				boolean ok;

				try {
					JSONObject jResponse = self.parseSubsonicResponse(responseStr);
					ok = (jResponse.getString("status").equals("ok"));
				} catch (Exception e) {
					Log.e(self.logTag, e.toString());
					ok = false;
				}

				callbackListener.onPingResponse(ok);
			}

			@Override
			public void onException(Exception e) {
				callbackListener.onPingResponse(false);
			}
		});
	}

	protected void getLicense(final OnLicenseResponseListener callbackListener) throws UnsupportedEncodingException, MalformedURLException, URISyntaxException {
		SubsonicCaller.call(this.serverUrl, Methods.GET_LICENSE, this.requiredParams, new OnRESTResponseListener() {
			@Override
			public void onRESTResponse(String responseStr) {
				try {
					JSONObject jResponse = self.parseSubsonicResponse(responseStr);
					callbackListener.onLicenseResponse(new License(jResponse.getJSONObject("license")));
				} catch (Exception e) {
					callbackListener.onException(e);
				}
			}

			@Override
			public void onException(Exception e) {
				callbackListener.onException(e);
			}
		});
	}

	protected void getMediaFolders(final OnMediaFoldersResponseListener callbackListener) throws UnsupportedEncodingException, MalformedURLException, URISyntaxException {
		SubsonicCaller.call(this.serverUrl, Methods.GET_MEDIA_FOLDERS, this.requiredParams, new OnRESTResponseListener() {
			@Override
			public void onRESTResponse(String responseStr) {
				try {
					JSONObject jResponse = self.parseSubsonicResponse(responseStr);

					List<MediaFolder> mediaFolders = new ArrayList<MediaFolder>();
					JSONArray jFolderArr = jResponse.getJSONObject("musicFolders").getJSONArray("musicFolder");

					int len = jFolderArr.length();
					for (int i = 0; i < len; i++) {
						mediaFolders.add(new MediaFolder(jFolderArr.getJSONObject(i)));
					}

					callbackListener.onMediaFoldersResponse(mediaFolders);
				} catch (Exception e) {
					callbackListener.onException(e);
				}
			}

			@Override
			public void onException(Exception e) {
				callbackListener.onException(e);
			}
		});
	}

	/**
	 * Returns an indexed structure of all artists.
	 *
	 * @param mediaFolder     ID of media folder (from GET_MEDIA_FOLDERS);
	 *                        if null, returns results from all media folders
	 * @param ifModifiedSince If specified, only return a result if the
	 *                        artist collection has changed since the given time.
	 *                        (format: java.util.Calendar.getTimeInMillis())
	 */
	protected void listMediaFolderContents(final MediaFolder mediaFolder, final Calendar ifModifiedSince, final OnMediaFolderContentsResponseListener callbackListener) throws UnsupportedEncodingException, MalformedURLException, URISyntaxException {
		Map<String, String> params = new HashMap<String, String>(this.requiredParams);
		if (mediaFolder != null)
			params.put("musicFolderId", Integer.toString(mediaFolder.id));
		if (ifModifiedSince != null)
			params.put("ifModifiedSince", Long.toString(ifModifiedSince.getTimeInMillis()));

		SubsonicCaller.call(this.serverUrl, Methods.LIST_MEDIA_FOLDER_CONTENTS, params, new OnRESTResponseListener() {
			@Override
			public void onRESTResponse(String responseStr) {
				try {
					JSONObject jResponse = self.parseSubsonicResponse(responseStr);

					List<FilesystemEntry> contents = new ArrayList<FilesystemEntry>();
					JSONObject jIndexesResponse = jResponse.getJSONObject("indexes");
					JSONArray jChildArray = jIndexesResponse.optJSONArray("child");
					JSONArray jIndexArray = jIndexesResponse.optJSONArray("index");

					if (jIndexArray != null) {
						int jIndexArrayLength = jIndexArray.length();
						for (int i = 0; i < jIndexArrayLength; i++) {
							JSONObject jIndex = jIndexArray.getJSONObject(i);

							// artist tag can be either an array or an object (if there's only one)
							JSONArray jFolderArray = jIndex.optJSONArray("artist");
							if (jFolderArray != null) {
								int jFolderArrayLength = jFolderArray.length();
								for (int j = 0; j < jFolderArrayLength; j++) {
									JSONObject jFolder = jFolderArray.getJSONObject(j);
									Folder folder = new Folder(jFolder);
									contents.add(folder);
								}
							} else {
								JSONObject jFolder = jIndex.getJSONObject("artist");
								Folder folder = new Folder(jFolder);
								contents.add(folder);
							}
						}
					}

					if (jChildArray != null) {
						int jChildArrayLength = jChildArray.length();
						for (int i = 0; i < jChildArrayLength; i++) {
							contents.add(new MediaFile(jChildArray.getJSONObject(i)));
						}
					}

					callbackListener.onMediaFolderContentsResponse(contents);
				} catch (Exception e) {
					callbackListener.onException(e);
				}
			}

			@Override
			public void onException(Exception e) {
				callbackListener.onException(e);
			}
		});
	}

	/**
	 * Returns a listing of all folders/files in a folder.
	 * Typically used to get list of albums for an artist,
	 * or list of songs for an album.
	 *
	 * @param folder The folder to retrieve the contents of.
	 */
	protected void listFolderContents(final Folder folder, final OnFolderContentsResponseListener callbackListener) throws UnsupportedEncodingException, MalformedURLException, URISyntaxException {
		Map<String, String> params = new HashMap<String, String>(this.requiredParams);
		params.put("id", Integer.toString(folder.id));

		SubsonicCaller.call(this.serverUrl, Methods.LIST_FOLDER_CONTENTS, params, new OnRESTResponseListener() {
			@Override
			public void onRESTResponse(String responseStr) {
				try {
					JSONObject jResponse = self.parseSubsonicResponse(responseStr);

					Folder parentFolder = folder;
					List<FilesystemEntry> contents = new ArrayList<FilesystemEntry>();
					JSONObject jDirectoryContents = jResponse.getJSONObject("directory");

					JSONArray jChildArray = jDirectoryContents.optJSONArray("child");
					if (jChildArray != null) {
						int jChildArrayLength = jChildArray.length();
						for (int i = 0; i < jChildArrayLength; i++) {
							JSONObject jChild = jChildArray.getJSONObject(i);

							FilesystemEntry entry;
							if (jChild.getBoolean("isDir")) {
								entry = new Folder(jChild);
							} else {
								entry = new MediaFile(jChild);
							}
							entry.parent = parentFolder;
							contents.add(entry);
						}
					} else {
						JSONObject jChild = jDirectoryContents.optJSONObject("child");

						if (jChild != null) {
							FilesystemEntry entry;
							if (jChild.getBoolean("isDir")) {
								entry = new Folder(jChild);
							} else {
								entry = new MediaFile(jChild);
							}
							entry.parent = parentFolder;
							contents.add(entry);
						}
					}

					callbackListener.onFolderContentsResponse(contents);
				} catch (Exception e) {
					callbackListener.onException(e);
				}
			}

			@Override
			public void onException(Exception e) {
				callbackListener.onException(e);
			}
		});
	}

	/**
	 * Streams a given media file to a capable external app via an Intent.
	 *
	 * @param mediaFile             The MediaFile to stream.
	 * @param maxBitRate            (1.2.0+) The maximum bit rate to transcode to; 0 for unlimited.
	 * @param format                (1.6.0+) The preferred transcoding format (e.g. "mp3", "flv") (can be null).
	 * @param timeOffset            The offset (in seconds) at which to start streaming a video file.
	 * @param videoSize             (1.6.0+) The size (in "WIDTHxHEIGHT" format) to request a video in (can be null).
	 * @param estimateContentLength (1.8.0+) Whether to estimate the content size in the Content-Length HTTP header.
	 * @throws java.net.MalformedURLException If the URL is somehow bad.
	 * @throws java.io.UnsupportedEncodingException
	 *
	 */
	protected void stream(MediaFile mediaFile, int maxBitRate, String format, int timeOffset, String videoSize, boolean estimateContentLength) throws MalformedURLException, UnsupportedEncodingException, URISyntaxException {
		Map<String, String> params = new HashMap<String, String>(this.requiredParams);
		params.put("id", Integer.toString(mediaFile.id));
		if (maxBitRate > 0)
			params.put("maxBitRate", Integer.toString(maxBitRate));
		if (format != null)
			params.put("format", format);
		if (timeOffset > 0)
			params.put("timeOffset", Integer.toString(timeOffset));
		if (videoSize != null)
			params.put("size", videoSize);
		if (estimateContentLength)
			params.put("estimateContentLength", "true");

		Uri streamUri = Uri.parse(this.buildRESTCallURI(this.serverUrl, Methods.STREAM, params).toString());
		Intent intent = new Intent(Intent.ACTION_VIEW);
		Log.d(this.logTag, Boolean.toString(mediaFile.isVideo));
		if (mediaFile.isVideo)
			intent.setDataAndType(streamUri, mediaFile.transcodedContentType != null ? mediaFile.transcodedContentType : "video/*");
		else
			intent.setDataAndType(streamUri, mediaFile.transcodedContentType != null ? mediaFile.transcodedContentType : "audio/*");
		Log.d(this.logTag, "trying to stream from " + streamUri.toString());
		this.activity.startActivity(intent);
	}

	/**
	 * Returns a DownloadTask for a transcoded file.
	 *
	 * @param mediaFile             The MediaFile to stream.
	 * @param maxBitRate            (1.2.0+) The maximum bit rate to transcode to; 0 for unlimited.
	 * @param format                (1.6.0+) The preferred transcoding format (e.g. "mp3", "flv") (can be null).
	 * @param timeOffset            The offset (in seconds) at which to start streaming a video file.
	 * @param videoSize             (1.6.0+) The size (in "WIDTHxHEIGHT" format) to request a video in (can be null).
	 * @param estimateContentLength (1.8.0+) Whether to estimate the content size in the Content-Length HTTP header.
	 * @throws java.net.MalformedURLException If the URL is somehow bad.
	 * @throws java.io.UnsupportedEncodingException
	 *
	 */
	public DownloadTask getTranscodedDownloadTask(final MediaFile mediaFile, final int maxBitRate, final String format, final int timeOffset, final String videoSize, final boolean estimateContentLength) throws IOException, URISyntaxException {
		URI downloadURL = this.buildRESTCallURI(this.serverUrl, Methods.STREAM, new HashMap<String, String>(this.requiredParams) {{
			this.put("id", Integer.toString(mediaFile.id));
			if (maxBitRate > 0)
				this.put("maxBitRate", Integer.toString(maxBitRate));
			if (timeOffset > 0)
				this.put("timeOffset", Integer.toString(timeOffset));
			if (videoSize != null)
				this.put("videoSize", videoSize);
			if (estimateContentLength)
				this.put("estimateContentLength", "true");
		}});

		String extStorageDir = Environment.getExternalStorageDirectory().toString() + "/SubsonicClient/";
		String filePath = extStorageDir + mediaFile.path.substring(0, mediaFile.path.lastIndexOf('.') + 1) + (mediaFile.transcodedSuffix != null ? mediaFile.transcodedSuffix : mediaFile.suffix);
		return new DownloadTask(downloadURL, mediaFile.name, filePath);
	}

	/**
	 * Returns a DownloadTask for an original (non-transcoded) file.
	 *
	 * @param mediaFile The MediaFile to download.
	 * @throws java.net.MalformedURLException
	 * @throws java.io.UnsupportedEncodingException
	 *
	 */
	public DownloadTask getOriginalDownloadTask(final MediaFile mediaFile) throws IOException, UnsupportedEncodingException, URISyntaxException {
		URI downloadURL = this.buildRESTCallURI(this.serverUrl, Methods.DOWNLOAD, new HashMap<String, String>(this.requiredParams) {{
			this.put("id", Integer.toString(mediaFile.id));
		}});

		String filePath = Environment.getExternalStorageDirectory().toString() + "/SubsonicClient/" + mediaFile.path;
		return new DownloadTask(downloadURL, mediaFile.name, filePath);
	}
}