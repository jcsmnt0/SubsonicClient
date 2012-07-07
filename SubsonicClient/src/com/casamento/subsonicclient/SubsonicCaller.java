package com.casamento.subsonicclient;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class SubsonicCaller {
	static final class SubsonicMethods {
		// documentation is mostly taken from http://www.subsonic.org/pages/api.jsp
		// "param: description" is a required parameter, "[param]: description" is optional
		// example outputs can be found at <server>/xsd
		
		
		// SYSTEM METHODS:
		
		/**
		 * Get details about the software license.
		 */
		protected static final String GET_LICENSE = "getLicense.view";
		
		/**
		 * Used to test connectivity with the server.
		 */
		protected static final String PING = "ping.view";
		
		
		// NAVIGATION METHODS:
		
		/**
		 * Returns all configured top-level media folders (confusingly termed "music
		 * folders" in the API, but called "media folders" in the Subsonic application
		 * itself).
		 */
		protected static final String GET_MEDIA_FOLDERS = "getMusicFolders.view";
		
		/**
		 * Returns an indexed structure of all artists.
		 * 
		 * [musicFolderId]:		ID of media folder (from GET_MEDIA_FOLDERS)
		 * [ifModifiedSince]: 	If specified, only return a result if the
		 * 						artist collection has changed since the given time.
		 * 						format: java.util.Calendar.getTimeInMillis()
		 */
		protected static final String LIST_MEDIA_FOLDER_CONTENTS = "getIndexes.view";
		
		/**
		 * Returns a listing of all files in a music directory.
		 * Typically used to get list of albums for an artist,
		 * or list of songs for an album.
		 * 
		 * id: 	A string which uniquely identifies the music folder.
		 * 		Obtained by calls to getIndexes or getMusicDirectory.
		 */
		protected static final String LIST_FOLDER_CONTENTS = "getMusicDirectory.view";
		
		/**
		 * (1.8.0+)
		 * Similar to LIST_MEDIA_FOLDER_CONTENTS, but organizes music
		 * according to ID3 tags.
		 */
		protected static final String LIST_ARTISTS = "getArtists.view";
		
		/**
		 * (1.8.0+)
		 * Returns details for an artist, including a list of albums.
		 * This method organizes music according to ID3 tags.
		 * 
		 * id: ID of artist (e.g. from LIST_ARTISTS)
		 */
		protected static final String GET_ARTIST_DETAILS = "getArtist.view";
		
		/**
		 * (1.8.0+)
		 * Returns details for an album, including a list of songs.
		 * This method organizes music according to ID3 tags.
		 * 
		 * id: ID of album (e.g. from GET_ARTIST_DETAILS or LIST_ALBUMS)
		 */
		protected static final String GET_ALBUM_DETAILS = "getAlbum.view";
		
		/**
		 * (1.8.0+)
		 * Returns details for a song.
		 * 
		 * id: ID of a song (e.g. from GET_RANDOM_SONGS)
		 */
		protected static final String GET_SONG_DETAILS = "getSong.view";
		
		/**
		 * (1.8.0+)
		 * Returns all video files.
		 */
		protected static final String LIST_VIDEOS = "getVideos.view";
		
		// ALBUM & SONG LISTS
		
		/**
		 * (1.2.0+)
		 * Returns a list of random, newest, highest rated etc. albums.
		 * Similar to the album lists on the home page of the Subsonic web interface.
		 * 
		 * type:		The list type. Must be one of the following: random, newest,
		 * 				highest, frequent, recent, alphabeticalByName (1.8.0+),
		 * 				alphabeticalByArtist (1.8.0+), starred (1.8.0+)
		 * [size]:		The number of albums to return. Default 10, max 500.
		 * [offset]:	The list offset, for paging.
		 */
		protected static final String LIST_ALBUMS = "getAlbumList.view";
		
		/**
		 * (1.8.0+)
		 * Similar to LIST_ALBUMS, but organizes music according to ID3 tags.
		 * 
		 * type:		The list type. Must be one of the following: random, newest,
		 * 				highest, frequent, recent, alphabeticalByName (1.8.0+),
		 * 				alphabeticalByArtist (1.8.0+), starred (1.8.0+)
		 * [size]:		The number of albums to return. Default 10, max 500.
		 * [offset]:	The list offset, for paging.
		 */
		protected static final String LIST_ALBUMS_ID3 = "getAlbumList2.view";
		
		/**
		 * (1.2.0+)
		 * Returns random songs matching the given criteria.
		 * 
		 * [size]:			Songs to return. Default 10, max 500.
		 * [genre]:			Only return songs within this genre.
		 * [fromYear]:		Only return songs from this year or after.
		 * [toYear]:		Only return songs from this year or before.
		 * [musicFolderId]:	Only return songs in the matching root folder.
		 */
		protected static final String GET_RANDOM_SONGS = "getRandomSongs.view";
		
		protected static final String GET_NOW_PLAYING = "getNowPlaying.view";
		protected static final String GET_STARRED_ITEMS = "getStarred.view";
		protected static final String GET_STARRED_ITEMS_ID3 = "getStarred2.view";
		
		
		// SEARCHING
		
		protected static final String OLD_SEARCH = "search.view";
		protected static final String SEARCH = "search2.view";
		protected static final String SEARCH_ID3 = "search3.view";
		
		
		// PLAYLISTS
		
		protected static final String LIST_PLAYLISTS = "getPlaylists.view";
		protected static final String GET_PLAYLIST_DETAILS = "getPlaylist.view";
		protected static final String CREATE_PLAYLIST = "createPlaylist.view";
		protected static final String UPDATE_PLAYLIST = "updatePlaylist.view";
		protected static final String DELETE_PLAYLIST = "deletePlaylist.view";
		
		
		// MEDIA RETRIEVAL
		
		/**
		 * Streams a given media file.
		 * Returns binary data on success, REST response on error.
		 * 
		 * id:						id of the file to stream
		 * [maxBitRate]:			(1.2.0+) 0 symbolizes no limit
		 * [format]:				(1.6.0+) preferred transcoding format (e.g. "mp3", "flv")
		 * [timeOffset]:			start streaming at given offset seconds into video
		 * [size]:					(1.6.0+) request video size as "WxH"
		 * [estimateContentLength]:	(1.8.0+) estimated size for transcoded media in Content-Length header
		 */
		protected static final String STREAM = "stream.view";
		protected static final String DOWNLOAD = "download.view";
		protected static final String GET_COVER_ART = "getCoverArt.view";
		protected static final String GET_LYRICS = "getLyrics.view";
		protected static final String GET_USER_AVATAR = "getAvatar.view";
		
		
		// MEDIA ANNOTATION
		
		protected static final String STAR = "star.view";
		protected static final String UNSTAR = "unstar.view";
		protected static final String SET_RATING = "setRating.view";
		protected static final String SCROBBLE = "scrobble.view";
		
		
		// SHARING
		
		protected static final String GET_SHARED_ITEMS = "getShares.view";
		protected static final String CREATE_SHARED_ITEM = "createShare.view";
		protected static final String UPDATE_SHARED_ITEM = "updateShare.view";
		protected static final String DELETE_SHARED_ITEM = "deleteShare.view";
		
		
		// PODCASTS
		
		protected static final String LIST_PODCASTS = "getPodcasts.view";
		
		
		// JUKEBOX
		
		protected static final String JUKEBOX_CONTROL = "jukeboxControl.view";
		
		
		// CHAT
		
		protected static final String LIST_CHAT_MESSAGES = "getChatMessages.view";
		protected static final String POST_CHAT_MESSAGE = "addChatMessage.view";
		
		
		// USER MANAGEMENT
		
		protected static final String GET_USER_DETAILS = "getUser.view";
		protected static final String CREATE_USER = "createUser.view";
		protected static final String DELETE_USER = "deleteUser.view";
		protected static final String CHANGE_USER_PASSWORD = "changePassword.view";
	}

	private final String logTag = "SubsonicCaller";
	private String url;
	private Map<String, String> requiredParams;
	private Context context;
	protected SubsonicCaller(final String url, final String username, final String password, final String version, final String clientId, final Context context) {
		this.url = url + "/rest";
		this.context = context;
		
		this.requiredParams = new HashMap<String, String>();
		this.requiredParams.put("u", username);
		this.requiredParams.put("p", password);
		this.requiredParams.put("v", version);
		this.requiredParams.put("c", clientId);
		this.requiredParams.put("f", "json");
	}
	
	protected void callMethod(String method, OnRESTResponseListener callbackListener) {
		new RESTTask(this.url, method, requiredParams, callbackListener).execute();
	}
	protected void callMethod(String method, ProgressDialog dialog, OnRESTResponseListener callbackListener) {
		new RESTTask(this.url, method, requiredParams, dialog, callbackListener).execute();
	}
	protected void callMethod(String method, Map<String, String> params, OnRESTResponseListener callbackListener) {
		params.putAll(this.requiredParams);
		new RESTTask(this.url, method, params, callbackListener).execute();
	}
	protected void callMethod(String method, ProgressDialog dialog, Map<String, String> params, OnRESTResponseListener callbackListener) {
		params.putAll(this.requiredParams);
		
		// dialog is passed to the RESTTask so it can update the progress bar, if there is one
		new RESTTask(this.url, method, params, dialog, callbackListener).execute();
	}

	
	/**
	 * Tests connectivity with the server.
	 */
	protected void ping(final OnPingResponseListener callbackListener) {
		callMethod(SubsonicMethods.PING, Util.createIndeterminateProgressDialog(this.context, "Checking connection to server..."), new OnRESTResponseListener() {
			public void onRESTResponse(String responseStr) {
				boolean ok;
				
				try {
					JSONObject jResponse = parseSubsonicResponse(responseStr);
					ok = (jResponse.getString("status").equals("ok"));
				} catch (JSONException e) {
					Log.d(logTag, e.toString());
					ok = false;
				} catch (SubsonicException e) {
					Log.d(logTag, e.toString());
					ok = false;
				} catch (NullPointerException e) {
					Log.d(logTag, e.toString());
					ok = false;
				}
				
				callbackListener.onPingResponse(ok);
			}
		});
	}
	
	/**
	 * Gets details about the software license.
	 */
	protected void getLicense(final OnLicenseResponseListener callbackListener) {
		callMethod(SubsonicMethods.GET_LICENSE, Util.createIndeterminateProgressDialog(this.context, "Retrieving license details..."), new OnRESTResponseListener() {
			public void onRESTResponse(String responseStr) {
				try {
					JSONObject jResponse = parseSubsonicResponse(responseStr);
					callbackListener.onLicenseResponse(new License(jResponse.getJSONObject("license")));
				} catch (Exception e) {
					callbackListener.onException(e);
				}
			}
		});
	}
	
	/**
	 * Returns all configured top-level media folders (confusingly termed "music folders" in the API, but called "media folders" in the Subsonic application itself).
	 */
	protected void getMediaFolders(final OnMediaFoldersResponseListener callbackListener) {
		callMethod(SubsonicMethods.GET_MEDIA_FOLDERS, Util.createIndeterminateProgressDialog(this.context, "Loading media folder list..."), new OnRESTResponseListener() {
			public void onRESTResponse(String responseStr) {
				try {
					JSONObject jResponse = parseSubsonicResponse(responseStr);
					
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
		});
	}
	
	/**
	 * Returns an indexed structure of all artists.
	 * 
	 * @param mediaFolder		ID of media folder (from GET_MEDIA_FOLDERS);
	 * 							if null, returns results from all media folders
	 * @param ifModifiedSince 	If specified, only return a result if the
	 * 							artist collection has changed since the given time.
	 * 							(format: java.util.Calendar.getTimeInMillis())
	 */
	protected void listMediaFolderContents(final MediaFolder mediaFolder, final Calendar ifModifiedSince, final OnMediaFolderContentsResponseListener callbackListener) {
		Map<String, String> params = new HashMap<String, String>();
		if (mediaFolder != null)
			params.put("musicFolderId", Integer.toString(mediaFolder.id));
		if (ifModifiedSince != null)
			params.put("ifModifiedSince", Long.toString(ifModifiedSince.getTimeInMillis()));
		
		callMethod(SubsonicMethods.LIST_MEDIA_FOLDER_CONTENTS, Util.createIndeterminateProgressDialog(this.context, "Loading folder contents..."), params, new OnRESTResponseListener() {
			public void onRESTResponse(String responseStr) {
				try {
					JSONObject jResponse = parseSubsonicResponse(responseStr);
					
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
		});
	}
	
	/**
	 * Returns a listing of all folders/files in a folder.
	 * Typically used to get list of albums for an artist,
	 * or list of songs for an album.
	 * 
	 * @param folder	The folder to retrieve the contents of.
	 */
	protected void listFolderContents(final Folder folder, final OnFolderContentsResponseListener callbackListener) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("id", Integer.toString(folder.id));
		
		callMethod(SubsonicMethods.LIST_FOLDER_CONTENTS, Util.createIndeterminateProgressDialog(this.context, "Loading folder contents..."), params, new OnRESTResponseListener() {
			public void onRESTResponse(String responseStr) {
				try {
					JSONObject jResponse = parseSubsonicResponse(responseStr);
					
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
		});
	}
	
	/**
	 * Streams a given media file to a capable external app via an Intent.
	 *  
	 * @param mediaFile						The MediaFile to stream.
	 * @param maxBitRate				(1.2.0+) The maximum bit rate to transcode to; 0 for unlimited.
	 * @param format					(1.6.0+) The preferred transcoding format (e.g. "mp3", "flv") (can be null).
	 * @param timeOffset				The offset (in seconds) at which to start streaming a video file.
	 * @param videoSize					(1.6.0+) The size (in "WIDTHxHEIGHT" format) to request a video in (can be null).
	 * @param estimateContentLength		(1.8.0+) Whether to estimate the content size in the Content-Length HTTP header.
	 * @throws MalformedURLException	If the URL is somehow bad.
	 * @throws UnsupportedEncodingException 
	 */
	protected void stream(MediaFile mediaFile, int maxBitRate, String format, int timeOffset, String videoSize, boolean estimateContentLength) throws MalformedURLException, ActivityNotFoundException, UnsupportedEncodingException {
		Map<String, String> params = new HashMap<String, String>();
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
		
		params.putAll(requiredParams);

		Uri streamUri = Uri.parse(RESTTask.buildRESTCallURL(this.url, SubsonicMethods.STREAM, params).toString());
		Intent intent = new Intent(Intent.ACTION_VIEW);
		Log.d(logTag, Boolean.toString(mediaFile.isVideo));
		if (mediaFile.isVideo)
			intent.setDataAndType(streamUri, mediaFile.transcodedContentType != null ? mediaFile.transcodedContentType : "video/*");
		else
			intent.setDataAndType(streamUri, mediaFile.transcodedContentType != null ? mediaFile.transcodedContentType : "audio/*");
		Log.d(logTag, "trying to stream from " + streamUri.toString());
		this.context.startActivity(intent);
	}

	/**
	 * Begins downloading a transcoded file from the server. 
	 * 
	 * @param mediaFile					The MediaFile to stream.
	 * @param maxBitRate				(1.2.0+) The maximum bit rate to transcode to; 0 for unlimited.
	 * @param format					(1.6.0+) The preferred transcoding format (e.g. "mp3", "flv") (can be null).
	 * @param timeOffset				The offset (in seconds) at which to start streaming a video file.
	 * @param videoSize					(1.6.0+) The size (in "WIDTHxHEIGHT" format) to request a video in (can be null).
	 * @param estimateContentLength		(1.8.0+) Whether to estimate the content size in the Content-Length HTTP header.
	 * @throws MalformedURLException	If the URL is somehow bad.
	 * @throws UnsupportedEncodingException 
	 */
	protected void downloadTranscoded(MediaFile mediaFile, int maxBitRate, String format, int timeOffset, String videoSize, boolean estimateContentLength) throws MalformedURLException, UnsupportedEncodingException {
		Map<String, String> params = new HashMap<String, String>();
		params.putAll(requiredParams);
		params.put("id", Integer.toString(mediaFile.id));
		
		ProgressDialog progressDialog = new ProgressDialog(this.context);
		progressDialog.setMessage("Downloading " + mediaFile.name);
		
		if (maxBitRate > 0)
			params.put("maxBitRate", Integer.toString(maxBitRate));
		if (format != null)
			params.put("format", format);
		if (timeOffset > 0)
			params.put("timeOffset", Integer.toString(timeOffset));
		if (videoSize != null)
			params.put("videoSize", videoSize);
		if (estimateContentLength)
			params.put("estimateContentLength", "true");
		
		URL downloadURL = RESTTask.buildRESTCallURL(this.url, SubsonicMethods.STREAM, params);
		String extStorageDir = Environment.getExternalStorageDirectory().toString() + "/SubsonicClient/";
		String filePath = extStorageDir + mediaFile.path.substring(0, mediaFile.path.lastIndexOf(".")+1) + (mediaFile.transcodedSuffix != null ? mediaFile.transcodedSuffix : mediaFile.suffix); 
		new DownloadTask(downloadURL, filePath, progressDialog).execute();
	}
	
	/**
	 * Begins downloading the original file from the server without transcoding.
	 * 
	 * @param mediaFile	The MediaFile to download.
	 * @throws MalformedURLException 
	 * @throws UnsupportedEncodingException 
	 */
	protected void downloadOriginal(MediaFile mediaFile) throws MalformedURLException, UnsupportedEncodingException {
		Map<String, String> params = new HashMap<String, String>();
		params.putAll(requiredParams);
		params.put("id", Integer.toString(mediaFile.id));
		
		ProgressDialog progressDialog = Util.createPercentProgressDialog(this.context, "Downloading " + mediaFile.name);
		
		URL downloadURL = RESTTask.buildRESTCallURL(this.url, SubsonicMethods.DOWNLOAD, params);
		String extStorageDir = Environment.getExternalStorageDirectory().toString();
		new DownloadTask(downloadURL, extStorageDir + "/SubsonicClient/" + mediaFile.path, progressDialog).execute();
	}
	private static JSONObject parseSubsonicResponse(String responseStr) throws JSONException, SubsonicException {
		JSONObject jResponse = null;
		jResponse = new JSONObject(responseStr).getJSONObject("subsonic-response");
		
		// handle Subsonic errors
		if (jResponse != null && jResponse.getString("status").equals("failed")) {
			JSONObject err = jResponse.getJSONObject("error");
			throw new SubsonicException(err.getInt("code"), err.getString("message"));
		}
		
		return jResponse;
	}
//	
//	public void onRESTResponse(String method, Map<String, Object> callbackParams, String response) {
//    	try {
//    		JSONObject jResponse;
//    		try {
//    			jResponse = new JSONObject(response).getJSONObject("subsonic-response");
//    		} catch (NullPointerException e) {
//    			jResponse = null;
//    		} catch (JSONException e) {
//    			jResponse = null;
//    		}
//			
//			// handle Subsonic errors
//			if (jResponse != null && jResponse.getString("status").equals("failed")) {
//    			JSONObject err = jResponse.getJSONObject("error");
//    			throw new SubsonicException(err.getInt("code"), err.getString("message"));
//    		}
//    		
//    		// special handling for ping, because it can handle a null response
//    		if (method.equals(SubsonicMethods.PING)) {
//    			this.handler.onPingResponse((jResponse != null && jResponse.getString("status").equals("ok")));
//    		} else {
//    			
//	    		if (method.equals(SubsonicMethods.GET_LICENSE)) {
//					this.handler.onGetLicenseResponse(new License(jResponse.getJSONObject("license")));
//					
//				} else if (method.equals(SubsonicMethods.GET_MEDIA_FOLDERS)) {
//					List<MediaFolder> mediaFolders = new ArrayList<MediaFolder>();
//					JSONArray jFolderArr = jResponse.getJSONObject("musicFolders").getJSONArray("musicFolder");
//					int len = jFolderArr.length();
//					for (int i = 0; i < len; i++) {
//						mediaFolders.add(new MediaFolder(jFolderArr.getJSONObject(i)));
//					}
//					this.handler.onGetMediaFoldersResponse(mediaFolders);
//					
//				} else if (method.equals(SubsonicMethods.LIST_MEDIA_FOLDER_CONTENTS)) {
//					List<FilesystemEntry> contents = new ArrayList<FilesystemEntry>();
//					JSONObject jIndexesResponse = jResponse.getJSONObject("indexes");
//					JSONArray jChildArray = jIndexesResponse.optJSONArray("child");
//					JSONArray jIndexArray = jIndexesResponse.optJSONArray("index");
//					
//					if (jIndexArray != null) {
//						int jIndexArrayLength = jIndexArray.length();
//						for (int i = 0; i < jIndexArrayLength; i++) {
//							JSONObject jIndex = jIndexArray.getJSONObject(i);
//							
//							// artist tag can be either an array or an object (if there's only one)
//							JSONArray jFolderArray = jIndex.optJSONArray("artist");
//							if (jFolderArray != null) {
//								int jFolderArrayLength = jFolderArray.length();
//								for (int j = 0; j < jFolderArrayLength; j++) {
//									JSONObject jFolder = jFolderArray.getJSONObject(j);
//									Folder folder = new Folder(jFolder);
//									contents.add(folder);
//								}
//							} else {
//								JSONObject jFolder = jIndex.getJSONObject("artist");
//								Folder folder = new Folder(jFolder);
//								contents.add(folder);
//							}
//						}
//					}
//					
//					if (jChildArray != null) {
//						int jChildArrayLength = jChildArray.length();
//						for (int i = 0; i < jChildArrayLength; i++) {
//							contents.add(new MediaFile(jChildArray.getJSONObject(i)));
//						}
//					}
//					
//					this.handler.onListMediaFolderContentsResponse(contents);
//					
//				} else if (method.equals(SubsonicMethods.LIST_FOLDER_CONTENTS)) {
//					Folder parentFolder = (Folder)callbackParams.get("parentFolder");
//					List<FilesystemEntry> contents = new ArrayList<FilesystemEntry>();
//					JSONObject jDirectoryContents = jResponse.getJSONObject("directory");
//					
//					JSONArray jChildArray = jDirectoryContents.optJSONArray("child");
//					if (jChildArray != null) {
//						int jChildArrayLength = jChildArray.length();
//						for (int i = 0; i < jChildArrayLength; i++) {
//							JSONObject jChild = jChildArray.getJSONObject(i);
//							
//							FilesystemEntry entry;
//							if (jChild.getBoolean("isDir")) {
//								entry = new Folder(jChild);
//							} else {
//								entry = new MediaFile(jChild);
//							}
//							entry.parent = parentFolder;
//							contents.add(entry);
//						}
//					} else {
//						JSONObject jChild = jDirectoryContents.optJSONObject("child");
//						
//						if (jChild != null) {
//							FilesystemEntry entry;
//							if (jChild.getBoolean("isDir")) {
//								entry = new Folder(jChild);
//							} else {
//								entry = new MediaFile(jChild);
//							}
//							entry.parent = parentFolder;
//							contents.add(entry);
//						}
//					}
//					
//					this.handler.onListFolderContentsResponse(contents);
//						
//	    		}
//    		}
//		} catch (JSONException e) {
//			Util.showSingleButtonAlertBox(this.context, e.getLocalizedMessage(), "OK");
//			e.printStackTrace();
//		} catch (SubsonicException e) {
//			Util.showSingleButtonAlertBox(this.context, e.getLocalizedMessage(), "OK");
//			e.printStackTrace();
//		} catch (ParseException e) {
//			Util.showSingleButtonAlertBox(this.context, e.getLocalizedMessage(), "OK");
//			e.printStackTrace();
//		}
//	}
}
