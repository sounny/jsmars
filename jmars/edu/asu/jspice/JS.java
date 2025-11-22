package edu.asu.jspice;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 ** Utility class for basic JSpice wrappers. In general, a routine
 ** belongs in here if it meets all of the following criteria:
 **
 ** <ul>
 ** <li>Doesn't involve any searches, iterations, recursion or for() loops.
 ** <li>Allows SpiceExceptions to propagate, if kernel data is used.
 ** <li>Accepts/returns equivalent Java objects, not double arrays like spice.
 ** </ul>
 **
 ** Functions contained in this class should avoid directly calling
 ** JSpice whenever possible, and instead invoke the appropriate
 ** wrapper. Ideally, no two functions in this class should directly
 ** invoke the same JSpice method.
 **/
public final class JS
 {
    private static DebugLog log = DebugLog.instance();

	public static final int SUN = 10;
	public static final int MARS = Config.get("naif.id.mars", 499);
	public static final int MOON = Config.get("lroc.naif.id.luna", 301);
	public static final int MRO = Config.get("naif.id.mro", -74);
	public static final int LRO = Config.get("lroc.naif.id.lroc", -85);
	
    public  static final String CK_KEY   = "CK";
    public  static final String FK_KEY   = "FK";
    public  static final String LSK_KEY  = "LS";
    public  static final String PCK_KEY  = "PCK";
    public  static final String SCLK_KEY = "SCLK";
    public  static final String SPK_KEY  = "SPK";
    
    /**
     * state.nk keys that have a filename following them as argument.
     */
    public static final Set<String> fileKeys = new HashSet<String>(Arrays.asList(new String[]{
    		CK_KEY,
    		FK_KEY,
    		LSK_KEY,
    		PCK_KEY,
    		SCLK_KEY,
    		SPK_KEY,
    }));

	/**
	 * Length of sidereal day of Mars in hours.
	 * 
	 * <br>Mean Mars sidereal day: 24.6229 hrs
	 * <br>Mean Mars solar day:    24.6597 hrs
	 *
	 * <p>(Taken from the Mars Institute website, see <a
	 * href="http://www.marsinstitute.info/epo/marsfacts.html">here</a>)
	 */
	public static final double SIDEREAL_DAY_MARS = Config.get("sidereal_day.mars", 24.6229);
	
	/**
	 * Length of sidereal day of the Moon in hours.
	 * <p>(Taken from JPL's web-site, see 
	 * <a href="http://solarsystem.jpl.nasa.gov/planets/profile.cfm?Object=Moon&Display=Facts&System=Metric">
	 * here</a>)
	 */
	public static final double SIDEREAL_DAY_MOON = Config.get("sidereal_day.moon", 655.72);

	
	private  final double equatorialRadius; // equatorial radius of planet
	private  final double flattening; // flattening coefficient of planet
	private  final double radii[]; // planet's radii
	private  final double sunRadii[]; // sun's radii
	private  final double sunEquatorialRadius; // sun's equatorial radius
	private  final int craftId; // spacecraft's NAIF id
	private  final int bodyId; // planet's NAIF id (the spacecraft is orbiting around)
	private  final String refFrame; // body fixed frame attached to the body given by bodyId
									// for example, IAU_MARS for MARS
	/** Rotational speed of the planet in km/sec as measured at one km from the axis of rotation. */
	private final double unitRotationSpeed;
	private final JSScanNormalProvider scanNormProvider;
	private final JSNadirProvider nadirProvider;
	
	// Singular instance holders for MRO & LRO.
	private static JS mroInstance = null;
	private static JS lroInstance = null;
	
	/**
	 * Returns an instance suitable for layer.obs.mro
	 */
	public static JS MROInstance(){
		if (mroInstance == null){
			log.println("Configuring MRO instance of JS with MRO="+MRO+" MARS="+MARS+" SIDEREAL_DAY="+SIDEREAL_DAY_MARS);
			mroInstance = new JS(MRO, MARS, SIDEREAL_DAY_MARS, new JSNadirProviderNormal(), new JSScanNormPerpGroundTrack());
		}
		return mroInstance; 
	}

	/**
	 * Returns an instance suitable for layer.obs.lroc
	 * @return
	 */
	public static JS LROInstance(){
		if (lroInstance == null){
			log.println("Configuring LRO instance of JS with LRO="+LRO+" MOON="+MOON+" SIDEREAL_DAY="+SIDEREAL_DAY_MOON);
			lroInstance = new JS(LRO, MOON, SIDEREAL_DAY_MOON, new JSNadirProviderCentroid(), new JSScanNormPerpOrbitTrack());
		}
		return lroInstance;
	}
	
	public int getCraftId(){
		return craftId;
	}
	
	public int getBodyId(){
		return bodyId;
	}
	
	/**
	 * Returns Sun's triaxial ellipsoid's radii.
	 * @return Radii in Km.
	 */
	public double[] getSunRadii(){
		return sunRadii.clone();
	}
	
	/**
	 * Returns Sun's equatorial radius.
	 * @return Equatorial radius in Km.
	 */
	public double getSunEquatorialRadius(){
		return sunEquatorialRadius;
	}
	
	/**
	 * Returns the radii of the body identified by {@link #getBodyId()}.
	 * @return Radii in Km.
	 */
	public double[] getRadii(){
		return radii.clone();
	}
	
	/**
	 * Returns the equatorial radius of the body identified by {@link #getBodyId()}.
	 * @return Equatorial radius in Km.
	 */
	public double getEquatorialRadius(){
		return equatorialRadius;
	}
	
	/**
	 * TODO: replace this with something similar to MttFileSet.
	 * Very simple state.nk file loader. Use with caution.
	 * @param fname state.nk file path. 
	 * @throws IOException Generated due to exception in either reading of
	 *         the state.nk file or any of the kernels.
	 * @throws FileNotFoundException If either the state.nk file is not found
	 *         or one of the kernels specified in the state.nk file is not found.
	 */
	public static void loadStateFile(String fname) throws IOException, FileNotFoundException {
		BufferedReader fin = null;
		String fpath = (new File(fname)).getAbsoluteFile().getParent();
		fin = new BufferedReader(new FileReader(fname));

		for (String line = fin.readLine(); line != null; line = fin.readLine()) {
			line = line.trim();
			if (line.startsWith("#") || line.equals(""))
				continue;

			StringTokenizer tok = new StringTokenizer(line);
			if (tok.countTokens() != 2)
				throw new IOException("Found " + tok.countTokens()
						+ " token(s) (expected 2)");

			String newWhat = tok.nextToken();
			if (!newWhat.endsWith(":"))
				throw new IOException("Missing colon");

			newWhat = newWhat.substring(0, newWhat.length() - 1).toUpperCase();
			String newFname = tok.nextToken();
			if (fileKeys.contains(newWhat)){
				// generate absolute path names for non-data keys
				if (!(new File(newFname)).isAbsolute()) {
					newFname = (new File(fpath, newFname)).getAbsolutePath();
				}
			}

			JSpice.furnshc(new StringBuffer(newFname));
		}
	}

	 
	 /**
		 * Intialize a JS object for the specified spacecraft going around the
		 * body.
		 * 
		 * @param spacecraft
		 * @param body
		 */
	 public JS(int spacecraft, int body, double lengthSiderealDay, JSNadirProvider nadirProvider, JSScanNormalProvider scanNormal) {
		 this.nadirProvider = nadirProvider;
		 this.scanNormProvider = scanNormal;
		 craftId = spacecraft;
		 bodyId = body;
		 refFrame = getBodyFixedFrame(body);
		 radii = getRadii(bodyId);
		 equatorialRadius = radii[0];
		 flattening = (equatorialRadius - radii[2]) / equatorialRadius;
		 sunRadii = getRadii(SUN);
		 sunEquatorialRadius = sunRadii[0];
		 unitRotationSpeed = getUnitRotationSpeed(lengthSiderealDay);
	 }
	 
	 private String getBodyFixedFrame(int bodyId){
		 int[] frcode = new int[1];
		 boolean[] found = new boolean[1];
		 int lenout = 256;
		 StringBuffer frname = new StringBuffer(lenout);
		 
		 JSpice.cidfrmc(bodyId, lenout, frcode, frname, found);
		 if (found[0])
			 return frname.toString();
		 
		 return null;
	 }
	 
	 public static String frmnamc(int body){
		 StringBuffer frname = new StringBuffer();
		 JSpice.frmnamc(body, 100, frname);
		 return frname.toString();
	 }
	 
	 public static double[] getRadii(int body){
		 int[] dim = new int[1];
		 double[] bodyRadii = new double[3];
		 JSpice.bodvarc(body, new StringBuffer("RADII"), dim, bodyRadii);
		 return bodyRadii;
	 }
	 
	 public static String bodc2nc(int body){
		 boolean[] found = { false };
		 StringBuffer bodyName = new StringBuffer();
		 
		 JSpice.bodc2nc(body, 256, bodyName, found);
		 if (found[0])
			 return bodyName.toString().trim();
		 
		 return null;
	 }
	 
	 /**
	  * Returns the rotational speed of the planet (or body), in km/sec,
	  * as measured at one km from the axis of rotation.
	  * @param lengthSiderealDay Length (in hours) of the sidereal day of
	  *        the planet (or body).
	  */
	 protected double getUnitRotationSpeed(double lengthSiderealDay){
		 return Math.PI*2 / (lengthSiderealDay * 60 * 60);
	 }

	/**
	 ** Java wrapper for SPICE inelpl routine. Given an ellipse and a
	 ** plane (as represented by 9-element and 4-element arrays,
	 ** respectively), calculates the intersection point(s).
	 **
	 ** @return The intersection(s), which may be zero in length but
	 ** will never be null.
	 **/
	public  HVector[] inelpl(double[] ellipse, double[] plane)
	 {
		double[] pt1 = new double[3];
		double[] pt2 = new double[3];
		int[] ptCount = { -1 };
		JSpice.inelpl(ellipse, plane, ptCount, pt1, pt2);

		if(ptCount[0] == 2)
			return  new HVector[] { new HVector(pt1),
									new HVector(pt2) };
		if(ptCount[0] == 1)
			return  new HVector[] { new HVector(pt1) };

		if(ptCount[0] == 0)
			return  new HVector[0];

		// Should be impossible
		log.printStack(-1);
		throw  new SpiceException(
			"SPICE returned invalid results from INELPL: ptCount="+ptCount[0]);
	 }

	/**
	 ** Java wrapper for SPICE surfpt routine. Given a point and a
	 ** ray, calculates the intersection with an ellipsoid.
	 **/
	public  HVector surfpt(HVector point, HVector ray,
								 double[] radii)
	 {
		double[] pt = new double[3];
		boolean[] found = new boolean[1];
		JSpice.surfptc(point.toArray(),
					   ray.toArray(),
					   radii[0], radii[1], radii[2],
					   pt, found);
		return  found[0] ? new HVector(pt) : null;
	 }

	/**
	 ** Java wrapper for SPICE surfpt routine, but with the assumption
	 ** of the {@link #bodyId} ellipsoid's radii. Given a point and a ray,
	 ** calculates the intersection with the {@link #bodyId} reference ellipsoid.
	 ** For example, for Mars, this routine will return points on Mars reference
	 ** ellipsoid.
	 **/
	public  HVector surfpt(HVector point, HVector ray)
	 {
		return  surfpt(point, ray, radii);
	 }

	/**
	 ** Java wrapper for SPICE surfpt routine, but with the assumption
	 ** of the Mars ellipsoid's radii. Given a point and a ray,
	 ** calculates the intersection with the {@link #bodyId} reference ellipsoid
	 ** expanded to the given altitude in kilometers.
	 **/
	public  HVector surfpt(HVector point, HVector ray, double alt)
	 {
		return  surfpt(point, ray, new double[] { radii[0] + alt,
												  radii[1] + alt,
												  radii[2] + alt });
	 }

	/**
	 ** Java wrapper for SPICE surfpt routine, but with the
	 ** assumption of the {@link #bodyId} ellipsoid's radii and a starting
	 ** point of the center of the planet. Given a ray, calculates
	 ** the intersection with the {@link #bodyId} reference ellipsoid. Useful
	 ** for converting a unit areocentric vector to a surface
	 ** vector for areographic use.
	 **/
	public  HVector surfpt(HVector ray)
	 {
		return  surfpt(HVector.ORIGIN, ray, radii);
	 }

	/**
	 ** Given an ET, returns the slewed velocity vector of the
	 ** {@link #craftId} relative to {@link #bodyId}.
	 **/
    public  HVector getVel(double et)
	 {
		return  spkez(et, craftId)[1];
	 }

    /**
	 ** Returns the non-normalized position of the {@link #craftId} at the
	 ** given ET, re-using the given HVector. The position is returned
	 ** in the body fixed reference frame attached to {@link #bodyId}.
	 **
	 ** @throws NullPointerException if v is null.
	 **/
	public  HVector getPos(double et, HVector v)
	 {
		double[] pos = new double[3];
		double[] lt = new double[1];
		JSpice.spkezpc(craftId,
					   et,
					   new StringBuffer(refFrame),
					   new StringBuffer("NONE"),
					   bodyId,
					   pos,
					   lt);
		v.set(pos);
		return  v;
	 }

	/**
	 ** Returns the non-normalized position of the spacecraft at the
	 ** given ET.
	 **/
	public  HVector getPos(double et)
	 {
		return  spkez(et, craftId)[0];
	 }

	/**
	 ** Implements an LRU caching map with a fixed capacity, where the
	 ** LRU item is discarded whenever an add would exceed the
	 ** capacity.
	 **/
	private  final class VectorCache extends LinkedHashMap
	 {
		VectorCache()
		 {
			super(cacheSize+1, 1f, true);
		 }

		public boolean removeEldestEntry(Map.Entry eldest)
		 {
			return  size() > cacheSize;
		 }
	 }

	private  final int cacheSize = Config.get("js.cachesize", 0);
	private  final boolean cacheEnabled = cacheSize!=0;
	private  final Map cacheTgt = new VectorCache();
	private  final Map cacheSpk = new VectorCache();

    /**
	 ** Sets the supplied vectors to the non-normalized position and
	 ** velocity of the spacecraft at the given ET.
	 **
	 ** @param pos receives the position
	 ** @param vel receives the velocity
	 **/
	public  void getPosVel(double et, HVector pos, HVector vel)
	 {
		spkez(et, craftId, pos, vel);
	 }

	/**
	 ** Given the position and velocity vectors from a spacecraft
	 ** state, removes planetary rotation from the velocity vector. The
	 ** position vector is unaffected, but required to calculate the
	 ** proper rotation velocity.
	 **/
	public  void removePlanetaryRotation(HVector pos, HVector vel)
	 {
		HVector rot = new HVector(-pos.y, pos.x, 0);
		rot.mulEq(rot.normalize() * unitRotationSpeed);
		vel.subEq(rot);
	 }

	/**
	 ** Returns the non-normalized position and velocity vectors of
	 ** the spacecraft at the given ET.
	 **
	 ** @return position and velocity (in body-fixed coordinates
	 ** attached to {@link #bodyId}) as a two-element array, in that order
	 **/
	public  HVector[] getPosVel(double et)
	 {
		HVector[] pv = { new HVector(),
						 new HVector() };
		getPosVel(et, pv[0], pv[1]);
		return  pv;
	 }

	/**
	 ** Given a time, returns the scan plane normal as a unit vector.
	 **/
	public HVector getScanNorm(double et){
		return scanNormProvider.getScanNorm(this, et);
	}

	/**
	 ** Given a pos and velocity, returns the scan plane normal as
	 ** a unit vector. Saves NAIF calls if you already have these
	 ** vectors.
	 **/
	public  HVector getScanNorm(HVector pos, HVector vel){
		return scanNormProvider.getScanNorm(this, pos, vel);
	}

	/**
	 * Given a time and a roll angle, returns the look vector of the
	 * spacecraft.
	 * @param et   ET at which look vector is desired.
	 * @param roll Slew angle in degrees.
	 * @return Look vector from the spacecraft rolled in the scan plane
	 *         by the amount of roll angle.
	 */
	public  HVector getLook(double et, double roll)
	 {
		return  getNadir(getPos(et)).rotate(getScanNorm(et),
											Math.toRadians(roll));
	 }

	/**
	 ** Returns the nadir look vector toward the planet, from the
	 ** given position vector.
	 **/
	public  HVector getNadir(HVector p){
		return nadirProvider.getNadir(this, p);
	}

	/**
	 ** Returns the normal look vector toward the planet, from the
	 ** given time.
	 **/
	public  HVector getNadir(double et){
		return nadirProvider.getNadir(this, et);
	}

	/**
	 * Given a time and a roll angle, returns the surface target
	 * point. Returns null if the planet isn't hit by the viewing ray
	 * at that roll angle.
	 * 
	 * @param et   ET at target point.
	 * @param roll Slew/roll angle in degrees.
	 */
	public  HVector getTarget(double et, double roll)
	 {
		if (!cacheEnabled){
			return  surfpt(getPos(et), getLook(et, roll));
		}
		else {
			TargetKey key = new TargetKey(et, roll, 0);
			HVector target;
			if ((target = (HVector)cacheTgt.get(key)) == null){
				target = surfpt(getPos(et), getLook(et, roll));
				cacheTgt.put(key, target);
			}
			return target;
		}
	 }

	/**
	 * Given a time, a roll angle, and an areodetic altitude, returns
	 * the surface target point under the assumption that the surface
	 * is 'alt' above the ideal ellipsoid. Returns null if the planet
	 * isn't hit by the viewing ray at that roll angle.
	 *
	 * @param et   ET at target point.
	 * @param roll Slew/roll angle in degrees.
	 * @param alt  Altitude from the reference ellipsoid.
	 */
	public  HVector getTarget(double et, double roll, double alt)
	 {
		if (!cacheEnabled){
			return surfpt(getPos(et), getLook(et, roll), alt);
		}
		else {
			TargetKey key = new TargetKey(et, roll, alt);
			HVector target;
			if (cacheTgt.containsKey(key)){
				target = (HVector)cacheTgt.get(key);
			}
			else {
				target = surfpt(getPos(et), getLook(et, roll), alt);
				cacheTgt.put(key, target);
			}
			return target;
		}
	 }

	/**
	 ** Given a time and a roll angle, returns the surface target
	 ** point or limb. Returns the limb only if the planet isn't hit
	 ** by the viewing ray at that roll angle.
	 **/
	public  HVector getTargetOrLimb(double et, double roll, double alt)
	 {
		HVector target = getTarget(et, roll, alt);
		if(target != null)
			return  target;

		HVector[] limbPts = getLimbPts(et, alt);
		switch(limbPts.length)
		 {
		 case 2:
			return  roll < 0 ? limbPts[0] : limbPts[1];

		 case 1:
			log.aprintln("Suspicious limb at et=" + et + " / roll=" + roll);
			return  limbPts[0];

		 default:
			log.aprintln(et);
			log.aprintln(roll);
			throw  new SpiceException("Unable to calculate limb at et=" + et +
									  " / roll=" + roll);
		 }
	 }

	/**
	 ** Given a time, returns the two limb points (the horizon points
	 ** on the planet, per the spacecraft's position). Technically,
	 ** there may be zero or one limb points if the spacecraft
	 ** geometry is just right... this is reflected in the length of
	 ** the return array.
	 **
	 ** @return 0, 1, or (normally) 2 limb points, but never null. If
	 ** two are returned, they are in order according to the roll
	 ** required to reach them.
	 **/
	public  HVector[] getLimbPts(double et, double alt)
	 {
		HVector[] posvel = getPosVel(et);
		double[] ellipse = edlimb(posvel[0], alt);
		HVector scanNorm = getScanNorm(posvel[0], posvel[1]);
		HVector nadir = getNadir(posvel[0]);
		double[] viewPlane = nvp2pl(scanNorm, nadir);
		/*
		System.err.print("Ellipse:");
		for(int i=0; i<ellipse.length; i++)
			System.err.print((i>0?",":"")+ellipse[i]);
		System.err.println();
		*/
		HVector[] limbPts = inelpl(ellipse, viewPlane);
		HVector posRollDir = scanNorm.ucross(nadir);
		if(limbPts.length == 2  &&
		   limbPts[0].dot(posRollDir) > limbPts[1].dot(posRollDir))
		 {
			HVector tmp = limbPts[0];
			limbPts[0] = limbPts[1];
			limbPts[1] = tmp;
		 }
		return  limbPts;
	 }

	/**
	 ** Java wrapper for SPICE tipbod function. Returns a 3x3 matrix
	 ** instead as well as (optionally) filling a given one.
	 **
	 ** @param tipm A 3x3 (pre-allocated) matrix to be filled, OR null.
	 ** @return tipm if it was non-null, otherwise a newly-allocated matrix.
	 **/
	public  double[][] tipbod(String refFrame,
									int body,
									double et,
									double[][] tipm)
	 {
		double[] tmp = new double[3*3];
		JSpice.tipbodc(new StringBuffer(refFrame), body, et, tmp);

		if(tipm == null)
			tipm = new double[3][3];

		System.arraycopy(tmp, 0, tipm[0], 0, 3);
		System.arraycopy(tmp, 3, tipm[1], 0, 3);
		System.arraycopy(tmp, 6, tipm[2], 0, 3);

		return  tipm;
	 }

	/**
	 ** Java wrapper for SPICE spkez function. Returns the state of an
	 ** arbitrary body with respect to {@link #bodyId}, in the {@link #refFrame}
	 ** (rotating/body-fixed) coordinate system.
	 **
	 ** @return An array { pos, vel } representing the state of the
	 ** body with respect to {@link #bodyId}, or null if the state cannot be
	 ** determined.
	 **/
	public  HVector[] spkez(double et, int BODY)
	 {
		HVector[] state = { new HVector(),
							new HVector() };
		spkez(BODY, et, refFrame, bodyId, state);
		return  state;
	 }

	/**
	 ** Java wrapper for SPICE spkez function. Returns the state of an
	 ** arbitrary body with respect to {@link #bodyId} within pos and vel, in the
	 ** {@link #refFrame} (rotating/body-fixed) coordinate system.
	 **/
	public  void spkez(double et, int craft, HVector pos, HVector vel)
	 {
		spkez(craft, et, refFrame, bodyId, new HVector[] { pos, vel });
	 }

	/**
	 ** Java wrapper for SPICE spkez function. Returns the state of a
	 ** target body with respect to an observing body, in the refFrame
	 ** coordinate system.
	 **
	 ** @param targBody a NAIF id code for the target
	 ** @param et ephemeris time
	 ** @param refFrame the coordinate system (i.e. "J2000", "IAU_MARS")
	 ** @param obsBody a NAIF id code for the observer
	 ** @param state must have at least two elements... on exit,
	 ** contains position then velocity
	 **/
	public  void spkez(int targBody,
							 double et,
							 String refFrame,
							 int obsBody,
							 HVector[] state)
	 {
		if(Double.isNaN(et))
			throw  new SpiceException("et = NaN passed into spkez");

		SpkKey key = null;
		double[] stateRaw = null;
		boolean found = false;
		if (cacheEnabled){
			key = new SpkKey(et, targBody, obsBody, refFrame);
			stateRaw = (double[])cacheSpk.get(key);
		}
		
		if (stateRaw == null){
			stateRaw = new double[6];
			double[] lt = new double[1];
			try {
				JSpice.spkezc(targBody,
						et,
						new StringBuffer(refFrame),
						new StringBuffer("NONE"),
						obsBody,
						stateRaw,
						lt);
			}
			catch(SpiceException e) {
				log.println(">>>>>>>>>>>> et = " + et);
				throw  e;
			}
		}
		else {
			found = true;
		}
		
		if (cacheEnabled && !found){
			cacheSpk.put(key, stateRaw);
		}
		
		state[0].set(stateRaw);
		state[1].set(stateRaw, 3);
	 }

	/**
	 ** Java wrapper for SPICE surfnm function, but doesn't require
	 ** the point to be on the surface. Returns a unit vector that, if
	 ** extended from the supplied pt, would be normal to {@link #bodyId}'s
	 ** surface (outward-pointing).
	 **/
	public  HVector surfnm(HVector pt)
	 {
		// "Ground" the point if necessary (project it onto the surface)
		double[] lla = recgeo(pt);
		if(Math.abs(lla[2]) > 1e-8) // altitude
			pt = georec(lla[0], lla[1], 0);

		double[] normal = new double[3];
		JSpice.surfnmc(radii[0], radii[1], radii[2],
					   pt.toArray(), normal);
		return  new HVector(normal);
	 }

	/**
	 ** Returns the areodetic east-leading longitude/latitude/altitude
	 ** (in that order) for a given vector projected onto the
	 ** surface. Convenience method, equivalent to {@link recgeo
	 ** recgeo}({@link surfpt surfpt}({@link HVector#ORIGIN}, pt)).
	 **/
	public  double[] recgeoS(HVector pt)
	 {
		return  recgeo(surfpt(HVector.ORIGIN, pt));
	 }

	/**
	 ** Java wrapper for SPICE edlimb function. Returns a 9-element
	 ** array representing the ellipse of the {@link #bodyId} limb from the given
	 ** viewing point.
	 **/
	public  double[] edlimb(HVector pt, double alt)
	 {
		double[] ellipse = new double[9];
		JSpice.edlimb(new double[] { radii[0]+alt },
					  new double[] { radii[1]+alt },
					  new double[] { radii[2]+alt },
					  pt.toArray(),
					  ellipse);
		return  ellipse;
	 }

	/**
	 ** Java wrapper for SPICE nvp2pl function. Returns a 4-element
	 ** array representing a plane with the given normal that passes
	 ** through the given point.
	 **/
	public  double[] nvp2pl(HVector norm, HVector point)
	 {
		double[] plane = new double[4];
		JSpice.nvp2pl(norm.toArray(), point.toArray(), plane);
		return  plane;
	 }

	/**
	 ** Java wrapper for SPICE georec function, but uses
	 ** degrees. Returns the vector representing the given areodetic
	 ** east-leading longitude/latitude/altitude combination. Angles
	 ** are in degrees, altitude is in kilometers (relative to the
	 ** reference ellipsoid).
	 **/
	public  HVector georec(double lon,
								 double lat,
								 double alt)
	 {
		double[] coords = new double[3];
		JSpice.georecc(Math.toRadians(lon), Math.toRadians(lat), alt,
					   equatorialRadius, flattening, coords);
		return  new HVector(coords);
	 }

	/**
	 ** Java wrapper for SPICE recgeo function, but uses degrees and
	 ** 0-360 longitudes. Returns the areodetic east-leading
	 ** longitude/latitude/altitude (in that order) for a given
	 ** vector. Angles are returned in degrees. Altitude is returned
	 ** in kilometers (relative to the reference ellipsoid). Longitude
	 ** is returned between 0 and 360 (NOT -180 and 180).
	 **/
	public  double[] recgeo(HVector v)
	 {
		double[] coords = v.toArray();
		double[] lon = new double[1];
		double[] lat = new double[1];
		double[] alt = new double[1];
		JSpice.recgeoc(coords, equatorialRadius, flattening,
					   lon, lat, alt);
		return  new double[] { (Math.toDegrees(lon[0]) + 360) % 360,
							   Math.toDegrees(lat[0]),
							   alt[0] };
	 }

	/**
	 ** Java wrapper for SPICE recgeo function, but uses degrees and
	 ** 0-360 longitudes. Returns the areodetic east-leading
	 ** longitude/latitude/altitude (in that order) for a given
	 ** vector. Angles are returned in degrees. Altitude is returned
	 ** in kilometers (relative to the reference ellipsoid). Longitude
	 ** is returned between 0 and 360 (NOT -180 and 180).
	 **/
	public  double[] recgeo(HVector v, double[] lla)
	 {
		double[] coords = v.toArray();
		double[] lon = new double[1];
		double[] lat = new double[1];
		double[] alt = new double[1];
		JSpice.recgeoc(coords, equatorialRadius, flattening,
					   lon, lat, alt);
		lla[0] = (Math.toDegrees(lon[0]) + 360) % 360;
		lla[1] = Math.toDegrees(lat[0]);
		lla[2] = alt[0];

		return  lla;
	 }

	public  void main(String[] av)
	 {
		double[] coords = new double[] { Double.parseDouble(av[0])*3400,
										 Double.parseDouble(av[1])*3400,
										 Double.parseDouble(av[2])*3400 };
		double[] lon = new double[1];
		double[] lat = new double[1];
		double[] alt = new double[1];
		JSpice.recgeoc(coords, equatorialRadius, flattening,
					   lon, lat, alt);

		log.aprintln((Math.toDegrees(lon[0]) + 360) % 360);
		log.aprintln(Math.toDegrees(lat[0]));
		log.aprintln(alt[0]);
	 }

	public  static double[] nvc2pl(HVector norm, double con)
	 {
		double[] plane = new double[4];
		JSpice.nvc2pl(norm.toArray(), new double[] { con }, plane);
		return  plane;
	 }

	public  double[] inedpl(double[] plane)
	 {
		double[] ellipse = new double[9];
		boolean[] found = new boolean[1];
		JSpice.inedpl(new double[] { radii[0] },
					  new double[] { radii[1] },
					  new double[] { radii[2] }, plane, ellipse, found);
		return  found[0] ? ellipse : null;
	 }
	
	/**
	 * A map from kernel file name to reference count. Maintains the list
	 * of loaded kernels.
	 */
	private static Map<String,Integer> loadedKernels = new HashMap<String,Integer>();
	
	/**
	 * Loads the specified kernel into the SPICE space. If it is already
	 * loaded, increment the reference counter only.
	 * @param path Path to the kernel file to load.
	 */
	public static void furnshc(String path){
		synchronized(loadedKernels){
			if (loadedKernels.containsKey(path)){
				loadedKernels.put(path, new Integer(loadedKernels.get(path).intValue()+1));
				log.println("Incremented ref-count for kernel "+path+" to "+loadedKernels.get(path)+".");
			}
			else {
				loadedKernels.put(path, new Integer(1));
				JSpice.furnshc(new StringBuffer(path));
				log.println("Added kernel "+path+" to pool.");
			}
		}
	}
	
	/**
	 * Decrements the reference count of the specified kernel, when it reaches
	 * zero the kernel is unloaded/unfurnished from the SPCIE space.
	 * @param path Path to the kernel file to load.
	 */
	public static void unloadc(String path){
		synchronized(loadedKernels){
			if (!loadedKernels.containsKey(path))
				throw new IllegalArgumentException("Kernel "+path+" is not known to be loaded.");
			
			loadedKernels.put(path, new Integer(loadedKernels.get(path).intValue()-1));
			if (loadedKernels.get(path).intValue() == 0){
				loadedKernels.remove(path);
				JSpice.unloadc(new StringBuffer(path));
				log.println("Removed kernel "+path+" from pool.");
			}
			else {
				log.println("Decremented ref-count for kernel "+path+" to "+loadedKernels.get(path)+".");
			}
		}
	}
	
	// The following two should be moved into Util somewhere.
	public static String cachePath = Main.getJMarsPath() + "localcache/";
	/**
	 * Retrieves the given remote file, caches it in cachePath. Subsequent
	 * calls return the cached copy. The cached copy is brought up-to-date
	 * with respect to the remoteUrl before being returned if updateCheck
	 * is <code>true</code>.
	 * @param remoteUrl URL of the source file
	 * @param updateCheck Whether to check for updates or not. This is only
	 *        applicable if the file exists already. If not, an update is automatically
	 *        performed.
	 * @return <code>null</code> in case of an error, or the {@link File} in case
	 *         of success.
	 */
	// TODO: use EHCache instead?
	public static File getCachedFile(String remoteUrl, boolean updateCheck) {
		try {
			URL url = new URL(remoteUrl);
			
			File localFile = new File(cachePath + url.getFile().replaceAll("[^a-zA-Z0-9]", "_"));
			if (!updateCheck){
				if (localFile.exists()){
					log.println("No update check requested, returning existing file.");
					return localFile;
				}
				else {
					log.println("No update check requested, but the file does not exist. Forcing update.");
				}
			}
			
			URLConnection conn = url.openConnection();
			conn.setRequestProperty("User-Agent", "Java");
			if (!(localFile.exists() && localFile.lastModified() == conn.getLastModified())){
				if (localFile.exists())
					log.println("File from "+remoteUrl+" is out of date ("+(new Date(localFile.lastModified()))+" vs "+(new Date(conn.getLastModified()))+").");
				else
					log.println("File from "+remoteUrl+" is not cached locally.");
				
				new File(cachePath).mkdirs();
				InputStream is = conn.getInputStream();
				OutputStream os = new BufferedOutputStream(new FileOutputStream(localFile));
				byte[] buff = new byte[1024];
				int nread;
				while((nread = is.read(buff)) > -1)
					os.write(buff, 0, nread);
				os.close();
				if (conn.getLastModified() != 0)
					localFile.setLastModified(conn.getLastModified());
				
				log.println("Downloaded file from " + remoteUrl+ " modification date: "+(new Date(conn.getLastModified())));
			}
			else {
				log.println("Using cached copy for "+remoteUrl+".");
			}
			return localFile;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Key for caching {@link JS#spkez(int, double, String, int, HVector[])} data.
	 */
	private static final class SpkKey {
		public final double et;
		public final int    obsBody;
		public final int    tgtBody;
		public final String refFrame;
		public final int    hashCode;
		
		public SpkKey(double et, int tgtBody, int obsBody, String refFrame){
			this.et = et;
			this.tgtBody = tgtBody;
			this.obsBody = obsBody;
			this.refFrame = refFrame.intern();
			
			int hash = 0;
			long l = Double.doubleToLongBits(et);
			hash += (int)(l ^ (l>>>32));
			hash += obsBody;
			hash += tgtBody;
			hash ^= refFrame.hashCode();
			
			hashCode = hash;
		}
		
		public int hashCode(){
			return hashCode;
		}
		
		public boolean equals(Object obj){
			if (!(obj instanceof SpkKey))
				return false;
			
			SpkKey other = (SpkKey)obj;
			return (et == other.et && obsBody == other.obsBody && tgtBody == other.tgtBody && refFrame == other.refFrame);
		}
		
		public String toString(){
			return getClass().getSimpleName()+"[et="+et+",src="+obsBody+",tgt="+tgtBody+",frame="+refFrame+"]";
		}
	}

	/**
	 * Key for caching {@link JS#getTarget(double, double, double)} data. 
	 */
	private static final class TargetKey{
		public final double et;
		public final double roll;
		public final double alt;
		public final int hashCode;
		
		public TargetKey(double et, double roll, double alt){
			this.et = et;
			this.roll = roll;
			this.alt = alt;
			
			long l;
			int hash = 0;
			l = Double.doubleToLongBits(alt);
			hash = (int)(l ^ (l>>>32));
			l = Double.doubleToLongBits(roll);
			hash = hash * 32 + (int)(l ^ (l>>>32));
			l = Double.doubleToLongBits(et);
			hash = hash * 32 + (int)(l ^ (l>>>32));
			
			this.hashCode = hash;
		}
		
		public int hashCode(){
			return hashCode;
		}
		
		public boolean equals(Object obj){
			if (!(obj instanceof TargetKey))
				return false;
			
			TargetKey t2 = (TargetKey)obj;
			return (et == t2.et && roll == t2.roll && alt == t2.alt);
		}
		
		public String toString(){
			return "TargetKey[et="+et+",roll="+roll+",alt="+alt+"]";
		}
	}
	
 }
