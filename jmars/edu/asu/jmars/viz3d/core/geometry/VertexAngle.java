package edu.asu.jmars.viz3d.core.geometry;

public class VertexAngle {
	int index;
	float[] vertex;
	float angle; // in degrees
	
	public VertexAngle(int idx, float[] vert, float ang) {
		index = idx;
		vertex = vert;
		angle = ang;
	}
	
	public int getIndex() {
		return index;
	}
}

