package edu.asu.jmars.viz3d.core.geometry;

import java.util.ArrayList;

/**
 * Class to contain all the data (outline points and triangles) created by
 * fitting a polygon to the surface of a shape model.
 * 
 * thread-safe
 */
public class FittedPolygonData {
	private ArrayList<float[]> points = new ArrayList<>();
	private ArrayList<Triangle> tris = new ArrayList<>();

	private float[] inputPts = null;

	/**
	 * Contructor
	 * 
	 * @param inPts
	 *            the points of the polygon to be fit to a shape model, must be
	 *            in counter-clockwise winding order (X1, Y1, Z1, X2, Y2, Z2...)
	 */
	public FittedPolygonData(float[] inPts) {
		inputPts = inPts;
	}

	/**
	 * Returns the fitted outline points of the polygon
	 * 
	 * @return float[]
	 * 
	 *         thread-safe
	 */
	public ArrayList<float[]> getPoints() {
		return points;
	}

	/**
	 * Sets the points of the polygon to be fitted.
	 * 
	 * @param points
	 *            must be in counter-clockwise winding order (X1, Y1, Z1, X2,
	 *            Y2, Z2...)
	 * 
	 *            thread-safe
	 */
	public void setPoints(ArrayList<float[]> points) {
		this.points = points;
	}

	/**
	 * Returns the triangles created by tessellating the polygon to fit a shape
	 * model
	 * 
	 * @return all the triangles constituting the polygon
	 * 
	 *         thread-safe
	 */
	public ArrayList<Triangle> getTris() {
		return tris;
	}

	/**
	 * Sets the triangles created by fitting the polygon to a shape model
	 * 
	 * @param tris
	 * 
	 *            thread-safe
	 */
	public void setTris(ArrayList<Triangle> tris) {
		this.tris = tris;
	}

	/**
	 * Returns the original points of the polygon
	 * 
	 * @return float[]
	 * 
	 *         thread-safe
	 */
	public float[] getInputPts() {
		return inputPts;
	}

}
