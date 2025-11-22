package edu.asu.jmars.viz3d.renderer.state;

/**
 * Enumeration of JOGL stencil operation types
 * 
 * thread-safe
 */

public enum StencilOperation {
	Zero, Invert, Keep, Replace, Increment, Decrement, IncrementWrap, DecrementWrap;

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
	public static StencilOperation forValue(int value) {
		return values()[value];
	}
}