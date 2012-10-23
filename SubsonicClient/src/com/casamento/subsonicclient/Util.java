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

import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

// Methods that haven't yet found a home elsewhere, and are needed in disparate places
public final class Util {
    public static Calendar getDateFromISOString(final String dateStr) {
        if (TextUtils.isEmpty(dateStr))
            return null;

        final Calendar c = Calendar.getInstance();
        final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        Date d = null;
        try {
            d = formatter.parse(dateStr);
        } catch (ParseException e) { // this shouldn't ever happen
            e.printStackTrace();
            Log.wtf("Date format problem", e.getLocalizedMessage(), e);
        }
        c.setTime(d);
        return c;
    }

    public static String getISOStringFromDate(final Calendar date) {
        if (date == null) return "";
        final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        return formatter.format(date.getTime());
    }

    public static String fixHtmlEntities(final String toFix) {
        if (toFix == null) return null;
        return Html.fromHtml(toFix).toString();
    }

    // Builds a URL to perform a call to a REST service.
    // Output form is url/method/key1=value1&key2=value2&... where key<n>/value<n> are the entries in the params Map
    public static String buildRestCall(final String url, final String method, final Map<String, String> params) {
        String urlStr = "";

        // Assume http if no protocol given
        if (!url.contains("://"))
            urlStr += "http://";

        urlStr += url;

        if (method != null)
            urlStr += ("/" + method);
        if (params != null && !params.isEmpty()) {
            String paramStr = "";
            for (final Map.Entry<String, String> param : params.entrySet()) {
                try {
                    final String key = URLEncoder.encode(param.getKey(), "UTF-8");
                    final String value = URLEncoder.encode(param.getValue(), "UTF-8");

                    paramStr += (key + "=" + value + "&");
                } catch (final UnsupportedEncodingException e) {
                    // Android always supports UTF-8, so this is never thrown
                    Log.wtf("UTF-8 is somehow not available", e);
                }
            }

            // Trim trailing '&' char, for cleanliness
            paramStr = paramStr.substring(0, paramStr.length()-1);

            urlStr += ("?" + paramStr);
        }

        return urlStr;
    }

    private static final int BUFFER_SIZE = 1024;

    // Reads all bytes from an InputStream into a String
    static String readAll(final InputStream input) throws IOException {
        String responseData = "";
        int bytesRead = 1;
        final byte[] buffer = new byte[BUFFER_SIZE];

        while (bytesRead > 0) {
            bytesRead = input.read(buffer);
            if (bytesRead > 0)
                responseData += new String(buffer, 0, bytesRead);
        }

        return responseData;
    }

    // Returns an InputStream wrapping a file on a network using basic preemptive HTTP authentication
    static InputStream getStream(final String restUrl, final String username, final String password) throws AuthenticationException, IOException {
        final HttpClient client = new DefaultHttpClient();
        final HttpUriRequest get = new HttpGet(restUrl);

        if (username != null && password != null) {
            final Credentials creds = new UsernamePasswordCredentials(username, password);
            get.addHeader(new BasicScheme().authenticate(creds, get));
        }

        // Make the request
        final HttpResponse response = client.execute(get);

        return new BufferedInputStream(response.getEntity().getContent());
    }
}
