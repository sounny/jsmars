package edu.asu.jmars.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * The following classes were partially liberated from a Nasa web 
 * site - its all still public dollars..
 * It is a narrowly held belief that this code may have adopted
 * from the Mars24 tool.
 */

public class JulianTime {
	public static final double RAD_PER_DEG  = 0.017453292519943295D;
	public static final double DEG_PER_RAD  = 57.295779513082323D;

	public JulianTime(long timeInMillis){
		unixEpoch = 0L;
		jdUTC = 0.0D;
		jdTT = 0.0D;
		delta2000 = 0.0D;
		utc2tt = 0.0D;
		setJulianTime(timeInMillis);
	}

	public void setJulianTime(long timeInMillis){
		unixEpoch = timeInMillis;
		jdUTC = julianDay(timeInMillis);
		delta2000 = jdUTC - 2451545D;
		utc2tt = 64.183999999999997D + (95D + 35D * (delta2000 / 36525D)) * (delta2000 / 36525D);
		jdTT = julianDay(timeInMillis + (long)(utc2tt * 1000D));
		delta2000 = jdTT - 2451545D;
	}

	public double julianDay(long timeInMillis){
		Date theDate = new Date(timeInMillis);

		GregorianCalendar cal = new GregorianCalendar();

		//convert to minutes
		cal.setTime(theDate);

		long tzoffset = Math.abs((cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET) ) / 60000);

		long theYear = cal.get(Calendar.YEAR);
		long theMonth = cal.get(Calendar.MONTH) + 1;
		long theDay = cal.get(Calendar.DAY_OF_MONTH);
		long theHour = cal.get(Calendar.HOUR_OF_DAY);
		long theMinute = cal.get(Calendar.MINUTE) + tzoffset;
		long theSecond = cal.get(Calendar.SECOND);


		long jy = theYear;
		long jm;
		if(theMonth > 2L){
			jm = theMonth + 1L;
		}
		else {
			jy--;
			jm = theMonth + 13L;
		}
		long julianDay = (long)(365.25D * (double)jy) + (long)(30.600100000000001D * (double)jm) + theDay + 0x1a42a3L;
		long ja = (long)(0.01D * (double)jy);
		julianDay += (2L - ja) + (long)(0.25D * (double)ja);
		double fraction = (((double)theHour - 12D) * 3600D + (double)theMinute * 60D + (double)theSecond) / 86400D;
		return (double)julianDay + fraction;
	}

	public static final boolean USE_PAPER_B = true;
	public static final double JD_2000 = 2451545D;
	public long unixEpoch;
	public double jdUTC;
	public double jdTT;
	public double delta2000;
	public double utc2tt;
}
