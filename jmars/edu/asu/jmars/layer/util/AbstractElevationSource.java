package edu.asu.jmars.layer.util;

import edu.asu.jmars.util.HVector;

/**
 * Provides an implementation of the
 * {@link ElevationSource#getSurfacePoint(HVector, HVector)} method, which is
 * reasonably fast as long as calls to {@link ElevationSource#getMinElevation()}
 * , {@link ElevationSource#getMaxElevation()}, and
 * {@link ElevationSource#getElevation(HVector)} are fast.
 */
public abstract class AbstractElevationSource implements ElevationSource {
	private static boolean DEBUG = false;
	
	private static HVector getSphereRadii(double val){
		return new HVector(val,val,val);
	}
	
	public double[] getElevation(HVector[] v) {
		double[] alts = new double[v.length];
		for(int i=0; i<v.length; i++){
			alts[i] = getElevation(v[i]);
		}
		return alts;
	}
	
	/**
	 * Computes the ground-location where the spacecraft is looking towards. The
	 * location is based on the elevation profile of the planet as provided by the
	 * elevation-source.
	 * 
	 * @param pos Position of the spacecraft w.r.t. the planet in planet-centric coordinates
	 * @param look Direction in which the spacecraft is looking (from pos).
	 * @param elevSource Altitude/elevation data provider
	 * @return Point on the altitude-corrected surface of the planet where
	 *         the spacecraft is looking, or <code>null</code> if the spacecraft
	 *         is looking away from the planet.
	 */
	public HVector getSurfacePoint(HVector pos, HVector look){
		HVector scanNorm = pos.unit().cross(look.unit());

		if (scanNorm.norm2() < 1e-8) // looking nadir
			return HVector.intersectMars(getSphereRadii(getElevation(pos)), pos, look);

		// We find the two vectors which bracket the space where a potential hit
		// may be found. The closest hit will be with the maximum elevation surface
		// and the farthest lit will be with the minimum elevation surface.
		// Ofcourse! The either of them may land outside the planet.
		HVector start = HVector.intersectMars(getSphereRadii(getMaxElevation()), pos, look);
		if (start == null)
			return null; // The look vector does not see the planet
		HVector stop = HVector.intersectMars(getSphereRadii(getMinElevation()), pos, look);
		if (DEBUG)
			System.err.println(start+"\t"+stop);

		double startAngle = start.separationPlanar(pos, scanNorm);
		double stopAngle = stop == null? Math.signum(startAngle)*Math.PI: stop.separationPlanar(pos, scanNorm);

		// omega is the map resolution in radians. There is no reason to have a
		// step-size through the map smaller than this while searching for a hit.
		double omega = (startAngle < 0? -1: 1) * Math.toRadians(1.0/getPPD());
		int nSteps = (int)Math.ceil(Math.abs(stopAngle - startAngle)/Math.abs(omega));

		// We now search from the start angle to the end angle
		HVector scanNormNeg = scanNorm.neg();
		double ang = startAngle;
		for (int i=0; i <= nSteps; i++){
			HVector p = pos.rotate(scanNormNeg, ang);
			HVector dir = p.cross(scanNormNeg);

			/*
			 * We construct the plane containing p with normal dir (the plane passes
			 * through the origin). Intersection of this plane with the look-vector
			 * "look" gives us a point q on the plane. If q's distance from origin
			 * is less than the surface-height (from the planet's center) then we
			 * have a hit.
			 * 
			 * In the following, the plane-line intersection math has been taken from
			 * http://local.wasp.uwa.edu.au/~pbourke/geometry/planeline/
			 * 
			 * Here: look defines the line, dir is the normal to the plane 
			 * with p & origin being the points in the plane. u is the parameter
			 * which defines where (along the look vector) did the look vector hit
			 * the plane. 
			 */
			double u = dir.dot(p.sub(pos))/dir.dot(look);
			HVector q = pos.add(look.mul(u));
			if (DEBUG)
				System.err.println(q);

			// ray height is magnitude of intersection between plane (dir,p) with line (pos,look)
			double rayHeight = q.norm();
			double surfHeight = getElevation(p);
			// if the ray is underground at the lateral center of the DEM cell, then this must be our cell
			// if the number of steps was 0, then we got here on the first pass and must accept this cell
			if (rayHeight <= surfHeight || nSteps == 0)
				return HVector.intersectMars(getSphereRadii(surfHeight), pos, look);

			ang += omega;
		}
		return null;
	}
}
