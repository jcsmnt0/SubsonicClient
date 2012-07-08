package com.casamento.subsonicclient;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;

class DownloadTask extends AsyncTask<Void, Long, String> {
	private final String logTag = "DownloadTask";
	private URL url;
	private String saveLocation;
	private ProgressDialog progressDialog;
	private boolean indeterminate;
	private String dialogMessage;
	
	protected DownloadTask(URL url, String saveLocation, ProgressDialog progressDialog) {
		this.url = url;
		this.progressDialog = progressDialog;
		this.saveLocation = saveLocation;
		Log.d(logTag, url.toString());
		Log.d(logTag, saveLocation);
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		progressDialog.show();
	}
	
	@Override
	protected void onProgressUpdate(Long... progress) {
		super.onProgressUpdate(progress);
		
		progressDialog.setProgress(progress[0].intValue());
	}
	
	@Override
	protected String doInBackground(Void... params) {
		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = null;
			
			httpGet = new HttpGet(this.url.toURI());
			HttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			
			if (entity != null) {
				InputStream input = new BufferedInputStream(entity.getContent());
				File outDir = new File(this.saveLocation.substring(0, this.saveLocation.lastIndexOf("/")));
				outDir.mkdirs();
				OutputStream output = new FileOutputStream(this.saveLocation);
				
				if (this.progressDialog != null) {
					int contentLength = (int)entity.getContentLength();
					if (contentLength > 0) {
						this.progressDialog.setMax(contentLength);
						this.progressDialog.setIndeterminate(this.indeterminate = false);
					} else {
						this.progressDialog.setIndeterminate(this.indeterminate = true);
					}
				}
				
				byte data[] = new byte[1024];
				long total = 0;
				int count;
				while ((count = input.read(data)) != -1) {
					total += count;
					if (!this.indeterminate)
						publishProgress(total);
					output.write(data, 0, count);
				}
				output.flush();
				output.close();
				input.close();
				this.progressDialog.dismiss();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
}
