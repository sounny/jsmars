package edu.asu.jmars.layer.stamp;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriterSpi;
import edu.asu.jmars.Main;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

public class StampCache implements Runnable {
	private static final String SEPARATOR = System.getProperty("file.separator");
    private static final String STAMP_CACHE = Main.getJMarsPath() + "stamps" + SEPARATOR;
    private static final String STAMP_SRC_CACHE = Main.getJMarsPath() + "stamps" + SEPARATOR + "src" + SEPARATOR;
    private static final DebugLog log = DebugLog.instance();
    
    static {
    	recreateCacheDirectories();
    }
    
	static ExecutorService pool;

	BufferedImage image=null;
	String filename=null;

	StampCache(String name, BufferedImage data) {
		image=data;
		filename=name;
	}
	
	public void queueProcessing() {
		synchronized (this) {
			if (pool == null) {
				int procs = Math.max(2, Runtime.getRuntime().availableProcessors());
				
				pool = Executors.newFixedThreadPool(procs, new StampThreadFactory("Projected Stamp Writer"));
			}
			
			pool.execute(this);
		}
	}
	
	 public StampCache() {
	 }
		
	public void run() {
        write(image, filename);
	}
		
	public static BufferedImage read(String filename, boolean numeric) {
    	BufferedImage image = null;
    	
    	File cachedFile = new File(filename);
    	
    	int retryCnt=0;
    	
    	while(retryCnt<5) {
	    	if (cachedFile.exists() && cachedFile.canRead()) {
		    	try {
					image = ImageIO.read(cachedFile);
					
	        		if (!numeric && image.getAlphaRaster()==null) {
	        			image = Util.makeBufferedImage(image);
	        		}
	
				} catch (Exception e) {
					// With multiple threads operating at once, it's possible to encounter errors reading partially written tif files
					// In this case, we'll try to wait half a second and try again.  But if that fails a number of times in a row,
					// we'll abort and print an error like normal
		    		retryCnt++;
		    		if (retryCnt>=5) {
		    			e.printStackTrace();
		    		} else {	
			    		try {
			    			Thread.sleep(500);
			    		} catch (Exception e2) {
			    			e2.printStackTrace();
			    		}
		    		}
				}
	    	}	 
	    	break;
    	}
    	
		return image;
	}
	
	public static BufferedImage readProj(String filename, boolean numeric) {
		return read(STAMP_CACHE + stripFilename(filename), numeric);
	}
	
	public static BufferedImage readSrc(String filename, boolean numeric) {
		return read(STAMP_SRC_CACHE + stripFilename(filename), numeric);
	}
	
	/**
	 * Check to see if a file has been cached previously and already exists
	 * @param fileName  Filename to look for
	 * @return True if the file exists
	 */
	public static boolean fileExists(String fileName){
		File test = new File(STAMP_SRC_CACHE + stripFilename(fileName));
		return test.exists();
	}
		
	// Performs a synchronous write
	public static void write(BufferedImage image, String filename) {
		if (image==null || filename==null) return;
		
		ImageWriter writer = null;

		try {
			ImageWriteParam param = null;

			if (writer==null) {
				writer = new TIFFImageWriterSpi().createWriterInstance();
			}
			
			if (param==null) {
				param = writer.getDefaultWriteParam();
				
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionType("LZW");

				param.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
				// 256 x 256 tiles seem like a reasonable default, but maybe something based on the overall size of the image would be better?
				param.setTiling(256, 256, 0 , 0);				
			}

            File outputFile = new File(filename);

            // This block is necessary in the event that some of the JMARS cache directories go away during execution.
            // This can happen if the user clicks the 'Manage Cache' option.
            String parentStr = outputFile.getParent();
            if (parentStr!=null) {
            	File parentDir = new File(parentStr);
            	if (parentDir!=null && !parentDir.exists()) {
            		parentDir.mkdirs();
            	}
            }

			ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile);
			writer.setOutput(ios);
			writer.write(null, new IIOImage(image,null,null), param);
			ios.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer!=null) writer.dispose();
		}
	}
	
	
	/* Takes a byte stream and writes it directly to disk without any conversions.  
	 * Useful for PDS files that can't be directly read by ImageIO. 
	 */
	public static void write(InputStream imageStream, String filename) {
		try {
			FileOutputStream fileStream = new FileOutputStream(filename);
			
	        byte[] temp = new byte[40960];
	        
	        int count;
	        while((count = imageStream.read(temp)) >= 0) {
	        	fileStream.write(temp, 0, count);
	        }
	
	        imageStream.close();
	        fileStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void writeSrc(InputStream imageStream, String filename) {
		write(imageStream, STAMP_SRC_CACHE + stripFilename(filename));
	}

	/*
	 * Accepts an image and a unique file identifier, and ASYNCHRONOUSLY writes the image out 
	 * to the cache location for non-JMARS projected source Stamp data.
	 */
	public static void writeSrc(BufferedImage image, String filename) {
		new StampCache(STAMP_SRC_CACHE + stripFilename(filename), image).queueProcessing();		
	}
	
	/*
	 * Accepts an image and a unique file identifier, and SYNCHRONOUSLY writes the image out 
	 * to the cache location for non-JMARS projected source Stamp data.
	 */
	public static void writeSrcImmediately(BufferedImage image, String filename) {
		write(image, STAMP_SRC_CACHE + stripFilename(filename));		
	}
	
	/*
	 * Accepts an image and a unique file identifier, and ASYNCHRONOUSLY writes the image out 
	 * to the cache location for JMARS projected Stamp data.
	 */
	public static void writeProj(BufferedImage image, String filename) {
		new StampCache(STAMP_CACHE + stripFilename(filename), image).queueProcessing();		
	}

	public static boolean srcCacheExists(String stampKey) {
		try {
			File testFile =  new File(STAMP_SRC_CACHE + stripFilename(stampKey));
			if (testFile.exists() && testFile.canRead() && testFile.length()>0) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static String getSrcLocation(String stampKey) {
		return STAMP_SRC_CACHE + stripFilename(stampKey);
	}
	
	public static String getProjLocation(String stampKey) {
		return STAMP_CACHE + stripFilename(stampKey);		
	}
	
	public static void recreateCacheDirectories() {
    	File cache = new File(STAMP_CACHE);
    	if (!cache.exists()) {
    		if (!cache.mkdirs()) {
    			System.err.println("Unable to create stamp cache directory, check permissions in " + Main.getJMarsPath());
    		}
    	} else if (!cache.isDirectory()) {
    		System.err.println("Stamp cache cannot be created, found regular file at " + STAMP_CACHE);
    	}
    	cache = new File(STAMP_SRC_CACHE);
    	if (!cache.exists()) {
    		if (!cache.mkdirs()) {
    			System.err.println("Unable to create stamp source cache directory, check permissions in " + Main.getJMarsPath());
    		}
    	} else if (!cache.isDirectory()) {
    		System.err.println("Stamp source cache cannot be created, found regular file at " + STAMP_CACHE);
    	}		
	}
	
	public static void cleanCache() {
		Util.recursiveRemoveDir(new File(STAMP_CACHE));
		Util.recursiveRemoveDir(new File(STAMP_SRC_CACHE));
		
		recreateCacheDirectories();
	}
	
	/*
	 * Remove troublesome characters from the stamp id or URL before using it to read or write to cache files.
	 */
    private static String stripFilename(String filename)
    {
    	// These three are probably not needed anymore...
    	filename=filename.replace(StampLayer.stampURL, "");
        filename=filename.replace(StampLayer.versionStr, "");
        filename=filename.replace(StampLayer.getAuthString(), "");
        filename=filename.replace("/", "");
        filename=filename.replace("&","");
        filename=filename.replace("http:", "");
        filename=filename.replace("\\?", "");
        filename=filename.replace("?", "");
        filename=filename.replace("=", "");
        filename=filename.replace(":","");
        filename=filename.replace("-","");
        
        return filename;
    }
    
  
}

