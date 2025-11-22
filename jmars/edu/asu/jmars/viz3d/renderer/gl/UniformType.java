package edu.asu.jmars.viz3d.renderer.gl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;

/**
 * Enumeration of JOGL Uniform types (parameters to be passed to shaders)
 *
 * thread-safe
 */
public enum UniformType {
 Int (GL4.GL_INT),
 Float (GL.GL_FLOAT),
 FloatVector2 (GL4.GL_FLOAT_VEC2),
 FloatVector3 (GL4.GL_FLOAT_VEC3),
 FloatVector4 (GL4.GL_FLOAT_VEC4),
 IntVector2 (GL4.GL_INT_VEC2),
 IntVector3 (GL4.GL_INT_VEC3),
 IntVector4 (GL4.GL_INT_VEC4),
 Bool (GL4.GL_BOOL),
 BoolVector2 (GL4.GL_BOOL_VEC2),
 BoolVector3 (GL4.GL_BOOL_VEC3),
 BoolVector4 (GL4.GL_BOOL_VEC4),
 FloatMatrix22 (GL4.GL_FLOAT_MAT2),
 FloatMatrix33 (GL4.GL_FLOAT_MAT3),
 FloatMatrix44 (GL4.GL_FLOAT_MAT4),
 Sampler1D (GL4.GL_SAMPLER_1D),
 Sampler2D (GL4.GL_SAMPLER_2D),
 Sampler2DRectangle (GL4.GL_SAMPLER_2D_RECT),
 Sampler2DRectangleShadow (GL4.GL_SAMPLER_2D_RECT_SHADOW),
 Sampler3D (GL4.GL_SAMPLER_3D),
 SamplerCube (GL4.GL_SAMPLER_CUBE),
 Sampler1DShadow (GL4.GL_SAMPLER_1D_SHADOW),
 Sampler2DShadow (GL4.GL_SAMPLER_2D_SHADOW),
 FloatMatrix23 (GL4.GL_FLOAT_MAT2x3),
 FloatMatrix24 (GL4.GL_FLOAT_MAT2x4),
 FloatMatrix32 (GL4.GL_FLOAT_MAT3x2),
 FloatMatrix34 (GL4.GL_FLOAT_MAT3x4),
 FloatMatrix42 (GL4.GL_FLOAT_MAT4x2),
 FloatMatrix43 (GL4.GL_FLOAT_MAT4x3),
 Sampler1DArray (GL4.GL_SAMPLER_1D_ARRAY),
 Sampler2DArray (GL4.GL_SAMPLER_2D_ARRAY),
 Sampler1DArrayShadow (GL4.GL_SAMPLER_1D_ARRAY_SHADOW),
 Sampler2DArrayShadow (GL4.GL_SAMPLER_2D_ARRAY_SHADOW),
 SamplerCubeShadow (GL4.GL_SAMPLER_CUBE_SHADOW),
 IntSampler1D (GL4.GL_INT_SAMPLER_1D),
 IntSampler2D (GL4.GL_INT_SAMPLER_2D),
 IntSampler2DRectangle (GL4.GL_INT_SAMPLER_2D_RECT),
 IntSampler3D (GL4.GL_INT_SAMPLER_3D),
 IntSamplerCube (GL4.GL_INT_SAMPLER_CUBE),
 IntSampler1DArray (GL4.GL_INT_SAMPLER_1D_ARRAY),
 IntSampler2DArray (GL4.GL_INT_SAMPLER_2D_ARRAY),
 UnsignedIntSampler1D (GL4.GL_UNSIGNED_INT_SAMPLER_1D),
 UnsignedIntSampler2D (GL4.GL_UNSIGNED_INT_SAMPLER_2D),
 UnsignedIntSampler2DRectangle (GL4.GL_UNSIGNED_INT_SAMPLER_2D_RECT),
 UnsignedIntSampler3D (GL4.GL_UNSIGNED_INT_SAMPLER_3D),
 UnsignedIntSamplerCube (GL4.GL_UNSIGNED_INT_SAMPLER_CUBE),
 UnsignedIntSampler1DArray (GL4.GL_UNSIGNED_INT_SAMPLER_1D_ARRAY),
 UnsignedIntSampler2DArray (GL4.GL_UNSIGNED_INT_SAMPLER_2D_ARRAY);

	private int value;
	
	private UniformType(int val) {
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
	public static UniformType forValue(int value) {
		return values()[value];
	}
}