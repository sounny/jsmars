package edu.asu.jmars.viz3d.renderer.state;

/**
 * Enumeration of JOGL object culling parameters
 * 
 * thread-safe
 */

public enum CullFace {
	Front, Back, FrontAndBack;

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
	public static CullFace forValue(int value) {
		return values()[value];
	}
}