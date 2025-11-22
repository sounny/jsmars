package edu.asu.jmars.util;

public class VicarException extends Exception {
	public VicarException(String message) {
		super(message);
	}

   public VicarException(String message, Throwable cause) {
	  super(message, cause);
   }
}
