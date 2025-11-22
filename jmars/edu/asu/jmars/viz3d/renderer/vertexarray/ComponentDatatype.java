package edu.asu.jmars.viz3d.renderer.vertexarray;

import com.jogamp.opengl.GL4;

/**
 * Enumeration of JOGL component data types
 *
 * thread-safe
 */

public enum ComponentDatatype {
	 Byte,
	 UnsignedByte,
	 Short,
	 UnsignedShort,
	 Int,
	 UnsignedInt,
	 Float,
	 HalfFloat,
	 Double;

	/**
	 * Returns the ordinal value of the Enumerated type
	 *
	 * @return the value of the type
	 *
	 * thread-safe
	 */
	public int getValue() {
		int retVal = 0;
		switch (this.ordinal()) {
		case 0: // ArrayBuffer
			retVal = GL4.GL_BYTE;
			break;
		case 1: // UnsignedByte
			retVal = GL4.GL_UNSIGNED_BYTE;
			break;
		case 2: // Short
			retVal = GL4.GL_SHORT;
			break;
		case 3: // UnsignedShort
			retVal = GL4.GL_UNSIGNED_SHORT;
			break;
		case 4: // Int
			retVal = GL4.GL_INT;
			break;
		case 5: // UnsignedInt
			retVal = GL4.GL_UNSIGNED_INT;
			break;
		case 6: // Float
			retVal = GL4.GL_FLOAT;
			break;
		case 7: // HalfFloat
			retVal = GL4.GL_HALF_FLOAT;
			break;
		case 8: // Double
			retVal = GL4.GL_DOUBLE;
		}

		return retVal;
	}

	/**
	 * Returns the Enumerated type that maps to the input ordinal value
	 *
	 * @param value
	 * @return Enumerated Type
	 *
	 * thread-safe
	 */
	public static ComponentDatatype forValue(int value) {
		return values()[value];
	}
}