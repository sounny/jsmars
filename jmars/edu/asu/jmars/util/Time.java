package edu.asu.jmars.util;

/**
 ** Utility class for time conversions. Based on an arbitrary instance
 ** in time, linearly extrapolated.
 **
 ** <p>What's referred to as "unix" time is the number of seconds
 ** since midnight, Jan 1, 1970 UTC.
 **/
public class Time
 {
	private static final String epochUtc = "2002-091 // 16:19:50.000";
	private static final long epochEt = 70950054;
	private static final long epochUnix = 1017703190;

	/**
	 ** Returns the current time in ET.
	 **/
	public static long getNowEt()
	 {
		return  unixToEt(System.currentTimeMillis() / 1000);
	 }

	/**
	 ** Given a unix time, returns its ET.
	 **/
	public static long unixToEt(long unix)
	 {
		return  unix - epochUnix + epochEt;
	 }
 }
