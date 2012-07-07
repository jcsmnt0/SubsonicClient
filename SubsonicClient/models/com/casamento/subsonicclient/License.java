package com.casamento.subsonicclient;

import java.text.ParseException;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import com.casamento.subsonicclient.Util;

class License {
	protected boolean valid;
	protected String email, key;
	protected Calendar date;
	
	protected License(JSONObject license) throws JSONException, ParseException {
		this.valid = license.getBoolean("valid");
		this.email = license.getString("email");
		this.key = license.getString("key");
		this.date = Util.getDateFromString(license.getString("date"));
	}
}
