package edu.asu.jmars.util;

/**
 ** Thrown when a problem is encountered while retrieving or
 ** converting time values.
 * 
 * This should really extend Exception not Throwable. - SLD
 **/
public class TimeException extends Throwable
 {
	public TimeException(String msg)
	 {
		super(msg);
	 }

	public TimeException(String msg, Throwable e)
	 {
		super(msg, e);
	 }
 }
