package com.casamento.subsonicclient;

import android.app.ProgressDialog;
import android.content.Context;
import android.text.Html;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

// convenience methods and such
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
	
	public static void showSingleButtonAlertBox(final SherlockFragmentActivity activity, final String message, final String buttonText) {
		SherlockDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(message);
		alertDialogFragment.show(activity.getSupportFragmentManager(), "dialog");
	}

	// TODO: read Content-Length header of JSON and use ActionBar progress bar instead of this
	public static ProgressDialog createIndeterminateProgressDialog(final Context context, final String message) {
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setIndeterminate(true);
		dialog.setMessage(message);
		return dialog;
	}
	
	public static String fixHTML(final String toFix) {
		if (toFix == null) return null;
		return Html.fromHtml(toFix).toString();
	}
}
