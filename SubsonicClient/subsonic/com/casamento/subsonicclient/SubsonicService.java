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

import android.os.Parcel;
import android.text.TextUtils;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class SubsonicService extends DataRetrievalService {
    private static final String API_VERSION = "1.8.0";
    private static final String CLIENT_ID = "Android Subsonic Client (pre-alpha)";

    private static final String
            METHOD_LIST_TOP_LEVEL_FOLDERS = "rest/getMusicFolders.view",
            METHOD_LIST_TOP_LEVEL_FOLDER_CONTENTS = "rest/getIndexes.view",
            METHOD_LIST_FOLDER_CONTENTS = "rest/getMusicDirectory.view",
            METHOD_STREAM = "rest/stream.view",
            METHOD_DOWNLOAD = "rest/download.view";

    private static final Map<String, String> mRequiredParams = new HashMap<String, String>() {
        {
            put("v", API_VERSION);
            put("c", CLIENT_ID);
            put("f", "json");
        }
    };

    static class SubsonicAccessInformation extends DataSource.AccessInformation {
        private final String mUrl, mUsername, mPassword;

        @Override
        public boolean hasCache() {
            return true;
        }

        @Override
        public String getDownloadUrl(final MediaFile mf, final boolean transcoded) {
            final Map<String, String> params = new HashMap<String, String>(mRequiredParams);
            params.put("id", Integer.toString(mf.id));

            return Util.buildRestCall(mUrl, transcoded ? METHOD_STREAM : METHOD_DOWNLOAD, params);
        }

        @Override
        public String getUsername() {
            return mUsername;
        }

        @Override
        public String getPassword() {
            return mPassword;
        }

        SubsonicAccessInformation(final String url, final String username, final String password) {
            mUrl = url;
            mUsername = username;
            mPassword = password;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel out, final int flags) {
            out.writeString(mUrl);
            out.writeString(mUsername);
            out.writeString(mPassword);
        }

        public static final Creator<SubsonicAccessInformation> CREATOR = new Creator<SubsonicAccessInformation>() {
            @Override
            public SubsonicAccessInformation[] newArray(final int size) {
                return new SubsonicAccessInformation[size];
            }

            @Override
            public SubsonicAccessInformation createFromParcel(final Parcel in) {
                return new SubsonicAccessInformation(in);
            }
        };

        private SubsonicAccessInformation(final Parcel in) {
            this(in.readString(), in.readString(), in.readString());
        }
    }

    private static Folder unmarshalTopLevelFolder(final JSONObject jsonFolder) throws JSONException {
        // Top-level folders are represented with negative IDs
        return new Folder(-jsonFolder.getInt("id"), null, jsonFolder.getString("name"));
    }

    @Override
    Folder[] retrieveTopLevelFolders(final DataSource.AccessInformation accessInfo) throws AuthenticationException, DataSourceException, JSONException, IOException {
        final SubsonicAccessInformation subInfo = (SubsonicAccessInformation) accessInfo;

        // Retrieve the data from the server
        final String callUrl = Util.buildRestCall(subInfo.mUrl, METHOD_LIST_TOP_LEVEL_FOLDERS, mRequiredParams);
        final String responseStr = Util.readAll(Util.getStream(callUrl, subInfo.mUsername, subInfo.mPassword));
        final JSONObject response = parseSubsonicResponse(responseStr);

        // The "musicFolder" element can be either an array or a single object
        final JSONObject foldersObject = response.getJSONObject("musicFolders");
        final JSONArray folderArray = foldersObject.optJSONArray("musicFolder");
        if (folderArray != null) {
            final int folderCount = folderArray.length();
            final Folder[] folders = new Folder[folderCount];

            for (int i = 0; i < folderCount; i++) {
                folders[i] = unmarshalTopLevelFolder(folderArray.getJSONObject(i));
            }

            return folders;
        } else {
            return new Folder[] { unmarshalTopLevelFolder(foldersObject.getJSONObject("musicFolder")) };
        }
    }

    @Override
    FilesystemEntry[] retrieveFolderContents(final DataSource.AccessInformation accessInfo, final Folder folder) throws IOException, AuthenticationException, DataSourceException, JSONException {
        final SubsonicAccessInformation subInfo = (SubsonicAccessInformation) accessInfo;

        final Map<String, String> params = new HashMap<String, String>(mRequiredParams);
        params.put("id", Integer.toString(folder.id));

        final String method;

        if (folder.isTopLevel) {
            // Top-level folders have negative IDs
            params.put("musicFolderId", Integer.toString(-folder.id));
            method = METHOD_LIST_TOP_LEVEL_FOLDER_CONTENTS;
        } else {
            params.put("id", Integer.toString(folder.id));
            method = METHOD_LIST_FOLDER_CONTENTS;
        }

        // Get the response from the server
        final String callUrl = Util.buildRestCall(subInfo.mUrl, method, params);
        final String responseStr = Util.readAll(Util.getStream(callUrl, subInfo.mUsername, subInfo.mPassword));
        final JSONObject response = parseSubsonicResponse(responseStr);

        final List<FilesystemEntry> entryList = new ArrayList<FilesystemEntry>();

        // Unmarshal the objects from the server response

        JSONObject indexesObj = response.optJSONObject("indexes");
        if (indexesObj == null) indexesObj = response.getJSONObject("directory");

        // The "child" element can be either an array or an object; optJSONArray("child") returns null if it's an object
        final JSONArray children = indexesObj.optJSONArray("child");
        if (children == null) {
            final JSONObject child = indexesObj.optJSONObject("child");
            if (child != null) {
                entryList.add(getFilesystemEntry(child, folder.id));
            }
        } else {
            for (int i = 0, len = children.length(); i < len; i++) {
                entryList.add(getFilesystemEntry(children.getJSONObject(i), folder.id));
            }
        }

        final JSONArray indexArr = indexesObj.optJSONArray("index");
        if (indexArr != null) {
            for (int i = 0, indexCount = indexArr.length(); i < indexCount; i++) {
                final JSONObject index = indexArr.getJSONObject(i);

                // Artist tag can be either an array or an object (if there's only one)
                final JSONArray folders = index.optJSONArray("artist");
                if (folders != null) {
                    for (int j = 0, folderCount = folders.length(); j < folderCount; j++) {
                        entryList.add(getFilesystemEntry(folders.getJSONObject(j), folder.id, true));
                    }
                } else {
                    entryList.add(getFilesystemEntry(index.getJSONObject("artist"), folder.id, true));
                }
            }
        }

        return entryList.toArray(new FilesystemEntry[entryList.size()]);
    }

    private static JSONObject parseSubsonicResponse(final CharSequence responseStr) throws JSONException, DataSourceException {
        final JSONObject jResponse = new JSONObject((String) responseStr).getJSONObject("subsonic-response");

        // Handle Subsonic errors
        if (jResponse != null && jResponse.getString("status").equals("failed")) {
            final JSONObject err = jResponse.getJSONObject("error");
            throw new DataSourceException(err.getInt("code"), err.getString("message"));
        }

        return jResponse;
    }

    private static FilesystemEntry getFilesystemEntry(final JSONObject json, final int parentId) throws JSONException {
        return getFilesystemEntry(json, parentId, json.optBoolean("isDir"));
    }

    private static FilesystemEntry getFilesystemEntry(final JSONObject json, final int parentId,
            final boolean isFolder) throws JSONException {
        return isFolder ?
                getFolder(json, parentId) :
                getMediaFile(json, parentId);
    }

    // Non-top-level folder factory method
    private static Folder getFolder(final JSONObject json, Integer parentId) throws JSONException {
        final Integer id = json.getInt("id");

        Integer coverArtId = json.optInt("coverArt", Integer.MIN_VALUE);
        if (coverArtId == Integer.MIN_VALUE) coverArtId = null;

        String name = json.optString("name");
        if (TextUtils.isEmpty(name)) name = json.optString("title");

        final String artist = Util.fixHtmlEntities(json.optString("artist"));
        final String album = Util.fixHtmlEntities(json.optString("album"));

        final Calendar created = Util.getDateFromISOString(json.optString("created"));

        return new Folder(id, parentId, name, coverArtId, artist, album, created, false);
    }

    private static MediaFile getMediaFile(final JSONObject json, final Integer parentId) throws JSONException {
        final Integer id = json.getInt("id");

        Integer coverArtId = json.optInt("coverArt", Integer.MIN_VALUE);
        if (coverArtId == Integer.MIN_VALUE) coverArtId = null;

        String name = json.optString("name");
        if (TextUtils.isEmpty(name)) name = json.optString("title");

        final String path = json.getString("path");
        final String suffix = Util.fixHtmlEntities(json.getString("suffix"));
        final String transcodedSuffix = Util.fixHtmlEntities(json.optString("suffix"));
        final String artist = Util.fixHtmlEntities(json.optString("artist"));
        final String album = Util.fixHtmlEntities(json.optString("album"));

        Long size = json.optLong("size", Integer.MIN_VALUE);
        if (size == Integer.MIN_VALUE) size = null;

        Integer duration = json.optInt("duration", Integer.MIN_VALUE);
        if (duration == Integer.MIN_VALUE) duration = null;

        Integer trackNumber = json.optInt("track", Integer.MIN_VALUE);
        if (trackNumber == Integer.MIN_VALUE) trackNumber = null;

        final Calendar created = Util.getDateFromISOString(json.optString("created"));

        return new MediaFile(id, parentId, name, coverArtId, path, suffix, transcodedSuffix, artist, album, size,
                duration, trackNumber, created);
    }
}