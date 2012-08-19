package com.casamento.subsonicclient;

import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Map;

class RestCaller {
	private static final String logTag = "RestCaller";

	protected static String buildRestCall(String restUrl, String method, Map<String, String> params) throws MalformedURLException, UnsupportedEncodingException, URISyntaxException {
		String urlStr = "";

		// assume http if no protocol given
		if (restUrl.indexOf("://") < 0)
			urlStr += "http://";

		urlStr += restUrl;

		if (method != null)
			urlStr += ("/" + method);
		if (params != null && params.size() > 0) {
			String paramStr = "";
			for (Map.Entry<String, String> param : params.entrySet()) {
				paramStr += (URLEncoder.encode(param.getKey(), "UTF-8") + "=" + URLEncoder.encode(param.getValue(), "UTF-8") + "&");
			}

			// trim trailing &, for cleanliness
			paramStr = paramStr.substring(0, paramStr.length()-1);

			urlStr += ("?" + paramStr);
		}

		Log.d(logTag, "calling " + urlStr);
		return urlStr;
	}

	static abstract class RestCallTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
		/**
		 * @param input the InputStream to read from
		 * @return all of the data in the InputStream
		 * @throws IOException
		 */
		private String readAllFromStream(InputStream input) throws IOException {
			String responseData = "";
			int bytesRead = 1;
			byte[] buffer = new byte[1024];

			while (bytesRead > 0 && !isCancelled()) {
				bytesRead = input.read(buffer);
				if (bytesRead > 0)
					responseData += new String(buffer, 0, bytesRead);
			}

			Log.w(logTag, "response: " + responseData);
			return responseData;
		}

		/**
		 * @param restUrl the URL of the REST call
		 * @return the response data as a String
		 * @throws IOException
		 */
		String getRestResponse(String restUrl) throws IOException {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(restUrl);
			HttpResponse response = client.execute(get);

			InputStream input = new BufferedInputStream(response.getEntity().getContent());
			return readAllFromStream(input);
		}

		/**
		 * @param restUrl the URL of the REST call
		 * @param username the username to put in the basic authentication string
		 * @param password the password to put in the basic authentication string
		 * @return the response data as a String
		 * @throws IOException
		 */
		String getRestResponse(String restUrl, String username, String password) throws IOException, AuthenticationException {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(restUrl);

			UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
			get.addHeader(new BasicScheme().authenticate(creds, get));

			// make the request
			HttpResponse response = client.execute(get);

			// read all the data and return it
			return readAllFromStream(new BufferedInputStream(response.getEntity().getContent()));
		}
	}
}
