package edu.asu.jmars.viz3d.core.geometry;

public interface Intersection {
	
	/**
	 * Method to return the intersecting poins in array form
	 * @return a float[?][3] array containing the intersection points in CCW order
	 */
	public abstract float[][] getPoints();

}
