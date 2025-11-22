package edu.asu.jmars.viz3d.core.geometry;

import java.util.ArrayList;

/**
 * Class to represent a linked list of vertices (corners) where the line
 * segments of a polygon intersect a facet (triangle) One instance of this class
 * will be one node in a linked list of "Corners"
 * 
 * thread-safe
 */
public class Corner implements Intersection {
	/** The vertex represented by this Corner **/
	public float[] location;
	/** The point at which a line segment enters the facet **/
	public ArrayList<float[]> startIntersects = new ArrayList<>();
	/** The point at which a line segment exits the facet **/
	public ArrayList<float[]> endIntersects = new ArrayList<>();
	/**
	 * The direction from the vertex represented by this Corner to the start
	 * intersection of the line segment
	 **/
	public float[] startDirection;
	/**
	 * The direction from the vertex represented by this Corner to the end
	 * intersection of the line segment
	 **/
	public float[] endDirection;
	/**
	 * Flag to indicate if there is a start intersection associated with this
	 * Corner
	 **/
	public boolean start;
	/**
	 * Flag to indicate if there is an end intersection associated with this
	 * Corner
	 **/
	public boolean end;
	/** link to the previous corner in the list **/
	public Corner prev;
	/** link to the next Corner is the list **/
	public Corner next;

	/**
	 * Convenience method to traverse to the end of the list
	 * 
	 * @return The last Corner in the list
	 * 
	 *         thread-safe
	 */
	public Corner getEnd() {
		Corner end = this;
		while (end.next != null) {
			end = end.next;
		}
		return end;
	}
	
	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public float[][] getPoints() {
		ArrayList<float[]> points = new ArrayList<>();

		// get all the Corner associated intersects
		if (endIntersects.size() > 0) {
			points.add(endIntersects.get(0));
		}
		points.add(location);
		if (startIntersects.size() > 0) {
			points.add(startIntersects.get(0));
		}
		
		Corner c  = next; 
		do {
			if (c == null) {
				break;
			}
			if (c.endIntersects.size() > 0) {
				points.add(c.endIntersects.get(0));
			}
			points.add(c.location);
			if (c.startIntersects.size() > 0) {
				points.add(c.startIntersects.get(0));
			}
			c = c.next;
		} while (c != null);
		
		float[][] intersectingPoints = new float[points.size()][];
		for (int i = 0; i < points.size(); i++) {
			intersectingPoints[i] = points.get(i);
		}
		return intersectingPoints;
	}
}
