/**
 * 
 */
package edu.asu.jmars.util.sphidx;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.asu.jmars.util.HVector;

class SphTri implements SphShape {
	/* corner vectors from origin */
	private HVector pts[];
	
	/* norm[i] = pts[i] x pts[i+1] */
	private HVector norms[];
	
	/**
	 * Create a spherical triangle with corner-points specified
	 * in counter clock-wise direction.
	 * @param pts Corner-points of spherical-triangle in
	 *    counter clock-wise direction.
	 */
	public SphTri(HVector pts[]){
		if (pts.length != 3)
			throw new IllegalArgumentException("Expecting three points, got"+pts.length+".");
		
		this.pts = new HVector[3];
		for(int i=0; i<this.pts.length; i++)
			this.pts[i] = (HVector)pts[i].clone();
		this.norms = new HVector[this.pts.length];
		for(int i=0; i<this.pts.length; i++)
			this.norms[i] = pts[i].cross(pts[(i+1)%this.pts.length]);
	}
	
	/**
	 * Create a spherical triangle with corner-points specified
	 * in counter clock-wise direction.
	 * @param p1
	 * @param p2
	 * @param p3
	 */
	public SphTri(HVector p1, HVector p2, HVector p3){
		this(new HVector[]{p1, p2, p3});
	}
	
	/**
	 * @return Corner vectors from origin in counter-clock-wise
	 * direction.
	 */
	public HVector[] getPts(){
		return pts;
	}
	
	public HVector[] getNorms(){
		return norms;
	}
	
	final public boolean contains(HVector pt){
		// TODO Figure out a way for making closed on some sides
		//      and open on others, just like in a rectangle
		//      two sides have >= test and the other two have
		//      a < test for coordinates. This will ensure 
		//      that the point is only contained in one of
		//      the spherical-triangles if it falls on the boundary.
		for(int i=0; i<3; i++){
			if (norms[i].dot(pt) < 0)
				return false;
		}
		return true;
	}
	
	/**
	 * Checks whether the given spherical-triangle intersects
	 * this spherical-triangle.
	 * @param sphTri
	 * @return <code>true</code> if they share any area,
	 *    <code>false</code> otherwise.
	 */
	final public boolean intersects(SphTri sphTri){
		HVector[] theirPts = sphTri.getPts();
		for(int i=0; i<theirPts.length; i++)
			if (contains(theirPts[i]))
				return true;
		for(int i=0; i<pts.length; i++)
			if (sphTri.contains(pts[i]))
				return true;

		for(int j=0; j<pts.length; j++){
			HVector b1 = pts[j], b2 = pts[(j+1)%pts.length];
			for(int i=0; i<theirPts.length; i++){
				HVector a1 = theirPts[i], a2 = theirPts[(i+1)%theirPts.length];
				if (HVector.intersectGSeg(a1,a2,b1,b2) != null)
					return true;
			}
		}
		return false;
	}

	public static Set<SphTri> intersects(Collection<SphTri> subTri, SphTri sphTri){
		Set<SphTri> out = new HashSet<SphTri>();
		
		for(SphTri t: subTri)
			if (t.intersects(sphTri))
				out.add(t);
		
		return out;
	}
	
	public static boolean contains(Collection<SphTri> subTri, SphTri sphTri){
		List<HVector> pts = new ArrayList<HVector>(Arrays.asList(sphTri.getPts()));
		for(SphTri t: subTri){
			for(int j=0; j<pts.size();)
				if (t.contains(pts.get(j)))
					pts.remove(j);
				else
					j++;
			if (pts.isEmpty())
				break;
		}
		return pts.isEmpty();
	}
	
	/**
	 * Checks whether the given spherical-triangle is completely
	 * contained with this spherical triangle.
	 * @param sphTri
	 * @return <code>true</code> if the specified spherical-triangle
	 *    is fully contained within this spherical triangle,
	 *    <code>false</code> otherwise.
	 */
	final public boolean contains(SphTri sphTri){
		HVector[] theirPts = sphTri.getPts();
		
		for(int i=0; i<theirPts.length; i++)
			if (!contains(theirPts[i]))
				return false;
		return true;
	}
	
	public SphTri[] triangulate(){
		return new SphTri[]{this};
	}
	
	/**
	 * 
	 * @return <code>true</code> if the points are collinear or any two points
	 *    of the triangle are coincident.
	 */
	public boolean isDegenerate(){
		if (pts[0].equals(pts[1]) || pts[0].equals(pts[2]) || pts[1].equals(pts[2]))
			return true;
		if (norms[0].dot(pts[1]) == 0)
			return true;
		return false;
	}
	
	public SphTri clone() throws CloneNotSupportedException {
		SphTri clone = (SphTri)super.clone();
		
		clone.pts = new HVector[pts.length];
		for(int i=0; i<pts.length; i++)
			clone.pts[i] = (HVector)pts[i].clone();
		
		clone.norms = new HVector[norms.length];
		for(int i=0; i<norms.length; i++)
			clone.norms[i] = (HVector)norms[i].clone();
		
		return clone;
	}
	
	public String toString(){
		Point2D pt = new Point2D.Double();
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(getClass().getSimpleName()+"[");
		for(int i=0; i<pts.length; i++){
			double lon = pts[i].lonE(), lat = pts[i].latC();
			pts[i].toLonLat(pt);
			sbuf.append(i==0?"":",");
			sbuf.append(String.format("(%g,%g)",lon,lat));
		}
		sbuf.append("]");
		return sbuf.toString();
	}
	
	public static void main(String[] args){
		SphTri t1 = new SphTri(new HVector[]{
			HVectorUtil.fromSpatialE(0, 0),	
			HVectorUtil.fromSpatialE(10, 0),	
			HVectorUtil.fromSpatialE(5, 5),
		});
		HVector pts[] = new HVector[]{
			HVectorUtil.fromSpatialE(0, 0),
			HVectorUtil.fromSpatialE(10, 0),
			HVectorUtil.fromSpatialE(5, 5),
			HVectorUtil.fromSpatialE(5, 0),
			HVectorUtil.fromSpatialE(3, 3),
		};
		for(HVector pt: pts){
			System.out.println("pt: "+t1.contains(pt));
		}
	}
}