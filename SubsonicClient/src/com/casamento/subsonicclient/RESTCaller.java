package com.casamento.subsonicclient;

import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Map;

class RESTCaller {
	private static final String logTag = "RESTCaller";

	static interface OnRESTResponseListener {
		void onRESTResponse(String responseStr);
		void onException(Exception e);
	}

	protected static URI buildRESTCallURI(String restUrl, String method, Map<String, String> params) throws MalformedURLException, UnsupportedEncodingException, URISyntaxException {
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
		return new URI(urlStr);
	}

	protected static void call(String restUrl, String method, OnRESTResponseListener callbackListener) throws UnsupportedEncodingException, URISyntaxException, MalformedURLException {
		new RESTTask(RESTCaller.buildRESTCallURI(restUrl, method, null), callbackListener).execute();
	}
	protected static void call(String restUrl, String method, Map<String, String> params, OnRESTResponseListener callbackListener) throws UnsupportedEncodingException, URISyntaxException, MalformedURLException {
		new RESTTask(RESTCaller.buildRESTCallURI(restUrl, method, params), callbackListener).execute();
	}

	protected static class RESTTask extends AsyncTask<Void, Integer, String> {
		private final static String logTag = "RESTTask";
		private final URI restURI;
		private final OnRESTResponseListener callbackListener;

		/**
		 * @param restURI			The URI of the REST service.
		 * @param callbackListener  The OnRESTResponseListener that will handle the callback.
		 */
		protected RESTTask(URI restURI, OnRESTResponseListener callbackListener) {
			this.restURI = restURI;
			this.callbackListener = callbackListener;
		}

		// triggered by publishProgress(Integer)
		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
		}

		@Override
		protected String doInBackground(Void... nothing) {
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = null;
			httpGet = new HttpGet(this.restURI);

			HttpResponse response;
			try {
				response = httpClient.execute(httpGet);
				HttpEntity entity = response.getEntity();

				InputStream input = new BufferedInputStream(entity.getContent());

				StringBuilder sb = new StringBuilder();
				int ch;
				while ((ch = input.read()) != -1 && !this.isCancelled()) {
					sb.append((char)ch);
				}
				return sb.toString();
			} catch (Exception e) {
				callbackListener.onException(e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			Log.v(logTag + ".onPostExecute", " results:\n" + result);
			Log.v(logTag + ".onPostExecute", result);
			callbackListener.onRESTResponse(result);
		}
	}
}
