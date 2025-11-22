package edu.asu.jmars.layer.stamp.projection;

import java.awt.geom.Point2D;

public interface Projection {

	public abstract Point2D lonLat(int line, int sample, Point2D returnPoint);
			
	public abstract Point2D lineSample(double lon, double lat, Point2D returnPoint);
	
}
