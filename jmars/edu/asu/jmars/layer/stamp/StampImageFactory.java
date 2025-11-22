package edu.asu.jmars.layer.stamp;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.HashMap;

import javax.imageio.ImageIO;

import edu.asu.jmars.layer.stamp.networking.StampLayerNetworking;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.asu.msff.Stamp;

/**
 * Factory for creating instances of StampImages from image data contained in
 * files, URLs, and any {@link Stamp} which contains a valid URL. 
 */
public class StampImageFactory
{
    private static final DebugLog log = DebugLog.instance();
       
    /**
     * Loads image from URL reference contained in {@link Stamp} instance; 
     * pops up progress monitor dialog as needed.  Supports
     * specialized tile-based caching mechanism used in {@link MocImage}.
     * 
     * @param s stamp containing valid URL.
     */
    public static StampImage load(StampShape s, String instrument, String type)
    {
        StampImage stampImage = null;

        // Fetch the projection params up front, so we can check for cases that need special handling
        HashMap<String,String> projectionParams = StampLayerNetworking.getProjectionParams(s.getId(), instrument, type);
        
        if (!projectionParams.containsKey("specialHandling")) {
        	return new StampImage(s, s.getId(), instrument, type, null, projectionParams);
        }
        
        // Currently only THEMIS and NEW_THEMIS stamps should make it this far

    	String urlStr="ImageServer?instrument="+instrument+"&id="+s.getId()+"&imageType="+type;
    	
    	String cacheFileName = urlStr;

    	File cacheFile = new File(StampCache.getSrcLocation(cacheFileName));

        try {
	        if (projectionParams.containsKey("pdsImage")) {
	        	if (!StampCache.srcCacheExists(cacheFileName)) {
	    			StampCache.writeSrc(StampLayer.queryServer(urlStr), cacheFileName);
	        	}
	        	        	
	        	// TODO: Used to hardcode THEMIS.  Do we still want to?
	        	return new PdsImage(s, cacheFile, s.getId(), type, instrument, projectionParams);            	
	        }

	        // Remaining types that potentially get here: NEW_THEMIS: BWS, RGB, D*, BEQR, BSNU, BPOL
	        // THEMIS: BWS, RGB, D*
        	BufferedImage image = StampCache.readSrc(cacheFileName, false);
        	
        	if (image==null) {            	
        		image = loadImage(urlStr, projectionParams.containsKey("numeric"));
        		if (image!=null) {
        			StampCache.writeSrcImmediately(image, cacheFileName);
        		}
        	}
            
        	if (projectionParams.containsKey("themisDCS")) {  // THEMIS DCS images, IR only
        		return new THEMISImage(s, cacheFile, s.getId(), type, "THEMIS", projectionParams);
        	}
        	
            if (image != null) {
           		stampImage = new StampImage(s, s.getId(), instrument, type, image, projectionParams);
            }
            return stampImage;
        }
        catch (Throwable e) {
            log.aprintln(e);
        }
        
        return stampImage;
    }    
                    
    /**
     * Loads image from URL
     * 
     * @param url Any valid URL supported by {@link URL} class.
     */
    protected static BufferedImage loadImage(String urlStr, boolean numeric)
    {
        BufferedImage image = null;
        
        try {
      		image = ImageIO.read(StampLayer.queryServer(urlStr));

            // Skipping this causes non-numeric, non-alpha images to look washed out      		
    		if (!numeric && image.getAlphaRaster()==null) {
    			image = Util.makeBufferedImage(image);
    		}

        } catch (Exception e) {
        	e.printStackTrace();
            log.println(e);
            return null;
        }
            
        return image;
    }
    
    /**
     * Used for pulling SHARAD radar images from the server.  Takes the 
     * url string and numeric boolean and checks the stamp cache first to 
     * see if the image is already located there.  If not, pulls from the 
     * server and saves to the cache for future efficiency.
     * @param urlStr  String passed to the stamp server
     * @param numeric  Whether the expected data is numeric or not
     * @return  A BufferedImage of whatever is returned from the stamp server
     */
    public static BufferedImage getImage(String urlStr, boolean numeric){
    	BufferedImage image = null;
    	
    	image = StampCache.readSrc(urlStr, numeric);
    	
    	if (image==null) {            	
    		image = loadImage(urlStr, numeric);
    		if (image!=null) {
    			StampCache.writeSrcImmediately(image, urlStr);
    		}
    	}

    	return image;
    }
               
}
