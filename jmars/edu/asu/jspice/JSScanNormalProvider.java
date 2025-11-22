package edu.asu.jspice;

import edu.asu.jmars.util.HVector;

public interface JSScanNormalProvider {
	/**
	 * Given a time, returns the scan plane normal as a unit vector.
	 */
	HVector getScanNorm(JS js, double et);

	/**
	 * Given a pos and velocity, returns the scan plane normal as
	 * a unit vector. Saves NAIF calls if you already have these
	 * vectors.
	 */
	HVector getScanNorm(JS js, HVector pos, HVector vel);
}
