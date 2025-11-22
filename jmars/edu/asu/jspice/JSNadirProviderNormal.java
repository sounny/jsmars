package edu.asu.jspice;

import edu.asu.jmars.util.HVector;

public final class JSNadirProviderNormal implements JSNadirProvider {
	public HVector getNadir(JS js, double et) {
		return  getNadir(js, js.getPos(et));
	}

	public HVector getNadir(JS js, HVector p) {
	    // Get the surface point along the normal
	    double[] lla = js.recgeo(p);
	    HVector s = js.georec(lla[0], lla[1], 0);

	    return  s.sub(p);
	}
}
