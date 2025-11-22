package edu.asu.jmars.viz3d.renderer.textures;

/**
 * Enumeration of JOGL texture minification filter types
 *
 * thread-safe
 */

public enum TextureMinificationFilter {
	Nearest, 
	Linear, 
	NearestMipmapNearest, 
	LinearMipmapNearest, 
	NearestMipmapLinear, 
	LinearMipmapLinear;

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
	public static TextureMinificationFilter forValue(int value) {
		return values()[value];
	}
}