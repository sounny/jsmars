package edu.asu.jmars.layer.util;

import edu.asu.jmars.util.HVector;

/**
 * Models a DEM, which may come from a file, an image in memory, or a web service.
 */
public interface ElevationSource {
	public int getPPD();
	/** @return the minimum elevation value of this DEM, in km above center of mass */
	public double getMinElevation();
	/** @return the maximum elevation value of this DEM, in km above center of mass */
	public double getMaxElevation();
	/** @return the elevation above this DEM at the give position, in km above center of mass */
	public double getElevation(HVector v);
	/** @return an array of elevations above this DEM at each corresponding position in the given array, each element in km above center of mass */
	public double[] getElevation(HVector[] v);
	/** @return the intersection of a ray from the given position along the given look vector with this DEM */
	public HVector getSurfacePoint(HVector pos, HVector look);
}
