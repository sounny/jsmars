package edu.asu.jmars.layer.stamp.projection;

import java.awt.geom.Point2D;

import edu.asu.jmars.util.HVector;

public class Unprojected implements Projection {

	int lines;
	int samples;
	HVector ll, lr, ul, ur;
	
	public Unprojected(int lines, int samples, Point2D ll, Point2D lr, Point2D ul, Point2D ur) {
		this.lines = lines;
		this.samples = samples;
		
		// Lower Left, Lower Right, Upper Left, Upper Right MUST be oriented such that:
		// Line 0, Sample 0 is Upper Left
		// Line maxLine, Sample maxSample is Lower Right
		// Actual orientation with respect to the planet is otherwise irrelevant. (I think)
		
        this.ll = new HVector(ll);
        this.lr = new HVector(lr);
        this.ul = new HVector(ul);
        this.ur = new HVector(ur);
	}
	
	public Point2D lonLat(int line, int sample, Point2D returnPoint) {		
		double linePercent = ((line -1) * 1.0) / (lines-1) ;
		HVector left = ul.interpolate(ll, linePercent);
		HVector right = ur.interpolate(lr, linePercent);
		
		double samplePercent = ((sample-1) * 1.0) / (samples-1);
		HVector newPoint = left.interpolate(right, samplePercent);
		
		return newPoint.toLonLat(returnPoint);
	}
	
	public Point2D lineSample(double lon, double lat, Point2D returnPoint) {
		System.out.println("UNIMPLEMENTED!!");
		System.out.println("UNIMPLEMENTED!!");
		System.out.println("UNIMPLEMENTED!!");
		System.out.println("UNIMPLEMENTED!!");
		System.out.println("UNIMPLEMENTED!!");
		// Much, much harder.... basically the projection logic now in ImageProjecter
		return null;
	}
	
	
	public static void main(String args[]) {
		Point2D ul = new Point2D.Double(304.631, -33.3992);
		Point2D ur = new Point2D.Double(305.639, -33.5174);
		Point2D ll = new Point2D.Double(305.546, -33.9094);
		Point2D lr = new Point2D.Double(304.545, -33.7933);
		
		Unprojected proj = new Unprojected(12127, 22528, ll, lr, ul, ur);

		Point2D pt = new Point2D.Double();
		
		System.out.println(proj.lonLat(1, 1, pt));
		System.out.println(proj.lonLat(12127, 1, pt));
		System.out.println(proj.lonLat(1, 22528, pt));
		System.out.println(proj.lonLat(12127, 22528, pt));
		
		System.out.println(proj.lonLat(500, 500, pt));
		System.out.println(proj.lonLat(10000, 10000, pt));
		
//		System.out.println(proj.lineSample(303.43074071938753, 21.44934997608904));
//		System.out.println(proj.lineSample(303.43074071938753, 21.09834));
//		System.out.println(proj.lineSample(303.577905, 21.09834));
//		
//		System.out.println(proj.lineSample(303.4975, 21.32));

	}
	
	
}

