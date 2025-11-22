package edu.asu.jmars.viz3d.renderer.gl.event;

import edu.asu.jmars.viz3d.spatial.RayWithEpsilon;

/**
 * Representation of the result of mouse point query in 3D
 * Results should represent the intersection of a ray from the mouse pointer to the first
 * contact location of a LineSegment along the JOGL camera line of sight. 
 *
 * thread-safe
 */

public class LineIntersectResult {
	private float lat;
	private float lon;
	private int id;
	private int index;
	private float percent;
	private boolean wasClicked = false;
	private boolean wasControlDown = false;
	private RayWithEpsilon ray;
	
	/**
	 * Method to inform the latitude of the intersection point 
	 *
	 * @return latitude
	 *
	 * thread-safe
	 */
	public float getLatitude() {
		return lat;
	}
	
	/**
	 * Method to set the latitude of the intersection point 
	 *
	 * @param lat latitude
	 *
	 * thread-safe
	 */
	public void setLatitude(float lat) {
		this.lat = lat;
	}
	
	/**
	 * Method to inform the longitude of the intersection point 
	 *
	 * @return longitude
	 *
	 * thread-safe
	 */
	public float getLongitude() {
		return lon;
	}
	
	/**
	 * Method to set the latitude of the intersection point 
	 *
	 * @param lat latitude
	 *
	 * thread-safe
	 */
	public void setLongitude(float lon) {
		this.lon = lon;
	}
	
	/**
	 * Method to retrieve the ID of the facet containing the intersection point
	 *
	 * @return facet ID
	 *
	 * thread-safe
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Method to set the facet ID
	 *
	 * @param id
	 *
	 * thread-safe
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	/**
	 * @return Whether this intersect result was generated from a click
	 */
	public boolean wasClicked(){
		return wasClicked;
	}
	
	/**
	 * Set whether this intersect result was generated from a click
	 * @param clicked  True if from click
	 */
	public void setClicked(boolean clicked){
		wasClicked = clicked;
	}
	
	/**
	 * @return Whether this intersect result was while the control button was down
	 */
	public boolean wasControlDown(){
		return wasControlDown;
	}
	
	/**
	 * Set whether this intersect result was generated with control button down
	 * @param control
	 */
	public void setControlDown(boolean control){
		wasControlDown= control;
	}

    public void setFacetId(int id2) {
        // TODO Auto-generated method stub
        
    }

    public float getPercent() {
        return percent;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

	public RayWithEpsilon getRayWithEpsilon() {
		return ray;
	}

	public void setRayWithEpsilon(RayWithEpsilon ray) {
		this.ray = ray;
	}
}
