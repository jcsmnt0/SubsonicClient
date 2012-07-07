package com.casamento.subsonicclient;

import java.text.ParseException;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import com.casamento.subsonicclient.Util;

class MediaFile extends FilesystemEntry {
	protected String contentType, suffix, transcodedSuffix, transcodedContentType, path, album, artist, type;
	protected int parent, duration, bitRate, albumId, artistId, year;
	protected long size;
	protected boolean isVideo;
	protected Calendar created;
	
	protected MediaFile(JSONObject jFile) throws JSONException, ParseException {
		super (jFile.getInt("id"), jFile.optString("title", null) != null ? jFile.getString("title") : jFile.getString("name"), false);
		
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
}
