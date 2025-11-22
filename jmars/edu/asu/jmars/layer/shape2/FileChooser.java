package edu.asu.jmars.layer.shape2;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import java.io.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import edu.asu.jmars.Main;

import edu.asu.jmars.layer.util.features.FeatureProvider;
import edu.asu.jmars.util.Util;

// for maintaining the files used in both SAVING and LOADING.
public class FileChooser extends JFileChooser {
	// Note: the parent directory is maintained so that each time the user
	// selects a file, the directory
	// that was current that last time the chooser was used is brought back up.
	private File startingDir = null;

	private Window parent = null;

	// Gets file(s) to be used for saving or loading.  "action" should be either "Save" or "Load".
	// If getting a file for loading, more than one file can be returned.  If saving, only one file
	// will be returned.
	public File[] chooseFile(Component c, String action) {
		parent = Util.getDisplayFrame(c);
		return chooseFile(action, null);
	}

	public File[] chooseFile(String action, String fileName) {
		// move to the previously used directory if any
		if (startingDir == null) {
			startingDir = new File(Util.getDefaultFCLocation());
		}
		setCurrentDirectory(startingDir);

		// select user specified file
		if (fileName != null) {
			setSelectedFile(new File(fileName));
		}

		// User can load multiple files, but can save only one
		boolean loadMode = action.equalsIgnoreCase("Load");
		boolean multiSelectionEnabled = loadMode;
		setMultiSelectionEnabled(multiSelectionEnabled);
		
		setApproveButtonText(action.toUpperCase());
		setToolTipText(action + " Shape File");
		setDialogTitle(action + " Shape File");

		int chooserResult = loadMode? showOpenDialog(parent): showSaveDialog(parent);

		if (chooserResult != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		if (this.getFeatureProvider() == null) {
			Util.showMessageDialog("Must specify a file format!");
			return null;
		}

		// get the file(s), set the new parent (for next time), and return the selected file(s).

		// save this directory for the next time this dialog is popped up
		startingDir = getCurrentDirectory();

		// get selected files and process them one by one
		File[] files = loadMode? getSelectedFiles(): new File[] { getSelectedFile() };

		if (files.length > 0) {
			setSelectedFile(files[0]);

			// Make sure each File has the extension for the selected provider
			String ext = this.getFeatureProvider().getExtension();
			for (int i = 0; i < files.length; i++)
				if (!files[i].getName().endsWith(ext))
					files[i] = new File( files[i].getPath() + ext);
		}

		return files;
	}

	public File getStartingDir() {
		return startingDir;
	}
	
	public void setStartingDir(File dir) {
		if (dir.exists() && dir.isDirectory() && dir.canRead()) {
			startingDir = dir;
		} else {
			throw new IllegalArgumentException("Starting directory must be a readable directory");
		}
	}
	
	/**
	 * Wraps the given feature provider in a Filter for the file chooser, adds
	 * it and returns it so clients can set the current filter if desired
	 */
	public FileFilter addFilter(FeatureProvider fp) {
		UniqueFilter f = new UniqueFilter(fp);
		addChoosableFileFilter(f);
		return f;
	}

	/**
	 * Returns the FeatureProvider selected from the file type combo box.
	 */
	public FeatureProvider getFeatureProvider(){
		FileFilter ff = getFileFilter();
		if (ff instanceof UniqueFilter){
			UniqueFilter uff = (UniqueFilter)ff;
			return uff.getFeatureProvider();
		}
		return null;
	}

	private static class UniqueFilter extends FileFilter {
		private FeatureProvider fp;

		public UniqueFilter(FeatureProvider fp) {
			this.fp = fp;
		}

		public FeatureProvider getFeatureProvider() {
			return fp;
		}

		public boolean accept(File f) {
			String fname = f.getName().toLowerCase();
			return f.isDirectory() || fname.endsWith(fp.getExtension());
		}

		public String getDescription() {
			return fp.getDescription();
		}
	}

} // end: class FileChooser
