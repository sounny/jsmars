package edu.asu.jmars.viz3d.renderer.textures;

/**
 * Enumeration of JOGL texture parameter name types
 *
 * thread-safe
 */

public enum TextureParameterName {
	Minfilter, Magfilter, TextureWrapS, TextureWrapT;

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
	public static TextureParameterName forValue(int value) {
		return values()[value];
	}
}