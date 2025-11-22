package edu.asu.jmars.viz3d.core.geometry;

/**
 * Enumeration of winding order
 *
 * thread-safe
 */
public enum WindingOrder {
    Clockwise,
    Counterclockwise;

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
    public static WindingOrder forValue(int value) {
        return values()[value];
    }
}