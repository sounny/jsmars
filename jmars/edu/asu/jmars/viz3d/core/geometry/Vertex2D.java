package edu.asu.jmars.viz3d.core.geometry;

public class Vertex2D {
	
	protected float x;
	protected float y;
	
	protected int index;
	
	public Vertex2D(float X, float Y, int idx) {
		x = X;
		y = Y;
		index = idx;
	}
	
	@Override
	public String toString() {
		return "x: "+x+" y: "+y+" index: "+index;
	}

}
