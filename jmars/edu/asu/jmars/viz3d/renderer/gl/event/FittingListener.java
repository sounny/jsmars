package edu.asu.jmars.viz3d.renderer.gl.event;

import edu.asu.jmars.viz3d.renderer.gl.scene.Scene;

/**
 * Listener for {@link Scene} fitting state changes 
 *
 */
public interface FittingListener {
	/**
	 * Method interface for a Scene implementation to send 3D Polygon/OutLine fitting state to registered listeners.
	 *
	 * @param result
	 */
	public void setResults(boolean isFittingEnabled);
	
}

