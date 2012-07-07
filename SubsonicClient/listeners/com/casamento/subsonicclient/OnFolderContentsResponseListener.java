package com.casamento.subsonicclient;

abstract class OnFolderContentsResponseListener extends OnExceptionListener {
	static { logTag = "OnFolderContentsResponse"; }
	
	abstract void onFolderContentsResponse(java.util.List<FilesystemEntry> contents);
}
