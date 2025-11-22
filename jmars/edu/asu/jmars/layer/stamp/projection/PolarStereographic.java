package edu.asu.jmars.layer.stamp.projection;

import java.awt.geom.Point2D;

public class PolarStereographic implements Projection {

	double line_projection_offset;
	double sample_projection_offset;
	double center_longitude;
	double center_latitude;
	double map_scale;
	double radius; // meters  
	
	/*
	 * This code only has one parameter for radius, ie. it assumes we are operating on a sphere.
	 * 
	 * This projection, as implemented, works for HiRISE DTM products.  It may or may not work for other data sets.
	 * In particular, this still does NOT work for HRSC products that claim to be in a Polar Stereographic projection.
	 */
	public PolarStereographic(double line_projection_offset, double sample_projection_offset, double center_longitude, double center_latitude, double map_scale, double radius) {
		this.line_projection_offset=line_projection_offset;
		this.sample_projection_offset=sample_projection_offset;
		this.center_latitude=center_latitude;
		this.center_longitude=center_longitude;
		this.map_scale=map_scale;
		this.radius=radius;
	}
	
	public Point2D lonLat(int line, int sample, Point2D returnPoint) {
//		double Rp = 3376.2*1000;  //hirise
//		double Rp = 3396;  // hrsc
		double x = (sample - sample_projection_offset)*map_scale;
		double y = (line_projection_offset - line)*map_scale;
		
		double lon=-1;
		double lat = -1;
		
		if (center_latitude==90) {
			lon = Math.atan(x / (-y));
			lat = Math.PI/2 - 2 * Math.atan(
					   Math.sqrt(
							   ( Math.pow(x,2) + Math.pow(y,2) )
						     / (4 * radius * radius)	   
					   
					   )				
					);
		       // Determine quadrant and modify longitude
		       if (x < 0) {
		    	   if(y > 0) {
		    		   lon += Math.PI;
		    	   } else {
			           lon += 2 * Math.PI;
		    	   }
			   } else if (y > 0) {
				   lon += Math.PI;
		       } else {
		    	   // Do nothing
		       }

		} else {
			lon = Math.atan(x / y);
			lat = 2 * Math.atan(
					   Math.sqrt(
							   ( Math.pow(x,2) + Math.pow(y,2) )
						     / (4 * radius * radius)	   
					   
					   )				
					) - Math.PI/2;
		       // Determine quadrant and modify longitude
		       if (x < 0) {
		    	   if(y < 0) {
		    		   lon += Math.PI;
		    	   } else {
			           lon += 2 * Math.PI;
		    	   }
			   } else if (y < 0) {
				   lon += Math.PI;
		       } else {
		    	   // Do nothing
		       }

		}
					
		
		lon = 360-(Math.toDegrees(lon) + center_longitude);
		lat = Math.toDegrees(lat);
		
		returnPoint.setLocation(lon, lat);
    	return returnPoint;			
	}
	
	public Point2D lineSample(double lon, double lat, Point2D returnPoint) {
//		double Rp = 3376.2*1000;
		
		double x=999;
		double y=999;
		
		if (center_latitude == 90) { // North
			x =  2 * radius * Math.tan(Math.PI / 4 - Math.toRadians(lat/ 2)) * Math.sin(Math.toRadians(lon - center_longitude));
		    y = -2 * radius * Math.tan(Math.PI / 4 - Math.toRadians(lat/ 2)) * Math.cos(Math.toRadians(lon - center_longitude));
		} else if (center_latitude == -90) { // South	
			x =  2 * radius * Math.tan(Math.PI / 4 + Math.toRadians(lat/ 2)) * Math.sin(Math.toRadians(lon - center_longitude));
 	        y =  2 * radius * Math.tan(Math.PI / 4 + Math.toRadians(lat/ 2)) * Math.cos(Math.toRadians(lon - center_longitude));
		}

		double sample = x / map_scale + sample_projection_offset;
		
		double line = line_projection_offset - y / map_scale ; 
		
		returnPoint.setLocation(line, sample);
		return returnPoint;
	}
	
	
	
	
}
