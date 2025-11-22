package edu.asu.jmars.util;


/**
 * The following classes were partially liberated from a Nasa web 
 * site - its all still public dollars..
 * This seems to have been adapted from Mars24 tool. 
 */
public class MarsOrbit extends JulianTime {

	MarsOrbit(long aTimeInMillis){
		super(aTimeInMillis);
		meanAnomaly = 0.0D;
		alphaFMS = 0.0D;
		ellSRad = 0.0D;
		ellSDeg = 0.0D;
		deltaS = 0.0D;
		rM = 0.0D;
		lambdaM = 0.0D;
		equationOfTime = 0.0D;
		primeMeridian = 0.0D;
		calculateOrbit();
	}

	public void calculateOrbit(){
		meanAnomaly = (19.387D + 0.52402075000000004D * super.delta2000) * RAD_PER_DEG;
		double sin1MeanAnom = Math.sin(meanAnomaly);
		double sin2MeanAnom = Math.sin(2D * meanAnomaly);
		double sin3MeanAnom = Math.sin(3D * meanAnomaly);
		double sin4MeanAnom = Math.sin(4D * meanAnomaly);
		double sin5MeanAnom = Math.sin(5D * meanAnomaly);
		alphaFMS = 270.38630000000001D + 0.52403840000000002D * super.delta2000;
		double pbs = 0.0D;
		for(int k = 1; k < 8; k++)
			pbs += pbsA[k] * Math.cos((pbsPhi[k] + pbsTauIn[k] * super.delta2000) * RAD_PER_DEG);

		for(ellSDeg = alphaFMS + (10.691000000000001D + 2.9999999999999999E-007D * super.delta2000) * sin1MeanAnom + 0.623D * sin2MeanAnom + 0.050000000000000003D * sin3MeanAnom + 0.0050000000000000001D * sin4MeanAnom + 0.00050000000000000001D * sin5MeanAnom + pbs; ellSDeg < 0.0D; ellSDeg += 360D);
		for(; ellSDeg > 360D; ellSDeg -= 360D);
		ellSRad = ellSDeg * RAD_PER_DEG;
		deltaS = DEG_PER_RAD * Math.asin(0.42564999999999997D * Math.sin(ellSRad)) + 0.25D * Math.sin(ellSRad);
		rM = 1.5236000000000001D * (1.0043599999999999D - 0.093090000000000006D * Math.cos(meanAnomaly) - 0.0043600000000000002D * Math.cos(2D * meanAnomaly) - 0.00031D * Math.cos(3D * meanAnomaly));
		for(lambdaM = (ellSDeg + 85.061000000000007D) - 0.014999999999999999D * Math.sin(2D * ellSRad + 71D * RAD_PER_DEG) - 5.4999999999999999E-006D * super.delta2000; lambdaM < 0.0D; lambdaM += 360D);
		for(; lambdaM > 360D; lambdaM -= 360D);
		equationOfTime = ((2.8610000000000002D * Math.sin(2D * ellSRad) - 0.070999999999999994D * Math.sin(4D * ellSRad)) + 0.002D * Math.sin(6D * ellSRad)) - ((10.691000000000001D + 2.9999999999999999E-007D * super.delta2000) * sin1MeanAnom + 0.623D * sin2MeanAnom + 0.050000000000000003D * sin3MeanAnom + 0.0050000000000000001D * sin4MeanAnom + pbs);
		for(primeMeridian = 313.476D + 350.89198520000002D * super.delta2000; primeMeridian < 0.0D; primeMeridian += 360D);
		for(; primeMeridian > 360D; primeMeridian -= 360D);
	}
	
	/**
	 * @deprecated The implementation {@link edu.asu.jmars.util.Util.lsubs(double)} is
	 * more precise, and should be used instead.
	 * @return The lsubs of the unix epoch this orbit was constructed with.
	 */
	public double getSolarLongitude(){
		return ellSDeg;
	}

	public String getSolarLongitudeString(long inPrecision){
		return Util.formatDouble(ellSDeg, (int)inPrecision);
	}

	/**
	 * @return planetographic solar declination.
	 */
	public double getSolarDeclination(){
		return deltaS;
	}

	public double getSubsolarLongitude(){
		double result;
		for(result = 15D * getTime(0.0D, 2) - 180D; result < 0.0D; result += 360D);
		for(; result > 360D; result -= 360D);
		return result;
	}

	/**
	 * @return West-leading heliocentric longitude.
	 */
	public double getHeliocentricLongitude(){
		return lambdaM;
	}

	/**
	 * @return Distance between Sun and Mars in AU.
	 */
	public double getHeliocentricDistance(){
		return rM;
	}

	public String GetTimestr(double longitude, int timeType){
		return getTimestr(longitude, timeType, true);
	}

	/**
	 * Computes and returns local time as a string.
	 * @param longitude Planetographic longitude.
	 * @param timeType 1=LMST, 2=LTST
	 * @param appendType Appends the string LMST or LTST as necessary to output string.
	 * @return local time as a string.
	 */
	public String getTimestr(double longitude, int timeType, boolean appendType){
		double localTime = getTime(longitude, timeType);
		long tHour = (long)localTime;
		long tMin = (long)((localTime - (double)tHour) * 60D);
		long tSec = (long)(((localTime - (double)tHour) * 60D - (double)tMin) * 60D);
		StringBuffer result = new StringBuffer(Util.zeroPadInt(tHour, 2) + ":" + Util.zeroPadInt(tMin, 2) + ":" + Util.zeroPadInt(tSec, 2));
		if(appendType)
			if(timeType == 2)
				result.append(" LTST");
			else
				result.append(" LMST");
		return result.toString();
	}

	/**
	 * Computes local time.
	 * @param longitude Planetographic longitude.
	 * @param timeType 1=LMST, 2=LTST
	 * @return local time at the specified longitude.
	 */
	public double getTime(double longitude, int timeType){
		double mst0 = 44795.999280000004D + (super.jdTT - 2451549.5D) / 1.02749125D;
		double mst = (mst0 - (double)(long)mst0) * 24D;
		double result = mst - longitude * 0.066666666666666666D;
		if(timeType == 2)
			result += equationOfTime / 15D;
		for(; result < 0.0D; result += 24D);
		for(; result > 24D; result -= 24D);
		return result;
	}

	public double marsSolarDay(){
		return 44795.999280000004D + (super.jdTT - 2400000.5D - 51549D) / 1.02749125D;
	}

	/**
	 * Computes the solar zenith angle at the given location.
	 * @param longitude Planetographic longitude of the location.
	 * @param latitude Planetographic latitude of the location.
	 * @return solar zenith angle in degrees.
	 */
	public double getZenithAngle(double longitude, double latitude){
		double subsolarLat = getSolarDeclination();
		double subsolarLong = getSubsolarLongitude();
		double cosColatitudeS = Math.cos((90D - subsolarLat) * RAD_PER_DEG);
		double sinColatitudeS = Math.sin((90D - subsolarLat) * RAD_PER_DEG);
		double cosDTheta = Math.cos((subsolarLong - longitude) * RAD_PER_DEG);
		double cosColatitudeX = Math.cos((90D - latitude) * RAD_PER_DEG);
		double sinColatitudeX = Math.sin((90D - latitude) * RAD_PER_DEG);
		double cosZenithAngle = cosColatitudeS * cosColatitudeX + sinColatitudeS * sinColatitudeX * cosDTheta;
		return Math.acos(cosZenithAngle) * DEG_PER_RAD;
	}

	/**
	 * Computes the solar azimuth angle at the given location.
	 * @param longitude Planetographic longitude of the location.
	 * @param latitude Planetographic latitude of the location.
	 * @return solar azimuth angle in degrees.
	 */
	public double getAzimuthAngle(double longitude, double latitude){
		double zenithAngle = getZenithAngle(longitude, latitude);
		double azimuthAngle = 0.0D;
		if(zenithAngle > 0.0D){
			double aSubsolarLat = getSolarDeclination();
			double aSubsolarLong = getSubsolarLongitude();
			double hourAngle = longitude - aSubsolarLong;
			if(hourAngle > 180D)
				hourAngle -= 360D;
			else
				if(hourAngle < -180D)
					hourAngle += 360D;
			double cosColatitudeX = Math.cos((90D - latitude) * RAD_PER_DEG);
			double sinColatitudeX = Math.sin((90D - latitude) * RAD_PER_DEG);
			double cosDTheta = Math.cos(hourAngle * RAD_PER_DEG);
			double sinDTheta = Math.sin(hourAngle * RAD_PER_DEG);
			double cosColatitudeS = Math.cos((90D - aSubsolarLat) * RAD_PER_DEG);
			double sinColatitudeS = Math.sin((90D - aSubsolarLat) * RAD_PER_DEG);
			if(sinColatitudeS > 0.0D){
				double cotColatitudeS = cosColatitudeS / sinColatitudeS;
				azimuthAngle = Math.atan2(sinDTheta, sinColatitudeX * cotColatitudeS - cosColatitudeX * cosDTheta) * DEG_PER_RAD;
			}
		}
		return azimuthAngle;
	}

	public String getPathfinderLTSTString(){
		double ltst = getTime(33.520000000000003D, 2);
		long ltsthr = (long)ltst;
		long ltstmi = (long)((ltst - (double)ltsthr) * 60D);
		long ltstse = (long)(((ltst - (double)ltsthr) * 60D - (double)ltstmi) * 60D);
		double solx = 1.0D + (super.jdTT - 2450634.0791779999D) / 1.02749125D;
		long sol = (long)solx;
		if((double)ltsthr < 1.0D){
			long solz = 1L + (long)((super.jdTT - 0.5D - 2450634.0791779999D) / 1.02749125D);
			if(solz == sol)
				sol++;
		} else
			if(ltst > 23D){
				long solz = 1L + (long)(((super.jdTT + 0.5D) - 2450634.0791779999D) / 1.02749125D);
				if(solz == sol)
					sol--;
			}
		return "SOL " + sol + " " + Util.zeroPadInt(ltsthr, 2) + ":" + Util.zeroPadInt(ltstmi, 2) + ":" + Util.zeroPadInt(ltstse, 2) + " LTST";
	}

	public static final int MEAN_SOLAR = 1;
	public static final int TRUE_SOLAR = 2;
	public static final boolean USE_EQ_A12 = true;
	public static final double TIME_RATIO = 1.02749125D;
	public double meanAnomaly;
	public double alphaFMS;
	public double ellSRad;
	public double ellSDeg;
	public double deltaS;
	public double rM;
	public double lambdaM;
	public double equationOfTime;
	public double primeMeridian;

	private static final double pbsA[] = {
		0.0D, 0.0071000000000000004D, 0.0057000000000000002D, 0.0038999999999999998D, 0.0037000000000000002D, 0.0020999999999999999D, 0.002D, 0.0018D, 0.0D
	};
	private static final double pbsTau[] = {
		1.0D, 2.2353000000000001D, 2.7543000000000002D, 1.1176999999999999D, 15.7866D, 2.1354000000000002D, 2.4693999999999998D, 32.849299999999999D, 0.0D
	};
	private static final double pbsPhi[] = {
		0.0D, 49.408999999999999D, 168.173D, 191.83699999999999D, 21.736000000000001D, 15.704000000000001D, 95.528000000000006D, 49.094999999999999D, 0.0D
	};
	private static final double pbsTauIn[] = {
		0.985626D, 0.44093678700845523D, 0.35784990741749262D, 0.88183412364677471D, 0.06243434305043518D, 0.46156504636133744D, 0.39913582246699608D, 0.030004474981202036D
	};
}

