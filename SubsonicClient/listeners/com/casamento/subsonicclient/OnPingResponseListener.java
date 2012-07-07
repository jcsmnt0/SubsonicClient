package com.casamento.subsonicclient;

abstract class OnPingResponseListener {
	// returns true on ping success, false on any failure (
	abstract void onPingResponse(boolean ok);
}
