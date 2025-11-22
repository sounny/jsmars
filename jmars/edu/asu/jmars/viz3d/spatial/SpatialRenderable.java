package edu.asu.jmars.viz3d.spatial;


import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.geom.Frustum;

// Object used in the edu.asu.jmars.viz3d.spatial.Octree
public interface SpatialRenderable {
	
	/**
	 * Return the points that define this object as an array of 3 element arrays.
	 * LineSegment Example: {{7, 4, 3}, {9, 0, 6}} where the three element arrays represent {x, y, z} 
	 * components in 3D.
	 * Single point objects will only return one 3 element array {{7, 4, 3}}|
	 */
	public abstract float[][] getPoints();
	
	/** clean up any resources
	 */
	public abstract void finalize();
	
	/**
	 * This method will return the "center of the 3D object"
	 * Examples: Points will return the point itself
	 * LineSegments will return the mid-point of the segment
	 * Cuboids will return the center of the Cuboid
	 * Triangles will return the geometric center of the triangle
	 * Polygons will return the average of the defining points
	 * Spheres will return the center of the Sphere
	 */
	public abstract float[] getCenter();
	
	/**
	 * The method returns the distance from getCenter to the object point
	 * most distant from the center.
	 */
	public abstract float getRadius();
	
	/**
	 * This method should return the Octtree node the object was inserted to in the 
	 * edu.asu.jmars.viz3d.spatial.Octree. If this object has not been inserted into
	 * an Octtree, this method will return null.
	 */
	public Node<SpatialRenderable> getNode();
	
	/**
	 * This method will set the edu.asu.jmars.viz3d.spatial.Octree node the object has
	 * been inserted into.
	 * 
	 * @param t edu.asu.jmars.viz3d.spatial.Node<>
	 */
	public void setNode(Node<?> t);
	
	/**
	 * This method will render the object in the 3D window if the object has been passed to
	 * ThreeDManager
	 * 
	 * @param gl The graphics context
	 */
	public void render(GL2 gl);
	
	/**
	 * This method intersects a RayWithEpsilon object with the SpatialRenderable.
	 * If an intersection is found, true is returned and the passed in IntersectResult is populated.
	 * @param ray
	 * @return a non-null and populated IntersectResultData if successful
	 */
	public IntersectResultData<SpatialRenderable> intersectRay(RayWithEpsilon ray);
	
	/**
	 * This method intersects a plane defined frustum with the object
	 * @param normal a surface normal vector to used to determine whether the 
	 * object is facing the camera "end" of the frustum
	 * @param frustum a 6 plane defined frustum where each plane is defined as a
	 * normal vector and a point on the plane. Each row of the frustum is a plane
	 * Example of a row: {Nx, Ny, Nz, P} 
	 * @return true if the frustum contains all or part of the object is inside
	 * the frustum
	 */
	public boolean intersectFrustum(float[] normal, float[][]frustum);
	
	/**
	 * This method intersects the object with a com.jogamp.opengl.math.geom.Frustum
	 * @param normal a surface normal vector to used to determine whether the 
	 * object is facing the camera "end" of the frustum
	 * @param frustum com.jogamp.opengl.math.geom.Frustum
	 * @return true if the frustum contains all or part of the object is inside
	 * the frustum
	 */
	public boolean intersectJoglFrustum(float[] normal, Frustum frustum);

	/** Method to retrieve the current color of the SpatialLRenderable
	 * 
	 * @param color 3 element array {R, G, B}
	 */
	public void setColor(float[] color);

	/** Method to retrieve the current color of the SpatialLRenderable
	 * 
	 * @return return the color as a 3 element array {R, G, B}
	 */
	public float[] getColor();

}
