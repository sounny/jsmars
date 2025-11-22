package edu.asu.jmars.viz3d.core.geometry;

import java.util.Comparator;

public class VertexAngleComparator implements Comparator<VertexAngle> {

	@Override
	public int compare(VertexAngle v1, VertexAngle v2) {
		return Float.compare(v1.angle, v2.angle);
	}

}
