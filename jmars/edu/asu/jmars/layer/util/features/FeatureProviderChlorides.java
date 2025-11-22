package edu.asu.jmars.layer.util.features;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import edu.asu.jmars.Main;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.HttpRequestType;

public class FeatureProviderChlorides implements FeatureProvider {
	private static final Object lock = new Object();
	private static final String base = Main.getJMarsPath() + "shapes" + File.separator;
    private static DebugLog log = DebugLog.instance();
    
	public String getDescription() {
		return "Osterloo et al., 2010 Chloride Survey"; 
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
		    JmarsHttpRequest request = null;
			try {
				String url = "http://jmars.mars.asu.edu/static/chlorides.zip";
	            request = new JmarsHttpRequest(url, HttpRequestType.GET);
	            boolean successful = request.send();
				if (successful) {
    	            ZipInputStream zis = new ZipInputStream(request.getResponseAsStream());
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
				} else {
				    log.println("Could not open: "+url);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			    request.close();
			}
		}
		
		return new FeatureProviderSHP().load(base + "Chlorides" + File.separator + "chloride_survey_final.polygon.shp");
	}
	
	public int save(FeatureCollection fc, String fileName) {
		throw new UnsupportedOperationException("This is not supported");
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
    
 
}
