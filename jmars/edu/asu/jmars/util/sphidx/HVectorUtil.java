package edu.asu.jmars.util.sphidx;

import java.awt.geom.Point2D;

import edu.asu.jmars.util.HVector;

public class HVectorUtil {
	public static HVector fromSpatialE(double lonE, double lat){
		return HVector.fromSpatial(360-lonE, lat);
	}
	
	public static Point2D toELonLat(HVector v, Point2D pt){
		if (pt == null)
			pt = new Point2D.Double();
		pt.setLocation(v.lonE(), v.lat());
		return pt;
	}
}
