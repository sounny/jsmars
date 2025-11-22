package edu.asu.jmars.viz3d.renderer.state;

/**
 * Enumeration of JOGL source blending factors
 * 
 * thread-safe
 */

public enum SourceBlendingFactor {
	Zero, One, SourceAlpha, OneMinusSourceAlpha, DestinationAlpha, OneMinusDestinationAlpha, 
	DestinationColor, OneMinusDestinationColor, SourceAlphaSaturate, ConstantColor, 
	OneMinusConstantColor, ConstantAlpha, OneMinusConstantAlpha;

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
	public static SourceBlendingFactor forValue(int value) {
		return values()[value];
	}
}