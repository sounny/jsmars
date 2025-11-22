package edu.asu.jmars.viz3d.core.geometry;

/**
 * Class to represent a Ray (an infinite directed line segment) in 3D
 *
 * thread-safe
 */
public class Ray {
	
	// The direction of the ray.
	float[] direction;
	// The origin of the ray.
	float[] origin;
	// end point representing the frustum far plane intersection
	float[] end;

	/** 
	 * Constructor
	 * @param origin 3D point of ray origin
	 * @param direction 3D vector of the ray direction
	 */
	public Ray (float[] origin, float[] direction) {
		this.origin = origin;
		this.direction = direction;
	}

	public Ray (float[] origin, float[] direction, float[] end) {
		this.origin = origin;
		this.direction = direction;
		this.end = end;
	}

	/**
	 * Returns the direction of the Ray
	 *
	 * @return float[X, Y, Z]
	 *
	 * thread-safe
	 */
	public float[] getDirection() {
		return direction;
	}
	
	/** 
	 * Sets the direction of the Ray
	 *
	 * @param direction float[X, Y, Z]
	 *
	 * thread-safe
	 */
	public void setDirection(float[] direction) {
		this.direction = direction;
	}
	
	/**
	 * Returns the point of Ray origin
	 *
	 * @return float[X, Y, Z]
	 *
	 * thread-safe
	 */
	public float[] getOrigin() {
		return origin;
	}
	
	public float[] getEnd() {
		return end;
	}
	
	/**
	 * Sets the point of origin of the Ray
	 *
	 * @param origin float[X, Y, Z]
	 *
	 * thread-safe
	 */
	public void setOrigin(float[] origin) {
		this.origin = origin;
	}

	@Override
	public String toString() {
		return ""+origin[0]+", "+origin[1]+", "+origin[2]+"\n"+direction[0]+", "+direction[1]+", "+direction[2]+"\n";
	}
	
	/**
	 * Convenience method for debug
	 *
	 * thread-safe
	 */
	public void print() {
		System.err.format("\n%11.9f %11.9f %11.9f %11.9f %11.9f %11.9f\n", origin[0], origin[1], origin[2], direction[0], direction[1], direction[2]);
	}
}