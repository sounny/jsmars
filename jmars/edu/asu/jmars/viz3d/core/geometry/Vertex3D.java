package edu.asu.jmars.viz3d.core.geometry;

public class Vertex3D {
	
	public int index;
	protected double angle;
	
	public Vertex3D(double angle, int idx) {
		index = idx;
		this.angle = angle;
	}
	
	@Override
	public String toString() {
		return "angle: "+angle+" index: "+index;
	}

}
