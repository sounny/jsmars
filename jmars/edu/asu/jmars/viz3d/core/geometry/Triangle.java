package edu.asu.jmars.viz3d.core.geometry;

import java.io.Serializable;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;

/**
 * This class represents a 3D triangle or facet. Primarily for use in a loose
 * Octtree.
 * 
 * thread-safe
 */
public class Triangle implements Comparable<Triangle>, Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6152542744348326858L;
	/** The DSk or Ancillary FITS ID of the triangle **/
	public int id = -1;
	/**
	 * The ID of the original triangle (facet) this triangle was subdivided from
	 **/
	public int parentId = -1;
	/**
	 * The points that define the triangles vertices. 3x3 array that must be in
	 * counter-clockwise winding order
	 **/
	public float[][] points;
	/**
	 * Link to next triangle in a list. Intended use is in a loose Octtree where
	 * 2 Triangles map to a single node
	 **/
	public Triangle next;
	/** Optional reference to the containing OctTree node **/
	public ONode node;
	/** A scratch variable for comparing one Triangle to another **/
	public volatile float compareVal;
	

	private float[] center;
	private float[] minPt;
	private float[] maxPt;
	private float radius = Float.MIN_VALUE;
	private float[] intersection = new float[3];
	
	private boolean isCorner = false;

	/**
	 * Convenience constant to avoid division by 3. Primarily of use in
	 * computing the center of the Triangle
	 **/
	public static final float ONE_THIRD = 0.33333333333333333f;

	/**
	 * Constructor
	 * 
	 * @param points
	 *            3x3 array that must be in counter-clockwise winding order.
	 * 
	 * @throws IllegalArgumentException
	 */
	public Triangle(float[][] points) throws IllegalArgumentException {
		if (points == null || points.length != 3 || points[0].length != 3) {
			throw new IllegalArgumentException(
					"Invalid float[][] used to initialize edu.asu.jmars.viz3d.octtree.Triangle");
		}
		this.points = points;
	}

	/**
	 * Computes the "radius" or distance from the center of the triangle to the
	 * farthest vertex
	 * 
	 * @returns distance from center to farthest vertex
	 **/
	public float getRadius() {
		if (Float.compare(radius, Float.MIN_VALUE) == 0 && points != null) { // radius
																				// hasn't
																				// been
																				// calculated
																				// yet
			float dist = 0f;
			for (int i = 0; i < 3; i++) {
				float temp = VectorUtil.distSquareVec3(getCenter(), points[i]);
				if (temp > dist) {
					dist = temp;
				}
			}
			radius = FloatUtil.sqrt(dist);
		}
		return radius;
	}

	/**
	 * Lazily calculates and returns the the "center" of the Triangle. This
	 * value is merely the average of the vertices.
	 * 
	 * @return
	 * 
	 *         thread-safe
	 */
	public float[] getCenter() {
		if (center == null && points != null) {
			float x = 0, y = 0, z = 0;
			for (int i = 0; i < 3; i++) {
				x += points[i][0];
				y += points[i][1];
				z += points[i][2];
			}
			center = new float[] { x * ONE_THIRD, // avg of x coords
					y * ONE_THIRD, // avg of y coords
					z * ONE_THIRD }; // avg of z coords
		}
		return center;
	}

	@Override
	public String toString() {
//		StringBuffer buf = new StringBuffer();
//		buf.append("\n");
//
//		for (int i = 0; i < 3; i++) {
//			buf.append(points[i][0]);
//			buf.append(" ");
//			buf.append(points[i][1]);
//			buf.append(" ");
//			buf.append(points[i][2]);
//			buf.append("\n");
//		}
//		return buf.toString();
		return prettyPrint();
	}

	/**
	 * Convenience method to print out the vertices for debug purposes.
	 * 
	 * <thread-safe?>
	 */
	public void print() {
		for (int i = 0; i < 3; i++) {
			System.err.format("%12.12f, %12.12f, %12.12f\n", points[i][0],
					points[i][1], points[i][2]);
		}
		System.err.format("%12.12f, %12.12f, %12.12f\n", points[0][0],
				points[0][1], points[0][2]);
		System.err.println();
		System.err.println();
		System.err.println();
		System.err.println();
		System.err.println();
	}

	public String prettyPrint() {
		StringBuffer buf = new StringBuffer();
//		buf.append("\n");
		for (int i = 0; i < 3; i++) {
			buf.append(String.format("%12.12f, %12.12f, %12.12f\n", points[i][0],
					points[i][1], points[i][2]));
		}
		buf.append(String.format("%12.12f, %12.12f, %12.12f\n", points[0][0],
				points[0][1], points[0][2]));
		buf.append("\n\n\n\n");
		return buf.toString();
	}

	/**
	 * Returns the facet ID of the Triangle or the facet ID of its parent
	 * Triangle if it is the result of a Triangle subdivision. If the Triangle
	 * did not originate from a Spice DSK or an Ancillary FITS file
	 * Integer.MAX_VALUE will be returned.
	 * 
	 * thread-safe
	 */
	public int getMeshId() {
		if (id < 0) {
			if (parentId < 0) {
				return Integer.MAX_VALUE;
			} else {
				return parentId;
			}
		} else {
			return id;
		}
	}

	/**
	 * Returns the System generated identity hash code for this instance of a
	 * Triangle.
	 * 
	 * @return int
	 * 
	 *         thread-safe
	 */
	public int getId() {
		return System.identityHashCode(this);
	}

	/**
	 * Returns the smallest Axis Aligned Bounding Box this triangle will fit
	 * inside. Intended for use in an octtree data structure.
	 * 
	 * @return 2x3 array containing a minimum point consisting of the smallest
	 *         X, Y, and Z values in the triangle and a maximum point consisting
	 *         of the largest X, Y, and Z values in the triangle.
	 * 
	 *         thread-safe
	 */
	public float[][] getMinBoundingBox() {
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float minZ = Float.MAX_VALUE;
		float maxX = -Float.MIN_VALUE;
		float maxY = -Float.MIN_VALUE;
		float maxZ = -Float.MIN_VALUE;

		for (int i = 0; i < points.length; i++) {
			if (points[i][0] < minX)
				minX = points[i][0];
			if (points[i][0] > maxX)
				maxX = points[i][0];
			if (points[i][1] < minY)
				minY = points[i][1];
			if (points[i][1] > maxY)
				maxY = points[i][1];
			if (points[i][2] < minZ)
				minZ = points[i][2];
			if (points[i][2] > maxZ)
				maxZ = points[i][2];
		}
		minPt = new float[] { minX, minY, minZ };
		maxPt = new float[] { maxX, maxY, maxZ };
		return new float[][] { minPt, maxPt };
	}

	/**
	 * Lazily returns the minimum point in the minimum Axis Aligned Bounding Box
	 * for this triangle.
	 * 
	 * @return float[X, Y, Z]
	 * 
	 *         thread-safe
	 */
	public float[] getMinPoint() {
		if (minPt == null) {
			getMinBoundingBox();
		}
		return minPt;
	}

	/**
	 * Lazily returns the maximum point in the minimum Axis Aligned Bounding Box
	 * for this triangle.
	 * 
	 * @return float[X, Y, Z]
	 * 
	 *         thread-safe
	 */
	public float[] getMaxPoint() {
		if (maxPt == null) {
			getMinBoundingBox();
		}
		return maxPt;
	}

	/**
	 * Returns the point of intersection IF a Ray was successfully intersected with the Triangle
	 *
	 * @return the intersection point if valid, otherwise the origin (0,0,0)
	 */
	public float[] getIntersection() {
		return intersection;
	}

	/**
	 * Sets the point where a Ray has intersected the triangle
	 *
	 * @param intersection the point of intersection
	 */
	public void setIntersection(float[] intersection) {
		this.intersection = intersection;
	}

	@Override
	public int compareTo(Triangle o) {		
		return Float.compare(compareVal, o.compareVal);
	}
	
	public boolean hasVertexOnPole() {
		if (this.points == null) {
			return false;
		} else {
			return OctTree.isPole(this.points[0]) ^ OctTree.isPole(this.points[1]) ^ OctTree.isPole(this.points[2]);
		}
	}
	
	public boolean isCorner() {
		return isCorner;
	}

	public void setCorner(boolean isCorner) {
		this.isCorner = isCorner;
	}

	public void delete() {
		if (next != null) {
			next.delete();
		}
		next = null;
		center = null;
		intersection = null;
		maxPt = null;
		minPt = null;
		points = null;
	}
	
	public Triangle cloneWithScalar(float scalar) {
		float[][] pts = new float[3][3];
		for (int i=0; i<3; i++) {
			pts[i][0] = points[i][0] * scalar;
			pts[i][1] = points[i][1] * scalar;
			pts[i][2] = points[i][2] * scalar;
		}
		Triangle tri = new Triangle(pts);
		tri.id = id;
		tri.parentId = parentId;
		return tri;		
	}

}
