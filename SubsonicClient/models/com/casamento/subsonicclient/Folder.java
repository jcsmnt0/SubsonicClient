package com.casamento.subsonicclient;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

class Folder extends FilesystemEntry {
	protected int parentId, coverArtId;
	protected List<FilesystemEntry> contents;
	protected String artist, album;
	protected Calendar created;
	
	
	
	protected Folder() {
		super(-1, null, true);
	}
	protected Folder(int id, String name) {
		super(id, name, true);
	}
	
	protected Folder(JSONObject jFolder) throws JSONException, ParseException {
		super(jFolder.getInt("id"), Util.fixHTML(jFolder.optString("title", null) != null ? jFolder.getString("title") : jFolder.getString("name")), true);
		
		this.artist	= Util.fixHTML(jFolder.optString("artist", null));
		this.album	= Util.fixHTML(jFolder.optString("album", null));
		
		this.parentId	= jFolder.optInt("parent", -1);
		this.coverArtId	= jFolder.optInt("coverArt", -1);
		
		this.created = Util.getDateFromString(jFolder.optString("created", null));
	}
	
	protected void initContents() {
		this.contents = new ArrayList<FilesystemEntry>();
	}
}
