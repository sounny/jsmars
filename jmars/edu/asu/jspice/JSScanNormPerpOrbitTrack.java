package edu.asu.jspice;
import edu.asu.jmars.util.HVector;

/**
 * Scan-plane's normal vector provider for spacecrafts such as Odyssey
 * and LRO which have their nadir point to the center of the planet. 
 */
public final class JSScanNormPerpOrbitTrack implements JSScanNormalProvider {
	public HVector getScanNorm(JS js, double et) {
		HVector pos = new HVector();
		HVector vel = new HVector();
		js.getPosVel(et, pos, vel);
		return getScanNorm(js, pos, vel);
	}

	public HVector getScanNorm(JS js, HVector pos, HVector vel) {
		HVector adjVel = new HVector(vel);
		js.removePlanetaryRotation(pos, adjVel);
		HVector nadir = js.getNadir(pos);
		return  nadir.cross(adjVel.unit()).cross(nadir).unit();
	}
}
