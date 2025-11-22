package edu.asu.jmars.layer.threed;

import java.awt.Color;
import java.util.HashMap;

import javax.vecmath.Vector3f;

import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

/**
 * Serialiable object to save 3D layer construction state to a JMARS session file.
 * 
 * Since the MapServer/MapSource objects are static data recreated when JMARS is
 * restarted, we store the server name and source name as unique identifiers and
 * look them up during layer reconstruction.
 * 
 * IMPORTANT! do not change the order or content of the instance variables, or
 * users won't be able to restore saved 3D layer sessions.
 */
public class StartupParameters extends HashMap<String,String> implements SerializedParameters {

	private static final long serialVersionUID = 1L;
	private transient DebugLog log = DebugLog.instance();

	/** should the directional light be on? */
    boolean             directionalLightBoolean   = false;
    
    /** the direction of the directional light.*/
    Vector3f            directionalLightDirection = new Vector3f(0.0f, 0.0f, 1.0f);

    /** the color of the directional light */
    Color               directionalLightColor     = new Color(128,128,128);

    /** the color of the background. */
    Color               backgroundColor           = new Color(0, 0, 0);

    /** the initial exaggeration of the scene. */
    String              zScaleString              = "1.0";

    /** should a backplane be defined? */
    boolean             backplaneBoolean          = false;
    
    /** angle of rotation about x axis */
	float 				alpha 					  = 0f; 
	
	/** angle rotation about y axis */
	float 				beta 					  = 0f;	
	
	/** angle rotation about the z axis */
	float 				gamma 					  = 0f; 
	
	/** zoom level */
	float 				zoomFactor 				  = 0.88f;

	/** translation along x axis */
    float 				transX 					  = 0.0f;
    
	/** translation along y axis */
    float 				transY 					  = 0.0f;

	/** translation along z axis */
    float 				transZ 					  = 0.0f;
	
    // JNN: added
    /** pan along x axis */
    float				xOffset					  = 0.0f;
    
    /** pan along y axis */
    float				yOffset					  = 0.0f;	
    
    int					scalingMode				  = 0; // scale using range of values
    
    String				scaleMode				  = ThreeDCanvas.SCALE_MODE_AUTO_SCALE;
    
    final float			scaleOffsetThisBody	      = (float) Config.get(Util.getProductBodyPrefix()+Config.CONFIG_THREED_SCALE_OFFSET, -0.002f);

    float				scaleOffset				  = scaleOffsetThisBody;

    boolean             scaleUnitsInKm            = (scaleOffsetThisBody < 0.1) ? true : false;
    	
    String				ipaddress = "";
    
	/** Initializes the elevation data from the jmars.config 'threed.default_elevation*' keys */
	public StartupParameters() {
	    String defaultServer = Config.get("threed.default_elevation.server");
	    String defaultSource = Config.get("threed.default_elevation.source");
		put("serverName", Config.get(Util.getProductBodyPrefix() + "threed.default_elevation.server", defaultServer));
		put("sourceName", Config.get(Util.getProductBodyPrefix() + "threed.default_elevation.source", defaultSource));
	}
	
	public MapSource getMapSource() {
		MapServer server = MapServerFactory.getServerByName(get("serverName"));
		if (server == null) {
			log.aprintln("Elevation server not accessible");
			return null;
		}
		
		MapSource source = server.getSourceByName(get("sourceName"));
		if (source == null) {
			log.aprintln("Elevation source not accessible");
			return null;
		}
		
		return source;
	}
	
	public void setMapSource(MapSource source) {
		put("serverName", source.getServer().getName());
		put("sourceName", source.getName());
	}
}
