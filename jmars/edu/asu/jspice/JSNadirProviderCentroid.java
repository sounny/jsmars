package edu.asu.jspice;

import edu.asu.jmars.util.HVector;

public final class JSNadirProviderCentroid implements JSNadirProvider {
	public HVector getNadir(JS js, double et) {
		return  getNadir(js, js.getPos(et));
	}

	public HVector getNadir(JS js, HVector p) {
		return js.surfpt(p).sub(p);
	}
}
