package edu.asu.jmars.layer.stamp;

import java.awt.geom.Point2D;

public class Analysis {

	// Special point that is tested to see whether it is inside or outside a given polygon.
	public static final Point2D sentinalPoint = new Point2D.Double(45,45); 
	
	int numShapes=0;
//	double minX = Double.NaN;
//	double maxX = Double.NaN;
//	int posMinX = -1;
//	int posMaxX = -1;
//	
//	double maxY = Double.NaN;
//	double minY = Double.NaN;
//	int posMinY = -1;
//	int posMaxY = -1;
	
	int meridianCrosses=0;
	int leftCrosses=0;
	int rightCrosses=0;
	
	int spatialMeridianCrosses=0;
	
	double spatialMinX = Double.NaN;
	double spatialMaxX = Double.NaN;    	
	
	public boolean is360wide() {
		return (meridianCrosses%2!=0);
	}
	
	public boolean is360wideSpatial() {
		return (spatialMaxX - spatialMinX >0360);
	}
	
	Point2D spatialInteriorPoint = null;
	
	boolean containsSentinalPoint = false;
	
}