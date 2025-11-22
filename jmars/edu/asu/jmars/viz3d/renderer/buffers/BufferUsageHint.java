package edu.asu.jmars.viz3d.renderer.buffers;

import com.jogamp.opengl.*;

/**
 * Ported enumeration of JOGL buffer usage hints
 *
 * thread-safe
 */
//
// (C) Copyright 2010 Patrick Cozzi and Kevin Ring
//
// Distributed under the MIT License.
// See License.txt or http://www.opensource.org/licenses/mit-license.php.
//

public enum BufferUsageHint {
    StreamDraw,
    StreamRead,
    StreamCopy,
    StaticDraw,
    StaticRead,
    StaticCopy,
    DynamicDraw,
    DynamicRead,
    DynamicCopy;

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
        case 0: // StreamDraw
        	retVal = GL3.GL_STREAM_DRAW;
        	break;
        case 1: // StreamRead
        	retVal = GL3.GL_STREAM_READ;
        	break;
        case 2: // StreamCopy
        	retVal = GL3.GL_STREAM_COPY;
        	break;
        case 3: // StaticDraw
        	retVal = GL3.GL_STATIC_DRAW;
        	break;
        case 4: // StaticRead
        	retVal = GL3.GL_STATIC_READ;
        	break;
        case 5: // StaticCopy
        	retVal = GL3.GL_STATIC_COPY;
        	break;
        case 6: // DynamicDraw
        	retVal = GL3.GL_DYNAMIC_DRAW;
        	break;
        case 7: // DynamicRead
        	retVal = GL3.GL_DYNAMIC_READ;
        	break;
        case 8: // DynamicCopy
        	retVal = GL3.GL_DYNAMIC_COPY;
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
    public static BufferUsageHint forValue(int value) {
        return values()[value];
    }
    
    public static void main (String [] args) {
    	System.out.println("StreamDraw "+ BufferUsageHint.StreamDraw.getValue());
    	System.out.println("StreamRead "+ BufferUsageHint.StreamRead.getValue());
    	System.out.println("StreamCopy "+ BufferUsageHint.StreamCopy.getValue());
    	System.out.println("StaticDraw "+ BufferUsageHint.StaticDraw.getValue());
    	System.out.println("StaticRead "+ BufferUsageHint.StaticRead.getValue());
    	System.out.println("StaticCopy "+ BufferUsageHint.StaticCopy.getValue());
    	System.out.println("DynamicDraw "+ BufferUsageHint.DynamicDraw.getValue());
    	System.out.println("DynamicRead "+ BufferUsageHint.DynamicRead.getValue());
    	System.out.println("DynamicCopy "+ BufferUsageHint.DynamicCopy.getValue());
    }}
