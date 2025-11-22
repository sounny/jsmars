package edu.asu.jmars.viz3d.renderer.gl.event;

import edu.asu.jmars.viz3d.renderer.gl.scene.Scene;

/**
 * Listener for {@link Scene} intersect events 
 *
 */
public interface IntersectListener {
	/**
	 * Method interface for a Scene implementation to send IntersectResults back to registered listeners.
	 *
	 * @param result
	 */
	public void setResults(IntersectResult result);
	
}
