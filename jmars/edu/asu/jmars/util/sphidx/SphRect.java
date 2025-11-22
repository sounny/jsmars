/**
 * 
 */
package edu.asu.jmars.util.sphidx;

import java.awt.geom.Point2D;

import edu.asu.jmars.util.HVector;

class SphRect implements SphShape {
	/* corner vectors from origin */
	private HVector pts[];
	
	/* norm[i] = pts[i] x pts[i+1] */
	private HVector norms[];
	
	/**
	 * @param pts Corner-points of spherical-triangle in
	 *    counter clock-wise direction.
	 */
	public SphRect(HVector pts[]){
		if (pts.length != 4)
			throw new IllegalArgumentException("Expecting 4 points, got "+pts.length+".");
		
		this.pts = new HVector[4];
		for(int i=0; i<this.pts.length; i++)
			this.pts[i] = (HVector)pts[i].clone();
		this.norms = new HVector[this.pts.length];
		for(int i=0; i<this.pts.length; i++)
			this.norms[i] = pts[i].cross(pts[(i+1)%this.pts.length]);
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
	
	public boolean contains(HVector pt){
		for(int i=0; i<pts.length; i++){
			if (norms[i].dot(pt) < 0)
				return false;
		}
		return true;
	}
	
	public boolean intersects(SphTri sphPoly){
		HVector[] theirPts = sphPoly.getPts();
		for(int i=0; i<theirPts.length; i++)
			if (contains(theirPts[i]))
				return true;
		for(int i=0; i<pts.length; i++)
			if (sphPoly.contains(pts[i]))
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
	
	public SphTri[] triangulate(){
		return new SphTri[]{
			new SphTri(new HVector[]{ pts[0], pts[1], pts[2] }),
			new SphTri(new HVector[]{ pts[2], pts[3], pts[0] }),
		};
	}
	
	public String toString(){
		Point2D pt = new Point2D.Double();
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(getClass().getSimpleName()+"[");
		for(int i=0; i<pts.length; i++){
			double lon = pts[i].lonE(), lat = pts[i].latC();
			pts[i].toLonLat(pt);
			sbuf.append(i==0?"":",");
			sbuf.append("("+lon+","+lat+")");
		}
		sbuf.append("]");
		return sbuf.toString();
	}
	
	public static void main(String[] args){
		
	}
}