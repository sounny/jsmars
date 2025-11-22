package edu.asu.jmars.layer.map2;

import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;

public class MapSourceDefault {
	/** The name of the default map source */
	public static String DEFAULT_NAME = Config.get(Util.getProductBodyPrefix() + "map.default.source");// @since change bodies - added prefix
	public static String DEFAULT_PLOT = Config.get(Util.getProductBodyPrefix() + "map.default.plot");
	
	/**
	 * This method is to udpate the default server name when the body is changed
	 * @since change bodies
	 */
	public static void updateDefaultName() {
		DEFAULT_NAME = Config.get(Util.getProductBodyPrefix() + "map.default.source");//@since change bodies
		DEFAULT_PLOT = Config.get(Util.getProductBodyPrefix() + "map.default.plot");
	}
	
	
}
