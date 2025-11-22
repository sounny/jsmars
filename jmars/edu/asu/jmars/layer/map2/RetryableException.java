package edu.asu.jmars.layer.map2;

/**
 * This exception is generated when it is possible to retry the 
 * operation causing this exception at a later stage to get a
 * valid response back. For example, download failure due to 
 * network outage or timeout.
 */
public class RetryableException extends Exception {
	public RetryableException(String message, Throwable cause) {
		super(message, cause);
	}

	public RetryableException(Throwable cause) {
		super(cause);
	}
	
	public RetryableException(String message) {
		super(message);
	}
}
