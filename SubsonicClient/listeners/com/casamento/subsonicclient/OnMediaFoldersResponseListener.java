package com.casamento.subsonicclient;

public abstract class OnMediaFoldersResponseListener extends OnExceptionListener {
	static { logTag = "OnMediaFoldersResponseListener"; }
	
	abstract void onMediaFoldersResponse(java.util.List<MediaFolder> mediaFolders);
}
