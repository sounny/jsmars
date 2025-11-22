package edu.asu.jmars.viz3d.renderer.buffers;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;

/** 
 * Enumeration of JOGL index buffer types
 *
 * thread-safe
 */
//
//(C) Copyright 2009 Patrick Cozzi and Deron Ohlarik
//
//Distributed under the MIT License.
//See License.txt or http://www.opensource.org/licenses/mit-license.php.
//

public enum IndexBufferDatatype {
	//
	// OpenGL supports byte indices. We do not use them
	// because they are unlikely to have a speed or memory benefit:
	//
	// http://www.opengl.org/discussion_boards/ubbthreads.php?ubb=showflat&Number=285547
	//

	Short (GL.GL_SHORT), Int (GL4.GL_INT);
	
	private int value;

	private IndexBufferDatatype(int val) {
		value = val;
	}
	
	/**
	 * Returns the ordinal value of the Enumerated type
	 *
	 * @return the value of the type
	 *
	 * thread-safe
	 */
	public int getValue() {
		return this.value;
	}

	/**
	 * Returns the Enumerated type that maps to the input ordinal value
	 *
	 * @param value
	 * @return Enumerated Type
	 *
	 * thread-safe
	 */
	public static IndexBufferDatatype forValue(int value) {
		return values()[value];
	}
}