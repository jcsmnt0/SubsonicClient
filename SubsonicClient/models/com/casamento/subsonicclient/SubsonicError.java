package com.casamento.subsonicclient;

class SubsonicError {
	Integer code;
	String message;
	
	protected SubsonicError(Integer code, String message) {
		this.code = code;
		this.message = message;
	}
}
