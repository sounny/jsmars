package edu.asu.jmars.viz3d.renderer.gl.outlines;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.viz3d.Disposable;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;
import edu.asu.jmars.viz3d.renderer.gl.OutLineType;

/**
 * This class represents any outline object to be drawn in 3D.
 * Examples would be target footprints, stamp outlines, and simple polygons.
 *
 * If the desired outline is open ended, the closedLoop parameter to the constructor
 * should be set to false, otherwise set to true.
 * Line width, color, and offset above the surface of the body are all user defined.
 *
 * Depends on JOGL 2.3.1 or higher
 *
 * not thread safe
 */
public class OutLine implements GLRenderable, Disposable {
	
	private float[] color = {1f, 1f, 1f};
	protected float[] points;
	float[] fittedPoints;
	int width = 1;
	float scaleFactor = 1.005f;
	boolean closed = false;
	boolean dashed = false;
	private boolean isScalable = true;
    private boolean hasBeenScaled = false;
    private boolean beingFitted = false;

	
	float[] lightDiffuse =	 { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] lightAmbient =	 { 0.8f, 0.8f, 0.8f, 1.0f };
	
	public static final short SOLID_DASH = (short)0xFFFF;
	public static final short BIG_DASH = 0x00FF;
	public static final short SMALL_DASH = 0x0AAA;
	public static final short ALTERNATING_DASH = 0x0C0F;
	
	short dashPattern = BIG_DASH;
	int multFactor = 1;
	
	OutLineType type;
	
	boolean fitted = false;
	boolean fittingEnabled = false;
	
    private static DebugLog log = DebugLog.instance();	 

    private float alpha = 0.5f;
    private Float displayAlpha;
    
    protected Object id = null;
	 

	/**
	 * Constructor
	 * @param points a single dimension array of floating point values representing sequential points in an outline in BIP or x,y,z organization
	 * @param lineColor desired color of the outline as an array of 3 normalized floating point values representing RGB color components
	 * @param lineWidth integer value representing the desired line width, a value of 1 is relatively thin when not zoomed in. Will be dynamic in the future
	 */
	public OutLine(Object idNumber, float[] points, float[] lineColor, int lineWidth,  boolean closedLoop) {
		
		if (points == null || points.length < 6) { // we need at lease 2 points (2x3 vertices) to create a line
			log.aprintln("Cannot create a polygon with less than 3 points...");
			return;
		} 

		id = idNumber;
		this.points = points;
		this.color = lineColor;
		this.width = lineWidth;
		closed = closedLoop;
		lightAmbient = lineColor;
		lightDiffuse = lineColor;
		type = OutLineType.OffBody;
	}

	/**
	 * Constructor
	 * @param points a single dimension array of floating point values representing sequential points in an outline in BIP or x,y,z organization
	 * @param lineColor desired color of the outline as an array of 3 normalized floating point values representing RGB color components
	 * @param lineWidth integer value representing the desired line width, a value of 1 is relatively thin when not zoomed in. Will be dynamic in the future
	 */
	public OutLine(Object idNumber, float[] points, float[] lineColor, int lineWidth, boolean closedLoop, boolean onBody) {
		
		if (points == null || points.length < 6) { // we need at lease 2 points (2x3 vertices) to create a line
			log.aprintln("Cannot create a line with less than 2 points...");
			return;
		} 

		id = idNumber;
		this.points = points;
		if (onBody) {
			type = OutLineType.OnBody;
			for (int i=0; i<this.points.length; i++) {
				this.points[i] = this.points[i] * scaleFactor;
		    }
		} else {
			type = OutLineType.OffBody;
		}
		this.color = lineColor;
		this.width = lineWidth;
		closed = closedLoop;
		lightAmbient = lineColor;
		lightDiffuse = lineColor;
	}
	
	/**
	 * Constructor
	 * @param points a single dimension array of floating point values representing sequential points in an outline in BIP or x,y,z organization
	 * @param lineColor desired color of the outline as an array of 3 normalized floating point values representing RGB color components
	 * @param lineWidth integer value representing the desired line width, a value of 1 is relatively thin when not zoomed in. Will be dynamic in the future
	 * @param dashPattern Sets the current dash pattern for lines. The pattern argument is a 16-bit series of 0s and 1s, and it's repeated as necessary 
	 * 		to stipple a given line. A 1 indicates that drawing occurs, and 0 that it does not, on a pixel-by-pixel basis, beginning with the low-order bit 
	 * 		of the pattern. 
	 * @param multFactor The dash pattern can be stretched out by using multFactor, which multiplies each sub-series of consecutive 1s and 0s. Thus, if three 
	 * 		consecutive 1s appear in the pattern, they're stretched to six if factor is 2. multFactor is clamped to lie between 1 and 255.
	 */
	public OutLine(Object idNumber, float[] points, float[] lineColor, int lineWidth, boolean closedLoop, short dashPattern, int multFactor) {
		
		if (points == null || points.length < 6) { // we need at lease 2 points (2x3 vertices) to create a line
			log.aprintln("Cannot create a line with less than 2 points...");
			return;
		} 

		id = idNumber;
		this.points = points;
		for (int i=0; i<this.points.length; i++) {
			this.points[i] = this.points[i] * scaleFactor;
		}
		this.color = lineColor;
		this.width = lineWidth;
		closed = closedLoop;
		lightAmbient = lineColor;
		lightDiffuse = lineColor;
		this.dashPattern = dashPattern;
		this.multFactor = multFactor;
		dashed = true;
		type = OutLineType.OffBody;
	}
	
	/**
	 * Constructor
	 * @param points a single dimension array of floating point values representing sequential points in an outline in BIP or x,y,z organization
	 * @param lineColor desired color of the outline as an array of 3 normalized floating point values representing RGB color components
	 * @param lineWidth integer value representing the desired line width, a value of 1 is relatively thin when not zoomed in. Will be dynamic in the future
	 * @param dashPattern Sets the current dash pattern for lines. The pattern argument is a 16-bit series of 0s and 1s, and it's repeated as necessary 
	 * 		to stipple a given line. A 1 indicates that drawing occurs, and 0 that it does not, on a pixel-by-pixel basis, beginning with the low-order bit 
	 * 		of the pattern. 
	 * @param multFactor The dash pattern can be stretched out by using multFactor, which multiplies each sub-series of consecutive 1s and 0s. Thus, if three 
	 * 		consecutive 1s appear in the pattern, they're stretched to six if factor is 2. multFactor is clamped to lie between 1 and 255.
	 * @param onBody if <code>true</code> fits the outline to the associated shape model
	 */
	public OutLine(Object idNumber, float[] points, float[] lineColor, int lineWidth, boolean closedLoop, short dashPattern, int multFactor, boolean onBody) {
		
		if (points == null || points.length < 6) { // we need at lease 2 points (2x3 vertices) to create a line
			log.aprintln("Cannot create a line with less than 2 points...");
			return;
		} 

		id = idNumber;
		this.points = points;
		if (onBody) {
			type = OutLineType.OnBody;
		} else {
			type = OutLineType.OffBody;
//			for (int i=0; i<this.points.length; i++) {
//				this.points[i] = this.points[i] * scaleFactor;
//			}
		}
		this.color = lineColor;
		this.width = lineWidth;
		closed = closedLoop;
		lightAmbient = lineColor;
		lightDiffuse = lineColor;
		this.dashPattern = dashPattern;
		this.multFactor = multFactor;
		dashed = true;
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
		if (points == null || points.length < 6) { // we need at lease 2 points (2x3 vertices) to create a line
			return;
		} 

		gl.glEnable(GL2.GL_LINE_SMOOTH);
		gl.glHint (GL2.GL_LINE_SMOOTH_HINT, GL2.GL_DONT_CARE);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_AMBIENT, lightAmbient, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_DIFFUSE, lightDiffuse, 0);

		if (closed) {
			if (fitted && fittingEnabled) {
				drawLineLoop(gl, this.fittedPoints, this.width, this.color);
			} else {
				drawLineLoop(gl, this.points, this.width, this.color);
			}
		} else {
			if (fitted && fittingEnabled) {
				drawLine(gl, this.fittedPoints, this.width, this.color);
			} else {
				drawLine(gl, this.points, this.width, this.color);
			}
		}		
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
	
	void drawLine(GL2 gl, float[] points, int width, float[] color)
	{
	    gl.glColor4f(color[0], color[1], color[2], getDisplayAlpha());
	    if (dashed) {
	    	gl.glEnable(GL2.GL_LINE_STIPPLE);
	    	gl.glLineStipple(multFactor, dashPattern);
	    }
	    
	    gl.glLineWidth(width);
	    gl.glBegin(GL2.GL_LINES);
	    for (int i=0; i<points.length-3; i+=3) {
	    	gl.glVertex3f(points[i], points[i+1], points[i+2]);
	    	gl.glVertex3f(points[i+3], points[i+4], points[i+5]);
	    }
	    gl.glEnd();
	    if (dashed) {
	    	gl.glDisable(GL2.GL_LINE_STIPPLE);
	    }
	}
	
	void drawLineLoop(GL2 gl, float[] points, int width, float[] color)
	{
	    gl.glColor4f(color[0], color[1], color[2], getDisplayAlpha());
	    if (dashed) {
	    	gl.glEnable(GL2.GL_LINE_STIPPLE);
	    	gl.glLineStipple(multFactor, dashPattern);
	    }
	    gl.glLineWidth(width);
	    gl.glBegin(GL2.GL_LINE_LOOP);
	    for (int i=0; i<points.length; i+=3) {
	    	gl.glVertex3f(points[i], points[i+1], points[i+2]);
	    }
	    gl.glEnd();
	    if (dashed) {
	    	gl.glDisable(GL2.GL_LINE_STIPPLE);
	    }
	}

	/**
	 * Retrieves the current color of the OutLine
	 *
	 * @return color as a float[]
	 * @throws
	 *
	 * thread-safe
	 */
	public float[] getColor() {
		return color;
	}

	/**
	 * Sets the color of the OutLine
	 *
	 * @param color
	 * @throws
	 *
	 * thread-safe
	 */
	public void setColor(float[] color) {
		this.color = color;
	}

	/**
	 * Retrieves the points that defines the OutLine
	 *
	 * @return points as a float[]
	 *
	 * thread-safe
	 */
	public float[] getPointsSequential () {
		return points;
	}

	/**
	 * Sets the points that define the OutLine
	 *
	 * @param points
	 *
	 * thread-safe
	 */
	public void setPoints(float[] points) {
		this.points = new float[points.length];
//		for (int i=0; i<points.length; i++) {
//			this.points[i] = points[i] * scaleFactor;
//		}
	}

	/**
	 * Retrieves the width of the line
	 *
	 * @return width as an int
	 *
	 * thread-safe
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Sets the width of the line
	 *
	 * @param width
	 *
	 * not thread-safe
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * Retrieves whether the line is configured as a dashed line
	 *
	 * @return true if this line is dashed
	 *
	 * thread-safe
	 */
	public boolean isDashed() {
		return dashed;
	}

	/** 
	 * Sets whether the line should be rendered as a dashed line
	 *
	 * @param dashed rendered as a dashed line if true
	 *
	 * thread-safe
	 */
	public void setDashed(boolean dashed) {
		this.dashed = dashed;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#delete(com.jogamp.opengl.GL2)
	 */
	@Override
	public void delete(GL2 gl) {
		dispose();		
	}

	/**
	 * Retrieves the OutLine Type
	 *
	 * @return OutLineType
	 *
	 * thread-safe
	 */
	public OutLineType getOutLineType() {
		return type;
	}

	/**
	 * Sets the points of the polygon that have been fit to a shape model
	 *
	 * @param data
	 *
	 * thread-safe
	 */
	public void setFittedPoints(float[] data) {
		this.fittedPoints = data;
		if (this.scaleFactor > 1.0f) {
			for (int i=0; i<this.fittedPoints.length; i++) {
				this.fittedPoints[i] = this.fittedPoints[i] * scaleFactor;
			}
		}		
		fitted = true;
		beingFitted = false;
	}
	
	/**
	 * Retrieves whether the line has been fit to a shape model
	 *
	 * @return true if fitted
	 *
	 * thread-safe
	 */
	public boolean isFitted() {
		return fitted;
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
	
	/**
	 * Method for the encapsulating code to enable/disable fitting the outline to the shape model if one exists
	 *
	 * @param fittingEnabled
	 *
	 * thread-safe
	 */
	public void setFittingEnabled(boolean fittingEnabled) {
		this.fittingEnabled = fittingEnabled;
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
		
		for (int i=0; i<points.length; i++) {
			points[i] *= scaleFactor;
		}
		if (fittedPoints != null && fittedPoints.length > 0) {
			for (int i=0; i<fittedPoints.length; i++) {
				fittedPoints[i] *= scaleFactor;
			}
		}
		hasBeenScaled = true;
	}
	
	/**
	 * Override the default alpha value.  Default is 0.5f.
	 * @param f the new alpha value
	 */
	public void setAlpha(float f){
		alpha = f;
	}

	@Override
	public void scaleToShapeModel(boolean canScale) {
		isScalable = canScale;		
	}

	@Override
	public boolean isScaled() {
		return hasBeenScaled;
	}
	
	public Object getIdNumber() {
		return id;
	}

	public boolean isBeingFitted() {
		return beingFitted;
	}

	public void setBeingFitted(boolean beingFitted) {
		this.beingFitted = beingFitted;
	}

	public boolean isFittingEnabled() {
		return fittingEnabled;
	}

}
