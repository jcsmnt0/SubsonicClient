package com.casamento.subsonicclient;

class OnExceptionListener {
	static String logTag;
	static { logTag = "OnExceptionListener"; } // so logTag can be re-assigned by subclasses
	
	void onException(Exception e) {
		android.util.Log.e(logTag, e.toString());
	}
}
