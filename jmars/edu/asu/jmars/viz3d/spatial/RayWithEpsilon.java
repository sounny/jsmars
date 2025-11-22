package edu.asu.jmars.viz3d.spatial;

import java.util.Arrays;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;


/**
 * Class to represent a Ray (an infinite directed line segment) in 3D
 *
 * thread-safe
 */
public class RayWithEpsilon implements GLRenderable {
	
	// The direction of the ray.
	float[] direction;
	// The origin of the ray.
	float[] origin;
	
	float epsilon;
	
	float[] color;
	
	float angle;
	float[] axis = new float[3];

	boolean cullFace = true;
	int slices = 32;
	int stacks = 32;
	
	float minEpsilonForRendering = 0.01f;


    private GLUquadric quadric = null;
	private GLU glu = new GLU();
	
	private boolean preRendered = false;
	
    private static DebugLog log = DebugLog.instance();
	private static float radToDeg = 180f / FloatUtil.PI;
	
	private float alpha = 1f;
	private Float displayAlpha;
	private boolean isScalable = true;
	private boolean hasBeenScaled = false;
	private float length;
	private float[] end = new float[3];

	/** 
	 * Constructor
	 * @param origin 3D point of ray origin
	 * @param direction 3D vector of the ray direction
	 * @param epsilon perpendicular distance from the Ray for an acceptable intersection
	 * @param color 3 element OpenGL color (normalized RGB)
	 */
	public RayWithEpsilon (float[] origin, float[] direction, float epsilon, float[] color) {
		this.origin = origin;
		this.direction = direction;
		this.epsilon = epsilon;
		this.color = color;
		calcEndPoint();
		calcAngleAxis();
	}
	
	public RayWithEpsilon (float[] origin, float[] direction, float[] end, float epsilon, float[] color) {
		this.origin = origin;
		this.direction = direction;
		this.epsilon = epsilon;
		this.color = color;
		this.end = end;
//		calcEndPoint();
		calcAngleAxis();
	}
	
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof RayWithEpsilon)) {
			return false;
		}
		
		RayWithEpsilon other = (RayWithEpsilon)obj;
		
		if (Arrays.equals(this.origin, other.origin)
			&& Arrays.equals(this.direction, other.direction)
			&& Arrays.equals(this.end, other.end)
			&& this.epsilon == other.epsilon
			&& Arrays.equals(this.color,other.color)) {
			return true;
		}
		return false;
	}
	
	public boolean hasSameEndPointsAs(RayWithEpsilon other) {
		if (other != null
			&& Arrays.equals(this.origin, other.origin)
			&& Arrays.equals(this.direction, other.direction)){
			return true;
		}
		return false;
	}
	
	private void calcEndPoint() {
		// length of origin
		float olen = VectorUtil.normVec3(origin);
		// scale direction to length of origin * 2
		float[] tmp = VectorUtil.scaleVec3(new float[3], direction, olen * 2);
		// add scaled direction to origin to determine the end
		end = VectorUtil.addVec3(new float[3], origin, tmp);
		length = VectorUtil.distVec3(origin, end);		
	}
	
	private void calcAngleAxis() {
		float[] diff = new float[3];
		// translate the vector to the origin
		diff = VectorUtil.subVec3(diff, end, origin);
		// calculate the angle to rotate the cylinder from from its starting Z-axis
		// aligned starting direction
		angle = VectorUtil.angleVec3(diff, new float[] { 0f, 0f, 1f })
				* radToDeg;
		// calculate the axis to rotate the cylinder about to put in final position
		axis = VectorUtil.crossVec3(axis, new float[] { 0f, 0f, 1f }, diff);		
	}

	/**
	 * Returns the direction of the Ray
	 *
	 * @return float[X, Y, Z]
	 *
	 * thread-safe
	 */
	public float[] getDirection() {
		return direction;
	}
	
	/** 
	 * Sets the direction of the Ray
	 *
	 * @param direction float[X, Y, Z]
	 *
	 * thread-safe
	 */
	public void setDirection(float[] direction) {
		this.direction = direction;
	}
	
	public float[] getEnd() {
		return end;
	}

	public void setEnd(float[] end) {
		this.end = end;
	}

	/**
	 * Returns the point of Ray origin
	 *
	 * @return float[X, Y, Z]
	 *
	 * thread-safe
	 */
	public float[] getOrigin() {
		return origin;
	}
	
	/**
	 * Sets the point of origin of the Ray
	 *
	 * @param origin float[X, Y, Z]
	 *
	 * thread-safe
	 */
	public void setOrigin(float[] origin) {
		this.origin = origin;
	}

	public float getEpsilon() {
		return epsilon;
	}

	public void setEpsilon(float epsilon) {
		this.epsilon = epsilon;
	}

	@Override
	public String toString() {
		return ""+origin[0]+", "+origin[1]+", "+origin[2]+"\n"+direction[0]+", "+direction[1]+", "+direction[2]+"\n"+end[0]+", "+end[1]+", "+end[2]+"\n";
	}
	
	/**
	 * Convenience method for debug
	 *
	 * thread-safe
	 */
	public void print() {
		System.err.format("\n%11.9f %11.9f %11.9f %11.9f %11.9f %11.9f\n", origin[0], origin[1], origin[2], direction[0], direction[1], direction[2]);
	}

	@Override
	public void execute(GL2 gl) {
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		if (epsilon > minEpsilonForRendering) {
			gl.glPushMatrix();
			gl.glTranslatef(origin[0], origin[1], origin[2]); // position the origin
			gl.glRotatef(angle, axis[0], axis[1], axis[2]); // rotate the ray to
															// align with the line
	
			// draw the cylinder
			gl.glColor4f(color[0], color[1], color[2], getDisplayAlpha());
			gl.glEnable(GL2.GL_BLEND);
			gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
			if (cullFace) {
				gl.glEnable(GL2.GL_CULL_FACE);
				gl.glCullFace(GL2.GL_BACK);
				gl.glFrontFace(GL2.GL_CCW);
			} else {
				gl.glDisable(GL2.GL_CULL_FACE);
			}
			// quadric - Specifies the quadrics object 
			// epsilon - Specifies the radius of the cylinder (epsilon) at z = 0
			// epsilon - Specifies the radius of the cylinder (epsilon) at z = length
			// length - Specifies the height of the cylinder
			// slices - Specifies the number of subdivisions around the z axis
			// stacks - Specifies the number of subdivisions along the z axis
			glu.gluCylinder(quadric, epsilon, epsilon, length, slices, stacks);
			gl.glPopMatrix();
		}
			// render the ray as a line segment
			gl.glColor4f(color[0], color[1], color[2], getDisplayAlpha());
			gl.glLineWidth(2);
			gl.glBegin(GL2.GL_LINES);
			gl.glVertex3f(origin[0], origin[1], origin[2]);
			gl.glVertex3f(end[0], end[1], end[2]);
			gl.glEnd();

	}

	@Override
	public void preRender(GL2 gl) {
		if (preRendered || epsilon < minEpsilonForRendering) {
			return;
		}
		if (quadric == null) {
	        quadric = glu.gluNewQuadric();
	        if (quadric == null) {
	    		log.aprint("Could not create Ray with Epsilon - out of memory on graphics card!");
	    		return;
	        }
		}
		preRendered = true;
	}

	@Override
	public void postRender(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public float getAlpha() {
		return alpha;
	}

	@Override
	public float getDisplayAlpha() {
		if (displayAlpha == null) {
			return alpha;
		}
		return displayAlpha;
	}

	@Override
	public void setDisplayAlpha(float alpha) {
		displayAlpha = alpha;
	}

	@Override
	public boolean isScalable() {
		return isScalable;
	}

	@Override
	public void scaleByDivision(float scalar) {
//		if (Float.compare(scalar, 0f) == 0) {
//			log.aprintln("Attempting to scale a GLRenderable by dividing by zero.");
//			return;
//		}
//		if (hasBeenScaled) {
//			//NOP
//			return;
//		}
//		float scaleFactor = 1f / scalar;
//
//		if (isScalable()) {
//			epsilon *= scaleFactor;
//			length *= scaleFactor;
//			origin = VectorUtil.scaleVec3(new float[3], origin, new float[]{scaleFactor, scaleFactor, scaleFactor});
//		}
//		hasBeenScaled = true;
	}

	@Override
	public void scaleToShapeModel(boolean canScale) {
		isScalable = canScale;		
	}

	@Override
	public boolean isScaled() {
		return hasBeenScaled;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}	
		
	public float getLength() {
		return VectorUtil.distVec3(origin,  end);
	}

	@Override
	public float[] getColor() {
		return color;
	}
	
}