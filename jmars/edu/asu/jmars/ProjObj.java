package edu.asu.jmars;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import edu.asu.jmars.graphics.TransformingIterator;
import edu.asu.jmars.graphics.TransformingIterator.Transformer;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;


public abstract class ProjObj
 {
	private static final DebugLog log = DebugLog.instance();

	public abstract double getServerOffsetX();
	public abstract double getXMin(double x);
	public abstract double getUnitWidth();
	public abstract double getUnitHeight();
	public abstract String getProjectType();
	public abstract String getProjectionSpecialParameters();
	public abstract double getCircumfrence();
	public abstract double getDelta(double ppd);
	public abstract double getCenterLon();
	public abstract double getCenterLat();
	public abstract Point2D getProjectionCenter();
	
	public abstract Point2D convSpatialToWorld(Point2D orig);
	public abstract Point2D convWorldToSpatial(Point2D orig);
	
	/** Transforms the given point from world coordinates to spatial coordinates. */
	public final Transformer worldToSpatial = new Transformer() {
		public void transform(Point2D.Double p) {
			p.setLocation(convWorldToSpatial(p));
		}
	};
	
	/** Transforms the given point from spatial coordinates to world coordinates. */
	public final Transformer spatialToWorld = new Transformer() {
		public void transform(Point2D.Double p) {
			p.setLocation(convSpatialToWorld(p));
		}
	};
	
	public GeneralPath convSpatialToWorld(Shape orig) {
		GeneralPath path = new GeneralPath();
		path.append(new TransformingIterator(orig.getPathIterator(null), spatialToWorld), false);
		return path;
	}
	
	public GeneralPath convWorldToSpatial(Shape orig) {
		GeneralPath path = new GeneralPath();
		path.append(new TransformingIterator(orig.getPathIterator(null), worldToSpatial), false);
		return path;
	}
	
	public final Transformer worldToSpatialEast = new Transformer() {
		public void transform(Point2D.Double p) {
			p.setLocation(convWorldToSpatial(p));
			p.x = lonNorm(-p.x);
		}
	};
	
	public final Transformer spatialEastToWorld = new Transformer() {
		public void transform(Point2D.Double p) {
			p.x = lonNorm(-p.x);
			p.setLocation(convSpatialToWorld(p));
		}
	};
	
    /**
     * Returns a longitude in the interval [0.0, 360.0).
     * This method should not be used on longitudes that are
     * more than 360 degrees outside the included interval;
     * it will return the expected result, but performance may
     * be poor.
     */
	public static final double lonNorm (double lon) {
		while (lon < 0.0) lon += 360.0;
		while (lon >= 360.0) lon -= 360.0;
		return lon;
	}
	
	public Point2D convSpatialToWorld(double x, double y) {
		return convSpatialToWorld(new Point2D.Double(x, y));
	}
	
	public Point2D convWorldToSpatial(double x, double y) {
		return convWorldToSpatial(new Point2D.Double(x, y));
	}
	
	String projString=null;
	public String getProjString() {
		if (projString==null) {
			projString = getCenterLon() + ":" + getCenterLat();
		}
		return projString;
	}
	
	public static class Projection_OC extends ProjObj
	 {
		private final HVector up;
		private final double  upLon;
		private final double  upLat;
		private double  projCenterLon;
		private double  projCenterLat;
		private final HVector center;
		private static double ROUND = Config.get("projection.round", 0);

		/**
		 ** Constructs from an arbitrary "up direction", rounded
		 ** according to config file parameters.
		 **
		 ** @param up The desired screen-up direction for the
		 ** projection (the y-axis of world coordinates).
		 ** @param round Indicates whether or not to round the
		 ** vector. Rounding is performed to the nearest
		 ** longitude/latitude multiple of the config value for
		 ** "projection.round".
		 **/
		
		public double getUpLon() {
			return upLon;
		}
		
		public double getUpLat() {
			return upLat;
		}
		
		public Projection_OC(HVector up, boolean round)
		 {
         log.println("Incoming UP: "+up.lon()+" , "+up.lat());

			double lon = up.lon();
			double lat = up.lat();
			if(round  &&  ROUND > 0)
			 {
				log.println("ROUND = " + ROUND);
				log.println("got: lon = " + lon);
				log.println("     lat = " + lat);
				lon = Util.roundToMultiple(lon, ROUND);
				lat = Util.roundToMultiple(lat, ROUND);
				if(90-Math.abs(lat) < 0.001) {
					log.println("I'm setting lon to 0 now");
					lon = 0;
				}
				up = new HVector(lon, lat);
				log.println("now: lon = " + lon);
				log.println("     lat = " + lat);
			 }

			this.upLon = lon;
			this.upLat = lat;
			this.up = up.unit();
			this.center = upLat >= 0
				? new HVector(180 + upLon, 90 - upLat)
				: new HVector(      upLon, 90 + upLat);


                        log.println("Up lon/lat {"+this.up.lon()+" , "+this.up.lat()+"}");
                        log.println("Cen lon/lat {"+this.center.lon()+" , "+this.center.lat()+"}");
                        log.println("Up Vector: "+this.up);
                        log.println("Cen Vector: "+this.center);
      

		 }

		/**
		 ** Constructs from an arbitrary "up direction", rounded
		 ** according to config file parameters.
		 **
		 ** @param up The desired screen-up direction for the
		 ** projection (the y-axis of world coordinates).
		 **/
		public Projection_OC(HVector up)
		 {
			this(up, true);
		 }

		/**
		 ** Constructs from an arbitrary centerpoint of the
		 ** projection, oriented to north-up. All arguments are in
		 ** degrees.
		 **/
		public Projection_OC(double centerLon, double centerLat)
		 {
			this(centerLat >= 0
				 ? new HVector(180 + centerLon, 90 - centerLat)
				 : new HVector(      centerLon, 90 + centerLat));

			projCenterLon = centerLon; //This is Western leading coords!
			projCenterLat = centerLat;
//We need to round our values as well
			if(ROUND > 0) {
				projCenterLon = Util.roundToMultiple(projCenterLon, ROUND);
				projCenterLat = Util.roundToMultiple(projCenterLat, ROUND);
			}
		 }

		public Point2D getProjectionCenter()
		{
			return(new Point2D.Double(projCenterLon, projCenterLat));
		}

		public double getCenterLon()
		 {
			return  center.lon();
		 }

		public double getCenterLat()
		 {
			return  center.lat();
		 }

		public double getServerOffsetX() { return  0.0; }
		public double getXMin(double x)		{ return ( (x < 0 ? ((360-Math.abs(x)%360.)%360.) : x % 360.));			}
		public double getUnitWidth()			{ return (1.0);		}
		public double getUnitHeight()			{ return (1.0);			}

		public String getProjectType()		{ return ("OC");			}
		public double getCircumfrence()		{ return (360.0);			}
		public double getDelta(double ppd)				{ return (1.0/ppd);			}

		// Locate the center and the "upward" direction vectors
		public HVector getCenter()
		 {
			return  (HVector) center.clone();
		 }
		public HVector getUp()
		 {
			return  (HVector) up.clone();
		 }

		public String getProjectionSpecialParameters()
		 {
			String pars =
				"&TRACK_centerLat=888&TRACK_centerLon=888" +
				"&TRACK_upLat=" + upLat +
				"&TRACK_upLon=" + upLon + "&TRACK_format=c";
			return  pars;
		 }

		/**
		 ** Takes a point in degrees left-right/up-down and outputs lat/long.
		 **/
		public Point2D convWorldToSpatial(Point2D orig)
		 {
			double x = Math.toRadians(orig.getX());
			double y = Math.toRadians(orig.getY());

			// Calculate the converted point's position as y degrees in
			// the center->up direction and x degrees about up
			HVector pt =
				getCenter().mul(Math.cos(y))
				.add(getUp().mul(Math.sin(y)))
				.rotate(getUp(), x);

			Point2D spat = new Point2D.Double(Math.toDegrees(lon_of(pt)) % 360,
											  Math.toDegrees(lat_of(pt)));
			return  spat;
		 }

		/**
		 * @param orig The point to convert; the x-axis is the west-leading
		 * longitude, the y-axis is the ocentric latitude.
		 * @return The point in world coordinates (this map projection's two-axis
		 * Euclidian coordinate system.)
		 */
		public Point2D convSpatialToWorld(Point2D orig)
		 {
			HVector pt = new HVector(orig);
			HVector up = getUp();
			HVector center = getCenter();

			HVector noZ = pt.sub( up.mul(up.dot(pt)) );

			double x = lon_of(new HVector(noZ.dot(center),
										  noZ.dot(center.cross(up)),
										  0));

//			double y = Math.asin(up.dot(pt)); <-- numerically unstable, NANs!!
			double y = Math.PI/2 - up.unitSeparation(pt);

			return  new Point2D.Double(Math.toDegrees(x) % 360.0,
                                                   Math.toDegrees(y));
		 }
		
	 }

	////////////// BEGIN internal cylindrical routines /////////////
	
	public static double lat_of(HVector p)
	 {
		return  Math.asin(p.unit().z);
	 }

	public static double lon_of(HVector p)
	 {
		if(p.y > 0)
			return  Math.PI * 2 - Math.atan2(p.y, p.x);

		else if(p.y < 0)
			return  -Math.atan2(p.y, p.x);

		else if(p.x < 0)
			return  Math.PI;

		else
			return  0;

	 }

	////////////// END internal cylindrical routines /////////////

	public static void main(String[] av)
	 throws Throwable
	 {
		if(av.length == 1)
		 {
			dumpGrid(av[0]);
			System.exit(0);
		 }
		else if(av.length == 2)
		 {
			ProjObj.Projection_OC po =
				new Projection_OC(Double.parseDouble(av[0]),
								  Double.parseDouble(av[1]));
			System.exit(0);
		 }

		ProjObj.Projection_OC po =
			new Projection_OC(new HVector(Double.parseDouble(av[0]),
										  Double.parseDouble(av[1])));
		BufferedReader in =
			new BufferedReader(new InputStreamReader(System.in));

		System.out.println("up = " +
						   po.getUp().lon() + " " +
						   po.getUp().lat());
		System.out.println("center = " +
						   po.getCenter().lon() + " " +
						   po.getCenter().lat());

		for(;;)
		 {
			double x = Double.parseDouble(in.readLine());
			double y = Double.parseDouble(in.readLine());

			Point2D w2s = po.convWorldToSpatial(x, y);
			System.out.println("W -> S\t" + new HVector(w2s));

			String cmd = "make_track" +
				" up_lon " + av[0] +
				" up_lat " + av[1] +
				" xmin " + x +
				" ymin " + y +
				" format c";
			cmd = "wget -q -O - 'http://jmars.asu.edu/internal/make_track.phtml?" +
				"format=c" +
				"&upLon=" + av[0] +
				"&upLat=" + av[1] +
				"&xmin=" + x +
				"&ymin=" + y +
				"'";
			System.out.println(cmd);
			Process p = Runtime.getRuntime().exec(cmd);

			p.waitFor();
			InputStream is = p.getInputStream();
//			System.out.println(is);
			System.out.println("track\t" +
							   new BufferedReader(new InputStreamReader(is))
								.readLine());
			p.destroy();
		 }
	 }

	private static void dumpGrid(String url)
	 {
		String args = url.substring(url.indexOf('?')+1);
		StringTokenizer tok = new StringTokenizer(args, "&=");
		double xmin = 0;
		double ymin = 0;
		double xdelta = 1;
		double ydelta = 1;
		int xcount = 1;
		int ycount = 1;
		double upLat = 999;
		double upLon = 999;

		while(tok.hasMoreTokens())
		 {
			String label = tok.nextToken().intern();
			double val = Double.parseDouble(tok.nextToken());

			if     (label == "xmin"  ) xmin   = val;
			else if(label == "ymin"  ) ymin   = val;
			else if(label == "xdelta") xdelta = val;
			else if(label == "ydelta") ydelta = val;
			else if(label == "xcount") xcount = (int) Math.round(val);
			else if(label == "ycount") ycount = (int) Math.round(val);
			else if(label == "upLat" ) upLat  = val;
			else if(label == "upLon" ) upLon  = val;
			else System.err.println("UNKNOWN LABEL: " + label);
		 }

		ProjObj.Projection_OC po =
			new Projection_OC(new HVector(upLon, upLat));

		for(int x=0; x<xcount; x++)
			for(int y=0; y<ycount; y++)
			 {
				Point2D world = new Point2D.Double(xmin + xdelta*x,
												   ymin + ydelta*y);
				Point2D spatial = po.convWorldToSpatial(world);
				HVector v = new HVector(spatial);
				System.out.println(v);
			 }
	 }
 }
