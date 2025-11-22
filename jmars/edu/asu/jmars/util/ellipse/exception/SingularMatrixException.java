/**
 * 
 */
package edu.asu.jmars.util.ellipse.exception;

/**
 * <description>
 *
 * <intended usage>
 *
 * <external dependencies>
 *
 * <multi-thread warning>
 */
public class SingularMatrixException extends RuntimeException {

	/**
	 * 
	 */
	public SingularMatrixException() {
	}

	/**
	 * @param message
	 */
	public SingularMatrixException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public SingularMatrixException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SingularMatrixException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public SingularMatrixException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
