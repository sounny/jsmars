package edu.asu.jmars.viz3d.renderer.gl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL4;

import edu.asu.jmars.viz3d.core.geometry.PrimitiveType;
import edu.asu.jmars.viz3d.core.geometry.WindingOrder;
import edu.asu.jmars.viz3d.renderer.ActiveAttribType;
import edu.asu.jmars.viz3d.renderer.ActiveUniformType;
import edu.asu.jmars.viz3d.renderer.DrawElementsType;
import edu.asu.jmars.viz3d.renderer.buffers.IndexBufferDatatype;
import edu.asu.jmars.viz3d.renderer.state.BlendEquation;
import edu.asu.jmars.viz3d.renderer.state.CullFace;
import edu.asu.jmars.viz3d.renderer.state.DepthTestFunction;
import edu.asu.jmars.viz3d.renderer.state.DestinationBlendingFactor;
import edu.asu.jmars.viz3d.renderer.state.RasterizationMode;
import edu.asu.jmars.viz3d.renderer.state.SourceBlendingFactor;
import edu.asu.jmars.viz3d.renderer.state.StencilOperation;
import edu.asu.jmars.viz3d.renderer.state.StencilTestFunction;
import edu.asu.jmars.viz3d.renderer.textures.ImageDataType;
import edu.asu.jmars.viz3d.renderer.textures.ImageFormat;
import edu.asu.jmars.viz3d.renderer.textures.TextureFormat;
import edu.asu.jmars.viz3d.renderer.textures.TextureMagnificationFilter;
import edu.asu.jmars.viz3d.renderer.textures.TextureMinificationFilter;
import edu.asu.jmars.viz3d.renderer.textures.TextureParameterName;
import edu.asu.jmars.viz3d.renderer.textures.TextureWrap;
import edu.asu.jmars.viz3d.renderer.vertexarray.ComponentDatatype;

/**
 * Utility class to convert JOGL enumeration values to their corresponding JOGL literal values 
 *
 * thread-safe
 */
public final class TypeConverterGL {
	/**
	 * Converts ClearBuffers to the corresponding ClearBufferMask
	 *
	 * @param type
	 * @return ClearBufferMask
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static ClearBufferMask to(ClearBuffers mask) {
		ClearBufferMask clearMask = new ClearBufferMask();;

		if ((mask.getValue() & ClearBuffers.ColorBuffer.getValue()) != 0) {
			clearMask.mask |= ClearBufferMask.ColorBufferBit;
		}

		if ((mask.getValue() & ClearBuffers.DepthBuffer.getValue()) != 0) {
			clearMask.mask |= ClearBufferMask.DepthBufferBit;
		}

		if ((mask.getValue() & ClearBuffers.StencilBuffer.getValue()) != 0) {
			clearMask.mask |= ClearBufferMask.StencilBufferBit;
		}

		return clearMask;
	}

	/**
	 * Converts ActiveAttribType
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(ActiveAttribType type) {
		switch (type) {
		case Float:
			return GL.GL_FLOAT;
		case FloatVec2:
			return GL2ES2.GL_FLOAT_VEC2;
		case FloatVec3:
			return GL2ES2.GL_FLOAT_VEC3;
		case FloatVec4:
			return GL2ES2.GL_FLOAT_VEC4;
		case FloatMat2:
			return GL2ES2.GL_FLOAT_MAT2;
		case FloatMat3:
			return GL2ES2.GL_FLOAT_MAT3;
		case FloatMat4:
			return GL2ES2.GL_FLOAT_MAT4;
		case Int:
			return GL2ES2.GL_INT;
		case IntVec2:
			return GL2ES2.GL_INT_VEC2;
		case IntVec3:
			return GL2ES2.GL_INT_VEC3;
		case IntVec4:
			return GL2ES2.GL_INT_VEC4;
		}

		throw new IllegalArgumentException("type");
	}

	/**
	 * Converts ActiveAttribType to the corresponding UniformType
	 *
	 * @param type
	 * @return UniformType
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static UniformType to(ActiveUniformType type) {
		switch (type) {
		case Int:
			return UniformType.Int;
		case Float:
			return UniformType.Float;
		case FloatVec2:
			return UniformType.FloatVector2;
		case FloatVec3:
			return UniformType.FloatVector3;
		case FloatVec4:
			return UniformType.FloatVector4;
		case IntVec2:
			return UniformType.IntVector2;
		case IntVec3:
			return UniformType.IntVector3;
		case IntVec4:
			return UniformType.IntVector4;
		case Bool:
			return UniformType.Bool;
		case BoolVec2:
			return UniformType.BoolVector2;
		case BoolVec3:
			return UniformType.BoolVector3;
		case BoolVec4:
			return UniformType.BoolVector4;
		case FloatMat2:
			return UniformType.FloatMatrix22;
		case FloatMat3:
			return UniformType.FloatMatrix33;
		case FloatMat4:
			return UniformType.FloatMatrix44;
		case Sampler1D:
			return UniformType.Sampler1D;
		case Sampler2D:
			return UniformType.Sampler2D;
		case Sampler2DRect:
			return UniformType.Sampler2DRectangle;
		case Sampler2DRectShadow:
			return UniformType.Sampler2DRectangleShadow;
		case Sampler3D:
			return UniformType.Sampler3D;
		case SamplerCube:
			return UniformType.SamplerCube;
		case Sampler1DShadow:
			return UniformType.Sampler1DShadow;
		case Sampler2DShadow:
			return UniformType.Sampler2DShadow;
		case FloatMat2x3:
			return UniformType.FloatMatrix23;
		case FloatMat2x4:
			return UniformType.FloatMatrix24;
		case FloatMat3x2:
			return UniformType.FloatMatrix32;
		case FloatMat3x4:
			return UniformType.FloatMatrix34;
		case FloatMat4x2:
			return UniformType.FloatMatrix42;
		case FloatMat4x3:
			return UniformType.FloatMatrix43;
		case Sampler1DArray:
			return UniformType.Sampler1DArray;
		case Sampler2DArray:
			return UniformType.Sampler2DArray;
		case Sampler1DArrayShadow:
			return UniformType.Sampler1DArrayShadow;
		case Sampler2DArrayShadow:
			return UniformType.Sampler2DArrayShadow;
		case SamplerCubeShadow:
			return UniformType.SamplerCubeShadow;
		case IntSampler1D:
			return UniformType.IntSampler1D;
		case IntSampler2D:
			return UniformType.IntSampler2D;
		case IntSampler2DRect:
			return UniformType.IntSampler2DRectangle;
		case IntSampler3D:
			return UniformType.IntSampler3D;
		case IntSamplerCube:
			return UniformType.IntSamplerCube;
		case IntSampler1DArray:
			return UniformType.IntSampler1DArray;
		case IntSampler2DArray:
			return UniformType.IntSampler2DArray;
		case UnsignedIntSampler1D:
			return UniformType.UnsignedIntSampler1D;
		case UnsignedIntSampler2D:
			return UniformType.UnsignedIntSampler2D;
		case UnsignedIntSampler2DRect:
			return UniformType.UnsignedIntSampler2DRectangle;
		case UnsignedIntSampler3D:
			return UniformType.UnsignedIntSampler3D;
		case UnsignedIntSamplerCube:
			return UniformType.UnsignedIntSamplerCube;
		case UnsignedIntSampler1DArray:
			return UniformType.UnsignedIntSampler1DArray;
		case UnsignedIntSampler2DArray:
			return UniformType.UnsignedIntSampler2DArray;
		}

		throw new IllegalArgumentException("type");
	}

	/**
	 * Converts ComponentDataype
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(ComponentDatatype type) {
		switch (type) {
		case Byte:
			return GL.GL_BYTE;
		case UnsignedByte:
			return GL.GL_UNSIGNED_BYTE;
		case Short:
			return GL.GL_SHORT;
		case UnsignedShort:
			return GL.GL_UNSIGNED_SHORT;
		case Int:
			return GL2ES2.GL_INT;  
		case UnsignedInt:
			return GL.GL_UNSIGNED_INT;
		case Float:
			return GL.GL_FLOAT;
		case Double:
			return GL2.GL_DOUBLE;
		case HalfFloat:
			return GL.GL_HALF_FLOAT;
		}

		throw new IllegalArgumentException("type");
	}

	/**
	 * Converts PrimitiveType
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(PrimitiveType type) {
		switch (type) {
		case Points:
			return GL.GL_POINTS;
		case Lines:
			return GL.GL_LINES;
		case LineLoop:
			return GL.GL_LINE_LOOP;
		case LineStrip:
			return GL.GL_LINE_STRIP;
		case Triangles:
			return GL.GL_TRIANGLES;
		case TriangleStrip:
			return GL.GL_TRIANGLE_STRIP;
		case LinesAdjacency:
			return GL4.GL_LINES_ADJACENCY;
		case LineStripAdjacency:
			return GL4.GL_LINE_STRIP_ADJACENCY;
		case TrianglesAdjacency:
			return GL4.GL_TRIANGLES_ADJACENCY;
		case TriangleStripAdjacency:
			return GL4.GL_TRIANGLE_STRIP_ADJACENCY;
		case TriangleFan:
			return GL.GL_TRIANGLE_FAN;
		}

		throw new IllegalArgumentException("type");
	}

	/**
	 * Converts IndexBufferDatatype to the corresponding DrawElementsType
	 *
	 * @param type
	 * @return DrawElementsType
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static DrawElementsType to(IndexBufferDatatype type) {
		switch (type) {
		case Short:
			return DrawElementsType.Short;
		case Int:
			return DrawElementsType.Int;
		}

		throw new IllegalArgumentException("type");
	}

	/**
	 * Converts DepthTestFunction
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(DepthTestFunction function) {
		switch (function) {
		case Never:
			return GL.GL_NEVER;
		case Less:
			return GL.GL_LESS;
		case Equal:
			return GL.GL_EQUAL;
		case LessThanOrEqual:
			return GL.GL_LEQUAL;
		case Greater:
			return GL.GL_GREATER;
		case NotEqual:
			return GL.GL_NOTEQUAL;
		case GreaterThanOrEqual:
			return GL.GL_GEQUAL;
		case Always:
			return GL.GL_ALWAYS;
		}

		throw new IllegalArgumentException("function");
	}

	/**
	 * Converts CullFace
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(CullFace face) {
		switch (face) {
		case Front:
			return GL.GL_FRONT;
		case Back:
			return GL.GL_BACK;
		case FrontAndBack:
			return GL.GL_FRONT_AND_BACK;
		}

		throw new IllegalArgumentException("face");
	}

	/**
	 * Converts WindingOrder
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(WindingOrder windingOrder) {
		switch (windingOrder) {
		case Clockwise:
			return GL.GL_CW;
		case Counterclockwise:
			return GL.GL_CCW;
		}

		throw new IllegalArgumentException("windingOrder");
	}

	/**
	 * Converts RasterizationMode
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(RasterizationMode mode) {
		switch (mode) {
		case Point:
			return GL4.GL_POINT;
		case Line:
			return GL4.GL_LINE;
		case Fill:
			return GL4.GL_FILL;
		}

		throw new IllegalArgumentException("rasterization mode");
	}

	/**
	 * Converts StencilOperation
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(StencilOperation operation) {
		switch (operation) {
		case Zero:
			return GL.GL_ZERO;
		case Invert:
			return GL.GL_INVERT;
		case Keep:
			return GL.GL_KEEP;
		case Replace:
			return GL.GL_REPLACE;
		case Increment:
			return GL.GL_INCR;
		case Decrement:
			return GL.GL_DECR;
		case IncrementWrap:
			return GL.GL_INCR_WRAP;
		case DecrementWrap:
			return GL.GL_DECR_WRAP;
		}

		throw new IllegalArgumentException("operation");
	}

	/**
	 * Converts StencilTestFunction
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(StencilTestFunction function) {
		switch (function) {
		case Never:
			return GL.GL_NEVER;
		case Less:
			return GL.GL_LESS;
		case Equal:
			return GL.GL_EQUAL;
		case LessThanOrEqual:
			return GL.GL_LEQUAL;
		case Greater:
			return GL.GL_GREATER;
		case NotEqual:
			return GL.GL_NOTEQUAL;
		case GreaterThanOrEqual:
			return GL.GL_GEQUAL;
		case Always:
			return GL.GL_ALWAYS;
		}

		throw new IllegalArgumentException("function");
	}

	/**
	 * Converts BlendEquation
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(BlendEquation equation) {
		switch (equation) {
		case Add:
			return GL.GL_FUNC_ADD;
		case Minimum:
			return GL4.GL_MIN;
		case Maximum:
			return GL4.GL_MAX;
		case Subtract:
			return GL4.GL_FUNC_SUBTRACT;
		case ReverseSubtract:
			return GL4.GL_FUNC_REVERSE_SUBTRACT;
		}

		throw new IllegalArgumentException("equation");
	}

	/**
	 * Converts SourceBlendingFactor
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(SourceBlendingFactor factor) {
		switch (factor) {
		case Zero:
			return GL.GL_ZERO;
		case One:
			return GL.GL_ONE;
		case SourceAlpha:
			return GL.GL_SRC_ALPHA;
		case OneMinusSourceAlpha:
			return GL.GL_ONE_MINUS_SRC_ALPHA;
		case DestinationAlpha:
			return GL.GL_DST_ALPHA;
		case OneMinusDestinationAlpha:
			return GL.GL_ONE_MINUS_DST_ALPHA;
		case DestinationColor:
			return GL.GL_DST_COLOR;
		case OneMinusDestinationColor:
			return GL.GL_ONE_MINUS_DST_COLOR;
		case SourceAlphaSaturate:
			return GL.GL_SRC_ALPHA_SATURATE;
		case ConstantColor:
			return GL2ES2.GL_CONSTANT_COLOR;
		case OneMinusConstantColor:
			return GL2ES2.GL_ONE_MINUS_CONSTANT_COLOR;
		case ConstantAlpha:
			return GL2ES2.GL_CONSTANT_ALPHA;
		case OneMinusConstantAlpha:
			return GL2ES2.GL_ONE_MINUS_CONSTANT_ALPHA;
		}

		throw new IllegalArgumentException("factor");
	}

	/**
	 * Converts DestinationBlendingFactor
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(DestinationBlendingFactor factor) {
		switch (factor) {
		case Zero:
			return GL.GL_ZERO;
		case One:
			return GL.GL_ONE;
		case SourceColor:
			return GL.GL_SRC_COLOR;
		case OneMinusSourceColor:
			return GL.GL_ONE_MINUS_SRC_COLOR;
		case SourceAlpha:
			return GL.GL_SRC_ALPHA;
		case OneMinusSourceAlpha:
			return GL.GL_ONE_MINUS_SRC_ALPHA;
		case DestinationAlpha:
			return GL.GL_DST_ALPHA;
		case OneMinusDestinationAlpha:
			return GL.GL_ONE_MINUS_DST_ALPHA;
		case DestinationColor:
			return GL.GL_DST_COLOR;
		case OneMinusDestinationColor:
			return GL.GL_ONE_MINUS_DST_COLOR;
		case ConstantColor:
			return GL4.GL_CONSTANT_COLOR;
		case OneMinusConstantColor:
			return GL4.GL_ONE_MINUS_CONSTANT_COLOR;
		case ConstantAlpha:
			return GL4.GL_CONSTANT_ALPHA;
		case OneMinusConstantAlpha:
			return GL4.GL_ONE_MINUS_CONSTANT_COLOR;
		}

		throw new IllegalArgumentException("factor");
	}

	/**
	 * Converts TextureFormat to the corresponding PixelInternalFormat
	 *
	 * @param type
	 * @return PixelInternalformat
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static PixelInternalFormat to(TextureFormat format) {
		switch (format) {
		case RedGreenBlue8:
			return PixelInternalFormat.Rgb8;
		case RedGreenBlue16:
			return PixelInternalFormat.Rgb16;
		case RedGreenBlueAlpha8:
			return PixelInternalFormat.Rgba8;
		case RedGreenBlue10A2:
			return PixelInternalFormat.Rgb10A2;
		case RedGreenBlueAlpha16:
			return PixelInternalFormat.Rgba16;
		case Depth16:
			return PixelInternalFormat.DepthComponent16;
		case Depth24:
			return PixelInternalFormat.DepthComponent24;
		case Red8:
			return PixelInternalFormat.R8;
		case Red16:
			return PixelInternalFormat.R16;
		case RedGreen8:
			return PixelInternalFormat.Rg8;
		case RedGreen16:
			return PixelInternalFormat.Rg16;
		case Red16f:
			return PixelInternalFormat.R16f;
		case Red32f:
			return PixelInternalFormat.R32f;
		case RedGreen16f:
			return PixelInternalFormat.Rg16f;
		case RedGreen32f:
			return PixelInternalFormat.Rg32f;
		case Red8i:
			return PixelInternalFormat.R8i;
		case Red8ui:
			return PixelInternalFormat.R8ui;
		case Red16i:
			return PixelInternalFormat.R16i;
		case Red16ui:
			return PixelInternalFormat.R16ui;
		case Red32i:
			return PixelInternalFormat.R32i;
		case Red32ui:
			return PixelInternalFormat.R32ui;
		case RedGreen8i:
			return PixelInternalFormat.Rg8i;
		case RedGreen8ui:
			return PixelInternalFormat.Rg8ui;
		case RedGreen16i:
			return PixelInternalFormat.Rg16i;
		case RedGreen16ui:
			return PixelInternalFormat.Rg16ui;
		case RedGreen32i:
			return PixelInternalFormat.Rg32i;
		case RedGreen32ui:
			return PixelInternalFormat.Rg32ui;
		case RedGreenBlueAlpha32f:
			return PixelInternalFormat.Rgba32f;
		case RedGreenBlue32f:
			return PixelInternalFormat.Rgb32f;
		case RedGreenBlueAlpha16f:
			return PixelInternalFormat.Rgba16f;
		case RedGreenBlue16f:
			return PixelInternalFormat.Rgb16f;
		case Depth24Stencil8:
			return PixelInternalFormat.Depth24Stencil8;
		case Red11fGreen11fBlue10f:
			return PixelInternalFormat.R11fG11fB10f;
		case RedGreenBlue9E5:
			return PixelInternalFormat.Rgb9E5;
		case SRedGreenBlue8:
			return PixelInternalFormat.Srgb8;
		case SRedGreenBlue8Alpha8:
			return PixelInternalFormat.Srgb8Alpha8;
		case Depth32f:
			return PixelInternalFormat.DepthComponent32f;
		case Depth32fStencil8:
			return PixelInternalFormat.Depth32fStencil8;
		case RedGreenBlueAlpha32ui:
			return PixelInternalFormat.Rgba32ui;
		case RedGreenBlue32ui:
			return PixelInternalFormat.Rgb32ui;
		case RedGreenBlueAlpha16ui:
			return PixelInternalFormat.Rgba16ui;
		case RedGreenBlue16ui:
			return PixelInternalFormat.Rgb16ui;
		case RedGreenBlueAlpha8ui:
			return PixelInternalFormat.Rgba8ui;
		case RedGreenBlue8ui:
			return PixelInternalFormat.Rgb8ui;
		case RedGreenBlueAlpha32i:
			return PixelInternalFormat.Rgba32i;
		case RedGreenBlue32i:
			return PixelInternalFormat.Rgb32i;
		case RedGreenBlueAlpha16i:
			return PixelInternalFormat.Rgba16i;
		case RedGreenBlue16i:
			return PixelInternalFormat.Rgb16i;
		case RedGreenBlueAlpha8i:
			return PixelInternalFormat.Rgba8i;
		case RedGreenBlue8i:
			return PixelInternalFormat.Rgb8i;
		}

		throw new IllegalArgumentException("format");
	}

	/**
	 * Converts ImageFormat
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(ImageFormat format) {
		switch (format) {
		case StencilIndex:
			return GL2ES2.GL_STENCIL_INDEX;
		case DepthComponent:
			return GL2ES2.GL_DEPTH_COMPONENT;
		case Red:
			return GL2ES2.GL_RED;
		case Green:
			return GL4.GL_GREEN;
		case Blue:
			return GL4.GL_BLUE;
		case RedGreenBlue:
			return GL.GL_RGB;
		case RedGreenBlueAlpha:
			return GL.GL_RGBA;
		case BlueGreenRed:
			return GL4.GL_BGR;
		case BlueGreenRedAlpha:
			return GL.GL_BGRA;
		case RedGreen:
			return GL4.GL_RG;
		case RedGreenInteger:
			return GL4.GL_RG_INTEGER;
		case DepthStencil:
			return GL.GL_DEPTH_STENCIL;
		case RedInteger:
			return GL4.GL_RED_INTEGER;
		case GreenInteger:
			return GL4.GL_GREEN_INTEGER;
		case BlueInteger:
			return GL4.GL_BLUE_INTEGER;
		case RedGreenBlueInteger:
			return GL4.GL_RGB_INTEGER;
		case RedGreenBlueAlphaInteger:
			return GL4.GL_RGBA_INTEGER;
		case BlueGreenRedInteger:
			return GL4.GL_BGR_INTEGER;
		case BlueGreenRedAlphaInteger:
			return GL4.GL_BGRA_INTEGER;
		}

		throw new IllegalArgumentException("format");
	}

	/**
	 * Converts ImageDataType
	 *
	 * @param type
	 * @return JOGL value
 	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(ImageDataType type) {
		switch (type) {
		case Byte:
			return GL.GL_BYTE;
		case UnsignedByte:
			return GL.GL_UNSIGNED_BYTE;
		case Short:
			return GL.GL_SHORT;
		case UnsignedShort:
			return GL.GL_UNSIGNED_SHORT;
		case Int:
			return GL4.GL_INT;
		case UnsignedInt:
			return GL.GL_UNSIGNED_INT;
		case Float:
			return GL.GL_FLOAT;
		case HalfFloat:
			return GL.GL_HALF_FLOAT;
		case UnsignedByte332:
			return GL4.GL_UNSIGNED_BYTE_3_3_2;
		case UnsignedShort4444:
			return GL4.GL_UNSIGNED_SHORT_4_4_4_4;
		case UnsignedShort5551:
			return GL4.GL_UNSIGNED_SHORT_5_5_5_1;
		case UnsignedInt8888:
			return GL4.GL_UNSIGNED_INT_8_8_8_8;
		case UnsignedInt1010102:
			return GL4.GL_UNSIGNED_INT_10_10_10_2;
		case UnsignedByte233Reversed:
			return GL4.GL_UNSIGNED_BYTE_2_3_3_REV;
		case UnsignedShort565:
			return GL4.GL_UNSIGNED_SHORT_5_6_5;
		case UnsignedShort565Reversed:
			return GL4.GL_UNSIGNED_SHORT_5_6_5_REV;
		case UnsignedShort4444Reversed:
			return GL4.GL_UNSIGNED_SHORT_4_4_4_4_REV;
		case UnsignedShort1555Reversed:
			return GL4.GL_UNSIGNED_SHORT_1_5_5_5_REV;
		case UnsignedInt8888Reversed:
			return GL4.GL_UNSIGNED_INT_8_8_8_8_REV;
		case UnsignedInt2101010Reversed:
			return GL4.GL_UNSIGNED_INT_2_10_10_10_REV;
		case UnsignedInt248:
			return GL4.GL_UNSIGNED_INT_24_8;
		case UnsignedInt10F11F11FReversed:
			return GL4.GL_UNSIGNED_INT_10F_11F_11F_REV;
		case UnsignedInt5999Reversed:
			return GL4.GL_UNSIGNED_INT_5_9_9_9_REV;
		case Float32UnsignedInt248Reversed:
			return GL4.GL_FLOAT_32_UNSIGNED_INT_24_8_REV;
		}

		throw new IllegalArgumentException("type");
	}

	/**
	 * Converts TextureFormat
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int textureToPixelFormat(TextureFormat textureFormat) {
		if (!isTextureFormatValid(textureFormat)) {
			throw new IllegalArgumentException("Invalid texture format");
		}

		// TODO: Not tested exhaustively
		switch (textureFormat) {
		case RedGreenBlue8:
			return GL.GL_RGB8;
		case RedGreenBlue16:
			return GL4.GL_RGB16;
		case RedGreenBlueAlpha8:
			return GL.GL_RGBA8;
		case RedGreenBlue10A2:
			return GL.GL_RGB10_A2;
		case RedGreenBlueAlpha16:
			return GL4.GL_RGBA16;
		case Depth16:
			return GL.GL_DEPTH_COMPONENT16;
		case Depth24:
			return GL.GL_DEPTH_COMPONENT24;
		case Red8:
			return GL4.GL_R8;
		case Red16:
			return GL4.GL_R16;
		case RedGreen8:
			return GL4.GL_RG8;
		case RedGreen16:
			return GL4.GL_RG16;
		case Red16f:
			return GL4.GL_R16F;
		case Red32f:
			return GL4.GL_R32F;
		case RedGreen16f:
			return GL4.GL_RG16F;
		case RedGreen32f:
			return GL4.GL_RG32F;
		case Red8i:
			return GL4.GL_R8I;
		case Red8ui:
			return GL4.GL_R8UI;
		case Red16i:
			return GL4.GL_R16I;
		case Red16ui:
			return GL4.GL_R16UI;
		case Red32i:
			return GL4.GL_R32I;
		case Red32ui:
			return GL4.GL_R32UI;
		case RedGreen8i:
			return GL4.GL_RG8I;
		case RedGreen8ui:
			return GL4.GL_RG8UI;
		case RedGreen16i:
			return GL4.GL_RG16I;
		case RedGreen16ui:
			return GL4.GL_RG16UI;
		case RedGreen32i:
			return GL4.GL_RG32I;
		case RedGreen32ui:
			return GL4.GL_RG32UI;
		case RedGreenBlueAlpha32f:
			return GL.GL_RGBA32F;
		case RedGreenBlue32f:
			return GL.GL_RGB32F;
		case RedGreenBlueAlpha16f:
			return GL4.GL_RGBA16F;
		case RedGreenBlue16f:
			return GL4.GL_RGB16F;
		case Depth24Stencil8:
			return GL.GL_DEPTH24_STENCIL8;
		case Red11fGreen11fBlue10f:
			return GL4.GL_R11F_G11F_B10F;
		case RedGreenBlue9E5:
			return GL4.GL_RGB9_E5;
		case SRedGreenBlue8:
			return GL4.GL_SRGB8;
		case SRedGreenBlue8Alpha8:
			return GL.GL_SRGB8_ALPHA8;
		case Depth32f:
			return GL4.GL_DEPTH_COMPONENT32F;
		case Depth32fStencil8:
			return GL4.GL_DEPTH32F_STENCIL8;
		case RedGreenBlueAlpha32ui:
			return GL4.GL_RGBA32UI;
		case RedGreenBlue32ui:
			return GL4.GL_RGB32UI;
		case RedGreenBlueAlpha16ui:
			return GL4.GL_RGBA16UI;
		case RedGreenBlue16ui:
			return GL4.GL_RGB16UI;
		case RedGreenBlueAlpha8ui:
			return GL4.GL_RGBA8UI;
		case RedGreenBlue8ui:
			return GL4.GL_RGB8UI;
		case RedGreenBlueAlpha32i:
			return GL4.GL_RGBA32I;
		case RedGreenBlue32i:
			return GL4.GL_RGB32I;
		case RedGreenBlueAlpha16i:
			return GL4.GL_RGBA16I;
		case RedGreenBlue16i:
			return GL4.GL_RGB16I;
		case RedGreenBlueAlpha8i:
			return GL4.GL_RGBA8I;
		case RedGreenBlue8i:
			return GL4.GL_RGB8I;
		}

		throw new IllegalArgumentException("textureFormat");
	}

	/**
	 * Converts TextureFormat to the corresponding PixelType
	 *
	 * @param type
	 * @return PixelType
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static PixelType textureToPixelType(TextureFormat textureFormat) {
		if (!isTextureFormatValid(textureFormat)) {
			throw new IllegalArgumentException("Invalid texture format.");
		}

		// TODO: Not tested exhaustively
		switch (textureFormat) {
		case RedGreenBlue8:
			return PixelType.UnsignedByte;
		case RedGreenBlue16:
			return PixelType.UnsignedShort;
		case RedGreenBlueAlpha8:
			return PixelType.UnsignedByte;
		case RedGreenBlue10A2:
			return PixelType.UnsignedInt1010102;
		case RedGreenBlueAlpha16:
			return PixelType.UnsignedShort;
		case Depth16:
			return PixelType.HalfFloat;
		case Depth24:
			return PixelType.Float;
		case Red8:
			return PixelType.UnsignedByte;
		case Red16:
			return PixelType.UnsignedShort;
		case RedGreen8:
			return PixelType.UnsignedByte;
		case RedGreen16:
			return PixelType.UnsignedShort;
		case Red16f:
			return PixelType.HalfFloat;
		case Red32f:
			return PixelType.Float;
		case RedGreen16f:
			return PixelType.HalfFloat;
		case RedGreen32f:
			return PixelType.Float;
		case Red8i:
			return PixelType.Byte;
		case Red8ui:
			return PixelType.UnsignedByte;
		case Red16i:
			return PixelType.Short;
		case Red16ui:
			return PixelType.UnsignedShort;
		case Red32i:
			return PixelType.Int;
		case Red32ui:
			return PixelType.UnsignedInt;
		case RedGreen8i:
			return PixelType.Byte;
		case RedGreen8ui:
			return PixelType.UnsignedByte;
		case RedGreen16i:
			return PixelType.Short;
		case RedGreen16ui:
			return PixelType.UnsignedShort;
		case RedGreen32i:
			return PixelType.Int;
		case RedGreen32ui:
			return PixelType.UnsignedInt;
		case RedGreenBlueAlpha32f:
			return PixelType.Float;
		case RedGreenBlue32f:
			return PixelType.Float;
		case RedGreenBlueAlpha16f:
			return PixelType.HalfFloat;
		case RedGreenBlue16f:
			return PixelType.HalfFloat;
		case Depth24Stencil8:
			return PixelType.UnsignedInt248;
		case Red11fGreen11fBlue10f:
			return PixelType.Float;
		case RedGreenBlue9E5:
			return PixelType.Float;
		case SRedGreenBlue8:
		case SRedGreenBlue8Alpha8:
			return PixelType.Byte;
		case Depth32f:
		case Depth32fStencil8:
			return PixelType.Float;
		case RedGreenBlueAlpha32ui:
		case RedGreenBlue32ui:
			return PixelType.UnsignedInt;
		case RedGreenBlueAlpha16ui:
		case RedGreenBlue16ui:
			return PixelType.UnsignedShort;
		case RedGreenBlueAlpha8ui:
		case RedGreenBlue8ui:
			return PixelType.UnsignedByte;
		case RedGreenBlueAlpha32i:
		case RedGreenBlue32i:
			return PixelType.UnsignedInt;
		case RedGreenBlueAlpha16i:
		case RedGreenBlue16i:
			return PixelType.UnsignedShort;
		case RedGreenBlueAlpha8i:
		case RedGreenBlue8i:
			return PixelType.UnsignedByte;
		}

		throw new IllegalArgumentException("textureFormat");
	}

	private static boolean isTextureFormatValid(TextureFormat textureFormat) {
		return (textureFormat == TextureFormat.RedGreenBlue8)
				|| (textureFormat == TextureFormat.RedGreenBlue16)
				|| (textureFormat == TextureFormat.RedGreenBlueAlpha8)
				|| (textureFormat == TextureFormat.RedGreenBlue10A2)
				|| (textureFormat == TextureFormat.RedGreenBlueAlpha16)
				|| (textureFormat == TextureFormat.Depth16)
				|| (textureFormat == TextureFormat.Depth24)
				|| (textureFormat == TextureFormat.Red8)
				|| (textureFormat == TextureFormat.Red16)
				|| (textureFormat == TextureFormat.RedGreen8)
				|| (textureFormat == TextureFormat.RedGreen16)
				|| (textureFormat == TextureFormat.Red16f)
				|| (textureFormat == TextureFormat.Red32f)
				|| (textureFormat == TextureFormat.RedGreen16f)
				|| (textureFormat == TextureFormat.RedGreen32f)
				|| (textureFormat == TextureFormat.Red8i)
				|| (textureFormat == TextureFormat.Red8ui)
				|| (textureFormat == TextureFormat.Red16i)
				|| (textureFormat == TextureFormat.Red16ui)
				|| (textureFormat == TextureFormat.Red32i)
				|| (textureFormat == TextureFormat.Red32ui)
				|| (textureFormat == TextureFormat.RedGreen8i)
				|| (textureFormat == TextureFormat.RedGreen8ui)
				|| (textureFormat == TextureFormat.RedGreen16i)
				|| (textureFormat == TextureFormat.RedGreen16ui)
				|| (textureFormat == TextureFormat.RedGreen32i)
				|| (textureFormat == TextureFormat.RedGreen32ui)
				|| (textureFormat == TextureFormat.RedGreenBlueAlpha32f)
				|| (textureFormat == TextureFormat.RedGreenBlue32f)
				|| (textureFormat == TextureFormat.RedGreenBlueAlpha16f)
				|| (textureFormat == TextureFormat.RedGreenBlue16f)
				|| (textureFormat == TextureFormat.Depth24Stencil8)
				|| (textureFormat == TextureFormat.Red11fGreen11fBlue10f)
				|| (textureFormat == TextureFormat.RedGreenBlue9E5)
				|| (textureFormat == TextureFormat.SRedGreenBlue8)
				|| (textureFormat == TextureFormat.SRedGreenBlue8Alpha8)
				|| (textureFormat == TextureFormat.Depth32f)
				|| (textureFormat == TextureFormat.Depth32fStencil8)
				|| (textureFormat == TextureFormat.RedGreenBlueAlpha32ui)
				|| (textureFormat == TextureFormat.RedGreenBlue32ui)
				|| (textureFormat == TextureFormat.RedGreenBlueAlpha16ui)
				|| (textureFormat == TextureFormat.RedGreenBlue16ui)
				|| (textureFormat == TextureFormat.RedGreenBlueAlpha8ui)
				|| (textureFormat == TextureFormat.RedGreenBlue8ui)
				|| (textureFormat == TextureFormat.RedGreenBlueAlpha32i)
				|| (textureFormat == TextureFormat.RedGreenBlue32i)
				|| (textureFormat == TextureFormat.RedGreenBlueAlpha16i)
				|| (textureFormat == TextureFormat.RedGreenBlue16i)
				|| (textureFormat == TextureFormat.RedGreenBlueAlpha8i)
				|| (textureFormat == TextureFormat.RedGreenBlue8i);
	}

	/**
	 * Converts TextureMinificationFilter
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(TextureMinificationFilter filter) {
		switch (filter) {
		case Nearest:
			return GL4.GL_NEAREST;
		case Linear:
			return GL4.GL_LINEAR;
		case NearestMipmapNearest:
			return GL4.GL_NEAREST_MIPMAP_NEAREST;
		case LinearMipmapNearest:
			return GL4.GL_LINEAR_MIPMAP_NEAREST;
		case NearestMipmapLinear:
			return GL4.GL_NEAREST_MIPMAP_LINEAR;
		case LinearMipmapLinear:
			return GL4.GL_LINEAR_MIPMAP_LINEAR;
		}

		throw new IllegalArgumentException("Texture Minification Filter");
	}

	/**
	 * Converts TextureMagnificationFilter
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(TextureMagnificationFilter filter) {
		switch (filter) {
		case Nearest:
			return GL4.GL_NEAREST;
		case Linear:
			return GL4.GL_LINEAR;
		}

		throw new IllegalArgumentException("Texture Magnification Filter");
	}

	/**
	 * Converts TextureWrap
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(TextureWrap wrap) {
		switch (wrap) {
		case Clamp:
			return GL4.GL_CLAMP_TO_EDGE;
		case Repeat:
			return GL4.GL_REPEAT;
		case MirroredRepeat:
			return GL4.GL_MIRRORED_REPEAT;
		}

		throw new IllegalArgumentException("Texture Wrap");
	}
	
	/**
	 * Converts TextureParameterName
	 *
	 * @param type
	 * @return JOGL value
	 * @throws IllegalArgumentException
	 *
	 * thread-safe
	 */
	public static int to(TextureParameterName name) {
		switch (name) {
		case Minfilter:
			return GL4.GL_TEXTURE_MIN_FILTER;
		case Magfilter:
			return GL4.GL_TEXTURE_MAG_FILTER;
		case TextureWrapS:
			return GL4.GL_TEXTURE_WRAP_S;
		case TextureWrapT:
			return GL4.GL_TEXTURE_WRAP_T;
		}

		throw new IllegalArgumentException("Texture Minification Filter");
	}

}