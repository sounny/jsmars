package edu.asu.jmars.viz3d.renderer;

/**
 * Enumeration of JOGL Uniform types
 */

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;

public enum ActiveUniformType {
	Int (GL4.GL_INT),
	Float (GL.GL_FLOAT),
	FloatVec2 (GL4.GL_FLOAT_VEC2),
	FloatVec3 (GL4.GL_FLOAT_VEC3),
	FloatVec4 (GL4.GL_FLOAT_VEC4),
	IntVec2 (GL4.GL_INT_VEC2),
	IntVec3 (GL4.GL_INT_VEC3),
	IntVec4 (GL4.GL_INT_VEC4),
	Bool (GL4.GL_BOOL),
	BoolVec2 (GL4.GL_BOOL_VEC2),
	BoolVec3 (GL4.GL_BOOL_VEC3),
	BoolVec4 (GL4.GL_BOOL_VEC4),
	FloatMat2 (GL4.GL_FLOAT_MAT2),
	FloatMat3 (GL4.GL_FLOAT_MAT3),
	FloatMat4 (GL4.GL_FLOAT_MAT4),
	Sampler1D (GL4.GL_SAMPLER_1D),
	Sampler2D (GL4.GL_SAMPLER_2D),
	Sampler2DRect (GL4.GL_SAMPLER_2D_RECT),
	Sampler2DRectShadow (GL4.GL_SAMPLER_2D_RECT_SHADOW),
	Sampler3D (GL4.GL_SAMPLER_2D_RECT_SHADOW),
	SamplerCube (GL4.GL_SAMPLER_CUBE),
	Sampler1DShadow (GL4.GL_SAMPLER_1D_SHADOW),
	Sampler2DShadow (GL4.GL_SAMPLER_2D_SHADOW),
	FloatMat2x3 (GL4.GL_FLOAT_MAT2x3),
	FloatMat2x4 (GL4.GL_FLOAT_MAT2x4),
	FloatMat3x2 (GL4.GL_FLOAT_MAT3x2),
	FloatMat3x4 (GL4.GL_FLOAT_MAT3x4),
	FloatMat4x2 (GL4.GL_FLOAT_MAT4x2),
	FloatMat4x3 (GL4.GL_FLOAT_MAT4x3),
	Sampler1DArray (GL4.GL_SAMPLER_1D_ARRAY),
	Sampler2DArray (GL4.GL_SAMPLER_2D_ARRAY),
	Sampler1DArrayShadow (GL4.GL_SAMPLER_1D_ARRAY_SHADOW),
	Sampler2DArrayShadow (GL4.GL_SAMPLER_2D_ARRAY_SHADOW),
	SamplerCubeShadow (GL4.GL_SAMPLER_CUBE_SHADOW),
	IntSampler1D (GL4.GL_INT_SAMPLER_1D),
	IntSampler2D (GL4.GL_INT_SAMPLER_2D),
	IntSampler2DRect (GL4.GL_INT_SAMPLER_2D_RECT),
	IntSampler3D (GL4.GL_INT_SAMPLER_3D),
	IntSamplerCube (GL4.GL_INT_SAMPLER_CUBE),
	IntSampler1DArray (GL4.GL_INT_SAMPLER_1D_ARRAY),
	IntSampler2DArray (GL4.GL_INT_SAMPLER_2D_ARRAY),
	UnsignedIntSampler1D (GL4.GL_UNSIGNED_INT_SAMPLER_1D),
	UnsignedIntSampler2D (GL4.GL_UNSIGNED_INT_SAMPLER_2D),
	UnsignedIntSampler2DRect (GL4.GL_UNSIGNED_INT_SAMPLER_2D_RECT),
	UnsignedIntSampler3D (GL4.GL_UNSIGNED_INT_SAMPLER_3D),
	UnsignedIntSamplerCube (GL4.GL_UNSIGNED_INT_SAMPLER_CUBE),
	UnsignedIntSampler1DArray (GL4.GL_UNSIGNED_INT_SAMPLER_1D_ARRAY),
	UnsignedIntSampler2DArray (GL4.GL_UNSIGNED_INT_SAMPLER_2D_ARRAY);

	private int value;
	
	private ActiveUniformType(int val) {
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
	public static ActiveUniformType forValue(int value) {
		return values()[value];
	}
}

