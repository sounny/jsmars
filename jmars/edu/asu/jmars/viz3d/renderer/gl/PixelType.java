package edu.asu.jmars.viz3d.renderer.gl;

import com.jogamp.opengl.GL4;

/**
 * Enumeration of JOGL supported pixel types
 *
 * thread-safe
 */

public enum PixelType {
	UnsignedByte,
	UnsignedShort,
	UnsignedInt1010102,
	HalfFloat,
	Float,
	Byte,
	Short,
	Int,
	UnsignedInt,
	UnsignedInt248;

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
        case 0:
        	retVal = GL4.GL_UNSIGNED_BYTE;
        	break;
	    case 1:
	    	retVal = GL4.GL_UNSIGNED_SHORT;
	    	break;
	    case 3:
	    	retVal = GL4.GL_UNSIGNED_INT_10_10_10_2;
	    	break;
	    case 4:
	    	retVal = GL4.GL_HALF_FLOAT;
	    	break;
	    case 5:
	    	retVal = GL4.GL_FLOAT;
	    	break;
	    case 6:
	    	retVal = GL4.GL_BYTE;
	    	break;
	    case 7:
	    	retVal = GL4.GL_SHORT;
	    	break;
	    case 8:
	    	retVal = GL4.GL_INT;
	    	break;
	    case 9:
	    	retVal = GL4.GL_UNSIGNED_INT;
	    	break;
	    case 10:
	    	retVal = GL4.GL_UNSIGNED_INT_24_8;
	    	break;
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
    public static PixelType forValue(int value) {
        return values()[value];
    }
}
