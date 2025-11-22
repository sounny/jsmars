package edu.asu.jmars.util.sphidx;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import edu.asu.jmars.util.HVector;
import edu.emory.mathcs.backport.java.util.Arrays;

public class SphPoly implements SphShape {
	private HVector[] pts;
	private HVector[] norms;
	private SphTri[] triangulation = null;
	
	public SphPoly(HVector[] pts){
		this.pts = new HVector[pts.length];
		for(int i=0; i<this.pts.length; i++)
			this.pts[i] = (HVector)pts[i].clone();
		
		this.norms = new HVector[this.pts.length];
		for(int i=0; i<this.pts.length; i++)
			this.norms[i] = pts[i].cross(pts[(i+1)%this.pts.length]);
	}
	
	public boolean contains(HVector pt) {
		SphTri[] subTri = triangulate();
		for(int i=0; i<subTri.length; i++)
			if (!subTri[i].contains(pt))
				return false;
		return true;
	}
	
	public boolean intersects(SphTri tri){
		SphTri[] subTri = triangulate();
		for(int i=0; i<subTri.length; i++)
			if (subTri[i].intersects(tri))
				return true;

		return false;
	}

	public boolean contains(SphTri tri){
		List<HVector> pts = new ArrayList<HVector>(Arrays.asList(tri.getPts()));
		SphTri[] subTri = triangulate();
		for(int i=0; i<subTri.length && !pts.isEmpty(); i++)
			for(int j=0; j<pts.size();)
				if (subTri[i].contains(pts.get(j)))
					pts.remove(j);
				else
					j++;
		return pts.isEmpty();
	}
	
	public HVector[] getNorms() {
		return norms;
	}

	public HVector[] getPts() {
		return pts;
	}
	
	private final int mod(int i, int n){
		if (i<0)
			return (i-(i/n-1)*n)%n;
		return i%n;
	}
	
	public SphPoly simplify(double dist, List<SphTri> removedEars){
		List<HVector> ptsList = new ArrayList<HVector>(Arrays.asList(this.pts));
		int i=0;
		while(i<ptsList.size()){
			HVector pp = ptsList.get(mod(i-1,ptsList.size()));
			HVector p  = ptsList.get(mod(i,  ptsList.size()));
			HVector pn = ptsList.get(mod(i+1,ptsList.size()));
			HVector n  = pp.cross(pn).unit();

			if (p.projOnto(n).norm() <= dist){
				if (n.dot(p) < 0){
					i++;
				}
				else {
					if (removedEars != null)
						removedEars.add(new SphTri(new HVector[]{ pn, p, pp }));
					ptsList.remove(mod(i, ptsList.size()));
					i--;
				}
			}
			else {
				i++;
			}
		}

		return new SphPoly(ptsList.toArray(new HVector[0]));
	}
	
	private int findExtrusion(List<HVector> pts){
		for(int i=0; i<pts.size(); i++){
			if (pts.get(i).cross(pts.get((i+2)%pts.size())).dot(pts.get((i+1)%pts.size())) < 0){
				SphTri tri = new SphTri(new HVector[]{
						pts.get(i), pts.get((i+1)%pts.size()), pts.get((i+2)%pts.size()),
				});
				
				// TODO n-squared
				boolean passed = true;
				for(int j=0; j<(pts.size()-3) && passed; j++){
					if (tri.contains(pts.get((j+(i+3))%pts.size())))
						passed = false;
				}
				if (passed)
					return i;
			}
		}
		return -1;
	}
	
	private class AreaComparator implements Comparator<HVector> {
		List<HVector> pts;
		
		public AreaComparator(List<HVector> pts){
			this.pts = pts;
		}
		
		/**
		 * Compute approximate area of the triangle formed by coordinates
		 * at (i-1, i, i+1) mod n, where n is the total number of
		 * coordinates.
		 * @param i Index of the middle coordinate of triangle.
		 * @return
		 */
		private double getArea(int i){
			HVector[] p = new HVector[]{
					pts.get(mod(i-1,pts.size())),
					pts.get(mod(i  ,pts.size())),
					pts.get(mod(i+1,pts.size()))
			};
			
			double area = p[1].sub(p[0]).cross(p[2].sub(p[1])).norm();
			//System.err.println("Area["+i+"]: "+area);
			return area;
		}
		
		private boolean isConvex(int i){
			HVector[] p = new HVector[]{
					pts.get(mod(i-1,pts.size())),
					pts.get(mod(i  ,pts.size())),
					pts.get(mod(i+1,pts.size()))
			};
			
			boolean convex = p[0].cross(p[2]).dot(p[1]) <= 0;
			//System.err.println("Convex["+i+"]: "+convex);
			return convex;
		}
		
		private boolean hasConcaveEnd(int i){
			return (!isConvex(i-1) || !isConvex(i+1));
		}
		
		public int compare(HVector o1, HVector o2) {
			int i1 = pts.indexOf(o1), i2 = pts.indexOf(o2);
			boolean c1 = isConvex(i1), c2 = isConvex(i2);
			
			if (c1 && c2){
				// Any convex 3-point segment with one of the
				// end-points being a concave 3-point segment
				//boolean hasConcave1 = hasConcaveEnd(i1);
				//boolean hasConcave2 = hasConcaveEnd(i2);
				//if (hasConcave1 && hasConcave2){
					// Smallest area first
					return (int)Math.signum(getArea(i1)-getArea(i2));
				//}
				//else if (hasConcave1 && !hasConcave2){
				//	return -1;
				//}
				//else if (!hasConcave1 && hasConcave2){
				//	return 1;
				//}
				//else {
				//	return 0;
				//}
			}
			
			// Convex first
			if (c1 && !c2)
				return -1;
			return 1; // (!c1 && c2)
		}
	}
	
	public SphTri[] triangulate_old(){
		List<SphTri> result = new ArrayList<SphTri>();
		List<HVector> pts = new ArrayList<HVector>(Arrays.asList(this.pts));
		SphPoly rest = new SphPoly(pts.toArray(new HVector[0]));
		
		while(pts.size() > 2){
			int loc = findExtrusion(pts);
			if (loc < 0)
				throw new RuntimeException("Triangulation failed. left:"+pts+" soFar:"+result);
			SphTri tri = new SphTri(new HVector[]{
					pts.get(loc), pts.get((loc+1)%pts.size()), pts.get((loc+2)%pts.size())
			});
			result.add(tri);
			pts.remove((loc+1)%pts.size());
			rest = new SphPoly(pts.toArray(new HVector[0]));
			System.err.println(">> Removed: "+tri+" Rest: "+rest);
		}
		return result.toArray(new SphTri[0]);
	}
	
	private List<Integer> makeIntList(int start, int step, int count){
		List<Integer> lst = new ArrayList<Integer>(count);
		for(int i=0; i<count; i++)
			lst.add(new Integer(start+i*step));
		return lst;
	}
	
	private List<Integer> mod(List<Integer> vals, int n){
		List<Integer> lst = new ArrayList<Integer>(vals.size());
		for(Integer i: vals)
			lst.add(mod(i,n));
		return vals;
	}
	
	public SphTri[] triangulate(){
		if (triangulation != null)
			return triangulation;
		
		List<SphTri> result = new ArrayList<SphTri>();
		List<HVector> pts = new ArrayList<HVector>(Arrays.asList(this.pts));
		//SphPoly rest = new SphPoly(pts.toArray(new HVector[0]));
		
		PriorityQueue<HVector> pq = new PriorityQueue<HVector>(pts.size(), new AreaComparator(pts));
		pq.addAll(pts);
		
		while(pq.size() > 2){
			int loc = pts.indexOf(pq.peek());
			SphTri tri = new SphTri(new HVector[]{
					pts.get(mod(loc-1,pts.size())),
					pts.get(mod(loc  ,pts.size())),
					pts.get(mod(loc+1,pts.size()))
			});
			result.add(tri);
			pts.remove(mod(loc,pts.size()));
			//rest = new SphPoly(pts.toArray(new HVector[0]));
			//System.err.println(">> Removed @ "+loc+": "+tri+" Rest: "+rest);
			pq.remove();
		}
		
		return triangulation = result.toArray(new SphTri[0]);
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
	
	static List<SphTri> filterNoArea(List<SphTri> inList){
		List<SphTri> outList = new ArrayList<SphTri>(inList.size());
		for(SphTri tri: inList){
			if (!tri.isDegenerate())
				outList.add(tri);
		}
		
		return outList;
	}
	
	public static void main(String[] args){
		SphPoly p3 = new SphPoly(new HVector[]{
				HVectorUtil.fromSpatialE(0, 80),
				HVectorUtil.fromSpatialE(90, 80),
				HVectorUtil.fromSpatialE(180, 80),
				HVectorUtil.fromSpatialE(270, 80),
			});
		boolean result = p3.contains(new SphTri(
				HVectorUtil.fromSpatialE(0, 85),
				HVectorUtil.fromSpatialE(90, 85),
				HVectorUtil.fromSpatialE(90, 90)));
		System.out.println(result);
		
		if (true)
			System.exit(1);
		
		SphPoly poly = new SphPoly(new HVector[]{
				HVectorUtil.fromSpatialE(0, 0),
				HVectorUtil.fromSpatialE(10, 0),
				HVectorUtil.fromSpatialE(10, 10),
				HVectorUtil.fromSpatialE(5, 10),
				HVectorUtil.fromSpatialE(0, 10),
				HVectorUtil.fromSpatialE(1, 9),
				HVectorUtil.fromSpatialE(0, 8),
				HVectorUtil.fromSpatialE(2, 7),
				HVectorUtil.fromSpatialE(0, 6),
				HVectorUtil.fromSpatialE(1, 5),
				HVectorUtil.fromSpatialE(0, 4),
				HVectorUtil.fromSpatialE(1, 3),
				HVectorUtil.fromSpatialE(0, 2),
				HVectorUtil.fromSpatialE(1, 1),
		});
		
		SphPoly poly2 = new SphPoly(new HVector[]{
			HVectorUtil.fromSpatialE(6,0),
			HVectorUtil.fromSpatialE(7,1),
			HVectorUtil.fromSpatialE(5,3),
			HVectorUtil.fromSpatialE(9,1),
			HVectorUtil.fromSpatialE(10,5),
			HVectorUtil.fromSpatialE(11,1),
			HVectorUtil.fromSpatialE(15,3),
			HVectorUtil.fromSpatialE(13,1),
			HVectorUtil.fromSpatialE(14,0),
			HVectorUtil.fromSpatialE(16,4),
			HVectorUtil.fromSpatialE(12,2),
			HVectorUtil.fromSpatialE(10,6),
			HVectorUtil.fromSpatialE(8,2),
			HVectorUtil.fromSpatialE(4,4),
		});
		System.out.println("poly:"+poly);
		List<SphTri> removedEars;
		//SphPoly p1 = poly.simplify(0.02, removedEars = new ArrayList<SphTri>());
		//System.out.println("p1:"+p1);
		//System.out.println("p1 ears removed:\n\t"+Util.join("\n\t", filterNoArea(removedEars)));
		//System.out.println("p1-ears triangulated: "+Arrays.asList(p1.triangulate()));
		for(SphTri t: poly.triangulate()){
			System.out.println("\t"+t);
		}
		System.out.println();
		
		//SphPoly p2 = poly.simplify(0.5, removedEars = new ArrayList<SphTri>());
		//System.out.println("p2:"+p2);
		//System.out.println("p2 ears removed:\n\t"+Util.join("\n\t", filterNoArea(removedEars)));
		//System.out.println("p2-ears triangulated: "+Arrays.asList(p2.triangulate()));
		//System.out.println();
		
	}
}
