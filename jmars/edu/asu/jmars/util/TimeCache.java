package edu.asu.jmars.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public interface TimeCache {

	/** Date format used in UTC times produced by TimeCache. */
	public static final SimpleDateFormat UTC_DF = new SimpleDateFormat("yyyy-DDD'T'HH:mm:ss.SSS");
	
	/** Date format corresponding to UTC_DF for NAIF */
	public static final String NAIF_UTC_FMT = "ISOD";

	/** Same date format as UTC_DF but with time-zone. */
	public static final SimpleDateFormat UTC_DF_TZ = new SimpleDateFormat("yyyy-DDD'T'HH:mm:ss.SSS z");
	
	/**
	 * Returns the list of time conversion formats supported by this
	 * time-cache.
	 * @return Returns the array of time conversion formats supported by
	 * this time-cache.
	 */
	public abstract String[] getSupportedFormats();

	/**
	 ** Converts a fraction-encoded floating-point sclk value into
	 ** et. For example, 1234.128 as a fraction-encoded sclk is
	 ** actually 1234:128.
	 **/
	public abstract double sclkf2et(double sclkFrac) throws TimeException;

	/**
	 ** Converts a decimal-encoded floating-point sclk value into
	 ** et. For example, 1234.5 as a decimal-encoded sclk is actually
	 ** 1234:128.
	 **/
	public abstract double sclkd2et(double sclkDec) throws TimeException;

	/**
	 ** Converts a sclk:fraction into et.
	 **/
	public abstract double sclk2et(long sclk, int frac) throws TimeException;

	/**
	 ** Converts an orbit+offset into et.
	 **/
	public abstract double orbit2et(int orbit, double offset)
			throws TimeException;

	/**
	 ** Converts a utc string into et. See {@link #utc2millis} for
	 ** accepted utc formats.
	 **/
	public abstract double utc2et(String utc) throws TimeException;

	/**
	 ** Converts a millisecond offset from 1970 into et.
	 **/
	public abstract double millis2et(long utcMillis) throws TimeException;

	/**
	 ** Converts an et into a fraction-encoded floating-point sclk.
	 **/
	public abstract double et2sclkf(double et) throws TimeException;

	/**
	 ** Converts an et into an orbit+offset, as a pair of ints in
	 ** that order.
	 **/
	public abstract int[] et2orbitn(double et) throws TimeException;

	/**
	 ** Converts an et into an orbit+offset.
	 **/
	public abstract String et2orbit(double et) throws TimeException;

	/**
	 ** Converts an et into a UTC string.
	 **/
	public abstract String et2utc(double et) throws TimeException;

	/**
	 ** Converts an et into milliseconds since 1970.
	 **/
	public abstract long et2millis(double et) throws TimeException;

	/**
	 ** Converts an et into a {@link Date} object.
	 **/
	public abstract Date et2date(double et) throws TimeException;

}