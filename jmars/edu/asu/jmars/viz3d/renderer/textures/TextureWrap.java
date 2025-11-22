package edu.asu.jmars.viz3d.renderer.textures;

/**
 * Enumeration of JOGL texture wrap types
 *
 * thread-safe
 */

public enum TextureWrap {
	Clamp, 
	Repeat, 
	MirroredRepeat;

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
	public static TextureWrap forValue(int value) {
		return values()[value];
	}
}