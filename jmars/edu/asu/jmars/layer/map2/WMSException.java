package edu.asu.jmars.layer.map2;

/**
 * WMS Exception returned by the WMS Server.
 */
public class WMSException extends Exception {
	public WMSException() {
	}

	public WMSException(String message) {
		super(message);
	}

	public WMSException(Throwable cause) {
		super(cause);
	}

	public WMSException(String message, Throwable cause) {
		super(message, cause);
	}

}
