/**
 * 
 */
package edu.asu.jmars.viz3d.renderer.gl.points;

import com.jogamp.opengl.GL2;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.viz3d.Disposable;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;

/**
 * This class represents any array of one or more points to be drawn in 3D.
 *
 * Point size and color are all user defined.
 *
 * Depends on JOGL 2.3.1 or higher
 *
 * not thread safe
 */
public class BasicPoints  implements GLRenderable, Disposable {

	private float[] points;
	private float[] color;
	private float size;
	private boolean isScalable = true;
    private boolean hasBeenScaled = false;

	private float alpha = 1f;
	private Float displayAlpha;
	
	private static DebugLog log = DebugLog.instance();

	/**
	 * Contructor
	 * @param points a single dimension array of floating point values representing sequential points in BIP or x,y,z organization
	 * @param color desired color of the points as an array of 3 normalized floating point values representing RGB color components
	 * @param size floating point value representing the desired point diameter. Will be dynamic in the future
	 */
	public BasicPoints(int idNumber, float[] points, float[] color, float size) {
		this.points = points;
		this.color = color;
		this.size = size;
	}
		
	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.Disposable#dispose()
	 */
	@Override
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#execute(com.jogamp.opengl.GL2)
	 */
	@Override
	public void execute(GL2 gl) {
	    gl.glColor4f(color[0], color[1], color[2], getDisplayAlpha());
	    gl.glPointSize(size);
		gl.glEnable(GL2.GL_POINT_SMOOTH);
	    gl.glBegin(GL2.GL_POINTS);
	    for (int i=0; i<points.length; i+=3) {
	    	gl.glVertex3f(points[i], points[i+1], points[i+2]);
	    }
	    gl.glEnd();
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#preRender(com.jogamp.opengl.GL2)
	 */
	@Override
	public void preRender(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#postRender(com.jogamp.opengl.GL2)
	 */
	@Override
	public void postRender(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#delete(com.jogamp.opengl.GL2)
	 */
	@Override
	public void delete(GL2 gl) {
		dispose();	
	}

	/**
	 * Returns the current color of the points
	 *
	 * @return color as a float[]
	 *
	 * thread-safe
	 */
	public float[] getColor() {
		return color;
	}

	/**
	 * Sets the color of the points
	 *
	 * @param color should be a normalized 3 element array (R,G,B)
	 *
	 * not thread-safe
	 */
	public void setColor(float[] color) {
		this.color = color;
	}

	/**
	 * Returns the current size of the points
	 *
	 * @return
	 *
	 * thread-safe
	 */
	public float getSize() {
		return size;
	}

	/**
	 * Sets the point size
	 *
	 * @param size
	 *
	 * thread-safe
	 */
	public void setSize(float size) {
		this.size = size;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#getAlpha()
	 */
	@Override
	public float getAlpha() {
		return alpha;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#getDisplayAlpha()
	 */
	@Override
	public float getDisplayAlpha() {
		if(displayAlpha == null){
			return alpha;
		}
		return displayAlpha;
	}

	/* (non-Javadoc)
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
		if (points != null && points.length > 0) {
			for (int i=0; i<points.length; i++) {
				points[i] *= scaleFactor;
			}
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

}
