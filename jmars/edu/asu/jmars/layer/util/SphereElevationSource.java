package edu.asu.jmars.layer.util;

import edu.asu.jmars.util.HVector;

public class SphereElevationSource extends AbstractElevationSource {
	private double radius;
	public SphereElevationSource(double radius) {
		this.radius = radius;
	}
	public double getElevation(HVector v) {
		return radius;
	}
	public double[] getElevation(HVector[] v) {
		double[] out = new double[v.length];
		for (int i = 0; i < v.length; i++) {
			out[i] = radius;
		}
		return out;
	}
	public double getMaxElevation() {
		return radius;
	}
	public double getMinElevation() {
		return radius;
	}
	public int getPPD() {
		return 1;
	}
}
