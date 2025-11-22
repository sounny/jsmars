package edu.asu.jmars.layer.stamp.projection;

import java.awt.geom.Point2D;

import edu.asu.jmars.util.HVector;

public class SimpleCylindrical implements Projection {

	int lines;
	int samples;
	HVector ll, lr, ul, ur;
	boolean globalImage=false;
	
	public SimpleCylindrical(int lines, int samples, Point2D ll, Point2D lr, Point2D ul, Point2D ur, boolean isGlobal) {
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
        globalImage = isGlobal;
	}
	
	/*
	 * There's a disconnect between how simple cylindrical products and true unprojected products should be rendered.
	 * This class is the start of an attempt to figure out what the right thing to do is....
	 */
	public Point2D lonLat(int line, int sample, Point2D returnPoint) {		
		double linePercent = ((line -1) * 1.0) / (lines-1) ;
	
		Point2D leftPt = interpolate(ul.toLonLat(null), ll.toLonLat(null), linePercent);
		Point2D rightPt = interpolate(ur.toLonLat(null), lr.toLonLat(null), linePercent);
		
		// TODO: Whoops, this results in following a great circle, when we really want to follow a latitude line
//		HVector left = ul.interpolate(ll, linePercent);
//		HVector right = ur.interpolate(lr, linePercent);
		
		double samplePercent = ((sample-1) * 1.0) / (samples-1);

		// Make use of return point for efficiency
		Point2D newPoint = interpolate(leftPt, rightPt, samplePercent);
		
//		double newLon = interpolate360(left.lon(), right.lon(), samplePercent);
		
		// TODO: May need to worry about whether left or right is bigger
//		double newLat = left.lat() + (left.lat() - right.lat())*linePercent;
		
//		HVector newPoint = left.interpolate(right, samplePercent);
	
		double newLon = newPoint.getX();
		double newLat = newPoint.getY();

//		System.out.println("line = " + line + "  sample = " + sample + " newLon = " + newLon + " : " + newLat + " sample%="+samplePercent);
		
		returnPoint.setLocation(newLon, newLat);
//		return newPoint.toLonLat(returnPoint);
		return returnPoint;
	}
	
	private Point2D interpolate(Point2D pt1, Point2D pt2, double percent) {
		Point2D newPoint = new Point2D.Double();
		
		double lon1 = pt1.getX();
		double lat1 = pt1.getY();

		double lon2 = pt2.getX();
		double lat2 = pt2.getY();
				
		double newLat = lat1 + (lat2 - lat1)*percent;
		double newLon = interpolate360(lon1, lon2, percent);
		
		if (globalImage) {
			newLon = lon1 + (lon2 - lon1)*percent;
		}

		newPoint.setLocation(newLon, newLat);
		
		return newPoint;
	}
	
	private double interpolate360(double a, double b, double r)
    {
		// Adjust for images that cross the prime meridian
		if (b>180+a) {
			a+=360;
		}
		double c;
//		if (a>b) return interpolate360(b, a, r);
		if (a>b) {
			c=360-(((360-a) + ((360-b)-(360-a)+360*3)%360 * r) % 360);
		} else {
			c= (a + (b-a+360*3)%360 * r) % 360;
		}
		
		return c;
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
		
		SimpleCylindrical proj = new SimpleCylindrical(12127, 22528, ll, lr, ul, ur, false);

		Point2D pt = new Point2D.Double();
		
		System.out.println(proj.lonLat(1, 1, pt));
		System.out.println(proj.lonLat(12127, 1, pt));
		System.out.println(proj.lonLat(1, 22528, pt));
		System.out.println(proj.lonLat(12127, 22528, pt));
		
		System.out.println(proj.lonLat(500, 500, pt));
		System.out.println(proj.lonLat(10000, 10000, pt));
	}
	
	
}

