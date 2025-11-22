package edu.asu.jmars.viz3d.renderer.state;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;


/**
 * Enumeration of JOGL color blending equation types
 *
 * thread-safe
 */

public enum BlendEquation {

    Add (GL.GL_FUNC_ADD),
    Minimum (GL4.GL_MIN),
    Maximum (GL4.GL_MAX),
    Subtract (GL.GL_FUNC_SUBTRACT),
    ReverseSubtract (GL.GL_FUNC_REVERSE_SUBTRACT);

	private int value;
	
	private BlendEquation(int val) {
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
    public static BlendEquation forValue(int value) {
        return values()[value];
    }
}