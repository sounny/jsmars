package edu.asu.jmars.viz3d.core.geometry;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.math.geom.Frustum;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.viz3d.core.math.Util3D;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;
import edu.asu.jmars.viz3d.renderer.gl.event.IntersectResult;
import edu.asu.jmars.viz3d.spatial.IntersectResultData;
import edu.asu.jmars.viz3d.spatial.Node;
import edu.asu.jmars.viz3d.spatial.RayWithEpsilon;
import edu.asu.jmars.viz3d.spatial.SpatialRenderable;
import edu.asu.jmars.viz3d.util.Utils;

public class LineSegment implements GLRenderable, SpatialRenderable {
    
    protected float[] pts;
    protected int idx;
    float percent = 0f;  
    final Object idNumber;
	SpatialRenderable next;
	String text;
	Node<?> node;
	private boolean isScalable = true;
	protected boolean hasBeenScaled = false;
	private static DebugLog log = DebugLog.instance();
	private float alpha = 1.0f;
    private Float displayAlpha;
	protected float[] color;
	private int lineWidth = 1;


    public LineSegment(float[] points, int index, Object idNumber, float[] color, int lineWidth) {
        if (points == null || points.length - index < 6) {
            throw new IllegalArgumentException("Invalid vertex array for LineSegment");
        }
        if (index < 0 || index > points.length - 6) {
            throw new IllegalArgumentException("Invalid vertex array index for LineSegment");
        }
        
        if (color == null || color.length != 3) {
            throw new IllegalArgumentException("Invalid color for LineSegment");
        }
        
        pts = points;
        idx = index;
        this.idNumber = idNumber;
        this.color = color;
        this.lineWidth = lineWidth;
    }

    public float[] getPts() {
        return pts;
    }

    public int getIdx() {
        return idx;
    }
    
    public float[] startVertex() {
        return new float[] {pts[idx], pts[idx+1], pts[idx+2]};
    }
    
    public float[] endVertex() {
        return new float[] {pts[idx+3], pts[idx+4], pts[idx+5]};
    }
    
    public float getPercent() {
        return percent;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

    public LineSegment cloneWithScalar(float scalar) {
        float[] points = new float[6];
        points[0] = pts[idx] * scalar;
        points[1] = pts[idx+1] * scalar;
        points[2] = pts[idx+2] * scalar;
        points[3] = pts[idx+3] * scalar;
        points[4] = pts[idx+4] * scalar;
        points[5] = pts[idx+5] * scalar;
        LineSegment line = new LineSegment(points, idx, idNumber, color, lineWidth);
        return line;     
    }   

    public IntersectResult getIntersectResult() {
        IntersectResult result = new IntersectResult();
        result.setIndex(idx);
        result.setPercent(percent);
        return result;
    }

	@Override
	public float[][] getPoints() {
		return new float[][] {{pts[idx], pts[idx+1], pts[idx+2]}, {pts[idx+3], pts[idx+4], pts[idx+5]}};
	}

	@Override
	public float[] getCenter() {
		return VectorUtil.midVec3(new float[3], new float[] {pts[idx], pts[idx+1], pts[idx+2]}, new float[] {pts[idx+3], pts[idx+4], pts[idx+5]});
	}

	@Override
	public float getRadius() {
		return VectorUtil.distVec3(new float[] {pts[idx], pts[idx+1], pts[idx+2]}, new float[] {pts[idx+3], pts[idx+4], pts[idx+5]});
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
		if (pts == null || color == null || idx < 0) {
			log.aprintln("Cannot render invalid LineSegment");
			return;
		}
		// TODO probably will be better to call getColor(), getLineWidth() instead of referring to color state variables
		gl.glColor4f(color[0], color[1], color[2], getDisplayAlpha());
	    gl.glLineWidth(lineWidth);
		gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(pts[idx], pts[idx+1], pts[idx+2]);
        gl.glVertex3f(pts[idx+3], pts[idx+4], pts[idx+5]);
		gl.glEnd();
		gl.glColor3f(1f, 1f, 1f);
	}

//	@Override
//	public boolean intersectRay(RayWithEpsilon ray, float[] intersection) {
//		Vector3D resultPt1 = new Vector3D();
//		Vector3D resultPt2 = new Vector3D();
//		Vector3D lineStart = new Vector3D(startVertex());
//		Vector3D lineEnd = new Vector3D(endVertex());
//		if (Util3D.calculateLineLineIntersection(new Vector3D(ray.getOrigin()[0], ray.getOrigin()[1], ray.getOrigin()[2]) , 
//				new Vector3D(ray.getEnd()[0], ray.getEnd()[1], ray.getEnd()[2]), 
//		        lineStart, lineEnd, resultPt1, resultPt2)) {
//		    float[] r1 = resultPt1.toFloatArray();
//		    float[] r2 = resultPt2.toFloatArray();
//System.err.println("Match! "+(VectorUtil.distVec3(r2, r1) < ray.getEpsilon()));		    
//		    if (!VectorUtil.isVec3Zero(r1, 0) && !VectorUtil.isVec3Zero(r2, 0) && (VectorUtil.distVec3(r2, r1) < ray.getEpsilon())) {
//System.err.println("Full Result!");		    	
//		        // We have a possible result
//		        float[] p1 = lineStart.toFloatArray();
//		        float[] p2 = lineEnd.toFloatArray();
//		        float total  = VectorUtil.distSquareVec3(p1, p2);
//		        float toPt = VectorUtil.distSquareVec3(p1, r2);
//		        float scalar = toPt/total;
//		        if (scalar > 0 && scalar < 1.0) {
//		            setPercent(scalar);
//		        }        
//		        return true;
//		    }			    			    
//		} 
//		return false;
//	}

	@Override
	public IntersectResultData<SpatialRenderable> intersectRay(RayWithEpsilon ray) {
		IntersectResultData<SpatialRenderable> result = null;
		
		if (Utils.isOccultedByShapeModel(this, ray)) {
			return result;
		}
		
		float[] pa = new float[3];
		float[] pb = new float[3];
		double[] mua = new double[1];
		double[] mub = new double[1];
		ray.setEpsilon(2f * FloatUtil.PI / 180);
		if (Util3D.lineLineIntersect(ray.getOrigin(), ray.getEnd(), startVertex(), endVertex(), pa, pb, mua, mub)) {
//			float coneRadius = ray.getLength() * FloatUtil.sin(ray.getEpsilon()) / FloatUtil.cos(ray.getEpsilon());
			float coneRadius = VectorUtil.distVec3(ray.getOrigin(), getCenter()) * FloatUtil.sin(ray.getEpsilon()) / FloatUtil.cos(ray.getEpsilon());
//			System.out.println("cone "+coneRadius);	
//			System.out.format("pa:(%g,%g,%g) pb:(%g,%g,%g)\n", pa[0],pa[1],pa[2], pb[0],pb[1],pb[2]);
//			System.out.println("distVec3(pb,pa): "+VectorUtil.distVec3(pb, pa)+" ray.epsilon: "+ray.getEpsilon()+" isZero(pa): "+VectorUtil.isVec3Zero(pa, 0)+ " isZero(pb): "+VectorUtil.isVec3Zero(pb, 0));
		    if (!VectorUtil.isVec3Zero(pa, 0) && !VectorUtil.isVec3Zero(pb, 0) && (VectorUtil.distVec3(pb, pa) <= coneRadius)) {
		    	result = new IntersectResultData<SpatialRenderable>();
		        // We have a possible result
		        float[] p1 = startVertex();
		        float[] p2 = endVertex();
		        float total  = VectorUtil.distSquareVec3(p1, p2);
		        float toPt = VectorUtil.distSquareVec3(p1, pb);
		        float scalar = toPt/total;
		        if (scalar > 0 && scalar < 1.0) {
		            setPercent(scalar);
		        } 
		        if (result.intersectionPoint == null) {
		        	result.intersectionPoint = new float[3];
		        }
		        for (int i=0; i<3; i++) {
		        	result.intersectionPoint[i] = pb[i];
		        }
		        
		        result.intersectDistance = VectorUtil.distVec3(pa, pb);
		        result.distanceToRayOrigin = VectorUtil.distVec3(ray.getOrigin(), pb);
		        result.distanceToCenter = VectorUtil.distVec3(this.getCenter(), pb);
		        
		        result.mua = (float)mua[0];
		        result.mub = (float)mub[0];
		        result.pa = pa == null? pa: pa.clone();
		        result.pb = pb == null? pb: pb.clone();
		        
//		        System.out.println("line start "+startVertex()[0]+", "+startVertex()[1]+", "+startVertex()[2]);
//		        System.out.println("line end "+endVertex()[0]+", "+endVertex()[1]+", "+endVertex()[2]);
//		        
//		        System.out.println("mua "+result.mua+" mub "+result.mub);
//		        System.out.println("Distance to Center "+result.distanceToCenter);
		        
		        result.object = this;
		        
		        
//		        float[] newEnd = VectorUtil.subVec3(new float[3], ray.getEnd(), ray.getOrigin());
//		        
//		        newEnd = VectorUtil.scaleVec3(new float[3], newEnd, 0.250f);
//		        newEnd = VectorUtil.addVec3(new float[3], newEnd, ray.getOrigin());
//				System.out.print("Ray\n"+ray.getOrigin()[0]+", "+ray.getOrigin()[1]+", "+ray.getOrigin()[2]+"\n"+
//						ray.getEnd()[0]+", "+ray.getEnd()[1]+", "+ray.getEnd()[2]+"\n"+
//						newEnd[0]+", "+newEnd[1]+", "+newEnd[2]+"\n"); 
//				
//				System.out.println("Epsilon "+ray.getEpsilon());
			
//				for (int j=0; j<pts.length; j+=3) {
//					System.out.print(pts[j]+", "+pts[j+1]+", "+pts[j+2]+"\n");
//				}
			
//		        System.out.print("Intersection\n"+pb[0]+", "+pb[1]+", "+pb[2]+"\n");
		    }			    			    
		} 
		return result;
	}

	// TODO incomplete
	@Override
	public boolean intersectFrustum(float[] normal, float[][] frustum) {
		 if (Util3D.pointInFrustum(startVertex(), frustum) && Util3D.pointInFrustum(endVertex(), frustum)) {
			 return true;
		 }
		 return false;
	}

	@Override
	public boolean intersectJoglFrustum(float[] normal, Frustum frustum) {
		// currently only check for completely in the frustum
		Visibility vis = Util3D.checkAgainstJoglFrustum(this, frustum);
		if (vis == Visibility.NoClip) {
			return true;
		}
		return false;
	}

	@Override
	public void setColor(float[] color) {
		if (color != null && color.length == 3) {
			this.color = color;
		}		
	}

	@Override
	public void execute(GL2 gl) {
		render(gl);
	}

	@Override
	public void preRender(GL2 gl) {
		// NOP
	}

	@Override
	public void postRender(GL2 gl) {
		//NOP
	}

	@Override
	public void delete(GL2 gl) {
		dispose();		
	}

	/**
	 * Method to return the user defined ID
	 * @return
	 */
	public Object getIdNumber() {
		return idNumber;
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
		if (pts != null && idx >= 0 && pts.length - idx >= 6) {
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
			float[] tmp1 = new float[3];
			tmp1 = VectorUtil.scaleVec3(new float[3], startVertex(), scaleFactor);
			pts[idx] = tmp1[0];
			pts[idx+1] = tmp1[1];
			pts[idx+2] = tmp1[2];
			
			float[] tmp2 = new float[3];
			tmp2 = VectorUtil.scaleVec3(new float[3], endVertex(), scaleFactor);
			pts[idx+3] = tmp2[0];
			pts[idx+4] = tmp2[1];
			pts[idx+5] = tmp2[2];			
		}
		hasBeenScaled = true;
	}

	@Override
	public void scaleToShapeModel(boolean canScale) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isScaled() {
		return hasBeenScaled;
	}


	@Override
	public void dispose() {
		pts = null;
		color = null;
		text = null;
		// TODO this next business should occur in Node
		if (next != null) {
			((LineSegment)next).dispose();
		}
	}

	public int getLineWidth() {
		return lineWidth;
	}

	public void setLineWidth(int lineWidth) {
		this.lineWidth = lineWidth;
	}

	/**
	 * Method to create a List of LineSegments from a single dimension array (x1y1z1, x2,y2,z2...) 
	 * @param points input point array
	 * @param index	index of the LineSegment start point in the input array
	 * @param length length of the input array
	 * @param id user defined id number, possibly the id of the input array.
	 * @param color JOGL 3 element color (each element (R,G,B) normalized between 0..1 inclusive)
	 * @return List of the generated LineSegments
	 */
	public static List<LineSegment> createLineSegments(float[] points, int index, int length, int id, float[] color, int lineWidth) {
		ArrayList<LineSegment> list = new ArrayList<>();
        for (int j=0; j<length-3; j+=3) {
           LineSegment seg = new LineSegment(points, j, id, color, lineWidth);
           list.add(seg);
        }		
		
		return list;
	}

	@Override
	public float[] getColor() {
		return color;
	}

	@Override
	public void finalize() {
        pts = null;
        if (next != null) {
            next = null;
        }		
	}
}
