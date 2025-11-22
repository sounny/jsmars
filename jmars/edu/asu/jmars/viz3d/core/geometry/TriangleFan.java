/**
 * 
 */
package edu.asu.jmars.viz3d.core.geometry;

import com.jogamp.opengl.GL2;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.viz3d.Disposable;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;

/**
 * A class to represent a triangle fan for rendering in 3D
 *
 * This class was created to allow rendering of elliptical cones.
 * However it is equally applicable to any triangle fan.
 * https://en.wikipedia.org/wiki/Triangle_fan
 *
 * not thread safe
 */
public class TriangleFan implements GLRenderable, Disposable {
	
	float[] vertices;
	float[] color;
	boolean cullBack = true;
	boolean closed = true;
	int id;
	
    private float alpha = 1f;
    private Float displayAlpha;
    private boolean isScalable = true;
    private boolean hasBeenScaled = false;
	
    private static DebugLog log = DebugLog.instance();
    
	/**
	 * Constructor
	 * 
	 * @param idNumber integer identifier for this object
	 * @param Vertices array that defines the fan. 
	 * 	The first three element (x,y,z) vertex (origin) must be the 
	 * 	vertex common to all the triangles. The remainder of the 
	 * 	vertices must be in CCW winding order. 
	 * @param color 4 element array (R, G, B, A)
	 * @param backFaceCull set to true if back face culling is desired otherwise
	 *  no face culling will be applied
	 * @throws IllegalArgumentException
	 */
	public TriangleFan(int idNumber, float[] vertices, float[] color, boolean backFaceCull, boolean closeFan) {
		if (vertices == null || vertices.length < 9) {
    		log.aprintln("Invalid vertex array for 3D Triangle Fan constructor");
    		throw new IllegalArgumentException("Invalid vertex array for 3D Triangle Fan constructor");
		}
		if (color == null || color.length != 4) {
    		log.aprintln("Invalid color array for 3D Triangle Fan constructor");
    		throw new IllegalArgumentException("Invalid color array for 3D Triangle Fan constructor");
		}
		id = idNumber;
		this.vertices = vertices;
		this.color = color;
		cullBack = backFaceCull;
		closed = closeFan;
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
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		
        if (cullBack) {
            gl.glEnable(GL2.GL_CULL_FACE);
    		gl.glCullFace(GL2.GL_BACK);
        	gl.glFrontFace(GL2.GL_CCW);
        } else {
        	gl.glDisable(GL2.GL_CULL_FACE);
        }
    	
	    gl.glBegin(GL2.GL_TRIANGLE_FAN);
	    for (int i=0; i<vertices.length; i+=3) {
	    	gl.glVertex3f(vertices[i], vertices[i+1], vertices[i+2]);
	    }
	    if (closed) {
	    	gl.glVertex3f(vertices[3], vertices[4], vertices[5]);
	    }
	    gl.glEnd();
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#preRender(com.jogamp.opengl.GL2)
	 */
	@Override
	public void preRender(GL2 gl) {
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#postRender(com.jogamp.opengl.GL2)
	 */
	@Override
	public void postRender(GL2 gl) {
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#delete(com.jogamp.opengl.GL2)
	 */
	@Override
	public void delete(GL2 gl) {
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
		for (int i=0; i<vertices.length; i++) {
			vertices[i] = vertices[i] * scaleFactor;
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
