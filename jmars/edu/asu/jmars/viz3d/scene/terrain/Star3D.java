package edu.asu.jmars.viz3d.scene.terrain;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.VectorUtil;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.viz3d.Disposable;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.asu.jmars.viz3d.core.geometry.TriangleFan;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;

/**
 * Representation of a star for 3D rendering.
 * This class is basically a container class for the star
 * with a small amount of rendering code.
 */
public class Star3D implements GLRenderable {
	/*
	 * Minimum allowed stellar magnitude
	 */
	private static final float MIN_MAGNITUDE = 14f;
	/*
	 * Maximum allowed stellar magnitude
	 */
	private static final float MAX_MAGNITUDE = -2f;
	
	private String tooltip = "No details available for this star.";
	private float magnitude;
	private float[] center;
	private float[] color = new float[]{1f, 1f, 1f};
	private float opacity = 1f;
	private Float displayOpacity;
	private float starSize = 1f;
    private boolean hasBeenScaled = false;

	private static DebugLog log = DebugLog.instance();

	/**
	 * Constructor
	 * @param point	3D (X,Y,Z) location of the star
	 * @param color (R,G,B) 
	 * @param magnitude stellar magnitude limited to the range [Star3D.MIN_MAGNITUDE, Star3D.MIN_MAGNITUDE]
	 * @param tooltip Text description to be displayed when user mouses over the star in the display
	 * @param opacity [0.0, 1.0]
	 * @throws IllegalArgumentException when an invalid star location or color are passed in
	 * 									as well when the stellar magnitude is greater than -2 or less than 14
	 */
	public Star3D(float[] point, float[] color, float magnitude, String tooltip, float opacity) throws IllegalArgumentException {
		if (point == null || point.length != 3) {
			log.aprintln("Invalid point");
			throw new IllegalArgumentException("Invalid point for Star constructor");
		}
		if (color == null || color.length != 3) {
			log.aprintln("Invalid color vector");
			throw new IllegalArgumentException("Invalid color vector for Star constructor");
		}
		if (magnitude > MIN_MAGNITUDE || magnitude < MAX_MAGNITUDE) {
			log.aprintln("Invalid magnitude: "+ magnitude);
			throw new IllegalArgumentException("Invalid magnitude for Star constructor: "+ magnitude + " should be within ["+MIN_MAGNITUDE+","+MAX_MAGNITUDE+"].");
		}
		
		center = point;
		this.magnitude = magnitude;
		this.tooltip = tooltip; 
		this.color = color;
		this.opacity = opacity;
		
	    double diff = MIN_MAGNITUDE-MAX_MAGNITUDE;
	    starSize = (float)Math.pow(1.04*Math.log10(diff), (diff-(magnitude-MAX_MAGNITUDE)));
	    
		this.tooltip = tooltip;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#execute(com.jogamp.opengl.GL2)
	 */
	@Override
	public void execute(GL2 gl) {
	    gl.glColor4f(color[0], color[1], color[2], getDisplayAlpha());
	    gl.glPointSize(starSize);
		gl.glEnable(GL2.GL_POINT_SMOOTH);
	    gl.glBegin(GL2.GL_POINTS);
	    	gl.glVertex3f(center[0], center[1], center[2]);
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
		return opacity;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#getDisplayAlpha()
	 */
	@Override
	public float getDisplayAlpha() {
		if(displayOpacity == null){
			return opacity;
		}
		return displayOpacity;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#setDisplayAlpha(float)
	 */
	@Override
	public void setDisplayAlpha(float alpha) {
		displayOpacity = alpha;
	}
	
	/**
	 * @return the 3D (X,Y,Z) location of the star
	 */
	public float[] getLocation() {
		return center;
	}
	/**
	 * @return the stellar magnitude
	 */
	public float getMagnitude() {
		return magnitude;
	}
	/**
	 * @return The textual description of the star (User defined)
	 */
	public String getTooltip() {
		return tooltip;
	}
	/**
	 * @return The star's size in OpenGL point units
	 */
	public float getStarSize() {
		return starSize;
	}

	/**
	 * Quick test method.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		ThreeDManager mgr;
		mgr = ThreeDManager.getInstance();
		mgr.show();
		Star3D newStar = new Star3D(new float[]{0.1f, 0f, 0f}, new float[]{1f, 1f, 1f}, -2f, "tooltip", 1f);	
		mgr.addRenderable(newStar);
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#isScalable()
	 */
	@Override
	public boolean isScalable() {
		if (center != null && center.length > 0) {
			return true;
		} else {
			return false;
		}
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
			center = VectorUtil.scaleVec3(new float[3], center, scaleFactor);
		}
		hasBeenScaled = true;
	}

	@Override
	public void scaleToShapeModel(boolean canScale) {
		// NOP		
	}

	@Override
	public boolean isScaled() {
		// TODO Auto-generated method stub
		return hasBeenScaled;
	}

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
        
    }

	@Override
	public float[] getColor() {
		return color;
	}

}
