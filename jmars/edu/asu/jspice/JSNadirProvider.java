package edu.asu.jspice;

import edu.asu.jmars.util.HVector;

public interface JSNadirProvider {
	/**
	 * Returns the normal look vector toward the planet, from the
	 * given time.
	 */
	HVector getNadir(JS js, double et);
	
	/**
	 * Returns the nadir look vector toward the planet, from the
	 * given position vector.
	 */
	HVector getNadir(JS js, HVector p);
}
