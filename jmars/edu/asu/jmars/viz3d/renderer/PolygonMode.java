package edu.asu.jmars.viz3d.renderer;

/**
 * Enumeration of JOGL polygon rendering types
 *
 * thread-safe
 */
public enum PolygonMode {
	Point,
	Line,
	Fill;

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
	public static PolygonMode forValue(int value) {
		return values()[value];
	}
}
