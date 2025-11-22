package edu.asu.jmars.viz3d.renderer.gl.event;

/**
 * Representation of the the eye to center of shape model vector in 3D
 * Results should represent the intersection of a ray from the camera to the 
 * center of the shape model along the JOGL camera line of sight. 
 *
 * thread-safe
 */

public class SynchronizeResult {
	private float lat;
	private float lon;
	
	/**
	 * Method to inform the latitude of the eye to center of body vector 
	 *
	 * @return latitude
	 *
	 * thread-safe
	 */
	public float getLatitude() {
		return lat;
	}
	
	/**
	 * Method to set the latitude of the eye to center of body vector 
	 *
	 * @param lat latitude
	 *
	 * thread-safe
	 */
	public void setLatitude(float lat) {
		this.lat = lat;
	}
	
	/**
	 * Method to inform the longitude of the eye to center of body vector 
	 *
	 * @return longitude
	 *
	 * thread-safe
	 */
	public float getLongitude() {
		return lon;
	}
	
	/**
	 * Method to set the latitude of the eye to center of body vector 
	 *
	 * @param lat latitude
	 *
	 * thread-safe
	 */
	public void setLongitude(float lon) {
		this.lon = lon;
	}
	
}
