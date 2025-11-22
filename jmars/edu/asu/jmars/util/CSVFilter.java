package edu.asu.jmars.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;

//Filter for the file chooser for exporting data
//(lets .jlf's be displayed??)
public class CSVFilter extends FileFilter {
	String filter;
	String desc;
	
	public CSVFilter(){
		desc = "CSV Files";
		filter = ".csv";
	}
	
	public boolean accept(File f) {
		String fname = f.getName().toLowerCase();
        return f.isDirectory() || fname.endsWith(filter);
	}

	public String getDescription() {
        return desc;
	}
}
