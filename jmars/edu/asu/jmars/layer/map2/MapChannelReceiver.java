package edu.asu.jmars.layer.map2;

/**
 * Implemented by any class that wishes to receive MapData updates from a
 * MapChannel
 */
public interface MapChannelReceiver {
	/**
	 * MapChannel provides MapData to implementors of this interface.
	 * 
	 * This method is always invoked on the AWT event thread.
	 * 
	 * The the resulting MapData object will have a null image if a fatal error
	 * occurred at any point (like the MapSource was invalid or a Stage threw an
	 * exception.)
	 */
	public void mapChanged(MapData mapData);
}
