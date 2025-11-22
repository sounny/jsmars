package edu.asu.jmars.layer.crater;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.Set;

import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.GeomSource;

public class Crater {
	
	final static Field LONGITUDE = new Field("Longitude", Double.class);
	final static Field LATITUDE = new Field("Latitude", Double.class);
	final static Field DIAMETER = new Field("Diameter", Double.class);
	final static Field DIAMETER_INT = new Field("Diameter", Integer.class);//for compatibility with layer older layer files
	final static Field USER = new Field("User", String.class);	
	final static Field COLOR = new Field("Color", Integer.class);
	final static Field COMMENT = new Field("Note", String.class);
		
	private double longitude = 0.0;
	private double latitude  = 0.0;
	private double diameter     = 0;
	private String user = "";
	private String comment = "";
	private int color = (Color.black).getRGB();	
	
	// Used when reloading from files
	public Crater(Feature feature) {
		super();
		Set<Field> fields = feature.getKeys();
		
		for (Field f : fields) {
			Object o = feature.getAttribute(f);			
			setAttribute(f, o);			
		}		
		
	}
	
	public Crater() {
		super();
	}
	
	public double getLon() {
		return longitude;
	}
	
	public void setLon(double lon) {
		longitude = lon;
	}
	
	public double getLat() {
//		FPath fp = getPath().getSpatialEast();
		
//		Point2D centerPoint=fp.getCenter();
		
		return latitude;
	}
	
	public void setLat(double lat) {
		latitude = lat;
	}
	
	// Diameter in METERS
	public double getDiameter() {
		return diameter;
	}
	
	/**
	 * @param newDiameter  Diameter in METERS
	 */
	public void setDiameter(double newDiameter) {
		diameter = newDiameter;
	}
	
	public void setLocation(Point2D pt) {
		this.longitude = pt.getX();
		this.latitude = pt.getY();
		
	}
	
	public String getUser() {
		return user;
	}
	
	public void setUser(String newUser) {
		user = newUser;
	}

	public String getComment() {
		return comment;
	}
	
	public void setComment(String newComment) {
		comment = newComment;		
	}
	
	public Color getColor() {
		return new Color(color);
	}
	
	public void setColor(Color newColor) {
		color = newColor.getRGB();
	}
	
	private void setPath(Feature f) {
		Point2D vert[]=new Point2D[1];
		vert[0]=new Point2D.Double(longitude, latitude);
		FPath path = new FPath(vert, FPath.SPATIAL_EAST, false);
		f.setPath(path);
	}
	
	public Feature getFeature(CraterSettings cs) {
		Feature f = new Feature();
		
		setPath(f);
		if (cs.inpExpDiameter) {
			f.setAttribute(DIAMETER, new Double(diameter));
		}
		if (cs.inpExpColor) {		
			f.setAttribute(COLOR, color);
		}
		if (cs.inpExpNote) {		
			f.setAttribute(COMMENT, comment);
		}
		if (cs.inpExpUser) {		
			f.setAttribute(USER, user);
		}
								
		return f;
	}
	
	
	public void setAttribute(Field var, Object value) {
		if (var.name != null && var.name.equals(LONGITUDE.name)) {
			longitude = ((Double)value).doubleValue();
		} else if (var.name != null && var.name.equals(LATITUDE.name)) {
			latitude = ((Double)value).doubleValue();
		} else if (var.name != null && var.name.equals(DIAMETER.name)) {
			if (value instanceof Integer) {
				diameter = ((Integer)value).intValue();
			} else {
				diameter = ((Double)value).doubleValue();
			}
		} else if (var.name != null && var.name.equals(USER.name)) {
			user = (String)value;
		} else if (var.name != null && var.name.equals(COLOR.name)) {
			if (value instanceof Integer) {
				color = ((Integer)value).intValue();
			} else if (value instanceof Color) {
				color = ((Color) value).getRGB();
			}
		} else if (var.name != null && var.name.equals(COMMENT.name)) {
			comment = (String)value;
		} else if (var.name != null && var.name.equals("path")) {
			if (value instanceof FPath) {
				FPath fp = (FPath)value;
				double[] coords = new double[2];
				coords = fp.getCoords(false);
				longitude = coords[0];
				latitude = coords[1];
			}
			
		}
	}

	  static final double TOLERANCE = 0.0000001;
	  static final double EXTRA_TOLERANCE = 0.000000001;

		public static Crater craterFrom3Points(final Point2D pt1, final Point2D pt2, final Point2D pt3) throws Exception {
			Crater c = null;
			if (!isPerpendicular(pt1, pt2, pt3) )	{
//				System.out.println("pt1, pt2, pt3");
				c = calcCrater(pt1, pt2, pt3);	
			}
			else if (!isPerpendicular(pt1, pt3, pt2) ) {
//				System.out.println("pt1, pt3, pt2");
				c = calcCrater(pt1, pt3, pt2);	
			}
			else if (!isPerpendicular(pt2, pt1, pt3) ) {
//				System.out.println("pt2, pt1, pt3");
				c = calcCrater(pt2, pt1, pt3);	
			}
			else if (!isPerpendicular(pt2, pt3, pt1) ) {
//				System.out.println("pt2, pt3, pt1");
				c = calcCrater(pt2, pt3, pt1);	
			}
			else if (!isPerpendicular(pt3, pt2, pt1) ) {
//				System.out.println("pt3, pt2, pt1");
				c = calcCrater(pt3, pt2, pt1);	
			}
			else if (!isPerpendicular(pt3, pt1, pt2) ) {
//				System.out.println("pt3, pt1, pt2");
				c = calcCrater(pt3, pt1, pt2);	
			}
			else { 
				throw new Exception("No combination of points and resulting line segments was found that allowed construction of a circle");
			}
			
			return c;
		}
		
		public static boolean isPerpendicular(final Point2D p1, final Point2D p2, final Point2D p3) throws Exception {
			
			double yDelta_a = p2.getY() - p1.getY();
			double xDelta_a = p2.getX() - p1.getX();
			double yDelta_b = p3.getY() - p2.getY();
			double xDelta_b = p3.getX() - p2.getX();

			// checking whether the line defined by the two points is vertical
			if (Math.abs(xDelta_a) <= EXTRA_TOLERANCE && Math.abs(yDelta_b) <= EXTRA_TOLERANCE) {
//				System.out.println("The points form pependicular lines and are parallel to x-y axis");
				return false;
			}

			if (Math.abs(yDelta_a) <= EXTRA_TOLERANCE) {
//				System.out.println(" A line of two of the points is parallel to x-axis 1");
				return true;
			}
			else if (Math.abs(yDelta_b) <= EXTRA_TOLERANCE) {
//				System.out.println(" A line of two of the points is parallel to x-axis 2");
				return true;
			}
			else if (Math.abs(xDelta_a)<= EXTRA_TOLERANCE) {
//				System.out.println(" A line of two of the points is parallel to y-axis 1");
				return true;
			}
			else if (Math.abs(xDelta_b)<= EXTRA_TOLERANCE) {
//				System.out.println(" A line of two of the points is parallel to y-axis 2");
				return true;
			}
			else {
				return false ;		
			}
		}
		
	  public static Crater calcCrater(final Point2D p1, final Point2D p2, final Point2D p3) throws Exception {
		  
		// Algorithm courtesy of http://mathforum.org/library/drmath/view/54323.html
		  
		double yDelta_a = p2.getY() - p1.getY();
		double xDelta_a = p2.getX() - p1.getX();
		double yDelta_b = p3.getY() - p2.getY();
		double xDelta_b = p3.getX() - p2.getX();
		double yDelta_c = p3.getY() - p1.getY();
		double xDelta_c = p3.getX() - p1.getX();

		if (((Math.abs(xDelta_a) <= EXTRA_TOLERANCE) && (Math.abs(yDelta_a) <= EXTRA_TOLERANCE)) ||
				((Math.abs(xDelta_b) <= EXTRA_TOLERANCE) && (Math.abs(yDelta_b) <= EXTRA_TOLERANCE)) ||
				((Math.abs(xDelta_c) <= EXTRA_TOLERANCE) && (Math.abs(yDelta_c) <= EXTRA_TOLERANCE))) {
			throw new Exception("At least two points coincide");
		} else if (((Math.abs(xDelta_a) <= TOLERANCE) && (Math.abs(xDelta_b) <= TOLERANCE)) &&
					(Math.abs((yDelta_a / xDelta_a) - (yDelta_b / xDelta_b)) <= EXTRA_TOLERANCE)) {
			throw new Exception("All three points are colinear");
		} 
		  
	    final double offset = Math.pow(p2.getX(),2) + Math.pow(p2.getY(),2);
	    final double bc =   ( Math.pow(p1.getX(),2) + Math.pow(p1.getY(),2) - offset )/2.0;
	    final double cd =   (offset - Math.pow(p3.getX(), 2) - Math.pow(p3.getY(), 2))/2.0;
	    final double det =  (p1.getX() - p2.getX()) * (p2.getY() - p3.getY()) - (p2.getX() - p3.getX())* (p1.getY() - p2.getY()); 

	    if (Math.abs(det) < TOLERANCE) { 
	    	throw new IllegalArgumentException("An error has occurred. Please try again. If the problem persists, please contact the support team."); 
	    }

	    final double idet = 1.0/det;

	    final double centerX =  (bc * (p2.getY() - p3.getY()) - cd * (p1.getY() - p2.getY())) * idet;
	    final double centerY =  (cd * (p1.getX() - p2.getX()) - bc * (p2.getX() - p3.getX())) * idet;
	    final double radius = 
		       Math.sqrt( Math.pow(p2.getX() - centerX,2) + Math.pow(p2.getY()-centerY,2));

	    Crater c = new Crater();
	    c.setLon(centerX);
	    c.setLat(centerY);
	    c.setDiameter(radius * 2.0); // this is not working correctly for JMars coord systems
	        
	    return c;
	  }	
	  
	  public Shape getProjectedShape() {
		  return getUnprojectedCirclePath().getWorld().getShape(); 
	  }
	  
	  public FPath getUnprojectedCirclePath(){
		  Point2D vert[]=new Point2D[1];
		  vert[0]=new Point2D.Double(longitude, latitude);
		  FPath path = new FPath(vert, FPath.SPATIAL_EAST, false);
			
		  double radius = diameter / 2 / 1000; // kilometers
		  return GeomSource.getCirclePath(path, radius, 36); 
	  }
	  
	  public Point2D[] getRadialPoints(int numberOfPoints, double kmLength, double angularOffset) {
		  Point2D centerPoint = new Point2D.Double(360-longitude, latitude);
		  return GeomSource.getCirclePoints(centerPoint, kmLength, numberOfPoints, angularOffset);
	  }
}

