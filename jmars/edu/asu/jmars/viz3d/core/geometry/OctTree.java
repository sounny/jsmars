package edu.asu.jmars.viz3d.core.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.badlogic.gdx.math.EarClippingTriangulator;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.math.geom.Frustum;

import edu.asu.jmars.layer.threed.Vec3dMath;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.asu.jmars.viz3d.core.math.Util3D;
import edu.asu.jmars.viz3d.core.vectors.Vector3D;
import edu.asu.jmars.viz3d.renderer.gl.PolygonType;

/**
 * Implementation of a loose volume octtree specifically designed to represent triangle meshes.
 * 
 * "Portions Copyright (C) Thatcher Ulrich, 2000"
 */

public class OctTree {
	
	final private int	OBJECT_COUNT;
	final public float WORLD_SIZE;
	final private int	MAX_DEPTH = 100;
	final private float maxDimension;
	static final float	DefaultFOV = FloatUtil.PI / 4;
	/** represents the 'X' position in a 3D vector */
	public static final int X = 0;
	/** represents the 'Y' position in a 3D vector */
	public static final int Y = 1;
	/** represents the 'Z' position in a 3D vector */
	public static final int Z = 2;
	/** represents the 'W' position in a 4D vector */
	public static final int W = 3;
	/** represents the first vertex position in a 3D triangle */
	/** also represents the 'X' coefficient in the equation of a line: Ax + By + Cz = D */
	public static final int A = 0;
	/** represents the second vertex position in a 3D triangle */
	/** also represents the 'Y' coefficient in the equation of a line: Ax + By + Cz = D */
	public static final int B = 1;
	/** represents the third vertex position in a 3D triangle */
	/** also represents the 'Z' coefficient in the equation of a line: Ax + By + Cz = D */
	public static final int C = 2;
	/** represents the first vertex position in a 3D triangle */
	/** also represents the constant in the equation of a line: Ax + By + Cz = D */
	public static final int D = 3;
	/** The origin */
	public static final float[] ORIGIN = new float[]{0f, 0f, 0f};
	/** Unit vector along the 'X' axis */
	public static final float[] X_AXIS = new float[]{1f, 0f, 0f};
	/** Unit vector along the 'Y' axis */
	public static final float[] Y_AXIS = new float[]{0f, 1f, 0f};
	/** Unit vector along the 'Z' axis */
	public static final float[] Z_AXIS = new float[]{0f, 0f, 1f};
	/** Low tolerance epsilon (0.0001) for floating point comparisons */
	public static final float MICRO_EPSILON = 0.0001f;
	/** Standard tolerance epsilon (0.00001) for floating point comparisons */
	public static final float MINI_EPSILON = 0.00001f;
	/** High tolerance epsilon (0.000001) for floating point comparisons */
	public static final float EPSILON = 0.000001f;
	/** epsilon^2 for floating point comparisons */
	public static final float EPSILON_SQUARE = EPSILON * EPSILON;
	/** Double epsilon for floating point comparisons */
	public static final float EPSILON_X2 = 2 * EPSILON;
	/** Super high tolerance epsilon (0.0000000000001) for floating point comparisons */
	public static final float SUPER_EPSILON = 0.0000000000001f;
	private static final boolean TEST_CULL = true;
	/** No division constant of 1/3 for 3D vector magnitude and average calculations */
	public static final float ONE_THIRD = 0.33333333333333333f;
	
   	final static float TESSELLATION_LIMIT = 0.0005f;
    private static DebugLog log = DebugLog.instance();

		
	int[] depthTotals = new int[MAX_DEPTH];
	int	nodesChecked = 0;
	int	objectsActuallyInFrustum = 0;
	int	objectsChecked = 0;
	int numPlates;
	public final float looseK = 2;
	
	boolean	showNodes = true;
	boolean	showPlates = true;
	boolean isObj = false;
	
	
	private ONode root = null;
	private float maxFacetRadius = 0f;
	
	private int nodeId = 0;
	
	// TODO SA: this should perhaps go in an external OctTree cache based on Apache Commons Collection, e.g.
	private HashMap<Float,OctTree> rescaledCache = new LinkedHashMap<>();
	
	/**
	 * Default constructor.
	 * Will create an uninitialized OctTree instance (no nodes).
	 */
	public OctTree () {
		this(null);
	}

	/**
	 * Constructor. Creates an initialized OctTree.
	 *  
	 * @param o root node
	 */
	public OctTree (ONode o) {	
		this(o, 2f, 0f, 0f, 4000000, false);
	}
	
	/**
	 * Constructor 
	 * 
	 * @param o root node
	 * @param worldSize maximum size of the largest node allowed in the tree along a single axis
	 * @param minRadius minimum radius of a single node, currently not used, can be any value
	 * @param maxRadius maximum radius of a single node
	 * @param maxPolygons maximum number of triangles that can be inserted into the tree
	 * @param isObj flag to indicate whether facets are 0 (OBJ source) or 1 (shape model source) indexed
	 */
	public OctTree (ONode o, float worldSize, float minRadius, float maxRadius, int maxPolygons, boolean isObj) {	
		root = o;
		WORLD_SIZE = worldSize;
		OBJECT_COUNT = maxPolygons;	
		maxDimension = maxRadius;
		this.isObj = isObj;		
	}
	
	/**
	 * Method to indicate if the is based on an OBJ file as opposed to a Shape Model.
	 * @return true if generated from an OBJ file
	 */
	public boolean isObj() {
		return isObj;
	}
	
	/*
	 * Tests whether the given triangle can fit in the box centered at (cx, cy, cz),
	 * with side dimensions of halfSize * 2.
	 *
	 * @param t the Triangle to test
	 * @param cx x dimension of the box
	 * @param cy y dimension of the box
	 * @param cz z dimension of the box
	 * @param halfSize the size of half of a node at a particular level, the distance from the center to the closest point of any side 
	 * @return true if the Triangle fits entirely inside the box.
	 *
	 * thread-safe?
	 */
	private boolean fitsInBox(Triangle t, float cx, float cy, float cz, float halfSize)
	{
		float[][] points = t.points;
		
		for (int i=0; i<points.length; i++) {
			if (points[i][X] < cx - halfSize ||
					points[i][X] > cx + halfSize	||
					points[i][Y] < cy - halfSize ||
					points[i][Y] > cy + halfSize ||
					points[i][Z] < cz - halfSize ||
					points[i][Z] > cz + halfSize)
			{
				return false;	
			} 
		}		
		return true;
	}

	/** 
	 * Renders a Triangle given the input JOGL profile/context
	 *
	 * @param gl JOGL/OpenGL profile/context
	 * @param t the Triangle to render
	 *
	 * not thread-safe
	 */
	public void renderTriangle(GL2 gl, Triangle t)
	// Draws the given object.
	{
		float[][] points = t.points;
		gl.glColor3f(1f, 1f, 1f);
		gl.glBegin(GL2.GL_POLYGON /* TRIANGLE_FAN */);
		for (int i=0; i<points.length; i++) {
			gl.glVertex4f(points[i][X], points[i][Y], points[i][Z], 1f);
		}
		gl.glEnd();
	}
	
	/*
	 * Given the box centered at (cx,cy, cz) and extending +/- HalfSize units
	 * along all three axes, this function checks to see if the box overlaps the
	 * given frustum.  If the box is totally outside the frustum, then
	 * returns NOT_VISIBLE; if the box is totally inside the frustum, then
	 * returns NO_CLIP; otherwise, returns SOME_CLIP.
	 *
	 * @param cx x dimension of the box
	 * @param cy y dimension of the box
	 * @param cz z dimension of the box
	 * @param halfSize the size of half of a node at a particular level, the distance from the center to the closest point of any side 
	 * @param frustum float[6][3] representation of a frustum as a point on the surface of each frustum side 
	 * @return how much of the box is visible in the frustum
	 *
	 * thread-safe
	 */
	 private static Visibility	checkBoxAgainstFrustum(float cx, float cy, float cz, float halfSize, float[][] frustum)
	{
		  int p;
		  int c;
		  int c2 = 0;
		  for( p = 0; p < 6; p++ )
		  {
		    c = 0;
		    if( frustum[p][0] * (cx - halfSize) + frustum[p][1] * (cy - halfSize) + frustum[p][2]    * (cz - halfSize) + frustum[p][3] > 0 ) {
		      c++;
		    }
		    if( frustum[p][0] * (cx + halfSize) + frustum[p][1] * (cy - halfSize) + frustum[p][2]    * (cz - halfSize) + frustum[p][3] > 0 ) {
		      c++;
		    }
		    if( frustum[p][0] * (cx - halfSize) + frustum[p][1] * (cy + halfSize) + frustum[p][2]    * (cz - halfSize) + frustum[p][3] > 0 ) {
		      c++;
		    }
		    if( frustum[p][0] * (cx + halfSize) + frustum[p][1] * (cy + halfSize) + frustum[p][2]    * (cz - halfSize) + frustum[p][3] > 0 ) {
		      c++;
		    }
		    if( frustum[p][0] * (cx - halfSize) + frustum[p][1] * (cy - halfSize) + frustum[p][2]    * (cz + halfSize) + frustum[p][3] > 0 ) {
		      c++;
		    }
		    if( frustum[p][0] * (cx + halfSize) + frustum[p][1] * (cy - halfSize) + frustum[p][2]    * (cz + halfSize) + frustum[p][3] > 0 ) {
		      c++;
		    }
		    if( frustum[p][0] * (cx - halfSize) + frustum[p][1] * (cy + halfSize) + frustum[p][2]    * (cz + halfSize) + frustum[p][3] > 0 ) {
		      c++;
		    }
		    if( frustum[p][0] * (cx + halfSize) + frustum[p][1] * (cy + halfSize) + frustum[p][2]    * (cz + halfSize) + frustum[p][3] > 0 ) {
		      c++;
		    }
		    if( c == 0 ) {
		      return Visibility.NotVisible;		// All the box vertices are outside one of the frustum edges.
		    }
		    if( c == 8 ) {
		      c2++;
		    }
		  }
		  return (c2 == 6) ? Visibility.NoClip : Visibility.SomeClip;

	}
	 
	 /*
	  * Determines whether a given Triangle is in, out, or partially inside a given JOGL frustum
	  *
	  * @param t the Triangle to test
	  * @param frustum the frustum to compare against
	  * @return the visibility of the Triangle
	  *
	  * thread-safe
	  */
	 private static Visibility checkTriangleAgainstJoglFrustum(Triangle t, com.jogamp.opengl.math.geom.Frustum frustum) {
		    float[][] points = t.points;
		    int pointInCount = 0;
		    
			for (int p=0; p<3; p++ ) {
				boolean isOut = frustum.isPointOutside(points[p]);
				if (!isOut) {
					pointInCount++;
				}
			}
		      
		    if (pointInCount == 3) {
		    	return Visibility.NoClip;
		    } else if (pointInCount > 0) {
		    	return Visibility.SomeClip;
		    } else {
		    	return Visibility.NotVisible;
		    }
		 }	 
		 
	 /*
	  * Determines whether a given Triangle is in, out, or partially inside a given frustum
	  *
	  * @param t the Triangle to test
	  * @param frustum the frustum to compare against
	  * @return the visibility of the Triangle
	  *
	  * thread-safe
	  */
	 public static Visibility checkTriangleAgainstFrustum(Triangle t, float[][] frustum) {
	    int f, p;
	    float[][] points = t.points;
	    int pointInCount = 0;
	    
	    for (f=0; f<6; f++)
	    {
	      for (p=0; p<3; p++ ) {
	        if (frustum[f][0] * points[p][X] + frustum[f][1] * points[p][Y] + frustum[f][2] * points[p][Z] + frustum[f][3] > 0) {
	        	pointInCount++;
	        }
	      }
	    }
	    if (pointInCount == 18) {
	    	return Visibility.NoClip;
	    } else if (pointInCount > 0) {
	    	return Visibility.SomeClip;
	    } else {
	    	return Visibility.NotVisible;
	    }
	 }	
	 
	 /*
	  * Determines whether a given Triangle is in, out, or partially inside a given frustum with any number of sides
	  *
	  * @param t the Triangle to test
	  * @param frustum the frustum to compare against
	  * @return the visibility of the Triangle
	  *
	  * thread-safe
	  */
	 public static Visibility checkTriangleAgainstAnyFrustum(Triangle t, float[][] frustum) {
	    int f, p;
	    float[][] points = t.points;
	    int pointInCount = 0;
	    
	    for (f=0; f<frustum.length; f++)
	    {
	      for (p=0; p<3; p++ ) {
	        if (frustum[f][0] * points[p][X] + frustum[f][1] * points[p][Y] + frustum[f][2] * points[p][Z] + frustum[f][3] > 0) {
	        	pointInCount++;
	        }
	      }
	    }
	    if (pointInCount == frustum.length * 3) {
	    	return Visibility.NoClip;
	    } else if (pointInCount > 0) {
	    	return Visibility.SomeClip;
	    } else {
	    	return Visibility.NotVisible;
	    }
	 }	 
	 	 
	/*
	 * This method sorts a 2 element float array so that array[0] <= array[1]
	 * @param f - input float array
	 * @return f
	 */
	private float[] sortFloat (float[] f) {
		if (f[0] > f[1]) {
			float c = f[0];
			f[0] = f[1];
			f[1] = c;
		}
		return f;
	}
	
	/**
	 * Method to return the radius of the largest facet in the Tree
	 * @return max radius
	 */
	public float getMaxFacetRadius() {
		return maxFacetRadius;
	}

	/*
	 * The following 
	 */
	
	/* Triangle/triangle intersection test routine,
	 * by Tomas Moller, 1997.
	 * See article "A Fast Triangle-Triangle Intersection Test",
	 * Journal of Graphics Tools, 2(2), 1997
	 *
	 * Updated June 1999: removed the divisions -- a little faster now!
	 * Updated October 1999: added {} to CROSS and SUB macros 
	 *
	 * int NoDivTriTriIsect(float V0[3],float V1[3],float V2[3],
	 *                      float U0[3],float U1[3],float U2[3])
	 *
	 * parameters: vertices of triangle 1: V0,V1,V2
	 *             vertices of triangle 2: U0,U1,U2
	 * result    : returns 1 if the triangles intersect, otherwise 0
	 *
	 */

//	private int i0=0, i1=0; // parameters for the following edge and triangle tests
	
	/*
	 * Ported from:
	 * Triangle/triangle intersection test routine,
	 * by Tomas Moller, 1997.
	 * See article "A Fast Triangle-Triangle Intersection Test",
	 * Journal of Graphics Tools, 2(2), 1997
	 */
	private boolean edgeToEdgeTest (float[] V0, float[] U0, float[] U1, float Ax, float Ay, Integer i0, Integer i1) {
		float Bx,By,Cx,Cy,e,d,f;    
		
		Bx=U0[i0]-U1[i0];                                   
		By=U0[i1]-U1[i1];                                   
		Cx=V0[i0]-U0[i0];                                   
		Cy=V0[i1]-U0[i1];                                   
		f=Ay*Bx-Ax*By;                                      
		  d=By*Cx-Bx*Cy;                                      
		  if((f>0 && d>=0 && d<=f) || (f<0 && d<=0 && d>=f))  
		  {                                                   
		    e=Ax*Cy-Ay*Cx;                                    
		    if(f>0)                                           
		    {                                                 
		      if(e>=0 && e<=f) return true;                      
		    }                                                 
		    else                                              
		    {                                                 
		      if(e<=0 && e>=f) return true;                      
		    } 
		  }
		return false;
	}
	
	/*
	 * Ported from:
	 * Triangle/triangle intersection test routine,
	 * by Tomas Moller, 1997.
	 * See article "A Fast Triangle-Triangle Intersection Test",
	 * Journal of Graphics Tools, 2(2), 1997
	 */
	  private boolean edgeAgainstTriangleEdges (float[] V0, float[]V1, float[]U0, float[] U1, float[] U2, Integer i0, Integer i1) { 
		float Ax=0f,Ay=0f;           
	    Ax=V1[i0]-V0[i0];                            
	    Ay=V1[i1]-V0[i1];                            
	    /* test edge U0,U1 against V0,V1 */          
	    if (edgeToEdgeTest(V0,U0,U1, Ax, Ay, i0, i1)) {
	    	return true;
	    } 
		/* test edge U1,U2 against V0,V1 */          
	    else if (edgeToEdgeTest(V0,U1,U2, Ax, Ay, i0, i1)) {
	    	return true;
	    }
	    /* test edge U2,U1 against V0,V1 */          
	    else if (edgeToEdgeTest(V0,U2,U0, Ax, Ay, i0, i1)) {
	    	return true;
	    } else return false;
	  }


	/*
	 * Ported from:
	 * Triangle/triangle intersection test routine,
	 * by Tomas Moller, 1997.
	 * See article "A Fast Triangle-Triangle Intersection Test",
	 * Journal of Graphics Tools, 2(2), 1997
	 */
	  private boolean pointInTriangle(float[] V0, float[] U0, float[] U1, float[] U2, Integer i0, Integer i1) {                                           
	    float a=0,b=0,c=0,d0=0,d1=0,d2=0;                     
	    /* is T1 completly inside T2? */          
	    /* check if V0 is inside tri(U0,U1,U2) */ 
	    a=U1[i1]-U0[i1];                          
	    b=-(U1[i0]-U0[i0]);                       
	    c=-a*U0[i0]-b*U0[i1];                     
	    d0=a*V0[i0]+b*V0[i1]+c;                   
	                                              
	    a=U2[i1]-U1[i1];                          
	    b=-(U2[i0]-U1[i0]);                       
	    c=-a*U1[i0]-b*U1[i1];                     
	    d1=a*V0[i0]+b*V0[i1]+c;                   
	                                              
	    a=U0[i1]-U2[i1];                          
	    b=-(U0[i0]-U2[i0]);                       
	    c=-a*U2[i0]-b*U2[i1];                     
	    d2=a*V0[i0]+b*V0[i1]+c;                   
	    if(d0*d1>0.0)                             
	    {                                         
	      if(d0*d2>0.0) {
	    	  return true;                 
	      } else {
	    	  return false;
	      }
	    } else {
	    	return false;
	    }
	  }
	  
	/*
	 * Ported from:
	 * Triangle/triangle intersection test routine,
	 * by Tomas Moller, 1997.
	 * See article "A Fast Triangle-Triangle Intersection Test",
	 * Journal of Graphics Tools, 2(2), 1997
	 */
	private boolean coplanarTriTri(float[] N, float[] V0, float[] V1,float[] V2,
	              float[] U0, float[] U1, float[] U2, Integer i0, Integer i1) {
		float[] A = new float[3];
		/* first project onto an axis-aligned plane, that maximizes the area */
		/* of the triangles, compute indices: i0,i1. */
		A[0]=FloatUtil.abs(N[0]);
		A[1]=FloatUtil.abs(N[1]);
		A[2]=FloatUtil.abs(N[2]);
		if(A[0]>A[1]) {
			if(A[0]>A[2]) {
			   i0=1;      /* A[0] is greatest */
			   i1=2;
			} else {
			   i0=0;      /* A[2] is greatest */
			   i1=1;
			}
		} else {   /* A[0]<=A[1] */
			if(A[2]>A[1]) {
			   i0=0;      /* A[2] is greatest */
			   i1=1;
			} else {
			   i0=0;      /* A[1] is greatest */
			   i1=2;
			}
		}
		
		/* test all edges of triangle 1 against the edges of triangle 2 */
		if (edgeAgainstTriangleEdges(V0,V1,U0,U1,U2, i0, i1)) { // return result here?
			return true;
		}
		if (edgeAgainstTriangleEdges(V1,V2,U0,U1,U2, i0, i1)) { // return result here?
			return true;
		}
		if (edgeAgainstTriangleEdges(V2,V0,U0,U1,U2, i0, i1)) { // return result here?
			return true;
		}
		
		/* finally, test if tri1 is totally contained in tri2 or vice versa */
		if (pointInTriangle(V0,U0,U1,U2, i0, i1)) { // return result here?
			return true;
		}
		if (pointInTriangle(U0,V0,V1,V2, i0, i1)) { // return result here?
			return true;
		}
		
		return false;
	}
	
	/*
	 * Ported from:
	 * Triangle/triangle intersection test routine,
	 * by Tomas Moller, 1997.
	 * See article "A Fast Triangle-Triangle Intersection Test",
	 * Journal of Graphics Tools, 2(2), 1997
	 */
	private boolean computeIntervals(float VV0, float VV1, float VV2, float D0, float D1, float D2, float D0D1, float D0D2, float[] A, float[] B, float[] C, float[] X0, float[] X1) { 
	        if(D0D1 > 0.0f) 
	        { 
	            /* here we know that D0D2<=0.0 */ 
	            /* that is D0, D1 are on the same side, D2 on the other or on the plane */ 
	                A[0]=VV2; B[0]=(VV0-VV2)*D2; C[0]=(VV1-VV2)*D2; X0[0]=D2-D0; X1[0]=D2-D1; 
	        } 
	        else if(D0D2>0.0f)
	        { 
	                /* here we know that d0d1<=0.0 */ 
	            A[0]=VV1; B[0]=(VV0-VV1)*D1; C[0]=(VV2-VV1)*D1; X0[0]=D1-D0; X1[0]=D1-D2; 
	        } 
	        else if(D1*D2>0.0f || D0!=0.0f) 
	        { 
	                /* here we know that d0d1<=0.0 or that D0!=0.0 */ 
	                A[0]=VV0; B[0]=(VV1-VV0)*D0; C[0]=(VV2-VV0)*D0; X0[0]=D0-D1; X1[0]=D0-D2; 
	        } 
	        else if(D1!=0.0f) 
	        { 
	                A[0]=VV1; B[0]=(VV0-VV1)*D1; C[0]=(VV2-VV1)*D1; X0[0]=D1-D0; X1[0]=D1-D2; 
	        } 
	        else if(D2!=0.0f) 
	        { 
	                A[0]=VV2; B[0]=(VV0-VV2)*D2; C[0]=(VV1-VV2)*D2; X0[0]=D2-D0; X1[0]=D2-D1; 
	        } 
	        else 
	        { 
	        	return true;
	        }
	        return false;
	}
	
	/*
	 * Tests whether two 3D triangles intersect.
	 * The algorithm is implemented without using any division.
	 * Ported from:
	 * Triangle/triangle intersection test routine,
	 * by Tomas Moller, 1997.
	 * See article "A Fast Triangle-Triangle Intersection Test",
	 * Journal of Graphics Tools, 2(2), 1997
	 *
	 * Updated June 1999: removed the divisions -- a little faster now!
	 * Updated October 1999: added {} to CROSS and SUB macros 
	 *
	 * int NoDivTriTriIsect(float V0[3],float V1[3],float V2[3],
	 *                      float U0[3],float U1[3],float U2[3])
	 *
	 * parameters: vertices of triangle 1: V0,V1,V2
	 *             vertices of triangle 2: U0,U1,U2
	 * result    : returns 1 if the triangles intersect, otherwise 0
	 *
	 *
	 *
	 * @param V0 float[X, Y, Z] first vertex of the first triangle
	 * @param V1 float[X, Y, Z] second vertex of the first triangle
	 * @param V2 float[X, Y, Z] third vertex of the first triangle
	 * @param U0 float[X, Y, Z] first vertex of the second triangle
	 * @param U1 float[X, Y, Z] second vertex of the second triangle
	 * @param U2 float[X, Y, Z] third vertex of the second triangle
	 * @return true if the triangles intersect
	 *
	 * not thread-safe
	 */
	private boolean noDivTriTriIsect(float[] V0, float[] V1, float[] V2,
	            float[] U0, float[] U1, float[] U2) {
		float[] E1 = new float[3], E2 = new float[3];
		float[] N1 = new float[3], N2 = new float[3];
		float d1=0f, d2=0f;
		float du0=0f, du1=0f, du2=0f, dv0=0f, dv1=0f, dv2=0f;
		float[] D = new float[3];
		float[] isect1 = new float[2], isect2 = new float[2];
		float du0du1=0f, du0du2=0f, dv0dv1=0f, dv0dv2=0f;
		int index = 0;;
		float vp0=0f, vp1=0f, vp2=0f;
		float up0=0f, up1=0f, up2=0f;
		float bb=0f, cc=0f, max=0f;	
		Integer i0 = 0;
		Integer i1 = 0;
		
		/* compute plane equation of triangle(V0,V1,V2) */
		E1 = VectorUtil.subVec3(E1,V1,V0);
		E2 = VectorUtil.subVec3(E2,V2,V0);
		N1 = VectorUtil.crossVec3(N1,E1,E2);
		d1 = -VectorUtil.dotVec3(N1,V0);
		/* plane equation 1: N1.X+d1=0 */
		
		/* put U0,U1,U2 into plane equation 1 to compute signed distances to the plane*/
		du0 = VectorUtil.dotVec3(N1,U0) + d1;
		du1= VectorUtil.dotVec3(N1,U1) + d1;
		du2= VectorUtil.dotVec3(N1,U2) + d1;
		
		/* coplanarity robustness check */
		if(FloatUtil.abs(du0) < Float.MIN_VALUE) du0 = 0f;
		if(FloatUtil.abs(du1) < Float.MIN_VALUE) du1 = 0f;
		if(FloatUtil.abs(du2) < Float.MIN_VALUE) du2 = 0f;
		
		du0du1= du0 * du1;
		du0du2= du0 * du2;
		
		if(du0du1 > 0.0f && du0du2 > 0.0f) /* same sign on all of them + not equal 0 ? */
			return false;                    /* no intersection occurs */
		
		/* compute plane of triangle (U0,U1,U2) */
		E1 = VectorUtil.subVec3(E1,U1,U0);
		E2 = VectorUtil.subVec3(E2,U2,U0);
		N2 = VectorUtil.crossVec3(N2,E1,E2);
		d2 = -VectorUtil.dotVec3(N2,U0);
		/* plane equation 2: N2.X+d2=0 */
		
		/* put V0,V1,V2 into plane equation 2 */
		dv0 = VectorUtil.dotVec3(N2,V0) + d2;
		dv1 = VectorUtil.dotVec3(N2,V1) + d2;
		dv2 = VectorUtil.dotVec3(N2,V2) + d2;
		
		if(FloatUtil.abs(dv0)<Float.MIN_VALUE) dv0 = 0f;
		if(FloatUtil.abs(dv1)<Float.MIN_VALUE) dv1 = 0f;
		if(FloatUtil.abs(dv2)<Float.MIN_VALUE) dv2 = 0f;
		
		dv0dv1 = dv0 * dv1;
		dv0dv2 = dv0 * dv2;
		
		if(dv0dv1 > 0.0f && dv0dv2 > 0.0f) /* same sign on all of them + not equal 0 ? */
			return false;                    /* no intersection occurs */
		
		/* compute direction of intersection line */
		D = VectorUtil.crossVec3(D,N1,N2);
		
		/* compute and index to the largest component of D */
		max = FloatUtil.abs(D[0]);
		index=0;
		bb = FloatUtil.abs(D[1]);
		cc = FloatUtil.abs(D[2]);
		if(bb>max) { 
			max=bb;
			index=1;
		}
		if(cc>max) {
			max=cc;
			index=2;
		}
		
		/* this is the simplified projection onto L*/
		vp0=V0[index];
		vp1=V1[index];
		vp2=V2[index];
		
		up0=U0[index];
		up1=U1[index];
		up2=U2[index];
		
		/* compute interval for triangle 1 */
		float[] a={0f}, b={0f}, c={0f}, x0={0f}, x1={0f};
		if (computeIntervals(vp0,vp1,vp2,dv0,dv1,dv2,dv0dv1,dv0dv2,a,b,c,x0,x1)) {
			if (coplanarTriTri(N1,V0,V1,V2,U0,U1,U2, i0, i1)) {
				return true;
			}
		}
		
		/* compute interval for triangle 2 */
		float[] d={0f}, e={0f}, f={0f}, y0={0f}, y1={0f};
		if (computeIntervals(up0,up1,up2,du0,du1,du2,du0du1,du0du2,d,e,f,y0,y1)) {
			if (coplanarTriTri(N1,V0,V1,V2,U0,U1,U2, i0, i1)) {
				return true;
			}
		}
		
		float xx=0f, yy=0f, xxyy = 0f, tmp = 0f;
		xx=x0[0]*x1[0];
		yy=y0[0]*y1[0];
		xxyy=xx*yy;
		
		tmp=a[0]*xxyy;
		isect1[0]=tmp+b[0]*x1[0]*yy;
		isect1[1]=tmp+c[0]*x0[0]*yy;
		
		tmp=d[0]*xxyy;
		isect2[0]=tmp+e[0]*xx*y1[0];
		isect2[1]=tmp+f[0]*xx*y0[0];
		
		isect1 = sortFloat(isect1);
		isect2 = sortFloat(isect2);
		
		if(isect1[1]<isect2[0] || isect2[1]<isect1[0]) {
			return false;
		}
		
		return true;
	}	
	  
	/**
	 * Returns the number of nodes in the given subtree.
	 *
	 * @param q the root node of the subtree to check
	 * @return node count
	 * @throws
	 *
	 * thread-safe
	 */
	public int	countNodes(ONode q)
	{
		int	count = 1;	// Count ourself.

		// Count descendants.
		int	i, j, k;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (q.child[k][j][i] != null) {
						count += countNodes(q.child[k][j][i]);
					}
				}
			}
		}
		return count;
	}

	/*
	 * Do not call this method unless the intent is to remove all nodes
	 * and their contents in their entirety!! 
	 * 
	 */
	public void clearAllNodes(ONode q)
	{
		int	i, j, k;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (q.child[k][j][i] != null) {
						clearAllNodes(q.child[k][j][i]);
					}
				}
			}
		}
		q.object.delete();
		return;
	}
	//                           |
	// Loose octtree functions. |
	//                           V

	/**
	 * Insert the given Triangle into the tree given by ONode.
	 * <Description>
	 *
	 * @param o the root node of the tree to insert into
	 * @param t the Triangle to insert
	 * @return Returns the depth of the node the object was placed in.
	 *
	 * not thread-safe
	 */
	public int looseOctTreeInsert(ONode o, Triangle t)
	{
		if (o == null) {
			o = root;
		}
		// Check child nodes to see if object fits in one of them.
		if (o.depth + 1 < MAX_DEPTH) {
			float halfSize = looseK * WORLD_SIZE / (2 << o.depth+1);
			float offset = (WORLD_SIZE / (2 << o.depth)) / 2;

			// Pick child based on classification of object's center point.
			float[] center = t.getCenter();
			int	i = (center[X] <= o.cx) ? 0 : 1;
			int	j = (center[Y] <= o.cy) ? 0 : 1;
			int	k = (center[Z] <= o.cz) ? 0 : 1;

			float	cx = o.cx + ((i == 0) ? -offset : offset);
			float	cy = o.cy + ((j == 0) ? -offset : offset);
			float	cz = o.cz + ((k == 0) ? -offset : offset);

			if (fitsInBox(t, cx, cy, cz, halfSize)) {
				// Recurse into this node.
				if (o.child[k][j][i] == null) {
					o.child[k][j][i] = new ONode(o, cx, cy, cz, o.depth + 1);
				}
				return looseOctTreeInsert(o.child[k][j][i], t);
			}
		}

		// Keep object in this node.
		t.next = o.object;
		o.object = t;
		t.node = o;
		depthTotals[o.depth]++;
		numPlates++;
		
		o.id = nodeId++;
		
		if (t.getRadius() > this.maxFacetRadius) {
			maxFacetRadius = t.getRadius();
		}
		
		return o.depth;
	}	

	/*
	 * 	Counts the objects in the ONode which might touch the given frustum, based on
	 *	the information in the loose octtree partitioning.
	 *
	 * @param o
	 * @param frustum 
	 * @param vis the desired visibility
	 * @return count of the number of objects partially or fully in the given frustum
	 *
	 * thread-safe
	 */
	private int looseOctTreeCountObjectsInJoglFrustum(ONode o, com.jogamp.opengl.math.geom.Frustum frustum, Visibility vis)
	{
		float halfSize = looseK * WORLD_SIZE / (2 << o.depth);
		
		if (vis == Visibility.SomeClip) {
			AABBox box = o.getAABbox(halfSize);
			boolean notVisible = frustum.isAABBoxOutside(box);
			if (notVisible) {
				return 0;
			}
		}

		nodesChecked++;
		
		int	count = 0;
		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (o.child[k][j][i] != null) {
						count += looseOctTreeCountObjectsInJoglFrustum(o.child[k][j][i], frustum, vis);
					}
				}
			}
		}

		// Count objects in this node.
		Triangle t = o.object;
		while (t != null) {
			if (checkTriangleAgainstJoglFrustum(t, frustum) != Visibility.NotVisible) {
				objectsActuallyInFrustum++;
			}
			
			count++;
			t = t.next;
		}

		return count;
	}

	/*
	 * Counts the objects in o which might touch the given frustum, based on
	 * the information in the loose octtree partitioning.
	 *
	 * @param o the node to count
	 * @param frustum the desired visibility
	 * @param vis the desired visibility
	 *
	 * thread-safe
	 */
	private int	looseOctTreeCountObjectsInFrustum(ONode o, float[][] frustum, Visibility vis)
	{
		float halfSize = looseK * WORLD_SIZE / (2 << o.depth);
		
		if (vis == Visibility.SomeClip) {
			vis = checkBoxAgainstFrustum(o.cx, o.cy, o.cz, halfSize, frustum);
			if (vis == Visibility.NotVisible) {
				return 0;
			}
		}

		nodesChecked++;
		
		int	count = 0;
		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (o.child[k][j][i] != null) {
						count += looseOctTreeCountObjectsInFrustum(o.child[k][j][i], frustum, vis);
					}
				}
			}
		}

		// Count objects in this node.
		Triangle t = o.object;
		while (t != null) {
			if (checkTriangleAgainstFrustum(t, frustum) != Visibility.NotVisible) {
				objectsActuallyInFrustum++;
			}
			
			count++;
			t = t.next;
		}

		return count;
	}
	
	/*
	 * Debug method to render the objects in a given node
	 *
	 * @param gl Current JOGL Profile/Context
	 * @param o the node to render
	 *
	 * not thread-safe
	 */
	// Render the objects in the frustum.
	private void renderLooseNode(GL2 gl, ONode o) {
		float halfSize = looseK * WORLD_SIZE / (2 << o.depth);
		
		float	d = (o.depth / 5.0f) + 0.2f;
		if (d > 1) d = 1;

		// Draw this node.
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
		gl.glColor3f(0f, d, 0f);
		gl.glBegin(GL2.GL_QUADS);
		
		// closest in XY plane
		gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz - halfSize);
		gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz - halfSize);
		gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz - halfSize);
		gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz - halfSize);
		
		// farthest in XY plane
		gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz + halfSize);
		gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz + halfSize);
		gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz + halfSize);
		gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz + halfSize);
		
		
		// closest in YZ plane
		gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz + halfSize);
		gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz - halfSize);
		gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz - halfSize);
		gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz + halfSize);
		
		// farthest in YZ plane
		gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz - halfSize);
		gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz + halfSize);
		gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz + halfSize);
		gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz - halfSize);
		
		// closest in XZ plane
		gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz - halfSize);
		gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz + halfSize);
		gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz + halfSize);
		gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz - halfSize);
		
		// farthest in XZ plane
		gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz - halfSize);
		gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz - halfSize);
		gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz + halfSize);
		gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz + halfSize);
		
		gl.glEnd();
		
		// Render objects in this node.
			Triangle t = o.object;
			while (t != null) {
					gl.glColor3f(0.5f, 0.0f, 0.5f);
					renderTriangle(gl,t);
				t = t.next;
			}
	}

	/**
	 * Debug method to render the objects in a node and its children that fully or partially fit within the given frustum. 
	 *
	 * @param gl Current JOGL Profile/Context
	 * @param o the node to render
	 * @param frustum
	 * @param vis the desired visibility
	 *
	 * not thread-safe
	 */
	// Render the objects in the frustum.
	public void renderLoose(GL2 gl, ONode o, float[][] frustum, Visibility vis, boolean showChildren, boolean showNodes)
	{
		float halfSize = looseK * WORLD_SIZE / (2 << o.depth);
		
		if (vis != null && vis == Visibility.SomeClip) {
			vis = checkBoxAgainstFrustum(o.cx, o.cy, o.cz, halfSize, frustum);
			if (vis == Visibility.NotVisible) {
				return;
			}
		}

		float	d = (o.depth / 7.0f) + 0.2f;
		if (d > 1) d = 1;

		if (showNodes) {
			gl.glPushMatrix();
			// Draw this node.
//			gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
			gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
			gl.glColor4f(0f, d, 0f, 0.25f);
			gl.glBegin(GL2.GL_TRIANGLES);
			
			// closest in XY plane
			gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz - halfSize);
			gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz - halfSize);
			gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz - halfSize);

			gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz - halfSize);
			gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz - halfSize);
			gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz - halfSize);
			
			// farthest in XY plane
			gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz + halfSize);
			gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz + halfSize);
			gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz + halfSize);

			gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz + halfSize);
			gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz + halfSize);
			gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz + halfSize);
			
			// closest in YZ plane
			gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz + halfSize);
			gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz - halfSize);
			gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz - halfSize);

			gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz - halfSize);
			gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz + halfSize);
			gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz + halfSize);
			
			// farthest in YZ plane
			gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz - halfSize);
			gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz + halfSize);
			gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz + halfSize);

			gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz + halfSize);
			gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz - halfSize);
			gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz - halfSize);
			
			// closest in XZ plane
			gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz - halfSize);
			gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz + halfSize);
			gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz + halfSize);

			gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz + halfSize);
			gl.glVertex3f(o.cx + halfSize, o.cy - halfSize, o.cz - halfSize);
			gl.glVertex3f(o.cx - halfSize, o.cy - halfSize, o.cz - halfSize);
			
			// farthest in XZ plane
			gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz - halfSize);
			gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz - halfSize);
			gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz + halfSize);

			gl.glVertex3f(o.cx + halfSize, o.cy + halfSize, o.cz + halfSize);
			gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz + halfSize);
			gl.glVertex3f(o.cx - halfSize, o.cy + halfSize, o.cz - halfSize);
			
			gl.glEnd();
			
			gl.glPopMatrix();
		}
		
		// Render objects in this node.
		if (showPlates) {
			Triangle t = o.object;
			while (t != null) {
					gl.glColor3f(1f, 1f, 0f);
					renderTriangle(gl,t);
				t = t.next;
			}
	
			if (showChildren) {
				// Render children.
				int	k, j, i;
				for (k = 0; k < 2; k++) {
					for (j = 0; j < 2; j++) {
						for (i = 0; i < 2; i++) {
							if (o.child[k][j][i] != null) {
								renderLoose(gl, o.child[k][j][i], frustum, vis, showChildren, showNodes);
							}
						}
					}
				}
			}
		}
	}
	
	/*
	 * Returns the number of objects within the node o and its children which contact
	 * the given object.  Increments ObjectsChecked for each object which
	 * is tested for contact.
	 *
	 * @param o the node
	 * @param t
	 * @return the number of objects in the octtree which contact the input object
	 *
	 * not thread-safe
	 */
	// Returns the number of objects within the subtree q which contact
	// the given object.  Increments ObjectsChecked for each object which
	// is tested for contact.
	private int looseCountContactingObjects(ONode o, Triangle t)
	{
		nodesChecked++;
		
		float halfSize = looseK * WORLD_SIZE / (2 << o.depth);

		// First check to see if the object is completely outside the boundary
		// of this node.
		float dx1 = FloatUtil.abs(o.cx - t.points[0][X]);
		float dy1 = FloatUtil.abs(o.cy - t.points[0][Y]);
		float dz1 = FloatUtil.abs(o.cz - t.points[0][Z]);
		float dx2 = FloatUtil.abs(o.cx - t.points[1][X]);
		float dy2 = FloatUtil.abs(o.cy - t.points[1][Y]);
		float dz2 = FloatUtil.abs(o.cz - t.points[1][Z]);
		float dx3 = FloatUtil.abs(o.cx - t.points[2][X]);
		float dy3 = FloatUtil.abs(o.cy - t.points[2][Y]);
		float dz3 = FloatUtil.abs(o.cz - t.points[2][Z]);
		float xRange = o.cx + halfSize;
		float yRange = o.cy + halfSize;
		float zRange = o.cz + halfSize;
		
		if (dx1 > xRange || 
				dy1 > yRange ||
				dz1 > zRange ||
				dx2 > xRange || 
				dy2 > yRange ||
				dz2 > zRange ||
				dx3 > xRange || 
				dy3 > yRange ||
				dz3 > zRange) {
			// Object is completely outside the boundary of this
			// node; don't bother checking contents.
			return 0;
		}
		
		int	count = 0;
		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (o.child[k][j][i] != null) {
						count += looseCountContactingObjects(o.child[k][j][i], t);
					}
				}
			}
		}

		// Check objects in this node.
		Triangle p = o.object;
		while (p != null) {
			if (t != p && noDivTriTriIsect(t.points[0], t.points[1], t.points[2], p.points[0], p.points[1], p.points[2])) {
				count++;
			}
			
			objectsChecked++;
			p = p.next;
		}

		return count;
	}
	
	/*
	 * Checks whether two nodes intersect.
	 *
	 * @param a a node
	 * @param b a node to test for intersection
	 * @return true if the nodes intersect
	 *
	 * thread-safe
	 */
	private boolean looseNodeNodeIntersect(ONode a, ONode b) {
		
		float aHalfSize = looseK * WORLD_SIZE / (2 << a.depth);
		float bHalfSize = looseK * WORLD_SIZE / (2 << b.depth);
		
		if (a.getMinX(aHalfSize) > b.getMaxX(bHalfSize)) {
			return false;
		}
		if (a.getMaxX(aHalfSize) < b.getMinX(bHalfSize)) {
			return false;
		}
		if (a.getMinY(aHalfSize) > b.getMaxY(bHalfSize)) {
			return false;
		}
		if (a.getMaxY(aHalfSize) < b.getMinY(bHalfSize)) {
			return false;
		}
		if (a.getMinZ(aHalfSize) > b.getMaxZ(bHalfSize)) {
			return false;
		}
		if (a.getMaxZ(aHalfSize) < b.getMinZ(bHalfSize)) {
			return false;
		}
		
		// we have overlap on all 3 axes so we have an intersection
		return true;
	}

	/*
	 * Returns all the polygons that contact the input polygon.
	 *
	 * @param n the node (including children to check)
	 * @param p the polygon (Triangle) to check
	 * @param found List of the contacting polygons (Triangles)
	 * @return count of the contacting polygons
	 *
	 * not thread-safe
	 */
	private int looseGetContactingPolygons(ONode n, Triangle p, ArrayList<Triangle> found) {
		int	count = 0;
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						count += looseGetContactingPolygons(n.child[k][j][i], p, found);
					}
				}
			}
		}
		
		Triangle poly = n.object;
		while (poly != null) {
			if (poly != p && (noDivTriTriIsect(poly.points[0], poly.points[1], poly.points[2], p.points[0], p.points[1], p.points[2]) ||
					triTriCornerIntersect(poly.points, p.points))) {
				count++;
				found.add(poly);
			}
			objectsChecked++;
			poly = poly.next;
		}
		return count;
	}
	
	/**
	 * Attempts to fit an array of points describing a segmented line in 3D to the mesh (Triangles) in this octtree
	 *
	 * @param points the points that define the segmented line
	 * @return the array of 3D points that describe the fit to the surface of the mesh in this octtree
	 *
	 * not thread-safe
	 */
	public float[] looseFitOutLine(float[] points) {
		
		float[][] tri1 = null;
		float[][] tri2 = null;
		ArrayList<float[]> outLine = new ArrayList<>();
		HashMap<Integer, TriLineIntersect> edgeIntercepts = new HashMap<>();
		ArrayList<Triangle> inside = new ArrayList<>();
		
		if (points.length < 2) { // we don't have enough points to have a valid input line
			log.aprintln("Could not fit a line to the shape model with < 2 points");
			return points;
		}
				
		// using two points of the outline at a time at a time create a line in CCW order
		// elevate the line until it either clears the body or divide it into two lines that clear the body
		// project the line(s) onto the surface of the body and find all the intersected triangles and
		//    the points on their edges where the line intersects them
		for (int i=0; i<points.length-3; i+=3) {
			// scale each two consecutive points up sufficiently so a ray between them clears the body
			tri1 = new float[][]{{points[i], points[i+1], points[i+2]}, 
					{points[i+3], points[i+4], points[i+5]}, 
					{0f, 0f, 0f}}; 
			
			tri1[0] = VectorUtil.scaleVec3(tri1[0], tri1[0], (float)Util.EQUAT_RADIUS * 10f);
			tri1[1] = VectorUtil.scaleVec3(tri1[1], tri1[1], (float)Util.EQUAT_RADIUS * 10f);
			tri1[2] = VectorUtil.scaleVec3(tri1[2], tri1[2], (float)Util.EQUAT_RADIUS * 10f);
			
			inside.clear();
			
			// need to test if the ray of the 2 non-origin points clears the body
			ArrayList<Triangle> ret = new ArrayList<>();
			HashMap<Integer, TriLineIntersect> inters1 = new HashMap<>();
			ArrayList<float[]> line1 = new ArrayList<>();
			looseOctTreeRayIntersectShortCircuit(new Ray(tri1[0], VectorUtil.subVec3(new float[3], tri1[1], tri1[0])), this.getRoot(), ret);
			// if we have an intersection we will need to add a midpoint and try the two resulting triangles separately
			if (ret.size() > 0) {
				ret.clear();
				ArrayList<Triangle> ret1 = new ArrayList<>();
				float[] mid = VectorUtil.midVec3(new float[3], tri1[0], tri1[1]);
				mid = VectorUtil.scaleVec3(mid, mid, 10f);
				tri1 = new float[][]{{points[i], points[i+1], points[i+2]}, 
						{mid[0], mid[1], mid[2]}, 
						{0f, 0f, 0f}}; 
				tri2 = new float[][]{{mid[0], mid[1], mid[2]}, 
						{points[i+3], points[i+4], points[i+5]}, 
						{0f, 0f, 0f}};
				
				// intersect the first triangle
				looseOctTreeTriangleIntersect(new Triangle(tri1), this.getRoot(), ret);
				ret = sortTriangles(ret, tri1[0]);
				looseOctTreeProjectLine(tri1[0], tri1[1], ret, edgeIntercepts, line1, inside, null, null);
				outLine.addAll(line1);
				
				// intersect the second triangle
				looseOctTreeTriangleIntersect(new Triangle(tri2), this.getRoot(), ret1);
				ret1 = sortTriangles(ret1, tri2[0]);
				ArrayList<float[]> line2 = new ArrayList<>();
				looseOctTreeProjectLine(tri2[0], tri2[1], ret1, edgeIntercepts, line2, inside, null, null);
				outLine.addAll(line2);				
			} else {
				// intersect the triangle created from the two polygon points and the origin with the oct tree
				// to get the list of triangles that straddle the boundary of the polygon
				looseOctTreeTriangleIntersect(new Triangle(tri1), this.getRoot(), ret);
				// sort the edge triangles as they will come out of the oct tree unordered
				ret = sortTriangles(ret, tri1[0]);
				// project the line representing the edge of the polygon at this section of the polygon
				// and extract the edge to triangle intercepts
				looseOctTreeProjectLine(tri1[0], tri1[1], ret, edgeIntercepts, line1, inside, null, null);
				
				outLine.addAll(line1);
			}	
		}
		float[][] temp = new float[outLine.size()][];
		for (int i=0; i<outLine.size(); i++) {
			temp[i] = outLine.get(i);
		}
		
		temp = OctTree.removeAdjacentDuplicatePoints(temp);
		return float2DTo1DArray(temp, 3);
	}
	
	
	/**
	 * Method will attempt to fit a Polygon to the mesh in the OctTree.
	 *
	 * @param poly The Polygon to be fit to the mesh
	 * @param edgeTriangles Return of the mesh Triangles intersected by the outline of the Polygon
	 * @param fillTriangles Return of any mesh Triangles that are completely contained within the Polygon
	 * @param tessellatedTriangles Return of the the Triangles resulting from splitting the edgeTriangles and 
	 *   tessellating the portions that are inside the Polygon boundary
	 * @param corners the 3 space coords (xyz) of the corners of a tile (i.e. Decal) may be null
	 * possibly thread-safe
	 */
	public void fitPolygonToShape(Polygon poly, List<Triangle> edgeTriangles, List<Triangle> fillTriangles, List<Triangle> tessellatedTriangles, float[][] corners) {
		if (poly.getPolygonType() != PolygonType.OnBody) {
			log.aprintln("Cannot fit an off body polygon to the body.");
			return;
		}
		if (poly.polyPoints.length < 9) { // we don't have enough points to have a valid input polygon
			log.aprintln("Could not process a polygon with < 3 points");
			return;
		}
		if (edgeTriangles == null || fillTriangles == null || tessellatedTriangles == null) {
			log.aprintln("One of more input Lists are null");
		}
		
		float[][] tri1 = null;
		float[][] tri2 = null;
		HashMap<Integer, Triangle> edgeTris = new HashMap<>();
		HashMap<Integer, TriLineIntersect> edgeIntercepts = new HashMap<>();
		
		float[] points = poly.polyPoints;
		
		// quick test to see if we can short circuit the fitting process for polygons whose vertices
		// are entirely contained within a single facet
		int triId = -1;
		boolean fullyContained = true;
		ArrayList<Triangle> scTris = new ArrayList<>();
		ArrayList<float[]> iPoints = new ArrayList<>();
		// we will use this boolean to piggyback on the following process to determine 
		// if we have the special case of a polygon vertex at one of the poles
		boolean vertOnPole = false;
		for (int j=0; j<points.length-3; j+=3) {
			float[] org = new float[]{points[j], points[j+1], points[j+2]};
			if (isPole(org)) {
				vertOnPole = true;
			}
			// scale up the point and aim the Ray at the origin
			Ray theRay = new Ray(VectorUtil.scaleVec3(org, org, 2f), new float[]{-org[X], -org[Y], -org[Z]});
			looseOctTreeRayIntersectShortCircuit(theRay, this.getRoot(), scTris);
			if (scTris.size() > 0) {
				Triangle t = scTris.get(0);
				if (triId == -1) {
					triId = t.getMeshId();
				}
				if (triId >= 0 && triId == t.getMeshId()) {
					iPoints.add(t.getIntersection());
				} else {
					// we are outside The Pale so eject
					fullyContained = false;
					break;
				}
			}
			scTris.clear();
		}
		if (fullyContained) {			
			ArrayList<Triangle> tris = OctTree.tessellateCoPlanarConvexPolygon(iPoints);
			if (tris.size() > 0) {
				if (triId != -1) {
					for (Triangle t : tris) {
						t.parentId = triId;
					}
				} else {
					// Bad! not sure what to do here...
				}
				tessellatedTriangles.addAll(tris);
			}
			return;
		}		
		
		// using two points of the polygon outline at a time at a time create a line in CCW order
		// elevate the line until it either clears the body or divide it into two lines that clear the body
		// project the line(s) onto the surface of the body and find all the intersected triangles and
		//    the points on their edges where the line intersects them
		for (int i=0; i<points.length-3; i+=3) {
			float[] center = new float[] {0f, 0f, 0f};
			// scale each two consecutive points up sufficiently so a ray between them clears the body
			tri1 = new float[][]{{points[i], points[i+1], points[i+2]}, 
					{points[i+3], points[i+4], points[i+5]}, 
					{0f, 0f, 0f}}; 
								
			tri1[0] = VectorUtil.scaleVec3(tri1[0], tri1[0], 10f);
			tri1[1] = VectorUtil.scaleVec3(tri1[1], tri1[1], 10f);
			tri1[2] = VectorUtil.scaleVec3(tri1[2], tri1[2], 10f);
			
			// OK this is a HACK to support tiling and opacity along Lat/Lon boundaries
			// This entire block of code should be re-factored!!!
			boolean fitToLatLine = false;
			float latZ = 0f;
			// We start the hack by checking to see if two we have a corner array passed in and consecutive points of the polygon that have
			// equal Z coords and are not colocated
			if (corners != null && FloatUtil.isEqual(tri1[0][2], tri1[1][2], MINI_EPSILON) && !VectorUtil.isVec3Equal(tri1[0], 0, tri1[1], 0, MINI_EPSILON)) {
				float[] rayVec = null;
				// Here we continue silly hacking by checking to see whether we are going West to East or East to West with the 
				// current 2 points. We pick our Ray to intersect the body accordingly
				if (VectorUtil.isVec3Equal(corners[0], 0, new float[]{points[i+3], points[i+4], points[i+5]}, 0, FloatUtil.EPSILON)) {
					rayVec = new float[]{points[i+3], points[i+4], points[i+5]}; // East to West
				} else if (VectorUtil.isVec3Equal(corners[1], 0, new float[]{points[i], points[i+1], points[i+2]}, 0, FloatUtil.EPSILON)) {
					rayVec = new float[]{points[i], points[i+1], points[i+2]}; // West to East
				}
				ArrayList<Triangle> intersect = new ArrayList<>();
				
				looseOctTreeRayIntersectShortCircuit(new Ray(rayVec, new float[]{-rayVec[X], -rayVec[Y], -rayVec[Z]}), this.getRoot(), intersect);
//				if (intersect.size() < 1) {
//System.err.println("Latlon intersect failed");					
//					looseOctTreeRayIntersect(new Ray(rayVec, new float[]{-rayVec[X], -rayVec[Y], -rayVec[Z]}), this.getRoot(), intersect);
//				}
				if (!intersect.isEmpty()) {
					// We continue the hack by setting a flag and rotating our intersecting triangle into the plane Latitude by 
					// using the Z coords of the original body intersection points (BIG HACK!)
					fitToLatLine = true;
					float[] surfaceInter = intersect.get(0).getIntersection();
					latZ = surfaceInter[Z];
					tri1[0][2] = latZ;
					tri1[1][2] = latZ;
					tri1[2][2] = latZ;
					center[2] = latZ;
				}
			}

			// need to test if the ray of the 2 non-origin points clears the body
			ArrayList<Triangle> ret = new ArrayList<>();
			ArrayList<float[]> line1 = new ArrayList<>();
			ArrayList<Triangle> inside = new ArrayList<>();
			
			looseOctTreeRayIntersectShortCircuit(new Ray(tri1[0], VectorUtil.subVec3(new float[3], tri1[1], tri1[0])), this.getRoot(), ret);
			// if we have an intersection we will need to add a midpoint and try the two resulting triangles separately
			if (ret.size() > 0) {
				ret.clear();
				ArrayList<Triangle> ret1 = new ArrayList<>();
				float[] mid = VectorUtil.midVec3(new float[3], tri1[0], tri1[1]);
				mid = VectorUtil.scaleVec3(mid, mid, 10f);
				tri1 = new float[][]{{points[0], points[i+1], points[i+2]}, 
						{mid[0], mid[1], mid[2]}, 
						{0f, 0f, 0f}}; 
				tri2 = new float[][]{{mid[0], mid[1], mid[2]}, 
						{points[i+3], points[i+4], points[i+5]}, 
						{0f, 0f, 0f}};
				
				// intersect the first triangle
				looseOctTreeTriangleIntersect(new Triangle(tri1), this.getRoot(), ret);
				ret = sortTriangles(ret, tri1[0]);
				looseOctTreeProjectLine(tri1[0], tri1[1], ret, edgeIntercepts, line1, inside, poly, center);
				for (Triangle tri : ret) {
					edgeTris.put(tri.id, tri);
				}
				
				// intersect the second triangle
				looseOctTreeTriangleIntersect(new Triangle(tri2), this.getRoot(), ret1);
				ret1 = sortTriangles(ret1, tri2[0]);
				ArrayList<float[]> line2 = new ArrayList<>();
				looseOctTreeProjectLine(tri2[0], tri2[1], ret1, edgeIntercepts, line2, inside, poly, center);
				for (Triangle tri : ret1) {
					edgeTris.put(tri.id, tri);
				}
				for (Triangle tri : inside) {
					if (!fillTriangles.contains(tri)) {
						fillTriangles.add(tri);
					}
				}
			} else {
				if (isTriangleDegenerate(tri1)) {
					continue;
				}
				
//				Triangle ta = new Triangle(tri1);
//				
//				final Polygon pt = new Polygon(ta.getId(), OctTree.float2DTo1DArray(ta.points, 3), new float[]{0f, 1f, 0f}, new float[]{0f, 0f, 1f},
//						1, 1.001f, 1.001f, 0.35f, false, true);
//				SwingUtilities.invokeLater(new Runnable() {
//					public void run() {
//						ThreeDManager.getInstance().addRenderable(pt);										}
//				});
//				

				// intersect the triangle created from the two polygon points and the origin with the oct tree
				// to get the list of triangles that straddle the boundary of the polygon
				looseOctTreeTriangleIntersect(new Triangle(tri1), this.getRoot(), ret);
				// sort the edge triangles as they will come out of the oct tree unordered
				ret = sortTriangles(ret, tri1[0]);
				// project the line representing the edge of the polygon at this section of the polygon
				// and extract the edge to triangle intercepts
				looseOctTreeProjectLine(tri1[0], tri1[1], ret, edgeIntercepts, line1, inside, poly, center);
				for (Triangle tri : ret) {
					edgeTris.put(tri.id, tri);
				}
				for (Triangle tri : inside) {
					if (!fillTriangles.contains(tri)) {
						fillTriangles.add(tri);
					}
				}
			}	
		}
		
		
		// now that all the bounding edge triangles of the polygon have been found
		// find any triangle that is inside the polygon edge triangles
		// this triangle(s) will be used as a seed triangle to locate all the polygons
		// inner triangles using a flood fill algorithm
		int foundCount = 0;
		ArrayList<Triangle> found = new ArrayList<>();
		HashMap<Integer, Triangle> marked = new HashMap<>();

			ArrayList<Triangle> inner = findInnerTriangle(edgeTris.values(), OctTree.float1DTo2DArray(points, 3));
			for (Triangle t : inner) {
				if (!edgeTris.containsKey(t.id)) {
					found.add(t);
					foundCount++;
				}
				if (foundCount > 0) {
					break;
				}
			}
		
		// flood fill with the inner polygon
		if (!found.isEmpty()) {
			HashMap<Integer, Triangle> flood = new HashMap<>();
			// mark all the edge triangles
			for (Triangle t : edgeTris.values()) {
				marked.put(t.getId(), t);
			}
			
			ArrayList<Triangle> sResults = new ArrayList<>(); 
			ArrayList<Triangle> seed = new ArrayList<>();
			ArrayList<Triangle> seedQ = new ArrayList<>();
			seed.add(found.get(0));
			float[][] points2D = null;
			if (vertOnPole) {
				points2D = float1DTo2DArray(points, 3);
			}
			do {
				seedQ.clear();
				for (Triangle s : seed) {
					sResults.clear();
					looseGetContactingPolygons(this.getRoot(), s, sResults);
					for (Triangle t : sResults) {
						// special case for a triangle that has a vertex coincident with the pole
						// but otherwise lays outside the polygon
						if (vertOnPole && t.hasVertexOnPole() && !isTriangleContainedWithinPolygon(points2D, t.points, Z)) {
							continue;
						}
						if (!marked.containsKey(t.getId())) {
							flood.put(t.id, t);
							marked.put(t.getId(), t);
							seedQ.add(t);
						}
					}
				}
				seed.clear();
				seed.addAll(seedQ);
				
			} while (seedQ.size() > 0);
			if (flood.size() == 0) { // no other contacting triangles fit inside the polygon	
				flood.put(found.get(0).id, found.get(0));
			}
			fillTriangles.addAll(flood.values());
		}
		
//		float[][] frusPoints = float1DTo2DArray (points, 3);
//		
//		float[][] frustum = getFrustumFromPolygon(frusPoints);
//		
//		float[] frusNorm = VectorUtil.normalizeVec3(avgOf3DPolygon(frusPoints));
//		
//		ArrayList<Triangle> fills = new ArrayList<>();
//		
//		looseOctTreeFrustumIntersectFacets(frusNorm, frustum, this.getRoot(), fills);

		
		// cut the edge triangles so we have a smooth edge and exact fit
		ArrayList<Triangle> edgeTessellation = OctTree.edgeTriangleSubdivideEarClip(edgeIntercepts.values());
		tessellatedTriangles.addAll(edgeTessellation);
		edgeTriangles.addAll(edgeTris.values());
	}
	
	/**
	 * Method to retrieve all the facets that fall within a region defined by the input polygon
	 * projected onto the surface of the mesh. All facets will be returned that either partially
	 * or fully fall within the region. 
	 * @param poly 3D polygon to define the region of interest
	 * @return list of the facets (Triangles) that fall within the region of interest
	 */
	public ArrayList<Triangle> getFacetsByPolygonalRegion(Polygon poly) {
		ArrayList<Triangle> tris = new ArrayList<>();
		if (poly.polyPoints.length < 9) { // we don't have enough points to have a valid input polygon
			log.aprintln("Could not process a polygon with < 3 points");
			return tris;
		}
		
		if (maxDimension >= 1f) {
			for (int j=0; j<poly.polyPoints.length; j++) {
				poly.polyPoints[j] *= (maxDimension * 2f); 
			}
		}
		
		float[][] ppts = OctTree.float1DTo2DArray(poly.getOrigPoints(), 3);
		float[][] frustum = getFrustumFromPolygon(ppts);
		float[] pe = VectorUtil.getPlaneVec3(new float[4], ppts[0], ppts[1], ppts[2], new float[3], new float[3]);
		looseOctTreeFrustumIntersect(VectorUtil.normalizeVec3(new float[3], new float[] {pe[X], pe[Y], pe[Z]}), frustum, this.getRoot(), tris);
//		Frustum jFrustum = new Frustum();
//		Frustum.Plane[] planes = new Frustum.Plane[frustum.length];
//		for (int i=0; i<frustum.length; i++) {
//			Frustum.Plane plane = new Frustum.Plane();
//			plane.n[X] = frustum[i][X];
//			plane.n[Y] = frustum[i][Y];
//			plane.n[Z] = frustum[i][Z];
//			plane.d = frustum[i][W];
//			planes[i] = plane;
//		}
//		jFrustum.updateByPlanes(planes);
//		looseOctTreeJoglFrustumIntersect(VectorUtil.normalizeVec3(new float[3], new float[] {pe[X], pe[Y], pe[Z]}), jFrustum, this.getRoot(), tris);
		return tris;
	}

	/**
	 * Method to retrieve all the facets that fall within a region defined by the input polygon
	 * projected onto the surface of the mesh. All facets will be returned that either partially
	 * or fully fall within the region. 
	 * @param poly 3D polygon to define the region of interest
	 * @return list of the facets (Triangles) that fall within the region of interest
	 */
	public ArrayList<Triangle> getEnclosedFacetsByPolygonalRegion(Polygon poly) {
		ArrayList<Triangle> tris = new ArrayList<>();
		if (poly.polyPoints.length < 9) { // we don't have enough points to have a valid input polygon
			log.aprintln("Could not process a polygon with < 3 points");
			return tris;
		}
		
		boolean needToScaleUp = true;
		// Check for a polygon that is scaled much smaller than the OctTree
		// TODO this should probably be changed at the method caller level but that would mean
		// changing OREX code...not willing to go there at this time...
		for (int i=0; i< poly.polyPoints.length; i++) {
			if (poly.polyPoints[i] > 1.1f) {
				// the polygon is assumed to be scaled to roughly match the shape model 
				needToScaleUp = false;
				break;
			}
		}
		
		if (needToScaleUp) {
			for (int j=0; j<poly.polyPoints.length; j++) {
				poly.polyPoints[j] *= (WORLD_SIZE / 2f); 
			}
		}
		
		float[][] ppts = OctTree.float1DTo2DArray(poly.getOrigPoints(), 3);
		float[][] frustum = getFrustumFromPolygon(ppts);
		float[] pe = VectorUtil.getPlaneVec3(new float[4], ppts[0], ppts[1], ppts[2], new float[3], new float[3]);
		looseOctTreeFrustumIntersect(VectorUtil.normalizeVec3(new float[3], new float[] {pe[X], pe[Y], pe[Z]}), frustum, this.getRoot(), tris);
		
		return tris;
	}

	/**
	 * Method will attempt to fit a Polygon to the mesh in the OctTree.
	 *
	 * @param poly The Polygon to be fit to the mesh
	 * @param edgeTriangles Return of the mesh Triangles intersected by the outline of the Polygon
	 * @param fillTriangles Return of any mesh Triangles that are completely contained within the Polygon
	 * possibly thread-safe
	 */
	public void fitPolygonToShapeNoTessellation(Polygon poly, List<Triangle> edgeTriangles, List<Triangle> fillTriangles) {
		if (poly.getPolygonType() != PolygonType.OnBody) {
			log.aprintln("Cannot fit an off body polygon to the body.");
			return;
		}
		if (poly.polyPoints.length < 9) { // we don't have enough points to have a valid input polygon
			log.aprintln("Could not process a polygon with < 3 points");
			return;
		}
		if (edgeTriangles == null || fillTriangles == null) {
			log.aprintln("One of more input Lists are null");
		}
		
		boolean needToScaleUp = true;
		// Check for a polygon that is scaled much smaller than the OctTree
		// TODO this should probably be changed at the method caller level but that would mean
		// changing OREX code...not willing to go there at this time...
		for (int i=0; i< poly.polyPoints.length; i++) {
			if (poly.polyPoints[i] > 1.1f) {
				// the polygon is assumed to be scaled to roughly match the shape model 
				needToScaleUp = false;
				break;
			}
		}
		
		if (needToScaleUp) {
			for (int j=0; j<poly.polyPoints.length; j++) {
				poly.polyPoints[j] *= (WORLD_SIZE / 2f); 
			}
		}
		
		float[][] tri1 = null;
		float[][] tri2 = null;
		HashMap<Integer, Triangle> edgeTris = new HashMap<>();
		HashMap<Integer, TriLineIntersect> edgeIntercepts = new HashMap<>();
		
		float[] points = poly.polyPoints;
		
		// quick test to see if we can short circuit the fitting process for polygons whose vertices
		// are entirely contained within a single facet
		int triId = -1;
		boolean fullyContained = true;
		ArrayList<Triangle> scTris = new ArrayList<>();
		ArrayList<float[]> iPoints = new ArrayList<>();
		// we will use this boolean to piggyback on the following process to determine 
		// if we have the special case of a polygon vertex at one of the poles
		boolean vertOnPole = false;
		for (int j=0; j<points.length-3; j+=3) {
			float[] org = new float[]{points[j], points[j+1], points[j+2]};
			if (isPole(org)) {
				vertOnPole = true;
			}
			// scale up the point and aim the Ray at the origin
			Ray theRay = new Ray(VectorUtil.scaleVec3(org, org, 2f), new float[]{-org[X], -org[Y], -org[Z]});
			looseOctTreeRayIntersectShortCircuit(theRay, this.getRoot(), scTris);
			if (scTris.size() > 0) {
				Triangle t = scTris.get(0);
				if (triId == -1) {
					triId = t.getMeshId();
				}
				if (triId >= 0 && triId == t.getMeshId()) {
					iPoints.add(t.getIntersection());
				} else {
					// we are outside The Pale so eject
					fullyContained = false;
					break;
				}
			}
			scTris.clear();
		}
		if (fullyContained) {			
			ArrayList<Triangle> tris = OctTree.tessellateCoPlanarConvexPolygon(iPoints);
			if (tris.size() > 0) {
				if (triId != -1) {
					for (Triangle t : tris) {
						t.parentId = triId;
					}
				}
				edgeTriangles.addAll(tris);
			}
			return;
		}		
		
		// using two points of the polygon outline at a time at a time create a line in CCW order
		// elevate the line until it either clears the body or divide it into two lines that clear the body
		// project the line(s) onto the surface of the body and find all the intersected triangles and
		//    the points on their edges where the line intersects them
		for (int i=0; i<points.length-3; i+=3) {
			float[] center = new float[] {0f, 0f, 0f};
			// scale each two consecutive points up sufficiently so a ray between them clears the body
			tri1 = new float[][]{{points[i], points[i+1], points[i+2]}, 
					{points[i+3], points[i+4], points[i+5]}, 
					{0f, 0f, 0f}}; 
								
			tri1[0] = VectorUtil.scaleVec3(tri1[0], tri1[0], 10f);
			tri1[1] = VectorUtil.scaleVec3(tri1[1], tri1[1], 10f);
			tri1[2] = VectorUtil.scaleVec3(tri1[2], tri1[2], 10f);
			
			// OK this is a HACK to support tiling and opacity along Lat/Lon boundaries
			// This entire block of code should be re-factored!!!
			boolean fitToLatLine = false;
			float latZ = 0f;
			// We start the hack by checking to see if two we have a corner array passed in and consecutive points of the polygon that have
			// equal Z coords and are not colocated
//			if (corners != null && FloatUtil.isEqual(tri1[0][2], tri1[1][2], MINI_EPSILON) && !VectorUtil.isVec3Equal(tri1[0], 0, tri1[1], 0, MINI_EPSILON)) {
//				float[] rayVec = null;
//				// Here we continue silly hacking by checking to see whether we are going West to East or East to West with the 
//				// current 2 points. We pick our Ray to intersect the body accordingly
//				if (VectorUtil.isVec3Equal(corners[0], 0, new float[]{points[i+3], points[i+4], points[i+5]}, 0, FloatUtil.EPSILON)) {
//					rayVec = new float[]{points[i+3], points[i+4], points[i+5]}; // East to West
//				} else if (VectorUtil.isVec3Equal(corners[1], 0, new float[]{points[i], points[i+1], points[i+2]}, 0, FloatUtil.EPSILON)) {
//					rayVec = new float[]{points[i], points[i+1], points[i+2]}; // Weat to East
//				}
//				ArrayList<Triangle> intersect = new ArrayList<>();
//				looseOctTreeRayIntersect(new Ray(rayVec, new float[]{-rayVec[X], -rayVec[Y], -rayVec[Z]}), this.getRoot(), intersect);
//				if (!intersect.isEmpty()) {
//					// We continue the hack by setting a flag and rotating our intersecting triangle into the plane Latitude by 
//					// using the Z coords of the original body intersection points (BIG HACK!)
//					fitToLatLine = true;
//					float[] surfaceInter = intersect.get(0).getIntersection();
//					latZ = surfaceInter[Z];
//					tri1[0][2] = latZ;
//					tri1[1][2] = latZ;
//					tri1[2][2] = latZ;
//					center[2] = latZ;
//				}
//			}

			// need to test if the ray of the 2 non-origin points clears the body
			ArrayList<Triangle> ret = new ArrayList<>();
			ArrayList<float[]> line1 = new ArrayList<>();
			ArrayList<Triangle> inside = new ArrayList<>();
			looseOctTreeRayIntersectShortCircuit(new Ray(tri1[0], VectorUtil.subVec3(new float[3], tri1[1], tri1[0])), this.getRoot(), ret);
			// if we have an intersection we will need to add a midpoint and try the two resulting triangles separately
			if (ret.size() > 0) {
				ret.clear();
				ArrayList<Triangle> ret1 = new ArrayList<>();
				float[] mid = VectorUtil.midVec3(new float[3], tri1[0], tri1[1]);
				mid = VectorUtil.scaleVec3(mid, mid, 10f);
				tri1 = new float[][]{{points[0], points[i+1], points[i+2]}, 
						{mid[0], mid[1], mid[2]}, 
						{0f, 0f, 0f}}; 
				tri2 = new float[][]{{mid[0], mid[1], mid[2]}, 
						{points[i+3], points[i+4], points[i+5]}, 
						{0f, 0f, 0f}};
				
				// intersect the first triangle
				looseOctTreeTriangleIntersect(new Triangle(tri1), this.getRoot(), ret);
				ret = sortTriangles(ret, tri1[0]);
				looseOctTreeProjectLine(tri1[0], tri1[1], ret, edgeIntercepts, line1, inside, poly, center);
				for (Triangle tri : ret) {
					edgeTris.put(tri.id, tri);
				}
				
				// intersect the second triangle
				looseOctTreeTriangleIntersect(new Triangle(tri2), this.getRoot(), ret1);
				ret1 = sortTriangles(ret1, tri2[0]);
				ArrayList<float[]> line2 = new ArrayList<>();
				looseOctTreeProjectLine(tri2[0], tri2[1], ret1, edgeIntercepts, line2, inside, poly, center);
				for (Triangle tri : ret1) {
					edgeTris.put(tri.id, tri);
				}
				for (Triangle tri : inside) {
					if (!fillTriangles.contains(tri)) {
						fillTriangles.add(tri);
					}
				}
			} else {
				if (isTriangleDegenerate(tri1)) {
					continue;
				}
				
//				Triangle ta = new Triangle(tri1);
//				
//				final Polygon pt = new Polygon(ta.getId(), OctTree.float2DTo1DArray(ta.points, 3), new float[]{0f, 1f, 0f}, new float[]{0f, 0f, 1f},
//						1, 1.001f, 1.001f, 0.35f, false, true);
//				SwingUtilities.invokeLater(new Runnable() {
//					public void run() {
//						ThreeDManager.getInstance().addRenderable(pt);										}
//				});
//				

				// intersect the triangle created from the two polygon points and the origin with the oct tree
				// to get the list of triangles that straddle the boundary of the polygon
				looseOctTreeTriangleIntersect(new Triangle(tri1), this.getRoot(), ret);
				// sort the edge triangles as they will come out of the oct tree unordered
				ret = sortTriangles(ret, tri1[0]);
				// project the line representing the edge of the polygon at this section of the polygon
				// and extract the edge to triangle intercepts
				looseOctTreeProjectLine(tri1[0], tri1[1], ret, edgeIntercepts, line1, inside, poly, center);
				for (Triangle tri : ret) {
					edgeTris.put(tri.id, tri);
				}
				for (Triangle tri : inside) {
					if (!fillTriangles.contains(tri)) {
						fillTriangles.add(tri);
					}
				}
			}	
		}
		
		
		// now that all the bounding edge triangles of the polygon have been found
		// find any triangle that is inside the polygon edge triangles
		// this triangle(s) will be used as a seed triangle to locate all the polygons
		// inner triangles using a flood fill algorithm
		int foundCount = 0;
		ArrayList<Triangle> found = new ArrayList<>();
		HashMap<Integer, Triangle> marked = new HashMap<>();

			ArrayList<Triangle> inner = findInnerTriangle(edgeTris.values(), OctTree.float1DTo2DArray(points, 3));
			for (Triangle t : inner) {
				if (!edgeTris.containsKey(t.id)) {
					found.add(t);
					foundCount++;
				}
				if (foundCount > 0) {
					break;
				}
			}
		
		// flood fill with the inner polygon
		if (!found.isEmpty()) {
			HashMap<Integer, Triangle> flood = new HashMap<>();
			// mark all the edge triangles
			for (Triangle t : edgeTris.values()) {
				marked.put(t.getId(), t);
			}
			
			ArrayList<Triangle> sResults = new ArrayList<>(); 
			ArrayList<Triangle> seed = new ArrayList<>();
			ArrayList<Triangle> seedQ = new ArrayList<>();
			seed.add(found.get(0));
			float[][] points2D = null;
			if (vertOnPole) {
				points2D = float1DTo2DArray(points, 3);
			}
			do {
				seedQ.clear();
				for (Triangle s : seed) {
					sResults.clear();
					looseGetContactingPolygons(this.getRoot(), s, sResults);
					for (Triangle t : sResults) {
						// special case for a triangle that has a vertex coincident with the pole
						// but otherwise lays outside the polygon
						if (vertOnPole && t.hasVertexOnPole() && !isTriangleContainedWithinPolygon(points2D, t.points, Z)) {
							continue;
						}
						if (!marked.containsKey(t.getId())) {
							flood.put(t.id, t);
							marked.put(t.getId(), t);
							seedQ.add(t);
						}
					}
				}
				seed.clear();
				seed.addAll(seedQ);
				
			} while (seedQ.size() > 0);
			if (flood.size() == 0) { // no other contacting triangles fit inside the polygon	
				flood.put(found.get(0).id, found.get(0));
			}
			fillTriangles.addAll(flood.values());
		}
		
//		float[][] frusPoints = float1DTo2DArray (points, 3);
//		
//		float[][] frustum = getFrustumFromPolygon(frusPoints);
//		
//		float[] frusNorm = VectorUtil.normalizeVec3(avgOf3DPolygon(frusPoints));
//		
//		ArrayList<Triangle> fills = new ArrayList<>();
//		
//		looseOctTreeFrustumIntersectFacets(frusNorm, frustum, this.getRoot(), fills);

		
//		// cut the edge triangles so we have a smooth edge and exact fit
//		ArrayList<Triangle> edgeTessellation = OctTree.edgeTriangleSubdivideEarClip(edgeIntercepts.values());
//		tessellatedTriangles.addAll(edgeTessellation);
		edgeTriangles.addAll(edgeTris.values());
	}


	/**
	 * Attempts to tightly fit a polygon to the surface of the mesh contained in the octtree
	 * If the angular separation of any two consecutive points about the body origin are more than 180 degrees apart
	 * this method will return garbage
	 * 
	 * @param pts array of 3D points that define the polygon, must be in counter-clockwise winding order
	 * @return the fitted points defining the polygon outline and all the Triangles that compose the filled polygon
	 *
	 * possibly thread-safe
	 */
	public FittedPolygonData looseFitPolygon(float[] pts) {
		FittedPolygonData data = null;
		float[][] tri1 = null;
		float[][] tri2 = null;
		ArrayList<Triangle> allTriangles = new ArrayList<>();
		HashMap<Integer, Triangle> edgeTriangles = new HashMap<>();
		HashMap<Integer, TriLineIntersect> edgeIntercepts = new HashMap<>();
		ArrayList<float[]> outLine = new ArrayList<>();
		
		if (pts.length < 9) { // we don't have enough points to have a valid input polygon
			log.aprintln("Could not process a polygon with < 3 points");
			return data;
		} else {
			data = new FittedPolygonData(pts);
		}
		
		float[] points = pts;
		
		// quick test to see if we can short circuit the fitting process for polygons whose vertices
		// are entirely contained within a single facet
		int triId = -1;
		boolean fullyContained = true;
		ArrayList<Triangle> scTris = new ArrayList<>();
		ArrayList<float[]> iPoints = new ArrayList<>();
		for (int j=0; j<points.length-3; j+=3) {
			float[] org = new float[]{points[j], points[j+1], points[j+2]};
			// scale up the point and aim the Ray at the origin
			Ray theRay = new Ray(VectorUtil.scaleVec3(org, org, 2f), new float[]{-org[X], -org[Y], -org[Z]});
			looseOctTreeRayIntersectShortCircuit(theRay, this.getRoot(), scTris);
			if (scTris.size() > 0) {
				Triangle t = scTris.get(0);
				if (triId == -1) {
					triId = t.getMeshId();
				}
				if (triId >= 0 && triId == t.getMeshId()) {
					iPoints.add(t.getIntersection());
				} else {
					// we are outside The Pale so eject
					fullyContained = false;
					break;
				}
			}
			scTris.clear();
		}
		if (fullyContained) {			
			data.setPoints(iPoints);
			ArrayList<Triangle> tris = OctTree.tessellateCoPlanarConvexPolygon(iPoints);
			// TODO need to set the id!!!!!
			if (tris.size() > 0) {
				if (triId != -1) {
					for (Triangle t : tris) {
						t.parentId = triId;
					}
				} else {
					// Bad! not sure what to do here...
				}
				data.setTris(tris);
			}
			return data;
		}
		
		// SLD: Declare these once, rather than recreating every loop
		ArrayList<Triangle> ret = new ArrayList<>();
		ArrayList<float[]> line1 = new ArrayList<>();
		ArrayList<Triangle> ret1 = new ArrayList<>();
		ArrayList<Triangle> inside = new ArrayList<>();
		
		// using two points of the polygon outline at a time at a time create a line in CCW order
		// elevate the line until it either clears the body or divide it into two lines that clear the body
		// project the line(s) onto the surface of the body and find all the intersected triangles and
		//    the points on their edges where the line intersects them
		for (int i=0; i<points.length-3; i+=3) {
			// scale each two consecutive points up sufficiently so a ray between them clears the body
			tri1 = new float[][]{{points[i], points[i+1], points[i+2]}, 
					{points[i+3], points[i+4], points[i+5]}, 
					{0f, 0f, 0f}}; 
			
			tri1[0] = VectorUtil.scaleVec3(tri1[0], tri1[0], 10f);
			tri1[1] = VectorUtil.scaleVec3(tri1[1], tri1[1], 10f);
			tri1[2] = VectorUtil.scaleVec3(tri1[2], tri1[2], 10f);
			
			// need to test if the ray of the 2 non-origin points clears the body
			ret.clear();
			line1.clear();
			inside.clear();
			
			// need to test if the ray of the 2 non-origin points clears the body
			looseOctTreeRayIntersectShortCircuit(new Ray(tri1[0], VectorUtil.subVec3(new float[3], tri1[1], tri1[0])), this.getRoot(), ret);
			// if we have an intersection we will need to add a midpoint and try the two resulting triangles separately
			if (ret.size() > 0) {
				ret.clear();
				ret1.clear();
				float[] mid = VectorUtil.midVec3(new float[3], tri1[0], tri1[1]);
				mid = VectorUtil.scaleVec3(mid, mid, 10f);
				tri1 = new float[][]{{points[i-3], points[i-2], points[i-1]}, 
						{mid[0], mid[1], mid[2]}, 
						{0f, 0f, 0f}}; 
				tri2 = new float[][]{{mid[0], mid[1], mid[2]}, 
						{points[i], points[i+1], points[i+2]}, 
						{0f, 0f, 0f}};
				
				// intersect the first triangle
				looseOctTreeTriangleIntersect(new Triangle(tri1), this.getRoot(), ret);

				ret = sortTriangles(ret, tri1[0]);
				looseOctTreeProjectLine(tri1[0], tri1[1], ret, edgeIntercepts, line1, inside, null, null);
				for (Triangle tri : ret) {
					edgeTriangles.put(tri.id, tri);
				}
				outLine.addAll(line1);
				
				// intersect the second triangle
				looseOctTreeTriangleIntersect(new Triangle(tri2), this.getRoot(), ret1);
				
				ret1 = sortTriangles(ret1, tri2[0]);
				ArrayList<float[]> line2 = new ArrayList<>();
				looseOctTreeProjectLine(tri2[0], tri2[1], ret1, edgeIntercepts, line2, inside, null, null);
				for (Triangle tri : ret1) {
					edgeTriangles.put(tri.id, tri);
				}
				outLine.addAll(line2);				
				allTriangles.addAll(inside);
			} else {
				// intersect the triangle created from the two polygon points and the origin with the oct tree
				// to get the list of triangles that straddle the boundary of the polygon
				looseOctTreeTriangleIntersect(new Triangle(tri1), this.getRoot(), ret);
				// sort the edge triangles as they will come out of the oct tree unordered
				ret = sortTriangles(ret, tri1[0]);
				// project the line representing the edge of the polygon at this section of the polygon
				// and extract the edge to triangle intercepts
				looseOctTreeProjectLine(tri1[0], tri1[1], ret, edgeIntercepts, line1, inside, null, null);
				for (Triangle tri : ret) {
					edgeTriangles.put(tri.id, tri);
				}
				outLine.addAll(line1);
				allTriangles.addAll(inside);
			}	
		}
		
		// now that all the bounding edge triangles of the polygon have been found
		// find any triangle that is inside the polygon edge triangles
		// this triangle(s) will be used as a a seed triangle to locate all the polygons
		// inner triangles using a flood fill algorithm
		int foundCount = 0;
		ArrayList<Triangle> found = new ArrayList<>();
		HashMap<Integer, Triangle> marked = new HashMap<>();

			ArrayList<Triangle> inner = findInnerTriangle(edgeTriangles.values(), OctTree.float1DTo2DArray(points, 3));
			for (Triangle t : inner) {
				if (!edgeTriangles.containsKey(t.id)) {
					found.add(t);
					foundCount++;
				}
				if (foundCount > 0) {
					break;
				}
			}
		
		// flood fill with the inner polygon
		if (!found.isEmpty()) {
			ArrayList<Triangle> flood = new ArrayList<>();
			// mark all the edge triangles
			for (Triangle poly : edgeTriangles.values()) {
				marked.put(poly.getId(), poly);
			}
			
			ArrayList<Triangle> sResults = new ArrayList<>(); 
			ArrayList<Triangle> seed = new ArrayList<>();
			ArrayList<Triangle> seedQ = new ArrayList<>();
			seed.add(found.get(0));
			do {
				seedQ.clear();
				for (Triangle s : seed) {
					sResults.clear();
					looseGetContactingPolygons(this.getRoot(), s, sResults);
					for (Triangle t : sResults) {
						if (!marked.containsKey(t.getId())) {
							flood.add(t);
							marked.put(t.getId(), t);
							seedQ.add(t);
						}
					}
				}
				seed.clear();
				seed.addAll(seedQ);
				
			} while (seedQ.size() > 0);
			
			if (flood.size() == 0) { // no other contacting triangles fit inside the polygon	
				flood.add(found.get(0));
			}
			allTriangles.addAll(flood);
		}
//		 cut the edge triangles so we have a smooth edge and exact fit
		ArrayList<Triangle> edgeTessellation = OctTree.edgeTriangleSubdivideEarClip(edgeIntercepts.values());
		allTriangles.addAll(edgeTessellation);
		data.setPoints(outLine);
		data.setTris(allTriangles);
		return data;
	}
	
	public FittedPolygonData fitPolygon(float[] pts) {
		FittedPolygonData data = null;
		if (pts.length < 9) { // we don't have enough points to have a valid input polygon
			log.aprintln("Could not process a polygon with < 3 points");
			return data;
		} else {
			data = new FittedPolygonData(pts);
		}

		
		ArrayList<Triangle> allTris = new ArrayList<>();
		
		float[] polyPts;
		if (!VectorUtil.isVec3Equal(pts, 0, pts, pts.length - 3, FloatUtil.EPSILON)) {
			// close the polygon if needed
			polyPts = new float[pts.length + 3];
			for (int i=0; i<polyPts.length-3; i++) {
				polyPts[i] = pts[i];
			}
			polyPts[polyPts.length - 3] = polyPts[0];
			polyPts[polyPts.length - 2] = polyPts[1];
			polyPts[polyPts.length - 1] = polyPts[2];				
		} else {
			polyPts = new float[pts.length];
			for (int i=0; i<polyPts.length; i++) {
				polyPts[i] = pts[i];
			}
		}

		// a copy of the polygon that is unscaled
		float[][] polyOrg = new float[pts.length / 3][];
		float[][] poly = new float[pts.length / 3][];

		polyOrg = OctTree.float1DTo2DArray(pts, 3);
		poly = OctTree.float1DTo2DArray(pts, 3);

		// Scale up the polygon for point by point intersection tests
		ArrayList<Integer> corners = new ArrayList<>();
		if (Util.EQUAT_RADIUS > 1d) {
			for (int i = 0; i < poly.length; i++) {
				poly[i] = VectorUtil.scaleVec3(poly[i], poly[i], ((float) Util.EQUAT_RADIUS * (float) Util.EQUAT_RADIUS));
			}
		} // find and identify all corner point triangles
		ArrayList<Triangle> interTris = new ArrayList<>();
		for (float[] pp : poly) {
			interTris.clear();
			Ray ray = new Ray(pp, new float[] { -pp[0], -pp[1], -pp[2] });
			looseOctTreeRayIntersectShortCircuit(ray, root, interTris);
			if (interTris.size() > 0) {
				for (Triangle cTri : interTris) {
					if (!corners.contains(cTri.id)) {
						corners.add(cTri.id);
					}
				}
			}
		}

		// Frustum intersection test to find all the facets that are enclosed in the polygon
		float[] normal = OctTree.avgOf3DPolygon(poly);
		normal = VectorUtil.normalizeVec3(new float[3], normal);
		float[][] frus = OctTree.getFrustumFromPolygon(poly);
		System.err.println();
		
		for (int i=0; i<frus.length; i++) {
			System.err.println(""+frus[i][0]+", "+frus[i][1]+", "+frus[i][2]+", "+frus[i][3]+", ");
		}
		System.err.println();
		
		HashMap<Integer, Triangle> tris = new HashMap<>();
		looseOctTreeFrustumIntersect(normal, frus, root, tris);

		ArrayList<Triangle> inside = new ArrayList<>();
		ArrayList<Triangle> edge = new ArrayList<>();

		// test to see which facets are completely enclosed and which are partially enclosed
		for (Triangle t : tris.values()) {
			Visibility v = OctTree.checkTriangleAgainstAnyFrustum(t, frus);
			if (v == Visibility.SomeClip) {
				edge.add(t);
			} else if (v == Visibility.NoClip) {
				inside.add(t);
			}
		}

		allTris.addAll(inside);

		HashMap<Integer, ClippingPolygon> edgePolys = new HashMap<>();
		HashMap<Integer, ClippingPolygon> cornerPolys = new HashMap<>();
		// for each line segment in the polygon
		for (int i = 0; i < polyOrg.length; i++) {
			float[] pt1;
			float[] pt2;
			if (i == polyOrg.length - 1) {
				pt1 = polyOrg[i];
				pt2 = polyOrg[0];
			} else {
				pt1 = polyOrg[i];
				pt2 = polyOrg[i + 1];
			}

			// determine line segment direction 
			float[] lineDir = VectorUtil.normalizeVec3(VectorUtil.subVec3(new float[3], pt2, pt1));

			// Clip the edge triangles down to fit in the polygon
			for (Triangle tr : edge) {
				// quick check to determine if this facet has only one vertex touching the edge of the polygon
				// if so, its considered outside the polygon
				boolean out = true;
				for (float[] ep : tr.points) {
					if (OctTree.pointLeftOfCcwLineUseDoubles(pt1, pt2, ep)) {
						out = false;
						break;
					}
				}
				if (out) {
					continue;
				}
				// start building the mark up class (ClippingPolygon) 
				float[] planeNormal = null;
				boolean isCorner = false;
				float[][] points = null;
				ClippingPolygon cp;
				if (cornerPolys.containsKey(tr.id)) {
					cp = cornerPolys.get(tr.id);
					points = cornerPolys.get(tr.id).points;
					isCorner = true;
					planeNormal = cornerPolys.get(tr.id).normal;
				} else {
					cp = new ClippingPolygon();
					for (int idx : corners) {
						if (tr.id == idx) {
							cp.id = tr.id;
							points = tr.points;
							isCorner = true;
							break;
						}
					}
					if (!isCorner) {
						if (edgePolys.containsKey(tr.id)) {
							cp = edgePolys.get(tr.id);
							points = cp.points;
							isCorner = false;
							planeNormal = edgePolys.get(tr.id).normal;
						} else {
							cp.id = tr.id;
							points = tr.points;
							isCorner = false;
						}
					}
				}

				if (points == null) {
					points = tr.points;
				}
				Integer co = 0;
				float[] source = new float[3];
				float[] target = new float[3];
				float[] start = new float[3];
				float[] end = new float[3];
				// find where the line segment intersects the facet (triangle)
				// this is done by creating a another triangle consisting of the line segment and the center of the body
				// and then intersecting this triangle with the facet
				float[] point1 = VectorUtil.scaleVec3(new float[3], pt1, 2f);
				float[] point2 = VectorUtil.scaleVec3(new float[3], pt2, 2f);
				int intersect = tri_tri_intersection_test_3d(point1, point2, new float[] { 0f, 0f, 0f }, tr.points[0],
						tr.points[1], tr.points[2], co, source, target);
				if (co == 0 && intersect == 1) {
					// Check for intersections that only hit a single vertex
					if (VectorUtil.isVec3Equal(source, 0, target, 0, 0.00001f) || VectorUtil.isVec3Zero(source, 0)
							|| VectorUtil.isVec3Zero(target, 0)) {
						continue;
					}

					// Sometimes tri_tri_intersection_test_3d() returns the intersect points in the
					// reverse order so fix if needed
					if (VectorUtil.dotVec3(
							VectorUtil.normalizeVec3(VectorUtil.subVec3(new float[3], target, source)),
							lineDir) < 0) {
						start = target;
						end = source;
					} else {
						start = source;
						end = target;
					}

					// check for co-linearity
					boolean colinear = false;
					for (int x = 0; x < points.length; x++) {
						if (x == points.length - 1) {
							colinear = OctTree.areLineSegmentsCollinear(start, end, points[points.length - 1],
									points[0]);
						} else {
							colinear = OctTree.areLineSegmentsCollinear(start, end, points[x], points[x + 1]);
						}
					}

					if (colinear) {
						continue;
					}

					cp.corners.add(start);
					cp.corners.add(end);

					if (planeNormal == null) {
						planeNormal = VectorUtil.crossVec3(new float[3],
								VectorUtil.subVec3(new float[3], points[1], points[0]),
								VectorUtil.subVec3(new float[3], points[2], points[0]));
					}
					// finish populating the ClippingPolygon
					
					if (cp.normal == null) {
						cp.normal = planeNormal;
						cp.points = points;
						cornerPolys.put(tr.id, cp);
					} else {
						cp.id = tr.id;
						cp.normal = planeNormal;
						cp.points = points;
						cornerPolys.put(tr.id, cp);
					}

				} else if (co == 1 && intersect == 1) {
					log.aprintln("Coplanar triangle/triangle intersection - try edge/edge test");
				} else if (co == 0 && intersect == 0) {
					continue;
				} else if (co == 1 && intersect == 0) {
					continue;
				}

			}

			// clip all the edge polygons and bin them accordingly by parent facet id
			// generate a curve from each corner polygon and then clip points outside the curve
			for (int key : cornerPolys.keySet()) {
				ClippingPolygon cp = cornerPolys.get(key);
				// build a concave curve
				ArrayList<float[]> curve = cp.corners;
				// convert curve to array
				float[][] curvePts = new float[curve.size()][];
				int idx = 0;
				for (float[] p : curve) {
					curvePts[idx++] = p;
				}

				float[][] editPts = OctTree.removeAdjacentDuplicatePoints(curvePts);

				float[] cpNorm = VectorUtil.normalizeVec3(new float[3], cp.normal);

				int axisToDrop = OctTree.fastMostOrthoganalAxisToPlane(cpNorm);

				// clip points outside the curve
				for (int k = 0; k < editPts.length - 1; k++) {
					clipFacetByLineIndexed(cp.points, editPts[k], editPts[k + 1], cp.inside);
				}

				float[][] eTmp = new float[editPts.length + cp.inside.size()][];
				int j;
				for (j = 0; j < editPts.length; j++) {
					eTmp[j] = editPts[j];
				}
				for (int pt : cp.inside) {
					eTmp[j++] = cp.points[pt];
				}
				
		        // rotate the polygon so that its surface normal vector and the Z axis are coincident prior to sorting and ear clipping	        		
        		// calculate the angle between the planeNormal and the Z axis in radians
        		float angle = VectorUtil.angleVec3(cpNorm, OctTree.Z_AXIS);
        		// calculate the axis of the needed rotation to put the planeNormal coincident to the Z axis
        		float[] rotationAxis = VectorUtil.crossVec3(new float[3], cpNorm, OctTree.Z_AXIS);
        		// craete a quaternion
        		Quaternion quat = new Quaternion(rotationAxis[X], rotationAxis[Y], rotationAxis[Z], angle);
        		
        		float[][] rot3Dpts = new float[eTmp.length][];
        		// rotate the polygon vectors to be 
        		for (int z=0; z<eTmp.length; z++) {
        			rot3Dpts[z] = quat.rotateVector(new float[3], 0, eTmp[z], 0);
        		}
	        			        		
	        	float[][] twoDpts = OctTree.project3DTo2D(rot3Dpts, Z);
	        			
        		// need to sort threeDpts into CCW here
    			int[] sorted = OctTree.sortPointsCCW(twoDpts, true);
        		
        		float[][] twoDtmp = new float[twoDpts.length][];
        		float[][] threeDtmp = new float[eTmp.length][];
    			for(int k=0; k<sorted.length; k++) {
    				twoDtmp[k] = twoDpts[sorted[k]];
    				threeDtmp[k] = eTmp[sorted[k]];
    			}
    			twoDpts = twoDtmp;
    			eTmp = threeDtmp;

				EarClippingTriangulator ect = new EarClippingTriangulator();
				int[] indices = ect.computeTrianglesReturnAsInts(OctTree.float2DTo1DArray(twoDpts, 2));

				float[][] newTris = new float[indices.length][];
				for (int x = 0; x < indices.length; x++) {
					newTris[x] = eTmp[indices[x]];
				}
				for (int y = 0; y < newTris.length; y += 3) {
					float[][] currTri = new float[][] { newTris[y], newTris[y + 1], newTris[y + 2] };
					// reorder the points based on the indices returned from ear clipping
					currTri = OctTree.correctTriangleWindingOrder(currTri, cpNorm);
					Triangle t = new Triangle(new float[][] { currTri[0], currTri[1], currTri[2] });
					if (OctTree.isTriangleDegenerate(t.points)) {
						continue;
					}
					t.parentId = key;
					allTris.add(t);
				}
			}
		}
		data.setTris(allTris);
		return data;
	}
	
	private static void clipFacetByLineIndexed(float[][] facet, float[] start, float[] end, TreeSet<Integer> inside) {
		float[] lineNorm = VectorUtil.crossVec3(new float[3], start, end);
		lineNorm = VectorUtil.normalizeVec3(lineNorm);
		for (int i = 0; i < facet.length; i++) {
			if (VectorUtil.dotVec3(lineNorm, VectorUtil.subVec3(new float[3], facet[i], start)) < 0) {
				inside.remove(i);
			}
		}
	}

	private static float[][] clipFacetByLine(float[][] facet, float[] start, float[] end) {
		float[] lineNorm = VectorUtil.crossVec3(new float[3], start, end);
		lineNorm = VectorUtil.normalizeVec3(lineNorm);
		ArrayList<float[]> inPts = new ArrayList<float[]>();
		boolean first = true;
		for (int i = 0; i < facet.length; i++) {
			if (VectorUtil.dotVec3(lineNorm, facet[i]) >= 0) {
				inPts.add(facet[i]);
			} else if (first) {
				inPts.add(start);
				inPts.add(end);
				first = false;
			}
		}
		float[][] ret = null;
		if (inPts.size() > 0) {
			ret = new float[inPts.size()][];
			int j = 0;
			for (float[] p : inPts) {
				ret[j++] = p;
			}
		}
		return ret;
	}


	// Returns the number of objects within the subtree q which contact
	// the given object. Increments ObjectsChecked for each object which
	// is tested for contact.
	private int looseGetContactingObjects(ONode q, Triangle o, ArrayList<Triangle> found)
	{
		nodesChecked++;
		
		float halfSize = looseK * WORLD_SIZE / (2 << q.depth);

		int	count = 0;
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (q.child[k][j][i] != null) {
						count += looseGetContactingObjects(q.child[k][j][i], o, found);
					}
				}
			}
		}

		// Check objects in this node.
		Triangle p = q.object;
		while (p != null) {
			if (o != p && (noDivTriTriIsect(o.points[0], o.points[1], o.points[2], p.points[0], p.points[1], p.points[2]) ||
					triTriCornerIntersect(o.points, p.points))) {
				count++;
				found.add(p);
			}
			
			objectsChecked++;
			p = p.next;
		}

		return count;
	}
	
	/**
	 * Prints the number of nodes at each depth for the Octtree to System.err
	 *
	 * thread-safe
	 */
	public void printDepthTotals()
	{
		log.aprintln("Oct Tree Depth Totals:");
		
		int	i;
		for (i = 0; i < MAX_DEPTH; i++) {
			if (depthTotals[i] != 0) {
				log.aprintln("ct["+i+"] = "+depthTotals[i]);
			}
		}
	}

	/**
	 * Returns the root ONode of the OctTree
	 *
	 * @return root node
	 *
	 * thread-safe
	 */
	public ONode getRoot() {
		return root;
	}

	/**
	 * Search an ONode and all its children for Triangles that are intersected by the input Ray
	 *
	 * @param ray the Ray to intersect the node with
	 * @param n the ONode to test for intersection
	 * @param list List of all the Triangles intersected by the Ray
	 *
	 * possibly thread-safe
	 */
	public void looseOctTreeRayIntersect(Ray ray, ONode n, ArrayList<Triangle> list) {
		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						looseOctTreeRayIntersect(ray, n.child[k][j][i], list);
					}
				}
			}
		}

		// Count objects in this node.
		Triangle o = n.object;
		while (o != null) {
			float[] bary = new float[]{0f, 0f, 0f};
			float closestIntersection = 1f;
			if (rayTriangleIntersect(ray.getOrigin(), ray.getDirection(), o.points[0], o.points[1], o.points[2], bary, closestIntersection) != Float.MAX_VALUE) {
				o.setIntersection(bary);
				list.add(o);
			}	
			o = o.next;
		}
	}
	
	/**
	 * Search an ONode and all its children for Triangles that are intersected by the input Ray
	 *
	 * @param ray the Ray to intersect the node with
	 * @param n the ONode to test for intersection
	 * @param list List of all the Triangles intersected by the Ray
	 *
	 * possibly thread-safe
	 */
	public void looseOctTreeRayIntersectShortCircuit(Ray ray, ONode n, ArrayList<Triangle> list) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		
		com.jogamp.opengl.math.Ray nRay = new com.jogamp.opengl.math.Ray();
		nRay.orig[X] = ray.origin[X];
		nRay.orig[Y] = ray.origin[Y];
		nRay.orig[Z] = ray.origin[Z];
		float[] direction = VectorUtil.normalizeVec3(new float[3], ray.direction);
		nRay.dir[X] = direction[X];
		nRay.dir[Y] = direction[Y];
		nRay.dir[Z] = direction[Z];
		
		AABBox box = n.getAABbox(halfSize);

		if (!box.intersectsRay(nRay)) {
			return;
		}

		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						looseOctTreeRayIntersectShortCircuit(ray, n.child[k][j][i], list);
					}
				}
			}
		}

		// Count objects in this node.
		Triangle o = n.object;
		while (o != null) {
			float[] bary = new float[]{0f, 0f, 0f};
			float closestIntersection = 1f;
			
			if (rayTriangleIntersect(ray.origin, ray.direction, o.points[0], o.points[1], o.points[2], bary, closestIntersection) != Float.MAX_VALUE) {
				o.setIntersection(bary);
				list.add(o);
			} 
			o = o.next;
		}
	}



	/*
	 * Tests for intersection of the input Ray with the axis-aligned bounding box of the input ONode
	 *
	 * @param ray the Ray to test
	 * @param n the ONode to intersect test
	 * @return true if the Ray intersects the axis-aligned bounding box of the ONode
	 *
	 * probably thread-safe
	 */
	private boolean looseRayNodeIntersectTest(Ray ray, ONode n) {	
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		float intersects = rayIntersectBox(ray.origin, ray.direction, n.cx, n.cy, n.cz, halfSize, null);
		if (Float.compare(intersects, Float.MAX_VALUE) < 0) {
			return true;
		} else {
			return false;
		}
	}
	
	/*
	 * This method implements the ray triangle intersection algorithm as proposed by Tomas Moller and Ben Trumbore in 1997.
	 * This code is a port of their reference implementation written in C.
	 * Their original paper and reference implementation can be found @ http://www.acm.org/jgt/papers/MollerTrumbore97/
	 *
	 * @param orig	ray origin
	 * @param dir	ray direction
	 * @param vert0	first triangle vertex assume CCW ordering
	 * @param vert1	second triangle vertex
	 * @param vert2	third triangle vertex
	 * @param t	distance from the translated ray to the triangle plane
	 * @param u	represents the first 2D cooord of the intersection point within the triangle
	 * @param v	represents the second 2D cooord of the intersection point within the triangle
	 * @return	true if ray intersects triangle
	 * not thread safe
	 */
	
	private static boolean rayIntersectTriangle(Vector3D orig, Vector3D dir, Vector3D vert0, Vector3D vert1, Vector3D vert2, Double t, Double u, Double v) {
		Vector3D edge1 = new Vector3D();
		Vector3D edge2 = new Vector3D();
		Vector3D tvec = new Vector3D();
		Vector3D pvec = new Vector3D();
		Vector3D qvec = new Vector3D();
		double det = 0, invDet = 0;
		
		// find vectors for two edges sharing vert0
		edge1 = Vector3D.OpSubtraction(vert1, vert0);
		edge2 = Vector3D.OpSubtraction(vert2, vert0);
		
		// begin calculating determinant - also used to calculate U parameter
		pvec = dir.cross(edge2);
		
		// if determinant is near zero, ray lies in plane of triangle
		det = edge1.dot(pvec);
		
		if (TEST_CULL) {	// culling is desired
			if (det < EPSILON) {
				return false;
			}
			
			// calculate distance from vert0 to ray origin
			tvec = Vector3D.OpSubtraction(orig, vert0);
			
			// calculate U parameter and test bounds
			u = tvec.dot(pvec);
			if (u < 0.0 || u > det) {
				return false;
			}
			
			// prepare to test V parameter
			qvec = tvec.cross(edge1);
			
			// calculate V parameter and test bounds
			v = dir.dot(qvec);
			if (v < 0.0 || (u + v) > det) {
				return false;
			}
			
			// calculate t, scale parameters, ray intersects triangle
			t = edge2.dot(qvec);
			invDet = 1.0 / det;
			t *= invDet;
			u *= invDet;
			v *= invDet;
		} else {	// skip culling
			if (det > -EPSILON && det < EPSILON) {
				return false;
			}
			
			invDet = 1.0 / det;
			
			// calculate distance from vert0 to ray origin
			tvec = Vector3D.OpSubtraction(orig, vert0);
			
			// calculate the U parameter and test bounds
			u = tvec.dot(pvec) * invDet;
			if (u < 0.0 || u > 1.0) {
				return false;
			}
			
			// prepare to test V parameter
			qvec = tvec.cross(edge1);
			
			// calculate V parameter and test bounds
			v = dir.dot(qvec) * invDet;
			if (v < 0.0 || u + v > 1.0) {
				return false;
			}
			
			// calculate t since the ray intersects the triangle
			t = edge2.dot(qvec) * invDet;
		}
				
		return true;
	}

	/**
	 * This method implements the ray triangle intersection test presented in
	 * "3D Math Primer for Graphics and Game Development, 2nd Edition" by Fletcher Dunn and Ian Parberry
	 * ISBN-13: 978-1568817231
	 * ISBN-10: 1568817231
	 *
	 * @param rayOrg 3D point of origin for the ray
	 * @param rayDir ray direction in 3D
	 * @param p0 1st triangle vertex
	 * @param p1 2nd triangle vertex
	 * @param p2 3rd triangle vertex
	 * @param intersection point in the triangle IF intersection occurs
	 * @param minT qualitative measure of the "goodness" of the intersection
	 * @return the parametric point of intersection
	 *
	 * thread-safe
	 */
	public static float rayTriangleIntersect (
			float[] rayOrg,				// origin of the ray
			float[] rayDir,				// ray direction 
			float[] p0, 				// triangle vertices
			float[] p1, 				// ...
			float[] p2, 				// ...
			float[] intersection,		// the intersection point IF an intersection is found
			float minT) {				// closest intersection found so far (start with 1.0)

		
		// return large number if no intersection found
		float kNoIntersection = Float.MAX_VALUE;
		
		// compute clockwise edge vectors
		float[] e1 = Vec3dMath.subtract(p1, p0);
		float[] e2 = Vec3dMath.subtract(p2, p1);
		
		// compute surface normal (unnormalized)
		float[] n = Vec3dMath.cross(e1, e2);
		
		// compute gradient, tells us how steep of an approach angle to the "front" side of the triangle
		float dot = Vec3dMath.dot(n, rayDir);
		
		// check for a ray that is parallel to the triangle or not pointing towards the "front" of the triangle
		// this will also reject degenerate triangles and rays
		if (!(dot < 0.0f)) {
			return kNoIntersection;
		}
		
		// compute d value for the plane equation. use the plane equation with d on the right side:
		// Ax + By + Cz = d
		float d = Vec3dMath.dot(n, p0);
		
		// compute the parametric point of intersection with the plane containing the triangle,
		// check at earliest stage for trivial rejection
		float t = d - Vec3dMath.dot(n, rayOrg);
		
		// is ray origin on the backside of the triangle?
		if (!(t <= 0.0f)) {
			return kNoIntersection;
		}
		
		// closer intersection already found? or ray doesn't reach the plane?
		if (!(t >= dot* minT)) {
			return kNoIntersection;
		}
		
		// ray intersects the plane. compute parametric point of intersection.
		t /= dot;
		if (!(t >= 0.0f) || !(t <= minT)) {
			return kNoIntersection;
		}
		
		// compute 3D point of intersection
		float[] p = Vec3dMath.add(rayOrg, Vec3dMath.multiply(t, rayDir));
		 		
		// find dominate axis to select which plane to select onto. 
		// and compute u's and v's
		float u0, u1, u2 = 0f;
		float v0, v1, v2 = 0f;
		
		if (Math.abs(n[0]) > Math.abs(n[1])) {
			if (Math.abs(n[0]) > Math.abs(n[2])) {
				u0 = p[1] - p0[1];
				u1 = p1[1] - p0[1];
				u2 = p2[1] - p0[1];
				
				v0 = p[2] - p0[2];
				v1 = p1[2] - p0[2];
				v2 = p2[2] - p0[2];
			} else {
				u0 = p[0] - p0[0];
				u1 = p1[0] - p0[0];
				u2 = p2[0] - p0[0];
				
				v0 = p[1] - p0[1];
				v1 = p1[1] - p0[1];
				v2 = p2[1] - p0[1];
			}
		} else {
			if (Math.abs(n[1]) > Math.abs(n[2])) {
				u0 = p[0] - p0[0];
				u1 = p1[0] - p0[0];
				u2 = p2[0] - p0[0];
				
				v0 = p[2] - p0[2];
				v1 = p1[2] - p0[2];
				v2 = p2[2] - p0[2];
			} else {
				u0 = p[0] - p0[0];
				u1 = p1[0] - p0[0];
				u2 = p2[0] - p0[0];
				
				v0 = p[1] - p0[1];
				v1 = p1[1] - p0[1];
				v2 = p2[1] - p0[1];
			}
			
		}
		
		// compute denominator , check for invalid
		float temp = u1 * v2 - v1 * u2;
		if (!(Float.compare(temp, 0.0f) != 0)) {
			return kNoIntersection;
		}
		
		temp = 1.0f / temp;
		
		// compute barycentric coords, check for out-of-range at each step
		float alpha = (u0 * v2 - v0 * u2) * temp;
		if (!(alpha >= 0.0f)) {
			return kNoIntersection;
		}
		
		float beta = (u1 * v0 - v1 * u0) * temp;
		if (!(beta >= 0.0f)) {
			return kNoIntersection;
		}
		
		float gamma = 1.0f - alpha - beta;
		if (!(gamma >= 0.0f)) {
			return kNoIntersection;
		}
		
		intersection[X] = p[X];
		intersection[Y] = p[Y];
		intersection[Z] = p[Z];
		
		return t;
	}

	
	
	private float rayIntersectBox (float[] rayOrg, float[] rayDelta, float cx, float cy, float cz, float halfSize, float[] returnNormal) {
		float noIntersection = Float.MAX_VALUE; // return this if no intersection
		boolean inside = true;
		float xt = 0f, xn = 0f;
		float[] min = new float[]{cx - halfSize, cy - halfSize, cz - halfSize};
		float[] max = new float[]{cx + halfSize, cy + halfSize, cz + halfSize};
		
		if (rayOrg[X] < min[X]) {
			xt = min[X] - rayOrg[X];
			if (xt > rayDelta[X]) {
				return noIntersection;
			}
			xt /= rayDelta[X];
			inside = false;
			xn = -1f;
		} else if (rayOrg[X] > max[X]){
			xt = max[X] - rayOrg[X];
			if (xt < rayDelta[X]) {
				return noIntersection;
			}
			xt /= rayDelta[X];
			inside = false;
			xn = 1f;
		} else {
			xt = -1f;
		}
		
		float yt = 0f, yn = 0f;
		if (rayOrg[Y] < min[Y]) {
			yt = min[Y] - rayOrg[Y];
			if (yt > rayDelta[Y]) {
				return noIntersection;
			}
			yt /= rayDelta[Y];
			inside = false;
			yn = -1f;
		} else if (rayOrg[Y] > max[Y]){
			yt = max[Y] - rayOrg[Y];
			if (yt < rayDelta[Y]) {
				return noIntersection;
			}
			yt /= rayDelta[Y];
			inside = false;
			yn = 1f;
		} else {
			yt = -1f;
		}
		
		float zt = 0f, zn = 0f;
		if (rayOrg[Z] < min[Z]) {
			zt = min[Z] - rayOrg[Z];
			if (zt > rayDelta[Z]) {
				return noIntersection;
			}
			zt /= rayDelta[Z];
			inside = false;
			zn = -1f;
		} else if (rayOrg[Z] > max[Z]){
			zt = max[Z] - rayOrg[Z];
			if (zt < rayDelta[Z]) {
				return noIntersection;
			}
			zt /= rayDelta[Z];
			inside = false;
			zn = 1f;
		} else {
			zt = -1f;
		}
		
		// ray origin inside the box?
		if (inside) {
			if (returnNormal != null) {
				returnNormal[X] = -rayDelta[X];
				returnNormal[Y] = -rayDelta[Y];
				returnNormal[Z] = -rayDelta[Z];
				Vec3dMath.normalize(returnNormal);
			}
			return 0f;
		}
		
		// select the farthest plane, this is the plane of intersection
		int which = 0;
		float t = xt;
		if (yt > t) {
			which = 1;
			t = yt;
		}
		if (zt > t) {
			which = 2;
			t = zt;
		}
		
		switch (which) {
			case 0: { // intersect with yz plane
				float y = rayOrg[Y] + rayDelta[Y] * t;
				if (y < min[Y] || y > max[Y]) {
					return noIntersection;
				}
				float z = rayOrg[Z] + rayDelta[Z] * t;
				if (z < min[Z] || z > max[Z]) {
					return noIntersection;
				}
				if (returnNormal != null) {
					returnNormal[X] = xn;
					returnNormal[Y] = 0f;
					returnNormal[Z] = 0f;
				}			
			} break;
			case 1: { // intersect with xz plane
				float x = rayOrg[X] + rayDelta[X] * t;
				if (x < min[X] || x > max[X]) {
					return noIntersection;
				}
				float z = rayOrg[Z] + rayDelta[Z] * t;
				if (z < min[Z] || z > max[Z]) {
					return noIntersection;
				}
				if (returnNormal != null) {
					returnNormal[X] = 0f;
					returnNormal[Y] = yn;
					returnNormal[Z] = 0f;
				}			
			} break;
			case 2: { // intersect with xy plane
				float x = rayOrg[X] + rayDelta[X] * t;
				if (x < min[X] || x > max[X]) {
					return noIntersection;
				}
				float y = rayOrg[Y] + rayDelta[Y] * t;
				if (y < min[Y] || y > max[Y]) {
					return noIntersection;
				}
				if (returnNormal != null) {
					returnNormal[X] = 0f;
					returnNormal[Y] = 0f;
					returnNormal[Z] = zn;
				}			
			} break;
		}
		
		return t;
	}

	private float rayTriIntersect (
			float[] rayOrg,	// origin of the ray
			float[] rayDelta,	// ray length and direction 
			float[] p0, 		// triangle vertices
			float[] p1, 		// ...
			float[] p2, 		// ...
		float[] bary,		// barycentric coords of the intersection point IF an intersection is found
		float minT) {		// closest intersection found so far (start with 1.0)
		
				// return large number if no intersection found
				float kNoIntersection = Float.MAX_VALUE;
				Vector3D pv0 = new Vector3D(p0[X], p0[Y], p0[Z]);
				Vector3D pv1 = new Vector3D(p1[X], p1[Y], p1[Z]);
				Vector3D pv2 = new Vector3D(p2[X], p2[Y], p2[Z]);
				Vector3D rayStart = new Vector3D(rayOrg[X], rayOrg[Y], rayOrg[Z]);
				Vector3D rayEnd = new Vector3D(rayDelta[X], rayDelta[Y], rayDelta[Z]);
				
				// compute clockwise edge vectors
				Vector3D ev1 = Vector3D.OpSubtraction(pv1, pv0);
				Vector3D ev2 = Vector3D.OpSubtraction(pv2, pv1);
				
				// compute surface normal (unnormalized)
				Vector3D n = ev1.cross(ev2);
				
				// compute gradient, tells us how steep of an approach angle to the "front" side of the triangle
				double dot = n.dot(rayEnd);
				
				// check for a ray that is parallel to the triangle or not pointing towards the "front" of the triangle
				// this will also reject degenerate triangles and rays
				if (!(dot < 0.0)) {
					return kNoIntersection;
				}
				
				// compute d value for the plane equation. use the plane equation with d on the right side:
				// Ax + By + Cz = d
				double d = n.dot(pv0);
				
				// compute the parametric point of intersection with the plane containing the triangle,
				// check at earliest stage for trivial rejection
				double t = d - n.dot(rayStart);
				
				// is ray origin on the backside of the triangle?
				if (!(t <= 0.0)) {
					return kNoIntersection;
				}
				
				// closer intersection already found? or ray desn't reach the plane?
				if (!(t >= dot * minT)) {
					return kNoIntersection;
				}
				
				// ray intersects the plane. compute parametric point of intersection.
				t /= dot;
				if (!(t >= 0.0) || !(t <= minT)) {
					(new Exception()).printStackTrace();
					return kNoIntersection;
				}

				
				// compute 3D point of intersection
				Vector3D v = Vector3D.OpMultiply(t, rayEnd);
				Vector3D p = Vector3D.OpAddition(rayStart, v);
				 
				
				// find dominate axis to select which plane to select onto. 
				// and compute u's and v's
				double u0, u1, u2 = 0;
				double v0, v1, v2 = 0;
				
				if (Math.abs(n.getX()) > Math.abs(n.getY())) {
					if (Math.abs(n.getX()) > Math.abs(n.getZ())) {
						u0 = p.getY() - pv0.getY();
						u1 = pv1.getY() - pv0.getY();
						u2 = pv2.getY() - pv0.getY();
						
						v0 = p.getZ() - pv0.getZ();
						v1 = pv1.getZ() - pv0.getZ();
						v2 = pv2.getZ() - pv0.getZ();
					} else {
						u0 = p.getX() - pv0.getX();
						u1 = pv1.getX() - pv0.getX();
						u2 = pv2.getX() - pv0.getX();
						
						v0 = p.getY() - pv0.getY();
						v1 = pv1.getY() - pv0.getY();
						v2 = pv2.getY() - pv0.getY();
					}
				} else {
					if (Math.abs(n.getY()) > Math.abs(n.getZ())) {
						u0 = p.getX() - pv0.getX();
						u1 = pv1.getX() - pv0.getX();
						u2 = pv2.getX() - pv0.getX();
						
						v0 = p.getZ() - pv0.getZ();
						v1 = pv1.getZ() - pv0.getZ();
						v2 = pv2.getZ() - pv0.getZ();
					} else {
						u0 = p.getX() - pv0.getX();
						u1 = pv1.getX() - pv0.getX();
						u2 = pv2.getX() - pv0.getX();
						
						v0 = p.getY() - pv0.getY();
						v1 = pv1.getY() - pv0.getY();
						v2 = pv2.getY() - pv0.getY();
					}
					
				}
				
				// compute denominator , check for invalid
				double temp = u1 * v2 - v1 * u2;
				if (!(Double.compare(temp, 0.0) != 0)) {
					return kNoIntersection;
				}
				
				temp = 1.0 / temp;
				
				// compute barycentric coords, check for out-of-range at each step
				double alpha = (u0 * v2 - v0 * u2) * temp;
				if (!(alpha >= 0.0)) {
					return kNoIntersection;
				}
				
				double beta = (u1 * v0 - v1 * u0) * temp;
				if (!(beta >= 0.0)) {
					return kNoIntersection;
				}
				
				double gamma = 1.0 - alpha - beta;
				if (!(gamma >= 0.0)) {
					return kNoIntersection;
				}
				
				bary[0] = (float)alpha;
				bary[1] = (float)beta;
				bary[2] = (float)gamma;
				
				return (float)t;
	}
		
	
	/*
	 * Ported from the following:|
	 *                           V
	 */	
	/********************************************************/

	/* AABB-triangle overlap test code                      */

	/* by Tomas Akenine-Möller                              */

	/* Function: int triBoxOverlap(float boxcenter[3],      */

	/*          float boxhalfsize[3],float triverts[3][3]); */

	/* History:                                             */

	/*   2001-03-05: released the code in its first version */

	/*   2001-06-18: changed the order of the tests, faster */

	/*                                                      */

	/* Acknowledgement: Many thanks to Pierre Terdiman for  */

	/* suggestions and discussions on how to optimize code. */

	/* Thanks to David Hunt for finding a ">="-bug!         */

	/********************************************************/

	private void findMinMax(Float x0, Float x1, Float x2, Float min, Float max) {

	  min = max = x0;   

	  if (x1 < min) {
		  min = x1;
	  }

	  if (x1 > max) {
		  max = x1;
	  }

	  if (x2 < min) {
		  min = x2;
	  }

	  if (x2 > max) {
		  max = x2;
	  }
	}

	private int planeBoxOverlap(float[] normal, float[] vert, float[] maxbox) {

	  int q;

	  float[] vmin = new float[3];
	  float[] vmax = new float[3];
	  float v = 0f;

	  for(q=X; q<=Z; q++) {
	    v=vert[q];					// -NJMP-

	    if(normal[q]>0.0f) {
	      vmin[q] =- maxbox[q] - v;	// -NJMP-
	      vmax[q] = maxbox[q] - v;	// -NJMP-
	    } else {
	      vmin[q] = maxbox[q] - v;	// -NJMP-
	      vmax[q] =- maxbox[q] - v;	// -NJMP-
	    }
	  }

	  if(VectorUtil.dotVec3(normal,vmin) > 0.0f) {
		  return 0;	// -NJMP-
	  }

	  if(VectorUtil.dotVec3(normal,vmax) >= 0.0f) {
		  return 1;	// -NJMP-
	  }
	  return 0;
	}

	/*======================== X-tests ========================*/

	private int axisTestX01(float a, float b, float fa, float fb, float[] boxHalfSize, Float p0, Float p2, float[] v0,
			float[] v2, Float min, Float max, Float rad) {

		p0 = a*v0[Y] - b*v0[Z];			       	   

		p2 = a*v2[Y] - b*v2[Z];			       	   

	        if(p0<p2) {
	        	min = p0; 
	        	max = p2;
	        } else {
	        	min = p2; 
	        	max = p0;
	        } 
	        
		rad = fa * boxHalfSize[Y] + fb * boxHalfSize[Z];   

		if(min > rad || max < -rad) {
			return 0;
		}
		return 1;
	}


	private int axisTestX2(float a, float b, float fa, float fb, float[] boxHalfSize, Float p0, Float p1, float[] v0,
			float[] v1, Float min, Float max, Float rad) {	

		p0 = a*v0[Y] - b*v0[Z];			         

		p1 = a*v1[Y] - b*v1[Z];			       	 

	        if(p0 < p1) {
	        	min = p0; 
	        	max = p1;
	        } else {
	        	min = p1; 
	        	max = p0;
	        }

		rad = fa * boxHalfSize[Y] + fb * boxHalfSize[Z];  

		if(min > rad || max < -rad) {
			return 0;
		}
		
		return 1;
	}

	/*======================== Y-tests ========================*/

	private int axisTestY02(float a, float b, float fa, float fb, float[] boxHalfSize, Float p0, Float p2, float[] v0,
			float[] v2, Float min, Float max, Float rad) {

		p0 = -a*v0[X] + b*v0[Z];		

		p2 = -a*v2[X] + b*v2[Z];	   

	        if(p0 < p2) {
	        	min = p0; 
	        	max = p2;
	        } else {
	        	min = p2; 
	        	max = p0;
	        } 

		rad = fa * boxHalfSize[X] + fb * boxHalfSize[Z];  

		if(min > rad || max < -rad) {
			return 0;
		}
		return 1;
	}

	private int axisTestY1(float a, float b, float fa, float fb, float[] boxHalfSize, Float p0, Float p1, float[] v0,
			float[] v1, Float min, Float max, Float rad) {

		p0 = -a*v0[X] + b*v0[Z];

		p1 = -a*v1[X] + b*v1[Z];	   

	        if(p0 < p1) {
	        	min = p0; 
	        	max = p1;
	        } else {
	        	min = p1; 
	        	max = p0;
	        }

		rad = fa * boxHalfSize[X] + fb * boxHalfSize[Z]; 

		if(min > rad || max <- rad) {
			return 0;
		}
		return 1;
	}

	/*======================== Z-tests ========================*/

	private int axisTestZ12(float a, float b, float fa, float fb, float[] boxHalfSize, Float p1, Float p2, float[] v1,
			float[] v2, Float min, Float max, Float rad) {

		p1 = a*v1[X] - b*v1[Y];	

		p2 = a*v2[X] - b*v2[Y];	

	        if(p2 < p1) {
	        	min = p2; 
	        	max = p1;
	        } else {
	        	min = p1; 
	        	max = p2;
	        } 

		rad = fa * boxHalfSize[X] + fb * boxHalfSize[Y]; 

		if(min > rad || max <- rad) {
			return 0;
		}
		return 1;
	}

	private int axisTestZ0(float a, float b, float fa, float fb, float[] boxHalfSize, Float p0, Float p1, float[] v0,
			float[] v1, Float min, Float max, Float rad) {	   

		p0 = a*v0[X] - b*v0[Y];				   

		p1 = a*v1[X] - b*v1[Y];			           

	        if(p0<p1) {
	        	min = p0; 
	        	max = p1;
	        } else {
	        	min = p1; 
	        	max = p0;
	        } 

		rad = fa * boxHalfSize[X] + fb * boxHalfSize[Y];   

		if(min > rad || max < -rad) {
			return 0;
		}

		return 1;
	}

	public boolean triBoxOverlap(float[] boxcenter /* size 3 */, float[] boxHalfSize /* size 3 */, float[][] triverts /* 3x3 array */)	{

	  /*    use separating axis theorem to test overlap between triangle and box */
	  /*    need to test for overlap in these directions: */
	  /*    1) the {x,y,z}-directions (actually, since we use the AABB of the triangle */
	  /*       we do not even need to test these) */
	  /*    2) normal of the triangle */
	  /*    3) crossproduct(edge from tri, {x,y,z}-directin) */
	  /*       this gives 3x3=9 more tests */

		Float min=0f, max=0f, p0=0f, p1=0f, p2=0f, rad=0f, fex=0f, fey=0f, fez=0f;	
		
		float[] v0 = new float[3];
		float[] v1 = new float[3];
		float[] v2 = new float[3];

		float[] normal = new float[3];
		float[] e0 = new float[3];
		float[] e1 = new float[3];
		float[] e2 = new float[3];

	   /* This is the fastest branch on Sun */

	   /* move everything so that the boxcenter is in (0,0,0) */
	   
	   VectorUtil.subVec3(v0, triverts[0], boxcenter);
	   VectorUtil.subVec3(v1, triverts[1], boxcenter);
	   VectorUtil.subVec3(v2, triverts[2], boxcenter);

	   /* compute triangle edges */
	   VectorUtil.subVec3(e0, v1, v0);	/* tri edge 0 */
	   VectorUtil.subVec3(e1, v2, v1);	/* tri edge 1 */
	   VectorUtil.subVec3(e2, v0, v2);	/* tri edge 2 */

	   /* Bullet 3:  */

	   /*  test the 9 tests first (this was faster) */

	   fex = FloatUtil.abs(e0[X]);
	   fey = FloatUtil.abs(e0[Y]);
	   fez = FloatUtil.abs(e0[Z]);

	   if (axisTestX01(e0[Z], e0[Y], fez, fey, boxHalfSize, p0, p2, v0,
				v2, min, max, rad) == 0) {
		   return false;
	   }
	   if (axisTestY02(e0[Z], e0[X], fez, fex, boxHalfSize, p0, p2, v0,
				v2, min, max, rad) == 0) {
		   return false;
	   }
	   if (axisTestZ12(e0[Y], e0[X], fey, fex, boxHalfSize, p1, p2, v1,
				v2, min, max, rad) == 0) {
		   return false;
	   }

	   fex = FloatUtil.abs(e1[X]);
	   fey = FloatUtil.abs(e1[Y]);
	   fez = FloatUtil.abs(e1[Z]);

	   if (axisTestX01(e1[Z], e1[Y], fez, fey, boxHalfSize, p0, p2, v0,
				v2, min, max, rad) == 0) {
		   return false;
	   }
	   if (axisTestY02(e1[Z], e1[X], fez, fex, boxHalfSize, p0, p2, v0,
				v2, min, max, rad) == 0) {
		   return false;
	   }
	   if (axisTestZ0(e1[Y], e1[X], fey, fex, boxHalfSize, p0, p1, v0,
				v1, min, max, rad) == 0) {
		   return false;
	   }

	   fex = FloatUtil.abs(e2[X]);
	   fey = FloatUtil.abs(e2[Y]);
	   fez = FloatUtil.abs(e2[Z]);

	   if (axisTestX2(e2[Z], e2[Y], fez, fey, boxHalfSize, p0, p1, v0,
				v1, min, max, rad) == 0) {
		   return false;
	   }
	   if (axisTestY1(e2[Z], e2[X], fez, fex, boxHalfSize, p0, p1, v0,
				v1, min, max, rad) == 0) {
		   return false;
	   }
	   if (axisTestZ12(e2[Y], e2[X], fey, fex, boxHalfSize, p1, p2, v1,
				v2, min, max, rad) == 0) {
		   return false;
	   }

	   /* Bullet 1: */

	   /*  first test overlap in the {x,y,z}-directions */
	   /*  find min, max of the triangle each direction, and test for overlap in */
	   /*  that direction -- this is equivalent to testing a minimal AABB around */
	   /*  the triangle against the AABB */

	   /* test in X-direction */
	   findMinMax(v0[X],v1[X],v2[X],min,max);
	   if (min > boxHalfSize[X] || max < -boxHalfSize[X]) {
		   return false;
	   }

	   /* test in Y-direction */
	   findMinMax(v0[Y],v1[Y],v2[Y],min,max);

	   if (min > boxHalfSize[Y] || max < -boxHalfSize[Y]) {
		   return false;
	   }

	   /* test in Z-direction */
	   findMinMax(v0[Z],v1[Z],v2[Z],min,max);

	   if (min > boxHalfSize[Z] || max < -boxHalfSize[Z]) {
		   return false;
	   }

	   /* Bullet 2: */

	   /*  test if the box intersects the plane of the triangle */
	   /*  compute plane equation of triangle: normal*x+d=0 */

	   VectorUtil.crossVec3(normal,e0,e1);

	   if (planeBoxOverlap(normal, v0, boxHalfSize) == 0) {
		   return false;	// -NJMP-
	   }

	   return true;   /* box and triangle overlaps */
	}
	
	
	public static boolean testAABBTriangle(AABBox b, Triangle tri) {
		float p0, p1, p2, r;
		float pMin;
		float pMax;

		float xMin;
		float yMin;
		float zMin;
		float xMax;
		float yMax;
		float zMax;

		// Compute box center and extents of AABoundingBox (if not already given in that
		// format)
		float[] c = b.getCenter();
		float e0 = (b.getHigh()[X] - b.getLow()[X]) * 0.5f;
		float e1 = (b.getHigh()[Y] - b.getLow()[Y]) * 0.5f;
		float e2 = (b.getHigh()[Z] - b.getLow()[Z]) * 0.5f;

		float[] v0 = tri.points[0];
		float[] v1 = tri.points[1];
		float[] v2 = tri.points[2];

		// Translate triangle as conceptually moving AABB to origin
		v0 = VectorUtil.subVec3(new float[3], v0, c);
		v1 = VectorUtil.subVec3(new float[3], v1, c);
		v2 = VectorUtil.subVec3(new float[3], v2, c);

		// Compute edge vectors for triangle
		float[] f0 = VectorUtil.subVec3(new float[3], v1, v0);
		float[] f1 = VectorUtil.subVec3(new float[3], v2, v1);
		float[] f2 = VectorUtil.subVec3(new float[3], v0, v2);

		// Test axes a00..a22 (category 3)

		// Test axis a00
		p0 = v0[Z] * v1[Y] - v0[Y] * v1[Z];
		p2 = v2[Z] * (v1[Y] - v0[Y]) - v2[Z] * (v1[Z] - v0[Z]);

		r = e1 * FloatUtil.abs(f0[Z]) + e2 * FloatUtil.abs(f0[Y]);

		if (Float.max(-Float.max(p0, p2), Float.min(p0, p2)) > r) {
			return false; // Axis is a separating axis
		}

		// Test axis a01 (a01 = new Vector3(0, -f1.Z, f1.Y))
		p0 = VectorUtil.dotVec3(v0, new float[] { 0f, -f1[Z], f1[Y] });
		p1 = VectorUtil.dotVec3(v1, new float[] { 0f, -f1[Z], f1[Y] });
		p2 = VectorUtil.dotVec3(v2, new float[] { 0f, -f1[Z], f1[Y] });

		// r =

		// [-r, r] and [min(p0, p1, p2), max(p0, p1, p2)] should be disjoint for this
		// axis
		pMin = Float.min(p0, p1);
		pMin = Float.min(pMin, p2);
		pMax = Float.max(p0, p1);
		pMax = Float.max(pMax, p2);

		if (Float.max(-pMax, pMin) > r) {
			return false; // Axis is a separating axis
		}

		// Test axis a02 (a02 = new Vector3(0, -f2.Z, f2.Y))
		p0 = VectorUtil.dotVec3(v0, new float[] { 0f, -f2[Z], f2[Y] });
		p1 = VectorUtil.dotVec3(v1, new float[] { 0f, -f2[Z], f2[Y] });
		p2 = VectorUtil.dotVec3(v2, new float[] { 0f, -f2[Z], f2[Y] });

		// r =

		// [-r, r] and [min(p0, p1, p2), max(p0, p1, p2)] should be disjoint for this
		// axis
		pMin = Float.min(p0, p1);
		pMin = Float.min(pMin, p2);
		pMax = Float.max(p0, p1);
		pMax = Float.max(pMax, p2);

		if (Float.max(-pMax, pMin) > r) {
			return false; // Axis is a separating axis
		}

		// Test axis a10 (a10 = new Vector3(f0.Z, 0, -f0.X))
		p0 = VectorUtil.dotVec3(v0, new float[] { f0[Z], 0f, -f0[X] });
		p1 = VectorUtil.dotVec3(v1, new float[] { f0[Z], 0f, -f0[X] });
		p2 = VectorUtil.dotVec3(v2, new float[] { f0[Z], 0f, -f0[X] });

		// r =

		// [-r, r] and [min(p0, p1, p2), max(p0, p1, p2)] should be disjoint for this
		// axis
		pMin = Float.min(p0, p1);
		pMin = Float.min(pMin, p2);
		pMax = Float.max(p0, p1);
		pMax = Float.max(pMax, p2);

		if (Float.max(-pMax, pMin) > r) {
			return false; // Axis is a separating axis
		}

		// Test axis a11 (a11 = new Vector3(f1.Z, 0, -f1.X))
		p0 = VectorUtil.dotVec3(v0, new float[] { f1[Z], 0f, -f1[X] });
		p1 = VectorUtil.dotVec3(v1, new float[] { f1[Z], 0f, -f1[X] });
		p2 = VectorUtil.dotVec3(v2, new float[] { f1[Z], 0f, -f1[X] });

		// r =

		// [-r, r] and [min(p0, p1, p2), max(p0, p1, p2)] should be disjoint for this
		// axis
		pMin = Float.min(p0, p1);
		pMin = Float.min(pMin, p2);
		pMax = Float.max(p0, p1);
		pMax = Float.max(pMax, p2);

		if (Float.max(-pMax, pMin) > r) {
			return false; // Axis is a separating axis
		}

		// Test axis a12 (a12 = new Vector3(f2.Z, 0, -f2.X))
		p0 = VectorUtil.dotVec3(v0, new float[] { f2[Z], 0f, -f2[X] });
		p1 = VectorUtil.dotVec3(v1, new float[] { f2[Z], 0f, -f2[X] });
		p2 = VectorUtil.dotVec3(v2, new float[] { f2[Z], 0f, -f2[X] });

		// r =

		// [-r, r] and [min(p0, p1, p2), max(p0, p1, p2)] should be disjoint for this
		// axis
		pMin = Float.min(p0, p1);
		pMin = Float.min(pMin, p2);
		pMax = Float.max(p0, p1);
		pMax = Float.max(pMax, p2);

		if (Float.max(-pMax, pMin) > r) {
			return false; // Axis is a separating axis
		}

		// Test axis a20 (a20 = new Vector3(-f0.Y, -f0.X, 0))
		p0 = VectorUtil.dotVec3(v0, new float[] { -f0[Y], -f0[X], 0f });
		p1 = VectorUtil.dotVec3(v1, new float[] { -f0[Y], -f0[X], 0f });
		p2 = VectorUtil.dotVec3(v2, new float[] { -f0[Y], -f0[X], 0f });

		// r =

		// [-r, r] and [min(p0, p1, p2), max(p0, p1, p2)] should be disjoint for this
		// axis
		pMin = Float.min(p0, p1);
		pMin = Float.min(pMin, p2);
		pMax = Float.max(p0, p1);
		pMax = Float.max(pMax, p2);

		if (Float.max(-pMax, pMin) > r) {
			return false; // Axis is a separating axis
		}

		// Test axis a21 (a21 = new Vector3(-f1.Y, -f1.X, 0))
		p0 = VectorUtil.dotVec3(v0, new float[] { -f1[Y], -f1[X], 0f });
		p1 = VectorUtil.dotVec3(v1, new float[] { -f1[Y], -f1[X], 0f });
		p2 = VectorUtil.dotVec3(v2, new float[] { -f1[Y], -f1[X], 0f });

		// r =

		// [-r, r] and [min(p0, p1, p2), max(p0, p1, p2)] should be disjoint for this
		// axis
		pMin = Float.min(p0, p1);
		pMin = Float.min(pMin, p2);
		pMax = Float.max(p0, p1);
		pMax = Float.max(pMax, p2);

		if (Float.max(-pMax, pMin) > r) {
			return false; // Axis is a separating axis
		}

		// Test axis a22 (a22 = new Vector3(-f2.Y, -f2.X, 0))
		p0 = VectorUtil.dotVec3(v0, new float[] { -f2[Y], -f2[X], 0f });
		p1 = VectorUtil.dotVec3(v1, new float[] { -f2[Y], -f2[X], 0f });
		p2 = VectorUtil.dotVec3(v2, new float[] { -f2[Y], -f2[X], 0f });

		// r =

		// [-r, r] and [min(p0, p1, p2), max(p0, p1, p2)] should be disjoint for this
		// axis
		pMin = Float.min(p0, p1);
		pMin = Float.min(pMin, p2);
		pMax = Float.max(p0, p1);
		pMax = Float.max(pMax, p2);

		if (Float.max(-pMax, pMin) > r) {
			return false; // Axis is a separating axis
		}

		// Test the three axes corresponding to the face normals of AABB b (category 1).
		xMax = Float.max(v0[X], v1[X]);
		xMax = Float.max(xMax, v2[X]);

		xMin = Float.min(v0[X], v1[X]);
		xMin = Float.min(xMin, v2[X]);

		yMax = Float.max(v0[Y], v1[Y]);
		yMax = Float.max(yMax, v2[Y]);

		yMin = Float.min(v0[Y], v1[Y]);
		yMin = Float.min(yMin, v2[Y]);

		zMax = Float.max(v0[Z], v1[Z]);
		zMax = Float.max(zMax, v2[Z]);

		zMin = Float.min(v0[Z], v1[Z]);
		zMin = Float.min(zMin, v2[Z]);

		// Exit if [-e0, e0] and [min(v0.x,v1.x,v2.x), max(v0.x,v1.x,v2.x)] do not
		// overlap
		if (xMax < -e0 || xMin > e0) {
			return false;
		}

		// ... [-e1, e1] and [min(v0.y,v1.y,v2.y), max(v0.y,v1.y,v2.y)] do not overlap
		if (yMax < -e1 || yMin > e1) {
			return false;
		}
		// ... [-e2, e2] and [min(v0.z,v1.z,v2.z), max(v0.z,v1.z,v2.z)] do not overlap
		if (zMax < -e2 || zMin > e2) {
			return false;
		}

		// Test separating axis corresponding to triangle face normal (category 2)
		// Face Normal is -ve as Triangle is clockwise winding
		float[] p = new float[4];
		p = VectorUtil.crossVec3(new float[4], f0, f1);
		p[X] *= -1f;
		p[Y] *= -1f;
		p[Z] *= -1f;
		p = VectorUtil.normalizeVec3(new float[4], p);
//	            p.D = Vector3.Dot(p.Normal, v0);
		// Must create Plane.D from ORIGINAL Triangle Points (not translated v0)
		p[D] = VectorUtil.dotVec3(p, tri.points[0]);

		return testAABBPlane(b, p);
	}

    // Test if AABB (Axis Aligned Bounding Box) b intersects plane p
	private static boolean testAABBPlane(AABBox b, float[] p) {
		// These two lines not necessary with a (center, extents) AABB representation
		float[] center = b.getCenter();
		float[] e = VectorUtil.subVec3(new float[3], b.getHigh(), center); // Compute positive extents

		// Compute the projection interval radius of b onto L(t) = b.c + t * p.n
		float r = e[X] * FloatUtil.abs(p[X]) + e[Y] * FloatUtil.abs(p[Y]) + e[Z] * FloatUtil.abs(p[Z]);

		// Compute distance (s) of box center from plane
		float s = VectorUtil.dotVec3(p, center) - p[D];

		// Intersection occurs when distance s falls within [-r,+r] interval
		if (FloatUtil.abs(s) <= r) {
			return true; 
		} else {
			return false;
		}
	}

	private void looseOctTreeTriangleIntersect(Triangle poly, ONode n, ArrayList<Triangle> list) {
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						looseOctTreeTriangleIntersect(poly, n.child[k][j][i], list);
					}
				}
			}
		}

		// Count objects in this node.
		Triangle o = n.object;
		while (o != null) {	
			if (noDivTriTriIsect(poly.points[0], poly.points[1], poly.points[2], o.points[0], o.points[1], o.points[2])) {
				list.add(o);

			}
			o = o.next;
		}
	}

	/*
	 * Method to handle 3D vector normalization even when the vector magnitude is < 1.0
	 * This version uses VectorUtil from JOGL to calculate the vector magnitude
	 *
	 * @param vout the return vector
	 * @param vin the input vector to be normalized
	 * @return the return vector
	 *
	 * thread-safe
	 */
	private static float[] normalizeVec3(float[] vout, float[] vin) {
		float vmag = VectorUtil.normVec3(vin);
		
		if ( vmag > 0.0f ) {
	      vout[0] = vin[0] / vmag;
	      vout[1] = vin[1] / vmag;
	      vout[2] = vin[2] / vmag;
	   } else {
	      vout[0] = 0.0f;
	      vout[1] = 0.0f;
	      vout[2] = 0.0f;
	   }
		
		return vout;
	}
	
	/**
	 * Method to handle 3D vector normalization even when the vector magnitude is < 1.0
	 * This version uses the Java Math class to calculate magnitude.
	 *
	 * @param vector the input vector to be normalized
	 * @return the normalized vector
	 *
	 * thread-safe
	 */
	public static float[] normalizeVec3(float[] vector) {
        final float[] newVector = new float[3];

        final double d = Math.sqrt(vector[0]*vector[0] + vector[1]*vector[1] + vector[2]*vector[2]);
        if(d> 0.0)
        {
            newVector[0] = (float)(vector[0]/d);
            newVector[1] = (float)(vector[1]/d);
            newVector[2] = (float)(vector[2]/d);
        } else {
        	newVector[0] = 0.0f;
        	newVector[1] = 0.0f;
        	newVector[2] = 0.0f;
  	   }
        
        return newVector;
    }

	/*
	 * Method to handle 3D vector normalization even when the vector magnitude is < 1.0
	 * This version uses the Java Math class to calculate magnitude.
	 *
	 * @param vector the input vector to be normalized
	 * @return the normalized vector
	 *
	 * thread-safe
	 */
	private static double[] normalizeVec3(double[] vector) {
        final double[] newVector = new double[3];

        final double d = Math.sqrt(vector[0]*vector[0] + vector[1]*vector[1] + vector[2]*vector[2]);
        if(d> 0.0)
        {
            newVector[0] = vector[0]/d;
            newVector[1] = vector[1]/d;
            newVector[2] = vector[2]/d;
        } else {
        	newVector[0] = 0.0;
        	newVector[1] = 0.0;
        	newVector[2] = 0.0;
  	   }
        
        return newVector;
    }

	/*
	 * Rescale a vector of direction "vector" with length "size"
	 *
	 * @param vector
	 * @param size
	 * @return the resized vector
	 *
	 * thread-safe
	 */
	private static float[] setVectorLength(float[] vector, float size){
 
		//normalize the vector
		float[] vectorNormalized = new float[3];
		vectorNormalized = normalizeVec3(vectorNormalized, vector);
 
		//scale the vector
		return VectorUtil.scaleVec3(vectorNormalized, vectorNormalized, size);
	}

	/*
	 * Method to calculate the closest point on two lines in 3D. 
	 * Two non-parallel lines which may or may not touch each other have a point on each line which are closest
	 * to each other. This function finds those two points. If the lines are not parallel, the function 
	 * outputs true, otherwise false.
	 * Coded using the algorithm from:
	 * "3D Math Primer for Graphics and Game Development, 2nd Edition" by Fletcher Dunn and Ian Parberry
	 * ISBN-13: 978-1568817231
	 * ISBN-10: 1568817231
	 *
	 * @param closestPointLine1 in/out return value
	 * @param closestPointLine2 in/out return value
	 * @param linePoint1 a point on the first line
	 * @param lineVec1 direction of the first line
	 * @param linePoint2 a point on the second line
	 * @param lineVec2 direction of the second line
	 * @return true if the lines are not parallel
	 *
	 * thread-safe
	 */
	private static boolean closestPointsOnTwoLines(float[] closestPointLine1, float[] closestPointLine2, float[] linePoint1, float[] lineVec1, float[] linePoint2, float[] lineVec2){
 
		closestPointLine1 = clearVector(closestPointLine1);
		closestPointLine2 = clearVector(closestPointLine2);
 
		float a = VectorUtil.dotVec3(lineVec1, lineVec1);
		float b = VectorUtil.dotVec3(lineVec1, lineVec2);
		float e = VectorUtil.dotVec3(lineVec2, lineVec2);
 
		float d = a*e - b*b;
 
		//lines are not parallel
		if(Float.compare(d, 0f) != 0){
 
			float[] r = new float[3];
			r = VectorUtil.subVec3(r, linePoint1, linePoint2);
			float c = VectorUtil.dotVec3(lineVec1, r);
			float f = VectorUtil.dotVec3(lineVec2, r);
 
			float s = (b*f - c*e) / d;
			float t = (a*f - c*b) / d;
			
			float[] lv1Scaled = new float[3];
			float[] lv2Scaled = new float[3];
			
			lv1Scaled = VectorUtil.scaleVec3(lv1Scaled, lineVec1, s);
			lv2Scaled = VectorUtil.scaleVec3(lv2Scaled, lineVec2, t);
			
			closestPointLine1 = VectorUtil.addVec3(closestPointLine1, linePoint1, lv1Scaled);
			closestPointLine2 = VectorUtil.addVec3(closestPointLine2, linePoint2, lv2Scaled);
 
			return true;
		}
 
		else{
			return false;
		}
	}	
 

	//Convert a plane defined by 3 points to a plane defined by a vector and a point. 
	//The plane point is the middle of the triangle defined by the 3 points.
	private static void planeFrom3Points(float[] planeNormal, float[] planePoint, float[] pointA, float[] pointB, float[] pointC) {
 
		planeNormal = clearVector(planeNormal);
		planePoint = clearVector(planePoint);
 
		//Make two vectors from the 3 input points, originating from point A
		float[] AB = new float[3];
		AB = VectorUtil.subVec3(AB, pointB, pointA);
		float[] AC = new float[3];
		AC = VectorUtil.subVec3(AC, pointC, pointA);
 
		//Calculate the normal
		planeNormal = VectorUtil.crossVec3(planeNormal, AB, AC);
 
		//Get the points in the middle AB and AC
		float[] halfAB = new float[3];
		float[] halfAC = new float[3];
		halfAB = VectorUtil.scaleVec3(halfAB, AB, 0.5f);
		halfAC = VectorUtil.scaleVec3(halfAC, AC, 0.5f);
		float[] middleAB = new float[3];
		float[] middleAC = new float[3];
		middleAB = VectorUtil.addVec3(middleAB, pointA, halfAB);
		middleAC = VectorUtil.addVec3(middleAC, pointA, halfAC);
 
		//Get vectors from the middle of AB and AC to the point which is not on that line.
		float[] middleABtoC = new float[3];
		float[] middleACtoB = new float[3];
		middleABtoC = VectorUtil.subVec3(middleABtoC, pointC, middleAB); 
		middleACtoB = VectorUtil.subVec3(middleACtoB, pointB, middleAC); 
 
		//Calculate the intersection between the two lines. This will be the center 
		//of the triangle defined by the 3 points.
		//We could use lineLineIntersection instead of closestPointsOnTwoLines but due to rounding errors 
		//this sometimes doesn't work.
		float[] temp = new float[3];
		closestPointsOnTwoLines(planePoint, temp, middleAB, middleABtoC, middleAC, middleACtoB);
	}
	
	
	private static float[] linePlaneIntersectTri(float[] intersection, float[] linePoint, float[] lineVec, Triangle t) {
		float[] triNorm = VectorUtil.crossVec3(new float[3], VectorUtil.subVec3(new float[3], t.points[1], t.points[0]), VectorUtil.subVec3(new float[3], t.points[2], t.points[0]));
		
		linePlaneIntersection(intersection, linePoint, lineVec, OctTree.normalizeVec3(triNorm), t.points[0]);
		
		return intersection;
	}
	

	//Get the intersection between a line and a plane. 
	//If the line and plane are not parallel, the function outputs true, otherwise false.
	private static boolean linePlaneIntersection(float[] intersection, float[] linePoint, float[] lineVec, float[] planeNormal, float[] planePoint) {
 
		float dot;
 
		//calculate the distance between the linePoint and the line-plane intersection point
		float[] u = new float[3];
		u = VectorUtil.subVec3(u, lineVec, linePoint);
		dot = VectorUtil.dotVec3(planeNormal, u);
		
		if (FloatUtil.abs(dot) > EPSILON) {
	        // the factor of the point between p0 -> p1 (0 - 1)
	        // if 'fac' is between (0 - 1) the point intersects with the segment.
	        // otherwise:
	        //  < 0.0: behind p0.
	        //  > 1.0: in front of p1.
	        float[] w = VectorUtil.subVec3(new float[3], linePoint, planePoint);
	        float fac = -VectorUtil.dotVec3(planeNormal, w) / dot;
	        u = VectorUtil.scaleVec3(new float[3], u, fac);
	        VectorUtil.addVec3(intersection, linePoint, u);
	        return true;
		} else {
			return false;
		}
	}

	private static boolean linePlaneIntersectionDouble(float[] intersection, float[] linePoint, float[] lineVec, float[] planeNormal, float[] planePoint) {
		 
		double dot;
		double[] ret = new double[3]; // intersection
		double[] p0 = floatToDoubleArray(linePoint);  // linePoint
		double[] p1 = floatToDoubleArray(lineVec);  // lineVec
		double[] pp = floatToDoubleArray(planePoint);  // planePoint
		double[] pn = floatToDoubleArray(planeNormal);  // planeNormal
		
		// map inputs above
 
		//calculate the distance between the linePoint and the line-plane intersection point
		double[] u = new double[3];
		u = subV3V3(p1, p0);
		dot = dotV3V3(pn, u);
		
		if (Math.abs(dot) > EPSILON) {
	        // the factor of the point between p0 -> p1 (0 - 1)
	        // if 'fac' is between (0 - 1) the point intersects with the segment.
	        // otherwise:
	        //  < 0.0: behind p0.
	        //  > 1.0: in front of p1.
			double[] w = subV3V3(p0, pp);
			double fac = -1.0 * dotV3V3(pn, w) / dot;
			u = multV3D(u, fac);
			ret = addV3V3(p0, u);
			// map ret into intersection
			intersection[X] = (float)ret[X];
			intersection[Y] = (float)ret[Y];
			intersection[Z] = (float)ret[Z];
	        return true;
		} else {
			return false;
		}
	}
	
	static double[] addV3V3(double[] a, double[] b) {
		double[] ret = new double[3];
		ret[X] = a[X] + b[X];
		ret[Y] = a[Y] + b[Y];
		ret[Z] = a[Z] + b[Z];
		return ret;
	}
	
	static double[] subV3V3(double[] a, double[] b) {
		double[] ret = new double[3];
		ret[X] = a[X] - b[X];
		ret[Y] = a[Y] - b[Y];
		ret[Z] = a[Z] - b[Z];
		return ret;
	}
	
	static double dotV3V3(double[] v0, double[] v1) {
		return  (v0[X] * v1[X]) +
			    (v0[Y] * v1[Y]) +
			    (v0[Z] * v1[Z]);
	}
	
	static double[] multV3D(double[] v, double f) {
		double[] ret = new double[3];
        ret[X] = v[X] * f;
        ret[Y] = v[Y] * f;
        ret[Z] = v[Z] * f;        
        return ret;
	}
	
	/*
	 * @param planeEq
	 * @param tri
	 */
	private static void comparePlanes(double[] planeEq, double[][] tri) {
		for (int i=0; i<tri.length; i++) {
			double pZ = (-planeEq[D] - planeEq[A]*tri[i][X] - planeEq[B]*tri[i][Y]) / planeEq[C];
			System.err.format(" Plane Eq z: %12.12f  Triangle vertex [%d] z: %12.12f\n", pZ, i, tri[i][Z]);
		}
	}

	/*
	 * @param tri
	 * @param planeNormal
	 * @param planePoint
	 * @return
	 * @throws
	 */
	// generate the plane equation of a triangle
	private static boolean triPlaneNormalPoint(double[][] tri, double[] planeNormal, double[] planePoint) {
		if (tri == null || tri.length != 3 || tri[0].length != 3) {
			return false;
		}
		// create vectors
		HVector triB = new HVector(tri[B]);
		HVector triA = new HVector(tri[A]);
		HVector triC = new HVector(tri[C]);
		HVector hv1 = triB.sub(triA);
		HVector hv2 = triC.sub(triA);
				
		//calculate the plane normal
		HVector xV1V2 = hv1.cross(hv2);
		planeNormal[X] = xV1V2.x;
		planeNormal[Y] = xV1V2.y;
		planeNormal[Z] = xV1V2.z;
		
		
		// calculate the "center" point of the triangle to use in calculating D, any point on the triangle would do
		HVector p = new HVector(new double[] {(triA.x+triB.x+triC.x)*ONE_THIRD, (triA.y+triB.y+triC.y)*ONE_THIRD, (triA.z+triB.z+triC.z)*ONE_THIRD});
		
		planePoint[X] = p.x;
		planePoint[Y] = p.y;
		planePoint[Z] = p.z;		
		
		return true;
	}
	
	// generate the plane equation of a triangle
	private static boolean triPlaneNormalPoint(float[][] tri, float[] planeNormal, float[] planePoint) {
		if (tri == null || tri.length != 3 || tri[0].length != 3) {
			return false;
		}
		// create vectors
		float[] hv1 = new float[3];
		hv1 = VectorUtil.subVec3(hv1, tri[B], tri[A]);
		float[] hv2 = new float[3];
		hv2 = VectorUtil.subVec3(hv2, tri[C], tri[A]);
				
		//calculate the plane normal
		planeNormal = VectorUtil.normalizeVec3(/* new float[3],*/ VectorUtil.crossVec3(planeNormal, 0, VectorUtil.normalizeVec3(/*new float[3],*/ hv1), 0, VectorUtil.normalizeVec3(/*new float[3],*/ hv2), 0));
		
		// calculate the "center" point of the triangle to use in calculating D, any point on the triangle would do
		planePoint[X] = (float)(((double)tri[A][X]+(double)tri[B][X]+(double)tri[C][X])/3.0); //*ONE_THIRD;
		planePoint[Y] = (float)(((double)tri[A][Y]+(double)tri[B][Y]+(double)tri[C][Y])/3.0); //*ONE_THIRD; 
		planePoint[Z] = (float)(((double)tri[A][Z]+(double)tri[B][Z]+(double)tri[C][Z])/3.0); //*ONE_THIRD;
		
		return true;
	}
	
	// generate the plane equation of a triangle
	private static boolean triPlaneNormalPoint2(double[][] tri, double[] planeNormal, double[] planePoint) {
		if (tri == null || tri.length != 3 || tri[0].length != 3) {
			return false;
		}
		// create vectors
		HVector triB = new HVector(normalizeVec3(tri[B]));
		HVector triA = new HVector(normalizeVec3(tri[A]));
		HVector triC = new HVector(normalizeVec3(tri[C]));
		HVector hv1 = triB.sub(triA);
		HVector hv2 = triC.sub(triA);
				
		//calculate the plane normal
//		HVector xV1V2 = triA.cross(triB);
		HVector xV1V2 = hv1.cross(hv2);
		planeNormal[X] = xV1V2.x;
		planeNormal[Y] = xV1V2.y;
		planeNormal[Z] = xV1V2.z;
		
		
		// calculate the "center" point of the triangle to use in calculating D, any point on the triangle would do
		HVector p = new HVector(new double[] {(triA.x+triB.x+triC.x)*ONE_THIRD, (triA.y+triB.y+triC.y)*ONE_THIRD, (triA.z+triB.z+triC.z)*ONE_THIRD});
		
		planePoint[X] = p.x;
		planePoint[Y] = p.y;
		planePoint[Z] = p.z;		
		
		return true;
	}

	// generate the plane equation of a triangle
	private static double[] triPlaneEquation(double[][] tri) {
		double[] eq = new double[4];
		
		// create vectors v1 and v2
		
		HVector triB = new HVector(tri[B]);
		HVector triA = new HVector(tri[A]);
		HVector triC = new HVector(tri[C]);
		HVector hv1 = triB.sub(triA);
		HVector hv2 = triC.sub(triA);
		HVector xV1V2 = hv1.cross(hv2);
		
		// plug the values back into Ax+By+Cz+D=0
		eq[A] = xV1V2.x;
		eq[B] = xV1V2.y;
		eq[C] = xV1V2.z;
		
		// calculate the "center" point of the triangle to use in calculating D, and point on the triangle would do
		HVector p = new HVector(new double[] {(triA.x+triB.x+triC.x)*ONE_THIRD, (triA.y+triB.y+triC.y)*ONE_THIRD, (triA.z+triB.z+triC.z)*ONE_THIRD});
		
		// D should be a point on the triangle
		eq[D] = -1*xV1V2.dot(p);

		comparePlanes(eq, tri);
		
		return eq;
	}
	
	/********************************************************
	* FUNCTION: linePlaneCollision()
	*  PURPOSE: Use parametrics to see where on the plane of
	*           tri1 the line made by a->b intersect
	******************************************************** */
	private float[] linePlaneCollision(float[] a, float[] b, Triangle tri1) {
	  double final_x=0f,final_y=0f,final_z=0f,final_t=0f;
	  double t=0f,i=0f;
	  float[] temp = new float[3];
	  
	  double [][] tri = new double[tri1.points.length][];
	  for (int j=0; j<tri1.points.length; j++) {
		  tri[j] = new double[3];
		  tri[j][X] = tri1.points[j][X];
		  tri[j][Y] = tri1.points[j][Y];
		  tri[j][Z] = tri1.points[j][Z];
	  }
	  
	  double[] planeEq = triPlaneEquation(tri);
	 
	  i+=(planeEq[A]*b[X])+(planeEq[B]*b[Y])+(planeEq[C]*b[Z])+(planeEq[D]);
	  t+=(planeEq[A]*(b[X]*-1))+(planeEq[B]*(b[Y]*-1))+(planeEq[C]*(b[Z]*-1));
	  t+=(planeEq[A]*a[X])+(planeEq[B]*a[Y])+(planeEq[C]*a[Z]);

	  // Be wary of possible divide-by-zeros here (i.e. if t==0)
	  if (Double.compare(t, 0f) == 0) {
		  return null;
	  }
	  final_t = (-i)/t;

	  // Vertical Line Segment
	  if (Float.compare(a[X], b[X]) == 0 && Float.compare(a[Z], b[Z]) == 0) { // vertical line segment
	    temp[X] = (float)a[X];
	    temp[Y] = (float)((-((planeEq[A]*a[X])+(planeEq[C]*a[Z])+(planeEq[D])))/(planeEq[B]));
	    temp[Z] = (float)a[Z];

	    return(temp);
	  }

	  final_x = (((a[X])*(final_t))+((b[X])*(1-final_t)));
	  final_y = (((a[Y])*(final_t))+((b[Y])*(1-final_t)));
	  final_z = (((a[Z])*(final_t))+((b[Z])*(1-final_t)));

	  temp[X] = (float)final_x;
	  temp[Y] = (float)final_y;
	  temp[Z] = (float)final_z;

	  return(temp);
	}

 
	/*
	   Calculate the line segment PaPb that is the shortest route between
	   two lines P1P2 and P3P4. Calculate also the values of mua and mub where
	      Pa = P1 + mua (P2 - P1)
	      Pb = P3 + mub (P4 - P3)
	   Return false if no solution exists.
	   Portions Copyright(C) Paul Bourke @ http://paulbourke.net/geometry/pointlineplane/
	*/
	private static boolean lineLineIntersect(float[] p1, float[] p2, float[] p3,
			float[] p4, float[] pa, float[] pb, double[] mua, double[] mub) {
		double EPS = Double.MIN_NORMAL;
		double[] p13 = new double[3];
		double[] p43 = new double[3];
		double[] p21 = new double[3];
		double d1343 = 0, d4321 = 0, d1321 = 0, d4343 = 0, d2121 = 0;
		double numer = 0, denom = 0;

		p13[X] = p1[X] - p3[X];
		p13[Y] = p1[Y] - p3[Y];
		p13[Z] = p1[Z] - p3[Z];
		p43[X] = p4[X] - p3[X];
		p43[Y] = p4[Y] - p3[Y];
		p43[Z] = p4[Z] - p3[Z];
		if (Math.abs(p43[X]) < EPS && Math.abs(p43[Y]) < EPS
				&& Math.abs(p43[Z]) < EPS) {
			return false;
		}
		p21[X] = p2[X] - p1[X];
		p21[Y] = p2[Y] - p1[Y];
		p21[Z] = p2[Z] - p1[Z];
		if (Math.abs(p21[X]) < EPS && Math.abs(p21[Y]) < EPS
				&& Math.abs(p21[Z]) < EPS) {
			return false;
		}

		d1343 = p13[X] * p43[X] + p13[Y] * p43[Y] + p13[Z] * p43[Z];
		d4321 = p43[X] * p21[X] + p43[Y] * p21[Y] + p43[Z] * p21[Z];
		d1321 = p13[X] * p21[X] + p13[Y] * p21[Y] + p13[Z] * p21[Z];
		d4343 = p43[X] * p43[X] + p43[Y] * p43[Y] + p43[Z] * p43[Z];
		d2121 = p21[X] * p21[X] + p21[Y] * p21[Y] + p21[Z] * p21[Z];

		denom = d2121 * d4343 - d4321 * d4321;
		
		numer = d1343 * d4321 - d1321 * d4343;
		
		if (Math.abs(denom) < EPS && Double.compare(d4343, 0.0) == 0 && Double.compare(d1343, 0.0) == 0) {
			// lines are coincident 
			pa[X] = p3[X];
			pa[Y] = p3[Y];
			pa[Z] = p3[Z];
			pb[X] = p4[X];
			pb[Y] = p4[Y];
			pb[Z] = p4[Z];
			
			return true;
		} else if (Math.abs(denom) < EPS || Double.compare(d4343, 0.0) == 0) {
			// lines are parallel or we will get divide by zero errors
			return false;
		}

		mua[0] = numer / denom;
		mub[0] = (d1343 + d4321 * (mua[0])) / d4343;
		
		if ((mua[0] < 0f || mua[0] > 1f) && (mub[0] < 0f || mub[0] > 1f)) {
			return false;
		}

		pa[X] = (float) (p1[X] + mua[0] * p21[X]);
		pa[Y] = (float) (p1[Y] + mua[0] * p21[Y]);
		pa[Z] = (float) (p1[Z] + mua[0] * p21[Z]);
		pb[X] = (float) (p3[X] + mub[0] * p43[X]);
		pb[Y] = (float) (p3[Y] + mub[0] * p43[Y]);
		pb[Z] = (float) (p3[Z] + mub[0] * p43[Z]);

		return true;
	}
	
	/**
	 * This method calculates the intersection line segment between 2 lines (not segments).
	 * If no solution can be found, the returned points of the intersection line segment will both be zero.
	 * 
	 * @param line1Point1 first point of first line
	 * @param line1Point2 second point of first line
	 * @param line2Point1 first point of second line
	 * @param line2Point2 second point of second line
	 * @param resultSegmentPoint1 first point of the intersection line segment
	 * @param resultSegmentPoint2 second point of the intersection line segment
	 * @return false if no solution can be found.
	 */
	public static boolean calculateLineLineIntersection(Vector3D line1Point1, Vector3D line1Point2, 
		Vector3D line2Point1, Vector3D line2Point2, Vector3D resultSegmentPoint1, Vector3D resultSegmentPoint2)
	{
	   // Algorithm is ported from the C algorithm of 
	   // Paul Bourke at http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline3d/
	   resultSegmentPoint1 = Vector3D.getZero();
	   resultSegmentPoint2 = Vector3D.getZero();
	 
	   Vector3D p1 = line1Point1;
	   Vector3D p2 = line1Point2;
	   Vector3D p3 = line2Point1;
	   Vector3D p4 = line2Point2;
	   Vector3D p13 = p1.subtract(p3);
	   Vector3D p43 = p4.subtract(p3);
	 
	   if (p43.getMagnitudeSquared() < FloatUtil.EPSILON) {
	      return false;
	   }
	   Vector3D p21 = p2.subtract(p1);
	   if (p21.getMagnitudeSquared() < FloatUtil.EPSILON) {
	      return false;
	   }
	 
	   double d1343 = p13.getX() * (double)p43.getX() + (double)p13.getY() * p43.getY() + (double)p13.getZ() * p43.getZ();
	   double d4321 = p43.getX() * (double)p21.getX() + (double)p43.getY() * p21.getY() + (double)p43.getZ() * p21.getZ();
	   double d1321 = p13.getX() * (double)p21.getX() + (double)p13.getY() * p21.getY() + (double)p13.getZ() * p21.getZ();
	   double d4343 = p43.getX() * (double)p43.getX() + (double)p43.getY() * p43.getY() + (double)p43.getZ() * p43.getZ();
	   double d2121 = p21.getX() * (double)p21.getX() + (double)p21.getY() * p21.getY() + (double)p21.getZ() * p21.getZ();
	 
	   double denom = d2121 * d4343 - d4321 * d4321;
	   if (Math.abs(denom) < FloatUtil.EPSILON) {
	      return false;
	   }
	   double numer = d1343 * d4321 - d1321 * d4343;
	 
	   double mua = numer / denom;
	   double mub = (d1343 + d4321 * (mua)) / d4343;
	 
	   resultSegmentPoint1 = new Vector3D(p1.getX() + mua * p21.getX(), p1.getY() + mua * p21.getY(), p1.getZ() + mua * p21.getZ());
	   resultSegmentPoint2 = new Vector3D(p3.getX() + mub * p43.getX(), p3.getY() + mub * p43.getY(), p3.getZ() + mub * p43.getZ());

	   return true;
	}
	
	//Calculate the intersection point of two lines. Returns true if lines intersect, otherwise false.
	//Note that in 3d, two lines do not intersect most of the time. So if the two lines are not in the 
	//same plane, don't use this method.
	private static boolean lineLineIntersection(float[] intersection, float[] linePoint1, float[] lineVec1, float[] linePoint2, float[] lineVec2) {
 
		intersection[X] = 0f;
		intersection[Y] = 0f;
		intersection[Z] = 0f;
		
		float[] lineVec3 = new float[3];
		
		lineVec3 = VectorUtil.subVec3(lineVec3, linePoint2, linePoint1);
 
		float[] crossVec1and2 = new float[3];
		crossVec1and2 = VectorUtil.crossVec3(crossVec1and2, lineVec1, lineVec2);
		float[] crossVec3and2 = new float[3];
		crossVec3and2 = VectorUtil.crossVec3(crossVec3and2, lineVec3, lineVec2);
 
		float planarFactor = VectorUtil.dotVec3(lineVec3, crossVec1and2);
 
		// Lines are not coplanar. Take into account rounding errors.
		if((planarFactor >= 0.00001f) || (planarFactor <= -0.00001f)) {
			log.aprintln("Intersecting lines are not coplanar!");
			return false;
		}
 
		//Note: normSquareVec3() does x*x+y*y+z*z on the input vector.
		float v1v2Sqr = VectorUtil.normSquareVec3(crossVec1and2);
		float s = VectorUtil.dotVec3(crossVec3and2, crossVec1and2) / v1v2Sqr;
 
		if((s >= 0.0f) && (s <= 1.0f)) {
			float[] v1Scaled = new float[3];
			v1Scaled = VectorUtil.scaleVec3(v1Scaled, lineVec1, s);
			intersection = VectorUtil.addVec3(intersection, linePoint1, v1Scaled);
			return true;
		}
 
		else {
			return false;       
		}
	}
	
	private boolean looseOctTreeProjectLine(float[] lineStart, float[] lineEnd, ArrayList<Triangle> list, HashMap<Integer, 
			TriLineIntersect> inters, ArrayList<float[]> line, ArrayList<Triangle> insideTris, Polygon polygon, float[] altCenter) {
		// create the triangle to intersect
		float[] start = new float[3];
		float[] end = new float[3];
		float[] center = new float[] {0f, 0f, 0f};
		start[X] = lineStart[X];
		start[Y] = lineStart[Y];
		start[Z] = lineStart[Z];
		end[X] = lineEnd[X];
		end[Y] = lineEnd[Y];
		end[Z] = lineEnd[Z];
		float[][] poly = new float[3][3];
		poly[0] = start;
		poly[1] = end;
		poly[2] = new float[]{0F, 0F, 0F};
		// if altCenter is not null and is valid we use it for the geometric center of the OctTree for all intersect operations
		// initially this was for fitting tiles exactly to Lat/Lon boundaries
		if (altCenter != null && altCenter.length == 3) {
			center = altCenter;
		}
		
		ArrayList<Float> points = new ArrayList<>();
		float[] planeNorm = new float[3];
		float[] planePt = new float[3];
		float[] temp1 = new float[3];
		float[] temp2 = new float[3];
		float[] ac = new float[3];
		float[] ab = new float[3];
		float[] ap = new float[3];
		float[] startIntersection = new float[3];
		float[] endIntersection = new float[3];
		boolean startAdded = false;
		boolean endFound = false;
		float[] startPoint = new float[3];
		ArrayList<Triangle> startList = new ArrayList<>();
		Ray startRay = new Ray(lineStart, new float[]{-lineStart[X], -lineStart[Y], -lineStart[Z]});
		
		// TODO: Tight fitting fails if this uses the q version...
		looseOctTreeRayIntersectShortCircuit(startRay, this.root, startList);
		if (startList.size() < 1) {
			return false;
		} else {
			startPoint = startList.get(0).getIntersection();
		}
		
		ArrayList<float[]> tempLine = new ArrayList<>();
		for (Triangle p : list) {
			// START of moving the line into the plane of the triangle to determine edge intercept points
			points.clear();
			planeNorm = clearVector(planeNorm);
			planePt = clearVector(planePt);
			temp1 = clearVector(temp1);
			temp2 = clearVector(temp2);
			ac = clearVector(ac);
			ab = clearVector(ab);
			ap = clearVector(ap);
			startIntersection = new float[3];
			endIntersection = new float[3];			
			startAdded = false;
			endFound = false;

			startIntersection = OctTree.linePlaneIntersectTri(startIntersection, center, start, p);
			if ( startIntersection == null) {
				// this means the plane of the triangle and the the end line we are trying to intersect with are parallel
				// so let's try taking the difference between the start vector and the end vector and see if we get a second
				// intersect point
				float[] diff = new float[3];
				diff = VectorUtil.subVec3(diff, start, end);
				startIntersection = OctTree.linePlaneIntersectTri(startIntersection, center, diff, p);
				if (startIntersection == null) {
					// ok let's try reversing the direction of diff since the spice intersect routine uses diff as a Ray
					float[] negDiff = new float[]{-1f * diff[X], -1f * diff[Y], -1f * diff[Z]};
					startIntersection = OctTree.linePlaneIntersectTri(startIntersection, center, negDiff, p);
					if (startIntersection == null) {
						// ok something really weird is wrong...panic!!!
						startAdded = false;
						endFound = false;
						continue;
					}
				}
			}
			endIntersection = OctTree.linePlaneIntersectTri(endIntersection, center, end, p);
			if ( endIntersection == null) {
				// this means the plane of the triangle and the the end line we are trying to intersect with are parallel
				// so let's try taking the difference between the start vector and the end vector and see if we get a second
				// intersect point
				float[] diff = new float[3];
				diff = VectorUtil.subVec3(diff, end, start);
				endIntersection = OctTree.linePlaneIntersectTri(endIntersection, center, diff, p);
				if (endIntersection == null) {
					// ok let's try reversing the direction of diff since the spice intersect routine uses diff as a Ray
					float[] negDiff = new float[]{-1f * diff[X], -1f * diff[Y],-1f * diff[Z]};
					endIntersection = OctTree.linePlaneIntersectTri(endIntersection, center, negDiff, p);
					if (endIntersection == null) {
						// ok something really weird is wrong...panic!!!
						startAdded = false;
						endFound = false;
						continue;
					}
				}
			}

			// if the point is inside the triangle it is our start vertex = add the point
			if (startIntersection != null && VectorUtil.isInTriangleVec3(p.points[0], p.points[1], p.points[2], startIntersection, ac, ab, ap)) {
				startAdded = true;
			}
			// find the point where the line end vector intersects the plane
			// if the point is inside the triangle it is our end vertex - add the point we are done, the line is entirely within the triangle
			if (endIntersection != null && (VectorUtil.isInTriangleVec3(p.points[0], p.points[1], p.points[2], endIntersection, ac, ab, ap))) {
				endFound = true;
			}
			// END of moving the line into the plane of the triangle
			
			TriLineIntersect tInt = null;
			if (inters.containsKey(p.id)) {
				tInt = inters.get(p.id);
			} else {
				tInt = new TriLineIntersect(p);
			}

			// sort out the corner cases
			if (startAdded && endFound) { // both points are in the triangle
				tempLine.add(startIntersection);
				tempLine.add(endIntersection);
				Corner c = tInt.getCornerByLoc(startIntersection);					
				if (c != null) {
					c.start = true;
					Corner endCorner = tInt.getCornerByLoc(endIntersection);
					if (endCorner != null) {
						// we need to connect the two ends of the polygon here and remove any corner duplication
						// in the enclosing TriLineIntersect object
						
						// connect the two ends
						endCorner.end = true;
						c.next = endCorner;
						endCorner.prev = c;
						// remove any duplicates
						tInt.removeCornerIndex(endCorner);						
					} else {
						endCorner = new Corner();
						endCorner.location = endIntersection;
						endCorner.end = true;
						c.next = endCorner;
						endCorner.prev = c;
					}
				} else {
					c = new Corner();
					c.location = startIntersection;
					c .start = true;
					Corner endCorner = new Corner();
					endCorner.location = endIntersection;
					endCorner.end = true;
					c.next = endCorner;
					endCorner.prev = c;
					tInt.addCorner(c);
					tInt.hasCorner = true;
					inters.put(p.id,tInt);
				}
				startAdded = false;
				endFound = false;
				continue;
			} else if (startAdded && !endFound) {
				boolean degenerate = false;
				// find the exit intersection
				float[][] newPoint = intersectSegmentTriangle(startIntersection, endIntersection, p.points[0], p.points[1], p.points[2]);
				if (newPoint != null) {
					if (newPoint.length == 2) {
						if (VectorUtil.isVec3Zero(VectorUtil.normalizeVec3(VectorUtil.crossVec3(new float[3], VectorUtil.normalizeVec3(new float[3], newPoint[0]), VectorUtil.normalizeVec3(new float[3], newPoint[1]))), 0, MINI_EPSILON)) {
							if (polygon != null && isPole(newPoint[0]) && !isTriangleContainedWithinPolygon(float1DTo2DArray(polygon.getOrigPoints(), 3), p.points, Z)) {
								float[] planePoint = new float[3];
								planeFrom3Points(new float[3], planePoint,p.points[0], p.points[1], p.points[2]);
								// if not don't do anything
								if (!pointLeftOfCcwLine(lineStart, lineEnd, planePoint)) {
									tempLine.add(newPoint[0]);
									continue;
								} 
							}  else if (polygon != null && isPole(newPoint[0]) && isTriangleContainedWithinPolygon(float1DTo2DArray(polygon.getOrigPoints(), 3), p.points, Z)) {
								if (!insideTris.contains(p)) {
									insideTris.add(p);
								}
								tempLine.add(newPoint[0]);
								continue;

							}
						}
						// we need to test for the degenerate case of the start intersection (i.e. the polygon vertex)
						// coinciding with one of the facet (triangle) vertices
						else if (VectorUtil.isVec3Equal(newPoint[0], 0, startIntersection, 0, OctTree.MINI_EPSILON) ||
								VectorUtil.isVec3Equal(newPoint[1], 0, startIntersection, 0, OctTree.MINI_EPSILON)) {
							for (int i=0; i<p.points.length; i++) {
								if (VectorUtil.isVec3Equal(newPoint[0], 0, p.points[i], 0, OctTree.MINI_EPSILON) || 
										VectorUtil.isVec3Equal(newPoint[1], 0, p.points[i], 0, OctTree.MINI_EPSILON)) {
									// we have the degenerate case so let's make it a straight intersection case
									IntersectingLine il = new IntersectingLine();
									// this should always be two points!
									il.startPt = newPoint[0];
									il.endPt = newPoint[1];
									tInt.addIntersectingLine(il);
									tInt.hasIntersection = true;
									inters.put(p.id, tInt);
									tempLine.add(newPoint[0]);
									tempLine.add(newPoint[1]);
									degenerate = true;
									break;
								}
							}
						}
					} 
				
				
					if (newPoint != null && !degenerate) {
						Corner c = tInt.getCornerByLoc(startIntersection);	
						if (c == null) {
							c = new Corner();
							c.location = startIntersection;
							tInt.addCorner(c);
							tInt.hasCorner = true;
							inters.put(p.id,tInt);
						}
						// set the start direction
						c.startDirection = new float[3];
						c.startDirection = VectorUtil.subVec3(c.startDirection, endIntersection, startIntersection);
						c.start = true;
						for (int i=0; i<newPoint.length; i++) {
							if (newPoint[i] != null && newPoint[i][X] != Float.NaN && !VectorUtil.isVec3Equal(c.location, 0, newPoint[i], 0, OctTree.MINI_EPSILON)) {
								// if we have a corner we don't want to keep any intersects that are beyond the start of the line
								float[] testV = new float[3];
								testV = VectorUtil.subVec3(testV, newPoint[i], c.location);
								// need to add the start/end points in the correct order
								if (VectorUtil.dotVec3(testV, c.startDirection) > 0f) {
									c.startIntersects.add(newPoint[i]);
									tempLine.add(newPoint[i]);
									tempLine.add(startIntersection);
								}
							}
						}
					}
				}
				
			} else if (endFound && !startAdded) {
				boolean degenerate = false;
				// find the exit intersection
				float[][] newPoint = intersectSegmentTriangle(startIntersection, endIntersection, p.points[0], p.points[1], p.points[2]);
				
				if (newPoint != null) {
					if (newPoint.length == 2) {
						if (VectorUtil.isVec3Zero(VectorUtil.normalizeVec3(VectorUtil.crossVec3(new float[3], VectorUtil.normalizeVec3(new float[3], newPoint[0]), VectorUtil.normalizeVec3(new float[3], newPoint[1]))), 0, MINI_EPSILON)) {
							if (polygon != null && isPole(newPoint[0]) && !isTriangleContainedWithinPolygon(float1DTo2DArray(polygon.getOrigPoints(), 3), p.points, Z)) {
								float[] planePoint = new float[3];
								planeFrom3Points(new float[3], planePoint,p.points[0], p.points[1], p.points[2]);
								// if not don't do anything
								if (!pointLeftOfCcwLine(lineStart, lineEnd, planePoint)) {
									tempLine.add(newPoint[0]);
									continue;
								} 
							}  else if (polygon != null && isPole(newPoint[0]) && isTriangleContainedWithinPolygon(float1DTo2DArray(polygon.getOrigPoints(), 3), p.points, Z)) {
								if (!insideTris.contains(p)) {
									insideTris.add(p);
								}
								tempLine.add(newPoint[0]);
								continue;

							}

						}
						// we need to test for the degenerate case of the start intersection (i.e. the polygon vertex)
						// coinciding with one of the facet (triangle) vertices
						else if (VectorUtil.isVec3Equal(newPoint[0], 0, endIntersection, 0, OctTree.MINI_EPSILON) ||
								VectorUtil.isVec3Equal(newPoint[1], 0, endIntersection, 0, OctTree.MINI_EPSILON)) {
							for (int i=0; i<p.points.length; i++) {
								if (VectorUtil.isVec3Equal(newPoint[0], 0, p.points[i], 0, OctTree.MINI_EPSILON) || 
										VectorUtil.isVec3Equal(newPoint[1], 0, p.points[i], 0, OctTree.MINI_EPSILON)) {
									// we have the degenerate case so let's make it a straight intersection case
									IntersectingLine il = new IntersectingLine();
									// this should always be two points!
									il.startPt = newPoint[0];
									il.endPt = newPoint[1];
									tInt.addIntersectingLine(il);
									tInt.hasIntersection = true;
									inters.put(p.id, tInt);
									tempLine.add(newPoint[0]);
									tempLine.add(newPoint[1]);
									degenerate = true;
									break;
								}
							}
						}
					}
				
					if (newPoint != null && !degenerate) {
						Corner c = tInt.getCornerByLoc(endIntersection);	
						if (c == null) {
							c = new Corner();
							c.location = endIntersection;
							tInt.addCorner(c);
							tInt.hasCorner = true;
							inters.put(p.id,tInt);
						}
						// set the start direction
						c.endDirection = new float[3];
						c.endDirection = VectorUtil.subVec3(c.endDirection, startIntersection, endIntersection);
						c.end = true;
						for (int i=0; i<newPoint.length; i++) {
							if (newPoint[i] != null && newPoint[i][X] != Float.NaN && !VectorUtil.isVec3Equal(c.location, 0, newPoint[i], 0, OctTree.MINI_EPSILON)) {
								// if we have a corner we don't want to keep any intersects that are beyond the start of the line
								float[] testV = new float[3];
								testV = VectorUtil.subVec3(testV, newPoint[i], c.location);
								// need to add the start/end points in the correct order
								if (VectorUtil.dotVec3(testV, c.endDirection) > 0f) {
									c.endIntersects.add(newPoint[i]);
									tempLine.add(endIntersection);
									tempLine.add(newPoint[i]);
								}
							}
						}
					}
				}
				
			} else { // neither line start nor end are in triangle	
				float[][] newPoint = null;
				// do the line intersection
				// check for collinearity
				if (VectorUtil.isCollinearVec3(p.points[0], p.points[1], startIntersection) && VectorUtil.isCollinearVec3(p.points[0], p.points[1], endIntersection)){
					newPoint = findInnerPts2CollinearLineSegs(startIntersection, endIntersection, p.points[0], p.points[1]);
				} else if (VectorUtil.isCollinearVec3(p.points[2], p.points[0], startIntersection) && VectorUtil.isCollinearVec3(p.points[2], p.points[0], endIntersection)) {
					newPoint = findInnerPts2CollinearLineSegs(startIntersection, endIntersection, p.points[2], p.points[0]);
				} else if (VectorUtil.isCollinearVec3(p.points[1], p.points[2], startIntersection) && VectorUtil.isCollinearVec3(p.points[1], p.points[2], endIntersection)) {
					newPoint = findInnerPts2CollinearLineSegs(startIntersection, endIntersection, p.points[1], p.points[2]);
				} 
				
				// check if the triangle centroid is inside the collinear line/triangle intersection
				if (newPoint != null) {
					float[] planePoint = new float[3];
					planeFrom3Points(new float[3], planePoint, p.points[0], p.points[1], p.points[2]);
					// if not don't do anything
					if (!pointLeftOfCcwLine(lineStart, lineEnd, planePoint)) {
						newPoint = null;
					}					
				} else {
					newPoint = intersectSegmentTriangle(startIntersection, endIntersection, p.points[0], p.points[1], p.points[2]);
				}
				
				if (newPoint == null) {
					// bad things have happened and we should abort
				} else if (newPoint.length == 2) {	// general case of an intercepting line where the line will intersect the triangle in two locations
					// if we have intersected one of the triangle vertices I don't think we care...
					if (VectorUtil.isVec3Zero(VectorUtil.normalizeVec3(VectorUtil.crossVec3(new float[3], VectorUtil.normalizeVec3(new float[3], newPoint[0]), VectorUtil.normalizeVec3(new float[3], newPoint[1]))), 0, MINI_EPSILON)) {
						if (polygon != null && isPole(newPoint[0]) && !isTriangleContainedWithinPolygon(float1DTo2DArray(polygon.getOrigPoints(), 3), p.points, Z)) {
							tempLine.add(newPoint[0]);
							continue;
						} else {
							float[] planePoint = new float[3];
							planeFrom3Points(new float[3], planePoint,p.points[0], p.points[1], p.points[2]);
							// if not don't do anything
							if (!pointLeftOfCcwLine(lineStart, lineEnd, planePoint)) {
								tempLine.add(newPoint[0]);
								continue;
							}  else if (polygon != null && isPole(newPoint[0]) && isTriangleContainedWithinPolygon(float1DTo2DArray(polygon.getOrigPoints(), 3), p.points, Z)) {
								if (!insideTris.contains(p)) {
									insideTris.add(p);
								}
								tempLine.add(newPoint[0]);
								continue;

							}
						}
					}
					
					
					IntersectingLine il = new IntersectingLine();
					// this should always be two points!
					il.startPt = newPoint[0];
					il.endPt = newPoint[1];
					tInt.addIntersectingLine(il);
					tInt.hasIntersection = true;
					inters.put(p.id, tInt);
					tempLine.add(newPoint[0]);
					tempLine.add(newPoint[1]);
				} else {
					// single point intersection means a single vertex intercept or colinear with side of the triangle
					if (polygon != null && isPole(newPoint[0]) && isTriangleContainedWithinPolygon(float1DTo2DArray(polygon.getOrigPoints(), 3), p.points, Z)) {
						// the entire triangle is inside the line so keep it
						insideTris.add(p);
					}
					tempLine.add(newPoint[0]);
				}
			}
		}
		
		float[][] temp = new float[tempLine.size()][];
		for (int j=0; j<tempLine.size(); j++) {
			temp[j] = tempLine.get(j);
		}
		temp = this.sort3DLine(startPoint, temp);
		temp = OctTree.removeAdjacentDuplicatePoints(temp);
		if (temp == null) {
			log.aprintln("Failed line fit.");
			return false;
		}
		ArrayList<float[]> sorted = new ArrayList<>();
		for (int k=0; k<temp.length; k++) {
			sorted.add(temp[k]);
		}
		line.addAll(sorted);
		
		return true;
	}
	
	/**
	 * Method to determine if a 3D point is located on a Geographic pole.
	 * @param point
	 * @return true if the point is located at +/- 90 degrees
	 */
	public static boolean isPole(float[] point) {
		if (point == null || point.length != 3) {
			log.aprintln("Cannot verify a point is located at the pole.");
			return false;
		}
		boolean isPole = false;
        HVector temp = new HVector();
	    	temp = temp.set(point[X], point[Y], point[Z]);
	    	double latitude = temp.lat();
	    	
	    	if (FloatUtil.isEqual((float)latitude, 90.0f, MINI_EPSILON) ||
	    			FloatUtil.isEqual((float)latitude, -90.0f, MINI_EPSILON)) {
	    		isPole = true;
	    	}
                
        return isPole;
	}
	
	private static float[][] intersectSegmentTriangle(float[] lineStart, float[] lineEnd, float[] v1, float[] v2, float[] v3) {
		// intersect the segment with each side of the triangle and take the closest intersection
		// we will only return the closest intersection point
		final float zeroLineEpsilon = 0f - OctTree.EPSILON; 
		final float unityLineEpsilon = 1f + OctTree.EPSILON;
		
		float[] inter1 = new float[3];
		float[] inter2 = new float[3];
		float[] inter3 = new float[3];
		float[][] list = null;
		
		float[] inter1b = new float[3];
		float[] inter2b = new float[3];
		float[] inter3b = new float[3];
		
		double[] mua1 = new double[]{0};
		double[] mub1 = new double[]{0};
		double[] mua2 = new double[]{0};
		double[] mub2 = new double[]{0};
		double[] mua3 = new double[]{0};
		double[] mub3 = new double[]{0};
		
		// short circuit code for the case where the intersecting line is collinear with one side of the triangle
		if (VectorUtil.isCollinearVec3(v1, v2, lineStart) && VectorUtil.isCollinearVec3(v1, v2, lineEnd)){
			return findInnerPts2CollinearLineSegs(lineStart, lineEnd, v1, v2);
		} else if (VectorUtil.isCollinearVec3(v3, v1, lineStart) && VectorUtil.isCollinearVec3(v3, v1, lineEnd)) {
			return findInnerPts2CollinearLineSegs(lineStart, lineEnd, v3, v1);
		} else if (VectorUtil.isCollinearVec3(v2, v3, lineStart) && VectorUtil.isCollinearVec3(v2, v3, lineEnd)) {
			return findInnerPts2CollinearLineSegs(lineStart, lineEnd, v2, v3);
		}
		
		// intersect the line with each side of the triangle in CCW sequence
		boolean side1 = lineLineIntersect(lineStart, lineEnd, v1, v2, inter1, inter1b, mua1, mub1);
		if (side1) {
			// determine which resulting intercept is valid
			if (mub1[0] >= zeroLineEpsilon && mub1[0] <= unityLineEpsilon && mua1[0] >= zeroLineEpsilon && mua1[0] <= unityLineEpsilon) {
				// nop1
			} else {
				side1 = false;
			}
		}
		
		boolean side2 = lineLineIntersect(lineStart, lineEnd, v2, v3, inter2, inter2b, mua2, mub2);
		if (side2) {
			// determine which resulting intercept is valid
			if (mub2[0] >= zeroLineEpsilon && mub2[0] <= unityLineEpsilon && mua2[0] >= zeroLineEpsilon && mua2[0] <= unityLineEpsilon) {
				// nop
			} else {
				side2 = false;
			}								
		}
		
		boolean side3 = lineLineIntersect(lineStart, lineEnd, v3, v1, inter3, inter3b, mua3, mub3);
		if (side3) {
			// determine which resulting intercept is valid		
			if (mub3[0] >= zeroLineEpsilon && mub3[0] <= unityLineEpsilon && mua3[0] >= zeroLineEpsilon && mua3[0] <= unityLineEpsilon) {
				// nop
			} else {
				side3 = false;
			}						
		}
		
		if (side1 || side2 || side3) {
			if (side1 && !side2 && !side3) { // 100
				list = new float[1][3];
				list[0] = inter1;
				return list;
			} else if (!side1 && side2 && !side3) { // 010
				list = new float[1][3];
				list[0] = inter2;
				return list;
			} else if (!side1 && !side2 && side3) { // 001
				list = new float[1][3];
				list[0] = inter3;
				return list;
			} else if (side1 && side2 && !side3) { // 110
				list = new float[2][3];
				if (VectorUtil.distSquareVec3(lineStart, inter1) <= VectorUtil.distSquareVec3(lineStart, inter2)) {
					list[0] = inter1;
					list[1] = inter2;
					return list;
				} else {
					list[0] = inter2;
					list[1] = inter1;
					return list;
				}
			} else if (side1 && !side2 && side3) { // 101
				list = new float[2][3];
				if (VectorUtil.distSquareVec3(lineStart, inter1) <= VectorUtil.distSquareVec3(lineStart, inter3)) {
					list[0] = inter1;
					list[1] = inter3;
					return list;
				} else {
					list[0] = inter3;
					list[1] = inter1;
					return list;
				}
			} else if (!side1 && side2 && side3) { // 011
				list = new float[2][3];
				if (VectorUtil.distSquareVec3(lineStart, inter2) <= VectorUtil.distSquareVec3(lineStart, inter3)) {
					list[0] = inter2;
					list[1] = inter3;
					return list;
				} else {
					list[0] = inter3;
					list[1] = inter2;
					return list;
				}
			} else if (side1 && side2 && side3) { // 111
				// we may have crossed a vertex
				// need to see if 2 of the intersects are essentially equal
				boolean i12 = VectorUtil.isVec3Equal(inter1, 0, inter2, 0, MINI_EPSILON);
				boolean i13 = VectorUtil.isVec3Equal(inter1, 0, inter3, 0, MINI_EPSILON);
				boolean i23 = VectorUtil.isVec3Equal(inter2, 0, inter3, 0, MINI_EPSILON);
				
				if (i12) {
					// test to see if inter3 is in the triangle
					if (isPointInPolygon(float2DTo1DArray(new float[][]{v1, v2, v3}, 3), inter3, MINI_EPSILON)) {
						list = new float[2][3];
						if (VectorUtil.distSquareVec3(lineStart, inter1) <= VectorUtil.distSquareVec3(lineStart, inter3)) {
							list[0] = inter1;
							list[1] = inter3;
							return list;
						} else {
							list[0] = inter3;
							list[1] = inter1;
							return list;
						}
					}					
				} else if (i13) {
					// test to see if inter2 is in the triangle
					if (isPointInPolygon(float2DTo1DArray(new float[][]{v1, v2, v3}, 3), inter2, MINI_EPSILON)) {
						list = new float[2][3];
						if (VectorUtil.distSquareVec3(lineStart, inter1) <= VectorUtil.distSquareVec3(lineStart, inter2)) {
							list[0] = inter1;
							list[1] = inter2;
							return list;
						} else {
							list[0] = inter2;
							list[1] = inter1;
							return list;
						}
					}
					
				} else if (i23) {
					// test to see if inter1 is in the triangle
					if (isPointInPolygon(float2DTo1DArray(new float[][]{v1, v2, v3}, 3), inter1, MINI_EPSILON)) {
						list = new float[2][3];
						if (VectorUtil.distSquareVec3(lineStart, inter1) <= VectorUtil.distSquareVec3(lineStart, inter3)) {
							list[0] = inter1;
							list[1] = inter3;
							return list;
						} else {
							list[0] = inter3;
							list[1] = inter1;
							return list;
						}
					}
					
				}

				list = new float[2][3];
				float dist1 = VectorUtil.distSquareVec3(lineStart, inter1);
				float dist2 = VectorUtil.distSquareVec3(lineStart, inter2);
				float dist3 = VectorUtil.distSquareVec3(lineStart, inter3);
				if (dist2 <= dist3) { // 2 < 3
					if (dist1 <= dist3) { // 1 < 3
						if (dist1 <= dist2) { // 1 < 2 < 3
							list[0] = inter1;
							list[1] = inter2;
							return list;
						} else { // 2 < 1 < 3
							list[0] = inter2;
							list[1] = inter1;
							return list;
						}
					} else { // 2 < 3 < 1
						list[0] = inter2;
						list[1] = inter3;
						return list;
					}
				} else { // 3 < 2
					if (dist1 <= dist2) { // 1 < 2
						if (dist1 <= dist3) { // 1 < 3 < 2
							list[0] = inter1;
							list[1] = inter3;
							return list;
						} else { // 3 < 1 < 2
							list[0] = inter3;
							list[1] = inter1;
							return list;
						}
					} else { // 3 < 2 < 1
						list[0] = inter3;
						list[1] = inter2;
						return list;
					}
				}
				
			} else {
				log.aprint("Found an intersection but can't determine if its the closest edge!\n");
				return null;
			}
		} else {
			return null;
		}
	}
	
	private static float[][] findInnerPts2CollinearLineSegs(float[] start, float[] end, float[] v1, float[] v2) {
		float[][] inner = new float[2][];
		float[] v1s = VectorUtil.subVec3(new float[3], v1, start);
		float[] es = VectorUtil.subVec3(new float[3], end, start);
		float[] ev2 = VectorUtil.subVec3(new float[3], end, v2);
		
		float v1sDotEs = VectorUtil.dotVec3(v1s, es); 
		float ev2DotEs = VectorUtil.dotVec3(ev2, es);
		
		if (v1sDotEs >= 0 && ev2DotEs >= 0) {
			inner[0] = v1;
			inner[1] = v2;
		} else if (v1sDotEs < 0 && ev2DotEs < 0) {
			inner[0] = start;
			inner[1] = end;
		} else if (v1sDotEs < 0 && ev2DotEs >= 0) {
			inner[0] = start;
			inner[1] = v2;
		} else if (v1sDotEs >= 0 && ev2DotEs < 0) {
			inner[0] = v1;
			inner[1] = end;
		} else {
			inner[0] = v1;
			inner[1] = v2;
		}		
		
		return inner;
	}

	/**
	 * Determines where a line segment intersects the perimeter of a given triangle.	
	 * The input triangle vertices must be in Counter-Clockwise Winding order (CCW). 
	 *
	 * @param lineStart
	 * @param lineEnd
	 * @param v1 1st triangle vertex
	 * @param v2 2nd triangle vertex
	 * @param v3 3rd triangle vertex
	 * @return an array of 3D points [N][3] with each row index representing an intersection point
	 */
	public static float[][] intersectSegmentTriangleLoose(float[] lineStart, float[] lineEnd, float[] v1, float[] v2, float[] v3) {
		// intersect the segment with each side of the triangle and take the closest intersection
		// we will only return the closest intersection point
		float[] inter1 = new float[3];
		float[] inter2 = new float[3];
		float[] inter3 = new float[3];
		float[][] list = null;
		
		float[] inter1b = new float[3];
		float[] inter2b = new float[3];
		float[] inter3b = new float[3];
		
		double[] mua = new double[]{0};
		double[] mub = new double[]{0};
		
		float[] ac = new float[3];
		float[] ab = new float[3];
		float[] ap = new float[3];
		float[] ac1 = new float[3];
		float[] ab1 = new float[3];
		float[] ap1 = new float[3];
		float[][] tri = new float[][]{v1, v2, v3};

		boolean side1 = lineLineIntersect(lineStart, lineEnd, v1, v2, inter1, inter1b, mua, mub);
		if (side1) {
			// determine which resulting intercept is valid
			if (mua[0] >= 0f && mua[0] <= 1f && pointInsideOrEdgeTriangle(tri, inter1)) {
				// NOP
			} else if (mub[0] >= 0f && mub[0] <= 1f && pointInsideOrEdgeTriangle(tri, inter1b)) {
				inter1 = inter1b;
			} else {
				side1 = false;
			}			
		}
		mua[0] = 0.0; mub[0] = 0.0;
		ac = clearVector(ac);
		ab = clearVector(ab);
		ap = clearVector(ap);
		ac1 = clearVector(ac1);
		ab1 = clearVector(ab1);
		ap1 = clearVector(ap1);
		boolean side2 = lineLineIntersect(lineStart, lineEnd, v2, v3, inter2, inter2b, mua, mub);
		if (side2) {
			// determine which resulting intercept is valid
			if (mua[0] >= 0f && mua[0] <= 1f && pointInsideOrEdgeTriangle(tri, inter2)) {
				// NOP
			} else if (mub[0] >= 0f && mub[0] <= 1f  && pointInsideOrEdgeTriangle(tri, inter2b)) {
				inter2 = inter2b;
			} else {
				side2 = false;
			}
		}
		mua[0] = 0.0; mub[0] = 0.0;
		ac = clearVector(ac);
		ab = clearVector(ab);
		ap = clearVector(ap);
		ac1 = clearVector(ac1);
		ab1 = clearVector(ab1);
		ap1 = clearVector(ap1);
		boolean side3 = lineLineIntersect(lineStart, lineEnd, v3, v1, inter3, inter3b, mua, mub);
		if (side3) {
			// determine which resulting intercept is valid		
			if (mua[0] >= 0f && mua[0] <= 1f && pointInsideOrEdgeTriangle(tri, inter3)) {
				// NOP
			} else if (mub[0] >= 0f && mub[0] <= 1f && pointInsideOrEdgeTriangle(tri, inter3b)) {
				inter3 = inter3b;
			} else {
				side3 = false;
			}
		}
		
		if (side1 || side2 || side3) {
			if (side1 && !side2 && !side3) { // 100
				list = new float[1][3];
				list[0] = inter1;
				return list;
			} else if (!side1 && side2 && !side3) { // 010
				list = new float[1][3];
				list[0] = inter2;
				return list;
			} else if (!side1 && !side2 && side3) { // 001
				list = new float[1][3];
				list[0] = inter3;
				return list;
			} else if (side1 && side2 && !side3) { // 110
				list = new float[2][3];
				if (VectorUtil.distSquareVec3(lineStart, inter1) <= VectorUtil.distSquareVec3(lineStart, inter2)) {
					list[0] = inter1;
					list[1] = inter2;
					return list;
				} else {
					list[0] = inter2;
					list[1] = inter1;
					return list;
				}
			} else if (side1 && !side2 && side3) { // 101
				list = new float[2][3];
				if (VectorUtil.distSquareVec3(lineStart, inter1) <= VectorUtil.distSquareVec3(lineStart, inter3)) {
					list[0] = inter1;
					list[1] = inter3;
					return list;
				} else {
					list[0] = inter3;
					list[1] = inter1;
					return list;
				}
			} else if (!side1 && side2 && side3) { // 011
				list = new float[2][3];
				if (VectorUtil.distSquareVec3(lineStart, inter2) <= VectorUtil.distSquareVec3(lineStart, inter3)) {
					list[0] = inter2;
					list[1] = inter3;
					return list;
				} else {
					list[0] = inter3;
					list[1] = inter2;
					return list;
				}
			} else if (side1 && side2 && side3) { // 111
				// this is usually a corner case but given the code above, technically should never happen
				// check for duplicates i.e. a corner point
				boolean keep1 = true;
				boolean keep2 = true;
				boolean keep3 = true;
				if (VectorUtil.isVec3Equal(inter1, 0, inter2, 0, OctTree.MINI_EPSILON)) {
					keep2 = false;
				}
				if (keep2 == true && VectorUtil.isVec3Equal(inter2, 0, inter3, 0, OctTree.MINI_EPSILON)) {
					keep3 = false;
				}
				if (keep1 == true && keep3 == true && VectorUtil.isVec3Equal(inter1, 0, inter3, 0, OctTree.MINI_EPSILON)) {
					keep3 = false;
				}
				int size = 0;
				if (keep1) size++;
				if (keep2) size++;
				if (keep3) size++;
				if (size == 1) {
					list = new float[1][3];
					if (keep1) list[0] = inter1;
					if (keep2) list[0] = inter2;
					if (keep3) list[0] = inter3;
					return list;
				}
				else if (size == 2) {
					list = new float[2][3];
					if (keep1 && keep2) { // 110
						if (VectorUtil.distSquareVec3(lineStart, inter1) <= VectorUtil.distSquareVec3(lineStart, inter2)) {
							list[0] = inter1;
							list[1] = inter2;
							return list;
						} else {
							list[0] = inter2;
							list[1] = inter1;
							return list;
						}
					} else if (keep1 && keep3) { // 101
						if (VectorUtil.distSquareVec3(lineStart, inter1) <= VectorUtil.distSquareVec3(lineStart, inter3)) {
							list[0] = inter1;
							list[1] = inter3;
							return list;
						} else {
							list[0] = inter3;
							list[1] = inter1;
							return list;
						}
					} else if (keep2 && keep3) { // 011
						if (VectorUtil.distSquareVec3(lineStart, inter2) <= VectorUtil.distSquareVec3(lineStart, inter3)) {
							list[0] = inter2;
							list[1] = inter3;
							return list;
						} else {
							list[0] = inter3;
							list[1] = inter2;
							return list;
						}
					}
				}
				list = new float[2][3];
				float dist1 = VectorUtil.distSquareVec3(lineStart, inter1);
				float dist2 = VectorUtil.distSquareVec3(lineStart, inter2);
				float dist3 = VectorUtil.distSquareVec3(lineStart, inter3);
				if (dist2 <= dist3) { // 2 < 3
					if (dist1 <= dist3) { // 1 < 3
						if (dist1 <= dist2) { // 1 < 2 < 3
							list[0] = inter1;
							list[1] = inter2;
							return list;
						} else { // 2 < 1 < 3
							list[0] = inter2;
							list[1] = inter1;
							return list;
						}
					} else { // 2 < 3 < 1
						list[0] = inter2;
						list[1] = inter3;
						return list;
					}
				} else { // 3 < 2
					if (dist1 <= dist2) { // 1 < 2
						if (dist1 <= dist3) { // 1 < 3 < 2
							list[0] = inter1;
							list[1] = inter3;
							return list;
						} else { // 3 < 1 < 2
							list[0] = inter3;
							list[1] = inter1;
							return list;
						}
					} else { // 3 < 2 < 1
						list[0] = inter3;
						list[1] = inter2;
						return list;
					}
				}
			} else {
				log.aprint("Found an intersection but can't determine if its the closest edge!");
				return null;
			}
		} else {
			return null;
		}
	}


	public static float[][] removeAdjacentDuplicatePoints(float[][] points) {
		if (points == null || points.length < 1) {
			return null;
		}
		ArrayList<Float> list = new ArrayList<>();
		
		for (int i=1; i<points.length; i++) {
			if (i == 1) {
				if (!vectorsEqualWithEpsilon(points[0], points[1], 0.000001f)) {
					// add both points
					list.add(points[0][X]);
					list.add(points[0][Y]);
					list.add(points[0][Z]);
					list.add(points[1][X]);
					list.add(points[1][Y]);
					list.add(points[1][Z]);
				} else {
					// add just the last point
					list.add(points[1][X]);
					list.add(points[1][Y]);
					list.add(points[1][Z]);
				}
			} else {
				if (!vectorsEqualWithEpsilon(points[i-1], points[i], 0.000001f)) {
					// add just the last point otherwise skip it
					list.add(points[i][X]);
					list.add(points[i][Y]);
					list.add(points[i][Z]);
				}			
			}
		}
		if (!list.isEmpty()) {
			float[][] ret = new float[list.size() / 3][3];
			int k = 0;
			for (int j=0; j<list.size(); j+=3) {
				ret[k][X] = list.get(j);
				ret[k][Y] = list.get(j+1);
				ret[k][Z] = list.get(j+2);
				k++;
			}
			return ret;
		}
		return null;
	}
	
	/**
	 * Removes adjacent duplicate points (3 consecutive values...X,Y,Z) from the input array
	 *
	 * @param points
	 * @return the updated array or a null array if the input array is not valid
	 */
	private static float[] removeAdjacentDuplicatePoints(float[] points) {
		if (points == null || points.length < 3 || points.length % 3 !=0) {
			return null;
		}
		ArrayList<Float> list = new ArrayList<>();
		
		for (int i=3; i<points.length; i+=3) {
			if (!vectorsEqualWithEpsilon(new float[]{points[i-3], points[i-2], points[i-1]}, new float[]{points[i], points[i+1], points[i+2]}, 0.000001f)) {
				// add both points
				list.add(points[i-3]);
				list.add(points[i-2]);
				list.add(points[i-1]);
				list.add(points[i]);
				list.add(points[i+1]);
				list.add(points[i+2]);
			} else {
				// add just the last point
				list.add(points[i]);
				list.add(points[i+1]);
				list.add(points[i+2]);
			}
		}
		if (!list.isEmpty()) {
			float[] ret = new float[list.size()];
			for (int j=0; j<list.size(); j++) {
				ret[j] = list.get(j);
			}
			return ret;
		}
		
		return null;
	}
	
	private static boolean vectorWithinEndRange(float[] start, float[] end, float[] v) {
		float startV = VectorUtil.distSquareVec3(start, v);
		float startEnd = VectorUtil.distSquareVec3(start,end);
		if (Float.compare(startV, startEnd) < 0) {
			return true;
		} else {
			return false;
		}
	}
	
	private static boolean vectorsEqualWithEpsilon(float[] v1, float[] v2, float epsilon) {
		if (Math.abs(v1[X] - v2[X]) <= epsilon
				&& Math.abs(v1[Y] - v2[Y]) <= epsilon
				&& Math.abs(v1[Z] - v2[Z]) <= epsilon) {
			return true;
		} else {
			return false;
		}
	}
	
	private static float[] clearVector(float[] v) {
		if (v == null) {
			return v;
		}
		for (int i=0; i<v.length; i++) {
			v[i] = 0f;
		}
		return v;
	}
	
	// check if point p is in triangle a-b-c
	// Adapted from http://www.java-gaming.org/index.php?topic=30375.0
	private static boolean isPointInTriangle(float[] p, float[] a, float[] b, float[] c) {
	   // bring triangle to p coordinate frame
	   float[] pa = new float[3];
	   float[] pb = new float[3];
	   float[] pc = new float[3];
		
	   pa = VectorUtil.subVec3(pa, a, p);
	   pb = VectorUtil.subVec3(pb, b, p);
	   pc = VectorUtil.subVec3(pc, c, p);
	   
	   float ab = VectorUtil.dotVec3(pa, pb);
	   float ac = VectorUtil.dotVec3(pa, pc);
	   float bc = VectorUtil.dotVec3(pb, pc);
	   float cc = VectorUtil.dotVec3(pc, pc);
	   
	   if (bc * ac - cc * ab < 0.0f) return false;
	   
	   float bb = VectorUtil.dotVec3(b,b);
	   
	   if (ab * bc - ac * bb < 0.0f) return false;
	   
	   return true;
	}

	private float[] getTriangleCentroid(float[] v1, float[] v2, float[] v3) {
		return new float[]{(v1[X]+v2[X]+v3[X])*ONE_THIRD, (v1[Y]+v2[Y]+v3[Y])*ONE_THIRD, (v1[Z]+v2[Z]+v3[Z])*ONE_THIRD};
	}
	
	private static ArrayList<Triangle> sortTriangles(ArrayList<Triangle> tris, float[] start) {
		if (tris == null || tris.isEmpty()) {
			return tris;
		}
		
		// put all the tris into a TreeMap to eliminate duplicates
		TreeMap<Integer, Triangle> tmap = new TreeMap<>();
		for (Triangle p : tris) {
			p.compareVal = VectorUtil.distSquareVec3(start, p.getCenter());
			tmap.put(p.id, p);
		}
		Collection<Triangle> polys = tmap.values();
		ArrayList<Triangle> sortedTriangleArray = new ArrayList<>();
		sortedTriangleArray.addAll(polys);

		Collections.sort(sortedTriangleArray, new Comparator<Triangle>(){
			public int compare(Triangle o1, Triangle o2) {
				return Float.compare(o1.compareVal, o2.compareVal);
			}
		});		
				
		return sortedTriangleArray;
	}
		
	private static boolean isTriangleInsideCurve(float[][] tri, float[][] curvePts) {
		if (curvePts.length < 2) {
			// not enough points to determine anything
			return false;
		}
		
		// calculate normals for the points assuming CCW winding order
		 float[][] norms = new float[curvePts.length-1][];
		 float[][] points = new float[curvePts.length-1][];
		 for (int i=0; i<curvePts.length-1; i++) {
			 // for each 2 curve points in sequence
			 // calculate a plane point and normal to aid in determining
			 // if the triangle is on the "left" side of the curve with
			 // respect to the origin in CCW order
			double[] planeNorm = new double[3];
			double[] planePt = new double[3];
			
			triPlaneNormalPoint(new double[][]{floatToDoubleArray(curvePts[i]), floatToDoubleArray(curvePts[i+1]), {0, 0, 0}}, planeNorm, planePt);
			 
			 float[] norm = doubleToFloatArray(planeNorm);
			 float[] pt = doubleToFloatArray(planePt);
			 norms[i] = norm;
			 points[i] = pt;			 
		 }
		
		 float[] diff = new float[3];
		 for (int j=0; j<norms.length; j++) {
			 for (int k=0; k<tri.length; k++) {
				 diff = VectorUtil.subVec3(diff, tri[k], points[j]);				 
				 if (VectorUtil.dotVec3(diff, norms[j]) <= 0f) {
					 // this triangle is outside the polygon
					 return false;
				 }
			 }
		 }
		
		
		return true;
	}
	
	// input vectors must be 3D and in CCW order
	private ArrayList<Triangle> findInnerTriangle(Collection<Triangle> edges,
			float[][] poly) {

		boolean isPathological = false;
		float[] polyNorm = new float[3];
		ArrayList<Triangle> ret = new ArrayList<>();
		float[] polyCenter = avgOf3DPolygon(poly);
		// increase magnitude of polygon center by factor of three to clear the shape model
		float[] newPolyCenter = VectorUtil.scaleVec3(new float[3], polyCenter, 10f);
		// create a Ray from the origin to the extended polygon center
		// and intersect with the shape model
		ArrayList<Triangle> centerTri = new ArrayList<>();
		looseOctTreeRayIntersectShortCircuit(new Ray(newPolyCenter, new float[]{-newPolyCenter[0], -newPolyCenter[1], -newPolyCenter[2]}), this.getRoot(), centerTri);

		if (centerTri.size() == 0) {
			// check for pathological case where all the polygon points lie in a plane through the origin
			polyNorm = VectorUtil.crossVec3(new float[3], VectorUtil.subVec3(new float[3], poly[0], polyCenter), VectorUtil.subVec3(new float[3], poly[1], polyCenter));
			float[] ray = VectorUtil.scaleVec3(new float[3], polyNorm, 20f);
			looseOctTreeRayIntersectShortCircuit(new Ray(ray, new float[]{-ray[0], -ray[1], -ray[2]}), this.getRoot(), centerTri);
			if (centerTri.size() == 0) {
				log.aprintln("Cannot determine an inner triangle for the given polygon.");
				return ret;
			}
			isPathological = true;
		} else {
			for (Triangle t : centerTri) {
				if (edges.contains(t)) {
					return  new ArrayList<Triangle>();
				}
			}
		}
		
		ArrayList<Triangle> contactingTris = new ArrayList<>();		
		contactingTris.add(centerTri.get(0));
		// get all neighbor triangles
		int foundCnt = looseGetContactingPolygons(getRoot(), centerTri.get(0), contactingTris);

		return contactingTris;
	}
	
	// input vectors must be 3D and in CCW order
	private ArrayList<Triangle> findInnerTriangle(float[][] poly) {
		ArrayList<Triangle> ret = new ArrayList<>();
		ArrayList<float[]> pts = new ArrayList<>();
		
		float[][] frustum = getFrustumFromPolygon(poly);
		if (frustum == null) {
			return ret;
		}
		float[] normal = VectorUtil.normalizeVec3(avgOf3DPolygon(poly));

//		looseOctTreeFrustumIntersect(normal, frustum, this.getRoot(), ret);
//		looseOctTreeFrustumIntersectFacets(normal, frustum, this.getRoot(), ret);
		looseOctTreeFrustumIntersectPoints(normal, frustum, this.getRoot(), ret, pts);
		
		return ret;
	}
	

	public static float[][] float1DTo2DArray (float[] sa, int stride) {
		if (sa == null || sa.length % stride != 0) {
			return null;
		}
		float[][] ret = new float[sa.length / stride][stride];
		int cnt = 0;
		for (int i=0; i<sa.length; i+=stride) {
			float[] tmp = new float[stride];
			int k=0;
			for (int j=i; j<i+stride; j++) {
				tmp[k++] = sa[j];
			}
			ret[cnt++] = tmp;
		}
	
		return ret;
	}
	
	// only works for even arrays!!!
	public static float[] float2DTo1DArray (float[][] sa, int stride) {
		if (sa == null || sa[0].length % stride != 0) {
			return null;
		}
		float[] ret = new float[sa.length * stride];
		int cnt = 0;
		for (int i=0; i<sa.length; i++) {
			float[] elem = sa[i];
			for (int j=0; j<elem.length; j++) {
				ret[cnt++] =elem[j];
			}
		}	
		return ret;
	}

	
	public static double[] floatToDoubleArray(float[] f) {
		if (f == null) {
			return null;
		}
	
		double[] d = new double[f.length];
		for (int i=0; i<f.length; i++) {
			d[i] = f[i];
		}
		return d;
	}

	public static double[][] floatToDoubleArray(float[][] f) {
		if (f == null) {
			return null;
		}
	
		double[][] d = new double[f.length][3];
		for (int i=0; i<f.length; i++) {
			for (int j=0; j<3; j++) {
				d[i][j] = f[i][j];
			}
		}
		return d;
	}


	public static float[] doubleToFloatArray(double[] f) {
		if (f == null) {
			return null;
		}
	
		float[] d = new float[f.length];
		for (int i=0; i<f.length; i++) {
			d[i] = (float)f[i];
		}
		return d;
	}
	
	public static float[][] doubleToFloatArray(double[][] f) {
		if (f == null) {
			return null;
		}
	
		float[][] d = new float[f.length][3];
		for (int i=0; i<f.length; i++) {
			for (int j=0; j<3; j++) {
				d[i][j] = (float)f[i][j];
			}
		}
		return d;
	}
	
	public static float[][] sortCCW(float[][] points) {
		float[] center = new float[3];
		float[] normal = new float[3];
		
		center = avgOf3DPolygon(points);
		normal = OctTree.normalizeVec3(normal, center);
		
        float[] temp;
        for (int i = 1; i < points.length; i++) {
            for(int j = i ; j > 0 ; j--){
                if(isBClockwiseFromA(center, normal, points[j], points[j-1])){
                    temp = points[j];
                    points[j] = points[j-1];
                    points[j-1] = temp;
                }
            }
        }   		    		
		return points;
	}
	
	private static double[][] sortCCW(double[][] p) {
		float[] center = new float[3];
		float[] normal = new float[3];
		float[][] points = new float[p.length][3];
		for (int k=0; k<p.length; k++) {
			points[k] = OctTree.doubleToFloatArray(p[k]);
		}
		
		center = avgOf3DPolygon(points);
		normal = VectorUtil.normalizeVec3(normal, center);
		
        float[] temp;
        for (int i = 1; i < points.length; i++) {
            for(int j = i ; j > 0 ; j--){
                if(isBClockwiseFromA(center, normal, points[j], points[j-1])){
                    temp = points[j];
                    points[j] = points[j-1];
                    points[j-1] = temp;
                }
            }
        }   		    		
		for (int x=0; x<p.length; x++) {
			p[x] = OctTree.floatToDoubleArray(points[x]);
		}
		
		return p;
	}
	
	private static boolean isBClockwiseFromA(float[] center, float[] normal, float[] A, float[] B) {
		// if dot(n, cross(A-C, B-C)) is positive, then B is clockwise from A
		if (VectorUtil.dotVec3(normal, VectorUtil.crossVec3(new float[3], VectorUtil.subVec3(new float[3], A, center), VectorUtil.subVec3(new float[3], B, center))) > 0f) {
			return true;
		} else {
			return false;
		}
	}
	
	private float[][] sort3DLine(float[] lineStart, float[][] line) {
		ArrayList<SortVertex> sorted = new ArrayList<>();
		for (int i=0; i<line.length; i++) {
			float dist = VectorUtil.normSquareVec3(VectorUtil.crossVec3(new float[3], lineStart, line[i]));
			sorted.add(new SortVertex(dist, line[i]));
		}
		
		Collections.sort(sorted, new Comparator<SortVertex>() {
			@Override
			public int compare(SortVertex o1, SortVertex o2) {
				return Float.compare(o1.distance, o2.distance);
			}			
		});
		
		float[][] ret = new float[sorted.size()][3];
		int j =0;
		for (SortVertex sv : sorted) {
			ret[j++] = sv.vertex;
		}
		return ret;
	}
	
	private class SortVertex {
		private float distance;
		private float[] vertex;
		
		private SortVertex(float dist, float[] vert) {
			distance = dist;
			vertex = vert;
		}
	}
	
	/**
	 * Method to determine the average point of an array of 3D points
	 * @param points
	 * @return a single average point
	 */
	public static float[] avgOf3DPolygon(float[][] points) {
		float invCount = 1f/points.length;
		float[] c = new float[3];
		
		for (int i=0; i<points.length; i++) {
			c[X] += points[i][X] * invCount;
			c[Y] += points[i][Y] * invCount;
			c[Z] += points[i][Z] * invCount;
		}
		
		return c;
	}
	
	public static double[] avgOf3DPolygon(double[][] points) {
		double invCount = 1.0/points.length;
		double[] c = new double[3];
		
		for (int i=0; i<points.length; i++) {
			c[X] += points[i][X] * invCount;
			c[Y] += points[i][Y] * invCount;
			c[Z] += points[i][Z] * invCount;
		}
		
		return c;
	}
	
	
	private static float[] avgOf2DPolygon(float[][] points) {
		float[] c = new float[2];
		float invCount = 1f/points.length;
		
		for (int i=0; i<points.length; i++) {
			c[X] += points[i][X] * invCount;
			c[Y] += points[i][Y] * invCount;
		}
		
		return c;
	}
	
	private boolean isTriangleContainedWithinPolygon(float[][] poly, float[][] tri) {
		
		// find most othogonal axis to polygon
		float[] AB = VectorUtil.subVec3(new float[3], tri[B], tri[A]);
		float[] AC = VectorUtil.subVec3(new float[3], tri[C], tri[A]);
		float[] normV = OctTree.normalizeVec3(VectorUtil.crossVec3(new float[3], AB, AC)); 
		
		int axis = mostOrthoganalAxisToPlane(normV);
		
		// project polygon and triangle into 2D
		float[][] newPoly = project3DTo2D(poly, axis);
		float[][] newTri = project3DTo2D(tri, axis);
		
		// check for containment
		for (int i=0; i<newTri.length; i++) {
			if (!pointInPolygon(newTri[i], newPoly)) {
				return false;
			}
		}
		
		return true;
	}
		
	private boolean isTriangleContainedWithinPolygon(float[][] poly, float[][] tri, int orthogonalAxis) {
		
		// project polygon and triangle into 2D
		float[][] newPoly = project3DTo2D(poly, orthogonalAxis);
		float[][] newTri = project3DTo2D(tri, orthogonalAxis);
		
		// check for containment
		for (int i=0; i<newTri.length; i++) {
			if (!insideOnPolygon(newPoly, newPoly.length, newTri[i])) {				
				return false;
			}
		}
		
		return true;
	}
		
	private static int mostOrthoganalAxisToPlane(float[] planeNormal) {
		
		float xVal = FloatUtil.abs(VectorUtil.dotVec3(planeNormal, X_AXIS));
		float yVal = FloatUtil.abs(VectorUtil.dotVec3(planeNormal, Y_AXIS));
		float zVal = FloatUtil.abs(VectorUtil.dotVec3(planeNormal, Z_AXIS));
		
		// find the largest value -> most orthoganal axis
		if (xVal > yVal) {
			if (xVal > zVal) {
				return X;
			} else {
				return Z;
			}
		} else if (yVal > zVal) {
			return Y;
		} else {
			return Z;
		}		
	}

	public static Integer fastMostOrthoganalAxisToPlane(float[] planeNormal) {
		if (planeNormal == null || planeNormal.length != 3) {
			return null;
		}
		
		float xVal = FloatUtil.abs(planeNormal[X]);
		float yVal = FloatUtil.abs(planeNormal[Y]);
		float zVal = FloatUtil.abs(planeNormal[Z]);
		
		// find the largest value -> most orthoganal axis
		if (xVal > yVal) {
			if (xVal > zVal) {
				return X;
			} else {
				return Z;
			}
		} else if (yVal > zVal) {
			return Y;
		} else {
			return Z;
		}		
	}
	
	public static float[][] project3DTo2D(float[][] points, int axisToDrop) {
		if (points == null || points.length < 1 || axisToDrop < X || axisToDrop > Z) {
			return null;
		}
		float[][] ret = new float[points.length][2];
		for (int i=0; i<points.length; i++) {
			if (axisToDrop == X) {
				ret[i] = new float[]{points[i][Y], points[i][Z]};
			} else if (axisToDrop == Y) {
				ret[i] = new float[]{points[i][X], points[i][Z]};
			} else { // need to drop Z
				ret[i] = new float[]{points[i][X], points[i][Y]};
			}
		}
		return ret;
	}

	private static boolean pointInsideOrEdgeTriangle (float[][] tri, float[] point, int orthogonalAxis) {
		// flatten both triangle and point into a 2D plane 
		float[][] newTri = project3DTo2D(tri, orthogonalAxis);
		float[][] newPoint = project3DTo2D(new float[][]{point}, orthogonalAxis);
		if (newTri == null || point == null) {
			log.aprint("Invalid triangle or point definition.");
		}
		// check if point is inside triangle
		boolean in2 =  accuratePointInTriangle(newTri[A][X], newTri[A][Y], newTri[B][X],
				newTri[B][Y], newTri[C][X], newTri[C][Y], newPoint[A][X], newPoint[A][Y]);		
		
		return in2;
	}
	
	
	private static boolean pointInsideOrEdgeTriangle (float[][] tri, float[] point) {
		// flatten both triangle and point into a 2D plane 
		float[] norm = VectorUtil.crossVec3(new float[3], VectorUtil.subVec3(new float[3], tri[C], tri[B]), VectorUtil.subVec3(new float[3], tri[A], tri[B]));
		Integer orthoAxis = fastMostOrthoganalAxisToPlane(norm);
		if (orthoAxis == null) {
			log.aprint("Cannot determine most orthoganal axis.");
			return false;
		}
		float[][] newTri = project3DTo2D(tri, orthoAxis);
		float[][] newPoint = project3DTo2D(new float[][]{point}, orthoAxis);
		if (newTri == null || point == null) {
			log.aprint("Invalid triangle or point definition.");
		}
		// check if point is inside triangle
		boolean in2 =  accuratePointInTriangle(newTri[A][X], newTri[A][Y], newTri[B][X],
				newTri[B][Y], newTri[C][X], newTri[C][Y], newPoint[A][X], newPoint[A][Y]);		
		
		return in2;
	}
	
	/**
	 * Determines if a 3D point is "inside" a 3D polygon.
	 * The polygon MUST be convex or the results are indeterminate.
	 * Both point and polygon are projected into a 2D plane for the test.
	 * The definition of inside includes coincidence with a vertex or edge
	 *
	 * @param polygon
	 * @param point
	 * @param epsilon distance the point can be outside the polygon and
	 * 	still be considered "inside"
	 * @return true if the point is inside the polygon
	 */
	public static boolean isPointInPolygon(float[] polygon, float[] point, float epsilon) {
		// project everything to 2D
		float[][] tmpPoly = float1DTo2DArray(polygon, 3);
		if (tmpPoly == null) {
			log.aprintln("Invald input polygon");
			return false;
		}		
		float[] avg = avgOf3DPolygon(tmpPoly);		
		float[] planeNormal = new float[3];
		float[] planePoint = new float[3];
		planeFrom3Points(planeNormal, planePoint, avg, tmpPoly[0], tmpPoly[tmpPoly.length / 3]);
		int dropAxis = fastMostOrthoganalAxisToPlane(planeNormal);				
		float[][] poly = project3DTo2D(tmpPoly, dropAxis); 
		float[][] tmpPt = project3DTo2D(new float[][]{point}, dropAxis);
		// compute the 2D polygon "centroid"
		float[] center = avgOf2DPolygon(poly);
		// move the input point to the origin and scale it using the epsilon
		float[] diff = VectorUtil.subVec2(new float[2], tmpPt[0], center);
		float scaleFactor = 1f - epsilon;
		float[] newDiff = VectorUtil.scaleVec2(new float[2], diff, scaleFactor);
		// create the new point with epsilon included
		float[] newPt = VectorUtil.addVec2(new float[2], newDiff, center);
		return pointInPolygon(newPt, poly);
	}
	
	
	// WARNING ONLY for 2D
	private static float triSide2D(float x1, float y1, float x2, float y2, float x,
			float y) {
		return (y2 - y1) * (x - x1) + (-x2 + x1) * (y - y1);
	}

	// WARNING ONLY for 2D
	private static boolean naivePointInTriangle(float x1, float y1, float x2, float y2,
			float x3, float y3, float x, float y) {
		boolean checkSide1 = triSide2D(x1, y1, x2, y2, x, y) >= 0f;
		boolean checkSide2 = triSide2D(x2, y2, x3, y3, x, y) >= 0f;
		boolean checkSide3 = triSide2D(x3, y3, x1, y1, x, y) >= 0f;
		return checkSide1 && checkSide2 && checkSide3;
	}

	// WARNING ONLY for 2D
	private static boolean pointInTriangleBoundingBox(float x1, float y1, float x2,
			float y2, float x3, float y3, float x, float y) {
		float xMin = Math.min(x1, Math.min(x2, x3)) - EPSILON;
		float xMax = Math.max(x1, Math.max(x2, x3)) + EPSILON;
		float yMin = Math.min(y1, Math.min(y2, y3)) - EPSILON;
		float yMax = Math.max(y1, Math.max(y2, y3)) + EPSILON;

		if (x < xMin || xMax < x || y < yMin || yMax < y) {
			return false;
		} else {
			return true;
		}
	}
	
	// WARNING ONLY for 2D
	private static float distanceSquarePointToSegment(float x1, float y1, float x2, float y2,
			float x, float y) {
		float p1P2SquareLength = (x2 - x1) * (x2 - x1) + (y2 - y1)
				* (y2 - y1);
		float dotProduct = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1))
				/ p1P2SquareLength;
		if (dotProduct < 0) {
			return (x - x1) * (x - x1) + (y - y1) * (y - y1);
		} else if (dotProduct <= 1) {
			float pP1SquareLength = (x1 - x) * (x1 - x) + (y1 - y) * (y1 - y);
			return pP1SquareLength - dotProduct * dotProduct
					* p1P2SquareLength;
		} else {
			return (x - x2) * (x - x2) + (y - y2) * (y - y2);
		}
	}
	
	// WARNING ONLY for 2D
	public static double distanceSquarePointToSegment(double x1, double y1, double x2, double y2,
			double x, double y) {
		double p1P2SquareLength = (x2 - x1) * (x2 - x1) + (y2 - y1)
				* (y2 - y1);
		double dotProduct = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1))
				/ p1P2SquareLength;
		if (dotProduct < 0) {
			return (x - x1) * (x - x1) + (y - y1) * (y - y1);
		} else if (dotProduct <= 1) {
			double pP1SquareLength = (x1 - x) * (x1 - x) + (y1 - y) * (y1 - y);
			return pP1SquareLength - dotProduct * dotProduct
					* p1P2SquareLength;
		} else {
			return (x - x2) * (x - x2) + (y - y2) * (y - y2);
		}
	}
	
	// WARNING ONLY for 2D
	/*
	 * A 2D test to determine if a point is inside or on the boundary of a triangle
	 * Ported from:
	 * http://totologic.blogspot.com/2014/01/accurate-point-in-triangle-test.html
	 *
	 * @param x1 
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param x3
	 * @param y3
	 * @param x
	 * @param y
	 * @return true if point is inside or on boundary of the triangle
	 *
	 * should be thread safe
	 */
	private static boolean accuratePointInTriangle(float x1, float y1, float x2,
			float y2, float x3, float y3, float x, float y) {
		if (!pointInTriangleBoundingBox(x1, y1, x2, y2, x3, y3, x, y)) {
			return false;
		}

		if (naivePointInTriangle(x1, y1, x2, y2, x3, y3, x, y)) {
			return true;
		}

		if (distanceSquarePointToSegment(x1, y1, x2, y2, x, y) <= EPSILON_X2) {
			return true;
		}
		if (distanceSquarePointToSegment(x2, y2, x3, y3, x, y) <= EPSILON_X2) {
			return true;
		}
		if (distanceSquarePointToSegment(x3, y3, x1, y1, x, y) <= EPSILON_X2) {
			return true;
		}

		return false;
	}
	
	/// Determine whether a point P is inside the triangle ABC. Note, this function
	/// assumes that P is coplanar with the triangle.
	/// returns True if the point is inside, false if it is not.
	
	// Ported from http://blogs.msdn.com/b/rezanour/archive/2011/08/07/barycentric-coordinates-and-point-in-triangle-tests.aspx
	private static boolean pointIsInTriangle(float[] A, float[] B, float[] C, float[] P) {
	    // Prepare our barycentric variables
		float[] u = new float[3];
		float[] v = new float[3];
		float[] w = new float[3];
		// B - A
	    u = VectorUtil.subVec3(u, B, A);
	    // C - A
	    v = VectorUtil.subVec3(v, C, A);
	    // P - A;
	    w = VectorUtil.subVec3(w, P, A);
	 
	    float[] vCrossW = new float[3];
	    vCrossW = VectorUtil.crossVec3(vCrossW, v, w);
	    float[] vCrossU = new float[3];
	    vCrossU = VectorUtil.crossVec3(vCrossU, v, u);
	 
	    // Test sign of r
	    float test1 = VectorUtil.dotVec3(OctTree.normalizeVec3(vCrossW), OctTree.normalizeVec3(vCrossU));
	    if (VectorUtil.dotVec3(OctTree.normalizeVec3(vCrossW), OctTree.normalizeVec3(vCrossU)) < (0f - EPSILON)) {
	    	return false;
	    }
	 
	    float[] uCrossW = new float[3];
	    uCrossW = VectorUtil.crossVec3(uCrossW, u, w);
	    float[] uCrossV = new float[3];
	    uCrossV = VectorUtil.crossVec3(uCrossV, u, v);
	 
	    // Test sign of t
	    float test2 = VectorUtil.dotVec3(OctTree.normalizeVec3(uCrossW), OctTree.normalizeVec3(uCrossV));
	    if (VectorUtil.dotVec3(OctTree.normalizeVec3(uCrossW), OctTree.normalizeVec3(uCrossV)) < 0f - EPSILON) {
	        return false;
	    }
	 
	    // At this point, we know that r and t are both > 0.
	    // Therefore, as long as their sum is <= 1, each must be less <= 1
	    float denom = VectorUtil.normVec3(uCrossV);
	    float r = VectorUtil.normVec3(vCrossW) / denom;
	    float t = VectorUtil.normVec3(uCrossW) / denom;   

	    
//		return (r <= 1f  + EPSILON  && t <= 1f + EPSILON ? true : false); 
	 	 
	    return (r + t <= 1f/* + EPSILON*/);
	}
	
	private static boolean triTriCornerIntersect(float[][] tri1, float[][] tri2) {
		int numMatches = 0;
		for (int i=0; i<tri1.length; i++) {
			for (int j=0; j<tri2.length; j++) {
				if (FloatUtil.abs(tri1[i][X] - tri2[j][X]) < SUPER_EPSILON
						&& FloatUtil.abs(tri1[i][Y] - tri2[j][Y]) < SUPER_EPSILON
						&& FloatUtil.abs(tri1[i][Z] - tri2[j][Z]) < SUPER_EPSILON) {
						numMatches++;
				}
			}
		}
		if (numMatches > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	private static ArrayList<Triangle> edgeTriangleSubdivide(Collection<TriLineIntersect> tris) {
		ArrayList<Triangle> retTris = new ArrayList<>();
		float[][] newTris = null;

		for (TriLineIntersect tli : tris) {
			Triangle tri = tli.getTriangle();
						
	        if (tli.getNumIntersectingLines() == 1 && !tli.hasCorner) { // std triangle intersected by one line
		        	float[][] ints = new float[][]{tli.getIntersectingLines().get(0).startPt, tli.getIntersectingLines().get(0).endPt};
		        	newTris = tessellateCutTriangle(tri.points, ints);
	        } else if (tli.getNumIntersectingLines() == 0 && tli.hasCorner && tli.getNumCorners() == 1 && tli.getCorners().get(0).next == null) { // triangle is cut by one corner point
		        	newTris = tessellateSingleCornerTriangle(tli);
	        } else if (tli.getNumIntersectingLines() == 0 && tli.hasCorner && tli.getNumCorners() == 1 && tli.getCorners().get(0).next != null && OctTree.isCornerCurveConvex(tli)) {
		        	// tessellate directly
		        	float[][] curvePts = tli.getOrderedCorners();
		        	float[][] interiorPts = OctTree.findPtsInteriorToConvexCurve(tli);
		        	ArrayList<float[]> points = new ArrayList<>();
		        	for(int i=0; i<curvePts.length; i++) {
		        		points.add(curvePts[i]);
		        	}
		        	if (interiorPts != null && interiorPts.length > 0) {
			        	for(int i=0; i<interiorPts.length; i++) {
			        		points.add(interiorPts[i]);
			        	}
		        	}
		        	
		        	newTris = OctTree.tessellateConvexPolygon(points);
	        	
	        } else { // triangle is cut by more than one corner and/or more than one intersecting line
	            newTris = subDivideTriangle(tli);
	        } 
	        
	        if (newTris != null) {
	        	for (int j=0; j<newTris.length; j+=3) {
	        		Triangle newTri = new Triangle(new float[][]{newTris[j], newTris[j+1], newTris[j+2]});
	        		newTri.parentId = tri.getMeshId();
	        		retTris.add(newTri);
	        	}
	        	newTris = null;
	        }
	    }
		return retTris;
	}
	
	/*
	 * This method uses Ear Clipping to tessellate any non-trivial triangle/Line intersections
	 */
	private static ArrayList<Triangle> edgeTriangleSubdivideEarClip(Collection<TriLineIntersect> tris) {
		ArrayList<Triangle> retTris = new ArrayList<>();
		float[][] newTris = null;

		for (TriLineIntersect tli : tris) {
			Triangle tri = tli.getTriangle();
			
	        if (tli.getNumIntersectingLines() == 1 && !tli.hasCorner) { // std triangle intersected by one line
	        		float[][] ints = new float[][]{tli.getIntersectingLines().get(0).startPt, tli.getIntersectingLines().get(0).endPt};
	        		newTris = tessellateCutTriangle(tri.points, ints);
	        } else if (tli.getNumIntersectingLines() == 0 && tli.hasCorner && tli.getNumCorners() == 1 && tli.getCorners().get(0).next == null) { // triangle is cut by one corner point
	        		newTris = tessellateSingleCornerTriangle(tli);
	        } else if (tli.getNumIntersectingLines() == 0 && tli.hasCorner && tli.getNumCorners() == 1 && tli.getCorners().get(0).next != null && OctTree.isCornerCurveConvex(tli)) {
		        	// tessellate directly
		        	float[][] curvePts = tli.getOrderedCorners();
		        	float[][] interiorPts = OctTree.findPtsInteriorToConvexCurve(tli);
		        	ArrayList<float[]> points = new ArrayList<>();
		        	for(int i=0; i<curvePts.length; i++) {
		        		points.add(curvePts[i]);
		        	}
		        	if (interiorPts != null && interiorPts.length > 0) {
			        	for(int i=0; i<interiorPts.length; i++) {
			        		points.add(interiorPts[i]);
			        	}
		        	}
		        	
		        	newTris = OctTree.tessellateConvexPolygon(points);
	        	
	        } else { // triangle is cut by more than one corner and/or more than one intersecting line
		        	float[][] intersectPts = tli.getIntersections();
		        	float[][] interiorPts = OctTree.findPtsInteriorToConvexCurve(tli);
		        	
		        	ArrayList<float[]> points = new ArrayList<>();
		        	if (intersectPts != null && intersectPts.length > 0) {
			        	for(int i=0; i<intersectPts.length; i++) {
			        		points.add(intersectPts[i]);
			        	}
		        	}
	
		        	if (interiorPts != null && interiorPts.length > 0) {
			        	for(int i=0; i<interiorPts.length; i++) {
			        		points.add(interiorPts[i]);
			        	}
		        	}
		        			        		        	
		        	// find the most orthogonal axis
		        	float[] planeNormal = new float[3];
		        	if (points.size() > 0 && triPlaneNormalPoint(tri.points, planeNormal, new float[3])) {
			        	// rotate the polygon so that its surface normal vector and the Z axis are coincident prior to sorting and ear clipping
		        		
		        		// calculate the angle between the planeNormal and the Z axis in radians
		        		float angle = VectorUtil.angleVec3(planeNormal, Z_AXIS);
		        		// calculate the axis of the needed rotation to put the planeNormal coincident to the Z axis
		        		float[] rotationAxis = VectorUtil.crossVec3(new float[3], planeNormal, Z_AXIS);
		        		// craete a quaternion
		        		Quaternion quat = new Quaternion(rotationAxis[X], rotationAxis[Y], rotationAxis[Z], angle);
		        		float[][] threeDpts = new float[points.size()][];
		        		for(int i=0; i<points.size(); i++) {
		        			threeDpts[i] = points.get(i);
		        		}
		        		
		        		float[][] noDups3D = removeDuplicatePoints(threeDpts);
		        		
		        		threeDpts = noDups3D;
		        		
		        		float[][] rot3Dpts = new float[threeDpts.length][];
		        		// rotate the polygon vectors to be 
		        		for (int i=0; i<threeDpts.length; i++) {
		        			rot3Dpts[i] = quat.rotateVector(new float[3], 0, threeDpts[i], 0);
		        		}
		        		
		        		
		        		
		        		float[][] twoDpts = project3DTo2D(rot3Dpts, Z);
		        			
			        		// need to sort threeDpts into CCW here
		        			int[] sorted = sortPointsCCW(twoDpts, true);
			        		
			        		float[][] twoDtmp = new float[twoDpts.length][];
			        		float[][] threeDtmp = new float[threeDpts.length][];
		        			for(int j=0; j<sorted.length; j++) {
		        				twoDtmp[j] = twoDpts[sorted[j]];
		        				threeDtmp[j] = threeDpts[sorted[j]];
		        			}
		        			twoDpts = twoDtmp;
		        			threeDpts = threeDtmp;
		        			
		        		EarClippingTriangulator ect = new EarClippingTriangulator();        		
		        		int[] indices = ect.computeTrianglesReturnAsInts(float2DTo1DArray(twoDpts, 2));
		        		
		        		newTris = new float[indices.length][];
		        		for (int j=0; j<indices.length; j++) {
		        			newTris[j] = threeDpts[indices[j]];
		        		} 
			        	for (int j=0; j<newTris.length; j+=3) {
			        		float[][] currTri = new float[][]{newTris[j], newTris[j+1], newTris[j+2]};
			        		currTri = correctTriangleWindingOrder(currTri, planeNormal);
			        		newTris[j] = currTri[A];
			        		newTris[j+1] = currTri[B];
			        		newTris[j+2] = currTri[C];
			        	}
		        	}
	        } 

	        
	        if (newTris != null) {
		        	for (int j=0; j<newTris.length; j+=3) {
		        		Triangle newTri = new Triangle(new float[][]{newTris[j], newTris[j+1], newTris[j+2]});
		        		newTri.parentId = tri.getMeshId();
		        		retTris.add(newTri);
		        	}
		        	newTris = null;
	        }
	    }
		return retTris;
	}
	
	public static int[] sortPointsCCW(float[][] in, boolean useCentroid) {
		ArrayList<VertexAngle> list = new ArrayList<>();
		float[] centroid;
		if (useCentroid) {
			centroid = avgOf2DPolygon(in);
		} else {
			centroid = new float[]{0f, 0f, 0f};
		}
		// add the starting vertex
		for (int j=0; j<in.length; j++) {
			list.add(new VertexAngle(j, in[j], getAngle2DPoints(centroid, in[j])));		
		}
		
		Collections.sort(list, new VertexAngleComparator());
		int[] sorted = new int[in.length];
		int idx = 0;
		Iterator<VertexAngle> vaItr = list.iterator();
		while(vaItr.hasNext())  {
			sorted[idx++] = vaItr.next().index;
		}
		
		return sorted;
	}
	
	private static float getAngle2DPoints(float[] pSrc, float[] pTarget) {
	    float angle = (float) Math.toDegrees(Math.atan2(pTarget[Y] - pSrc[Y], pTarget[X] - pSrc[X]));

	    if(angle < 0){
	        angle += 360;
	    }

	    return angle;
	}
	
	public static float[][] correctTriangleWindingOrder(float[][] triangle, float[] normal) {
		float[] triNormal = new float[3];
		if (triPlaneNormalPoint(triangle, triNormal, new float[3])) {
			if (VectorUtil.dotVec3(normal, triNormal) < 0) {
				// need to reverse winding order
				float[] tmp = new float[]{triangle[A][X], triangle[A][Y], triangle[A][Z]};
				triangle[A] = triangle[C];
				triangle[C] = tmp;
			}			
		}
		return triangle;
	}
	
	private static float[][] subDivideTriangle(TriLineIntersect tli) {
		ArrayList<float[]> tris = new ArrayList<>();
		float[][] ret = null;
		
		tris = subdivideTriangleWithIntersect(tli.getTriangle().points, tli, tris);
		
		if (tris.size() > 0) {
			ret = new float[tris.size()][3];
			int i=0;
			for (float[] a : tris) {
				ret[i++] = a;
			}
		}
		return ret;
	}
	
	private static ArrayList<float[]> subdivideTriangleWithIntersect(float[][] tri, TriLineIntersect tli, ArrayList<float[]> tris) {
		
		if (isTriangleDegenerate(tri)) {			
			return tris;
		}
	
		float[][] subTris = splitTri(tri);

		float[][] tri1 = new float[][] {subTris[0], subTris[1], subTris[2]};
		float[][] tri2 = new float[][] {subTris[3], subTris[4], subTris[5]};
		
		TriLineIntersect[] inters = subdivideTriLineIntersect(tri1, tri2, tli);
		
		// if we have a null the triangle contains no corners or intersects
		if(inters[0] == null) {
			// TODO shouldn't this be tested against the polygon directly???
			if (isTriangleInsideCurve(tri1, tli.getOrderedIntersectingPoints())) {
				tris.add(tri1[0]);
				tris.add(tri1[1]);
				tris.add(tri1[2]);
			} else {
				// NOP
			}
		} else if (inters[0].hasCorner && inters[0].getNumCorners() == 1 && inters[0].getCorners().get(0).next == null && inters[0].getNumIntersectingLines() == 0){
			// the case of a triangle only having one corner in it
			float[][] sct = tessellateSingleCornerTriangle(inters[0]);
			if (sct != null) {
				for(int i=0; i<sct.length; i++) {
					tris.add(sct[i]);
				}
			}
		} else if (!inters[0].hasCorner && inters[0].getNumIntersectingLines() == 1) {
			// the case of a single instersecting line
			float[][] cut = tessellateCutTriangle(inters[0].getTriangle().points, new float[][]{inters[0].getIntersectingLines().get(0).startPt, inters[0].getIntersectingLines().get(0).endPt});
			if (cut != null) {
				for(int i=0; i<cut.length; i++) {
					tris.add(cut[i]);
				}
			}
        } else if (inters[0].getNumIntersectingLines() == 0 && inters[0].hasCorner && inters[0].getNumCorners() == 1 && inters[0].getCorners().get(0).next != null && OctTree.isCornerCurveConvex(inters[0])) {
        	// tessellate directly
        	float[][] curvePts = inters[0].getOrderedCorners();
        	float[][] interiorPts = OctTree.findPtsInteriorToConvexCurve(inters[0]);
        	ArrayList<float[]> points = new ArrayList<>();
        	for(int i=0; i<curvePts.length; i++) {
        		points.add(curvePts[i]);
        	}
        	if (interiorPts != null && interiorPts.length > 0) {
	        	for(int i=0; i<interiorPts.length; i++) {
	        		points.add(interiorPts[i]);
	        	}
        	}
        	
        	ArrayList<Triangle> retTris = OctTree.tessellateCoPlanarConvexPolygon(points);
        	
        	for(Triangle t : retTris) {
        		tris.add(t.points[A]);
        		tris.add(t.points[B]);
        		tris.add(t.points[C]);
        	}
		} else {
			// we need to keep subdividing
			subdivideTriangleWithIntersect(inters[0].getTriangle().points, inters[0], tris);
		}
			
					
		// if we have a null the triangle contains no corners or intersects
		if(inters[1] == null) {
			// TODO shouldn't this be tested against the polygon directly???
			if (isTriangleInsideCurve(tri2, tli.getOrderedIntersectingPoints())) {
				tris.add(tri2[0]);
				tris.add(tri2[1]);
				tris.add(tri2[2]);
			} else {
				// NOP
			}
		} else if (inters[1].hasCorner && inters[1].getNumCorners() == 1 && inters[1].getCorners().get(0).next == null && inters[1].getNumIntersectingLines() == 0){
			// the case of a triangle only having one corner in it
			float[][] sct = tessellateSingleCornerTriangle(inters[1]);
			if (sct != null) {
				for(int i=0; i<sct.length; i++) {
					tris.add(sct[i]);
				}
			}
		} else if (!inters[1].hasCorner && inters[1].getNumIntersectingLines() == 1) {
			// the case of a single instersecting line
			float[][] cut = tessellateCutTriangle(inters[1].getTriangle().points, new float[][]{inters[1].getIntersectingLines().get(0).startPt, inters[1].getIntersectingLines().get(0).endPt});
			if (cut != null) {
				for(int i=0; i<cut.length; i++) {
					tris.add(cut[i]);
				}
			}
//        } else if (OctTree.isCurveConvex(inters[1])) {
        } else if (inters[1].getNumIntersectingLines() == 0 && inters[1].hasCorner && inters[1].getNumCorners() == 1 && inters[1].getCorners().get(0).next != null && OctTree.isCornerCurveConvex(inters[1])) {
        	// tessellate directly
//        	float[][] curvePts = inters[1].getOrderedIntersectingPoints();
        	float[][] curvePts = inters[1].getOrderedCorners();
        	float[][] interiorPts = OctTree.findPtsInteriorToConvexCurve(inters[1]);
        	ArrayList<float[]> points = new ArrayList<>();
        	for(int i=0; i<curvePts.length; i++) {
        		points.add(curvePts[i]);
        	}
        	if (interiorPts != null && interiorPts.length > 0) {
	        	for(int i=0; i<interiorPts.length; i++) {
	        		points.add(interiorPts[i]);
	        	}
        	}
        	
        	ArrayList<Triangle> retTris = OctTree.tessellateCoPlanarConvexPolygon(points);
        	
        	for(Triangle t : retTris) {
        		tris.add(t.points[A]);
        		tris.add(t.points[B]);
        		tris.add(t.points[C]);
        	}
        	
		} else {
			// we need to keep subdividing
			subdivideTriangleWithIntersect(inters[1].getTriangle().points, inters[1], tris);
		}
		return tris;
	}
	
	private static TriLineIntersect[] subdivideTriLineIntersect(float[][] tri1, float[][] tri2, TriLineIntersect tli) {
		TriLineIntersect[] triLines = new TriLineIntersect[2];
		TriLineIntersect tli1 = new TriLineIntersect(new Triangle(tri1));
		TriLineIntersect tli2 = new TriLineIntersect(new Triangle(tri2));
		float[] ac = new float[3];
		float[] ab = new float[3];
		float[] ap = new float[3];
		boolean breakLink = false;
		
		for (Corner c : tli.getCorners()) {
		// keep it simple let's find the polygon corners in the first triangle 
			Corner currentC = null;
			while (c != null) {
				if (VectorUtil.isInTriangleVec3(tri1[0], tri1[1], tri1[2], c.location, ac, ab, ap)) {
					Corner newCorner = new Corner();
					newCorner.location = c.location;
					boolean addCorner = false;
					if (c.prev == null && c.end && c.endIntersects.size() > 0) { // this is the first corner in the parent triangle in this sequence
							float[][] newPoints = OctTree.intersectSegmentTriangleLoose(c.endIntersects.get(0), c.location, tri1[0], tri1[1], tri1[2]);
							if (newPoints != null) {
								// find the correct intersection point
								for (int i=0; i<newPoints.length; i++) {
									// if the line corner is in the triangle, add it and continue
									if (VectorUtil.distSquareVec3(c.endIntersects.get(0), newPoints[i]) <=  VectorUtil.distSquareVec3(c.endIntersects.get(0), c.location)  + FloatUtil.EPSILON &&
											VectorUtil.dotVec3(OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], newPoints[i], c.location)), OctTree.normalizeVec3(c.endDirection)) >= -FloatUtil.EPSILON /*MINI_EPSILON*/) {
										newCorner.endIntersects.add(newPoints[i]);
										newCorner.end = true;
										newCorner.endDirection = c.endDirection;
										addCorner = true;
										break;
									}
								}
							}
							// now we need to intersect the other triangle to see if this end segment crosses the other triangle
							float[][] nextPoints = OctTree.intersectSegmentTriangleLoose(c.location, c.endIntersects.get(0), tri2[0], tri2[1], tri2[2]);
							// find the correct intersection point
							if (nextPoints != null && nextPoints.length == 2) {
								if (pointInsideOrEdgeTriangle (tri2, nextPoints[0]) && pointInsideOrEdgeTriangle (tri2, nextPoints[1]) &&
										VectorUtil.dotVec3(OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], nextPoints[1], c.location)), OctTree.normalizeVec3(c.endDirection)) >= -FloatUtil.EPSILON /*MINI_EPSILON*/) {
									IntersectingLine newLine = new IntersectingLine();
									newLine.startPt = nextPoints[1]; // need to reverse the order of the points here to preserve correct winding order
									newLine.endPt = nextPoints[0];
									tli2.addIntersectingLine(newLine);
								}
							}

					} else if (c.prev != null && c.endIntersects.size() == 0) {
						// check to see if the previous corner is in the other triangle
						if (VectorUtil.isInTriangleVec3(tri2[0], tri2[1], tri2[2], c.prev.location, ac, ab, ap)) {
							float[][] newPoints = OctTree.intersectSegmentTriangleLoose(c.prev.location, c.location, tri1[0], tri1[1], tri1[2]);
							if (newPoints != null) {
							// find the correct intersection point
								for (int i=0; i<newPoints.length; i++) {
									// if the line intersect is in the triangle, add it and continue
									if (VectorUtil.distSquareVec3(c.location, newPoints[i]) <  VectorUtil.distSquareVec3(c.prev.location, c.location)  &&
											VectorUtil.dotVec3(OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], newPoints[i], c.prev.location)), OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], c.location, c.prev.location))) >= -FloatUtil.EPSILON /*MINI_EPSILON*/) {
										newCorner.endIntersects.add(newPoints[i]);
										newCorner.end = true;
										newCorner.endDirection = VectorUtil.subVec3(new float[3], c.prev.location, c.location);
										addCorner = true;
										breakLink = true;
										break;
									}
								}
							}
						}				
					} 
					
					if (c.next == null && c.start && c.startIntersects.size() > 0) { // this is the last corner in the parent triangle in this sequence
						float[][] newPoints = OctTree.intersectSegmentTriangleLoose(c.location, c.startIntersects.get(0), tri1[0], tri1[1], tri1[2]);
							if (newPoints != null) {
							// find the correct intersection point
							for (int i=0; i<newPoints.length; i++) {
								if (VectorUtil.distSquareVec3(c.location, newPoints[i]) <=  VectorUtil.distSquareVec3(c.location, c.startIntersects.get(0))  + FloatUtil.EPSILON &&
										VectorUtil.dotVec3(OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], newPoints[i], c.location)), OctTree.normalizeVec3(c.startDirection)) >= -FloatUtil.EPSILON/*MINI_EPSILON*/) {
									newCorner.startIntersects.add(newPoints[i]);
									newCorner.start = true;
									newCorner.startDirection = c.startDirection;
									addCorner = true;
									break;
								}
							}
						}
						// now we need to intersect the other triangle to see if this start segment crosses the other triangle
						float[][] nextPoints = OctTree.intersectSegmentTriangleLoose(c.location, c.startIntersects.get(0), tri2[0], tri2[1], tri2[2]);
						// find the correct intersection point
						if (nextPoints != null && nextPoints.length == 2) {
							if (pointInsideOrEdgeTriangle (tri2, nextPoints[0]) && pointInsideOrEdgeTriangle (tri2, nextPoints[1]) &&
									VectorUtil.dotVec3(OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], nextPoints[1], c.location)), OctTree.normalizeVec3(c.startDirection)) >= -FloatUtil.EPSILON /*MINI_EPSILON*/) {
								IntersectingLine newLine = new IntersectingLine();
								newLine.startPt = nextPoints[0];
								newLine.endPt = nextPoints[1];
								tli2.addIntersectingLine(newLine);
							}
						}
						
					} else if (c.next != null && c.startIntersects.size() == 0) {
						// check to see if the next corner is in the other triangle
						if (VectorUtil.isInTriangleVec3(tri2[0], tri2[1], tri2[2], c.next.location, ac, ab, ap)) {
							float[][] newPoints = OctTree.intersectSegmentTriangleLoose(c.location, c.next.location, tri1[0], tri1[1], tri1[2]);
							if (newPoints != null) {
								// find the correct intersection point
								for (int i=0; i<newPoints.length; i++) {
									// if the line intersect is in the triangle, add it and continue
									if (VectorUtil.distSquareVec3(c.location, newPoints[i]) <  VectorUtil.distSquareVec3(c.next.location, c.location) &&
											VectorUtil.dotVec3(OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], newPoints[i], c.location)), OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], c.next.location, c.location))) >= -FloatUtil.EPSILON /*MINI_EPSILON*/) {
										newCorner.startIntersects.add(newPoints[i]);
										newCorner.start = true;
										newCorner.startDirection = VectorUtil.subVec3(new float[3], c.next.location, c.location);
										addCorner = true;
										break;
									}
								}
							}
						}
						// this is an interior triangle add it
						addCorner = true;
					}
					
					if (addCorner) {
						if (currentC == null) {	// just starting out, add corner to new sequence
							currentC = newCorner;
						} else if (currentC != null && breakLink) {	// need a new sequence of corners
							tli1.addCorner(currentC);
							currentC = newCorner;
						} else {
							Corner tmp = currentC.getEnd();
							// add this corner to the current sequence
							tmp.next = newCorner;
							// need to check if the prev corner was in a different triangle,
							// if so we don't want to link back to it
							if (c.prev != null && VectorUtil.isInTriangleVec3(tri1[0], tri1[1], tri1[2], c.prev.location, ac, ab, ap)) {
								newCorner.prev = tmp;
							}
						}
						breakLink = false;
					}													
				}
				c = c.next;
			}
			if (currentC != null) {	// we have at least one valid Corner
				tli1.addCorner(currentC);
				tli1.hasCorner = true;
			}
		}
			
		for (Corner c : tli.getCorners()) { // reset to the beginning of the corners
		// keep it simple let's find the second triangles corners
			breakLink = false;
			Corner currentC = null;
			while (c != null) {
				if (VectorUtil.isInTriangleVec3(tri2[0], tri2[1], tri2[2], c.location, ac, ab, ap)) {
					Corner newCorner = new Corner();
					newCorner.location = c.location;
					boolean addCorner = false;
					if (c.prev == null && c.end && c.endIntersects.size() > 0) { // this is the first corner in the parent triangle in this sequence
							float[][] newPoints = OctTree.intersectSegmentTriangleLoose(c.endIntersects.get(0), c.location, tri2[0], tri2[1], tri2[2]);
							if (newPoints != null) {
								// find the correct intersection point
								for (int i=0; i<newPoints.length; i++) {
									// if the line corner is in the triangle, add it and continue
									if (VectorUtil.distSquareVec3(c.endIntersects.get(0), newPoints[i]) <=  VectorUtil.distSquareVec3(c.endIntersects.get(0), c.location)  + FloatUtil.EPSILON &&
											VectorUtil.dotVec3(OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], newPoints[i], c.location)), OctTree.normalizeVec3(c.endDirection)) >= -FloatUtil.EPSILON /*MINI_EPSILON*/) {
										newCorner.endIntersects.add(newPoints[i]);
										newCorner.end = true;
										newCorner.endDirection = c.endDirection;
										addCorner = true;
										break;
									}
								}
							}
							// now we need to intersect the other triangle to see if this end segment crosses the other triangle
							float[][] nextPoints = OctTree.intersectSegmentTriangleLoose(c.location, c.endIntersects.get(0), tri1[0], tri1[1], tri1[2]);
							// find the correct intersection point
							if (nextPoints != null && nextPoints.length == 2) {
								if (pointInsideOrEdgeTriangle (tri2, nextPoints[0]) && pointInsideOrEdgeTriangle (tri1, nextPoints[1]) &&
										VectorUtil.dotVec3(OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], nextPoints[1], c.location)), OctTree.normalizeVec3(c.endDirection)) >= -FloatUtil.EPSILON /*MINI_EPSILON*/) {
									IntersectingLine newLine = new IntersectingLine();
									newLine.startPt = nextPoints[1]; // need to reverse the order of the points here to preserve correct winding order
									newLine.endPt = nextPoints[0];
									tli1.addIntersectingLine(newLine);
								}
							}
					} else if (c.prev != null && c.endIntersects.size() == 0) {
						// check to see if the previous corner is in the other triangle
						if (VectorUtil.isInTriangleVec3(tri1[0], tri1[1], tri1[2], c.prev.location, ac, ab, ap)) {
							float[][] newPoints = OctTree.intersectSegmentTriangleLoose(c.prev.location, c.location, tri2[0], tri2[1], tri2[2]);
							if (newPoints != null) {
								// find the correct intersection point
								for (int i=0; i<newPoints.length; i++) {
									// if the line intersect is in the triangle, add it and continue
									if (VectorUtil.distSquareVec3(c.location, newPoints[i]) <  VectorUtil.distSquareVec3(c.prev.location, c.location)  &&
											VectorUtil.dotVec3(OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], newPoints[i], c.prev.location)), OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], c.location, c.prev.location))) >= -FloatUtil.EPSILON /*MINI_EPSILON*/) {
										newCorner.endIntersects.add(newPoints[i]);
										newCorner.end = true;
										newCorner.endDirection = VectorUtil.subVec3(new float[3], c.prev.location, c.location);
										addCorner = true;
										breakLink = true;
										break;
									}
								}
							}
						}				
					} 
					
					if (c.next == null && c.start && c.startIntersects.size() > 0) { // this is the last corner in the parent triangle in this sequence
						float[][] newPoints = OctTree.intersectSegmentTriangleLoose(c.location, c.startIntersects.get(0), tri2[0], tri2[1], tri2[2]);
						if (newPoints != null) {
							// find the correct intersection point
							for (int i=0; i<newPoints.length; i++) {
								if (VectorUtil.distSquareVec3(c.location, newPoints[i]) <=  VectorUtil.distSquareVec3(c.location, c.startIntersects.get(0))  + FloatUtil.EPSILON  &&
										VectorUtil.dotVec3(OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], newPoints[i], c.location)), OctTree.normalizeVec3(c.startDirection)) >= -FloatUtil.EPSILON) {
									newCorner.startIntersects.add(newPoints[i]);
									newCorner.start = true;
									newCorner.startDirection = c.startDirection;
									addCorner = true;
									break;
								}
							}
						}
						// now we need to intersect the other triangle to see if this start segment crosses the other triangle
						float[][] nextPoints = OctTree.intersectSegmentTriangleLoose(c.location, c.startIntersects.get(0), tri1[0], tri1[1], tri1[2]);
						// find the correct intersection point
						if (nextPoints != null && nextPoints.length == 2) {
							if (pointInsideOrEdgeTriangle (tri2, nextPoints[0]) && pointInsideOrEdgeTriangle (tri1, nextPoints[1]) &&
									VectorUtil.dotVec3(OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], nextPoints[1], c.location)), OctTree.normalizeVec3(c.startDirection)) >= -FloatUtil.EPSILON /*MINI_EPSILON*/) {
								IntersectingLine newLine = new IntersectingLine();
								newLine.startPt = nextPoints[0];
								newLine.endPt = nextPoints[1];
								tli1.addIntersectingLine(newLine);
							}
						}
						
					} else if (c.next != null && c.startIntersects.size() == 0) {
						// check to see if the next corner is in the other triangle
						if (VectorUtil.isInTriangleVec3(tri1[0], tri1[1], tri1[2], c.next.location, ac, ab, ap)) {
							float[][] newPoints = OctTree.intersectSegmentTriangleLoose(c.location, c.next.location, tri2[0], tri2[1], tri2[2]);
							if (newPoints != null) {
								// find the correct intersection point
								for (int i=0; i<newPoints.length; i++) {
									// if the line intersect is in the triangle, add it and continue
									if (VectorUtil.distSquareVec3(c.location, newPoints[i]) <  VectorUtil.distSquareVec3(c.next.location, c.location) &&
											VectorUtil.dotVec3(
													OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], newPoints[i], c.location)), 
													OctTree.normalizeVec3(VectorUtil.subVec3(new float[3], c.next.location, c.location))) >= -FloatUtil.EPSILON) {
										newCorner.startIntersects.add(newPoints[i]);
										newCorner.start = true;
										newCorner.startDirection = VectorUtil.subVec3(new float[3], c.next.location, c.location);
										addCorner = true;
										break;
									}
								}
							}
						}					
						// this is an interior triangle add it
						addCorner = true;
					}
					
					if (addCorner) {
						if (currentC == null) {
							currentC = newCorner;
						} else if (currentC != null && breakLink) {	// need a new sequence of corners
							tli2.addCorner(currentC); // add the current sequence
							currentC = newCorner;
							breakLink = false; 
						} else {
							Corner tmp = currentC.getEnd();
							// add this corner to the current sequence
							tmp.next = newCorner;
							// need to check if the prev corner was in a different triangle,
							// if so we don't want to link back to it
							if (c.prev != null && VectorUtil.isInTriangleVec3(tri2[0], tri2[1], tri2[2], c.prev.location, ac, ab, ap)) {
								newCorner.prev = tmp;
							}
						}
						breakLink = false;
					}
													
				}
				c = c.next;
			}
			if (currentC != null) {	// we have at least one valid Corner
				tli2.addCorner(currentC);
				tli2.hasCorner = true;
			}
		}
					
		// next we need to sort and partition the line intersects between the two triangles
		for (IntersectingLine il : tli.getIntersectingLines()) {
			// intersect with first triangle
			// make sure the start point is in the plane of the triangle
			float[] newStart = new float[3];
			Triangle t1 = new Triangle(tri1);
			linePlaneIntersectTri(newStart, new float[] {0f, 0f, 0f}, il.startPt, t1);
			// make sure the end point is in the plane of the triangle
			float[] newEnd = new float[3];
			linePlaneIntersectTri(newEnd, new float[] {0f, 0f, 0f}, il.endPt, t1);
			float[][] newPoints = OctTree.intersectSegmentTriangleLoose(newStart, newEnd, tri1[0], tri1[1], tri1[2]);
			if (newPoints != null && newPoints.length == 2) {
				IntersectingLine newLine = new IntersectingLine();
				newLine.startPt = newPoints[0];
				newLine.endPt = newPoints[1];
				tli1.addIntersectingLine(newLine);
			}
			
			// intersect with the second triangle
			// make sure the start point is in the plane of the triangle
			float[] newStart2 = new float[3];
			Triangle t2 = new Triangle(tri2);
			linePlaneIntersectTri(newStart2, new float[] {0f, 0f, 0f}, il.startPt, t2);
			// make sure the end point is in the plane of the triangle
			float[] newEnd2 = new float[3];
			linePlaneIntersectTri(newEnd2, new float[] {0f, 0f, 0f}, il.endPt, t2);
			float[][] newPoints2 = OctTree.intersectSegmentTriangleLoose(newStart2, newEnd2, tri2[0], tri2[1], tri2[2]);
			if (newPoints2 != null && newPoints2.length == 2) {
				IntersectingLine newLine2 = new IntersectingLine();
				newLine2.startPt = newPoints2[0];
				newLine2.endPt = newPoints2[1];
				tli2.addIntersectingLine(newLine2);
			}
		}
		
		if (tli1.hasCorner || tli1.getNumIntersectingLines() > 0) {
			triLines[0] = tli1;
		}
		if (tli2.hasCorner || tli2.getNumIntersectingLines() > 0) {
			triLines[1] = tli2;
		}
		
		return triLines;
	}

	private void looseGetAllTriangles(ONode n, HashMap<Integer, Triangle> list) {
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						looseGetAllTriangles(n.child[k][j][i], list);
					}
				}
			}
		}

		Triangle o = n.object;
		while (o != null) {
			list.put(o.id, o);
			o = o.next;
		}
	}
	
	private static ArrayList<float[]> subdivideTriWithCorners(float[][] tri, float[][] lines, float[][] normals, float[][] normPts, ArrayList<float[]> tris) {	
		float[][] subTris = splitTri(tri);
		
		// test each returned tri to see if a) they are at the threshold and b) if they are still contained inside the line
		float[][] tri1 = new float[][] {subTris[0], subTris[1], subTris[2]};
		float[][] tri2 = new float[][] {subTris[3], subTris[4], subTris[5]};
		boolean above1 = triangleAboveThreshold(tri1, TESSELLATION_LIMIT);
		if (triangleContainedByConvexCurve(tri1, normals, normPts) && above1) {
			tris.add(tri1[0]); tris.add(tri1[1]); tris.add(tri1[2]);
		} else if (above1) {
			subdivideTriSection(tri1, lines, normals, normPts, tris);
		}
		
		boolean above2 = triangleAboveThreshold(tri2, TESSELLATION_LIMIT);
		if (triangleContainedByConvexCurve(tri2, normals, normPts) && above2) {
			tris.add(tri2[0]); tris.add(tri2[1]); tris.add(tri2[2]);
		} else if (above2) {
			subdivideTriSection(tri2, lines, normals, normPts, tris);
		}
		
		return tris;
	}
	

	// This method subdivides a triangle to its closest tessellation limited by TESSELLATION_LIMIT
	private static ArrayList<float[]> subdivideTriSection(float[][] tri, float[][] lines, float[][] normals, float[][] normPts, ArrayList<float[]> tris) {	
		float[][] subTris = splitTri(tri);
		
		// test each returned tri to see if a) they are at the threshold and b) if they are still contained inside the line
		float[][] tri1 = new float[][] {subTris[0], subTris[1], subTris[2]};
		float[][] tri2 = new float[][] {subTris[3], subTris[4], subTris[5]};
		boolean above1 = triangleAboveThreshold(tri1, TESSELLATION_LIMIT);
		if (triangleContainedByConvexCurve(tri1, normals, normPts) && above1) {
			tris.add(tri1[0]); tris.add(tri1[1]); tris.add(tri1[2]);
		} else if (above1) {
			subdivideTriSection(tri1, lines, normals, normPts, tris);
		}
		
		boolean above2 = triangleAboveThreshold(tri2, TESSELLATION_LIMIT);
		if (triangleContainedByConvexCurve(tri2, normals, normPts) && above2) {
			tris.add(tri2[0]); tris.add(tri2[1]); tris.add(tri2[2]);
		} else if (above2) {
			subdivideTriSection(tri2, lines, normals, normPts, tris);
		}
		
		return tris;
	}
	
	// This method subdivides a triangle to its smallest limit
	private static ArrayList<float[]> subdivideTri(float[][] tri, ArrayList<float[]> tris) {	
		float[][] subTris = splitTri(tri);
		
		// test each returned tri to see if they are below the threshold 
		float[][] tri1 = new float[][] {subTris[0], subTris[1], subTris[2]};
		float[][] tri2 = new float[][] {subTris[3], subTris[4], subTris[5]};
		boolean above1 = triangleAboveThreshold(tri1, TESSELLATION_LIMIT);
		if (!above1) {
			tris.add(tri1[0]); tris.add(tri1[1]); tris.add(tri1[2]);
		} else {
			subdivideTri(tri1, tris);
		}
		
		boolean above2 = triangleAboveThreshold(tri2, TESSELLATION_LIMIT);
		if (!above2) {
			tris.add(tri2[0]); tris.add(tri2[1]); tris.add(tri2[2]);
		} else {
			subdivideTri(tri2, tris);
		}
		
		return tris;
	}
	
	/**
	 * Enum to define polygon types
	 */
	public enum PolyType {CONVEX, COMPLEX, INVALID};
	/* Ported from:
	 * https://www.opengl.org/discussion_boards/showthread.php/165971-algorithm-to-detect-the-type-of-a-polygon
	 */
	/****************************************************************************************************/
	 
	/**
	 * Method to determine if a polygon or curve is convex or concave(complex?)
	 *
	 * @param p ordered polygon vertices
	 * @param numVertices if start point is repeated at end this value should 
	 * 	be num of vertices - 1
	 * @return PolygonType
	 */
	public static PolyType detectPolygonType(float[][] p, int numVertices) {
		float[] v1 = new float[2];
		float[] v2 = new float[2];
		double detValue = 0.0;
		double curDetValue = 0.0;
		int i = 0;

		if (numVertices < 3) {
			return (PolyType.INVALID);
		}

		v1 = calcVector(p[0], p[numVertices - 1]);
		v2 = calcVector(p[1], p[0]);
		detValue = calcDeterminant(v1, v2);

		for (i = 1; i < numVertices - 1; i++) {
			v1 = v2;
			v2 = calcVector(p[i + 1], p[i]);
			curDetValue = calcDeterminant(v1, v2);

			if ((curDetValue * detValue) < 0.0) {
				return (PolyType.COMPLEX);
			}

		}

		v1 = v2;
		v2 = calcVector(p[0], p[numVertices - 1]);
		curDetValue = calcDeterminant(v1, v2);

		if ((curDetValue * detValue) < 0.0) {
			return (PolyType.COMPLEX);
		} else {
			return (PolyType.CONVEX);
		}

	}	 
	 
	private static double calcDeterminant(float[] p1, float[] p2) {
		return (p1[X] * p2[Y] - p1[Y] * p2[X]);
	}

	private static float[] calcVector(float[] p0, float[] p1) {
		float[] p = new float[2];
		p[X] = p0[X] - p1[X];
		p[Y] = p0[Y] - p1[Y];
		return (p);
	}

	/****************************************************************************************************/

	
	// assume parallel normal arrays
	private static boolean triangleContainedByConvexCurve(float[][] tri, float[][] normals, float[][] normPts) {
		boolean contained = true;
		// check each triangle vertex against each segment of the curve (point and normal vector)
		// if any vertex is outside any segment fail immediately
		for (int i=0; i<normals.length; i++) {
			float[] r1 = VectorUtil.subVec3(new float[3], tri[0], normPts[i]);
			if (VectorUtil.dotVec3(OctTree.normalizeVec3(r1), OctTree.normalizeVec3(normals[i])) < 0f) {
				return false;
			}
			float[] r2 = VectorUtil.subVec3(new float[3], tri[1], normPts[i]);
			if (VectorUtil.dotVec3(OctTree.normalizeVec3(r2), OctTree.normalizeVec3(normals[i])) < 0f) {
				return false;
			}
			float[] r3 = VectorUtil.subVec3(new float[3], tri[2], normPts[i]);
			if (VectorUtil.dotVec3(OctTree.normalizeVec3(r3), OctTree.normalizeVec3(normals[i])) < 0f) {
				return false;
			}
		}    		
		return contained;
	}
	
	// assume parallel normal arrays
	private static boolean pointContainedByConvexCurve(float[] point, float[][] normals, float[][] normPts) {
		boolean contained = true;
		// check the point against each segment of the curve (point and normal vector)
		for (int i=0; i<normals.length; i++) {
			float[] r1 = VectorUtil.subVec3(new float[3], point, normPts[i]);
			if (VectorUtil.dotVec3(OctTree.normalizeVec3(r1), OctTree.normalizeVec3(normals[i])) < 0f) {
				return false;
			}
		}    		
		return contained;
	}
	
	private static boolean isCornerCurveConvex(TriLineIntersect tli) {
		float[][] points = tli.getOrderedCorners();
		if (points.length < 3) {
			return false;
		}
		float[] norm = new float[3];
		// get the triangle normal
		OctTree.planeFrom3Points(norm, new float[3], tli.getTriangle().points[A], tli.getTriangle().points[B], tli.getTriangle().points[C]);
		// determine which axis to drop
		int dropAxis = OctTree.mostOrthoganalAxisToPlane(norm);
		// project the points to 2D
		float[][] pts2D = OctTree.project3DTo2D(points, dropAxis);
		// determine convexity
		if (OctTree.detectPolygonType(pts2D, pts2D.length) == PolyType.CONVEX) {
			return true;
		}
				
		return false;
	}
	
	private static boolean isCurveConvex(TriLineIntersect tli) {
		float[][] points = tli.getOrderedIntersectingPoints();
		if (points.length < 3) {
			return false;
		}
		float[] norm = new float[3];
		// get the triangle normal
		OctTree.planeFrom3Points(norm, new float[3], tli.getTriangle().points[A], tli.getTriangle().points[B], tli.getTriangle().points[C]);
		// determine which axis to drop
		int dropAxis = OctTree.mostOrthoganalAxisToPlane(norm);
		// project the points to 2D
		float[][] pts2D = OctTree.project3DTo2D(points, dropAxis);
		// determine convexity
		if (OctTree.detectPolygonType(pts2D, pts2D.length) == PolyType.CONVEX) {
			return true;
		}
				
		return false;
	}
	
	private static boolean isCurveConvex(float[][] normals, float[] planeNorm) {
		// assuming CCW winding order
		// pts must have at least 2 normals
		// atan2((Vb x Va) . Vn, Va . Vb)
		for (int i=1; i<normals.length; i++) {
			float ang = FloatUtil.atan2(VectorUtil.dotVec3((VectorUtil.crossVec3(new float[3], normals[i], normals[i-1])), planeNorm), VectorUtil.dotVec3(normals[i-1], normals[i]));
    		if (ang > 0f){
    			return false;        			
    		}
		}   		
		return true;
	}
	
	private static boolean triangleAboveThreshold(float[][] tri, float threshold) {
		float[] len = new float[]{0f};
		longestSideOfTriangle(tri, len);
		
		if (len[0] > threshold) {
			return true;
		} else {
			return false;
		}
	}
	
	private static float[][] splitTri(float[][] tri) {
		float[][] retTris = new float[6][3];
		// find the longest side of the triangle
		int longSide = longestSideOfTriangle(tri, new float[1]);
		
		if (longSide == 0) {
			float[] mid = VectorUtil.midVec3(new float[3], tri[0], tri[1]);
			// first triangle
			retTris[0] = tri[0];
			retTris[1] = mid;
			retTris[2] = tri[2];
			// second triangle
			retTris[3] = mid;
			retTris[4] = tri[1];
			retTris[5] = tri[2];
		} else if (longSide == 1) {
			float[] mid = VectorUtil.midVec3(new float[3], tri[1], tri[2]);
			// first triangle
			retTris[0] = tri[0];
			retTris[1] = tri[1];
			retTris[2] = mid;
			// second triangle
			retTris[3] = tri[0];
			retTris[4] = mid;
			retTris[5] = tri[2];   			
		}else if (longSide == 2) {
			float[] mid = VectorUtil.midVec3(new float[3], tri[2], tri[0]);
			// first triangle
			retTris[0] = tri[0];
			retTris[1] = tri[1];
			retTris[2] = mid;
			// second triangle
			retTris[3] = tri[1];
			retTris[4] = tri[2];
			retTris[5] = mid; 
		}
		
		return retTris;
	}
	
	private static float[][] tessellateCutTriangle (float[][] tri, float[][] line) {
		ArrayList<float[]> list = new ArrayList<>();
		// determine how many triangle vertices are inside the "cutting" line
		float[] norm = VectorUtil.normalizeVec3(VectorUtil.crossVec3(new float[3], VectorUtil.normalizeVec3(new float[3], line[0]), VectorUtil.normalizeVec3(new float[3], line[1])));
		float[] midPoint = VectorUtil.midVec3(new float[3], line[0], line[1]);
		boolean[] indices = new boolean[]{false, false, false};
		int cnt = 0;
		for (int i=0; i<tri.length; i++) {
			float[] dir = VectorUtil.subVec3(new float[3], tri[i], midPoint);
			if (VectorUtil.dotVec3(VectorUtil.normalizeVec3(dir), norm) >= 0f) {
				indices[i] = true;
				cnt++;
			}
		}
		
		// the triangle is outside the cutting line...BAD things have happened
		if (cnt == 0) {
			log.aprintln("Trying to cut triangle that is outside the cutting line!");
		}
		// simple case, create a new smaller triangle
		else if (cnt == 1) {
			for (int i=0; i<indices.length; i++) {
				if (indices[i]) {
					list.add(line[0]);
					list.add(line[1]);
					list.add(tri[i]);
				}
			}
		}
		// the more complex case, two vertices are inside the cutting line
		else if (cnt == 2) {
			if (indices[0] && indices[1]) {
				float[] mid = VectorUtil.midVec3(new float[3], tri[0], tri[1]);
				// create first triangle
				list.add(line[1]);
				list.add(tri[0]);
				list.add(mid);
				// create second triangle
				list.add(line[1]);
				list.add(mid);
				list.add(line[0]);
				// create third triangle
				list.add(line[0]);
				list.add(mid);
				list.add(tri[1]);
			} else if (indices[1] && indices[2]) {
				float[] mid = VectorUtil.midVec3(new float[3], tri[1], tri[2]);
				// create first triangle
				list.add(line[1]);
				list.add(tri[1]);
				list.add(mid);
				// create second triangle
				list.add(line[1]);
				list.add(mid);
				list.add(line[0]);
				// create third triangle
				list.add(line[0]);
				list.add(mid);
				list.add(tri[2]);
			} else if (indices[2] && indices[0]) {
				float[] mid = VectorUtil.midVec3(new float[3], tri[2], tri[0]);
				// create first triangle
				list.add(line[1]);
				list.add(tri[2]);
				list.add(mid);
				// create second triangle
				list.add(line[1]);
				list.add(mid);
				list.add(line[0]);
				// create third triangle
				list.add(line[0]);
				list.add(mid);
				list.add(tri[0]);
			}
		} else if (cnt == 3) {
			// triangle is entirely inside the cutting line, just return the triangle
			list.add(tri[0]);
			list.add(tri[1]);
			list.add(tri[2]);
		}
		
		
		float[][] ret = new float[list.size()][3];
		int i = 0;
		for (float[] f : list) {
			ret[i++] = f;
		}
		
		return ret;
	}
	
	private static ArrayList<Triangle> tessellateCutTriangleToTriangles (Triangle tri, float[][] line) {
		ArrayList<Triangle> list = new ArrayList<>();
		// determine how many triangle vertices are inside the "cutting" line
		float[] norm = VectorUtil.crossVec3(new float[3], line[0], line[1]);
		boolean[] indices = new boolean[]{false, false, false};
		int cnt = 0;
		for (int i=0; i<tri.points.length; i++) {
			float[] dir = VectorUtil.subVec3(new float[3], tri.points[i], line[0]);
			if (VectorUtil.dotVec3(OctTree.normalizeVec3(dir), OctTree.normalizeVec3(norm)) > 0f) {
				indices[i] = true;
				cnt++;
			}
		}
		
		// the triangle is outside the cutting line...BAD things have happened
		if (cnt == 0) {
			log.aprintln("Trying to cut triangle that is outside the cutting line!");
		}
		// simple case, create a new smaller triangle
		else if (cnt == 1) {
			for (int i=0; i<indices.length; i++) {
				if (indices[i]) {
					list.add(new Triangle(new float[][]{line[0], line[1], tri.points[i]}));
				}
			}
		}
		// the more complex case, two vertices are inside the cutting line
		else if (cnt == 2) {
			if (indices[0] && indices[1]) {
				float[] mid = VectorUtil.midVec3(new float[3], tri.points[0], tri.points[1]);
				// create first triangle
				list.add(new Triangle(new float[][]{line[1], tri.points[0], mid}));
				// create second triangle
				list.add(new Triangle(new float[][]{line[1], mid, line[0]}));
				// create third triangle
				list.add(new Triangle(new float[][]{line[0], mid, tri.points[1]}));
			} else if(indices[1] && indices[2]) {
				float[] mid = VectorUtil.midVec3(new float[3], tri.points[1], tri.points[2]);
				// create first triangle
				list.add(new Triangle(new float[][]{line[1], tri.points[1], mid}));
				// create second triangle
				list.add(new Triangle(new float[][]{line[1], mid, line[0]}));
				// create third triangle
				list.add(new Triangle(new float[][]{line[0], mid, tri.points[2]}));
			}
			 else if(indices[2] && indices[0]) {
 				float[] mid = VectorUtil.midVec3(new float[3], tri.points[2], tri.points[0]);
 				// create first triangle
				list.add(new Triangle(new float[][]{line[1], tri.points[2], mid}));
 				// create second triangle
				list.add(new Triangle(new float[][]{line[1], mid, line[0]}));
 				// create third triangle
				list.add(new Triangle(new float[][]{line[0], mid, tri.points[0]}));
 			} else if(cnt == 3) {
 				// triangle is entirely inside the cutting line, just return the triangle
 				list.add(tri);
 			}
		}
		
		for (Triangle t : list) {
			t.id = tri.getMeshId();
		}
				
		return list;
	}
	

	private static float[][] tessellateSingleCornerTriangle(TriLineIntersect tli) {
		float[][] ret = null;
		if (tli.getNumCorners() != 1) {
			log.aprintln("Error trying to tessellate a single corner triangle that has no corners.");
			return ret;
		}
		Corner c = tli.getCorners().get(0);
		if (c == null || c.endIntersects == null || c.location == null || c.startIntersects == null || c.endIntersects.size() < 1 || c.startIntersects.size() < 1) {
			return ret;
		}
		float[][] lines = new float[][]{c.endIntersects.get(0), c.location, c.startIntersects.get(0)};
		float[][] norms = new float[][]{VectorUtil.crossVec3(new float[3], lines[0], lines[1]), VectorUtil.crossVec3(new float[3], lines[1], lines[2])};
		float[][] vertices = tli.getTriangle().points;
		boolean[] in = new boolean[]{false, false, false}; 
		boolean inside = OctTree.pointLeftOfCcwLine(c.startIntersects.get(0), c.endIntersects.get(0), c.location);
		
		int cnt = 0;
		// case for an angle < 90 degrees
		if (inside) {
			for (int i=0; i<vertices.length; i++) {
				if (pointContainedByConvexCurve(vertices[i], norms, lines)) {
					in[i] = true;
					cnt++;
				}
			}			
		} else {	// case for an angle >= 90 degrees
			float[] normal = VectorUtil.crossVec3(new float[3], c.endIntersects.get(0), c.startIntersects.get(0));
			for (int i=0; i<vertices.length; i++) {
				float[] dir = VectorUtil.subVec3(new float[3], vertices[i], c.startIntersects.get(0));
				if (VectorUtil.dotVec3(OctTree.normalizeVec3(dir), OctTree.normalizeVec3(normal)) > 0f - MICRO_EPSILON) {
					in[i] = true;
					cnt++;
				}
			}			
		}
		// mark the vertices that are inside the curve
		
		
		if (cnt == 0) { // easy case, take the corner data form a triangle and return
			ret = lines;
		} else if (cnt == 1) { // split into two triangles and go
			ret = new float[6][3];
			for (int i=0; i<in.length; i++) {
				if (in[i]) {
					// first triangle
					ret[0] = c.location; 
					ret[1] = c.startIntersects.get(0);
					ret[2] = vertices[i];
					// second triangle
					ret[3] = c.endIntersects.get(0);
					ret[4] = c.location;
					ret[5] = vertices[i];
				}
			}
		} else if (cnt == 2) { // more complex case
			ret = new float[9][3];
			if (in[0] && in[1]) {
				// first triangle
				ret[0] = c.location;
				ret[1] = c.startIntersects.get(0);
				ret[2] = vertices[0];
				// second triangle
				ret[3] = c.location;
				ret[4] = vertices[0];
				ret[5] = vertices[1];
				// third triangle
				ret[6] = c.location;
				ret[7] = vertices[1];
				ret[8] = c.endIntersects.get(0);				
			} else if (in[1] && in[2]) {
				// first triangle
				ret[0] = c.location;
				ret[1] = c.startIntersects.get(0);
				ret[2] = vertices[1];
				// second triangle
				ret[3] = c.location;
				ret[4] = vertices[1];
				ret[5] = vertices[2];
				// third triangle
				ret[6] = c.location;
				ret[7] = vertices[2];
				ret[8] = c.endIntersects.get(0);				
			} else if (in[2] && in[0]) {
				// first triangle
				ret[0] = c.location;
				ret[1] = c.startIntersects.get(0);
				ret[2] = vertices[2];
				// second triangle
				ret[3] = c.location;
				ret[4] = vertices[2];
				ret[5] = vertices[0];
				// third triangle
				ret[6] = c.location;
				ret[7] = vertices[0];
				ret[8] = c.endIntersects.get(0);				
			}
		} else if (cnt == 3) {  // inverse of case 0,  hardest case
			// determine which side the endIntersects are associated with
			// caluculate plane normal for triiangle
			float[] triNormal = VectorUtil.crossVec3(new float[3], VectorUtil.subVec2(new float[3], vertices[B], vertices[A]), VectorUtil.subVec3(new float[3], vertices[C], vertices[A]));
			// determine which axis to drop for projection to 2D
			int axisToDrop = OctTree.fastMostOrthoganalAxisToPlane(triNormal);
			// project the triangle to 2D
			float[][] vertices2D = OctTree.project3DTo2D(vertices, axisToDrop);
			float[][] endInters2D = OctTree.project3DTo2D(new float[][]{c.endIntersects.get(0),  c.startIntersects.get(0)}, axisToDrop);
			
			int closestStartSide = -1;
			int closestEndSide = -1;
			
			// calculate the distances from the end intersect to each side of the triangle
			float end0 = OctTree.distanceSquarePointToSegment(vertices2D[0][X], vertices2D[0][Y], vertices2D[1][X], vertices[1][Y], endInters2D[0][X], endInters2D[0][Y]);
			float end1 = OctTree.distanceSquarePointToSegment(vertices2D[1][X], vertices2D[1][Y], vertices2D[2][X], vertices[2][Y], endInters2D[0][X], endInters2D[0][Y]);
			float end2 = OctTree.distanceSquarePointToSegment(vertices2D[2][X], vertices2D[2][Y], vertices2D[0][X], vertices[0][Y], endInters2D[0][X], endInters2D[0][Y]);
			
			// calculate the distances from the start intersect to each side of the triangle
			float start0 = OctTree.distanceSquarePointToSegment(vertices2D[0][X], vertices2D[0][Y], vertices2D[1][X], vertices[1][Y], endInters2D[1][X], endInters2D[1][Y]);
			float start1 = OctTree.distanceSquarePointToSegment(vertices2D[1][X], vertices2D[1][Y], vertices2D[2][X], vertices[2][Y], endInters2D[1][X], endInters2D[1][Y]);
			float start2 = OctTree.distanceSquarePointToSegment(vertices2D[2][X], vertices2D[2][Y], vertices2D[0][X], vertices[0][Y], endInters2D[1][X], endInters2D[1][Y]);
			
			if (end0 < end1) {
				if (end0 < end2) {
					closestEndSide = A;
				} else { // end2
					closestEndSide = C;
				}
			} else if (end1 < end2) {
				closestEndSide = B;
			} else {
				closestEndSide = C;
			}
			
			if (start0 < start1) {
				if (start0 < start2) {
					closestStartSide = A;
				} else { // start2
					closestStartSide = C;
				}
			} else if (start1 < start2) {
				closestStartSide = B;
			} else {
				closestStartSide = C;
			}
			
			if (closestEndSide == closestStartSide) {
				ret = new float[12][3];
				if (closestEndSide == A) {
					// first triangle
					ret[0] = vertices[2];
					ret[1] = vertices[0];
					ret[2] = c.endIntersects.get(0);
					// second triangle
					ret[3] = vertices[2];
					ret[4] = c.endIntersects.get(0);
					ret[5] = c.location;
					// third triangle
					ret[6] = vertices[2];
					ret[7] = c.location;
					ret[8] = c.startIntersects.get(0);				
					// fourth triangle
					ret[9] = vertices[2];
					ret[10] = c.startIntersects.get(0);
					ret[11] = vertices[1];				
				} else if (closestEndSide == B) {
					// first triangle
					ret[0] = vertices[0];
					ret[1] = vertices[1];
					ret[2] = c.endIntersects.get(0);
					// second triangle
					ret[3] = vertices[0];
					ret[4] = c.endIntersects.get(0);
					ret[5] = c.location;
					// third triangle
					ret[6] = vertices[0];
					ret[7] = c.location;
					ret[8] = c.startIntersects.get(0);				
					// fourth triangle
					ret[9] = vertices[0];
					ret[10] = c.startIntersects.get(0);
					ret[11] = vertices[2];									
				} else if (closestEndSide == C) {
					// first triangle
					ret[0] = vertices[1];
					ret[1] = vertices[2];
					ret[2] = c.endIntersects.get(0);
					// second triangle
					ret[3] = vertices[1];
					ret[4] = c.endIntersects.get(0);
					ret[5] = c.location;
					// third triangle
					ret[6] = vertices[1];
					ret[7] = c.location;
					ret[8] = c.startIntersects.get(0);				
					// fourth triangle
					ret[9] = vertices[1];
					ret[10] = c.startIntersects.get(0);
					ret[11] = vertices[0];									
				}
			} else { // this is a weird case...
				log.aprintln("Unable to tessellate a single corner triangle with all vertices enclosed.");
				return ret;
			}
		}
		
		return ret;
	}
	
	private static float[][] findPtsInteriorToConvexCurve(TriLineIntersect tli) {
		float[][] ret = null;
		float[][] lines = tli.getIntersections();
		if (lines.length < 2) {
			return ret;
		}
		float[][] vertices = tli.getTriangle().points;
		
		int cnt = tli.getIntersectionObjs().size();
		int[] in = new int[] {0,0,0}; 

		for (Intersection inter : tli.getIntersectionObjs()) {
			float[][] pts = inter.getPoints();
			if (pts.length == 2 && VectorUtil.isVec3Zero(VectorUtil.crossVec3(new float[3], normalizeVec3(new float[3], pts[0]), normalizeVec3(new float[3], pts[1])), 0, MINI_EPSILON)) {
				cnt--;
				continue;
			}

			float[][] norms = new float[pts.length - 1][3];
			for (int j=1; j<pts.length; j++) {
				norms[j-1] = normalizeVec3(new float[3], VectorUtil.crossVec3(new float[3], normalizeVec3(new float[3], pts[j-1]), normalizeVec3(new float[3], pts[j])));
			}

			for (int i=0; i<vertices.length; i++) {
				if (pointContainedByConvexCurve(vertices[i], norms, pts)) {
					in[i]++;
				}
			}						
		}
			
		
		if (in[0] < cnt && in[1] < cnt && in[2] < cnt) { // easy case no tri vertices inside the curve
			return null;
		} else if (in[0] == cnt ^ in[1] == cnt ^ in[2] == cnt) { // almost as easy
			for (int i=0; i<in.length; i++) {
				if (in[i] == cnt) {
					return new float[][]{vertices[i]};
				}
			}
		} else { // more complex case
			ret = new float[2][3];
			if (in[0] == cnt && in[1] == cnt && in[2] < cnt) {
				ret[0] = vertices[0];
				ret[1] = vertices[1];
			} else if (in[1] == cnt && in[2] == cnt && in[0] < cnt) {
				ret[0] = vertices[1];
				ret[1] = vertices[2];
			} else if (in[2] == cnt && in[0] == cnt && in[1] < cnt) {
				// first triangle
				ret[0] = vertices[2];
				ret[1] = vertices[0];
			}
		}	
		return ret;
	}

	
	/*
	 * Tessellates a convex polygon of co-planar 3D points in a triangle fan fashion 
	 *
	 * @param points ArrayList of 3D points that MUST be in CCW winding order or results will be unpredictable
	 * @return list of generated Triangles
	 */
	private static ArrayList<Triangle> tessellateCoPlanarConvexPolygon(ArrayList<float[]> points) {
		ArrayList<Triangle> poly = new ArrayList<>();
		if (points.size() < 3) {
			// nothing to do here
			return poly;
		}
		if (points.size() == 3) {
			poly.add(new Triangle(new float[][]{points.get(0), points.get(1), points.get(2)}));
			return poly;
		}
		float[] start = points.get(0);
		
		for (int i=1; i<points.size() - 1; i++) {
			float[][] pts = new float[3][3];
			pts[0] = start;
			pts[1] = points.get(i);
			pts[2] = points.get(i+1);
			Triangle t = new Triangle(pts);
			poly.add(t);
		}
		return poly;
	}
	
	private static float[][] tessellateConvexPolygon(ArrayList<float[]> points) {
		ArrayList<Triangle> poly = new ArrayList<>();
		if (points.size() < 4) {
			// nothing to do here
			return null;
		}
		float[] start = points.get(0);
		
		for (int i=1; i<points.size() - 1; i++) {
			float[][] pts = new float[3][3];
			pts[0] = start;
			pts[1] = points.get(i);
			pts[2] = points.get(i+1);
			Triangle t = new Triangle(pts);
			poly.add(t);
		}
		float[][] ret = new float[poly.size() * 3][3];
		int idx = 0;
		for (Triangle tri : poly) {
			ret[idx++] = tri.points[A];
			ret[idx++] = tri.points[B];
			ret[idx++] = tri.points[C];
		}
		return ret;
	}
	
	private static int longestSideOfTriangle(float[][] tri, float[] length) {
		float[] sideLen = new float[3];
		sideLen[0] = VectorUtil.distVec3(tri[0],tri[1]);
		sideLen[1] = VectorUtil.distVec3(tri[1], tri[2]);
		sideLen[2] = VectorUtil.distVec3(tri[2], tri[0]);
		
		int longSide = 0;
		float temp = 0f;
		for (int i=0; i<sideLen.length; i++) {
			if (sideLen[i] > temp) {
				temp = sideLen[i];
				longSide = i;
			}	
		}
		length[0] = temp;
		return longSide;
	}
	
	private static float[][] normalsFrom2PointsOrigin(float[][] lines) {
		float[][] normals = new float[lines.length-1][3];
		
		for(int i=1; i<lines.length; i++) {
            normals[i-1] = VectorUtil.crossVec3(normals[i-1], lines[i-1], lines[i]);
		}
		
		return normals;
	}
	
	public static boolean pointLeftOfCcwLine(float[] lineStart, float[] lineEnd, float[] point) {
		// get a reference normal vector
		float[] cross = VectorUtil.crossVec3(new float[3], VectorUtil.normalizeVec3(new float[3], lineStart), VectorUtil.normalizeVec3(new float[3], lineEnd));
		// move the vector to the origin
		float[] diff = VectorUtil.subVec3(new float[3], point, lineStart);
		if (VectorUtil.dotVec3(VectorUtil.normalizeVec3(diff), VectorUtil.normalizeVec3(cross)) > 0f) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean pointLeftOfCcwLineUseDoubles(float[] lineStart, float[] lineEnd, float[] point) {
		// get a reference normal vector
		Vector3D start = new Vector3D(lineStart[X], lineStart[Y], lineStart[Z]);
		Vector3D end = new Vector3D(lineEnd[X], lineEnd[Y], lineEnd[Z]);
		Vector3D pt = new Vector3D(point[X], point[Y], point[Z]);
		
		Vector3D normStart = start.normalize();
		Vector3D normEnd = end.normalize();
		
		Vector3D cross = normStart.cross(normEnd);
		Vector3D diff = pt.subtract(start);
		Vector3D nDiff = pt.normalize();
		Vector3D nCross = cross.normalize();
		if (nDiff.dot(nCross) > 0.0) {
			return true;
		} else {
			return false;
		}
	}
	
	private static float[] closePointLoop(float[] pts) {
		float[] newPts = new float[pts.length + 3];
		int i=0;
		for(; i<pts.length; i++) {
			newPts[i] = pts[i];
		}
		newPts[i++] = pts[0];
		newPts[i++] = pts[1];
		newPts[i++] = pts[2];		
		return newPts;
	}
	
	private float[][] copyTriangle(float[][] tri) {
		float[][] ret = new float[tri.length][3];
		for (int i=0; i<tri.length; i++) {
			ret[i][0] = tri[i][0];
			ret[i][1] = tri[i][1];
			ret[i][2] = tri[i][2];
		}
		return ret;
	}
	
	/*
	 * Method to return the number of triangles in the OctTree
	 *
	 * @return number of triangles
	 *
	 * <thread-safe?>
	 */
	private int getSize() {
		return OBJECT_COUNT;
	}
	
	/**
	 * Method to determine if a triangle is degenerate
	 * i.e. all three points for a line or two points are co-incident
	 * @param tri the triangle to test as an array of XYZ points
	 * @return true if the triangle is degenerate
	 */
	public static boolean isTriangleDegenerate(float[][] tri) {
		float[] ba = VectorUtil.subVec3(new float[3], tri[A], tri[B]);
		float[] ban = VectorUtil.normalizeVec3(new float[3], ba);
		float[] bc = VectorUtil.subVec3(new float[3], tri[C], tri[B]);
		float[] bcn = VectorUtil.normalizeVec3(new float[3], bc);
		float[] baXbc = VectorUtil.crossVec3(new float[3], ban, bcn);
		float baXbcSq = VectorUtil.normSquareVec3(baXbc);
		
		if (baXbcSq <= FloatUtil.EPSILON) {
			return true;
		} else {
			return false;
		}
		
	}
	

	// ported from http://paulbourke.net/geometry/polygonmesh/
	private static float min(float x, float y) {
		return x < y ? x : y;
	}

	// ported from http://paulbourke.net/geometry/polygonmesh/
	private static float max(float x, float y) {
		return x > y ? x : y;
	}

	// ported from http://paulbourke.net/geometry/polygonmesh/
	private static boolean insidePolygon(float[][] polygon, int n, float[] p) {
		int counter = 0;
		int i;
		double xinters;
		float[] p1 = new float[2];
		float[] p2 = new float[2];

		p1 = polygon[0];
		if (Float.compare(p1[X],p[X]) == 0 && Float.compare(p1[Y],p[Y]) == 0) {
			// This is the pathological case where the point is an exact match for a polygon vertex
			// In this case we consider it inside the polygon
			return true;
		}
		for (i = 1; i <= n; i++) {
			p2 = polygon[i % n];
			if (Float.compare(p2[X],p[X]) == 0 && Float.compare(p2[Y],p[Y]) == 0) {
				// This is the pathological case where the point is an exact match for a polygon vertex
				// In this case we consider it inside the polygon
				return true;
			}
			if (p[Y] > min(p1[Y], p2[Y])) {
				if (p[Y] <= max(p1[Y], p2[Y])) {
					if (p[X] <= max(p1[X], p2[X])) {
						if (p1[Y] != p2[Y]) {
							xinters = (p[Y] - p1[Y]) * (p2[X] - p1[X])
									/ (p2[Y] - p1[Y]) + p1[X];
							if (Float.compare(p1[X], p2[X]) == 0 || p[X] <= xinters) {
								counter++;
							}
						}
					}
				}
			}
			p1 = p2;
		}

		if (counter % 2 == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	// ported from http://paulbourke.net/geometry/polygonmesh/
	private static boolean insideOnPolygon(float[][] polygon, int n, float[] p) {
		int counter = 0;
		int i;
		double xinters;
		float[] p1 = new float[2];
		float[] p2 = new float[2];

		p1 = polygon[0];
		
		if (FloatUtil.compare(p1[X],p[X], MINI_EPSILON) == 0 && FloatUtil.compare(p1[Y],p[Y], MINI_EPSILON) == 0) {
			// This is the pathological case where the point is an exact match for a polygon vertex
			// In this case we consider it inside the polygon
			return true;
		}
		for (i = 1; i <= n; i++) {
			p2 = polygon[i % n];
			if (FloatUtil.compare(p2[X],p[X], MINI_EPSILON) == 0 && FloatUtil.compare(p2[Y],p[Y], MINI_EPSILON) == 0) {
				// This is the pathological case where the point is an exact match for a polygon vertex
				// In this case we consider it inside the polygon
				return true;
			}
			if (p[Y] > min(p1[Y], p2[Y])) {
				if (p[Y] <= max(p1[Y], p2[Y])) {
					if (p[X] <= max(p1[X], p2[X])) {
						if (p1[Y] != p2[Y]) {
							xinters = (p[Y] - p1[Y]) * (p2[X] - p1[X])
									/ (p2[Y] - p1[Y]) + p1[X];
							if (Float.compare(p1[X], p2[X]) == 0 || p[X] <= xinters) {
								counter++;
							}
						}
					}
				}
			}
			p1 = p2;
		}

		if (counter % 2 == 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Ported from https://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
	 * including a modification to include points that lie exactly on the edge of the polygon
	 * 
	 * Copyright (c) 1970-2003, Wm. Randolph Franklin
	 *
	 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), 
	 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
	 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
	 *		
	 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimers.
	 * Redistributions in binary form must reproduce the above copyright notice in the documentation and/or other materials provided with the distribution.
	 * The name of W. Randolph Franklin may not be used to endorse or promote products derived from this Software without specific prior written permission.
	 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
	 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
	 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
	 * IN THE SOFTWARE.
	 * 
	 * @param point
	 * @param polygon may be an open OR closed polygon
	 * @return true if point is in or on the edge of the polygon
	 */
	public static boolean pointInPolygon(float[] point, float[][] polygon) {
		int i, j, nvert = polygon.length;
		boolean c = false;

		for (i = 0, j = nvert - 1; i < nvert; j = i++) {
			if (((polygon[i][Y] >= point[Y]) != (polygon[j][Y] >= point[Y]))
					&& (point[X] <= (polygon[j][X] - polygon[i][X])
							* (point[Y] - polygon[i][Y])
							/ (polygon[j][Y] - polygon[i][Y]) + polygon[i][X]))
				c = !c;
		}

		return c;
	}
	
	/* Port of by Philippe Reverdy's 2D solution as found on:
	 * http://paulbourke.net/geometry/polygonmesh/
	 */
	private boolean isPointInsidePolygon(float[][] polygon, int n, float[] p) {
	   int i;
	   double angle=0;
	   float[] p1 = new float[2];
	   float[] p2 = new float[2];

	   for (i=0; i<n; i++) {
	      p1[X] = polygon[i][X] - p[X];
	      p1[Y] = polygon[i][Y] - p[Y];
	      p2[X] = polygon[(i+1) % n][X] - p[X];
	      p2[Y] = polygon[(i+1) % n][Y] - p[Y];
	      angle += angle2D(p1[X], p1[Y], p2[X], p2[Y]);
	   }

	   if (Math.abs(angle) < Math.PI) {
	      return(false);
	   }
	   else {
	      return(true);
	   }
	}

	/*
	 * Ported from http://paulbourke.net/geometry/polygonmesh/
	 * Return the angle between two vectors on a plane
	 * The angle is from vector 1 to vector 2, positive anticlockwise
	 * The result is between -pi -> pi
	*/
	private double angle2D(double x1, double y1, double x2, double y2) {
	   double dtheta,theta1,theta2;

	   theta1 = Math.atan2(y1,x1);
	   theta2 = Math.atan2(y2,x2);
	   dtheta = theta2 - theta1;
	   while (dtheta > Math.PI)
	      dtheta -= (Math.PI + Math.PI);
	   while (dtheta < -Math.PI)
	      dtheta += (Math.PI + Math.PI);

	   return(dtheta);
	}	
	
	private static boolean isPoint2OnlineSegment2(float[] pt, float[] start, float[] end) {
		float dxc = pt[X] - start[X];
		float dyc = pt[Y] - start[Y];

		float dxl = end[X] - start[X];
		float dyl = end[Y] - start[Y];

		float cross = dxc * dyl - dyc * dxl;
		
		if (cross > EPSILON) {
			return false;
		}
		
		if (FloatUtil.abs(dxl) >= FloatUtil.abs(dyl))
			  return dxl > 0 ? 
			    start[X] <= pt[X] && pt[X] <= end[X] :
			    end[X] <= pt[X] && pt[X] <= start[X];
			else
			  return dyl > 0 ? 
			    start[Y] <= pt[Y] && pt[Y] <= end[Y] :
			    end[Y] <= pt[Y] && pt[Y] <= start[Y];		
		
	}
		
	/**
	 * Method to return all the facets of the shape model currently in use, if one is in use.
	 *
	 * @return a Map of all the Facets in the shape model in the form of Triangle objects
	 *
	 * probably thread-safe
	 */
	public Map<Integer, Triangle> getAllShapeModelFacets() {
		HashMap<Integer, Triangle> list = new HashMap<>();
		if (this.root != null) {
			this.looseGetAllTriangles(this.root, list);
		}
		return list;
	}
	
	/**
	 * Method to determine if 2 line segments are collinear
	 * The segments can have full, partial, or no overlap
	 *
	 * @param s1Start 1st segment start point
	 * @param s1End 1st segment end point
	 * @param s2Start 2nd segment start point
	 * @param s2End 2nd segment end point
	 * @return true if line segments are collinear
	 */
	public static boolean areLineSegmentsCollinear(float[] s1Start, float[] s1End, float[] s2Start, float[] s2End) {
		float m1 = VectorUtil.normVec3(VectorUtil.crossVec3(new float[3], VectorUtil.subVec3(new float[3], s1End, s1Start), VectorUtil.subVec3(new float[3], s2Start, s1Start)));
		float m2 = VectorUtil.normVec3(VectorUtil.crossVec3(new float[3], VectorUtil.subVec3(new float[3], s1End, s1Start), VectorUtil.subVec3(new float[3], s2End, s1Start)));
		if (FloatUtil.isZero(m1, FloatUtil.EPSILON) && FloatUtil.isZero(m2, FloatUtil.EPSILON)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean collinearLineSegs(float[] s1Start, float[] s1End, float[] s2Start, float[] s2End) {
		// move 1st segment to the origin and normalize
		float[] seg1 = VectorUtil.normalizeVec3(new float[3], VectorUtil.subVec3(new float[3], s1End, s1Start));
		float[] seg2 = VectorUtil.normalizeVec3(new float[3], VectorUtil.subVec3(new float[3], s2End, s2Start));
//		float dot = VectorUtil.dotVec3(seg1, seg2);
		float[] cross = VectorUtil.crossVec3(new float[3], seg1, seg2);
		
//		System.err.println("Cross collinearity "+FloatUtil.isZero(VectorUtil.normVec3(cross), 0.00001f));
		
		if (FloatUtil.isZero(VectorUtil.normVec3(cross), 0.00001f)) {
//		if (FloatUtil.isEqual(FloatUtil.abs(dot), 1f, FloatUtil.EPSILON)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Method to determine the shortest distance from a 3D point to a 3D line segment
	 * Implemented from http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html 
	 *
	 * @param lineStart start of the line segment
	 * @param lineEnd end of the line segment
	 * @param point point to measure distance from
	 * @return the distance from the line segment to the point
	 */
	public static float closestDistancePointToLine(float[] lineStart, float[] lineEnd, float[] point) {
		if (lineStart == null || lineStart.length != 3 
				|| lineEnd == null || lineEnd.length != 3
				|| point == null || point.length != 3) {
			return Float.NaN;
		}
		float denom = VectorUtil.normVec3(VectorUtil.subVec3(new float[3], lineEnd, lineStart));
		
		// check for a degenerate line which can trigger divide by zero
		if (FloatUtil.isZero(denom, FloatUtil.EPSILON)) {
			return Float.POSITIVE_INFINITY;
		}
		
		float[] ptStartDiff = VectorUtil.subVec3(new float[3], point, lineStart);
		float[] ptEndDiff = VectorUtil.subVec3(new float[3], point, lineEnd);
		float[] startDiffxEndDiff = VectorUtil.crossVec3(new float[3], ptStartDiff, ptEndDiff);
		
		float num = VectorUtil.normVec3(startDiffxEndDiff);
		
		return num/denom;
	}
	
	/**
	 * Method to generate a frustum from a surface polygon.
	 * Each edge of the polygon combined the origin become
	 * a makeshift frustum with as many "sides" as the polygon
	 * has edges.
	 * @param corners the points off the input polygon
	 * @return the frustum as a 2D array
	 */
	public static float[][] getFrustumFromPolygon(float[][] corners) {
		if (corners == null || corners.length < 3) {
			return null;
		}
		float[][] fcorners = null;
		int len = corners.length;
		if (VectorUtil.isVec3Equal(corners[0], 0, corners[corners.length-1], 0)) {
			len--;
		}
		fcorners = new float[len][];
		for (int j=0; j<len; j++) {
			fcorners[j] = new float[] {corners[j][0], corners[j][1], corners[j][2]};
		}
		float[][] frus = new float[len + 2][4];
		int frustumIdx = 0;
		
		float[] planeNorm = new float[3];
		float[] planePt = new float[3];

		// generate all the "polygonal" sides except the "closing" side
		for (int i=0; i<len; i++) {
			if (triPlaneNormalPoint(new float[][]{fcorners[i%fcorners.length], fcorners[(i+1)%fcorners.length], new float[]{0f, 0f, 0f}}, planeNorm, planePt)) {
				float distance = FloatUtil.abs(VectorUtil.dotVec3(planePt, planeNorm));
				frus[frustumIdx][X] = planeNorm[X];
				frus[frustumIdx][Y] = planeNorm[Y];
				frus[frustumIdx][Z] = planeNorm[Z];
				frus[frustumIdx][W] = distance;
			} else {
				return null;
			}
			planeNorm = clearVector(planeNorm);
			planePt = clearVector(planePt);
			frustumIdx++;
		}
		
//		// generate the "closing" side
//		
//		if (triPlaneNormalPoint(new float[][]{corners[corners.length-1], corners[0], new float[]{0f, 0f, 0f}}, planeNorm, planePt)) {
//			float distance = FloatUtil.abs(VectorUtil.dotVec3(planePt, planeNorm));
//			frus[frustumIdx][X] = planeNorm[X];
//			frus[frustumIdx][Y] = planeNorm[Y];
//			frus[frustumIdx][Z] = planeNorm[Z];
//			frus[frustumIdx][W] = distance;
//		} else {
//			return null;
//		}
//		planeNorm = clearVector(planeNorm);
//		planePt = clearVector(planePt);
//		frustumIdx++;
		
		// FAR plane
		float[] fPlaneNorm = avgOf3DPolygon(corners);
		float[] fPlaneNormUnit = VectorUtil.normalizeVec3(new float[] {fPlaneNorm[0],fPlaneNorm[1],fPlaneNorm[2]});
		float distance = 0f; // plane is at the origin
		frus[frustumIdx][X] = fPlaneNormUnit[X];
		frus[frustumIdx][Y] = fPlaneNormUnit[Y];
		frus[frustumIdx][Z] = fPlaneNormUnit[Z];
		frus[frustumIdx][W] = distance;
		frustumIdx++;

		// NEAR plane
		// need to scale up to be sure we clear any topography contained within the region
		distance = VectorUtil.normVec3(fPlaneNorm) * 2f;
		frus[frustumIdx][X] = -fPlaneNormUnit[X];
		frus[frustumIdx][Y] = -fPlaneNormUnit[Y];
		frus[frustumIdx][Z] = -fPlaneNormUnit[Z];
		frus[frustumIdx][W] = distance;
		
		return frus;
	}

	/**
	 * Method to intersect a frustum with the OctTree and return all facets and points that fall within the frustum.
	 * @param normal surface normal to include only facets facing the large end of the frustum
	 * @param frustum
	 * @param n OctTree node starting point
	 * @param list return list of entirely contained facets
	 * @param pts return list of entirely contained points
	 */
	public void looseOctTreeFrustumIntersectPoints(float[] normal, float[][] frustum, ONode n, ArrayList<Triangle> list, ArrayList<float[]> pts) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		
		// check to see if this node is in the frustum
		if (cubeInAnyFrustum(n.cx, n.cy, n.cz, halfSize, frustum) == 0) {
			// nothing to do here node is outside the frustum
			return;
		}
		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						looseOctTreeFrustumIntersectPoints(normal, frustum, n.child[k][j][i], list, pts);
					}
				}
			}
		}

		// test every triangle in the node and if any part of the triangle is inside the frustum add the points that are inside to the return list.
		Triangle t = n.object;
		while (t != null) {
			float[] norm = new float[]{0f, 0f, 0f};
			triPlaneNormalPoint(t.points, norm, new float[3]);
			if (polygonInFrustum(t.points, frustum) && VectorUtil.dotVec3(normal, OctTree.normalizeVec3(norm)) >= 0) {
				int ptsAdded = 0;
				for (int idx=0; idx<t.points.length; idx++) {
					if (pointInFrustum(t.points[idx], frustum)) {
						pts.add(t.points[idx]);
						ptsAdded++;
					}					
				}
				if (ptsAdded == 3) {
					list.add(t);
				}
			}	
			t = t.next;
		}
	}
/**
 * Method to intersect a frustum with the OctTree and return all facets that fall within the frustum using a point by point test.
 * @param normal surface normal to include only facets facing the large end of the frustum
 * @param frustum 
 * @param n OctTree node starting point
 * @param list return list of entirely contained facets
 */
	public void looseOctTreeFrustumIntersectFacets(float[] normal, float[][] frustum, ONode n, ArrayList<Triangle> list) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		
		// check to see if this node is in the frustum
		if (cubeInAnyFrustum(n.cx, n.cy, n.cz, halfSize, frustum) == 0) {
			// nothing to do here node is outside the frustum
			return;
		}
		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						looseOctTreeFrustumIntersectFacets(normal, frustum, n.child[k][j][i], list);
					}
				}
			}
		}

		// test every triangle in the node and if any part of the triangle is inside the frustum add the points that are inside to the return list.
		Triangle t = n.object;
		while (t != null) {
			float[] norm = new float[]{0f, 0f, 0f};
			triPlaneNormalPoint(t.points, norm, new float[3]);
			boolean inFrustum = true;
			if (VectorUtil.dotVec3(normal, VectorUtil.normalizeVec3(norm)) >= 0) {
				for (int idx=0; idx<t.points.length && inFrustum; idx++) {
					if (!pointInFrustum(t.points[idx], frustum)) {
						inFrustum = false;
					}					
				}
				if (inFrustum) {
					list.add(t);
				}
			}
			t = t.next;
		}
	}

	/**
	 * Method to intersect a frustum with the OctTree and return all facets that fall within the frustum using a polygon test.
	 * @param normal surface normal to include only facets facing the small end of the frustum
	 * @param frustum
	 * @param n OctTree node starting point
	 * @param list return list of entirely contained facets
	 */
	public void looseOctTreeFrustumIntersect(float[] normal, float[][] frustum, ONode n, ArrayList<Triangle> list) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		
		// check to see if this node is in the frustum
		if (cubeInAnyFrustum(n.cx, n.cy, n.cz, halfSize, frustum) == 0) {
			// nothing to do here node is outside the frustum
			return;
		}
		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						looseOctTreeFrustumIntersect(normal, frustum, n.child[k][j][i], list);
					}
				}
			}
		}

		// Count objects in this node.
		Triangle o = n.object;
		while (o != null) {
//			float[] norm = new float[]{0f, 0f, 0f};
//			triPlaneNormalPoint(o.points, norm, new float[3]);
//			float[] planeEq = new float[4];
//			VectorUtil.getPlaneVec3(planeEq, o.points[A], o.points[B], o.points[C], new float[3], new float[3]);
			float[] pt1 = VectorUtil.normalizeVec3(new float[3], o.points[0]);
			float[] pt2 = VectorUtil.normalizeVec3(new float[3], o.points[1]);
			float[] pt3 = VectorUtil.normalizeVec3(new float[3], o.points[2]);
			
			float[] cross = VectorUtil.crossVec3(new float[3], VectorUtil.subVec3(new float[3], o.points[1], o.points[0]), 
					VectorUtil.subVec3(new float[3], o.points[2], o.points[0]));
			
			if (VectorUtil.normVec3(cross) < 0.0005f) {
				cross[0] *= 1000f;
				cross[1] *= 1000f;
				cross[2] *= 1000f;
			}

//			if (polygonInFrustum(o.points, frustum) && VectorUtil.dotVec3(normal, VectorUtil.normalizeVec3(new float[3], new float[]{planeEq[X], planeEq[Y], planeEq[Z]})) => 0) {
			if (polygonInFrustum(o.points, frustum) && 
					VectorUtil.dotVec3(VectorUtil.normalizeVec3(new float[3], normal), 
							VectorUtil.normalizeVec3(new float[3], new float[]{cross[X], cross[Y], cross[Z]})) > 0) {
				if (!list.contains(o)) {
					list.add(o);
				}
			}	
			o = o.next;
		}
	}

	public void looseOctTreeFrustumIntersect(float[] normal, float[][] frustum, ONode n, HashMap<Integer, Triangle> list) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		
		// check to see if this node is in the frustum
		if (cubeInAnyFrustum(n.cx, n.cy, n.cz, halfSize, frustum) == 0) {
			// nothing to do here node is outside the frustum
			return;
		}
		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						looseOctTreeFrustumIntersect(normal, frustum, n.child[k][j][i], list);
					}
				}
			}
		}

		// Count objects in this node.
		Triangle o = n.object;
		while (o != null) {
			float[] cross = VectorUtil.crossVec3(new float[3], VectorUtil.subVec3(new float[3], o.points[1], o.points[0]), 
					VectorUtil.subVec3(new float[3], o.points[2], o.points[0]));
			
			if (VectorUtil.normVec3(cross) < 0.0005f) {
				cross[0] *= 1000f;
				cross[1] *= 1000f;
				cross[2] *= 1000f;
			}

			if (polygonInFrustum(o.points, frustum) && 
					VectorUtil.dotVec3(VectorUtil.normalizeVec3(new float[3], normal), 
							VectorUtil.normalizeVec3(new float[3], new float[]{cross[X], cross[Y], cross[Z]})) > 0) {
					list.put(o.id, o);
			}	
			o = o.next;
		}
	}
	

	/**
	 * Method to intersect a frustum with the OctTree and return all facets that fall within the frustum using a polygon test using JOGL objects.
	 * @param normal surface normal to include only facets facing the large end of the frustum
	 * @param frustum
	 * @param n OctTree node starting point
	 * @param list return list of entirely contained facets
	 */
	public void looseOctTreeJoglFrustumIntersect(float[] normal, Frustum frustum, ONode n, ArrayList<Triangle> list) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		
		if(frustum.isAABBoxOutside(new AABBox(new float[] {n.cx-halfSize, n.cy-halfSize, n.cz-halfSize}, new float[] {n.cx+halfSize, n.cy+halfSize, n.cz+halfSize}))) {
			return;
		}
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						looseOctTreeJoglFrustumIntersect(normal, frustum, n.child[k][j][i], list);
					}
				}
			}
		}

		// Count objects in this node.
		Triangle o = n.object;
		while (o != null) {
			float[] norm = new float[]{0f, 0f, 0f};
			triPlaneNormalPoint(o.points, norm, new float[3]);
			float[] planeEq = new float[4];
			VectorUtil.getPlaneVec3(planeEq, o.points[A], o.points[B], o.points[C], new float[3], new float[3]);
			if (polygonInJoglFrustum(o.points, frustum) && VectorUtil.dotVec3(normal, VectorUtil.normalizeVec3(new float[3], new float[]{planeEq[X], planeEq[Y], planeEq[Z]})) >= 0) {
				list.add(o);
			}	
			o = o.next;
		}
	}


	/**
	 * Ported from: http://www.crownandcutlass.com/features/technicaldetails/frustum.html
	 * This routine tests all eight corners against each plane, counting how many corners are in front of the plane. 
	 * If none of the eight corners are in front, then we can exit immediately because the entire cube is outside the frustum. 
	 * If all eight are in front of the plane then we increment a second counter. 
	 * When we reach the end we know that at least some part of the cube is in the frustum, so we check the second counter. 
	 * If it equals the number of sides in the frustum then the cube is completely within the frustum, otherwise only part of it is.
	 *
	 * @param x cube center X coordinate
	 * @param y cube center Y coordinate
	 * @param z cube center Z coordinate
	 * @param size half the width of a single side of the cube
	 * @param frustum
	 * @return 2 is the cube is entirely inside the frustum, 1 if part of the cube is inside the frustum, 0 if the cube is outside the frustum
	 */
	public static int cubeInAnyFrustum(float x, float y, float z, float size, float[][] frustum) {
	   int p;
	   int c;
	   int c2 = 0;

	   for( p = 0; p < frustum.length; p++ ) {
	      c = 0;
	      if( frustum[p][0] * (x - size) + frustum[p][1] * (y - size) + frustum[p][2] * (z - size) + frustum[p][3] > 0 ) {
	         c++;
	      }
	      if( frustum[p][0] * (x + size) + frustum[p][1] * (y - size) + frustum[p][2] * (z - size) + frustum[p][3] > 0 ) {
	         c++;
	      }
	      if( frustum[p][0] * (x - size) + frustum[p][1] * (y + size) + frustum[p][2] * (z - size) + frustum[p][3] > 0 ) {
	         c++;
	      }
	      if( frustum[p][0] * (x + size) + frustum[p][1] * (y + size) + frustum[p][2] * (z - size) + frustum[p][3] > 0 ) {
	         c++;
	      }
	      if( frustum[p][0] * (x - size) + frustum[p][1] * (y - size) + frustum[p][2] * (z + size) + frustum[p][3] > 0 ) {
	         c++;
	      }
	      if( frustum[p][0] * (x + size) + frustum[p][1] * (y - size) + frustum[p][2] * (z + size) + frustum[p][3] > 0 ) {
	         c++;
	      }
	      if( frustum[p][0] * (x - size) + frustum[p][1] * (y + size) + frustum[p][2] * (z + size) + frustum[p][3] > 0 ) {
	         c++;
	      }
	      if( frustum[p][0] * (x + size) + frustum[p][1] * (y + size) + frustum[p][2] * (z + size) + frustum[p][3] > 0 ) {
	         c++;
	      }
	      if( c == 0 ) {
	         return 0;
	      }
	      if( c == 8 ) {
	         c2++;
	      }
	   }
	   return (c2 == frustum.length) ? 2 : 1;
	}

	/**
	 * Ported from: http://www.crownandcutlass.com/features/technicaldetails/frustum.html
	 * If all of the points are behind any single frustum plane then the polygon is invisible, 
	 * otherwise at least some part of it is in the frustum (we assume the polygons are convex of course).
	 * For each frustum plane we test all the points, breaking out early if a point is in front of a plane. 
	 * If all the points are behind the plane we return false immediately. 
	 * If we make it to the end then every plane has at least one point in front of it so the polygon is potentially visible.
	 * Don't use to test every polygon in the octtree!!!
	 *
	 * @param points
	 * @param frustum
	 * @return
	 */
	public static boolean polygonInFrustum(float[][] points, float[][] frustum) {
	   int f, p;
	   int numPts = points.length; 

	   for( f = 0; f < frustum.length; f++ ) {
	      for(p = 0; p < numPts; p++) {
	         if(frustum[f][0] * points[p][X] + frustum[f][1] * points[p][Y] + frustum[f][2] * points[p][Z] + frustum[f][3] > 0 )
	            break;
	      }
	      if(p == numPts) {
	         return false;
	      }
	   }
	   return true;
	}

	 /** 
	  * If all of the points are behind any single frustum plane then the polygon is invisible, 
	 * otherwise at least some part of it is in the frustum (we assume the polygons are convex of course).
	 * For each frustum plane we test all the points, breaking out early if a point is in front of a plane. 
	 * If all the points are behind the plane we return false immediately. 
	 * If we make it to the end then every plane has at least one point in front of it so the polygon is potentially visible.
	 * Don't use to test every polygon in the octtree!!!
	 *
	 * @param points
	 * @param frustum
	 * @return
	 */
	public static boolean polygonInJoglFrustum(float[][] points, Frustum frustum) {
		   int f, p;

	      for(p = 0; p < points.length; p++) {
	    	 Frustum.Location loc = frustum.classifyPoint(points[p]);
	         if(loc == Frustum.Location.INSIDE || loc == Frustum.Location.INTERSECT) {
	            return true;
	         }
	      }
		   return false;
		}

	/**
	 * Ported from: http://www.crownandcutlass.com/features/technicaldetails/frustum.html
	 * If a point is in front of all the frustum planes then the point is visible. 
	 * If any point is behind any frustum plane we return false immediately. 
	 * If we make it to the end then the point is potentially visible.
	 * Don't use to test every point in the octtree!!!
	 *
	 * @param pt
	 * @param frustum
	 * @return true if pt is inside the frustum
	 */
	public static boolean pointInFrustum(float[] pt, float[][] frustum)
	{
	   for (int p = 0; p < frustum.length; p++ ) {
	      if (frustum[p][0] * pt[X] + frustum[p][1] * pt[Y] + frustum[p][2] * pt[Z] + frustum[p][3] <= 0) {
	         return false;
	      }
	   }
	   return true;
	}

	public static boolean polygonFullyInFrustum(float[][] points, float[][] frustum) {
		   int f, p;
		   int numPts = points.length; 

		   for( f = 0; f < frustum.length; f++ ) {
		      for(p = 0; p < numPts; p++) {
		         if(frustum[f][0] * points[p][X] + frustum[f][1] * points[p][Y] + frustum[f][2] * points[p][Z] + frustum[f][3] <= 0 )
		            return false;
		      }
		   }
		   return true;
		}

	/**
	 * Method to remove duplicate 3 element float arrays from an array of float arrays (float[][]).
	 * The contained float arrays must be 3 element arrays representing 3D vectors (X,Y,Z)
	 *
	 * @param points input array of vectors
	 * @return array of unique vectors or null if the input array could not be processed
	 */
	public static float[][] removeDuplicatePoints(float[][] points) {
		
		if (points == null || points.length < 1) {
			return null;
		}
		
		TreeSet<float[]> set = new TreeSet<>(new java.util.Comparator<float[]>() {
		    public int compare(float[] a, float[] b) {
		    	
		    if (VectorUtil.isVec3Equal(a, 0, b, 0, MICRO_EPSILON)) {
		    		return 0;
		    }
		    
		    		int x = FloatUtil.compare(a[X], b[X], MICRO_EPSILON);
		        if (x != 0) {
		        	return x;
		        }
		        int y = FloatUtil.compare(a[Y], b[Y], MICRO_EPSILON);
		        if (y != 0) {
		        	return y;
		        }
		        int z = FloatUtil.compare(a[Z], b[Z], MICRO_EPSILON);
		        if (z != 0) {
		        	return z;
		        }
		        return 0;
		    }
		});
		
		for (int i=0; i<points.length; i++) {
			if (points[i].length != 3) {
				continue; // skip this one, it's not a valid 3D vector
			}
			set.add(points[i]);
		}
		if (set.size() < 1) {
			return null;
		}
		
		float[][] ret = new float[set.size()][];
		int idx = 0;
		for (float[] fa : set) {
			ret[idx++] = fa;
		}
		
		return ret;
	}
	
	public void looseOctTreeTriangleIntersectShortCircuit(Triangle poly, ONode n, ArrayList<float []> list) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		
		com.jogamp.opengl.math.Ray nRay1 = new com.jogamp.opengl.math.Ray();
		nRay1.orig[X] = poly.points[0][X];
		nRay1.orig[Y] = poly.points[0][Y];
		nRay1.orig[Z] = poly.points[0][Z];
		float[] direction = VectorUtil.normalizeVec3(new float[3], new float[] {-poly.points[0][X], -poly.points[0][Y], -poly.points[0][Z]});
		nRay1.dir[X] = direction[X];
		nRay1.dir[Y] = direction[Y];
		nRay1.dir[Z] = direction[Z];
		
		com.jogamp.opengl.math.Ray nRay2 = new com.jogamp.opengl.math.Ray();
		nRay2.orig[X] = poly.points[1][X];
		nRay2.orig[Y] = poly.points[1][Y];
		nRay2.orig[Z] = poly.points[1][Z];
		float[] direction1 = VectorUtil.normalizeVec3(new float[3], new float[] {-poly.points[1][X], -poly.points[1][Y], -poly.points[1][Z]});
		nRay2.dir[X] = direction1[X];
		nRay2.dir[Y] = direction1[Y];
		nRay2.dir[Z] = direction1[Z];
		
		AABBox box = n.getAABbox(halfSize);

		Triangle tri = new Triangle(new float[][] {poly.points[2], poly.points[1], poly.points[0]}); // ??? order ???
		if (!box.intersectsRay(nRay1) && !box.intersectsRay(nRay2) && !triBoxOverlap(box.getCenter(), new float[] {halfSize, halfSize, halfSize}, poly.points) && !testAABBTriangle(box, tri)) {
			return;
		}
		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						looseOctTreeTriangleIntersectShortCircuit(poly, n.child[k][j][i], list);
					}
				}
			}
		}

		// Count objects in this node.
		Triangle o = n.object;
		while (o != null) {	
			
			if (noDivTriTriIsect(poly.points[0], poly.points[1], new float[] {0f, 0f, 0f}, o.points[0], o.points[1], o.points[2])) {
				Integer co = 0;
				float[] source = new float[3];
				float[] target = new float[3];
				if (tri_tri_intersection_test_3d(poly.points[0], poly.points[1], new float[] {0f, 0f, 0f}, o.points[0], o.points[1], o.points[2], co, source, target) == 1 && 
						co == 0) {
					list.add(source);
					list.add(target);
				}
			
			}



			o = o.next;
		}
	}

 	/**
	 * Method to calculate the distance between 2 points on the shape model given the locations in
	 * LonLat (WEST leading).
	 * @param lonFrom longitude (West) of the starting point
	 * @param latFrom latitude of the starting point
	 * @param lonTo longitude (West) of the ending point
	 * @param latTo latitude of the ending point
	 * @return the distance in the same units the shape model is defined in (usually kilometers)
	 */
	public float getSurfaceDistance(float lonFrom, float latFrom, float lonTo, float latTo) {
		if (Float.compare(lonFrom, lonTo) == 0 && Float.compare(latFrom, latTo) == 0) {
			return 0f;
		}

		HVector startPt = new HVector(lonFrom, latFrom);
		HVector endPt = new HVector(lonTo, latTo);
		float scalar = (float)Util.EQUAT_RADIUS;
		float maxMesh = looseK * WORLD_SIZE / 2f;
		
		float[] start = new float[] {(float)startPt.x * maxMesh * 2f, (float)startPt.y * maxMesh * 2f,
				(float)startPt.z * maxMesh * 2f};
		float[] end = new float[] {(float)endPt.x * maxMesh * 2f, (float)endPt.y * maxMesh * 2f,
				(float)endPt.z * maxMesh * 2f};
		
		Ray startRay = new Ray(start, new float[] {-start[X], -start[Y], -start[Z]});
		ArrayList<Triangle> sr = new ArrayList<>();
		looseOctTreeRayIntersectShortCircuit(startRay, this.root, sr);
		int startID = sr.get(0).id;
		float[] initialPt = sr.get(0).getIntersection();
		
		Ray endRay = new Ray(end, new float[] {-end[X], -end[Y], -end[Z]});
		ArrayList<Triangle> er = new ArrayList<>();
		looseOctTreeRayIntersectShortCircuit(endRay, this.root, er);
		float[] finalPt = er.get(0).getIntersection();
		int endID = er.get(0).id;
		
		Triangle t = new Triangle(new float[][] {start, end, new float[]{0f, 0f, 0f}});
		ArrayList <float[]> list = new ArrayList<>();
		looseOctTreeTriangleIntersectShortCircuit(t, getRoot(), list);
		if (list.size() < 1) {
			// bit of a hack to ensure accurate distance based where the start and end are on the same facet 
			if (startID == endID) {
				return VectorUtil.distVec3(new float[] {(float)startPt.x * scalar, (float)startPt.y * scalar, (float)startPt.z * scalar},
						new float[] {(float)endPt.x * scalar, (float)endPt.y * scalar, (float)endPt.z * scalar});
			}
			return Float.NEGATIVE_INFINITY;
		}
		
		float[][] points = new float[list.size()][];
			
		int k = 0;
		for (float[] p : list) {
			points[k++] = p;
		}	
		
		int [] sorted = sort3DCCW (initialPt, finalPt, points);

		float line[][] = new float[points.length][];
		
		for (k=0; k<sorted.length; k++) {
			line[k] = points[sorted[k]];
		}
		
		float[][] editPts = OctTree.removeAdjacentDuplicatePoints(line);
		
		if (ThreeDManager.getInstance().getShapeModel().isUnitSphere()) {
			for (int i=0; i<editPts.length; i++) {
				editPts[i][0] *= scalar;
				editPts[i][1] *= scalar;
				editPts[i][2] *= scalar;
			}
		}
		
		float distance = 0f;
		for (int j=0; j<editPts.length-1; j++) {
			distance += VectorUtil.distVec3(editPts[j], 
					editPts[j+1]);			
		}
		
		return distance;
	}
	
	/**
	 * Method to sort 3D (x,y,z) points that are all in a plane
	 * @param startPt
	 * @param endPt
	 * @param points
	 * @return
	 */
	public static int[] sort3DCCW (float[] startPt, float[] endPt, float[][] points) {
		int[] sorted = new int[points.length];
		
		HVector start = new HVector(startPt[X], startPt[Y], startPt[Z]);
		HVector end = new HVector(endPt[X], endPt[Y], endPt[Z]);
		HVector norm = start.cross(end);
		HVector tmp = new HVector();
		
		ArrayList<Vertex3D> list = new ArrayList<>();
		
		int idx = 0;
		for (float[] pt : points) {
			tmp.set(pt[X], pt[Y], pt[Z]);
			double ang = start.separationPlanar(tmp, norm);
			list.add(new Vertex3D(ang, idx++));
		}
		
		Collections.sort(list, new Vertex3DAngleComparator());
		int i = 0;
		for (Vertex3D v : list) {
			sorted[i++] = v.index;
		}
				
		return sorted;
	}
	
	/*              
	 *  Triangle-Triangle Overlap Test Routines             
	 *  July, 2002                                                          
	 *  Updated December 2003                                                
	 *                                                                       
	 *  This file contains an implementation of algorithms for                
	 *  performing two and three-dimensional triangle-triangle intersection test 
	 *  The algorithms and underlying theory are described in                    
	 *                                                                           
	 * "Fast and Robust Triangle-Triangle Overlap Test 
	 *  Using Orientation Predicates"  P. Guigue - O. Devillers
	 *                                                 
	 *  Journal of Graphics Tools, 8(1), 2003                                    
	 *                                                                           
	 *  Several geometric predicates are defined.  Their parameters are all      
	 *  points.  Each point is an array of two or three real precision         
	 *  floating point numbers. The geometric predicates implemented in          
	 *  this file are:                                                            
	 *                                                                           
	 *    int tri_tri_overlap_test_3d(p1,q1,r1,p2,q2,r2)                         
	 *    int tri_tri_overlap_test_2d(p1,q1,r1,p2,q2,r2)                         
	 *                                                                           
	 *    int tri_tri_intersection_test_3d(p1,q1,r1,p2,q2,r2,
	 *                                     coplanar,source,target)               
	 *                                                                           
	 *       is a version that computes the segment of intersection when            
	 *       the triangles overlap (and are not coplanar)                        
	 *                                                                           
	 *    each function returns 1 if the triangles (including their              
	 *    boundary) intersect, otherwise 0                                       
	 *                                                                           
	 *                                                                           
	 *  Other information are available from the Web page                        
	 *  http:<i>//www.acm.org/jgt/papers/GuigueDevillers03/                         
	 *  or from 
	 *  http://home.arcor.de/philippe.guigue/triangle_triangle_intersection.htm                                                                         
	 */

	// modified by Aaron to better detect coplanarity

	boolean ZERO_TEST(float x)  {
		if (Float.compare(x, 0f) == 0) {
			return true;
		}
		return false;
	}
	//#define ZERO_TEST(x)  ((x) > -0.001 && (x) < .001)

//	#include "stdio.h"

	/* function prototype */

//	int tri_tri_overlap_test_3d(real p1[3], real q1[3], real r1[3], 
//	                            real p2[3], real q2[3], real r2[3]);
//
//
//	int coplanar_tri_tri3d(real  p1[3], real  q1[3], real  r1[3],
//	                       real  p2[3], real  q2[3], real  r2[3],
//	                       real  N1[3], real  N2[3]);
//
//
//	int tri_tri_overlap_test_2d(real p1[2], real q1[2], real r1[2], 
//	                            real p2[2], real q2[2], real r2[2]);
//
//
//	int tri_tri_intersection_test_3d(real p1[3], real q1[3], real r1[3], 
//	                                 real p2[3], real q2[3], real r2[3],
//	                                 int * coplanar, 
//	                                 real source[3],real target[3]);

	/* coplanar returns whether the triangles are coplanar  
	 *  source and target are the endpoints of the segment of 
	 *  intersection if it exists) 
	 */


	/* some 3D macros */

	void CROSS(float[] dest, float[] v1, float[] v2) {
		dest[0]=v1[1]*v2[2]-v1[2]*v2[1]; 
		dest[1]=v1[2]*v2[0]-v1[0]*v2[2]; 
		dest[2]=v1[0]*v2[1]-v1[1]*v2[0];
	}

	float DOT(float[] v1, float[] v2) {
		return v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2];
	}

	void SUB(float[] dest, float[] v1, float[] v2) {
		dest[0]=v1[0]-v2[0];
		dest[1]=v1[1]-v2[1]; 
		dest[2]=v1[2]-v2[2]; 
	}

	void SCALAR(float[] dest, float alpha, float[] v) {
		dest[0] = alpha * v[0]; 
		dest[1] = alpha * v[1]; 
		dest[2] = alpha * v[2];
	}

	int CHECK_MIN_MAX(float[] p1, float[] q1, float[] r1, float[] p2, float[] q2, float[] r2) {
		float[] v1 = new float[3];
		float[] v2 = new float[3];
		float[] N1 = new float[3];
		
		SUB(v1,p2,q1);
		SUB(v2,p1,q1);
		CROSS(N1,v1,v2);
		SUB(v1,q2,q1);
		if (DOT(v1,N1) > 0.0f) {
			return 0;
		}
		SUB(v1,p2,p1);
		SUB(v2,r1,p1);
		CROSS(N1,v1,v2);
		SUB(v1,r2,p1) ;
		if (DOT(v1,N1) > 0.0f) {
			return 0;
		} else {
			return 1; 
		}
	}



	/* Permutation in a canonical form of T2's vertices */

	int TRI_TRI_3D(float[] p1, float[] q1, float[] r1, float[] p2, float[] q2, float[] r2, float dp2, float dq2, float dr2, float[] N1, float[] N2) { 
		if (dp2 > 0.0f) { 
			if (dq2 > 0.0f) return CHECK_MIN_MAX(p1,r1,q1,r2,p2,q2);
			else if (dr2 > 0.0f) return CHECK_MIN_MAX(p1,r1,q1,q2,r2,p2);
			else return CHECK_MIN_MAX(p1,q1,r1,p2,q2,r2); 
		} else if (dp2 < 0.0f) { 
			if (dq2 < 0.0f) return CHECK_MIN_MAX(p1,q1,r1,r2,p2,q2);
			else if (dr2 < 0.0f) return CHECK_MIN_MAX(p1,q1,r1,q2,r2,p2);
			else return CHECK_MIN_MAX(p1,r1,q1,p2,q2,r2);
		} else { 
			if (dq2 < 0.0f) { 
				if (dr2 >= 0.0f) return CHECK_MIN_MAX(p1,r1,q1,q2,r2,p2);
				else return CHECK_MIN_MAX(p1,q1,r1,p2,q2,r2);
			} else if (dq2 > 0.0f) { 
				if (dr2 > 0.0f) return CHECK_MIN_MAX(p1,r1,q1,p2,q2,r2);
				else  return CHECK_MIN_MAX(p1,q1,r1,q2,r2,p2);
			} else  { 
				if (dr2 > 0.0f) return CHECK_MIN_MAX(p1,q1,r1,r2,p2,q2);
				else if (dr2 < 0.0f) return CHECK_MIN_MAX(p1,r1,q1,r2,p2,q2);
				else return coplanar_tri_tri3d(p1,q1,r1,p2,q2,r2,N1,N2);
			}
		}
	}



	/*
	 *
	 *  Three-dimensional Triangle-Triangle Overlap Test
	 *
	 */


	public int tri_tri_overlap_test_3d(float[] p1, float[] q1, float[] r1, 
	                            float[] p2, float[] q2, float[] r2, float[] intersect1, float[] intersect2) {
		
	    float dp1, dq1, dr1, dp2, dq2, dr2;
	    float[] v1 = new float[3];
	    float[] v2 = new float[3];
	    float[] N1 = new float[3];
	    float[] N2 = new float[3]; 

	    /* Compute distance signs  of p1, q1 and r1 to the plane of
	     triangle(p2,q2,r2) */


	    SUB(v1,p2,r2);
	    SUB(v2,q2,r2);
	    CROSS(N2,v1,v2);

	    SUB(v1,p1,r2);
	    dp1 = DOT(v1,N2);
	    SUB(v1,q1,r2);
	    dq1 = DOT(v1,N2);
	    SUB(v1,r1,r2);
	    dr1 = DOT(v1,N2);

	    if (((dp1 * dq1) > 0.0f) && ((dp1 * dr1) > 0.0f))  return 0; 

	    /* Compute distance signs  of p2, q2 and r2 to the plane of
	     triangle(p1,q1,r1) */


	    SUB(v1,q1,p1);
	    SUB(v2,r1,p1);
	    CROSS(N1,v1,v2);

	    SUB(v1,p2,r1);
	    dp2 = DOT(v1,N1);
	    SUB(v1,q2,r1);
	    dq2 = DOT(v1,N1); 
	    SUB(v1,r2,r1);
	    dr2 = DOT(v1,N1);

	    if (((dp2 * dq2) > 0.0f) && ((dp2 * dr2) > 0.0f)) return 0;

	    /* Permutation in a canonical form of T1's vertices */




	    if (dp1 > 0.0f) {
	        if (dq1 > 0.0f) return TRI_TRI_3D(r1,p1,q1,p2,r2,q2,dp2,dr2,dq2,N1,N2);
	        else if (dr1 > 0.0f) return TRI_TRI_3D(q1,r1,p1,p2,r2,q2,dp2,dr2,dq2,N1,N2);  
	        else return TRI_TRI_3D(p1,q1,r1,p2,q2,r2,dp2,dq2,dr2,N1,N2);
	    } else if (dp1 < 0.0f) {
	        if (dq1 < 0.0f) return TRI_TRI_3D(r1,p1,q1,p2,q2,r2,dp2,dq2,dr2,N1,N2);
	        else if (dr1 < 0.0f) return TRI_TRI_3D(q1,r1,p1,p2,q2,r2,dp2,dq2,dr2,N1,N2);
	        else return TRI_TRI_3D(p1,q1,r1,p2,r2,q2,dp2,dr2,dq2,N1,N2);
	    } else {
	        if (dq1 < 0.0f) {
	        	if (dr1 >= 0.0f) return TRI_TRI_3D(q1,r1,p1,p2,r2,q2,dp2,dr2,dq2,N1,N2);
	            else return TRI_TRI_3D(p1,q1,r1,p2,q2,r2,dp2,dq2,dr2,N1,N2);
	        } else if (dq1 > 0.0f) {
	        	if (dr1 > 0.0f) return TRI_TRI_3D(p1,q1,r1,p2,r2,q2,dp2,dr2,dq2,N1,N2);
	            else return TRI_TRI_3D(q1,r1,p1,p2,q2,r2,dp2,dq2,dr2,N1,N2);
	        } else  {
	        	if (dr1 > 0.0f) return TRI_TRI_3D(r1,p1,q1,p2,q2,r2,dp2,dq2,dr2,N1,N2);
	            else if (dr1 < 0.0f) return TRI_TRI_3D(r1,p1,q1,p2,r2,q2,dp2,dr2,dq2,N1,N2);
	            else return coplanar_tri_tri3d(p1,q1,r1,p2,q2,r2,N1,N2);
	        }
	    }
	}



	public int coplanar_tri_tri3d(float[] p1, float[] q1, float[] r1,
			float[] p2, float[] q2, float[] r2,
			float[] normal_1, float[] normal_2) {

		float[] P1 = new float[2];
		float[] Q1 = new float[2];
		float[] R1 = new float[2];
		float[] P2 = new float[2];
		float[] Q2 = new float[2];
		float[] R2 = new float[2];

	    float n_x, n_y, n_z;

	    n_x = ((normal_1[0]<0)?-normal_1[0]:normal_1[0]);
	    n_y = ((normal_1[1]<0)?-normal_1[1]:normal_1[1]);
	    n_z = ((normal_1[2]<0)?-normal_1[2]:normal_1[2]);


	    /* Projection of the triangles in 3D onto 2D such that the area of
	     the projection is maximized. */


	    if (( n_x > n_z ) && ( n_x >= n_y )) {
	        // Project onto plane YZ

	        P1[0] = q1[2]; P1[1] = q1[1];
	        Q1[0] = p1[2]; Q1[1] = p1[1];
	        R1[0] = r1[2]; R1[1] = r1[1]; 

	        P2[0] = q2[2]; P2[1] = q2[1];
	        Q2[0] = p2[2]; Q2[1] = p2[1];
	        R2[0] = r2[2]; R2[1] = r2[1]; 

	    } else if (( n_y > n_z ) && ( n_y >= n_x )) {
	        // Project onto plane XZ

	        P1[0] = q1[0]; P1[1] = q1[2];
	        Q1[0] = p1[0]; Q1[1] = p1[2];
	        R1[0] = r1[0]; R1[1] = r1[2]; 

	        P2[0] = q2[0]; P2[1] = q2[2];
	        Q2[0] = p2[0]; Q2[1] = p2[2];
	        R2[0] = r2[0]; R2[1] = r2[2]; 

	    } else {
	        // Project onto plane XY

	        P1[0] = p1[0]; P1[1] = p1[1]; 
	        Q1[0] = q1[0]; Q1[1] = q1[1]; 
	        R1[0] = r1[0]; R1[1] = r1[1]; 

	        P2[0] = p2[0]; P2[1] = p2[1]; 
	        Q2[0] = q2[0]; Q2[1] = q2[1]; 
	        R2[0] = r2[0]; R2[1] = r2[1]; 
	    }

	    return tri_tri_overlap_test_2d(P1,Q1,R1,P2,Q2,R2);

	}



	/*
	 *                                                                
	 *  Three-dimensional Triangle-Triangle Intersection              
	 *
	 */

	/*
	 This macro is called when the triangles surely intersect
	 It constructs the segment of intersection of the two triangles
	 if they are not coplanar.
	 */

	int CONSTRUCT_INTERSECTION(float[] p1, float[] q1, float[] r1, float[] p2, float[] q2, float[] r2, float[] source, float[] target, float[] N1, float[] N2) { 
		float alpha;
		float[] N = new float[3];
		float[] v = new float[3];
		float[] v1 = new float[3];
		float[] v2 = new float[3];

		SUB(v1,q1,p1);
		SUB(v2,r2,p1); 
		CROSS(N,v1,v2); 
		SUB(v,p2,p1);
		if (DOT(v,N) > 0.0f) {
			SUB(v1,r1,p1); 
			CROSS(N,v1,v2);
			if (DOT(v,N) <= 0.0f) {
				SUB(v2,q2,p1);
				CROSS(N,v1,v2);
				if (DOT(v,N) > 0.0f) {
					SUB(v1,p1,p2);
					SUB(v2,p1,r1);
					alpha = DOT(v1,N2) / DOT(v2,N2);
					SCALAR(v1,alpha,v2);
					SUB(source,p1,v1);
					SUB(v1,p2,p1);
					SUB(v2,p2,r2);
					alpha = DOT(v1,N1) / DOT(v2,N1);
					SCALAR(v1,alpha,v2);
					SUB(target,p2,v1);
					return 1;
				} else {
					SUB(v1,p2,p1);
					SUB(v2,p2,q2);
					alpha = DOT(v1,N1) / DOT(v2,N1);
					SCALAR(v1,alpha,v2);
					SUB(source,p2,v1);
					SUB(v1,p2,p1);
					SUB(v2,p2,r2);
					alpha = DOT(v1,N1) / DOT(v2,N1);
					SCALAR(v1,alpha,v2);
					SUB(target,p2,v1);
					return 1;
				}
			} else {
				return 0;
			}
		} else {
			SUB(v2,q2,p1);
			CROSS(N,v1,v2);
			if (DOT(v,N) < 0.0f) {
				return 0;
			} else {
				SUB(v1,r1,p1);
				CROSS(N,v1,v2);
				if (DOT(v,N) >= 0.0f) {
					SUB(v1,p1,p2);
					SUB(v2,p1,r1);
					alpha = DOT(v1,N2) / DOT(v2,N2);
					SCALAR(v1,alpha,v2);
					SUB(source,p1,v1);
					SUB(v1,p1,p2);
					SUB(v2,p1,q1);
					alpha = DOT(v1,N2) / DOT(v2,N2);
					SCALAR(v1,alpha,v2);
					SUB(target,p1,v1);
					return 1;
				} else {
					SUB(v1,p2,p1);
					SUB(v2,p2,q2);
					alpha = DOT(v1,N1) / DOT(v2,N1);
					SCALAR(v1,alpha,v2);
					SUB(source,p2,v1);
					SUB(v1,p1,p2);
					SUB(v2,p1,q1);
					alpha = DOT(v1,N2) / DOT(v2,N2);
					SCALAR(v1,alpha,v2);
					SUB(target,p1,v1);
					return 1;
				}
			}
		}
	} 



	int TRI_TRI_INTER_3D(float[] p1,float[] q1,float[] r1, float[] p2, float[] q2, float[] r2, float dp2, float dq2, float dr2, Integer coplanar,
			float[] N1, float[] N2, float[] source, float[] target) {
		if (dp2 > 0.0f) {
			if (dq2 > 0.0f) return CONSTRUCT_INTERSECTION(p1,r1,q1,r2,p2,q2, source, target, N1, N2);
			else if (dr2 > 0.0f) return CONSTRUCT_INTERSECTION(p1,r1,q1,q2,r2,p2, source, target, N1, N2);
			else return CONSTRUCT_INTERSECTION(p1,q1,r1,p2,q2,r2, source, target, N1, N2); 
		}
		else if (dp2 < 0.0f) {
			if (dq2 < 0.0f) return CONSTRUCT_INTERSECTION(p1,q1,r1,r2,p2,q2, source, target, N1, N2);
			else if (dr2 < 0.0f) return CONSTRUCT_INTERSECTION(p1,q1,r1,q2,r2,p2, source, target, N1, N2);
			else return CONSTRUCT_INTERSECTION(p1,r1,q1,p2,q2,r2, source, target, N1, N2);
		} else {
			if (dq2 < 0.0f) {
				if (dr2 >= 0.0f) return CONSTRUCT_INTERSECTION(p1,r1,q1,q2,r2,p2, source, target, N1, N2);
				else return CONSTRUCT_INTERSECTION(p1,q1,r1,p2,q2,r2, source, target, N1, N2);
			}
			else if (dq2 > 0.0f) {
				if (dr2 > 0.0f) return CONSTRUCT_INTERSECTION(p1,r1,q1,p2,q2,r2, source, target, N1, N2);
				else  return CONSTRUCT_INTERSECTION(p1,q1,r1,q2,r2,p2, source, target, N1, N2);
			}
			else  {
				if (dr2 > 0.0f) return CONSTRUCT_INTERSECTION(p1,q1,r1,r2,p2,q2, source, target, N1, N2);
				else if (dr2 < 0.0f) return CONSTRUCT_INTERSECTION(p1,r1,q1,r2,p2,q2, source, target, N1, N2);
				else {
					coplanar = 1;
					return coplanar_tri_tri3d(p1,q1,r1,p2,q2,r2,N1,N2);
				}
			}
		} 
	}


	/**
	 * The following version computes the segment of intersection of the
	 * two triangles if it exists. 
	 * coplanar returns whether the triangles are coplanar
	 * source and target are the end points of the line segment of intersection 
	 * @param p1 Triangle 1 first vertex
	 * @param q1 Triangle 1 second vertex
	 * @param r1 Triangle 1 third vertex
	 * @param p2 Triangle 2 first vertex
	 * @param q2 Triangle 2 second vertex
	 * @param r2 Triangle 2 third vertex
	 * @param coplanar 0 if not coplanar, 1 if coplanar
	 * @param source starting end point of the line segment of intersection
	 * @param target ending point of the line segment of intersection
	 * @return true if there is an intersection
	 */
	public int tri_tri_intersection_test_3d(float[] p1, float[] q1, float[] r1, 
			float[]  p2, float[]  q2, float[]  r2, Integer coplanar, float[]  source, float[] target) {
		
	    float dp1, dq1, dr1, dp2, dq2, dr2;
	    float[] v1 = new float[3];
	    float[] v2 = new float[3];
	    float[] N1 = new float[3];
	    float[] N2 = new float[3]; 

	    // Compute distance signs  of p1, q1 and r1 
	    // to the plane of triangle(p2,q2,r2)


	    SUB(v1,p2,r2);
	    SUB(v2,q2,r2);
	    CROSS(N2,v1,v2);

	    SUB(v1,p1,r2);
	    dp1 = DOT(v1,N2);
	    SUB(v1,q1,r2);
	    dq1 = DOT(v1,N2);
	    SUB(v1,r1,r2);
	    dr1 = DOT(v1,N2);

	    if (((dp1 * dq1) > 0.0f) && ((dp1 * dr1) > 0.0f))  return 0; 

	    // Compute distance signs  of p2, q2 and r2 
	    // to the plane of triangle(p1,q1,r1)


	    SUB(v1,q1,p1);
	    SUB(v2,r1,p1);
	    CROSS(N1,v1,v2);

	    SUB(v1,p2,r1);
	    dp2 = DOT(v1,N1);
	    SUB(v1,q2,r1);
	    dq2 = DOT(v1,N1);
	    SUB(v1,r2,r1);
	    dr2 = DOT(v1,N1);

	    if (((dp2 * dq2) > 0.0f) && ((dp2 * dr2) > 0.0f)) return 0;

	    // Permutation in a canonical form of T1's vertices


	    //  printf("d1 = [%f %f %f], d2 = [%f %f %f]\n", dp1, dq1, dr1, dp2, dq2, dr2);
	    /*
	     // added by Aaron
	     if (ZERO_TEST(dp1) || ZERO_TEST(dq1) ||ZERO_TEST(dr1) ||ZERO_TEST(dp2) ||ZERO_TEST(dq2) ||ZERO_TEST(dr2))
	     {
	     coplanar = 1;
	     return 0;
	     }
	     */


	    if (dp1 > 0.0f) {
	        if (dq1 > 0.0f) return TRI_TRI_INTER_3D(r1,p1,q1,p2,r2,q2,dp2,dr2,dq2, coplanar, N1, N2, source, target);
	        else if (dr1 > 0.0f) return TRI_TRI_INTER_3D(q1,r1,p1,p2,r2,q2,dp2,dr2,dq2, coplanar, N1, N2, source, target);
	        else return TRI_TRI_INTER_3D(p1,q1,r1,p2,q2,r2,dp2,dq2,dr2, coplanar, N1, N2, source, target);
	    } else if (dp1 < 0.0f) {
	    	if (dq1 < 0.0f) return TRI_TRI_INTER_3D(r1,p1,q1,p2,q2,r2,dp2,dq2,dr2, coplanar, N1, N2, source, target);
	        else if (dr1 < 0.0f) return TRI_TRI_INTER_3D(q1,r1,p1,p2,q2,r2,dp2,dq2,dr2, coplanar, N1, N2, source, target);
	        else return TRI_TRI_INTER_3D(p1,q1,r1,p2,r2,q2,dp2,dr2,dq2, coplanar, N1, N2, source, target);
	    } else {
	        if (dq1 < 0.0f) {
	        	if (dr1 >= 0.0f) return TRI_TRI_INTER_3D(q1,r1,p1,p2,r2,q2,dp2,dr2,dq2, coplanar, N1, N2, source, target);
	        	else return TRI_TRI_INTER_3D(p1,q1,r1,p2,q2,r2,dp2,dq2,dr2, coplanar, N1, N2, source, target);
	        }
	        else if (dq1 > 0.0f) {
	        	if (dr1 > 0.0f) return TRI_TRI_INTER_3D(p1,q1,r1,p2,r2,q2,dp2,dr2,dq2, coplanar, N1, N2, source, target);
	            else return TRI_TRI_INTER_3D(q1,r1,p1,p2,q2,r2,dp2,dq2,dr2, coplanar, N1, N2, source, target);
	        }
	        else  {
	        	if (dr1 > 0.0f) return TRI_TRI_INTER_3D(r1,p1,q1,p2,q2,r2,dp2,dq2,dr2, coplanar, N1, N2, source, target);
	            else if (dr1 < 0.0f) return TRI_TRI_INTER_3D(r1,p1,q1,p2,r2,q2,dp2,dr2,dq2, coplanar, N1, N2, source, target);
	            else {
	            	// triangles are co-planar
	               coplanar = 1;
	               return coplanar_tri_tri3d(p1,q1,r1,p2,q2,r2,N1,N2); 
	            }
	        }
	    }
	}





	/*
	 *
	 *  Two dimensional Triangle-Triangle Overlap Test    
	 *
	 */


	/* some 2D macros */
	
	float ORIENT_2D(float[] a, float[] b, float[] c) {
		return ((a[0]-c[0])*(b[1]-c[1])-(a[1]-c[1])*(b[0]-c[0]));
	}


	int INTERSECTION_TEST_VERTEXA(float[] P1, float[] Q1, float[] R1, float[] P2, float[] Q2, float[] R2) {
		if (ORIENT_2D(R2, P2, Q1) >= 0.0f)
			if (ORIENT_2D(R2, Q2, Q1) <= 0.0f)
				if (ORIENT_2D(P1, P2, Q1) > 0.0f) {
					if (ORIENT_2D(P1, Q2, Q1) <= 0.0f)
						return 1;
					else
						return 0;
				} else {
					if (ORIENT_2D(P1, P2, R1) >= 0.0f)
						if (ORIENT_2D(Q1, R1, P2) >= 0.0f)
							return 1;
						else
							return 0;
					else
						return 0;
				}
			else if (ORIENT_2D(P1, Q2, Q1) <= 0.0f)
				if (ORIENT_2D(R2, Q2, R1) <= 0.0f)
					if (ORIENT_2D(Q1, R1, Q2) >= 0.0f)
						return 1;
					else
						return 0;
				else
					return 0;
			else
				return 0;
		else if (ORIENT_2D(R2, P2, R1) >= 0.0f)
			if (ORIENT_2D(Q1, R1, R2) >= 0.0f)
				if (ORIENT_2D(P1, P2, R1) >= 0.0f)
					return 1;
				else
					return 0;
			else if (ORIENT_2D(Q1, R1, Q2) >= 0.0f) {
				if (ORIENT_2D(R2, R1, Q2) >= 0.0f)
					return 1;
				else
					return 0;
			} else
				return 0;
		else
			return 0;
	}

	int INTERSECTION_TEST_VERTEX(float[] P1, float[] Q1, float[] R1, float[] P2, float[] Q2, float[] R2) {
		if (ORIENT_2D(R2, P2, Q1) >= 0.0f)
			if (ORIENT_2D(R2, Q2, Q1) <= 0.0f)
				if (ORIENT_2D(P1, P2, Q1) > 0.0f) {
					if (ORIENT_2D(P1, Q2, Q1) <= 0.0f)
						return 1;
					else
						return 0;
				} else {
					if (ORIENT_2D(P1, P2, R1) >= 0.0f)
						if (ORIENT_2D(Q1, R1, P2) >= 0.0f)
							return 1;
						else
							return 0;
					else
						return 0;
				}
			else if (ORIENT_2D(P1, Q2, Q1) <= 0.0f)
				if (ORIENT_2D(R2, Q2, R1) <= 0.0f)
					if (ORIENT_2D(Q1, R1, Q2) >= 0.0f)
						return 1;
					else
						return 0;
				else
					return 0;
			else
				return 0;
		else if (ORIENT_2D(R2, P2, R1) >= 0.0f)
			if (ORIENT_2D(Q1, R1, R2) >= 0.0f)
				if (ORIENT_2D(P1, P2, R1) >= 0.0f)
					return 1;
				else
					return 0;
			else if (ORIENT_2D(Q1, R1, Q2) >= 0.0f) {
				if (ORIENT_2D(R2, R1, Q2) >= 0.0f)
					return 1;
				else
					return 0;
			} else
				return 0;
		else
			return 0;
	}

	int INTERSECTION_TEST_EDGE(float[] P1, float[] Q1, float[] R1, float[] P2, float[] Q2, float[] R2) {
		if (ORIENT_2D(R2,P2,Q1) >= 0.0f) {
			if (ORIENT_2D(P1,P2,Q1) >= 0.0f) { 
				if (ORIENT_2D(P1,Q1,R2) >= 0.0f) 
					return 1;
				else 
					return 0;
			} else { 
				if (ORIENT_2D(Q1,R1,P2) >= 0.0f){ 
					if (ORIENT_2D(R1,P1,P2) >= 0.0f) 
						return 1; 
					else 
						return 0;
				} 
				else 
					return 0; 
			}
		} else {
			if (ORIENT_2D(R2,P2,R1) >= 0.0f) {
				if (ORIENT_2D(P1,P2,R1) >= 0.0f) {
					if (ORIENT_2D(P1,R1,R2) >= 0.0f) 
						return 1;
					else {
						if (ORIENT_2D(Q1,R1,R2) >= 0.0f) 
							return 1; 
						else 
							return 0;
					}
				}
				else  
					return 0; 
			}
			else 
				return 0; 
		}
	}


	public int ccw_tri_tri_intersection_2d(float[] p1, float[] q1, float[] r1, 
			float[] p2, float[] q2, float[] r2) {
	    if ( ORIENT_2D(p2,q2,p1) >= 0.0f ) {
	        if ( ORIENT_2D(q2,r2,p1) >= 0.0f ) {
	            if ( ORIENT_2D(r2,p2,p1) >= 0.0f ) 
	            	return 1;
	            else 
	            	return INTERSECTION_TEST_EDGE(p1,q1,r1,p2,q2,r2);
	        } else {  
	        	if ( ORIENT_2D(r2,p2,p1) >= 0.0f ) 
	        		return INTERSECTION_TEST_EDGE(p1,q1,r1,r2,p2,q2);
	        	else 
	        		return INTERSECTION_TEST_VERTEX(p1,q1,r1,p2,q2,r2);
	        }
	    } else {
	        if ( ORIENT_2D(q2,r2,p1) >= 0.0f ) {
	            if ( ORIENT_2D(r2,p2,p1) >= 0.0f ) 
	                return INTERSECTION_TEST_EDGE(p1,q1,r1,q2,r2,p2);
	            else  
	            	return INTERSECTION_TEST_VERTEX(p1,q1,r1,q2,r2,p2);
	        }
	        else 
	        	return INTERSECTION_TEST_VERTEX(p1,q1,r1,r2,p2,q2);
	    }
	}


	public int tri_tri_overlap_test_2d(float[]  p1, float[] q1, float[] r1, 
			float[] p2, float[] q2, float[] r2) { 
	    if ( ORIENT_2D(p1,q1,r1) < 0.0f )
	        if ( ORIENT_2D(p2,q2,r2) < 0.0f )
	            return ccw_tri_tri_intersection_2d(p1,r1,q1,p2,r2,q2);
	        else
	            return ccw_tri_tri_intersection_2d(p1,r1,q1,p2,q2,r2);
	    else if ( ORIENT_2D(p2,q2,r2) < 0.0f )
	    	return ccw_tri_tri_intersection_2d(p1,q1,r1,p2,r2,q2);
	    else
	    	return ccw_tri_tri_intersection_2d(p1,q1,r1,p2,q2,r2);

	}
	/*  END OF            
	 *  Triangle-Triangle Overlap Test Routines             
	 */
	
	/*              
	 *  Triangle-Triangle Overlap Test Routines             
	 *  July, 2002                                                          
	 *  Updated December 2003                                                
	 *                                                                       
	 *  This file contains an implementation of algorithms for                
	 *  performing two and three-dimensional triangle-triangle intersection test 
	 *  The algorithms and underlying theory are described in                    
	 *                                                                           
	 * "Fast and Robust Triangle-Triangle Overlap Test 
	 *  Using Orientation Predicates"  P. Guigue - O. Devillers
	 *                                                 
	 *  Journal of Graphics Tools, 8(1), 2003                                    
	 *                                                                           
	 *  Several geometric predicates are defined.  Their parameters are all      
	 *  points.  Each point is an array of two or three real precision         
	 *  floating point numbers. The geometric predicates implemented in          
	 *  this file are:                                                            
	 *                                                                           
	 *    int tri_tri_overlap_test_3d(p1,q1,r1,p2,q2,r2)                         
	 *    int tri_tri_overlap_test_2d(p1,q1,r1,p2,q2,r2)                         
	 *                                                                           
	 *    int tri_tri_intersection_test_3d(p1,q1,r1,p2,q2,r2,
	 *                                     coplanar,source,target)               
	 *                                                                           
	 *       is a version that computes the segment of intersection when            
	 *       the triangles overlap (and are not coplanar)                        
	 *                                                                           
	 *    each function returns 1 if the triangles (including their              
	 *    boundary) intersect, otherwise 0                                       
	 *                                                                           
	 *                                                                           
	 *  Other information are available from the Web page                        
	 *  http:<i>//www.acm.org/jgt/papers/GuigueDevillers03/                         
	 *  or from 
	 *  http://home.arcor.de/philippe.guigue/triangle_triangle_intersection.htm                                                                         
	 */

	// modified by Aaron to better detect coplanarity

	boolean ZERO_TEST(double x)  {
		if (Double.compare(x, 0f) == 0) {
			return true;
		}
		return false;
	}
	//#define ZERO_TEST(x)  ((x) > -0.001 && (x) < .001)

//	#include "stdio.h"

	/* function prototype */

//	int tri_tri_overlap_test_3d(real p1[3], real q1[3], real r1[3], 
//	                            real p2[3], real q2[3], real r2[3]);
//
//
//	int coplanar_tri_tri3d(real  p1[3], real  q1[3], real  r1[3],
//	                       real  p2[3], real  q2[3], real  r2[3],
//	                       real  N1[3], real  N2[3]);
//
//
//	int tri_tri_overlap_test_2d(real p1[2], real q1[2], real r1[2], 
//	                            real p2[2], real q2[2], real r2[2]);
//
//
//	int tri_tri_intersection_test_3d(real p1[3], real q1[3], real r1[3], 
//	                                 real p2[3], real q2[3], real r2[3],
//	                                 int * coplanar, 
//	                                 real source[3],real target[3]);

	/* coplanar returns whether the triangles are coplanar  
	 *  source and target are the endpoints of the segment of 
	 *  intersection if it exists) 
	 */


	/* some 3D macros */

	void CROSS(double[] dest, double[] v1, double[] v2) {
		dest[0]=v1[1]*v2[2]-v1[2]*v2[1]; 
		dest[1]=v1[2]*v2[0]-v1[0]*v2[2]; 
		dest[2]=v1[0]*v2[1]-v1[1]*v2[0];
	}

	double DOT(double[] v1, double[] v2) {
		return v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2];
	}

	void SUB(double[] dest, double[] v1, double[] v2) {
		dest[0]=v1[0]-v2[0];
		dest[1]=v1[1]-v2[1]; 
		dest[2]=v1[2]-v2[2]; 
	}

	void SCALAR(double[] dest, double alpha, double[] v) {
		dest[0] = alpha * v[0]; 
		dest[1] = alpha * v[1]; 
		dest[2] = alpha * v[2];
	}

	int CHECK_MIN_MAX(double[] p1, double[] q1, double[] r1, double[] p2, double[] q2, double[] r2) {
		double[] v1 = new double[3];
		double[] v2 = new double[3];
		double[] N1 = new double[3];
		
		SUB(v1,p2,q1);
		SUB(v2,p1,q1);
		CROSS(N1,v1,v2);
		SUB(v1,q2,q1);
		if (DOT(v1,N1) > 0.0f) {
			return 0;
		}
		SUB(v1,p2,p1);
		SUB(v2,r1,p1);
		CROSS(N1,v1,v2);
		SUB(v1,r2,p1) ;
		if (DOT(v1,N1) > 0.0f) {
			return 0;
		} else {
			return 1; 
		}
	}



	/* Permutation in a canonical form of T2's vertices */

	int TRI_TRI_3D(double[] p1, double[] q1, double[] r1, double[] p2, double[] q2, double[] r2, double dp2, double dq2, double dr2, double[] N1, double[] N2) { 
		if (dp2 > 0.0f) { 
			if (dq2 > 0.0f) return CHECK_MIN_MAX(p1,r1,q1,r2,p2,q2);
			else if (dr2 > 0.0f) return CHECK_MIN_MAX(p1,r1,q1,q2,r2,p2);
			else return CHECK_MIN_MAX(p1,q1,r1,p2,q2,r2); 
		} else if (dp2 < 0.0f) { 
			if (dq2 < 0.0f) return CHECK_MIN_MAX(p1,q1,r1,r2,p2,q2);
			else if (dr2 < 0.0f) return CHECK_MIN_MAX(p1,q1,r1,q2,r2,p2);
			else return CHECK_MIN_MAX(p1,r1,q1,p2,q2,r2);
		} else { 
			if (dq2 < 0.0f) { 
				if (dr2 >= 0.0f) return CHECK_MIN_MAX(p1,r1,q1,q2,r2,p2);
				else return CHECK_MIN_MAX(p1,q1,r1,p2,q2,r2);
			} else if (dq2 > 0.0f) { 
				if (dr2 > 0.0f) return CHECK_MIN_MAX(p1,r1,q1,p2,q2,r2);
				else  return CHECK_MIN_MAX(p1,q1,r1,q2,r2,p2);
			} else  { 
				if (dr2 > 0.0f) return CHECK_MIN_MAX(p1,q1,r1,r2,p2,q2);
				else if (dr2 < 0.0f) return CHECK_MIN_MAX(p1,r1,q1,r2,p2,q2);
				else return coplanar_tri_tri3d(p1,q1,r1,p2,q2,r2,N1,N2);
			}
		}
	}



	/*
	 *
	 *  Three-dimensional Triangle-Triangle Overlap Test
	 *
	 */


	public int tri_tri_overlap_test_3d(double[] p1, double[] q1, double[] r1, 
			double[] p2, double[] q2, double[] r2, double[] intersect1, double[] intersect2) {
		
		double dp1, dq1, dr1, dp2, dq2, dr2;
		double[] v1 = new double[3];
		double[] v2 = new double[3];
		double[] N1 = new double[3];
		double[] N2 = new double[3]; 

	    /* Compute distance signs  of p1, q1 and r1 to the plane of
	     triangle(p2,q2,r2) */


	    SUB(v1,p2,r2);
	    SUB(v2,q2,r2);
	    CROSS(N2,v1,v2);

	    SUB(v1,p1,r2);
	    dp1 = DOT(v1,N2);
	    SUB(v1,q1,r2);
	    dq1 = DOT(v1,N2);
	    SUB(v1,r1,r2);
	    dr1 = DOT(v1,N2);

	    if (((dp1 * dq1) > 0.0f) && ((dp1 * dr1) > 0.0f))  return 0; 

	    /* Compute distance signs  of p2, q2 and r2 to the plane of
	     triangle(p1,q1,r1) */


	    SUB(v1,q1,p1);
	    SUB(v2,r1,p1);
	    CROSS(N1,v1,v2);

	    SUB(v1,p2,r1);
	    dp2 = DOT(v1,N1);
	    SUB(v1,q2,r1);
	    dq2 = DOT(v1,N1); 
	    SUB(v1,r2,r1);
	    dr2 = DOT(v1,N1);

	    if (((dp2 * dq2) > 0.0f) && ((dp2 * dr2) > 0.0f)) return 0;

	    /* Permutation in a canonical form of T1's vertices */




	    if (dp1 > 0.0f) {
	        if (dq1 > 0.0f) return TRI_TRI_3D(r1,p1,q1,p2,r2,q2,dp2,dr2,dq2,N1,N2);
	        else if (dr1 > 0.0f) return TRI_TRI_3D(q1,r1,p1,p2,r2,q2,dp2,dr2,dq2,N1,N2);  
	        else return TRI_TRI_3D(p1,q1,r1,p2,q2,r2,dp2,dq2,dr2,N1,N2);
	    } else if (dp1 < 0.0f) {
	        if (dq1 < 0.0f) return TRI_TRI_3D(r1,p1,q1,p2,q2,r2,dp2,dq2,dr2,N1,N2);
	        else if (dr1 < 0.0f) return TRI_TRI_3D(q1,r1,p1,p2,q2,r2,dp2,dq2,dr2,N1,N2);
	        else return TRI_TRI_3D(p1,q1,r1,p2,r2,q2,dp2,dr2,dq2,N1,N2);
	    } else {
	        if (dq1 < 0.0f) {
	        	if (dr1 >= 0.0f) return TRI_TRI_3D(q1,r1,p1,p2,r2,q2,dp2,dr2,dq2,N1,N2);
	            else return TRI_TRI_3D(p1,q1,r1,p2,q2,r2,dp2,dq2,dr2,N1,N2);
	        } else if (dq1 > 0.0f) {
	        	if (dr1 > 0.0f) return TRI_TRI_3D(p1,q1,r1,p2,r2,q2,dp2,dr2,dq2,N1,N2);
	            else return TRI_TRI_3D(q1,r1,p1,p2,q2,r2,dp2,dq2,dr2,N1,N2);
	        } else  {
	        	if (dr1 > 0.0f) return TRI_TRI_3D(r1,p1,q1,p2,q2,r2,dp2,dq2,dr2,N1,N2);
	            else if (dr1 < 0.0f) return TRI_TRI_3D(r1,p1,q1,p2,r2,q2,dp2,dr2,dq2,N1,N2);
	            else return coplanar_tri_tri3d(p1,q1,r1,p2,q2,r2,N1,N2);
	        }
	    }
	}



	public int coplanar_tri_tri3d(double[] p1, double[] q1, double[] r1,
			double[] p2, double[] q2, double[] r2,
			double[] normal_1, double[] normal_2) {

		double[] P1 = new double[2];
		double[] Q1 = new double[2];
		double[] R1 = new double[2];
		double[] P2 = new double[2];
		double[] Q2 = new double[2];
		double[] R2 = new double[2];

	    double n_x, n_y, n_z;

	    n_x = ((normal_1[0]<0)?-normal_1[0]:normal_1[0]);
	    n_y = ((normal_1[1]<0)?-normal_1[1]:normal_1[1]);
	    n_z = ((normal_1[2]<0)?-normal_1[2]:normal_1[2]);


	    /* Projection of the triangles in 3D onto 2D such that the area of
	     the projection is maximized. */


	    if (( n_x > n_z ) && ( n_x >= n_y )) {
	        // Project onto plane YZ

	        P1[0] = q1[2]; P1[1] = q1[1];
	        Q1[0] = p1[2]; Q1[1] = p1[1];
	        R1[0] = r1[2]; R1[1] = r1[1]; 

	        P2[0] = q2[2]; P2[1] = q2[1];
	        Q2[0] = p2[2]; Q2[1] = p2[1];
	        R2[0] = r2[2]; R2[1] = r2[1]; 

	    } else if (( n_y > n_z ) && ( n_y >= n_x )) {
	        // Project onto plane XZ

	        P1[0] = q1[0]; P1[1] = q1[2];
	        Q1[0] = p1[0]; Q1[1] = p1[2];
	        R1[0] = r1[0]; R1[1] = r1[2]; 

	        P2[0] = q2[0]; P2[1] = q2[2];
	        Q2[0] = p2[0]; Q2[1] = p2[2];
	        R2[0] = r2[0]; R2[1] = r2[2]; 

	    } else {
	        // Project onto plane XY

	        P1[0] = p1[0]; P1[1] = p1[1]; 
	        Q1[0] = q1[0]; Q1[1] = q1[1]; 
	        R1[0] = r1[0]; R1[1] = r1[1]; 

	        P2[0] = p2[0]; P2[1] = p2[1]; 
	        Q2[0] = q2[0]; Q2[1] = q2[1]; 
	        R2[0] = r2[0]; R2[1] = r2[1]; 
	    }

	    return tri_tri_overlap_test_2d(P1,Q1,R1,P2,Q2,R2);

	}



	/*
	 *                                                                
	 *  Three-dimensional Triangle-Triangle Intersection              
	 *
	 */

	/*
	 This macro is called when the triangles surely intersect
	 It constructs the segment of intersection of the two triangles
	 if they are not coplanar.
	 */

	int CONSTRUCT_INTERSECTION(double[] p1, double[] q1, double[] r1, double[] p2, double[] q2, double[] r2, double[] source, double[] target, double[] N1, double[] N2) { 
		double alpha;
		double[] N = new double[3];
		double[] v = new double[3];
		double[] v1 = new double[3];
		double[] v2 = new double[3];

		SUB(v1,q1,p1);
		SUB(v2,r2,p1); 
		CROSS(N,v1,v2); 
		SUB(v,p2,p1);
		if (DOT(v,N) > 0.0f) {
			SUB(v1,r1,p1); 
			CROSS(N,v1,v2);
			if (DOT(v,N) <= 0.0f) {
				SUB(v2,q2,p1);
				CROSS(N,v1,v2);
				if (DOT(v,N) > 0.0f) {
					SUB(v1,p1,p2);
					SUB(v2,p1,r1);
					alpha = DOT(v1,N2) / DOT(v2,N2);
					SCALAR(v1,alpha,v2);
					SUB(source,p1,v1);
					SUB(v1,p2,p1);
					SUB(v2,p2,r2);
					alpha = DOT(v1,N1) / DOT(v2,N1);
					SCALAR(v1,alpha,v2);
					SUB(target,p2,v1);
					return 1;
				} else {
					SUB(v1,p2,p1);
					SUB(v2,p2,q2);
					alpha = DOT(v1,N1) / DOT(v2,N1);
					SCALAR(v1,alpha,v2);
					SUB(source,p2,v1);
					SUB(v1,p2,p1);
					SUB(v2,p2,r2);
					alpha = DOT(v1,N1) / DOT(v2,N1);
					SCALAR(v1,alpha,v2);
					SUB(target,p2,v1);
					return 1;
				}
			} else {
				return 0;
			}
		} else {
			SUB(v2,q2,p1);
			CROSS(N,v1,v2);
			if (DOT(v,N) < 0.0f) {
				return 0;
			} else {
				SUB(v1,r1,p1);
				CROSS(N,v1,v2);
				if (DOT(v,N) >= 0.0f) {
					SUB(v1,p1,p2);
					SUB(v2,p1,r1);
					alpha = DOT(v1,N2) / DOT(v2,N2);
					SCALAR(v1,alpha,v2);
					SUB(source,p1,v1);
					SUB(v1,p1,p2);
					SUB(v2,p1,q1);
					alpha = DOT(v1,N2) / DOT(v2,N2);
					SCALAR(v1,alpha,v2);
					SUB(target,p1,v1);
					return 1;
				} else {
					SUB(v1,p2,p1);
					SUB(v2,p2,q2);
					alpha = DOT(v1,N1) / DOT(v2,N1);
					SCALAR(v1,alpha,v2);
					SUB(source,p2,v1);
					SUB(v1,p1,p2);
					SUB(v2,p1,q1);
					alpha = DOT(v1,N2) / DOT(v2,N2);
					SCALAR(v1,alpha,v2);
					SUB(target,p1,v1);
					return 1;
				}
			}
		}
	} 



	int TRI_TRI_INTER_3D(double[] p1, double[] q1, double[] r1, double[] p2, double[] q2, double[] r2, double dp2, double dq2, double dr2, Integer coplanar,
			double[] N1, double[] N2, double[] source, double[] target) {
		if (dp2 > 0.0f) {
			if (dq2 > 0.0f) return CONSTRUCT_INTERSECTION(p1,r1,q1,r2,p2,q2, source, target, N1, N2);
			else if (dr2 > 0.0f) return CONSTRUCT_INTERSECTION(p1,r1,q1,q2,r2,p2, source, target, N1, N2);
			else return CONSTRUCT_INTERSECTION(p1,q1,r1,p2,q2,r2, source, target, N1, N2); 
		}
		else if (dp2 < 0.0f) {
			if (dq2 < 0.0f) return CONSTRUCT_INTERSECTION(p1,q1,r1,r2,p2,q2, source, target, N1, N2);
			else if (dr2 < 0.0f) return CONSTRUCT_INTERSECTION(p1,q1,r1,q2,r2,p2, source, target, N1, N2);
			else return CONSTRUCT_INTERSECTION(p1,r1,q1,p2,q2,r2, source, target, N1, N2);
		} else {
			if (dq2 < 0.0f) {
				if (dr2 >= 0.0f) return CONSTRUCT_INTERSECTION(p1,r1,q1,q2,r2,p2, source, target, N1, N2);
				else return CONSTRUCT_INTERSECTION(p1,q1,r1,p2,q2,r2, source, target, N1, N2);
			}
			else if (dq2 > 0.0f) {
				if (dr2 > 0.0f) return CONSTRUCT_INTERSECTION(p1,r1,q1,p2,q2,r2, source, target, N1, N2);
				else  return CONSTRUCT_INTERSECTION(p1,q1,r1,q2,r2,p2, source, target, N1, N2);
			}
			else  {
				if (dr2 > 0.0f) return CONSTRUCT_INTERSECTION(p1,q1,r1,r2,p2,q2, source, target, N1, N2);
				else if (dr2 < 0.0f) return CONSTRUCT_INTERSECTION(p1,r1,q1,r2,p2,q2, source, target, N1, N2);
				else {
					coplanar = 1;
					return coplanar_tri_tri3d(p1,q1,r1,p2,q2,r2,N1,N2);
				}
			}
		} 
	}


	/**
	 * The following version computes the segment of intersection of the
	 * two triangles if it exists. 
	 * coplanar returns whether the triangles are coplanar
	 * source and target are the end points of the line segment of intersection 
	 * @param p1 Triangle 1 first vertex
	 * @param q1 Triangle 1 second vertex
	 * @param r1 Triangle 1 third vertex
	 * @param p2 Triangle 2 first vertex
	 * @param q2 Triangle 2 second vertex
	 * @param r2 Triangle 2 third vertex
	 * @param coplanar 0 if not coplanar, 1 if coplanar
	 * @param source starting end point of the line segment of intersection
	 * @param target ending point of the line segment of intersection
	 * @return true if there is an intersection
	 */
	public int tri_tri_intersection_test_3d(double[] p1, double[] q1, double[] r1, 
			double[]  p2, double[]  q2, double[]  r2, Integer coplanar, double[]  source, double[] target) {
		
		double dp1, dq1, dr1, dp2, dq2, dr2;
		double[] v1 = new double[3];
		double[] v2 = new double[3];
		double[] N1 = new double[3];
		double[] N2 = new double[3]; 

	    // Compute distance signs  of p1, q1 and r1 
	    // to the plane of triangle(p2,q2,r2)


	    SUB(v1,p2,r2);
	    SUB(v2,q2,r2);
	    CROSS(N2,v1,v2);

	    SUB(v1,p1,r2);
	    dp1 = DOT(v1,N2);
	    SUB(v1,q1,r2);
	    dq1 = DOT(v1,N2);
	    SUB(v1,r1,r2);
	    dr1 = DOT(v1,N2);

	    if (((dp1 * dq1) > 0.0f) && ((dp1 * dr1) > 0.0f))  return 0; 

	    // Compute distance signs  of p2, q2 and r2 
	    // to the plane of triangle(p1,q1,r1)


	    SUB(v1,q1,p1);
	    SUB(v2,r1,p1);
	    CROSS(N1,v1,v2);

	    SUB(v1,p2,r1);
	    dp2 = DOT(v1,N1);
	    SUB(v1,q2,r1);
	    dq2 = DOT(v1,N1);
	    SUB(v1,r2,r1);
	    dr2 = DOT(v1,N1);

	    if (((dp2 * dq2) > 0.0f) && ((dp2 * dr2) > 0.0f)) return 0;

	    // Permutation in a canonical form of T1's vertices


	    //  printf("d1 = [%f %f %f], d2 = [%f %f %f]\n", dp1, dq1, dr1, dp2, dq2, dr2);
	    /*
	     // added by Aaron
	     if (ZERO_TEST(dp1) || ZERO_TEST(dq1) ||ZERO_TEST(dr1) ||ZERO_TEST(dp2) ||ZERO_TEST(dq2) ||ZERO_TEST(dr2))
	     {
	     coplanar = 1;
	     return 0;
	     }
	     */


	    if (dp1 > 0.0f) {
	        if (dq1 > 0.0f) return TRI_TRI_INTER_3D(r1,p1,q1,p2,r2,q2,dp2,dr2,dq2, coplanar, N1, N2, source, target);
	        else if (dr1 > 0.0f) return TRI_TRI_INTER_3D(q1,r1,p1,p2,r2,q2,dp2,dr2,dq2, coplanar, N1, N2, source, target);
	        else return TRI_TRI_INTER_3D(p1,q1,r1,p2,q2,r2,dp2,dq2,dr2, coplanar, N1, N2, source, target);
	    } else if (dp1 < 0.0f) {
	    	if (dq1 < 0.0f) return TRI_TRI_INTER_3D(r1,p1,q1,p2,q2,r2,dp2,dq2,dr2, coplanar, N1, N2, source, target);
	        else if (dr1 < 0.0f) return TRI_TRI_INTER_3D(q1,r1,p1,p2,q2,r2,dp2,dq2,dr2, coplanar, N1, N2, source, target);
	        else return TRI_TRI_INTER_3D(p1,q1,r1,p2,r2,q2,dp2,dr2,dq2, coplanar, N1, N2, source, target);
	    } else {
	        if (dq1 < 0.0f) {
	        	if (dr1 >= 0.0f) return TRI_TRI_INTER_3D(q1,r1,p1,p2,r2,q2,dp2,dr2,dq2, coplanar, N1, N2, source, target);
	        	else return TRI_TRI_INTER_3D(p1,q1,r1,p2,q2,r2,dp2,dq2,dr2, coplanar, N1, N2, source, target);
	        }
	        else if (dq1 > 0.0f) {
	        	if (dr1 > 0.0f) return TRI_TRI_INTER_3D(p1,q1,r1,p2,r2,q2,dp2,dr2,dq2, coplanar, N1, N2, source, target);
	            else return TRI_TRI_INTER_3D(q1,r1,p1,p2,q2,r2,dp2,dq2,dr2, coplanar, N1, N2, source, target);
	        }
	        else  {
	        	if (dr1 > 0.0f) return TRI_TRI_INTER_3D(r1,p1,q1,p2,q2,r2,dp2,dq2,dr2, coplanar, N1, N2, source, target);
	            else if (dr1 < 0.0f) return TRI_TRI_INTER_3D(r1,p1,q1,p2,r2,q2,dp2,dr2,dq2, coplanar, N1, N2, source, target);
	            else {
	            	// triangles are co-planar
	               coplanar = 1;
	               return coplanar_tri_tri3d(p1,q1,r1,p2,q2,r2,N1,N2); 
	            }
	        }
	    }
	}





	/*
	 *
	 *  Two dimensional Triangle-Triangle Overlap Test    
	 *
	 */


	/* some 2D macros */
	
	double ORIENT_2D(double[] a, double[] b, double[] c) {
		return ((a[0]-c[0])*(b[1]-c[1])-(a[1]-c[1])*(b[0]-c[0]));
	}


	int INTERSECTION_TEST_VERTEXA(double[] P1, double[] Q1, double[] R1, double[] P2, double[] Q2, double[] R2) {
		if (ORIENT_2D(R2, P2, Q1) >= 0.0f)
			if (ORIENT_2D(R2, Q2, Q1) <= 0.0f)
				if (ORIENT_2D(P1, P2, Q1) > 0.0f) {
					if (ORIENT_2D(P1, Q2, Q1) <= 0.0f)
						return 1;
					else
						return 0;
				} else {
					if (ORIENT_2D(P1, P2, R1) >= 0.0f)
						if (ORIENT_2D(Q1, R1, P2) >= 0.0f)
							return 1;
						else
							return 0;
					else
						return 0;
				}
			else if (ORIENT_2D(P1, Q2, Q1) <= 0.0f)
				if (ORIENT_2D(R2, Q2, R1) <= 0.0f)
					if (ORIENT_2D(Q1, R1, Q2) >= 0.0f)
						return 1;
					else
						return 0;
				else
					return 0;
			else
				return 0;
		else if (ORIENT_2D(R2, P2, R1) >= 0.0f)
			if (ORIENT_2D(Q1, R1, R2) >= 0.0f)
				if (ORIENT_2D(P1, P2, R1) >= 0.0f)
					return 1;
				else
					return 0;
			else if (ORIENT_2D(Q1, R1, Q2) >= 0.0f) {
				if (ORIENT_2D(R2, R1, Q2) >= 0.0f)
					return 1;
				else
					return 0;
			} else
				return 0;
		else
			return 0;
	}

	int INTERSECTION_TEST_VERTEX(double[] P1, double[] Q1, double[] R1, double[] P2, double[] Q2, double[] R2) {
		if (ORIENT_2D(R2, P2, Q1) >= 0.0f)
			if (ORIENT_2D(R2, Q2, Q1) <= 0.0f)
				if (ORIENT_2D(P1, P2, Q1) > 0.0f) {
					if (ORIENT_2D(P1, Q2, Q1) <= 0.0f)
						return 1;
					else
						return 0;
				} else {
					if (ORIENT_2D(P1, P2, R1) >= 0.0f)
						if (ORIENT_2D(Q1, R1, P2) >= 0.0f)
							return 1;
						else
							return 0;
					else
						return 0;
				}
			else if (ORIENT_2D(P1, Q2, Q1) <= 0.0f)
				if (ORIENT_2D(R2, Q2, R1) <= 0.0f)
					if (ORIENT_2D(Q1, R1, Q2) >= 0.0f)
						return 1;
					else
						return 0;
				else
					return 0;
			else
				return 0;
		else if (ORIENT_2D(R2, P2, R1) >= 0.0f)
			if (ORIENT_2D(Q1, R1, R2) >= 0.0f)
				if (ORIENT_2D(P1, P2, R1) >= 0.0f)
					return 1;
				else
					return 0;
			else if (ORIENT_2D(Q1, R1, Q2) >= 0.0f) {
				if (ORIENT_2D(R2, R1, Q2) >= 0.0f)
					return 1;
				else
					return 0;
			} else
				return 0;
		else
			return 0;
	}

	int INTERSECTION_TEST_EDGE(double[] P1, double[] Q1, double[] R1, double[] P2, double[] Q2, double[] R2) {
		if (ORIENT_2D(R2,P2,Q1) >= 0.0f) {
			if (ORIENT_2D(P1,P2,Q1) >= 0.0f) { 
				if (ORIENT_2D(P1,Q1,R2) >= 0.0f) 
					return 1;
				else 
					return 0;
			} else { 
				if (ORIENT_2D(Q1,R1,P2) >= 0.0f){ 
					if (ORIENT_2D(R1,P1,P2) >= 0.0f) 
						return 1; 
					else 
						return 0;
				} 
				else 
					return 0; 
			}
		} else {
			if (ORIENT_2D(R2,P2,R1) >= 0.0f) {
				if (ORIENT_2D(P1,P2,R1) >= 0.0f) {
					if (ORIENT_2D(P1,R1,R2) >= 0.0f) 
						return 1;
					else {
						if (ORIENT_2D(Q1,R1,R2) >= 0.0f) 
							return 1; 
						else 
							return 0;
					}
				}
				else  
					return 0; 
			}
			else 
				return 0; 
		}
	}


	public int ccw_tri_tri_intersection_2d(double[] p1, double[] q1, double[] r1, 
			double[] p2, double[] q2, double[] r2) {
	    if ( ORIENT_2D(p2,q2,p1) >= 0.0f ) {
	        if ( ORIENT_2D(q2,r2,p1) >= 0.0f ) {
	            if ( ORIENT_2D(r2,p2,p1) >= 0.0f ) 
	            	return 1;
	            else 
	            	return INTERSECTION_TEST_EDGE(p1,q1,r1,p2,q2,r2);
	        } else {  
	        	if ( ORIENT_2D(r2,p2,p1) >= 0.0f ) 
	        		return INTERSECTION_TEST_EDGE(p1,q1,r1,r2,p2,q2);
	        	else 
	        		return INTERSECTION_TEST_VERTEX(p1,q1,r1,p2,q2,r2);
	        }
	    } else {
	        if ( ORIENT_2D(q2,r2,p1) >= 0.0f ) {
	            if ( ORIENT_2D(r2,p2,p1) >= 0.0f ) 
	                return INTERSECTION_TEST_EDGE(p1,q1,r1,q2,r2,p2);
	            else  
	            	return INTERSECTION_TEST_VERTEX(p1,q1,r1,q2,r2,p2);
	        }
	        else 
	        	return INTERSECTION_TEST_VERTEX(p1,q1,r1,r2,p2,q2);
	    }
	}


	public int tri_tri_overlap_test_2d(double[]  p1, double[] q1, double[] r1, 
			double[] p2, double[] q2, double[] r2) { 
	    if ( ORIENT_2D(p1,q1,r1) < 0.0f )
	        if ( ORIENT_2D(p2,q2,r2) < 0.0f )
	            return ccw_tri_tri_intersection_2d(p1,r1,q1,p2,r2,q2);
	        else
	            return ccw_tri_tri_intersection_2d(p1,r1,q1,p2,q2,r2);
	    else if ( ORIENT_2D(p2,q2,r2) < 0.0f )
	    	return ccw_tri_tri_intersection_2d(p1,q1,r1,p2,r2,q2);
	    else
	    	return ccw_tri_tri_intersection_2d(p1,q1,r1,p2,q2,r2);

	}
	/*  END OF            
	 *  Triangle-Triangle Overlap Test Routines             
	 */

	/**
	 * @return the WORLD_SIZE
	 */
	public float getWORLD_SIZE() {
		return WORLD_SIZE;
	}
	
	/**
	 * This method clones the OctTree to a scaled value.
	 * @param scaleFactor
	 * @return the scaled OctTree
	 */
	public OctTree getRescaledOctTree(float scaleFactor) {
		Float key = Float.valueOf(scaleFactor);

		if (rescaledCache.containsKey(key)) {
			return rescaledCache.get(key);
		}
		else {
			try {
				HashMap<Integer, Triangle> facets = new HashMap<>();
				
				looseGetAllTriangles(root, facets);
				
				ONode newRoot = new ONode(null, 0, 0, 0, 0);
				
				OctTree tmpTree = new OctTree(newRoot, this.WORLD_SIZE * scaleFactor, 1f, maxDimension * scaleFactor, facets.size(), isObj);
				
				for (Triangle t : facets.values()) {
					tmpTree.looseOctTreeInsert(newRoot, t.cloneWithScalar(scaleFactor));
				}
				
				rescaledCache.put(key, tmpTree);
				
				return tmpTree;
			}
			catch(Exception ex) {
				log.aprintln(ex.getClass().getName()+" occurred while generating "+scaleFactor+" scaled version of OctTree");
				throw ex;
			}
		}
	}
	
	public void delete() {
		
	}
		
}
