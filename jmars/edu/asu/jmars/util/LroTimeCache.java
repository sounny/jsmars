package edu.asu.jmars.util;

import java.text.ParseException;
import java.util.Date;

import edu.asu.jspice.JS;
import edu.asu.jspice.JSpice;
import edu.asu.jspice.SpiceException;

public class LroTimeCache implements TimeCache {
	
	public static final String[] supportedFormats = {
		TimeCacheFactory.FMT_NAME_ET,
		TimeCacheFactory.FMT_NAME_UTC
	};
	
	public LroTimeCache() throws TimeException {
		try {
			// Load appropriate kernels
			JS.furnshc(JS.getCachedFile(Config.get("time.ls"), true).getAbsolutePath());
		}
		catch(Exception ex){
			throw new TimeException("Error creating LRO Time Cache.", ex);
		}
	}
	
	public Date et2date(double et) throws TimeException {
		try {
			return UTC_DF_TZ.parse(et2utc(et)+" UTC");
		}
		catch(ParseException ex){
			throw new TimeException("Invalid input UTC time format.", ex);
		}
	}

	public long et2millis(double et) throws TimeException {
		return et2date(et).getTime();
	}

	public String et2orbit(double et) throws TimeException {
		throw new TimeException("Unsupported operation.");
	}

	public int[] et2orbitn(double et) throws TimeException {
		throw new TimeException("Unsupported operation.");
	}

	public double et2sclkf(double et) throws TimeException {
		throw new TimeException("Unsupported operation.");
	}

	public String et2utc(double et) throws TimeException {
		int lenOut = 50;
		StringBuffer utcStr = new StringBuffer(lenOut);
		int prec = 3;
		
		try {
			JSpice.et2utcc(et, new StringBuffer(NAIF_UTC_FMT), prec, lenOut, utcStr);
		}
		catch(SpiceException ex){
			throw new TimeException("Spice exception.", ex);
		}
		
		return utcStr.toString();
	}

	public double millis2et(long utcMillis) throws TimeException {
		double[] et = new double[]{ Double.NaN };
		String dateStr = UTC_DF_TZ.format(new Date(utcMillis));
		dateStr = dateStr.replace(" UTC", "");
		JSpice.utc2etc(new StringBuffer(dateStr), et);
		return et[0];
	}

	public double orbit2et(int orbit, double offset) throws TimeException {
		throw new TimeException("Unsupported operation.");
	}

	public double sclk2et(long sclk, int frac) throws TimeException {
		throw new TimeException("Unsupported operation.");
	}

	public double sclkd2et(double sclkDec) throws TimeException {
		throw new TimeException("Unsupported operation.");
	}

	public double sclkf2et(double sclkFrac) throws TimeException {
		throw new TimeException("Unsupported operation.");
	}

	public double utc2et(String utc) throws TimeException {
		double[] etOut = new double[]{ Double.NaN };
		try {
			JSpice.utc2etc(new StringBuffer(utc), etOut);
		}
		catch(SpiceException ex){
			throw new TimeException("Spice exception.", ex);
		}
		
		return etOut[0];
	}

	public String[] getSupportedFormats() {
		return supportedFormats.clone();
	}
}
