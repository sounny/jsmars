/**
 * 
 */
package edu.asu.jmars.util.sphidx;

import edu.asu.jmars.util.HVector;

interface SphShape {
	public HVector[] getPts();
	public HVector[] getNorms();
	public boolean contains(HVector pt);
	public SphTri[] triangulate();
}