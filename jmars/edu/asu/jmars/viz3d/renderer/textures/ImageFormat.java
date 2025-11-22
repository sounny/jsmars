package edu.asu.jmars.viz3d.renderer.textures;


/**
 * Enumeration of JOGL image format types
 *
 * thread-safe
 */

public enum ImageFormat {
	 StencilIndex,
	 DepthComponent,
	 Red,
	 Green,
	 Blue,
	 RedGreenBlue,
	 RedGreenBlueAlpha,
	 BlueGreenRed,
	 BlueGreenRedAlpha,
	 RedGreen,
	 RedGreenInteger,
	 DepthStencil,
	 RedInteger,
	 GreenInteger,
	 BlueInteger,
	 RedGreenBlueInteger,
	 RedGreenBlueAlphaInteger,
	 BlueGreenRedInteger,
	 BlueGreenRedAlphaInteger;

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
	 * Returns the ordinal value of the Enumerated type
	 *
	 * @return the value of the type
	 *
	 * thread-safe
	 */
	public static ImageFormat forValue(int value) {
		return values()[value];
	}
}