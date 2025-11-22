package edu.asu.jmars.viz3d.renderer;

/**
 * Enumeration of JOGL floating point and integral types
 *
 * thread-safe
 */


public enum ActiveAttribType {
	Float, FloatVec2, FloatVec3, FloatVec4, FloatMat2, FloatMat3, FloatMat4, Int, IntVec2, IntVec3, IntVec4;

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
	public static ActiveAttribType forValue(int value) {
		return values()[value];
	}
}
