package edu.asu.jmars.viz3d.core.geometry;

/**
 * Convenience class to represent a line segment which crossed a triangle
 * The line segment must not have an end point that intersects the triangle
 *
 * thread-safe
 */
public class IntersectingLine implements Intersection {
	/** The point at which the line segment first intersects the triangle **/
	public float[] startPt = new float[3];
	/** The point at which the line segment last intersects the triangle **/
	public float[] endPt = new float[3];
	@Override
	public float[][] getPoints() {
		return new float[][] {startPt, endPt};
	}
	
	public int hashCode() {
		return System.identityHashCode(this);
	}

}
