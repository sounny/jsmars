package edu.asu.jmars.viz3d.core.geometry;

import java.util.ArrayList;
import java.util.Arrays;

import com.badlogic.gdx.math.EarClippingTriangulator;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.math.geom.Frustum;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.viz3d.Disposable;
import edu.asu.jmars.viz3d.ThreeDException;
import edu.asu.jmars.viz3d.core.math.Util3D;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;
import edu.asu.jmars.viz3d.renderer.gl.PolygonType;
import edu.asu.jmars.viz3d.renderer.gl.event.IntersectResult;
import edu.asu.jmars.viz3d.renderer.gl.outlines.OutLine;
import edu.asu.jmars.viz3d.spatial.IntersectResultData;
import edu.asu.jmars.viz3d.spatial.Node;
import edu.asu.jmars.viz3d.spatial.RayWithEpsilon;
import edu.asu.jmars.viz3d.spatial.SpatialRenderable;
import edu.asu.jmars.viz3d.util.Utils;

/**
 * This class represents a filled polygon including a an outline to be drawn in 3D.
 * The only polygons that are currently supported are closed, convex polygons.
 *
 * not thread safe
 */
public class Polygon extends OutLine implements GLRenderable, Disposable, SpatialRenderable {
	
	private float[] fillColor;
	private float[] orgColor = new float[4];
	float[] polyPoints;
	private float[] fittedPoints;
//	private float polyScale = 1.001f;
	private float polyScale = 1.005f;
	private boolean drawOutline = true;
	private boolean useIndices = false;
	PolygonType type;
	private boolean fitted = false;
	private boolean fittingEnabled = false;
	private boolean beingFitted = false;
	private boolean isScalable = true;
	private boolean hasBeenScaled = false;
	private float alpha = 1.0f;
    private Float displayAlpha;
	SpatialRenderable next;
	Node<?> node;
	float[] intersection;
	float[] center;
	boolean isClosed = false;
	int[] indices;
	final float MAX_ANGULAR_EXTENT = 12;


    private static DebugLog log = DebugLog.instance();


	/**
	 * @param idNumber not currently used, can be any integral value
	 * @param points redundant at this point will be eliminated in a future release
	 * @param lineColor color of the outline or border
	 * @param fillColor color of the filled region of the polygon
	 * @param lineWidth width of the outline or border
	 * @param opacity only floating point numbers from 0 to 1 are allowed, 0 = invisible, 1 = fully opaque
	 */
	public Polygon(Object idNumber, float[] points, float[] lineColor, float[] fillColor,
			int lineWidth,  float alpha) {
		this(idNumber, points, lineColor, fillColor, lineWidth, alpha, false);
	}
	/**
	 * @param idNumber not currently used, can be any integral value
	 * @param points redundant at this point will be eliminated in a future release
	 * @param lineColor color of the outline or border
	 * @param fillColor color of the filled region of the polygon
	 * @param lineWidth width of the outline or border
	 * @param opacity only floating point numbers from 0 to 1 are allowed, 0 = invisible, 1 = fully opaque
	 * @param drawOutline if <code>true</code> draws the polygon's perimeter
	 */
	public Polygon(Object idNumber, float[] points, float[] lineColor, float[] fillColor,
			int lineWidth, float alpha, boolean drawOutline) {
		super(idNumber, points, lineColor, lineWidth, true);
		
		if (points == null || points.length < 9) { // we need at lease 3 points (3x3 vertices) to create a polygon
			log.aprintln("Cannot create a polygon with less than 3 points...");
			return;
		} else {
			this.drawOutline = drawOutline;
		}
		
		if (VectorUtil.isVec3Equal(points, 0, points, points.length - 3, FloatUtil.EPSILON)) {
			isClosed = true;
		}
		
		this.fillColor = fillColor;
		this.alpha = alpha;
		this.polyScale = (polyScale < 0f) ? 1f : polyScale;
		this.polyPoints = new float[points.length];
		for (int i=0; i<points.length; i++) {
			this.polyPoints[i] = points[i]  / polyScale;
		}
	}
	
	/**
	 * 
	 * @param idNumber
	 * @param points redundant at this point will be eliminated in a future release
	 * @param lineColor color of the outline or border
	 * @param fillColor color of the filled region of the polygon
	 * @param lineWidth width of the outline or border
	 * @param opacity only floating point numbers from 0 to 1 are allowed, 0 = invisible, 1 = fully opaque
	 * @param drawOutline if <code>true</code> draws the polygon's perimeter
	 * @param onBody if <code>true</code> fits the polygon to the associated shape model 
	 * @param indices
	 */
	public Polygon(Object idNumber, float[] points, float[] lineColor, float[] fillColor,
			int lineWidth, float alpha, boolean drawOutline, boolean onBody, int[] indices) {
		
		this(idNumber, points, lineColor, fillColor,
				lineWidth, alpha, drawOutline, onBody);
		
		if (indices == null || indices.length < 3) {
			log.aprintln("Cannot create a polygon with less than 3 indices...");
			return;
		} else {
			this.indices = indices;
			useIndices = true;
		}
	}

	/**
	 * @param points redundant at this point will be eliminated in a future release
	 * @param lineColor color of the outline or border
	 * @param fillColor color of the filled region of the polygon
	 * @param lineWidth width of the outline or border
	 * @param opacity only floating point numbers from 0 to 1 are allowed, 0 = invisible, 1 = fully opaque
	 * @param drawOutline if <code>true</code> draws the polygon's perimeter
	 * @param onBody if <code>true</code> fits the polygon to the associated shape model 
	 */
	public Polygon(Object idNumber, float[] points, float[] lineColor, float[] fillColor,
			int lineWidth, float alpha, boolean drawOutline, boolean onBody) {
		super(idNumber, points, lineColor, lineWidth, true);
			
		if (points == null || points.length < 9) { // we need at lease 3 points (3x3 vertices) to create a polygon
			log.aprintln("Cannot create a polygon with less than 3 points...");
			return;
		} else {
			this.drawOutline = drawOutline;
		}

		if (onBody) {
			type = PolygonType.OnBody;
		} else {
			type = PolygonType.OffBody;
		}
		this.fillColor = fillColor;
		this.alpha = alpha;
		// need to check if the polygon is closed - the start and end points have the same values
		// if the polygon is closed mark it as such
		if (VectorUtil.isVec3Equal(points, 0, points, points.length - 3, FloatUtil.EPSILON)) {
			isClosed = true;
			this.polyPoints = new float[points.length];
		} else {
			this.polyPoints = new float[points.length + 3];
		}
		if (!onBody) {
			// these will be applied later after the polygon has been fit to the body
			for (int i=0; i<points.length; i++) {
				this.polyPoints[i] = points[i] / polyScale;
//				this.polyPoints[i] = points[i];
			}
			// if the polygon is not closed add the start point to the end to close it
			if (!isClosed) {
				this.polyPoints[polyPoints.length - 3] = points[0] / polyScale;
				this.polyPoints[polyPoints.length - 2] = points[1] / polyScale;
				this.polyPoints[polyPoints.length - 1] = points[2] / polyScale;				
//				this.polyPoints[polyPoints.length - 3] = points[0];
//				this.polyPoints[polyPoints.length - 2] = points[1];
//				this.polyPoints[polyPoints.length - 1] = points[2];				
			}
		} else {
			for (int i=0; i<points.length; i++) {
				this.polyPoints[i] = points[i];
			}
			// if the polygon is not closed add the start point to the end to close it
			if (!isClosed) {
				this.polyPoints[polyPoints.length - 3] = points[0];
				this.polyPoints[polyPoints.length - 2] = points[1];
				this.polyPoints[polyPoints.length - 1] = points[2];				
			}
		}
		// TODO shouldn't this be moved up to the else block above?? WH
		if (polyScale > 1f) {
			for (int i=0; i< polyPoints.length; i++) {
				polyPoints[i] *= polyScale;
			}
		}
	}

	/**
	 * Reset the fill color for this polygon to a new value
	 * @param newColor
	 */
	public void setFillColor(float[] newColor) {
		fillColor = newColor;
	}
	
	@Override
	public void setColor(float[] color) {
		if (color != null && color.length == 3) {
			this.setFillColor(color);
		}
		
	}
	
	private void drawFilledPolygon(GL2 gl, float[] pts, float[] color, float opacity) {
		gl.glGetFloatv(GL2.GL_CURRENT_COLOR, orgColor, 0);
	    gl.glColor4f(fillColor[0], fillColor[1], fillColor[2], opacity);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
	    gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
	    gl.glEnable(GL2.GL_POLYGON_OFFSET_LINE);
	    gl.glPolygonOffset(-1f,-1f);	    
	    gl.glBegin(GL2.GL_POLYGON);
	    for (int i=0; i<pts.length; i+=3) {
	    	gl.glVertex3f(pts[i], pts[i+1], pts[i+2]);
	    }
	    gl.glEnd();
	    gl.glColor4f(orgColor[0], orgColor[1], orgColor[2], orgColor[3]);
	}
	
	private void drawFittedPolygon(GL2 gl, float[] pts, float[] color, float opacity) {
		gl.glGetFloatv(GL2.GL_CURRENT_COLOR, orgColor, 0);
	    gl.glColor4f(fillColor[0], fillColor[1], fillColor[2], opacity);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
	    gl.glBegin(GL2.GL_TRIANGLES);
	    for (int i=0; i<pts.length; i+=3) {
	    	gl.glVertex3f(pts[i], pts[i+1], pts[i+2]);
	    }
	    gl.glEnd();
	    gl.glColor4f(orgColor[0], orgColor[1], orgColor[2], orgColor[3]);
	}
	
	private void drawIndexedPolygon(GL2 gl, float[] pts, int[] indices, float[] color, float opacity) {
		gl.glGetFloatv(GL2.GL_CURRENT_COLOR, orgColor, 0);
	    gl.glColor4f(fillColor[0], fillColor[1], fillColor[2], opacity);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
	    gl.glBegin(GL2.GL_TRIANGLES);
	    for (int i=0; i<indices.length; i++) {
	    	gl.glVertex3f(pts[indices[i]], pts[indices[i]+1], pts[indices[i]+2]);
	    }
	    gl.glEnd();
	    gl.glColor4f(orgColor[0], orgColor[1], orgColor[2], orgColor[3]);
	}

	/**
	 * Method to set vertices of the polygon after it has been fit to an existing shape model
	 *
	 * @param data object containing the triangular tessellation results of fitting the polygon to a shape model
	 *
	 * not thread-safe
	 */
	public void setFittedData(FittedPolygonData data) {
		int i = 0;
		float[] newVerts = new float[data.getTris().size() * 9];
		// map the vertices in one triangle at a time
		for (Triangle t : data.getTris()) {
			// first point
			newVerts[i++] = t.points[0][0];
			newVerts[i++] = t.points[0][1];
			newVerts[i++] = t.points[0][2];
			// second point
			newVerts[i++] = t.points[1][0];
			newVerts[i++] = t.points[1][1];
			newVerts[i++] = t.points[1][2];
			// third point
			newVerts[i++] = t.points[2][0];
			newVerts[i++] = t.points[2][1];
			newVerts[i++] = t.points[2][2];
		}
		
		fittedPoints = newVerts;
		// apply the fitted outline data to the outline of the polygon
		i = 0;
		float[] newLine = new float[data.getPoints().size() * 3];
		for (float[] f : data.getPoints()) {
			newLine[i++] = f[0];
			newLine[i++] = f[1];
			newLine[i++] = f[1];
		}
		super.setFittedPoints(newLine);
		
		fitted = true;
		beingFitted = false;
	}

	/**
	 * Method to inform whether the polygon has been fitted to a shape model
	 */
	public boolean isFitted() {
		return fitted;
	}
	
	/**
	 * Method to enable or disable the display of the polygon outline.
	 *
	 * @param display the outline will be displayed if set to true (default).
	 *
	 * This method may even be thread safe
	 */
	public void displayOutline(boolean display) {
		drawOutline = display;		
	}
	
	/**
	 * Method for user of the class to determine if the polygon outline will be
	 * drawn when rendered.
	 *
	 * @return true if the outline will be drawn
	 *
	 * Thread safe
	 */
	public boolean isOutlineDisplayed() {
		return drawOutline;
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
		if (polyPoints == null || polyPoints.length < 9) { // we need at lease 3 points (3x3 vertices) to create a polygon
			return;
		}

		if (useIndices) {
			drawIndexedPolygon(gl, this.polyPoints, indices, fillColor, getDisplayAlpha()*alpha);
		} else if (fitted && fittingEnabled) {
			drawFittedPolygon(gl, this.fittedPoints, fillColor, getDisplayAlpha()*alpha);
		} else {
			drawFilledPolygon(gl, this.polyPoints, fillColor, getDisplayAlpha()*alpha);
		}
		if (drawOutline) {
			super.execute(gl);
		}
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
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#getAlpha(com.jogamp.opengl.GL2)
	 */
	@Override
	public float getAlpha() {
		return alpha;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#getDisplayAlpha(com.jogamp.opengl.GL2)
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
	 * Method to return the PolygonType
	 *
	 * @return the PolygonType
	 *
	 * thread-safe
	 */
	public PolygonType getPolygonType() {
		return type;
	}
	
	/**
	 * Method to return whether the polygon will be fit to the surface of the shape model if if one exists
	 *
	 * @return true of the polygon will be fit to the shape model when rendered
	 *
	 * thread-safe
	 */
	public boolean isFittingEnabled() {
		return fittingEnabled;
	}
	
	/**
	 * Method for the encapsulating code to enable/disable fitting the polygon to the shape model if one exists
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

	@Override
	public void scaleToShapeModel(boolean canScale) {
		isScalable = canScale;		
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
		for (int i=0; i<polyPoints.length; i++) {
			polyPoints[i] *= scaleFactor;
		}
		if (fittedPoints != null && fittedPoints.length > 0) {
			for (int i=0; i<fittedPoints.length; i++) {
				fittedPoints[i] *= scaleFactor;
			}
		}
		hasBeenScaled = true;
		if (drawOutline) {
			super.scaleByDivision(scalar);
		}
	}
	
	@Override
	public boolean isScaled() {
		return hasBeenScaled;
	}

	public float[] getOrigPoints() {
		return polyPoints;
	}
	public boolean isBeingFitted() {
		return beingFitted;
	}
	public void setBeingFitted(boolean beingFitted) {
		this.beingFitted = beingFitted;
	}
	@Override
	public float[][] getPoints() {
		float[] pts = polyPoints;
		if (pts != null && pts.length >= 9) {
			return Util3D.float1DTo2DArray(pts, 3);
		}
		return null;
	}
	
	@Override
	public float[] getCenter() {
		if (center != null) {
			return center;
		}
		float[][] pts = getPoints();
		if (pts != null && pts.length >= 3) {		
			return Util3D.avgOf3DPolygon(pts);
		}
		return null;
	}
	@Override
	public float getRadius() {
		float[] center = getCenter();
		if (center == null) {
			return 0;
		}
		return VectorUtil.normVec3(center);
	}
	
	public Object getIdNumber() {
		return id;
	}

	public SpatialRenderable getNext() {
		return next;
	}

	public void setNext(SpatialRenderable t) {
		if (t instanceof Polygon) {
			next = t;	
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	public Node<SpatialRenderable> getNode() {
		return (Node<SpatialRenderable>) node;
	}
	@Override
	public void setNode(Node<?> t) {
		node = t;
	}
	@Override
	public void render(GL2 gl) {
		execute(gl);		
	}
	@Override
	public IntersectResultData<SpatialRenderable> intersectRay(RayWithEpsilon ray) {
		IntersectResultData<SpatialRenderable> result = null;	
		
		float[] dir = ray.getDirection();
		float[] reverseDir = new float[3];
	    for (int i=0; i<3; i++) {		    	
	    	reverseDir = new float[] {-dir[0], -dir[1], -dir[2]};
	    }	    
	    float[] center = this.getCenter();
	    
	    float dot = VectorUtil.dotVec3(VectorUtil.normalizeVec3(reverseDir), VectorUtil.normalizeVec3(new float[3], center));
	    
	    if (dot < 0f) {
	    	return result;
	    }

		
		float[] intersection = new float[3];
		float points[][] = new float[3][];
		if (useIndices) {
			float[] pts = this.polyPoints;
		    for (int i=0; i<3; i++) {		    	
		    	points[i] = new float[] {pts[indices[i]], pts[indices[i]+1], pts[indices[i]+2]};
		    }
		} else {
			points = getPoints();
		}
		
		if (Util3D.rayTriangleIntersect(ray.getOrigin(), ray.getDirection(), points[0], points[1], points[2], intersection, 1f) != Float.MAX_VALUE) {
			result = new IntersectResultData<SpatialRenderable>();
			result.intersectionPoint = new float[3];
			for (int i=0; i<3; i++) {
				result.intersectionPoint[i] = intersection[i];
			}
			result.distanceToRayOrigin = VectorUtil.distVec3(ray.getOrigin(), intersection);
			result.intersectDistance = 0;
			result.object = this;
			return result;
		} else {
			// we need to test the edges against the epsilon
			float[][] pts = points;
			float[] pa = new float[3];
			float[] pb = new float[3];
			double[] mua = new double[1];
			double[] mub = new double[1];
			if (Util3D.lineLineIntersect(ray.getOrigin(), ray.getEnd(), pts[0], pts[1], pa, pb, mua, mub)) {
			    if (!VectorUtil.isVec3Zero(pa, 0) && !VectorUtil.isVec3Zero(pb, 0) && (VectorUtil.distVec3(pb, pa) <= ray.getEpsilon())) {
			        // We have a possible result
					result = new IntersectResultData<SpatialRenderable>();
					result.intersectionPoint = new float[3];
					for (int i=0; i<3; i++) {
						result.intersectionPoint[i] = pb[i];
					}
					result.distanceToRayOrigin = VectorUtil.distVec3(ray.getOrigin(), pb);
					result.intersectDistance = VectorUtil.distVec3(pb,pa);
					result.object = this;
			        return result;
			    }			    			    
			} 
			
			pa = new float[3];
			pb = new float[3];
			mua = new double[1];
			mub = new double[1];
			
			if (Util3D.lineLineIntersect(ray.getOrigin(), ray.getEnd(), pts[1], pts[2], pa, pb, mua, mub)) {
			    if (!VectorUtil.isVec3Zero(pa, 0) && !VectorUtil.isVec3Zero(pb, 0) && (VectorUtil.distVec3(pb, pa) <= ray.getEpsilon())) {
			        // We have a possible result
					result = new IntersectResultData<SpatialRenderable>();
					result.intersectionPoint = new float[3];
					for (int i=0; i<3; i++) {
						result.intersectionPoint[i] = pb[i];
					}
					result.distanceToRayOrigin = VectorUtil.distVec3(ray.getOrigin(), pb);
					result.intersectDistance = VectorUtil.distVec3(pb,pa);
					result.object = this;
			        return result;
			    }			    			    
			} 

			pa = new float[3];
			pb = new float[3];
			mua = new double[1];
			mub = new double[1];
			
			if (Util3D.lineLineIntersect(ray.getOrigin(), ray.getEnd(), pts[2], pts[0], pa, pb, mua, mub)) {
			    if (!VectorUtil.isVec3Zero(pa, 0) && !VectorUtil.isVec3Zero(pb, 0) && (VectorUtil.distVec3(pb, pa) <= ray.getEpsilon())) {
			        // We have a possible result
					result = new IntersectResultData<SpatialRenderable>();
					result.intersectionPoint = new float[3];
					for (int i=0; i<3; i++) {
						result.intersectionPoint[i] = pb[i];
					}
					result.distanceToRayOrigin = VectorUtil.distVec3(ray.getOrigin(), pb);
					result.intersectDistance = VectorUtil.distVec3(pb,pa);
					result.object = this;
			        return result;
			    }			    			    
			} 
		}
		return result;
	}
	@Override
	public boolean intersectFrustum(float[] normal, float[][] frustum) {
		Visibility vis = Util3D.checkAgainstFrustum(this, frustum);
		if (vis != Visibility.NotVisible) {
			return true;
		}
		return false;
	}
	@Override
	public boolean intersectJoglFrustum(float[] normal, Frustum frustum) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public int[] getIndices() {
		return indices;
	}
	public void setIndices(int[] indices) {
		useIndices = true;
		this.indices = indices;
	}
	public boolean isIndexed() {
		return useIndices;
	}
	public void setIndexed(boolean isIndexed) {
		this.useIndices = isIndexed;
	}
	
	/**
	 * Method to return the Polygon as a list of triangles/facets
	 * instantiated as Polygons
	 * @return ArrayList of Polygons as triangles/facets
	 */
	public ArrayList<Polygon> getFacets() {
		ArrayList<Polygon> shards = new ArrayList<>();
		if (!useIndices) {
			shards.add(this);
			return shards;
		}
		for (int i=0; i<indices.length; i+=3) {
			Polygon p = new Polygon(super.getIdNumber(), points, getColor(), fillColor,
					getWidth(),  alpha);
			p.setIndices(Arrays.copyOfRange(indices, i, i+3));
			shards.add(p);
		}
		return shards;
	}
	public boolean useIndices() {
		return useIndices;
	}
	public void setUseIndices(boolean useIndices) {
		this.useIndices = useIndices;
	}
	
	public ArrayList<Polygon> decimate() throws ThreeDException {
		ArrayList<Polygon> polys = new ArrayList<>();
	
		float[][]pts = getPoints();
		if (pts.length < 3) {
			return polys;
		}
		
		if (pts.length == 3) {
			this.setIndices(new int[] {0, 3, 6});
			polys.add(this);
			return polys;
		}
		
		float[] center = getCenter();
		
		for (float[] p : pts) {
			float ang = VectorUtil.angleVec3(p, center);
			if (ang > MAX_ANGULAR_EXTENT) {
				throw new ThreeDException("Polygon max angular extent cannot exceed "+(MAX_ANGULAR_EXTENT*2)+" degrees. Currently is "+FloatUtil.abs(ang));
			}			
		}
		
		
		float[] cpNorm = VectorUtil.normalizeVec3(new float[3], center);	
		float[][] editPts = null;
		// check to see if the polygon is closed
		if (VectorUtil.isVec3Equal(pts[0], 0, pts[pts.length - 1], 0, FloatUtil.EPSILON)) {
			editPts = new float[pts.length - 1][];
			for (int i=0; i<pts.length - 1; i++) {
				editPts[i] = new float[] {pts[i][Util3D.X], pts[i][Util3D.Y], pts[i][Util3D.Z]};
			}			
		} else {
			editPts = new float[pts.length][];
			for (int i=0; i<pts.length; i++) {
				editPts[i] = new float[] {pts[i][Util3D.X], pts[i][Util3D.Y], pts[i][Util3D.Z]};
			}			
		}
		
        // rotate the polygon so that its surface normal vector and the Z axis are coincident prior to sorting and ear clipping	        		
		// calculate the angle between the planeNormal and the Z axis in radians
		float angle = VectorUtil.angleVec3(cpNorm, OctTree.Z_AXIS);
		// calculate the axis of the needed rotation to put the planeNormal coincident to the Z axis
		float[] rotationAxis = VectorUtil.crossVec3(new float[3], cpNorm, OctTree.Z_AXIS);
		// craete a quaternion
		Quaternion quat = new Quaternion(rotationAxis[0], rotationAxis[1], rotationAxis[2], angle);
		
		float[][] rot3Dpts = new float[editPts.length][];
		// rotate the polygon vectors to be 
		for (int z=0; z<editPts.length; z++) {
			rot3Dpts[z] = quat.rotateVector(new float[3], 0, editPts[z], 0);
		}
    			        		
    	float[][] twoDpts = OctTree.project3DTo2D(rot3Dpts, Util3D.Z);
    			
    	// We are going to assume the polygon points are in CCW winding order 
		EarClippingTriangulator ect = new EarClippingTriangulator();
		int[] indices = ect.computeTrianglesReturnAsInts(OctTree.float2DTo1DArray(twoDpts, 2));

		float[][] newTris = new float[indices.length][];
		for (int x = 0; x < indices.length; x++) {
			newTris[x] = editPts[indices[x]];
		}
		for (int y = 0; y < indices.length; y += 3) {
			int[] index = new int[] {indices[y+2], indices[y+1], indices[y]}; 
			
			

			Triangle t = new Triangle(new float[][] { pts[index[0]], pts[index[1]], pts[index[2]]});
			if (OctTree.isTriangleDegenerate(t.points)) {
				continue;
			}
			
			Polygon tmp = new Polygon(this.id, polyPoints, new float[] {0f, 1f, 1f}, new float[] {1f, 1f, 0f}, 2,  0.7f);
			int[] idx = new int[] {index[0]*3, index[1]*3, index[2]*3};
			tmp.setIndices(idx);
			polys.add(tmp);
			
		}
		
		
		return polys;
	}
	
	public void print() {
		if (useIndices) {
		    for (int i=0; i<indices.length; i++) {
		    	System.out.println(""+polyPoints[indices[i]]+", "+polyPoints[indices[i]+1]+", "+polyPoints[indices[i]+2]);
		    }
		    System.out.println();
		    System.out.println();
		    System.out.println();
		    System.out.println();
		}
		else {
		    for (int i=0; i<polyPoints.length; i+=3) {
		    	System.out.println(""+polyPoints[i]+", "+polyPoints[i+1]+", "+polyPoints[i+2]);
		    }
		    System.out.println();
		    System.out.println();
		    System.out.println();
		    System.out.println();
		}
	}
	
	public void finalize() {
		fillColor = null;
		orgColor = null;
		polyPoints = null;
		fittedPoints = null;
		node = null;
		intersection = null;
		center = null;
		indices = null;
		displayAlpha = null;
	}
}
