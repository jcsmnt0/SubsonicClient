package com.casamento.subsonicclient;

class FilesystemEntry {
	protected int id;
	protected boolean isFolder;
	
	// name is "title" attribute of files, "name" of directories/indices
	protected String name;
	protected Folder parent;
	
	protected FilesystemEntry(int id, String name, boolean isFolder) {
		this.id = id;
		this.isFolder = isFolder;
		this.name = name;
	}
}
