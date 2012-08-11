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

import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

// convenience methods
public final class Util {
	public static Calendar getDateFromString(final String dateStr) {
		if (TextUtils.isEmpty(dateStr))
			return null;
		
		Calendar c = Calendar.getInstance();
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
		Date d = null;
		try {
			d = formatter.parse(dateStr);
		} catch (ParseException e) { // this shouldn't ever happen
			e.printStackTrace();
			Log.wtf("Subsonic date problem", e.getLocalizedMessage());
		}
		c.setTime(d);
		return c;
	}

	public static String getStringFromDate(final Calendar date) {
		if (date == null) return "";
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
		return formatter.format(date.getTime());
	}
	
	public static String fixHTML(final String toFix) {
		if (toFix == null) return null;
		return Html.fromHtml(toFix).toString();
	}
}
