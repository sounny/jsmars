package edu.asu.jmars.viz3d.core.geometry;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.viz3d.Disposable;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;

/**
 * This class renders an open-ended cone in 3D
 * 
 * not thread-safe
 */
public class Cone implements GLRenderable, Disposable {
	int id;
	float radius;
	float[] tip;
	float[] base;
	float[] color;
	float length;
	float angle;
	float[] axis = new float[3];

	boolean cullFace = true;
	int slices = 32;
	int stacks = 32;

	private GLUquadric quadric = null;
	private GLU glu = new GLU();

	private static float radToDeg = 180f / FloatUtil.PI;
	private static DebugLog log = DebugLog.instance();

	private boolean preRendered = false;
	private float alpha = 1f;
	private Float displayAlpha;
	private boolean isScalable = true;
	private boolean hasBeenScaled = false;

	/**
	 * Cone Constructor
	 * 
	 * @param id
	 *            Identifier of this Cone instance. Should be unique!
	 * @param radiusOfBase
	 *            radius of the base of the cone
	 * @param coneTip
	 *            location (x,y,z) of the tip of the cone
	 * @param baseLoc
	 *            location (x,y,z) of the center of the base of the cone
	 * @param numOfRadialSegments
	 *            defines the "smoothness" of the cone along its width higher
	 *            values = greater smoothness but can incur performance hits
	 * @param numOfLengthSegments
	 *            "smoothness" of the cone along its length
	 * @param color
	 *            color of the cone including opacity (R,G,B,A)
	 * @param cullBackFace
	 *            set to true if back face culling is desired otherwise no face
	 *            culling will be applied
	 * @throws IllegalArgumentException
	 */
	public Cone(int id, float radiusOfBase, float[] coneTip, float[] baseLoc,
			int numOfRadialSegments, int numOfLengthSegments, float[] color,
			boolean cullBackFace) throws IllegalArgumentException {
		if (coneTip == null || coneTip.length != 3) {
			log.aprintln("Invalid tip array for 3D True Cone constructor");
			throw new IllegalArgumentException(
					"Invalid tip array for 3D True Cone constructor");
		}
		if (baseLoc == null || baseLoc.length != 3) {
			log.aprintln("Invalid base array for 3D True Cone constructor");
			throw new IllegalArgumentException(
					"Invalid base array for 3D True Cone constructor");
		}
		if (color == null || color.length != 4) {
			log.aprintln("Invalid color array for 3D True Cone constructor");
			throw new IllegalArgumentException(
					"Invalid color array for 3D True Cone constructor");
		}

		this.id = id;
		radius = radiusOfBase;
		tip = coneTip;
		base = baseLoc;
		slices = numOfRadialSegments;
		stacks = numOfLengthSegments;
		this.color = color;
		cullFace = cullBackFace;

		length = VectorUtil.distVec3(tip, base);

		float[] diff = new float[3];
		// translate the cone vector to the origin
		diff = VectorUtil.subVec3(diff, tip, base);
		// calculate the angle to rotate the cone from from its starting Z-axis
		// aligned starting direction
		angle = VectorUtil.angleVec3(diff, new float[] { 0f, 0f, 1f })
				* radToDeg;
		// calculate the axis to rotate the cone about to put in final position
		axis = VectorUtil.crossVec3(axis, new float[] { 0f, 0f, 1f }, diff);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.asu.jmars.viz3d.Disposable#dispose()
	 */
	@Override
	public void dispose() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.asu.jmars.viz3d.renderer.gl.GLRenderable#execute(com.jogamp.opengl
	 * .GL2)
	 */
	@Override
	public void execute(GL2 gl) {
		gl.glPushMatrix();
		gl.glTranslatef(base[0], base[1], base[2]); // position the cone
		gl.glRotatef(angle, axis[0], axis[1], axis[2]); // rotate the cone to
														// align with the line

		// draw the cone
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
		// (GLUquadric, base, top, height, #slices, #stacks)
		glu.gluCylinder(quadric, radius, 0.0f, length, slices, stacks);
		gl.glPopMatrix();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.asu.jmars.viz3d.renderer.gl.GLRenderable#preRender(com.jogamp.opengl
	 * .GL2)
	 */
	@Override
	public void preRender(GL2 gl) {
		if (preRendered) {
			return;
		}
		if (quadric == null) {
			// Create A Pointer To The Cone Quadric Object
			quadric = glu.gluNewQuadric();
			if (quadric == null) {
				log.aprint("Could not create 3D TrueCone - out of memory on graphics card!");
			}
			glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH); // Create Smooth
															// Normals
			glu.gluQuadricTexture(quadric, true); // Create Texture Coords
		}
		preRendered = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.asu.jmars.viz3d.renderer.gl.GLRenderable#postRender(com.jogamp.opengl
	 * .GL2)
	 */
	@Override
	public void postRender(GL2 gl) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.asu.jmars.viz3d.renderer.gl.GLRenderable#delete(com.jogamp.opengl
	 * .GL2)
	 */
	@Override
	public void delete(GL2 gl) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#getAlpha()
	 */
	@Override
	public float getAlpha() {
		return alpha;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#getDisplayAlpha()
	 */
	@Override
	public float getDisplayAlpha() {
		if (displayAlpha == null) {
			return alpha;
		}
		return displayAlpha;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#setDisplayAlpha(float)
	 */
	@Override
	public void setDisplayAlpha(float alpha) {
		displayAlpha = alpha;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#isScalable()
	 */
	@Override
	public boolean isScalable() {
		return isScalable;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#scaleByDivision(float)
	 */
	@Override
	public void scaleByDivision(float scalar) {
		if (Float.compare(scalar, 0f) == 0) {
			log.aprintln("Attempting to scale a GLRenderable by dividing by zero.");
			return;
		}
		if (hasBeenScaled) {
			//NOP
			return;
		}
		float scaleFactor = 1f / scalar;

		if (isScalable()) {
			radius *= scaleFactor;
			length *= scaleFactor;
			base = VectorUtil.scaleVec3(new float[3], base, new float[]{scaleFactor, scaleFactor, scaleFactor});
		}
		hasBeenScaled = true;
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
	public float[] getColor() {
		return color;
	}
}
