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
import java.net.URISyntaxException;
import java.net.URL;

class DownloadTask extends AsyncTask<Void, Long, String> {
	private final String logTag = "DownloadTask";
	private TextView progressView;
	private ProgressBar progressBar;
	private long contentLength;
	protected String name, savePath;
	protected URL url;

	protected DownloadTask(final URL url, final String name, final String savePath) {
		this.url = url;
		this.name = name;
		this.savePath = savePath;
	}

	protected void attachProgressView(TextView progressView) {
		this.progressView = progressView;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}
	
	@Override
	protected void onProgressUpdate(Long... progress) {
		super.onProgressUpdate(progress);

		if (this.progressView != null) {
			this.progressView.setText(progress[0].toString() + "/" + Long.toString(this.contentLength) + " bytes");
		}
	}
	
	@Override
	protected String doInBackground(Void... params) {
		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet;
			
			httpGet = new HttpGet(this.url.toURI());
			HttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			
			if (entity != null) {
				InputStream input = new BufferedInputStream(entity.getContent());
				File outDir = new File(this.savePath.substring(0, this.savePath.lastIndexOf("/")));
				outDir.mkdirs();
				OutputStream output = new FileOutputStream(this.savePath);

				this.contentLength = entity.getContentLength();
				boolean indeterminate = (contentLength <= 0);

//				if (!indeterminate) {
//					this.progressBar.setMax((int)contentLength);
//					this.progressBar.setIndeterminate(false);
//				} else {
//					this.progressBar.setVisibility(View.GONE);
//				}
				
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
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
}
