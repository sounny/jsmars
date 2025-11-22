package edu.asu.jmars.viz3d.renderer.gl;

import com.jogamp.opengl.GL;

/**
 * Statically defined JOGL buffer clearing bit patterns
 *
 * thread-safe
 */
public class ClearBufferMask {
	/**
	 * Color Buffer Mask
	 */
	public final static int ColorBufferBit = GL.GL_COLOR_BUFFER_BIT;
	/**
	 * Depth Buffer Mask
	 */
	public final static int DepthBufferBit = GL.GL_DEPTH_BUFFER_BIT;
	/**
	 * Stencil Buffer Mask
	 */
	public final static int StencilBufferBit = GL.GL_STENCIL_BUFFER_BIT;
	/**
	 * Generic mask for clearing a buffer mask
	 */
	public int mask = 0;	
}
