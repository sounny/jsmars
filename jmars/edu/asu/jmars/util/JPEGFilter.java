package edu.asu.jmars.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class JPEGFilter extends FileFilter {
	String filter;
	String desc;
	
	public JPEGFilter(){
		desc = "JPEG Files";
		filter = ".jpg";
	}
	
	public boolean accept(File f) {
		String fname = f.getName().toLowerCase();
        return f.isDirectory() || fname.endsWith(filter);
	}

	public String getDescription() {
        return desc;
	}
}
