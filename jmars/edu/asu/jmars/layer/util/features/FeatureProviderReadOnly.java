package edu.asu.jmars.layer.util.features;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JOptionPane;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

public class FeatureProviderReadOnly implements FeatureProvider {
	private static final Object lock = new Object();
	private static final String base = Main.getJMarsPath() + "shapes" + File.separator;
	private static DebugLog log = DebugLog.instance();
	
	private String directory;
	private String file; //.shp
	private String urlString;
	
	public FeatureProviderReadOnly(String dir, String fileName, String URL){
		directory=dir;
		file=fileName;
		urlString=URL;
	}
	
	  public FeatureProviderReadOnly() {
	  }
	
	public String getDirectory() {
		return this.directory;
	}
	public String getFile() {
		return this.file;
	}
	public String getUrlString() {
		return this.urlString;
	}
	public String getDescription() {
		return null;
	}

	public File[] getExistingSaveToFiles(FeatureCollection fc, String baseName) {
		return null;
	}

	public String getExtension() {
		return null;
	}

	public boolean isFileBased() {
		return false;
	}

	public boolean isRepresentable(FeatureCollection fc) {
		return false;
	}
	
	public FeatureCollection load(String fileName) {
		synchronized(lock) {
			File out = null;
			try {
				URL url = new URL(urlString);
				ZipInputStream zis = new ZipInputStream(url.openStream());
				for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
					long time = entry.getTime();
					out = new File(base + entry.getName());
					if (entry.isDirectory()) {
						if (out.exists() && !out.isDirectory()) {
							//the parent directory exists but is not a directory, give the user the option to delete the file.
							int returnVal = Util.showConfirmDialog("<html>The file: "+out.getPath()+" will prevent this layer from being loaded."
									+ "<br />Would you like to delete this file and continue? ", "Confirm Delete", JOptionPane.YES_NO_OPTION);
							if (returnVal == JOptionPane.YES_OPTION) {
								out.delete();
								continue;//parent directories get created below, move on to the next iteration in the loop.
							}
						}
					} else if (!out.exists() || out.lastModified() != time) {
						out.getParentFile().mkdirs();
						File parentDir = out.getParentFile();
						if (!parentDir.exists()) {
							//we were unable to create the necessary directory. Fail 
							Util.showMessageDialog("<html>JMARS was unable to create a necessary directory <br />"
									+ "in the "+base+" directory.<br />"
									+ "This could be a permissions issue. Please try again or contact support.", "ERROR", JOptionPane.YES_NO_OPTION);
							log.println("Unable to create parent directory for shape layer zip file load.");
							break;
						} else if (!parentDir.isDirectory()){
							//the parent directory exists but is not a directory, give the user the option to delete the file.
							int returnVal = Util.showConfirmDialog("<html>The file: "+parentDir.getPath()+" will prevent this layer from being loaded."
									+ "<br />Would you like to delete this file and continue? ", "Confirm Delete", JOptionPane.YES_NO_OPTION);
							if (returnVal == JOptionPane.YES_OPTION) {
								parentDir.delete();
								out.getParentFile().mkdirs();
							} //if the user does not delete the file, we will likely fail in the following lines.
						}
						FileOutputStream fos = new FileOutputStream(out);
						Util.copy(zis, fos);
						fos.flush();
						fos.close();
					}
					out.setLastModified(time);
					
				}
			} catch (Exception e) {
				log.println("Exception while trying to unzip Shape Layer file: "+urlString);
				log.println(e.getMessage());
				Util.showMessageDialog("<html>There was a problem loading the files for this layer.<br />"
						+ "The file: "+out.getName()+" could not be created.<br />"
						+ "Error: "+e.getMessage()+"<br />"
						+ "If the problem persists, please contact support. <br />"
						+ "</html>", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		if(file.contains(".csv")){
			return new FeatureProviderCSV().load(base + directory + File.separator + file);
		}
		
		return new FeatureProviderSHP(false).load(base + directory + File.separator + file);
	}
	
	public int save(FeatureCollection fc, String fileName) {
		throw new UnsupportedOperationException("This is not supported");
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }    
 
}

