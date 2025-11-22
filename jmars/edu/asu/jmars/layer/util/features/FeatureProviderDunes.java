package edu.asu.jmars.layer.util.features;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import edu.asu.jmars.Main;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

public class FeatureProviderDunes implements FeatureProvider{
	private static final Object lock = new Object();
	private static final String base = Main.getJMarsPath() + "shapes" + File.separator;
	 private static DebugLog log = DebugLog.instance();
	
	public String getDescription() {
		return "USGS Mars Dune Database"; 
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
			try {
				ZipInputStream zis = new ZipInputStream(Main.getResourceAsStream("resources/Dune_Field.zip"));
				for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
					long time = entry.getTime();
					File out = new File(base + entry.getName());
					if (!out.exists() || out.lastModified() != time) {
						out.getParentFile().mkdirs();
						FileOutputStream fos = new FileOutputStream(out);
						Util.copy(zis, fos);
						fos.flush();
						fos.close();
						out.setLastModified(time);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return new FeatureProviderSHP(false).load(base + "Dune_Field" + File.separator + "Dune_Field.shp");
	}
	
	public int save(FeatureCollection fc, String fileName) {
		throw new UnsupportedOperationException("This is not supported");
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
  
}

