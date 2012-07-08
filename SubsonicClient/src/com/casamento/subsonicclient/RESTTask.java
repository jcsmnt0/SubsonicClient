package com.casamento.subsonicclient;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

class RESTTask extends AsyncTask<Void, Integer, String> {
	private final static String logTag = "RESTTask";
	private final String url;
	private final String method;
	private final OnRESTResponseListener callbackListener;
	private Map<String, String> params;
	private ProgressDialog dialog;
	
	/**
	 * @param url				The URL of the REST service.
	 * @param method			The REST method to call (can be null).
	 * @param params			The parameters to pass to the REST method (can be null).
	 * @param callbackListener  The OnRESTResponseListener that will handle the callback.
	 */
	protected RESTTask(String url, String method, Map<String, String> params, OnRESTResponseListener callbackListener) {
		this.url = url;
		this.method = method;
		this.params  = params;
		this.callbackListener = callbackListener;
	}

	/**
	 * @param url				The URL of the REST service.
	 * @param method			The REST method to call (can be null).
	 * @param params			The parameters to pass to the REST method (can be null).
	 * @param dialog			The ProgressDialog to update (can be null).
	 * @param callbackListener  The OnRESTResponseListener that will handle the callback.
	 */
	protected RESTTask(String url, String method, Map<String, String> params, ProgressDialog dialog, OnRESTResponseListener callbackListener) {
		this(url, method, params, callbackListener);
		this.dialog = dialog;
	}
	
	/**
	 * Returns the URL that calls a REST method with parameters.
	 * 
	 * @param restUrl	The URL of the REST server.
	 * @param method	The REST method to call (can be null).
	 * @param params	The parameters to pass (can be null).
	 * @return          The URL of the REST method call.
	 * @throws MalformedURLException 
	 * @throws UnsupportedEncodingException 
	 */
	protected static URL buildRESTCallURL(String restUrl, String method, Map<String, String> params) throws MalformedURLException, UnsupportedEncodingException {
		StringBuilder urlStr = new StringBuilder();
		
		// assume http if no protocol given
		if (restUrl.indexOf("://") < 0)
			urlStr.append("http://");
		
		urlStr.append(restUrl);
		
		if (method != null)
			urlStr.append("/" + method);
		if (params != null && params.size() > 0) {
			String paramStr = "";
			for (Map.Entry<String, String> param : params.entrySet()) {
				paramStr += (URLEncoder.encode(param.getKey(), "UTF-8") + "=" + URLEncoder.encode(param.getValue(), "UTF-8") + "&"); 
			}
			
			// trim trailing &, for cleanliness
			paramStr = paramStr.substring(0, paramStr.length()-1);

			urlStr.append("?" + paramStr);
		}
		
		Log.d(logTag, urlStr.toString());
		return new URL(urlStr.toString());
	}
	
	@Override
	protected void onPreExecute() {
		if (dialog != null)
			dialog.show();
		Log.d(logTag, "Calling " + this.method);
	}
	
	// triggered by publishProgress(Integer)
	@Override
	protected void onProgressUpdate(Integer... progress) {
		super.onProgressUpdate(progress);
		if (dialog != null)
			dialog.setProgress(progress[0]);
	}
	
	@Override
	protected String doInBackground(Void... nothing) {
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = null;
		try {
			httpGet = new HttpGet(buildRESTCallURL(this.url, this.method, this.params).toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return "";
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return "";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
		
		HttpResponse response;
		try {
			response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			
			InputStream input = new BufferedInputStream(entity.getContent());
			
			StringBuilder sb = new StringBuilder();
			int ch;
			while ((ch = input.read()) != -1) {
				sb.append((char)ch);
			}
			return sb.toString();
		} catch (IOException e) {
			Log.e(logTag, e.toString());
			return "";
		}
	}
	
	@Override
	protected void onPostExecute(String result) {
		Log.v(logTag + ".onPostExecute", this.method + " results: ");
		Log.v(logTag + ".onPostExecute", result);
		if (dialog != null)
			dialog.dismiss();
		callbackListener.onRESTResponse(result);
	}
}
