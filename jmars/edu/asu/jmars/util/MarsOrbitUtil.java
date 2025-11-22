package edu.asu.jmars.util;

import java.awt.geom.Point2D;

public class MarsOrbitUtil {
	/**
	 * TODO: blindly lifted out of themis code.  What are some of these constants?
	 * get them documented.
	 */
    public static final double knownET = -21228413.463514;
    public static final double knownLon = 146.995591;
    
    //these are mars constants
    protected static final double secPerDay = 88775.2;
    protected static final double knownTimeAtET = 2.0386;
    protected static final double hrsInDay = 24.0;
    
    
	/**
	 * @param etTime When to compute local time
	 * @param latlong Lon/Lat pair to get local time at
	 * @return Returns local time
	 */
    public static double calcLocalTime(long etTime, Point2D latlong) {

    	if(latlong == null) {
    		return Double.NaN;
    	}
    	//calculate local time
    	double et =  etTime - knownET;
    	double days = et / secPerDay;
    	double hours = (days - (int)days) * hrsInDay;
    	double lon = latlong.getX() - knownLon;
    	if (lon > 0) {
    		lon = lon - 360;
    	}
    	double t = knownTimeAtET + hours - (lon / 15.0);
    	if (t < 0.0) t += hrsInDay;
    	if (t > hrsInDay) t -= hrsInDay;

    	return(t);
    }
    
	/**
	 * Converts ET-seconds to ms.
	 * To get the proper UNIX epoch time, add et to the base unix epoch time for the same time.
	 * The baseMillis is equivalent to January 1, 2000 11:58:55 GMT - which is an et of 0.
	 * @param et ET seconds.
	 * @return 
	 */
	public static long etToMs(long et){
		long baseMillis = 946727935000L;
		long timeNow = baseMillis + (et * 1000); // convert et to ms
		return timeNow;
	}
	
	public static double getHeliocentricLon(long et) 
	{
		long timeNow = etToMs(et);
		MarsOrbit marsOrbit = new MarsOrbit(timeNow);
		double incidentAngle = marsOrbit.getHeliocentricLongitude();
		return incidentAngle;

	}

	/**
	 * Computes the solar incidence angle at the specified time, given the
	 * target position.
	 * Incidence angle is the angle between the sun-vector and surface-normal vector
	 * at the given time.
	 * @param et ET at which to compute the incidence angle.
	 * @param latlong West-leading position of the target.
	 * @return Incidence angle in degrees.
	 */
	public static double getSolarIncidentAngle(long et, Point2D latlong) 
	{
		long timeNow = etToMs(et);
		MarsOrbit marsOrbit = new MarsOrbit(timeNow);
		double incidentAngle = marsOrbit.getZenithAngle(latlong.getX(), latlong.getY());
		return incidentAngle;

	}
	
	/**
	 * Computes the phase angle at the specified time, given observer's position.
	 * Phase angle is the angle between sun-vector and the observer at the
	 * specified time.
	 * @param et ET at which to compute the phase angle.
	 * @param target Target vector from the center of to the surface of Mars.
	 * @param pos Position of spacecraft with respect to Mars.
	 * @return Phase angle in degrees.
	 */
	public static double getPhaseAngle(long et, HVector target, HVector pos){
		long timeNow = etToMs(et);
		MarsOrbit marsOrbit = new MarsOrbit(timeNow);
		double subLon = marsOrbit.getSubsolarLongitude();
		double subLat = Util.ographic2ocentric(marsOrbit.getSolarDeclination());
		HVector sun = HVector.fromSpatial(subLon,subLat);
		sun.mulEq(149598000000D* marsOrbit.getHeliocentricDistance());
		
		return Math.toDegrees(pos.sub(target).separation(sun.sub(target)));
	}
	
	/**
	 * Computes the emission angle at the specified time, given observer's position.
	 * Emission angle is the angle between the surface-normal at target and
	 * the observer's position from the target.
	 * @param target Target vector from the center of to the surface of Mars.
	 * @param pos Position of spacecraft with respect to Mars.
	 * @return Emission angle in degrees.
	 */
	public static double getEmissionAngle(HVector target, HVector pos){
		Point2D tgtLonLat = target.toLonLat(null);
		HVector n = HVector.fromSpatial(tgtLonLat.getX(), Util.ocentric2ographic(tgtLonLat.getY()));
		return Math.toDegrees(n.separation(pos.sub(target)));
	}
	
	/**
	 * Computes distance between Mars and Sun.
	 * @param et ET at which the distance is required.
	 * @return Mars-Sun distance in AU.
	 */
	public static double getMarsSunDistance(long et){
		long timeNow = etToMs(et);
		MarsOrbit marsOrbit = new MarsOrbit(timeNow);
		return marsOrbit.getHeliocentricDistance();
	}
}
