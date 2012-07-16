package com.casamento.subsonicclient;

import android.text.Html;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

// convenience methods
public final class Util {
	public static Calendar getDateFromString(final String dateStr) throws ParseException {
		if (dateStr == null)
			return null;
		
		Calendar c = Calendar.getInstance();
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
		Date d = formatter.parse(dateStr);
		c.setTime(d);
		return c;
	}
	
	public static String fixHTML(final String toFix) {
		if (toFix == null) return null;
		return Html.fromHtml(toFix).toString();
	}
}
