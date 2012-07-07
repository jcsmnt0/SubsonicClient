package com.casamento.subsonicclient;

import java.text.ParseException;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

class MediaFolder extends Folder {
	protected MediaFolder() {
		super();
	}
	protected MediaFolder(String name) {
		super(-1, name);
	}
	protected MediaFolder(JSONObject jMediaFolder) throws JSONException, ParseException {
		super(jMediaFolder.getInt("id"), jMediaFolder.getString("name"));
	}
}
