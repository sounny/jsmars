package edu.asu.jspice;

import edu.asu.jmars.util.HVector;

/**
 * Scan-plane's normal provider for spacecrafts like MRO which have
 * their nadir vector perpendicular to the surface of the planet.
 */
public final class JSScanNormPerpGroundTrack implements JSScanNormalProvider {
	public HVector getScanNorm(JS js, double et) {
		HVector pos = new HVector();
		HVector vel = new HVector();
		js.getPosVel(et, pos, vel);
		return getScanNorm(js, pos, vel);
	}

	public HVector getScanNorm(JS js, HVector pos, HVector vel) {
		HVector nadir = js.getNadir(pos);
		return  nadir.cross(vel.unit()).cross(nadir).unit();
	}
}
