package edu.asu.jmars.layer.map2;

import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;

public class MapServerDefault {
	/** The name of the default map server */
	public static String DEFAULT_NAME = Config.get(Util.getProductBodyPrefix() + "map.default.server");// @since change bodies - added prefix
	
	/**
	 * This method is to udpate the default server name when the body is changed
	 * @since change bodies
	 */
	public static void updateDefaultName() {
		DEFAULT_NAME = Config.get(Util.getProductBodyPrefix() + "map.default.server");//@since change bodies
	}
	
	
}
