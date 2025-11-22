package edu.asu.jmars.viz3d.renderer;

/**
 * Enumeration of JOGL stencil operations
 *
 * thread-safe
 */
public enum StencilOp {
	Zero,
	Invert,
	Keep,
	Replace,
	Incr,
	Decr,
	IncrWrap,
	DecrWrap;

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
	public static StencilOp forValue(int value) {
		return values()[value];
	}
}
