package edu.asu.jmars.viz3d.renderer.gl;

import com.jogamp.opengl.GL3;

/**
 * Enumeration of JOGL supported pixel formats
 *
 * thread-safe
 */

public enum PixelInternalFormat {
	Rgb8,
	Rgb16,
	Rgba8,
	Rgb10A2,
	Rgba16,
	DepthComponent16,
	DepthComponent24,
	R8,
	R16,
	Rg8,
	Rg16,
	R16f,
	R32f,
	Rg16f,
	Rg32f,
	R8i,
	R8ui,
	R16i,
	R16ui,
	R32i,
	R32ui,
	Rg8i,
	Rg8ui,
	Rg16i,
	Rg16ui,
	Rg32i,
	Rg32ui,
	Rgba32f,
	Rgb32f,
	Rgba16f,
	Rgb16f,
	Depth24Stencil8,
	R11fG11fB10f,
	Rgb9E5,
	Srgb8,
	Srgb8Alpha8,
	DepthComponent32f,
	Depth32fStencil8,
	Rgba32ui,
	Rgb32ui,
	Rgba16ui,
	Rgb16ui,
	Rgba8ui,
	Rgb8ui,
	Rgba32i,
	Rgb32i,
	Rgba16i,
	Rgb16i,
	Rgba8i,
	Rgb8i;
	
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
        	retVal = GL3.GL_RGB8;
        	break;
	    case 1:
	    	retVal = GL3.GL_RGB16;
	    	break;
		case 2:
			retVal = GL3.GL_RGBA8;
			break;
		case 3:
			retVal = GL3.GL_RGB10_A2;
			break;
		case 4:
			retVal = GL3.GL_RGBA16;
			break;
		case 5:
			retVal = GL3.GL_DEPTH_COMPONENT16;
			break;
        case 6:
        	retVal = GL3.GL_DEPTH_COMPONENT24;
        	break;
	    case 7:
	    	retVal = GL3.GL_R8;
	    	break;
		case 8:
			retVal = GL3.GL_R16;
			break;
		case 9:
			retVal = GL3.GL_RG8;
			break;
		case 10:
			retVal = GL3.GL_RG16;
			break;
		case 11:
			retVal = GL3.GL_R16F;
			break;
        case 12:
        	retVal = GL3.GL_R32F;
        	break;
	    case 13:
	    	retVal = GL3.GL_RG16F;
	    	break;
		case 14:
			retVal = GL3.GL_RG32F;
			break;
		case 15:
			retVal = GL3.GL_R8I;
			break;
		case 16:
			retVal = GL3.GL_R8UI;
			break;
		case 17:
			retVal = GL3.GL_R16I;
			break;
        case 18:
        	retVal = GL3.GL_R16UI;
        	break;
	    case 19:
	    	retVal = GL3.GL_R32I;
	    	break;
		case 20:
			retVal = GL3.GL_R32UI;
			break;
		case 21:
			retVal = GL3.GL_RG8I;
			break;
		case 22:
			retVal = GL3.GL_RG8UI;
			break;
		case 23:
			retVal = GL3.GL_RG16I;
			break;
        case 24:
        	retVal = GL3.GL_RG16UI;
        	break;
	    case 25:
	    	retVal = GL3.GL_RG32I;
	    	break;
		case 26:
			retVal = GL3.GL_RG32UI;
			break;
		case 27:
			retVal = GL3.GL_RGBA32F;
			break;
		case 28:
			retVal = GL3.GL_RGB32F;
			break;
		case 29:
			retVal = GL3.GL_RGBA16F;
			break;
        case 30:
        	retVal = GL3.GL_RGB16F;
        	break;
	    case 31:
	    	retVal = GL3.GL_DEPTH24_STENCIL8;
	    	break;
		case 32:
			retVal = GL3.GL_R11F_G11F_B10F;
			break;
		case 33:
			retVal = GL3.GL_RGB9_E5;
			break;
		case 34:
			retVal = GL3.GL_SRGB8;
			break;
		case 35:
			retVal = GL3.GL_SRGB8_ALPHA8;
			break;
        case 36:
        	retVal = GL3.GL_DEPTH_COMPONENT32F;
        	break;
	    case 37:
	    	retVal = GL3.GL_DEPTH32F_STENCIL8;
	    	break;
		case 38:
			retVal = GL3.GL_RGBA32UI;
			break;
		case 39:
			retVal = GL3.GL_RGB32UI;
			break;
		case 40:
			retVal = GL3.GL_RGBA16UI;
			break;
		case 41:
			retVal = GL3.GL_RGB16UI;
			break;
		case 42:
			retVal = GL3.GL_RGBA8UI;
			break;
		case 43:
			retVal = GL3.GL_RGB8UI;
			break;
        case 44:
        	retVal = GL3.GL_RGBA32I;
        	break;
	    case 45:
	    	retVal = GL3.GL_RGB32I;
	    	break;
		case 46:
			retVal = GL3.GL_RGBA16I;
			break;
		case 47:
			retVal = GL3.GL_RGB16I;
			break;
		case 48:
			retVal = GL3.GL_RGBA8I;
			break;
		case 49:
			retVal = GL3.GL_RGB8I;
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
    public static PixelInternalFormat forValue(int value) {
        return values()[value];
    }


}
