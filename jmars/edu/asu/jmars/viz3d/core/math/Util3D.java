package edu.asu.jmars.viz3d.core.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.vecmath.Matrix3d;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.math.geom.Frustum;

import edu.asu.jmars.layer.threed.Vec3dMath;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.viz3d.core.geometry.Cuboid;
import edu.asu.jmars.viz3d.core.geometry.OctTree;
import edu.asu.jmars.viz3d.core.vectors.Vector3D;
import edu.asu.jmars.viz3d.spatial.RayWithEpsilon;
import edu.asu.jmars.viz3d.spatial.SpatialRenderable;
import edu.asu.jmars.viz3d.core.geometry.Vertex3D;
import edu.asu.jmars.viz3d.core.geometry.Vertex3DAngleComparator;
import edu.asu.jmars.viz3d.core.geometry.VertexAngle;
import edu.asu.jmars.viz3d.core.geometry.VertexAngleComparator;
import edu.asu.jmars.viz3d.core.geometry.Visibility;
import edu.asu.jmars.viz3d.core.geometry.Triangle;

public class Util3D {

	private static DebugLog log = DebugLog.instance();
	public static boolean TEST_CULL = true;
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
	/** High tolerance epsilon (0.000001) for floating point comparisons */
	public static final float EPSILON = 0.000001f;
	/** Low tolerance epsilon (0.0001) for floating point comparisons */
	public static final float MICRO_EPSILON = 0.0001f;
	/** Standard tolerance epsilon (0.00001) for floating point comparisons */
	public static final float MINI_EPSILON = 0.00001f;
	/** Double epsilon for floating point comparisons */
	public static final float EPSILON_X2 = 2 * EPSILON;
	/** Super high tolerance epsilon (0.0000000000001) for floating point comparisons */
	public static final float SUPER_EPSILON = 0.0000000000001f;
	/** High tolerance epsilon (0.000001) for floating point comparisons */
	public static final float RAY_EPSILON_DEFAULT = 0.01f;
	/** No division constant of 1/3 for 3D vector magnitude and average calculations */
	public static final float ONE_THIRD = 0.33333333333333333f;

   	final static float TESSELLATION_LIMIT = 0.0005f;
	
	
	/*
	 * End of totologic code
	 */

	/**
	 * This method attempts to determine if a point is located inside a triangle
	 * It is assumed the point is known to be inside or on the edge of the triangle
	 * @param point
	 * @param v1 triangle vertex 1
	 * @param v2 triangle vertex 2
	 * @param v3 triangle vertex 3
	 * @return true if point is sufficiently distant from all three sides
	 */			
	public static boolean pointInTriangleInterior(float[] point, float[] v1, float[] v2, float[] v3) {
		double epsilon = 0.00001;
		double[] sa = OctTree.floatToDoubleArray(v1);
		double[] sb = OctTree.floatToDoubleArray(v2);
		double[] sc = OctTree.floatToDoubleArray(v3);
		
		double[] p = OctTree.floatToDoubleArray(point);
		
		if (Util3D.distanceFromPointToLine3D(p, sa, sb) < epsilon) {
			return false;
		} else if (Util3D.distanceFromPointToLine3D(p, sb, sc) < epsilon) {
			return false;
		} else if (Util3D.distanceFromPointToLine3D(p, sc, sa) < epsilon) {
			return false;
		} else {
			return true;
		}		
	}
	
	/**
	 * A numerically stable (stable for values less than 1) method for computing the 
	 * distance from a point to a line segment in 3D 
	 * @param point
	 * @param linePtA
	 * @param linePtB
	 * @return the distance
	 */
	public static double distanceFromPointToLine3D(double[] point, double[] linePtA, double[] linePtB) {
		double ret = Double.NaN;
		// Apply Heron's formula to get area of triangle defined by vertices point, linePtA, linePtB
		// calculate the lengths of each side
		Vector3D p = new Vector3D(point[0], point[1], point[2]);
		Vector3D a = new Vector3D(linePtA[0], linePtA[1], linePtA[2]);
		Vector3D b = new Vector3D(linePtB[0], linePtB[1], linePtB[2]);
		double pa = Vector3D.OpSubtraction(p, a).getMagnitude();
		double ab = Vector3D.OpSubtraction(a, b).getMagnitude();
		double bp = Vector3D.OpSubtraction(b, p).getMagnitude();
		// get the area
		double area = triangleAreaByHeron(pa, ab, bp);
		
		ret = (area * 2) / ab;
		
		return ret;
	}

	/**
	 * Method to compute the area of a triangle using the lengths of the sides 
	 * (numerically stable Heron's Method)
	 * @param a side a
	 * @param b side b
	 * @param c side c
	 * @return area
	 */
	public static double triangleAreaByHeron(double a, double b, double c) {
		double[] s = new double[] {a, b, c};
		double temp = 0;
		// sort the sides descending
		if (s[0] < s[1]) {
		    temp = s[0];
		    s[0] = s[1];
		    s[1] = temp;
		}
		if (s[0] < s[2]) {
		    temp = s[0];
		    s[0] = s[2];
		    s[2] = temp;
		}
		if (s[1] < s[2]) {
		    temp = s[1];
		    s[1] = s[2];
		    s[2] = temp;
		}
		double n = (s[0] + (s[1] + s[2])) * (s[2] - (s[0] - s[1])) * (s[2] + (s[0] - s[1])) * (s[0] + (s[1] - s[2]));
		return Math.sqrt(n) / 4.0;		
	}
	

	public static boolean pointOnTriangleEdge(float[] point, float[] v1, float[] v2, float[] v3) {
		if (OctTree.collinearLineSegs(v1, point, v1, v2)) {
			return true;
		}
		if (OctTree.collinearLineSegs(v1, point, v1, v3)) {
			return true;
		}
		if (OctTree.collinearLineSegs(v2, point, v2, v3)) {
			return true;
		}
		return false;		
	}

	public static boolean joglPointOnTriangleEdge(float[] point, float[] v1, float[] v2, float[] v3) {
		if (VectorUtil.isCollinearVec3(v1, v2, point)) {
			return true;
		}
		if (VectorUtil.isCollinearVec3(v1, v3, point)) {
			return true;
		}
		if (VectorUtil.isCollinearVec3(v3, v2, point)) {
			return true;
		}
		return false;		
	}

	/**
	 * 	 Method to find the rotation matrix of the right-handed frame having a
	 *   given vector as a specified axis and having a second given
	 *   vector lying in a specified coordinate plane

	 * @param axdef Vector defining a principal axis
	 * @param indexa Principal axis number of AXDEF (X=1, Y=2, Z=3)
	 * @param plndef Vector defining (with AXDEF) a principal plane
	 * @param indexp Second axis number (with INDEXA) of principal plane
	 * @param mout Output rotation matrix
	 * @throws Exception 
	 */
	public static double[] twovec(HVector axdef, int indexa, HVector plndef, int indexp, double[] mout) throws Exception {
		if (mout == null) {
			mout = new double[3*3];
		}
				
		if (Math.max(indexa, indexp) > 3 || Math.min(indexa, indexp) < 1) {
			log.aprintln("The defintion indexes must lie in the range from 1 to 3. The value of indexa was "+indexa+". The value of indexp was "+indexp+".");
			throw new Exception("A fatal error occured during the area calculation of your shape(s).");
		}
		
		if (Double.compare(axdef.unit().dot(plndef.unit()), 1.0) == 0) {
			log.aprintln("The defintion indexes indexa and indexp cannot be identical.");
			throw new Exception("A fatal error occured during the area calculation of your shape(s).");
		}
	
		int seqnce[] = {1,2,3,1,2};
		
		int i1 = indexa;
		int i2 = seqnce[indexa];
		int i3 = seqnce[indexa+1];
		
		System.arraycopy(axdef.unit().toArray(), 0, mout, (i1-1)*3, 3);
		
		if (indexp == i2) {
			HVector c = axdef.ucross(plndef);
			System.arraycopy(c.toArray(),               0, mout, (i3-1)*3, 3);
			System.arraycopy(c.ucross(axdef).toArray(), 0, mout, (i2-1)*3, 3);
		}
		else {
			HVector c = plndef.ucross(axdef);
			System.arraycopy(c.toArray(),               0, mout, (i2-1)*3, 3);
			System.arraycopy(axdef.ucross(c).toArray(), 0, mout, (i3-1)*3, 3);
		}
		
		if (mout[(i2-1)*3] == 0 && mout[(i2-1)*3+1] == 0 && mout[(i2-1)*3+2] == 0) {
			log.aprintln("The input vectors axdef and plndef are linearly dependent.");
			throw new Exception("A fatal error occured during the area calculation of your shape(s).");
		}
		
		return mout;
	}
		
	public static double[][] m1dto3x3(double[] m1d, double[][] m3x3){
		if (m3x3 == null)
			m3x3 = new double[3][3];
		System.arraycopy(m1d, 0, m3x3[0], 0, 3);
		System.arraycopy(m1d, 3, m3x3[1], 0, 3);
		System.arraycopy(m1d, 6, m3x3[2], 0, 3);
		return m3x3;
	}

	/**
	 * Method to multiply a 3x3 matrix by a column vector. 
	 * @param m 3x3 matrix
	 * @param pts HVector
	 * @return HVector
	 */
	public static HVector[] mxv(double[][] m, HVector[] pts){
		HVector[] out = new HVector[pts.length];
		
		for(int i=0; i<pts.length; i++){
			double[] x = mulRowMat3Vec3(new double[3], m, pts[i].toArray());
			out[i] = new HVector(x);
		}
		
		return out;
	}
	
    /**
     * Matrix Vector multiplication
     * @param rawMatrix column matrix (3x3)
     * @param vec vector(x,y,z)
     * @return result
     */
    public static double[] mulRowMat3Vec3(final double[] result, final double[][] rawMatrix, final double[] vec)
    {
        result[0] = vec[0]*rawMatrix[0][0] + vec[1]*rawMatrix[0][1] + vec[2]*rawMatrix[0][2];
        result[1] = vec[0]*rawMatrix[1][0] + vec[1]*rawMatrix[1][1] + vec[2]*rawMatrix[1][2];
        result[2] = vec[0]*rawMatrix[2][0] + vec[1]*rawMatrix[2][1] + vec[2]*rawMatrix[2][2];

        return result;
    }

	public static double[][] double1DTo2DArray (double[] sa, int stride) {
		if (sa == null || sa.length % stride != 0) {
			return null;
		}
		double[][] ret = new double[sa.length / stride][stride];
		int cnt = 0;
		for (int i=0; i<sa.length; i+=stride) {
			double[] tmp = new double[stride];
			int k=0;
			for (int j=i; j<i+stride; j++) {
				tmp[k++] = sa[j];
			}
			ret[cnt++] = tmp;
		}
	
		return ret;
	}
	
    public static boolean isPowerOf2(int i) {
        return i > 2 && ((i&-i)==i);
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
			if (det < FloatUtil.EPSILON) {
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
			if (det > -FloatUtil.EPSILON && det < FloatUtil.EPSILON) {
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

	
	
	public static float rayIntersectBox (float[] rayOrg, float[] rayDelta, float cx, float cy, float cz, float halfSize, float[] returnNormal) {
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
	public static float[] normalizeVec3(float[] vout, float[] vin) {
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
	
	/*
	 * Rescale a vector of direction "vector" with length "size"
	 *
	 * @param vector
	 * @param size
	 * @return the resized vector
	 *
	 * thread-safe
	 */
	public static float[] setVectorLength(float[] vector, float size){
 
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
	public static boolean closestPointsOnTwoLines(float[] closestPointLine1, float[] closestPointLine2, float[] linePoint1, float[] lineVec1, float[] linePoint2, float[] lineVec2){
 
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
	public static void planeFrom3Points(float[] planeNormal, float[] planePoint, float[] pointA, float[] pointB, float[] pointC) {
 
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
	
	public static boolean vectorWithinEndRange(float[] start, float[] end, float[] v) {
		float startV = VectorUtil.distSquareVec3(start, v);
		float startEnd = VectorUtil.distSquareVec3(start,end);
		if (Float.compare(startV, startEnd) < 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean vectorsEqualWithEpsilon(float[] v1, float[] v2, float epsilon) {
		if (Math.abs(v1[X] - v2[X]) <= epsilon
				&& Math.abs(v1[Y] - v2[Y]) <= epsilon
				&& Math.abs(v1[Z] - v2[Z]) <= epsilon) {
			return true;
		} else {
			return false;
		}
	}
	
	public static float[] clearVector(float[] v) {
		if (v == null) {
			return v;
		}
		for (int i=0; i<v.length; i++) {
			v[i] = 0f;
		}
		return v;
	}
	
	public static float[] linePlaneIntersectTri(float[] intersection, float[] linePoint, float[] lineVec, Triangle t) {
		float[] triNorm = VectorUtil.crossVec3(new float[3], VectorUtil.subVec3(new float[3], t.points[1], t.points[0]), VectorUtil.subVec3(new float[3], t.points[2], t.points[0]));
		
		linePlaneIntersection(intersection, linePoint, lineVec, Util3D.normalizeVec3(new float[3], triNorm), t.points[0]);
		
		return intersection;
	}
	
	//Get the intersection between a line and a plane. 
	//If the line and plane are not parallel, the function outputs true, otherwise false.
	public static boolean linePlaneIntersection(float[] intersection, float[] linePoint, float[] lineVec, float[] planeNormal, float[] planePoint) {
 
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

	public static double[] addV3V3(double[] a, double[] b) {
		double[] ret = new double[3];
		ret[X] = a[X] + b[X];
		ret[Y] = a[Y] + b[Y];
		ret[Z] = a[Z] + b[Z];
		return ret;
	}
	
	public static double[] subV3V3(double[] a, double[] b) {
		double[] ret = new double[3];
		ret[X] = a[X] - b[X];
		ret[Y] = a[Y] - b[Y];
		ret[Z] = a[Z] - b[Z];
		return ret;
	}
	
	public static double dotV3V3(double[] v0, double[] v1) {
		return  (v0[X] * v1[X]) +
			    (v0[Y] * v1[Y]) +
			    (v0[Z] * v1[Z]);
	}
	
	public static double[] multV3D(double[] v, double f) {
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
	public static void comparePlanes(double[] planeEq, double[][] tri) {
		for (int i=0; i<tri.length; i++) {
			double pZ = (-planeEq[D] - planeEq[A]*tri[i][X] - planeEq[B]*tri[i][Y]) / planeEq[C];
			System.err.format(" Plane Eq z: %12.12f  Triangle vertex [%d] z: %12.12f\n", pZ, i, tri[i][Z]);
		}
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
	public float[] linePlaneCollision(float[] a, float[] b, Triangle tri1) {
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
	   Calculate the line segment PaPb that is the shortest line segment between
	   two lines defined by P1P2 and P3P4. This intersecting line segment will by 
	   definition be perpendicular to both lines. Calculate also the values of mua and mub where
	      Pa = P1 + mua (P2 - P1)
	      Pb = P3 + mub (P4 - P3)
	   Return false if no solution exists.
	   Portions Copyright(C) Paul Bourke @ http://paulbourke.net/geometry/pointlineplane/
	*/
	public static boolean lineLineIntersect(float[] p1, float[] p2, float[] p3,
			float[] p4, float[] pa, float[] pb, double[] mua, double[] mub) {
		double EPS = Double.MIN_NORMAL;
		double[] p13 = new double[3];
		double[] p43 = new double[3];
		double[] p21 = new double[3];
		double d1343 = 0, d4321 = 0, d1321 = 0, d4343 = 0, d2121 = 0;
		double numer = 0, denom = 0;

		boolean debug = false;
		
		if (debug)
			System.out.format("p1:(%g,%g,%g) p2:(%g,%g,%g)  p3:(%g,%g,%g) p4:(%g,%g,%g)\n",
					p1[0],p1[1],p1[2],p2[0],p2[1],p2[2],
					p3[0],p3[1],p3[2],p4[0],p4[1],p4[2]);

		p13[X] = p1[X] - p3[X];
		p13[Y] = p1[Y] - p3[Y];
		p13[Z] = p1[Z] - p3[Z];
		p43[X] = p4[X] - p3[X];
		p43[Y] = p4[Y] - p3[Y];
		p43[Z] = p4[Z] - p3[Z];
		if (Math.abs(p43[X]) < EPS && Math.abs(p43[Y]) < EPS
				&& Math.abs(p43[Z]) < EPS) {
			if (debug) System.out.println("Miss 1");			
			return false;
		}
		p21[X] = p2[X] - p1[X];
		p21[Y] = p2[Y] - p1[Y];
		p21[Z] = p2[Z] - p1[Z];
		if (Math.abs(p21[X]) < EPS && Math.abs(p21[Y]) < EPS
				&& Math.abs(p21[Z]) < EPS) {
			if (debug) System.out.println("Miss 2");			
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
			if (debug) System.out.println("coincident lines");						
			return true;
		} else if (Math.abs(denom) < EPS || Double.compare(d4343, 0.0) == 0) {
			// lines are parallel or we will get divide by zero errors
			if (debug) System.out.println("parallel lines");						
			return false;
		}

		mua[0] = numer / denom;
		mub[0] = (d1343 + d4321 * (mua[0])) / d4343;
		if (debug) System.out.println("mua[0]: "+mua[0]+" mub[0]: "+mub[0]);
		
		// if mua or mub are outside the line segment of lines a (defined by p1 and p2) or b (p3, p4)
		// we will not consider the intersection valid
		// this test is to eliminate intersections that are beyond the area of interest of both lines
		if ((mua[0] < 0f || mua[0] > 1f) || (mub[0] < 0f || mub[0] > 1f)) {
//		if ((mua[0] < 0f || mua[0] > 1f) || (mub[0] < -0.1f || mub[0] > 1.1f)) {
			if (debug) System.out.println("Miss mub "+mub[0]);						
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
	   // Paul Bourke at http://www.paulbourke.net/geometry/pointlineplane/calclineline.cs
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
	
	public static float[][] findInnerPts2CollinearLineSegs(float[] start, float[] end, float[] v1, float[] v2) {
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
	
	// check if point p is in triangle a-b-c
	// Adapted from http://www.java-gaming.org/index.php?topic=30375.0
	public static boolean isPointInTriangle(float[] p, float[] a, float[] b, float[] c) {
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

	public float[] getTriangleCentroid(float[] v1, float[] v2, float[] v3) {
		return new float[]{(v1[X]+v2[X]+v3[X])*ONE_THIRD, (v1[Y]+v2[Y]+v3[Y])*ONE_THIRD, (v1[Z]+v2[Z]+v3[Z])*ONE_THIRD};
	}
	
	public static ArrayList<Triangle> sortTriangles(ArrayList<Triangle> tris, float[] start) {
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
		
	public static boolean isTriangleInsideCurve(float[][] tri, float[][] curvePts) {
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
	
	
	/*
	 * @param tri
	 * @param planeNormal
	 * @param planePoint
	 * @return
	 * @throws
	 */
	// generate the plane equation of a triangle
	public static boolean triPlaneNormalPoint(double[][] tri, double[] planeNormal, double[] planePoint) {
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
		HVector xV1V2 = hv1.ucross(hv2);
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
	public static boolean triPlaneNormalPoint(float[][] tri, float[] planeNormal, float[] planePoint) {
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
	
	//Calculate the intersection point of two lines. Returns true if lines intersect, otherwise false.
	//Note that in 3d, two lines do not intersect most of the time. So if the two lines are not in the 
	//same plane, don't use this method.
	public static boolean lineLineIntersection(float[] intersection, float[] linePoint1, float[] lineVec1, float[] linePoint2, float[] lineVec2) {
 
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
		normal = normalizeVec3(normal, center);
		
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
	
	public static boolean isBClockwiseFromA(float[] center, float[] normal, float[] A, float[] B) {
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
	
	// ported from http://paulbourke.net/geometry/polygonmesh/
	private static float min(float x, float y) {
		return x < y ? x : y;
	}

	// ported from http://paulbourke.net/geometry/polygonmesh/
	private static float max(float x, float y) {
		return x > y ? x : y;
	}

	// ported from http://paulbourke.net/geometry/polygonmesh/
	public static boolean insidePolygon(float[][] polygon, int n, float[] p) {
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
	public static boolean insideOnPolygon(float[][] polygon, int n, float[] p) {
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
	
	
	/*
	 * The following code is ported from http://totologic.blogspot.com/2014/01/accurate-point-in-triangle-test.html
	 */
	
	public static final float TOTO_EPSILON = 0.001f;
	public static final float TOTO_EPSILON_SQUARE = TOTO_EPSILON * TOTO_EPSILON;

	public static float side(float x1, float y1, float x2, float y2, float x, float y) {
		return (y2 - y1) * (x - x1) + (-x2 + x1) * (y - y1);
	}

	
	// WARNING ONLY for 2D
	public static float triSide2D(float x1, float y1, float x2, float y2, float x,
			float y) {
		return (y2 - y1) * (x - x1) + (-x2 + x1) * (y - y1);
	}

	// WARNING ONLY for 2D
	public static boolean naivePointInTriangle(float x1, float y1, float x2, float y2,
			float x3, float y3, float x, float y) {
		boolean checkSide1 = triSide2D(x1, y1, x2, y2, x, y) >= 0f;
		boolean checkSide2 = triSide2D(x2, y2, x3, y3, x, y) >= 0f;
		boolean checkSide3 = triSide2D(x3, y3, x1, y1, x, y) >= 0f;
		return checkSide1 && checkSide2 && checkSide3;
	}

	// WARNING ONLY for 2D
	public static boolean pointInTriangleBoundingBox(float x1, float y1, float x2,
			float y2, float x3, float y3, float x, float y) {
		float xMin = Math.min(x1, Math.min(x2, x3)) - EPSILON; // TOTO_EPSILON??
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
	public static float distanceSquarePointToSegment(float x1, float y1, float x2, float y2,
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

		if (distanceSquarePointToSegment(x1, y1, x2, y2, x, y) <= EPSILON_X2) { // TOTO_EPSILON_SQUARE??
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
			sorted[idx++] = vaItr.next().getIndex();
		}
		
		return sorted;
	}
	
	public static float getAngle2DPoints(float[] pSrc, float[] pTarget) {
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
	
	private static boolean triangleAboveThreshold(float[][] tri, float threshold) {
		float[] len = new float[]{0f};
		longestSideOfTriangle(tri, len);
		
		if (len[0] > threshold) {
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
		float[] cross = VectorUtil.crossVec3(new float[3], seg1, seg2);
		
		
		if (FloatUtil.isZero(VectorUtil.normVec3(cross), 0.00001f)) { // TODO no magic numbers!
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Method to generate a frustum from a shape model surface polygon.
	 * Each edge of the polygon combined with the origin become
	 * a makeshift frustum with as many "sides" as the polygon
	 * has edges.
	 * @param corners the points of the input polygon, points must be in CCW winding order
	 * @return the frustum as a 2D array
	 */
	public static float[][] getFrustumFromSurfacePolygon(float[][] corners) {
		if (corners == null || corners.length < 3) {
			return null;
		}
		float[][] frus = new float[corners.length + 2][4];
		int frustumIdx = 0;
		
		float[] planeNorm = new float[3];
		float[] planePt = new float[3];

		// generate all the "polygonal" sides except the "closing" side
		for (int i=0; i<corners.length; i++) {
			if (triPlaneNormalPoint(new float[][]{corners[i%corners.length], corners[(i+1)%corners.length], new float[]{0f, 0f, 0f}}, planeNorm, planePt)) {
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
	 * Method to generate a frustum from two convex polygons with the same number of points and parallel surfaces.
	 * @param corners the points of the input polygons, points must be in CCW winding order with the first half of 
	 * the points representing the near polygon and the second half of the points representing the far polygon.
	 * Side edges are defined as "opposing points" in each half of the input array.
	 * Example: an array of 6 3-element (xyz) points will map side edges as P0->P3, P1->P4, P2->P5 using zero indexing 
	 * with the near polygon being defined by P0,P1,P2 and the far polygon defined as P3,P4,P5 such that the input array 
	 * would be {P0,P1,P2,P3,P4,P5}
	 * @return the frustum as a 2D array
	 */
	public static float[][] getFrustumFromTwoPolygons(float[][] corners) {
		if (corners == null || corners.length < 3 || (corners.length % 2 != 0)) {
			return null;
		}
		float[][] frus = new float[corners.length + 2][4];
		int frustumIdx = 0;
		
		float[] planeNorm = new float[3];
		float[] planePt = new float[3];

		// generate all the "polygonal" sides except the "closing" side
		for (int i=0; i<corners.length/2; i++) {
			if (triPlaneNormalPoint(new float[][]{corners[i%corners.length], corners[(i+1)%corners.length], corners[(i+(corners.length/2))%corners.length]}, planeNorm, planePt)) {
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
		
		// FAR plane
		int idx = 0;
		float[][] far = new float[corners.length/2][];
		for (int j=corners.length/2; j<corners.length; j++) {
			far[idx++] = corners[j];
		}
		float[] fPlaneNorm = avgOf3DPolygon(far);
		float[] fPlaneNormUnit = VectorUtil.normalizeVec3(new float[] {fPlaneNorm[0],fPlaneNorm[1],fPlaneNorm[2]});
		float distance = VectorUtil.normVec3(far[0]); // distance
		frus[frustumIdx][X] = fPlaneNormUnit[X];
		frus[frustumIdx][Y] = fPlaneNormUnit[Y];
		frus[frustumIdx][Z] = fPlaneNormUnit[Z];
		frus[frustumIdx][W] = distance;
		frustumIdx++;

		// NEAR plane
		idx = 0;
		float[][] near = new float[corners.length/2][];
		for (int k=0; k<corners.length/2; k++) {
			near[idx++] = corners[k];
		}
		fPlaneNorm = avgOf3DPolygon(near);
		fPlaneNormUnit = VectorUtil.normalizeVec3(new float[] {fPlaneNorm[0],fPlaneNorm[1],fPlaneNorm[2]});
		distance = VectorUtil.normVec3(near[0]); // distance
		frus[frustumIdx][X] = -fPlaneNormUnit[X]; // negate to point the near plane normal back at the far plane
		frus[frustumIdx][Y] = -fPlaneNormUnit[Y];
		frus[frustumIdx][Z] = -fPlaneNormUnit[Z];
		frus[frustumIdx][W] = distance;
		
		return frus;
	}
	
	
	
	
	/**
	 * This method takes a 3D cube organized in a 3D array as follows:
	 * The side closest to the definer, and ordered in CCW as viewed from the perspective of the viewer (x1y1z1, x2y2z2, x3y3z3, x4y4z4)
	 * Second array row: The side farthest from the definer, ordered in CCW as viewed from the perspective of the viewer (x5y5z5, x6y6z6, x6y7z7, x8y8z8)
	 * where the remaining edges of the cube are defined edges at the same column locations. Example x1y1z1 and x5y5z5 form an edge. 
	 * x2y2z2 and x6y6z6 would form another edge etc. and creates a 4-sided frustum
	 * @param cube
	 * @return a frustum defined in plane equation form.
	 */
	public static float[][] getFrustumFromCube(float[][] cube) throws IllegalArgumentException {
		if (cube == null) {
			log.aprintln("Cannot create a frustum from a null cube.");
			throw new IllegalArgumentException("Cannot create a frustum from a null cube.");
		}
		if (cube.length !=8) {
			log.aprintln("Cannot create a frustum from a cube with "+cube.length+" rows.");
			throw new IllegalArgumentException("Cannot create a frustum from a cube with "+cube.length+" rows.");
		}
		float[][] frus = new float[6][4];
		int frustumIdx = 0;
		
		float[] planeNorm = new float[3];
		float[] planePt = new float[3];
		//side 2,6,5 the right side
		if (triPlaneNormalPoint(new float[][] {cube[2], cube[6], cube[5]}, planeNorm, planePt)) {
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
		
		//side 0,4,7 left side
		if (triPlaneNormalPoint(new float[][] {cube[0], cube[4], cube[7]}, planeNorm, planePt)) {
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
		
		//side 1,5,4 bottom side
		if (triPlaneNormalPoint(new float[][] {cube[1], cube[5], cube[4]}, planeNorm, planePt)) {
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

		//side 2,3,7 top side
		if (triPlaneNormalPoint(new float[][] {cube[2], cube[3], cube[7]}, planeNorm, planePt)) {
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

		//side 5,6,7 far side
		if (triPlaneNormalPoint(new float[][] {cube[5], cube[6], cube[7]}, planeNorm, planePt)) {
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


		// 3,2,1 near side
		if (triPlaneNormalPoint(new float[][] {cube[3], cube[2], cube[1]}, planeNorm, planePt)) {
			float distance = FloatUtil.abs(VectorUtil.dotVec3(planePt, planeNorm));
			frus[frustumIdx][X] = planeNorm[X];
			frus[frustumIdx][Y] = planeNorm[Y];
			frus[frustumIdx][Z] = planeNorm[Z];
			frus[frustumIdx][W] = distance;
		} else {
			return null;
		}
		
		return frus;
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
	public static boolean pointInFrustum(float[] pt, float[][] frustum) {
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
	 * Tests whether the given SpatialRenderable can fit in the box centered at (cx, cy, cz),
	 * with side dimensions of halfSize * 2.
	 *
	 * @param t the SpatialRenderable to test
	 * @param cx x dimension of the box
	 * @param cy y dimension of the box
	 * @param cz z dimension of the box
	 * @param halfSize the size of half of a node at a particular level, the distance from the center to the closest point of any side 
	 * @return true if the SpatialRenderable fits entirely inside the box.
	 *
	 * thread-safe?
	 */
	public static boolean fitsInBox(SpatialRenderable t, float cx, float cy, float cz, float halfSize)
	{
		float[][] points = t.getPoints();
		
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
	 public static Visibility checkBoxAgainstFrustum(float cx, float cy, float cz, float halfSize, float[][] frustum)
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
	  * Determines whether a given SpatialRenderable is in, out, or partially inside a given JOGL frustum
	  *
	  * @param t the SpatialRenderable to test
	  * @param frustum the frustum to compare against
	  * @return the visibility of the SpatialRenderable
	  *
	  * thread-safe
	  */
	 public static Visibility checkAgainstJoglFrustum(SpatialRenderable t, com.jogamp.opengl.math.geom.Frustum frustum) {
		    float[][] points = t.getPoints();
		    int pointInCount = 0;
		    
			for (int p=0; p<points.length; p++ ) {
				boolean isOut = frustum.isPointOutside(points[p]);
				if (!isOut) {
					pointInCount++;
				}
			}
		      
		    if (pointInCount == points.length) {
		    	return Visibility.NoClip;
		    } else if (pointInCount > 0) {
		    	return Visibility.SomeClip;
		    } else {
		    	return Visibility.NotVisible;
		    }
	 }	 
		 
	 /*
	  * Determines whether a given SpatialRenderable is in, out, or partially inside a given frustum
	  *
	  * @param t the SpatialRenderable to test
	  * @param frustum the frustum to compare against
	  * @return the visibility of the SpatialRenderable
	  *
	  * thread-safe
	  */
	 public static Visibility checkAgainstFrustum(SpatialRenderable t, float[][] frustum) {
	    int f, p;
	    float[][] points = t.getPoints();
	    int pointInCount = 0;
	    
	    for (f=0; f<6; f++)
	    {
	      for (p=0; p<points.length; p++ ) {
	        if (frustum[f][0] * points[p][X] + frustum[f][1] * points[p][Y] + frustum[f][2] * points[p][Z] + frustum[f][3] > 0) {
	        	pointInCount++;
	        }
	      }
	    }
	    if (pointInCount == (points.length * 6)) {
	    	return Visibility.NoClip;
	    } else if (pointInCount > 0) {
	    	return Visibility.SomeClip;
	    } else {
	    	return Visibility.NotVisible;
	    }
	 }	
	 
	 /*
	  * Determines whether a given SpatialRenderable is in, out, or partially inside a given frustum with any number of sides
	  *
	  * @param t the SpatialRenderable to test
	  * @param frustum the frustum to compare against
	  * @return the visibility of the SpatialRenderable
	  *
	  * thread-safe
	  */
	 public static Visibility checkAgainstAnyFrustum(SpatialRenderable t, float[][] frustum) {
	    int f, p;
	    float[][] points = t.getPoints();
	    int pointInCount = 0;
	    
	    for (f=0; f<frustum.length; f++)
	    {
	      for (p=0; p<points.length; p++ ) {
	        if (frustum[f][0] * points[p][X] + frustum[f][1] * points[p][Y] + frustum[f][2] * points[p][Z] + frustum[f][3] > 0) {
	        	pointInCount++;
	        }
	      }
	    }
	    if (pointInCount == frustum.length * points.length) {
	    	return Visibility.NoClip;
	    } else if (pointInCount > 0) {
	    	return Visibility.SomeClip;
	    } else {
	    	return Visibility.NotVisible;
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

	 
	 
		public static int cuboidInAnyFrustum(Cuboid cube, float[][] frustum) {
		   int p;
		   int c;
		   int c2 = 0;
		   float x = cube.getCenter()[0]; 
		   float y = cube.getCenter()[1];; 
		   float z = cube.getCenter()[2];; 
		   float xSize = cube.getSize()[0];
		   float ySize = cube.getSize()[1];
		   float zSize = cube.getSize()[2];

		   for( p = 0; p < frustum.length; p++ ) {
		      c = 0;
		      if( frustum[p][0] * (x - xSize) + frustum[p][1] * (y - ySize) + frustum[p][2] * (z - zSize) + frustum[p][3] > 0 ) {
		         c++;
		      }
		      if( frustum[p][0] * (x + xSize) + frustum[p][1] * (y - ySize) + frustum[p][2] * (z - zSize) + frustum[p][3] > 0 ) {
		         c++;
		      }
		      if( frustum[p][0] * (x - xSize) + frustum[p][1] * (y + ySize) + frustum[p][2] * (z - zSize) + frustum[p][3] > 0 ) {
		         c++;
		      }
		      if( frustum[p][0] * (x + xSize) + frustum[p][1] * (y + ySize) + frustum[p][2] * (z - zSize) + frustum[p][3] > 0 ) {
		         c++;
		      }
		      if( frustum[p][0] * (x - xSize) + frustum[p][1] * (y - ySize) + frustum[p][2] * (z + zSize) + frustum[p][3] > 0 ) {
		         c++;
		      }
		      if( frustum[p][0] * (x + xSize) + frustum[p][1] * (y - ySize) + frustum[p][2] * (z + zSize) + frustum[p][3] > 0 ) {
		         c++;
		      }
		      if( frustum[p][0] * (x - xSize) + frustum[p][1] * (y + ySize) + frustum[p][2] * (z + zSize) + frustum[p][3] > 0 ) {
		         c++;
		      }
		      if( frustum[p][0] * (x + xSize) + frustum[p][1] * (y + ySize) + frustum[p][2] * (z + zSize) + frustum[p][3] > 0 ) {
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

//		/**
//		 * (ref: https://math.stackexchange.com/questions/2563909/find-points-on-a-plane).
//		 * @param plane
//		 * @return
//		 */
//		public static HVector ptOnPlane(double[] plane) {
//			
//		}
		
		/**
		 * Computes intersection point of a bundle of planes.
		 * A bundle of planes is a set of planes sharing a point in common
		 * (ref: http://mathworld.wolfram.com/BundleofPlanes.html).
		 * @param plane1 Plane 1 (A,B,C,D)
		 * @param plane2 Plane 2 (A,B,C,D)
		 * @param plane3 Plane 3 (A,B,C,D)
		 * @return Intersection point.
		 */
		public static HVector intersect3(double[] plane1, double[] plane2, double[] plane3) {
			double[] p1 = new double[1], p2 = new double[1], p3 = new double[1];
			HVector n1 = planeNormal(plane1,p1).unit();
			HVector n2 = planeNormal(plane2,p2).unit();
			HVector n3 = planeNormal(plane3,p3).unit();
			
			HVector x1 = n1.mul(-p1[0]);
			HVector x2 = n2.mul(-p2[0]);
			HVector x3 = n3.mul(-p3[0]);
			
			HVector v = n2.cross(n3).mul(x1.dot(n1)).add(n3.cross(n1).mul(x2.dot(n2))).add(n1.cross(n2).mul(x3.dot(n3)));
			
			Matrix3d m3d = new Matrix3d(n1.x, n1.y, n1.z, n2.x, n2.y, n2.z, n3.x, n3.y, n3.z);
			m3d.transpose();
			double det = m3d.determinant();
			
			if (det == 0) { // determinant == 0 means two planes are parallel
				throw new IllegalArgumentException("zero determinant - two planes are parallel");
			}
			
			v.divEq(det);
			
			return v;
		}
		
		// Bundle of planes: A set of planes sharing a point in common.
		// From mathworld (Hessian Normal Form):
		//   nx = a/sqrt(a^2+b^2+c^2); ny = b/sqrt(a^2+b^2+c^2); nz = c/sqrt(a^2+b^2+c^2); p = d/sqrt(a^2+b^2+c^2);
		// See: http://mathworld.wolfram.com/Plane-PlaneIntersection.html
		// See: https://stackoverflow.com/questions/32597503/finding-the-intersection-point-of-a-bundle-of-planes-3-in-three-js
		
		/**
		 * Computes normal to the specified plane.
		 * (ref: http://mathworld.wolfram.com/HessianNormalForm.html)
		 * @param plane 4-element vector containing A, B, C, D for plane equation Ax+By+Cz+D=0.
		 * @param p outputs constant in the Hessian Normal Form in the zero'th element
		 *   (containing distance of plane to the origin), if non-null.
		 * @return Normal to the plane.
		 */
		public static HVector planeNormal(double[] plane, double[] p) {
			double a2b2c2 = Math.sqrt(Math.pow(plane[0],2)+Math.pow(plane[1],2)+Math.pow(plane[2],2));
			HVector n = new HVector(plane[0]/a2b2c2, plane[1]/a2b2c2, plane[2]/a2b2c2);
			if (p != null) {
				p[0] = plane[3]/a2b2c2;
			}
			return n;
		}
		

}
