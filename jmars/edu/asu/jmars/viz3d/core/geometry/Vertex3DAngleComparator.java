package edu.asu.jmars.viz3d.core.geometry;
import java.util.Comparator;

public class Vertex3DAngleComparator implements Comparator<Vertex3D> {

	public Vertex3DAngleComparator() {
	}
	
	@Override
	public int compare(Vertex3D v1, Vertex3D v2) {
		
		return Double.compare(v1.angle, v2.angle);
		
	}

}
