package com.casamento.subsonicclient;

import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.net.URI;

class DownloadTask extends AsyncTask<Void, Long, String> {
	private final String logTag = "DownloadTask";
	private TextView progressView;
	private ProgressBar progressBar;
	private long contentLength;
	private long progress;
	private String name, savePath;
	private URI uri;
	private OnDownloadTaskCompletedListener callbackListener;

	protected String getName() {
		return name;
	}

	protected String getSavePath() {
		return savePath;
	}

	protected interface OnDownloadTaskCompletedListener {
		void onDownloadTaskCompleted(DownloadTask task);
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

	protected DownloadTask(final URI uri, final String name, final String savePath) {
		this.uri = uri;
		this.name = name;
		this.savePath = savePath;
	}

	protected void setProgressView(TextView progressView) {
		this.progressView = progressView;
		this.progressView.setText(ByteSize.getByteString(this.progress) + "/" + ByteSize.getByteString(this.contentLength));
	}

	protected void setProgressBar(ProgressBar progressBar) {
		this.progressBar = progressBar;
		this.progressBar.setIndeterminate(this.contentLength > 0);
		this.progressBar.setMax((int)this.contentLength); // this may cause issues with files of >10GB
		this.progressBar.setProgress((int)this.progress);
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}
	
	@Override
	protected void onProgressUpdate(Long... progress) {
		super.onProgressUpdate(progress);
		this.progress = progress[0];

		if (this.progressView != null) {
			this.progressView.setText(ByteSize.getByteString(this.progress) + "/" + ByteSize.getByteString(this.contentLength));
		}

		if (this.progressBar != null) {
			this.progressBar.setMax((int)this.contentLength); // this may cause issues with files of >10GB
			this.progressBar.setProgress((int)this.progress);
		}
	}

	@Override
	protected String doInBackground(Void... params) {
		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet;
			
			httpGet = new HttpGet(this.uri);
			HttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			
			if (entity != null) {
				InputStream input = new BufferedInputStream(entity.getContent());
				File outDir = new File(this.savePath.substring(0, this.savePath.lastIndexOf("/")));
				outDir.mkdirs();
				OutputStream output = new FileOutputStream(this.savePath);

				this.contentLength = entity.getContentLength();
				boolean indeterminate = (contentLength <= 0);
				
				byte data[] = new byte[1024];
				long total = 0;
				int count;
				while ((count = input.read(data)) != -1) {
					total += count;
					if (!indeterminate)
						this.publishProgress(total);
					output.write(data, 0, count);
				}
				output.flush();
				output.close();
				input.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(String s) {
		super.onPostExecute(s);
		if (callbackListener != null)
			callbackListener.onDownloadTaskCompleted(this);
	}
}
