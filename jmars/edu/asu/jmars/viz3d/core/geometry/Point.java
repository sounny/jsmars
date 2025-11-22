package edu.asu.jmars.viz3d.core.geometry;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.math.geom.Frustum;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.viz3d.core.math.Util3D;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;
import edu.asu.jmars.viz3d.spatial.IntersectResultData;
import edu.asu.jmars.viz3d.spatial.Node;
import edu.asu.jmars.viz3d.spatial.RayWithEpsilon;
import edu.asu.jmars.viz3d.spatial.SpatialRenderable;

/**
 * 3D representation of a point.
 */
public class Point implements GLRenderable, SpatialRenderable {
	
	float[] point = new float[3];
	float[] color = new float[] {1f, 0f, 0f};
	Node<?> node;
	private boolean isScalable = true;
	private boolean hasBeenScaled = false;
	private float alpha = 1.0f;
    private Float displayAlpha;
	private float size = 1f;
	private static DebugLog log = DebugLog.instance();

	
    public Point(float[] point, float[] color, float size, float opacity) throws IllegalArgumentException {
		if (point == null || point.length != 3) {
			log.aprintln("Invalid point");
			throw new IllegalArgumentException("Invalid point for Point constructor");
		}
		if (color == null || color.length != 3) {
			log.aprintln("Invalid color vector");
			throw new IllegalArgumentException("Invalid color vector for Point constructor");
		}
		if (size < 0f) {
			log.aprintln("Invalid magnitude: "+ size);
			throw new IllegalArgumentException("Invalid magnitude for Point constructor: "+ size + " should be > zero.");
		}
		
		this.point = point;
		this.color = color;
		this.size = size;
		alpha = opacity;
    	
    }
    
   	@Override
	public float[][] getPoints() {
		return new float[][] {point};
	}

	@Override
	public float[] getCenter() {
		return new float[] {point[0], point[1], point[2]};
	}

	@Override
	public float getRadius() {
		return 0;
	}

	public float[] getIntersection() {
		return new float[] {point[0], point[1], point[2]};
	}

	public float[] getColor() {
		return color;
	}

	@Override
	public void setColor(float[] color) {
		if (color != null && color.length == 3) {
			this.color = color;
		}		
	}

	@SuppressWarnings("unchecked")
	@Override
	public Node<SpatialRenderable> getNode() {
		return (Node<SpatialRenderable>) node;
	}

	@Override
	public void setNode(Node<?> t) {
		if (t instanceof Node<?>) {
			node = t;
		}
		
	}

	@Override
	public void render(GL2 gl) {
	    gl.glColor4f(color[0], color[1], color[2], getDisplayAlpha());
	    gl.glPointSize(size);
		gl.glEnable(GL2.GL_POINT_SMOOTH);
	    gl.glBegin(GL2.GL_POINTS);
	    	gl.glVertex3f(point[0], point[1], point[2]);
	    gl.glEnd();
	}

	@Override
	public IntersectResultData<SpatialRenderable> intersectRay(RayWithEpsilon ray) {
		IntersectResultData<SpatialRenderable> result = null;
		float[] org = ray.getOrigin();
		float[] end = ray.getEnd();
		
		double dist = Util3D.distanceFromPointToLine3D(Util3D.floatToDoubleArray(point), Util3D.floatToDoubleArray(org), Util3D.floatToDoubleArray(end));
		
		if ((float)dist <= ray.getEpsilon()) {
			result = new IntersectResultData<SpatialRenderable>();
			
			result.intersectDistance = (float)dist;
			result.distanceToRayOrigin = VectorUtil.distVec3(org, point);
			if (result.intersectionPoint == null) {
				result.intersectionPoint = new float[3];
			}
			for (int i=0; i<3; i++) {
				result.intersectionPoint[i] = point[i];
			}
			result.object = this;
		}
		return result;
	}

	@Override
	public boolean intersectFrustum(float[] normal, float[][] frustum) {
		return Util3D.pointInFrustum(point, frustum);
	}

	@Override
	public boolean intersectJoglFrustum(float[] normal, Frustum frustum) {
		Frustum.Location loc = frustum.classifyPoint(point);
		if (loc == Frustum.Location.INSIDE) {
			return true;
		}
		return false;
	}

	@Override
	public void execute(GL2 gl) {
		render(gl);		
	}

	@Override
	public void preRender(GL2 gl) {
		// TODO Auto-generated method stub
		
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
		if(displayAlpha == null){
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
		if (point != null && point.length == 3 && node == null) {
			return true;
		} else {
			return false;
		}
	}

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
			point = VectorUtil.scaleVec3(new float[3], point, scaleFactor);
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
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void finalize() {
		point = null;
		color = null;
		node = null;;
		displayAlpha = null;

	}

}
