package edu.asu.jmars.viz3d.util;

import java.util.ArrayList;

import com.jogamp.opengl.math.VectorUtil;

import edu.asu.jmars.Main;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.asu.jmars.viz3d.core.geometry.Ray;
import edu.asu.jmars.viz3d.core.geometry.Triangle;
import edu.asu.jmars.viz3d.spatial.RayWithEpsilon;
import edu.asu.jmars.viz3d.spatial.SpatialRenderable;

public class Utils {
	
    public static boolean isWindowsOS()
    {
        String os = System.getProperty("os.name");
        
        if (os != null && os.toUpperCase().contains("WINDOWS")) {
        	return true;
        } else {
        	return false;
        }
    }
    
    public static void printHierarchy(Class<?> c) {
        Class<?> c1 = c;
        String cname = c1.getName();        
        System.out.println(cname);
        Class<?> sc = c1.getSuperclass();
        while (sc != null) {
            cname = sc.getName();
            System.out.println(cname);
            c1 = sc;
            sc = c1.getSuperclass();
        }
    }
    
//   	final static float TESSELLATION_LIMIT = 0.0005f;
   	final static float TESSELLATION_LIMIT = 0.005f;

	
 	// This method subdivides a triangle to its smallest limit
	public static ArrayList<float[]> subdivideTri(float[][] tri, ArrayList<float[]> tris) {	
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
 
	/**
	 * Method to test if an intersected SpatialRenderable is occulted by the primary shape model
	 * @param renderable
	 * @param rayEps
	 * @return true if occulted
	 */
	public static boolean isOccultedByShapeModel(SpatialRenderable renderable, RayWithEpsilon rayEps) {
		boolean result = false;
		ThreeDManager mgr = null;
		if (Main.testDriver != null) {
			mgr = ThreeDManager.getInstance();
		} else {
			mgr = ThreeDManager.getInstanceNoJmars();
		}
		
		if (mgr != null && mgr.hasShapeModel()) {
			Ray ray = new Ray(rayEps.getOrigin(), rayEps.getDirection(), rayEps.getEnd());
			
			ArrayList<Triangle> selectedTri = new ArrayList<>();
			// TODO this is oblique distance, we want distance orthogonal to the near plane - also how to handle long lines with varying distance
			float distToLineSeg = com.jogamp.opengl.math.VectorUtil.distVec3(rayEps.getOrigin(), renderable.getCenter());
			
			if (ThreeDManager.getInstance().getShapeModel().rayIntersect(ray, selectedTri)) {
				for (Triangle t : selectedTri) {
					float distToFacet = com.jogamp.opengl.math.VectorUtil.distVec3(rayEps.getOrigin(), t.getIntersection());
					if (distToFacet < distToLineSeg) {
						result = true;
					}
				}
			}
		}
		
		return result;
	}

}
