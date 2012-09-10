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

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
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
import java.util.Map;

abstract class RestLoader<T> extends AsyncTaskLoader<T> {
    RestLoader(final Context c) { super(c); }

    private static final String logTag = "RestLoader";
    private static final int BUFFER_SIZE = 1024;

    static String buildRestCall(final String url, final String method, final Map<String, String> params) throws UnsupportedEncodingException {
        String urlStr = "";

        // assume http if no protocol given
        if (!url.contains("://"))
            urlStr += "http://";

        urlStr += url;

        if (method != null)
            urlStr += ("/" + method);
        if (params != null && !params.isEmpty()) {
            String paramStr = "";
            for (final Map.Entry<String, String> param : params.entrySet()) {
                paramStr += (URLEncoder.encode(param.getKey(), "UTF-8") + "=" + URLEncoder.encode(param.getValue(), "UTF-8") + "&");
            }

            // trim trailing &, for cleanliness
            paramStr = paramStr.substring(0, paramStr.length()-1);

            urlStr += ("?" + paramStr);
        }

        Log.v(logTag, "calling " + urlStr);
        return urlStr;
    }

    static String readAll(final InputStream input) throws IOException {
        String responseData = "";
        int bytesRead = 1;
        final byte[] buffer = new byte[BUFFER_SIZE];

        while (bytesRead > 0) {
            bytesRead = input.read(buffer);
            if (bytesRead > 0)
                responseData += new String(buffer, 0, bytesRead);
        }

        Log.w(logTag, "response: " + responseData);
        return responseData;
    }

    static InputStream getStream(final String restUrl, final String username, final String password) throws AuthenticationException, IOException {
        final HttpClient client = new DefaultHttpClient();
        final HttpUriRequest get = new HttpGet(restUrl);

        final Credentials creds = new UsernamePasswordCredentials(username, password);
        get.addHeader(new BasicScheme().authenticate(creds, get));

        // make the request
        final HttpResponse response = client.execute(get);

        return new BufferedInputStream(response.getEntity().getContent());
    }
}
