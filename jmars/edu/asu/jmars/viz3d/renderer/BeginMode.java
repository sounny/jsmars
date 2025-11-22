package edu.asu.jmars.viz3d.renderer;

/**
 * Enumeration of JOGL 'begin rendering' types
 *
 * thread-safe
 */

public enum BeginMode {
	Points,
	Lines,
	LineLoop,
	LineStrip,
	Triangles,
	TriangleStrip,
	LinesAdjacency,
	LineStripAdjacency,
	TrianglesAdjacency,
	TriangleStripAdjacency,
	TriangleFan;

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
	public static BeginMode forValue(int value) {
		return values()[value];
	}
}

