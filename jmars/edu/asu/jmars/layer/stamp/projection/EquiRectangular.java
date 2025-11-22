package edu.asu.jmars.layer.stamp.projection;

import java.awt.geom.Point2D;

/*
 * This projection, as implemented, works for HiRISE DTM products.  It may or may not work for other products in EquiRectangular projections.  
 */
public class EquiRectangular implements Projection {
	double line_projection_offset;
	double sample_projection_offset;
	double center_longitude;
	double center_latitude;
	double map_resolution;
			
	private double cosCenterLatitude;
	
	public EquiRectangular(double line_projection_offset, double sample_projection_offset, double center_longitude, double center_latitude, double map_resolution) {
		this.line_projection_offset=line_projection_offset;
		this.sample_projection_offset=sample_projection_offset;
		this.center_latitude=center_latitude;
		this.center_longitude=center_longitude;
		this.map_resolution=map_resolution;
		
		// Calculate this once and save the result, rather than recalculating on every pixel
		cosCenterLatitude=Math.cos(Math.toRadians(center_latitude));
	}
			
	public Point2D lonLat(int line, int sample, Point2D returnPoint) {
		double lon;
		double lat;
				
		lat = (line_projection_offset - line) / map_resolution;
		lon = 360 - (center_longitude + (sample - sample_projection_offset) / (map_resolution * Math.cos(Math.toRadians(center_latitude))));
				
		returnPoint.setLocation(lon, lat);
		return returnPoint;
	}
			
	public Point2D lineSample(double lon, double lat, Point2D returnPoint) {
		double line = line_projection_offset - lat * map_resolution;
		double sample = (lon - center_longitude)*(map_resolution * cosCenterLatitude) + sample_projection_offset;
		
		// For somewhat hard to predict reasons, the calculated sample is sometimes wildly wrong.  
		// The below adjustment seems to fix this.  I'm assuming we won't have many images with more than 200,000 samples
		// per line.
		if (sample>200000) {
			sample = ((lon-360)- center_longitude)*(map_resolution * cosCenterLatitude) + sample_projection_offset;
		}
		
		returnPoint.setLocation(line, sample);
		return returnPoint;
	}
}
