package com.casamento.subsonicclient;

class SubsonicException extends Exception {
	private static final long serialVersionUID = 8045416354641967497L;

	protected int code;
	protected String message;
	
	protected SubsonicException(Integer code, String message) {
		super(message);
		this.code = code;
	}
}
