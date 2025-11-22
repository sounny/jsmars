/**
 * 
 */
package edu.asu.jmars.viz3d.renderer.gl.lines;

import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.viz3d.Disposable;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;

/**
 * This class represents a directed arrow to be drawn in 3D
 *
 * Arrow start and end points, line width and color, and arrowhead length are all user configurable
 *
 * no dependencies
 *
 * not thread safe
 */
public class Arrow implements GLRenderable, Disposable {

    private GLUquadric quadric = null;
    private GLUquadric diskQuadric = null;
	private GLU glu = new GLU();
	private int lineWidth = 1;
	private int multFactor = 1;
	private float lineLength;
	private float adjustedLineLength;
	private float arrowheadLength;
	private float[] start;
	private float[] end;
	private float[] lineEnd;
	private float[] color;
    private static DebugLog log = DebugLog.instance();
    private float ARROWHEAD_BASE_LENGTH_RATIO = 0.25f;
    private boolean preRendered = false;
    private boolean isScalable = true;
    private boolean hasBeenScaled = false;
	private boolean dashed = false;


    private static float radToDeg = 180f / FloatUtil.PI;
    
    private static final int SLICES = 8;
    private static final int STACKS = 2;
    
	public static final short SOLID_DASH = (short)0xFFFF;
	public static final short BIG_DASH = 0x00FF;
	public static final short SMALL_DASH = 0x0AAA;
	public static final short ALTERNATING_DASH = 0x0C0F;
	
	short dashPattern = BIG_DASH;

    
    private float alpha = 1f;
    private Float displayAlpha;
    
    /**
     * Constructor
     * @param start start or base of the arrow in 3D (x,y,z)
     * @param end end or head of the arrow (x,y,z)
     * @param lineWidth
     * @param color color vector (R, G, B, A) with each color band and opacity independently normalized
     * @param arrowheadLength
     */
    public Arrow(float[] start, float[] end, int lineWidth, float[] color, float arrowheadLength) throws IllegalArgumentException {  
    	if (start == null || start.length != 3) {
    		log.aprint("Invalid start vector for 3D Arrow constructor");
    		throw new IllegalArgumentException("Invalid start vector for 3D Arrow constructor");
    	}
    	if (end == null || end.length != 3) {
    		log.aprint("Invalid end vector for 3D Arrow constructor");
    		throw new IllegalArgumentException("Invalid end vector for 3D Arrow constructor");
    	}
    	if (color == null || color.length != 4) {
    		log.aprint("Invalid color vector for 3D Arrow constructor");
    		throw new IllegalArgumentException("Invalid color vector for 3D Arrow constructor");
    	}
        this.start = start;
        this.end = end;
        this.lineWidth = lineWidth;
        this.color = color;
        this.arrowheadLength = arrowheadLength;
        dashed = false;
        calculateNewLineEnd();
    }

    /**
     * 
     * @param start  start or base of the arrow in 3D (x,y,z)
     * @param end end or head of the arrow (x,y,z)
     * @param lineWidth
     * @param color color vector (R, G, B, A) with each color band and opacity independently normalized
     * @param arrowheadLength
     * @param dashPattern Sets the current dash pattern for lines. The pattern argument is a 16-bit series of 0s and 1s, and it's repeated as necessary 
	 * 		to stipple a given line. A 1 indicates that drawing occurs, and 0 that it does not, on a pixel-by-pixel basis, beginning with the low-order bit 
	 * 		of the pattern.
     * @param multFactor multFactor The dash pattern can be stretched out by using multFactor, which multiplies each sub-series of consecutive 1s and 0s. Thus, if three 
	 * 		consecutive 1s appear in the pattern, they're stretched to six if factor is 2. multFactor is clamped to lie between 1 and 255.
     * @throws IllegalArgumentException
     */
    public Arrow(float[] start, float[] end, int lineWidth, float[] color, float arrowheadLength, short dashPattern, int multFactor) throws IllegalArgumentException {  
    	if (start == null || start.length != 3) {
    		log.aprint("Invalid start vector for 3D Arrow constructor");
    		throw new IllegalArgumentException("Invalid start vector for 3D Arrow constructor");
    	}
    	if (end == null || end.length != 3) {
    		log.aprint("Invalid end vector for 3D Arrow constructor");
    		throw new IllegalArgumentException("Invalid end vector for 3D Arrow constructor");
    	}
    	if (color == null || color.length != 4) {
    		log.aprint("Invalid color vector for 3D Arrow constructor");
    		throw new IllegalArgumentException("Invalid color vector for 3D Arrow constructor");
    	}
    	if (dashPattern != SOLID_DASH && dashPattern != BIG_DASH && dashPattern != SMALL_DASH && dashPattern != ALTERNATING_DASH) {
    		log.aprint("Invalid dash pattern for 3D Arrow contructor");
    		throw new IllegalArgumentException("Invalid dash pattern for 3D Arrow constructor");
    	}
        this.start = start;
        this.end = end;
        this.lineWidth = lineWidth;
        this.color = color;
        this.arrowheadLength = arrowheadLength;
        this.dashPattern = dashPattern;
        this.multFactor = multFactor;
        dashed = true;
        calculateNewLineEnd();
    }
    
    private void calculateNewLineEnd() {
    	// move the line to the origin
        float[] diff = new float[3];
        diff = VectorUtil.subVec3(diff, end, start);
        lineLength = VectorUtil.normVec3(diff);
        adjustedLineLength = lineLength - (arrowheadLength * 0.9f);
        float tempScale = adjustedLineLength / lineLength;
        float[] newEnd = new float[3];
        newEnd = VectorUtil.scaleVec3(newEnd, diff, tempScale);
        lineEnd = new float[3];
    	lineEnd = VectorUtil.addVec3(lineEnd, newEnd, start);
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
        float[] diff = new float[3];
        diff = VectorUtil.subVec3(diff, end, start);
        float angle = VectorUtil.angleVec3(diff, new float[]{0f, 0f, 1f}) * radToDeg;
        float[] axis = new float[3];
        axis = VectorUtil.crossVec3(axis, new float[]{0f, 0f, 1f}, diff);

        this.drawLine(gl, start, lineEnd, lineWidth, color); // z axis
        
        // calculate distance to translate the arrowhead
        // we want the tip to be co-incident(sp) with the end of the line
        // mag of the line
        float headDistance = lineLength - arrowheadLength;
        float headScale = (headDistance / lineLength);
        diff = VectorUtil.scaleVec3(diff, diff, headScale);
        float[] transVector = new float[3];
        transVector = VectorUtil.addVec3(transVector, diff, start);
        gl.glPushMatrix();
        gl.glTranslatef(transVector[0], transVector[1], transVector[2]);   // position the cone
        gl.glRotatef(angle, axis[0], axis[1], axis[2]);	// rotate the cone to align with the line
        
       // draw the cone 
        // (GLUquadric, base, top, height, #slices, #stacks)
        glu.gluCylinder(quadric, arrowheadLength * ARROWHEAD_BASE_LENGTH_RATIO, 0.0f, arrowheadLength, SLICES, STACKS); 
        glu.gluDisk(diskQuadric, 0, arrowheadLength * ARROWHEAD_BASE_LENGTH_RATIO, SLICES, STACKS);
        gl.glPopMatrix();
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#preRender(com.jogamp.opengl.GL2)
	 */
	@Override
	public void preRender(GL2 gl) {
		if (preRendered) {
			return;
		}
		// Create A Pointer To The Cone Quadric Object
        quadric = glu.gluNewQuadric();
        if (quadric == null) {
    		log.aprint("Could not create arrowhead - out of memory!");
        	return;
        }
        glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH);  // Create Smooth Normals
        glu.gluQuadricTexture(quadric, true);            // Create Texture Coords   
        
		// Create A Pointer To The Disk Quadric Object
        diskQuadric = glu.gluNewQuadric();
        if (diskQuadric == null) {
    		log.aprint("Could not create arrowhead base - out of memory!");
        	return;
        }
        glu.gluQuadricNormals(diskQuadric, GLU.GLU_SMOOTH);  // Create Smooth Normals
        glu.gluQuadricTexture(diskQuadric, true);            // Create Texture Coords   
        glu.gluQuadricOrientation(diskQuadric, GLU.GLU_INSIDE);
        preRendered = true;

	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#postRender(com.jogamp.opengl.GL2)
	 */
	@Override
	public void postRender(GL2 gl) {
	}

	void drawLine(GL2 gl, float[] a, float[] b, int width, float[] color)
	{
	    gl.glColor4f(color[0], color[1], color[2], getDisplayAlpha());
	    if (dashed) {
	    	gl.glEnable(GL2.GL_LINE_STIPPLE);
	    	gl.glLineStipple(multFactor, dashPattern);
	    } else {
	    	gl.glDisable(GL2.GL_LINE_STIPPLE);	    	
	    }
	    gl.glLineWidth(width);
	    gl.glBegin(GL2.GL_LINES);
	    gl.glVertex3f(a[0], a[1], a[2]);
	    gl.glVertex3f(b[0], b[1], b[2]);
	    gl.glEnd();
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
		lineLength *= scaleFactor;
		arrowheadLength *= scaleFactor;
		start = VectorUtil.scaleVec3(new float[3], start, scaleFactor);
		end = VectorUtil.scaleVec3(new float[3], end, scaleFactor);
		lineEnd = VectorUtil.scaleVec3(new float[3], lineEnd, scaleFactor);
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
