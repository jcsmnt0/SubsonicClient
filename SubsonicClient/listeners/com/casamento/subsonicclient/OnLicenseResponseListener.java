package com.casamento.subsonicclient;

abstract class OnLicenseResponseListener extends OnExceptionListener {
	static { logTag = "OnLicenseResponseListener"; }
	
	abstract void onLicenseResponse(License license);
}
