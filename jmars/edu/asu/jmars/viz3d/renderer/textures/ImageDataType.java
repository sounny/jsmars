package edu.asu.jmars.viz3d.renderer.textures;

/**
 * Enumeration of JOGL image data types
 *
 * thread-safe
 */

public enum ImageDataType {
	 Byte,
	 UnsignedByte,
	 Short,
	 UnsignedShort,
	 Int,
	 UnsignedInt,
	 Float,
	 HalfFloat,
	 UnsignedByte332,
	 UnsignedShort4444,
	 UnsignedShort5551,
	 UnsignedInt8888,
	 UnsignedInt1010102,
	 UnsignedByte233Reversed,
	 UnsignedShort565,
	 UnsignedShort565Reversed,
	 UnsignedShort4444Reversed,
	 UnsignedShort1555Reversed,
	 UnsignedInt8888Reversed,
	 UnsignedInt2101010Reversed,
	 UnsignedInt248,
	 UnsignedInt10F11F11FReversed,
	 UnsignedInt5999Reversed,
	 Float32UnsignedInt248Reversed;

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
	public static ImageDataType forValue(int value) {
		return values()[value];
	}
}