package com.casamento.subsonicclient;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;

class DownloadTask extends AsyncTask<Void, Long, String> {
	private final String logTag = "DownloadTask";
	String url, savePath, mUsername, mPassword;
	DownloadTaskListener mListener;

	interface DownloadTaskListener {
		void onProgressUpdate(long progress);
		void onDownloadCompletion(DownloadTask task);
	}

	private enum ByteSize {
		KB(1L << 10),
		MB(1L << 20),
		GB(1L << 30),
		TB(1L << 40); // hopefully nobody is downloading files bigger than 1024 terabytes

		private final long size;
		private ByteSize(long size) {
			this.size = size;
		}

		public static String getByteString(long bytes) {
			if (bytes >= TB.size) {
				return String.format("%.2fTB", (double)bytes / TB.size);
			} else if (bytes >= GB.size) {
				return String.format("%.2fGB", (double)bytes / GB.size);
			} else if (bytes >= MB.size) {
				return String.format("%.2fMB", (double)bytes / MB.size);
			} else if (bytes >= KB.size) {
				return String.format("%.2fKB", (double)bytes / KB.size);
			}
			else return String.format("%dB", bytes);
		}
	}

	DownloadTask(String url, String savePath, String username, String password) {
		this.url = url;
		this.savePath = savePath;
		mUsername = username;
		mPassword = password;
	}

	void setListener(DownloadTaskListener listener) {
		mListener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}
	
	@Override
	protected void onProgressUpdate(Long... progress) {
		super.onProgressUpdate(progress);
		mListener.onProgressUpdate(progress[0]);
	}

	@Override
	protected String doInBackground(Void... params) {
		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet;
			
			httpGet = new HttpGet(url);

			if (!TextUtils.isEmpty(mUsername) && !TextUtils.isEmpty(mPassword)) {
				UsernamePasswordCredentials creds = new UsernamePasswordCredentials(mUsername, mPassword);
				httpGet.addHeader(new BasicScheme().authenticate(creds, httpGet));
			}

			HttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			
			if (entity != null) {
				InputStream input = new BufferedInputStream(entity.getContent());
				File outDir = new File(savePath.substring(0, savePath.lastIndexOf("/")));
				outDir.mkdirs();
				OutputStream output = new FileOutputStream(savePath);

				byte data[] = new byte[1024];
				long total = 0;
				int count;
				while ((count = input.read(data)) != -1) {
					total += count;
					publishProgress(total);
					Log.v(logTag, Long.toString(total));
					output.write(data, 0, count);
				}
				output.flush();
				output.close();
				input.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(String s) {
		super.onPostExecute(s);
		if (mListener != null)
			mListener.onDownloadCompletion(this);
	}
}
