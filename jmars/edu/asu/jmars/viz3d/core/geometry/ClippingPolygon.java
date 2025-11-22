package edu.asu.jmars.viz3d.core.geometry;

import java.util.ArrayList;
import java.util.TreeSet;

public class ClippingPolygon {
	
	public int id;
	public float[][] points;
	public float[] normal;
	public float[] startInter;
	public float[] endInter;
	public float[] startCorner;
	public float[] endCorner;
	public ArrayList<float[]> corners = new ArrayList<>();
	public TreeSet<Integer> inside = new TreeSet<>();
	public boolean isCorner;
	
	public ClippingPolygon() {
		inside.add(0);
		inside.add(1);
		inside.add(2);
	}

}
