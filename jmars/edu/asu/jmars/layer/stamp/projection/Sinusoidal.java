package edu.asu.jmars.layer.stamp.projection;

import java.awt.geom.Point2D;

/*
 * This projection works for HRSC images with Sinusoidal projections.  It may or may not work for other data products.
 */
public class Sinusoidal implements Projection {

	double line_projection_offset;
	double sample_projection_offset;
	double center_longitude;
	double center_latitude;
	double map_resolution;
	
	public Sinusoidal(double line_projection_offset, double sample_projection_offset, double center_longitude, double center_latitude, double map_resolution) {
		this.line_projection_offset=line_projection_offset;
		this.sample_projection_offset=sample_projection_offset;
		this.center_latitude=center_latitude;
		this.center_longitude=center_longitude;
		this.map_resolution=map_resolution;
	}
	
	public Point2D lonLat(int line, int sample, Point2D returnPoint) {
		// There are only going to be N unique lines, despite there being N^2 calls to this routine.
		// Caching the N possible cos(lat) values will save N^2 cosine calculations.
		double lat =  (line_projection_offset - line ) / map_resolution;
		double lon = 360 - ((sample - sample_projection_offset) / 
		             ( map_resolution *  Math.cos(Math.toRadians(lat))) + center_longitude) ;
    		        	
		returnPoint.setLocation(lon, lat);
		return returnPoint;
	}
	
	public Point2D lineSample(double lon, double lat, Point2D returnPoint) {
		double line = (line_projection_offset - (lat * map_resolution));
		
		double sample = (sample_projection_offset + (lon - center_longitude)*map_resolution*Math.cos(Math.toRadians(lat)));

		if (sample>200000) { // Pretty obviously wrong for HRSC
			if (lon%360>350) {
				sample = (sample_projection_offset + (lon - (360+center_longitude))*map_resolution*Math.cos(Math.toRadians(lat)));
			}
		} else if (sample < -100) {
				sample = (sample_projection_offset + (lon - (center_longitude-360))*map_resolution*Math.cos(Math.toRadians(lat)));
		}

		returnPoint.setLocation(line, sample);
		return returnPoint;
	}
	
	
	public static void main(String args[]) {

		double S0 = 3822.5;
		double L0 = 1259460.5;		            		
		double Resolution = 58717.84;
		double LonP = 303.5;
		double LatP = 20;

		Sinusoidal proj = new Sinusoidal(L0, S0, LonP, LatP, Resolution);

		Point2D pt = new Point2D.Double();
		
		System.out.println(proj.lonLat(1, 1, pt));
		System.out.println(proj.lonLat(20611, 1, pt));
		System.out.println(proj.lonLat(1, 8121, pt));
		System.out.println(proj.lonLat(20611, 8121, pt));
		
		System.out.println(proj.lineSample(303.43074071938753, 21.44934997608904, pt));
	}
	
	
}
