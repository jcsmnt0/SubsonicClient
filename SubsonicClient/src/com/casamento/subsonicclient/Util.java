package com.casamento.subsonicclient;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.text.Html;

public final class Util {
	public static Calendar getDateFromString(final String dateStr) throws ParseException {
		if (dateStr == null)
			return null;
		
		// God help me if Subsonic uses different date formats based on locales
		Calendar c = Calendar.getInstance();
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
		Date d = formatter.parse(dateStr);
		c.setTime(d);
		return c;
	}
	
	public static void showSingleButtonAlertBox(final Context context, final String message, final String buttonText) {
		AlertDialog.Builder alertBox = new AlertDialog.Builder(context);
		alertBox.setMessage(message);
		alertBox.setNeutralButton(buttonText, null);
		alertBox.show();
	}
	
	public static ProgressDialog createIndeterminateProgressDialog(final Context context, final String message) {
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setIndeterminate(true);
		dialog.setMessage(message);
		return dialog;
	}
	
	public static ProgressDialog createPercentProgressDialog(final Context context, final String message) {
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setMessage(message);
		dialog.setIndeterminate(false);
		dialog.setMax(100);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		return dialog;
	}
	
	public static String getURLContentType(final URL url) throws IOException {
		return url.openConnection().getContentType();
	}
	
	public static String fixHTML(final String toFix) {
		if (toFix == null) return null;
		return Html.fromHtml((String)toFix).toString();
	}
}
