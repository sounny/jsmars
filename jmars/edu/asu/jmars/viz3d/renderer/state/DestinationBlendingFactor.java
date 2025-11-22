package edu.asu.jmars.viz3d.renderer.state;

/**
 * Enumeration of JOGL destination color blending factors
 *
 * thread-safe
 */

public enum DestinationBlendingFactor {
    Zero,
    One,
    SourceColor,
    OneMinusSourceColor,
    SourceAlpha,
    OneMinusSourceAlpha,
    DestinationAlpha,
    OneMinusDestinationAlpha,
    DestinationColor,
    OneMinusDestinationColor,
    ConstantColor,
    OneMinusConstantColor,
    ConstantAlpha,
    OneMinusConstantAlpha;

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
    public static DestinationBlendingFactor forValue(int value) {
        return values()[value];
    }
}