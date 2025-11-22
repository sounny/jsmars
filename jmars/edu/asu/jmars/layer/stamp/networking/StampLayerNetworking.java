package edu.asu.jmars.layer.stamp.networking;

import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HttpRequestType;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.asu.msff.DataField;
import edu.asu.msff.util.QueryTemplate;

/**
 * This class encapsulates all of the information related to communication with the backend StampServer.
 * 
 * Servlet names, parameter formatting, etc should largely be isolated to this class, and not scattered across other parts of the stamp
 * layer.
 * 
 * @author sdickens
 *
 */
public class StampLayerNetworking {
    private static DebugLog log = DebugLog.instance();
  
    private static final String version = "3.11";
    private static final String jmarsVersion = Util.getVersionNumber();
    private static final String jreName = System.getProperty("java.runtime.name");
    private static final String jreVendor = System.getProperty("java.vm.vendor");
    private static final String osName = System.getProperty("os.name");
    private static final String osArch = System.getProperty("os.arch");
    private static final String osVersion = System.getProperty("os.version");
    public static final String versionStr="&version="+version+"&jmars_timestamp="+Main.ABOUT().SECS+"&jmars_version="+jmarsVersion +
    		"&java_runtime_name="+jreName+"&java_vm_vendor="+jreVendor+"&os_name="+osName+"&os_arch="+osArch+"&os_version="+osVersion;
    
    public static final String stampURL = Config.get("stamps.url");
    
    /**
     * Asks the backend StampServer for a list of stamp instruments available for the current body.
     * In addition, the default set of display columns is also returned for each instruments
     * @return instruments and default columns
     */
    public static HashMap<String, String[]> getInstruments() {
    	HashMap<String, String[]> layerTypeMap = new HashMap<String, String[]>();
    	
		String body=Config.get(Util.getProductBodyPrefix() + Config.CONFIG_BODY_NAME, "Mars");
		String urlStr = "InstrumentFetcher?planet="+body+"&format=JAVA";
		try {
			ObjectInputStream ois = new ObjectInputStream(queryServer(urlStr));
			layerTypeMap = (HashMap<String, String[]>) ois.readObject();
			ois.close();
		} catch (Exception e) {
			log.aprintln("Error retrieving list of stamps");
			log.aprintln(e);
		}
		return layerTypeMap;
    }
    
    /**
     * Asks the backend StampServer for the misc parameters associated with a specified instrument.
     * 
     * Examples includes plot units, urls for web links, etc.
     * 
     * @param instrument parameters
     * @return
     */
    public static HashMap<String,String> fetchParams(String instrument) {
		String urlStr = "ParameterFetcher?instrument="+instrument+"&format=JAVA";
		HashMap<String, String> newParams;
		
		try {
			ObjectInputStream ois = new ObjectInputStream(queryServer(urlStr));
			newParams = (HashMap<String, String>) ois.readObject();
			ois.close();
		} catch (Exception e) {
			log.aprintln("Unable to retrieve layer parameters");
			newParams = new HashMap<String,String>();
		}
		
		return newParams;
    }

    /**
     * Asks the backend StampServer for the query templates available for this instrument.
     * 
     * @param instrument query templates
     * @return
     */
    public static ArrayList<QueryTemplate> retrieveQueryTemplates(String instrument) {
		String urlStr = "RetrieveQueryTemplates?instrument="+instrument+"&format=JAVA";
		ArrayList<QueryTemplate> newTemplates;
		
		try {
			ObjectInputStream ois = new ObjectInputStream(queryServer(urlStr));
			newTemplates = (ArrayList<QueryTemplate>) ois.readObject();
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();

			log.aprintln("Unable to retrieve query templates");
			newTemplates = new ArrayList<QueryTemplate>();
		}
		
		return newTemplates;
    }

    
    private static String authStr = null;
    
    /**
     * Generates and returns the authentication string that should be sent to the backend StampServer
     * @return auth String
     */
    public static String getAuthString() {
    	if (authStr==null) {   		
    		if (Config.get("sendStampCredentials", true)) {
		        String user = Main.USER;
		        String pass = Main.PASS;
		        String domain = Main.AUTH_DOMAIN;
		        String product = Main.PRODUCT;
		
		        // No longer hash passwords.  They're sent via https instead.
		        // This allows our verification scripts to authenticate against
		        // the various sources we have to try for authentication.  ie. 
		        // mysql, LDAP, etc.	
		        authStr = "&user="+user+"&password="+pass+"&product="+product+"&domain="+domain;
    		} else {
    			// For environments such as the LROC SOC, the backend doesn't check credentials, so don't bother sending
    			// 'plaintext' passwords across the network.  The data should be communicated via https and as a result be
    			// secure, but error messages on one side or the other can inadvertently output passwords.  We still send
    			// the real userid to allow for debugging of issues in the logfiles.
    			authStr = "&user="+Main.USER+"&password="+"dummy";
    		}
    	}
        return authStr;
    }
                	
	private static final int DEFAULT_TIMEOUT = 60;

	// TODO: Change so this can be private
	public static InputStream queryServer(String urlStr) throws IOException, URISyntaxException {
		return queryServer(urlStr, DEFAULT_TIMEOUT*20);
	}

	// TODO: Change so this can be private
	static InputStream queryServer(String urlStr, int TIMEOUT) throws IOException, URISyntaxException {
		int idx = urlStr.indexOf("?");
		
		String connStr = urlStr.substring(0,idx+1);
		
		String data = urlStr.substring(idx+1);

		// Strip off a passed in stampURL, just in case we have one saved from an old save file
		connStr = connStr.replace(stampURL, "");
		
    	connStr = connStr.replaceAll(".*StampFetcher\\?","StampFetcher?");
    	connStr = connStr.replace("?", "");

		return queryServer(connStr, data, TIMEOUT);

	}

	private static InputStream queryServer(String urlStr, String data) throws IOException, URISyntaxException {
		return queryServer(urlStr, data, DEFAULT_TIMEOUT);
	}
	
	private static InputStream queryServer(String urlStr, String data, int TIMEOUT) throws IOException, URISyntaxException {
		urlStr = stampURL + urlStr;
		data = data + getAuthString() + versionStr;
		
		// Send parameters via POST to avoid the limit on parameter length with GET
        JmarsHttpRequest request = new JmarsHttpRequest(urlStr, HttpRequestType.POST);

        // Connect timeout and SO_TIMEOUT of 10 seconds
        request.setConnectionTimeout(TIMEOUT*1000);
        request.setReadTimeout(TIMEOUT*1000);
        		
        for (String param : data.split("&")) {
        	String[] nvp = param.split("=");
        	String key = nvp[0];
        	String val = (nvp.length >= 2) ? nvp[1] : ""; 
        	request.addRequestParameter(key, val);
        }
        
        request.send();
        
        InputStream returnStream = request.getResponseAsStream();
        
	    /*
         * We are going to read the entire input so that we can properly close the HTTP
         * connection.  Then, we will return the captured input as a stream.
         */
        byte[] capturedInputStream = IOUtils.toByteArray(returnStream);
        request.close();
        
		return new ByteArrayInputStream(capturedInputStream);		
	}
				
	/**
	 * Given an image id, instrument, and imageType, queries the backend StampServer to retrieve the corner points that should be used
	 * for image rendering.  These may or may not have any relation to any of the outline points.  For projected images, these points will
	 * frequently fall completely outside the area covered by the stamp outline itself.
	 * 
	 * Generally, if four points are returned, the results are interpreted as the four corner points of a rectangular image.
	 * 
	 * If a multiple of four points are returned, the results are interpreted as corners of multiple frames (see: many THEMIS image types)
	 * 
	 * Non-image based stamp layers may interpret these results differently.  ie. SHARAD/Radar type layers use this to get a higher resolution
	 * version of the ground track.
	 * 
	 * @param id
	 * @param instrument
	 * @param imageType
	 * @return An array of lon,lat pairs representing the points to use when drawing a stamp image
	 */
    public static double[] getPoints(String id, String instrument, String imageType) {
    	double dpts[] = new double[0];
    	
		try {
			String urlStr = "PointFetcher?id="+id+"&instrument="+instrument;
			
			if (imageType!=null && imageType.length()>0) {
				urlStr+="&imageType="+imageType;
			}
        
			ObjectInputStream ois = new ObjectInputStream(StampLayerNetworking.queryServer(urlStr));
    
			dpts = (double[])ois.readObject();
		               		   
		} catch (Exception e) {
			e.printStackTrace();
		}    		
    	
    	return dpts;
    }
	
    /**
     * Retrieves the full resolution size of this image from the StampServer.  This is used while breaking a large image into smaller pieces that
     * can be requested from the backend in a hopefully efficient manner.
     * 
     * @param id
     * @param instrument
     * @param imageType
     * @return int array containing number of lines in the first element and number of samples in the second
     */
    public static int[] getFullImageSize(String id, String instrument, String imageType) {
    	int size[]=new int[2];
    	
    	size[0]=0;
    	size[1]=1;
    	
		try {
			String sizeLookupStr = "ImageSizeLookup?id="+id+"&instrument="+instrument+"&imageType="+imageType+"&format=JAVA";
					
			ObjectInputStream ois = new ObjectInputStream(StampLayerNetworking.queryServer(sizeLookupStr));
			
			Integer samples = (Integer)ois.readObject();
			Long lines = (Long)ois.readObject();
			
			// Not sure why I ever thought lines needed to be a Long.  An image that's 2 BILLION lines long is... big.
			size[0]=lines.intValue();
			size[1]=samples.intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return size;
    }
	
    /**
     * Given an image id, instrument, and imageType, queries the backend StampServer to return a list of the projection parameters
     * needed to render the image.  These parameters can include things like projection type (UNPROJECTED, CYLINDRICAL, SINUSOIDAL, STEREOGRAPHIC), 
     * resolution, scale, line_projection_offset, etc.
     * 
     * @param id
     * @param instrument
     * @param imageType
     * @return a hashmap of projection parameter names and values
     */
    public static HashMap<String, String> getProjectionParams(String id, String instrument, String imageType) {
    	HashMap<String, String> projectionParams = new HashMap<String, String>();
    	
		try {
			String paramLookupStr = "ProjectionParamLookup?id="+id+"&imageType="+imageType+"&instrument="+instrument+"&format=JAVA";
					
			ObjectInputStream ois = new ObjectInputStream(StampLayerNetworking.queryServer(paramLookupStr));
			
			projectionParams = (HashMap<String,String>)ois.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return projectionParams;
    }
    
    /**
     * Given a stamp id and instrument, returns the east-lon and lat offset applied by this particular user via stamp nudging
     * 
     * @param id
     * @param instrument
     * @return east-lon and lat offset
     */
    public static Point2D.Double loadOffset(String id, String instrument) {
    	Point2D.Double offset = null;
    	
		String body=Config.get(Util.getProductBodyPrefix() + Config.CONFIG_BODY_NAME, "Mars");

    	try
    	{    		
            String urlStr = "OffsetFetcher?stamp="+id;
            
            urlStr+="&body="+body;
            
            urlStr+="&instrument="+instrument;
                            
            ObjectInputStream ois = new ObjectInputStream(StampLayerNetworking.queryServer(urlStr));

            double pt[] = (double[]) ois.readObject();
            offset = new Point2D.Double(pt[0], pt[1]);
            ois.close();
    	}
    	catch(Exception e)
    	{
    		log.aprintln(e.getMessage());
    	}
    	
    	return offset;
    }
    
    /**
     * Sends the provided offset to the backend StampServer to be saved for this stamp id and instrument, for the particular user logged
     * into JMARS.
     * 
     * @param id
     * @param instrument
     * @param offset (east-lon, lat)
     */
    public static void saveOffset(String id, String instrument, Point2D offset) {
		try
		{
			String body=Config.get(Util.getProductBodyPrefix() + Config.CONFIG_BODY_NAME, "Mars");

			final DecimalFormat decimalFormatter = new DecimalFormat(".########");

			String updateStr = "OffsetUpdater?stamp="+id;
				
			updateStr += "&xoffset="+decimalFormatter.format(offset.getX())+"&yoffset="+decimalFormatter.format(offset.getY());
				
            updateStr+="&body="+body;
	            
            updateStr+="&instrument="+instrument;

            StampLayerNetworking.queryServer(updateStr);
		}
		catch(Exception e)
		{
			log.aprintln(e.getMessage());
		}
    }
    
    public static DataField[] fetchFields(String instrument, String layerGroup) {
    	DataField datafields[] = new DataField[0];
		try {
			String urlStr = "FieldFetcher?instrument="+instrument;
			if (layerGroup!=null&&layerGroup.length()>0) urlStr+="&group="+layerGroup;

			ObjectInputStream ois = new ObjectInputStream(StampLayerNetworking.queryServer(urlStr));

			datafields = (DataField[])ois.readObject();

			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	
    	return datafields;
    }
    
    public static List<String> getImageTypes(String instrument, String imagesToLookup) {
    	String urlStr = "ImageTypeLookup?instrument="+instrument+"&format=JAVA";;
    	
    	List<String> imageTypes = new ArrayList<String>();
    	
		try {						
			ObjectInputStream ois = new ObjectInputStream(queryServer(urlStr, "id="+imagesToLookup));

			imageTypes = (List<String>)ois.readObject();

			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return imageTypes;
    }
        
    public static String getBrowseURL(String id, String instrument, int num) {
    	String url = null;
		try {
			String browseLookupStr = "BrowseLookup?id="+id+"&instrument="+instrument+"&format=JAVA";
					
			ObjectInputStream ois = new ObjectInputStream(queryServer(browseLookupStr));
			
			// Get the num-th URL
			for (int i=0; i<num; i++) {
				url = (String)ois.readObject();
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
		return url;
    }
    
    static HashMap<String,ArrayList<String>> map2Units = new HashMap<String,ArrayList<String>>();
    static HashMap<String,ArrayList<Integer[]>> map2Colors = new HashMap<String,ArrayList<Integer[]>>();
    
    // either of these 2 methods should populate the return values for the other as well
    public synchronized static ArrayList<String> getUnitsForGeologicMap(String mapId) {
    	if (!map2Units.containsKey(mapId)) {
			String body=Config.get(Util.getProductBodyPrefix() + Config.CONFIG_BODY_NAME, "Mars");
			
	    	String urlStr = "LegendFetcher?body="+body+"" +
	    			"&mapName=" + mapId +
	    			"&format=JAVA";;
	    	
			try {						
				ObjectInputStream ois = new ObjectInputStream(queryServer(urlStr));
	
				ArrayList<String> strVals = (ArrayList<String>)ois.readObject();
				ArrayList<Integer[]> mapVals = (ArrayList<Integer[]>)ois.readObject();
				
				map2Units.put(mapId, strVals);
				map2Colors.put(mapId, mapVals);
				ois.close();
			} catch (Exception e) {
				e.printStackTrace();
				map2Units.put(mapId,  new ArrayList<String>());
				map2Colors.put(mapId,  new ArrayList<Integer[]>());
			}
    	}
    	return map2Units.get(mapId);
    }
    
    public synchronized static ArrayList<Integer[]> getColorMapForGeologicMap(String mapId) {
    	if (!map2Units.containsKey(mapId)) {
			String body=Config.get(Util.getProductBodyPrefix() + Config.CONFIG_BODY_NAME, "Mars");
			
	    	String urlStr = "LegendFetcher?body="+body+"" +
	    			"&mapName=" + mapId +
	    			"&format=JAVA";;
	    	
			try {						
				ObjectInputStream ois = new ObjectInputStream(queryServer(urlStr));
	
				ArrayList<String> strVals = (ArrayList<String>)ois.readObject();
				ArrayList<Integer[]> mapVals = (ArrayList<Integer[]>)ois.readObject();
				
				map2Units.put(mapId, strVals);
				map2Colors.put(mapId, mapVals);
				ois.close();
			} catch (Exception e) {
				e.printStackTrace();
				map2Units.put(mapId,  new ArrayList<String>());
				map2Colors.put(mapId,  new ArrayList<Integer[]>());
			}
    	}
    	return map2Colors.get(mapId);    	
    }
    
    public static String[] getDefaultColumns(String instrument) {
    	String[] defaults = new String[0];
    	
		String body=Config.get(Util.getProductBodyPrefix() + Config.CONFIG_BODY_NAME, "Mars");
		
    	String urlStr = "PreferenceFetcher?body="+body+"" +
    			"&layerType=stamp" +
    			"&layerName="+instrument+
    			"&prefName=displayColumns"+
    			"&format=JAVA";;
    	
		try {						
			ObjectInputStream ois = new ObjectInputStream(queryServer(urlStr));

			String valStr = (String)ois.readObject();
			ois.close();

			if (valStr!=null && valStr.trim().length()>0) {
				defaults = valStr.split(",");
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}

    	return defaults;
    }
    
    public static void setDefaultColumns(String instrument, String newDefaults[]) {
		String body=Config.get(Util.getProductBodyPrefix() + Config.CONFIG_BODY_NAME, "Mars");
		
		String defaults="";
		
		// TODO: Add a utility method to do this right
		for (String nextDefault : newDefaults) {
			defaults += nextDefault + ",";
		}
		
		if (defaults.endsWith(",")) {
			defaults = defaults.substring(0, defaults.length()-1);
		}
		
    	String urlStr = "PreferenceUpdater?body="+body+"" +
    			"&layerType=stamp" +
    			"&layerName="+instrument+
    			"&prefName=displayColumns"+
    			"&prefValue="+defaults;
    	
		try {						
			// Perform the update and close the returned stream
			queryServer(urlStr).close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public static ArrayList<String> getLayerInfoById(String layerId) {
		String body=Config.get(Util.getProductBodyPrefix() + Config.CONFIG_BODY_NAME, "Mars");

		String urlStr = "LayerInfoFetcher?body="+body+"&layerId="+layerId;
		
		return fetchInfo(urlStr);
    }

    public static ArrayList<String> getLayerInfoByTypeAndKey(String layerType, String layerKey) {
		String body=Config.get(Util.getProductBodyPrefix() + Config.CONFIG_BODY_NAME, "Mars");
		
		String urlStr = "LayerInfoFetcher?body="+body+"&layerType="+layerType+"&layerKey="+layerKey;
		
		return fetchInfo(urlStr);
    }

    private static ArrayList<String> fetchInfo(String urlStr) {
		ArrayList<String> fields = new ArrayList<String>();
		
		try {
			int idx = urlStr.indexOf("?");

			String connStr = LayerParameters.paramsURL + urlStr.substring(0,idx);

			String data = urlStr.substring(idx+1)+getAuthString()+StampLayer.versionStr;
			
			JmarsHttpRequest req = new JmarsHttpRequest(connStr, HttpRequestType.POST);
			req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
			req.addOutputData(data);
			req.setConnectionTimeout(10*1000);
			req.setReadTimeout(10*1000);
			req.send();
			ObjectInputStream ois = new ObjectInputStream(req.getResponseAsStream());
			fields = (ArrayList<String>) ois.readObject();
			ois.close();
			req.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error retrieving layer parameters");
			fields = new ArrayList<String>();
		}    	
		
		return fields;    	    	
    }    
}
