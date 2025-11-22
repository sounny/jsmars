package edu.asu.jmars.viz3d.core.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jogamp.opengl.math.VectorUtil;

/**
 * Container class for edu.asu.jmars.viz3d.core.geometry.Corner and
 * edu.asu.jmars.viz3d.core.geometry.IntersectingLine objects as well as the
 * edu.asu.jmars.viz3d.core.geometry.Triangle object they are associated with.
 * This class is intended to be used in the tessellation process used to fit
 * polygons to a shape model.
 * 
 * thread-safe
 */
public class TriLineIntersect {
	/** The Triangle object an instance of this class is applied to **/
	private Triangle tri;
	/**
	 * List of all the line segments that completely cross this associated
	 * Triangle
	 **/
	private ArrayList<IntersectingLine> intersects = new ArrayList<>();
	/**
	 * Flag to indicate whether this TriLineIntersect and its associated
	 * Triangle have been processed at least once.
	 **/
	public boolean marked;
	/** Flag to indicate whether this instance contains any Corner objects **/
	public boolean hasCorner;
	public boolean hasIntersection;
	/**
	 * List of all the polygon vertices that fall within the associated Triangle
	 **/
	private ArrayList<Corner> corners = new ArrayList<>();
	/* An ordered list of Intersection objects. */
	private ArrayList<Intersection> intersections = new ArrayList<>();
	/**
	 * Convenience array to store all polygon vertices that intersect the
	 * associated Triangle outside of their Corner objects
	 **/
	private float[][] intersectingPoints = null;

	/**
	 * Constructor
	 * 
	 * @param tri
	 *            the Triangle to associate with this instance
	 * 
	 * @throws IllegalArgumentException
	 */
	public TriLineIntersect(Triangle tri) {
		if (tri == null) {
			throw new IllegalArgumentException(
					"edu.asu.jmars.viz3d.octtree.TriLineIntersect null constructor argument");
		}
		this.tri = tri;
	}

	/**
	 * Returns the last Corner added
	 * 
	 * @return last corner added
	 * 
	 *         thread-safe
	 */
	public Corner getCurrentCorner() {
		if (corners.size() > 0) {
			return corners.get(corners.size() - 1);
		} else {
			return null;
		}
	}

	/**
	 * Returns the contained Corner that maps to the input 3D coordinate
	 * 
	 * @param loc
	 *            the 3D coordinate (X, Y, Z) of the desired Corner
	 * 
	 * @return the corner that maps to the input location
	 * 
	 *         thread-safe
	 */
	public Corner getCornerByLoc(float[] loc) {
		for (Corner c : corners) {
			while (c != null) {
				if (VectorUtil.isVec3Equal(c.location, 0, loc, 0)) {
					return c;
				}
				c = c.next;
			}
		}
		return null;
	}

	/**
	 * Removes an input Corner from the associated corners
	 * 
	 * @param corner
	 * 
	 * @return true if the desired Corner was found and removed successfully
	 * 
	 *         not thread-safe
	 */
	public boolean removeCornerIndex(Corner corner) {
		for (int i = 0; i < corners.size(); i++) {
			Corner c = corners.get(i);
			if (c == corner) {
				corners.remove(i);
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all the 3D locations of all the points of the
	 * Triangle where line segments intersect
	 * 
	 * @return [?][3] array of edge and interior intersection points
	 * 
	 *         not thread-safe
	 */
	public float[][] getIntersectingPoints() {
		if (intersectingPoints != null) {
			return intersectingPoints;
		}
		ArrayList<float[]> points = new ArrayList<>();

		// get all the Corner associated intersects
		for (Corner c : corners) {
			while (c != null) {
				if (c.endIntersects.size() > 0) {
					points.add(c.endIntersects.get(0));
				}
				points.add(c.location);
				if (c.startIntersects.size() > 0) {
					points.add(c.startIntersects.get(0));
				}
				c = c.next;
			}
		}
		// get all the line segment associated intersects
		for (IntersectingLine inter : intersects) {
			points.add(inter.startPt);
			points.add(inter.endPt);
		}

		float[][] intersectingPoints = new float[points.size()][];
		for (int i = 0; i < points.size(); i++) {
			intersectingPoints[i] = points.get(i);
		}
		return intersectingPoints;
	}
	
	public float[][] getIntersections() {
		ArrayList<float[]> points = new ArrayList<>();
		if (!intersections.isEmpty()) {
			for (Intersection inter : intersections) {
				if (inter instanceof Corner) {
					Corner c = (Corner)inter;
					while (c != null) {
						if (c.endIntersects.size() > 0) {
							points.add(c.endIntersects.get(0));
						}
						points.add(c.location);
						if (c.startIntersects.size() > 0) {
							points.add(c.startIntersects.get(0));
						}
						c = c.next;
					}					
				} else if (inter instanceof IntersectingLine) {
					IntersectingLine line = (IntersectingLine)inter;
					points.add(line.startPt);
					points.add(line.endPt);
				}
			}
		}
		float[][] intersectingPoints = new float[points.size()][];
		for (int i = 0; i < points.size(); i++) {
			intersectingPoints[i] = points.get(i);
		}
		return intersectingPoints;
	}
	
	/**
	 * Returns all the 3D locations of all the points of the
	 * Triangle where line segments intersect in the order they
	 * were added.
	 * 
	 * @return [?][3] array of edge and interior intersection points
	 * 
	 *         not thread-safe
	 */
	public float[][] getOrderedIntersectingPoints() {
		ArrayList<float[]> points = new ArrayList<>();
		// get all the line segment associated intersects
//		for (IntersectingLine inter : intersects) {
//			points.add(inter.startPt);
//			points.add(inter.endPt);
//		}
		float[][] intersectingPoints = new float[points.size()][];
		for (int i = 0; i < points.size(); i++) {
			intersectingPoints[i] = points.get(i);
		}
		return intersectingPoints;		
	}
	
	/**
	 * Method to return all the corners that have been associated with a Triangle
	 * in the order they were created.
	 *
	 * @return the ordered list of Corners
	 */
	public float[][] getOrderedCorners() {
		ArrayList<float[]> points = new ArrayList<>();

		// get all the Corner associated intersects
		for (Corner c : corners) {
			while (c != null) {
				if (c.endIntersects.size() > 0) {
					points.add(c.endIntersects.get(0));
				}
				points.add(c.location);
				if (c.startIntersects.size() > 0) {
					points.add(c.startIntersects.get(0));
				}
				c = c.next;
			}
		}

		float[][] intersectingPoints = new float[points.size()][];
		for (int i = 0; i < points.size(); i++) {
			intersectingPoints[i] = points.get(i);
		}
		return intersectingPoints;
	}
	
	public Triangle getTriangle() {
		return tri;
	}
	
	public void addIntersectingLine(IntersectingLine line) {
		intersects.add(line);
		intersections.add(line);
	}
	
	public int getNumIntersectingLines() {
		return intersects.size();
	}
	
	public List<IntersectingLine> getIntersectingLines() {
		return Collections.unmodifiableList(intersects);
	}
	
	public List<Intersection> getIntersectionObjs() {
		return Collections.unmodifiableList(intersections);
	}
	
	public void addCorner(Corner c) {
		corners.add(c);
		intersections.add(c);
	}
	
	public int getNumCorners() {
		return corners.size();
	}
	
	public List<Corner> getCorners() {
		return Collections.unmodifiableList(corners);
	}
 }
