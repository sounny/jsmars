package edu.asu.jmars.layer.map2;

/**
 * This exception is generated when it is NOT possible to retry the 
 * operation causing this exception at a later stage and get a good
 * response back.
 */
public class NonRetryableException extends Exception {
	public NonRetryableException(String message, Throwable cause) {
		super(message, cause);
	}

	public NonRetryableException(Throwable cause) {
		super(cause);
	}
	
	public NonRetryableException(String message){
		super(message);
	}
}
