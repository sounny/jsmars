package edu.asu.jmars.viz3d.core.geometry;

import java.util.Comparator;

import com.jogamp.opengl.math.VectorUtil;

public class Vertex2DAngleComparator implements Comparator<Vertex2D> {

	private float[] m;
	
	public Vertex2DAngleComparator(float[] origin) {
		m = origin;
	}
	
	@Override
	public int compare(Vertex2D v1, Vertex2D v2) {
		
		double angle1 = 0.0;
		double angle2 = 0.0;
		float epsilon = 0.000001f;
		
		if (!VectorUtil.isVec2Equal(new float[] {v1.x, v1.y}, 0, new float[] {m[0], m[1]}, 0, epsilon)) {
	        angle1 = (Math.toDegrees(Math.atan2(v1.y - m[1], v1.x - m[0])) + 360) % 360;			
		}
		
		if (!VectorUtil.isVec2Equal(new float[] {v2.x, v2.y}, 0, new float[] {m[0], m[1]}, 0, epsilon)) {
			angle2 = (Math.toDegrees(Math.atan2(v2.y - m[1], v2.x - m[0])) + 360) % 360;
		}
        System.err.println(v1.toString()+" angle "+angle1);
        System.err.println(v2.toString()+" angle "+angle2);

        //For reversing counter-clockwise or clockwise, just reverse the signs of the return values
        //CCW currently
        if (angle1 < angle2) {
            System.err.println(v1.toString()+" angle1 "+angle1+" < "+v2.toString()+" angle2 "+angle2);
            return -1;
        } else if (angle2 < angle1) {
            System.err.println(v1.toString()+" angle1 "+angle1+" > "+v2.toString()+" angle2 "+angle2);
        	return 1;
        } else {
        	System.err.println("angles equal");
        	return 0;
        }
	}

}
