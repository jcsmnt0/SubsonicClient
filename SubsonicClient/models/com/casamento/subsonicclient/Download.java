package com.casamento.subsonicclient;

import android.widget.ProgressBar;

import java.net.URL;

class Download {
	protected ProgressBar progressBar;
	protected String name, savePath;
	protected URL url;

	protected Download(URL url, String name, String savePath) {
		this.name = name;
		this.savePath = savePath;
		this.url = url;
	}
}