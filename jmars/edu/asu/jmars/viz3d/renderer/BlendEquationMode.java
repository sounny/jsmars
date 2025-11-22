package edu.asu.jmars.viz3d.renderer;

/**
 * Enumeration of JOGL blend equation parameters
 *
 * thread-safe
 */
public enum BlendEquationMode {
	FuncAdd,
	Min,
	Max,
	FuncSubtract,
	FuncReverseSubtract;

	/**
	 * Returns the ordinal value of the Enumerated type
	 *
	 * @return the value of the type
	 *
	 * thread-safe
	 */
	public int getValue() {
		return this.ordinal();
	}

	/**
	 * Returns the Enumerated type that maps to the input ordinal value
	 *
	 * @param value
	 * @return Enumerated Type
	 *
	 * thread-safe
	 */
	public static BlendEquationMode forValue(int value) {
		return values()[value];
	}
}
