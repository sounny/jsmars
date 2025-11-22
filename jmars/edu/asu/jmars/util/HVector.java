package edu.asu.jmars.util;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.StringTokenizer;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.Util;

public final class HVector
 implements Cloneable, Serializable
 {
	private static DebugLog log = DebugLog.instance();

	public static final HVector ORIGIN = new HVector(0,0,0);
	public static final HVector X_AXIS = new HVector(1,0,0);
	public static final HVector Y_AXIS = new HVector(0,1,0);
	public static final HVector Z_AXIS = new HVector(0,0,1);

	public double x;
	public double y;
	public double z;

	public HVector()
	 {
		x = y = z = 0.0;
	 }

    /**
     ** Convenience method for converting an HVector into world
     ** coordinates.
     **/
    public Point2D toWorld()
     {
	return  Main.PO.convSpatialToWorld(lonW(), latC());
     }

	/**
	 ** Returns an array of the vector's coordinates.
	 **
	 ** @return { {@link #x}, {@link #y}, {@link #z} }
	 **/
	public double[] toArray()
	 {
		return  new double[] { x, y, z };
	 }

	private static HVector marsll2vector(double lon, double lat)
	 {
		return  new HVector(Math.cos(lat)*Math.cos(-lon),
							Math.cos(lat)*Math.sin(-lon),
							Math.sin(lat));
	 }

	/**
	 ** Constructs an HVector from Mars lon/lat (west-leading ocentric degrees).
	 **/
	public HVector(double lon, double lat)
	 {
		this(
			Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(-lon)),
			Math.cos(Math.toRadians(lat)) * Math.sin(Math.toRadians(-lon)),
			Math.sin(Math.toRadians(lat))
			);
	 }

	/**
	 ** Constructs an HVector from Mars lon/lat (degrees).
	 **/
	public HVector(Point2D lonlat)
	 {
		this(lonlat.getX(), lonlat.getY());
	 }

	public HVector(double[] coords)
	 {
		this(coords[0], coords[1], coords[2]);
	 }

	public HVector(double[] coords, int startIdx)
	 {
		this(coords[startIdx + 0],
			 coords[startIdx + 1],
			 coords[startIdx + 2]);
	 }

	public HVector(double x, double y, double z)
	 {
		this.x = x;
		this.y = y;
		this.z = z;
	 }

	public HVector(HVector old)
	 {
		this.x = old.x;
		this.y = old.y;
		this.z = old.z;
	 }

	public Object clone()
	 {
		try
		 {
			return  super.clone();
		 }
		catch(CloneNotSupportedException e)
		 {
			log.aprintln("PROGRAMMER: Failed clone.");
			log.aprint(e);
			return  null;
		 }
	 }

	public boolean equals(Object obj)
	 {
		if(obj == null  ||  !(obj instanceof HVector))
			return  false;

		HVector v = (HVector) obj;
		return  v.x == x
			&&  v.y == y
			&&  v.z == z;
	 }

	public int hashCode()
	 {
		long v = (Double.doubleToLongBits(x) ^
				  Double.doubleToLongBits(y) ^
				  Double.doubleToLongBits(z));

		return  (int) (v ^ (v>>>32));
	 }

	public HVector set(HVector v)
	 {
		x = v.x;
		y = v.y;
		z = v.z;
		return  this;
	 }

	public HVector set(double x, double y, double z)
	 {
		this.x = x;
		this.y = y;
		this.z = z;
		return  this;
	 }

	public HVector set(double[] coords)
	 {
		this.x = coords[0];
		this.y = coords[1];
		this.z = coords[2];
		return  this;
	 }

	public HVector set(double[] coords, int startIdx)
	 {
		this.x = coords[startIdx + 0];
		this.y = coords[startIdx + 1];
		this.z = coords[startIdx + 2];
		return  this;
	 }

	// Basic arithmetic

	public HVector add(double c){
		return new HVector(x+c,y+c,z+c);
	}
	
	public HVector add(HVector v)
	 {
		return  new HVector(x + v.x,
							y + v.y,
							z + v.z);
	 }

	public HVector sub(HVector v)
	 {
		return  new HVector(x - v.x,
							y - v.y,
							z - v.z);
	 }

	public HVector mul(double c)
	 {
		return  new HVector(x * c,
							y * c,
							z * c);
	 }

	public HVector div(double c)
	 {
		return  new HVector(x / c,
							y / c,
							z / c);
	 }

	/**
	 ** Returns a new vector that is the negation of this one.
	 **/
	public HVector neg()
	 {
		return  new HVector(-x,
							-y,
							-z);
	 }

	/**
	 ** Negates this vector.
	 **/
	public void negate()
	 {
		x = -x;
		y = -y;
		z = -z;
	 }

	// Compound assignment arithmetic

	public HVector addEq(HVector v)
	 {
		return set(x + v.x, y + v.y, z + v.z);
	 }

	public HVector subEq(HVector v)
	 {
		return set(x - v.x, y - v.y, z - v.z);
	 }

	public HVector mulEq(double c)
	 {
		return set(x*c, y*c, z*c);
	 }

	public HVector divEq(double c)
	 {
		return set(x/c, y/c, z/c);
	 }

	// Silly divide, used in QMV
	private HVector ratioDivEq(HVector v)
	 {
		x /= v.x;
		y /= v.y;
		z /= v.z;
		return  this;
	 }

	// Products

	public double dot(HVector v)
	 {
		return  x * v.x
			+   y * v.y
			+   z * v.z;
	 }

	public HVector cross(HVector v)
	 {
		return  new HVector(y * v.z - z * v.y,
							z * v.x - x * v.z,
							x * v.y - y * v.x);
	 }

	/**
	 ** Returns a the cross product normalized (i.e. as a unit
	 ** vector).
	 **/
	public HVector ucross(HVector v)
	 {
		return  new HVector(y * v.z - z * v.y,
							z * v.x - x * v.z,
							x * v.y - y * v.x).unit();
	 }

	public double norm()
	 {
		return  Math.sqrt(this.dot(this));
	 }

	public double norm2()
	 {
		return  this.dot(this);
	 }

	public HVector unit()
	 {
		double n2 = this.norm2();
		if(n2 == 0)
			return  (HVector) clone();
		else
			return  this.div(Math.sqrt(n2));
	 }

	public double normalize()
	 {
		double n2 = this.norm2();
		if(n2 == 0)
			return  0;

		double n = Math.sqrt(n2);
		this.divEq(n);
		return  n;
	 }

	public double separation(HVector v)
	 {
		// Copied out of hvector.C from the QMV library. This is more
		// numerically stable than the most intuitive implementation:

//		return  Math.acos(this.dot(v) / (v.norm() * this.norm()));

		// Stability is not just a theoretical issue... the above
		// readily breaks down in real-world usage when
		// this.equals(v) is true, generating NaN values.

		HVector w = (HVector) this.clone();
		HVector y = (HVector) v.clone();

		double magu = w.normalize();
		double magv = y.normalize();
		if(magu == 0  ||  magv == 0)
			return  0;

		double dp = w.dot(y);

		if(dp > 0)
		 {
			HVector temp = w.sub(y);
			double dxp = temp.norm();
			return  2 * Math.asin(dxp / 2);
		 }
		else if(dp < 0)
		 {
			HVector temp = w.add(y);
			double dxp = temp.norm();
			return  Math.PI - 2 * Math.asin(dxp / 2);
		 }
		else
			return  Math.PI / 2;
	 }

	/**
	 ** Computes the angular separation (in radians) between two unit
	 ** vectors.
	 **/
	public double unitSeparation(HVector y)
	 {
		double dp = dot(y);
		if(dp > 0)
		 {
			HVector temp = sub(y);
			double dxp = temp.norm();
			return  2 * Math.asin(dxp / 2);
		 }
		else if(dp < 0)
		 {
			HVector temp = add(y);
			double dxp = temp.norm();
			return  Math.PI - 2 * Math.asin(dxp / 2);
		 }
		else
			return  Math.PI / 2;
	 }
	
	// Copied from Qmv/include/qmv/macros.h
	private boolean same_sign(double a, double b){
		return (((a >= 0) && (b >= 0)) || ((a <= 0) && (b <= 0)));
	}

	// Copied out of hvector.C from the QMV library, this is actually
	// the QMV routine signed_separation().
	public double separation(HVector v, HVector axis)
	 {
//  		if(true)
//  			throw  new Error("UNTESTED FUNCTION");

		// Returns the signed value of the smallest angle between two vectors
		// Returns 0. if either vector norm = 0
		// so you must check magnitudes if zero is returned

		HVector w = (HVector) this.clone();
		HVector y = (HVector) v.clone();
		double magu = w.normalize();
		double magv = y.normalize();
		if(magu == 0  ||  magv == 0)
			return  0;

		HVector t = w.cross(y);
		t.normalize();
		boolean wrongSense =
			// Assuming the axis is at least in the correct quadrant
			!(same_sign(t.x, axis.x) && same_sign(t.y, axis.y) && same_sign(t.z, axis.z));
		/*
			sign(t.x) != sign(axis.x)  ||
			sign(t.y) != sign(axis.y)  ||
			sign(t.z) != sign(axis.z);
			*/
		double dp = w.dot(y);
		double result = 0;
		if(dp > 0)
		 {
			HVector temp = w.sub(y);
			double dxp = temp.norm();
			result = 2 * Math.asin(dxp / 2);
		 }
		else if(dp < 0)
		 {
			HVector temp = w.add(y);
			double dxp = temp.norm();
			result = Math.PI - 2 * Math.asin(dxp / 2);
		 }
		else
			result = Math.PI / 2;
		if(wrongSense)
			result = -result;

		return  result;
	 }

    /**
	 ** Returns the signed separation between two planes, about axis:
	 **
	 ** <ul>
	 ** <li>Plane 1: spans axis and this</li>
	 ** <li>Plane 2: spans axis and v</li>
	 ** </ul>
	 **
	 ** Does not assume that axis is normal to either other vector.
	 **/
	public double separationPlanar(HVector v, HVector axis)
	 {
		HVector a = axis.cross(this.cross(axis));
		HVector b = axis.cross(   v.cross(axis));
		return  a.separation(b, axis);
	 }

	/////////////////////////////////////////////////////////////////////////
	// Mars-specific stuff
	/////////////////////////////////////////////////////////////////////////

	/**
	 ** Returns areocentric latitude of the vector in degrees.
	 **/
	public double lat()
	 {
		return  latC();
	 }

	/**
	 ** Returns west-leading longitude of the vector in degrees.
	 **/
	public double lon()
	 {
		return  lonW();
	 }

	/**
	 ** Returns areocentric latitude of the vector in degrees.
	 **/
	public double latC()
	 {
		return  Math.toDegrees(Math.asin(unit().z));
	 }

	/**
	 ** Returns east-leading longitude of the vector in degrees.
	 **/
	public double lonE()
	 {
		return  (360+Math.toDegrees(Math.atan2(y, x))) % 360;
	 }

	/**
	 ** Returns west-leading longitude of the vector in degrees.
	 **/
	public double lonW()
	 {
		if(y > 0)
			return  Math.toDegrees(Math.PI * 2 - Math.atan2(y, x));

		else if(y < 0)
			return  Math.toDegrees(-Math.atan2(y, x));

		else if(x < 0)
			return  Math.toDegrees(Math.PI);

		else
			return  0;

	 }

	public Point2D toLonLat(Point2D pt)
	 {
		if(pt == null)
			return  new Point2D.Double(lon(), lat());
		pt.setLocation(lon(), lat());
		return  pt;
	 }

	public void fromLonLat(double lon, double lat)
	 {
		x = Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(-lon));
		y = Math.cos(Math.toRadians(lat)) * Math.sin(Math.toRadians(-lon));
		z = Math.sin(Math.toRadians(lat));
	 }

	public void fromLonLat(Point2D lonlat)
	 {
		fromLonLat(lonlat.getX(), lonlat.getY());
	 }

	private static final HVector ellipsoid_radii = new HVector(Util.EQUAT_RADIUS,
															   Util.EQUAT_RADIUS,
															   Util.POLAR_RADIUS);
	/**
	 * This method is to refresh the x,y,z values for the current body if a new body has been selected. 
	 * Without a call to this method after selecting a new body, the 
	 * calculations will not be accurate. 
	 * @since change bodies
	 */
	public static void refreshEllipsoidRadii() {
		ellipsoid_radii.x = Util.EQUAT_RADIUS;
		ellipsoid_radii.y = Util.EQUAT_RADIUS;
		ellipsoid_radii.z = Util.POLAR_RADIUS;
	}
	/** @return Mars radii in Km. */
	public static HVector getMarsRadii(){
		return new HVector(ellipsoid_radii);
	}
	
	/**
	 * Returns the point on Mars that is hit from a ray at the given
	 * vertex in the given direction. Returns null if there is no
	 * such point.
	 */
	public static HVector intersectMars(HVector vertex, HVector direction)
	 {
		return intersectMars(ellipsoid_radii, vertex, direction);
	 }
	
	/**
	 * Returns the point on Mars that is hit from a ray at the given
	 * vertex in the given direction. Returns null if there is no
	 * such point.
	 *
	 * <p>Shamelessly stolen from intersect.C in QMV, then converted
	 * and modified all over the place.
	 * 
	 * @param ellipsoid_radii Mars radii in Km.
	 * @param vertex Ray's origin.
	 * @param direction Ray's head.
	 * @return A point on Mars in case of a hit or <code>null</code>
	 *    in case there is no hit.
	 */
	public static HVector intersectMars(HVector ellipsoid_radii, HVector vertex, HVector direction)
	 {
		// The point we eventually return, it was a member of QMV Intersection
		HVector point1;

		/*
		  The intersection of a line-of-sight vector with the surface
		  of an Ellipsoid. There may be two points of intersection.
		  Point1 is the closest. This is surfpt by W.L. Taber

		  Check the input vector to see if its the zero vector. If it
		  is signal an error and return.
		*/
		if(direction.norm2() == 0)
			return  null;

		/*
		  Check the axes to make sure that none of them is less than
		  or equal to zero. If one is, signal an error and return.
		*/
		// removed

		/*
		  We need to find the smallest positive value of t, such that
		  ray.vertex + t * U lies on the ellipsoid.  That is, plug
		  this variable point into the equation of the ellipsoid, and
		  solve the resulting quadratic equation for t.  Take the
		  smallest positive value found if one exists.

		  First set up some temporary vectors for computing the
		  coefficients of the quadratic equation:

		  alpha t**2 + 2*beta t + gamma = 0
		*/
		HVector x = new HVector(direction);
		x.ratioDivEq(ellipsoid_radii);

		HVector y = new HVector(vertex);
		y.ratioDivEq(ellipsoid_radii);

		double alpha = x.dot(x);
		double beta = x.dot(y);
		double gamma = y.dot(y) - 1;
		/*
		  The solutions to the equation are of course

		  (-beta (+ or -) sqrt(beta*beta - alpha * gamma))/alpha;

		  Let's first make sure the discriminant is non-negative.
		*/
		double dscrm = beta*beta - alpha*gamma;
		if(dscrm < 0)
		 {
			/*
			  In this case there can be no solutions.  We can't take a
			  real square root of a negative number.
			*/
			return  null;
		 }
		else if(gamma < 0)
		 {
			/* 
			   The discriminant is positive. gamma < 0 implies that
			   the point vertex is inside the ellipsoid.  Clearly
			   there must be a point where the ray intersects the
			   ellipsoid.  Moreover, vertex plus a positive scalar
			   multiple of U must give this point. vertex plus
			   some negative scalar multiple of U will give a point of
			   intersection of the anti-ray and the ellipsoid.  These
			   scalar multiples must both be roots of the quadratic
			   equation.  In our case we want the positive root ---
			   that is the larger of the two roots.
			*/
			double scalar = (-beta + Math.sqrt(dscrm))/alpha;
			point1 = vertex.add(direction.mul(scalar));
			return  point1;
		 }
		else if(gamma == 0)
		 {
			/*
			  The point must be ON the ellipsoid.  We'll take it to be
			  the intercept point
			*/
			point1 = new HVector(vertex);
			return  point1;
		 }
		else if(beta < 0)
		 {
			/*
			  The discriminant is positive, and the point vertex
			  is outside the ellipsoid.  One of two cases must be
			  true.

			  1. The ray intersects the ellipsoid in two points or
			  tangentially

			  2. The anti-ray intersects the ellipsoid in two points
			  or tangentially.

			  In case 1. both roots of the quadratic expression must
			  be positive.  This is where the sign of beta comes in
			  --- if beta is negative, we know that at least one of
			  the two roots of the quadratic is positive, but since
			  we've made it this far, it follows that both roots
			  (counting multiplicities in the tangential case) must be
			  positive.  Thus the ray must intersect and the first
			  intersection of the ray corresponds to the smaller root
			  of the quadratic expression.
			*/
			double scalar = (-beta - Math.sqrt(dscrm))/alpha;
			point1 = vertex.add(direction.mul(scalar));
//			scalar = (-beta + Math.sqrt(dscrm))/alpha;
			return  point1;
		 }
		else
		 {
			/*
			  In Case 2, if beta is positive or zero there will be at
			  least one negative root, but again since we've made it
			  this far, we know from geometry that both roots must
			  have the same sign.  Thus both roots must be negative.
			  Consequently it is the anti-ray and not the ray that
			  intersects the ellipsoid.
			*/
			return  null;
		 }
	 }

	/**
	 * Returns normal to the ellipsoid at the specified point. <em>
     * Lifted from cspice.</em>
	 * @param radii Semi-major axes of the ellipsoid.
	 * @param p Point (must be) on the ellipsoid.
	 * @return Normal to surface with the specified radii at point p.
	 */
	public static final HVector surfNorm(HVector radii, HVector p){
		if (radii.x <= 0 || radii.y <= 0 || radii.z <= 0)
			throw new IllegalArgumentException("Radii cannot be less than or equal to zero.");
		
		double minRadius = Math.min(radii.x, Math.min(radii.y, radii.z));
		HVector r = new HVector(minRadius/radii.x, minRadius/radii.y, minRadius/radii.z);
		
		HVector normal = new HVector(p.x*(r.x*r.x),p.y*(r.y*r.y),p.z*(r.z*r.z));
		return normal.unit();
	}
	
	/////////////////////////////////////////////////////////////////////////
	// I/O routines
	/////////////////////////////////////////////////////////////////////////

	// toggles between x/y/z and lon/lat input and output formats for vectors
	public static final boolean INPUT_usesLatLon = false;  // must be false
	public static final boolean OUTPUT_usesLatLon = "true".equals(Config.get(
		"hvector.out.ll"));
	static
	 {
		if(INPUT_usesLatLon)
		 {
			log.aprintln("------------------------------------------------");
			log.aprintln(" MICHAEL: HVector.INPUT_usesLatLon is set true!");
			log.aprintln("------------------------------------------------");
		 }
	 }

	public String toString()
	 {
		if(OUTPUT_usesLatLon)
		 {
			double x = lon();
			double y = lat();
			if(x > 300)
				x -= 360;
			return  x + "\t" + y;
		 }
		else
			return  x + "\t" + y + "\t" + z;
	 }

	public static HVector read(BufferedReader fin)
	 {
		try
		 {
			String line = fin.readLine();
			if(line == null)
				return  null;
			StringTokenizer tok = new StringTokenizer(line);
			if(INPUT_usesLatLon)
				return  marsll2vector(
					Math.toRadians(Double.parseDouble(tok.nextToken())),
					Math.toRadians(Double.parseDouble(tok.nextToken())) );
			else
				return  new HVector(Double.parseDouble(tok.nextToken()),
									Double.parseDouble(tok.nextToken()),
									Double.parseDouble(tok.nextToken()) );
		 }
		catch(Throwable e)
		 {
			return  null;
		 }
	 }

	/**
	 ** Identical to {@link #read}, but throws an exception on error
	 ** instead of returning null.
	 **/
	public static HVector readExc(BufferedReader fin)
	 throws IOException
	 {
		String line = null;
		try
		 {
			line = fin.readLine();
			if(line == null)
				return  null;
			StringTokenizer tok = new StringTokenizer(line);
			if(INPUT_usesLatLon)
				return  marsll2vector(
					Math.toRadians(Double.parseDouble(tok.nextToken())),
					Math.toRadians(Double.parseDouble(tok.nextToken())) );
			else
				return  new HVector(Double.parseDouble(tok.nextToken()),
									Double.parseDouble(tok.nextToken()),
									Double.parseDouble(tok.nextToken()) );
		 }
		catch(Throwable e)
		 {
			throw new IOException("Unable to parse vector! [" +
								  line + "] (" + e + ")");
		 }
	 }

	/////////////////////////////////////////////////////////////////////////
	// Fancy stuff (all taken directly from hvector.C and hvector.h)
	/////////////////////////////////////////////////////////////////////////

	public HVector
	rotate(HVector axis,
		   double theta)
	 {
		// rotate vector around axis by theta radians 
		if(axis.dot(axis) != 0.0)
		 {
			HVector w = axis.unit();
			HVector pr = this.projOnto(w);
			HVector v1 = this.sub(pr);
			HVector v2 = w.cross(v1);
			v1.mulEq(Math.cos(theta));
			v2.mulEq(Math.sin(theta));
			return  v1.add(v2).add(pr);
		 }
		else
			return  new HVector(this); // rotate around a zero length axis
	 }

	public HVector
	projOnto(HVector u)
	 {
		// This vector projected onto u

		double bigv = biggest();
		double bigu = u.biggest();
		if((bigu == 0.0)||(bigv==0.0)) return(this);
    
		HVector r = u.div(bigu);
		HVector t = this.div(bigv);
		double dotv = t.dot(r);
		double dotu = r.dot(r);
		double scale;
		if(dotv ==  0.0)scale = 0.0;
		else scale = dotv*bigv/dotu;

		HVector temp = r.mul(scale);
		return(temp);
	 }

    public double biggest()
	 {
	   double m = Math.abs(x);
	   if(m < Math.abs(y))m=Math.abs(y);
	   if(m < Math.abs(z))m=Math.abs(z);
	   return(m);
	 }

	/////////////////////////////////////////////////////////////////////////
	// Handy routines from various east:~gigabyte/sphere/*.C files
	/////////////////////////////////////////////////////////////////////////

	/**
	 ** Returns the intersection (if one is found) of two great-circle
	 ** segments. Returns null if there is none.
	 **
	 ** <p>Passed vectors MUST be unit vectors.
	 **/
	public static HVector intersectGSeg(HVector a1, HVector a2,
										HVector b1, HVector b2)
	 {
		// Test axis and minimum dot product for segment a
		HVector aperp = a1.add(a2);
		double amin = aperp.dot(a1);

		// Test axis and minimum dot product for segment b
		HVector bperp = b1.add(b2);
		double bmin = bperp.dot(b1);

		// Get the normals to the great circles of the segs
		HVector aN = a1.cross(a2);
		HVector bN = b1.cross(b2);

		// Candidate intersection point: exactly one of pt and -pt is possible
		HVector pt = aN.cross(bN).unit();

		// Test the dot product along the test axes
		double adot = pt.dot(aperp);
		double bdot = pt.dot(bperp);

		// Did we get the sign wrong on +pt versus -pt?
		if(adot < 0)
		 {
			adot = -adot;
			bdot = -bdot;
			pt = pt.neg();
		 }

		final double ZERO = 1e-8;
		// Did the dot product tests indicate the segments intersect?
		if(adot - amin >= -ZERO  &&
		   bdot - bmin >= -ZERO)      // "FP-sloppy" comparison
			return  pt;

		return  null;
	 }

	/**
	 ** Rotation around a perpendicular axis. <b>GOOD <i>ONLY</i> FOR
	 ** UNIT VECTORS!
	 **/
	public HVector rotateP(HVector axis, double theta)
	 {
		HVector dir = axis.cross(this); // need .unit() for non-unit vectors

		// We've set up a coordinate system: this is x, dir is y.
		// Return angle theta in that coordinate system and we're done.
		return
			this.mul( Math.cos(theta) ).add(
			 dir.mul( Math.sin(theta) )    );
	 }

	/**
	 * Angular uninterpolation of a vector between two other vectors.
	 * @return Returns the scalar projection of <code>pt</code> onto the
	 * <code>p0</code> to <code>p1</code> line. If between the ends of the line,
	 * the result will be between 0 and 1. If outside the line boundaries, the
	 * value will be less than 0 if on the <code>p0</code> side, and greater than
	 * 1 if on the <code>p1</code> side.
	 **/
	public static double uninterpolate(HVector p0,
									   HVector p1,
									   HVector pt)
	 {
		double part = ( pt.sub(p0).dot(p1.sub(p0).unit()) ); 
		double total = p1.sub(p0).norm();
		
		return  part / total;
	 }

	public final HVector interpolate(HVector p1,
									 double perc)
	 {
		//normal to great-circle-segment (this,p1)
		HVector normal = this.cross(p1).unit();
		double angle = Math.acos(this.dot(p1) / this.norm() / p1.norm());

		HVector direction = this.rotateP(normal, angle * perc);
		direction.normalize();
		return  direction.mul((1-perc) * this.norm() + perc * p1.norm());
	 }

	//////////////////////////////////////////////////////////////////////
	// Misc stuff
	//////////////////////////////////////////////////////////////////////

	public static final HVector fromSpatial(Point2D spatialPt)
	 {
		return  fromSpatial(spatialPt.getX(),
							spatialPt.getY());
	 }

	public static final HVector fromSpatial(double x, double y)
	 {
		// Break down into sin & cos of Theta & Phi
		x = -x;
		double sinTh = Math.sin(Math.toRadians(x));
		double cosTh = Math.cos(Math.toRadians(x));
		double sinPh = Math.sin(Math.toRadians(y));
		double cosPh = Math.cos(Math.toRadians(y));

		return  new HVector(cosTh * cosPh,
							sinTh * cosPh,
							sinPh);
	 }

	//////////////////////////////////////////////////////////////////////
	// Occasional test driver for various features
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] av)
	 throws Throwable
	 {
		PrintStream out = System.out;
		while(in.ready())
		 {
			double lon, lat, ht;
			lon = readDouble();
			lat = readDouble();
			ht = readDouble();
			HVector vertex = new HVector(lon, lat);
			vertex.mulEq(ht);

			double x, y, z;
			x = readDouble();
			y = readDouble();
			z = readDouble();
			HVector dir = new HVector(x, y, z);
			in.readLine();

			HVector pt = intersectMars(vertex, dir);
			out.print("Result: " + pt + "\n");
		 }
	 }

	static BufferedReader in =
		new BufferedReader(new InputStreamReader(System.in));
	static double readDouble()
	 throws Throwable
	 {
		String line = in.readLine();
		return  Double.parseDouble(line);
	 }
 }
