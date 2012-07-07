package com.casamento.subsonicclient;

abstract class OnMediaFolderContentsResponseListener extends OnExceptionListener {
	static { logTag = "OnMediaFolderContentsResponseListener"; }
	
	abstract void onMediaFolderContentsResponse(java.util.List<FilesystemEntry> contents);
}
