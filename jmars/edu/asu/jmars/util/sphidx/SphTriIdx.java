package edu.asu.jmars.util.sphidx;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.asu.jmars.util.HVector;

public class SphTriIdx {
	Node top[];
	
	public SphTriIdx(){
		top = new Node[8];
		for(int i=0; i<4; i++){
			top[i] = new Node(new SphTri(new HVector[]{
					HVectorUtil.fromSpatialE(i*90, 90),
					HVectorUtil.fromSpatialE(i*90, 0),
					HVectorUtil.fromSpatialE((i+1)*90, 0),
			}));
		}
		for(int i=4; i<8; i++){
			top[i] = new Node(new SphTri(new HVector[]{
					HVectorUtil.fromSpatialE(i*90, -90),
					HVectorUtil.fromSpatialE((i+1)*90, 0),
					HVectorUtil.fromSpatialE(i*90, 0),
			}));
		}
	}
	
	public Node[] getTopLevelNodes(){
		return top;
	}
	
	private void recursiveInsert(Node node, HVector pt){
		if (node.sub == null){
			if (node.contents == null){
				node.contents = new ArrayList<HVector>();
				node.contents.add(pt);
			}
			else if (node.contents.contains(pt)){
				node.contents.add(pt);
			}
			else {
				node.sub = new Node[4];
				
				HVector pts[] = node.triangle.getPts();
				HVector m02 = pts[0].interpolate(pts[2], 0.5);
				HVector m21 = pts[2].interpolate(pts[1], 0.5);
				HVector m10 = pts[1].interpolate(pts[0], 0.5);
				
				node.sub[0] = new Node(new SphTri(new HVector[]{ m02,    m10,    m21    }));
				node.sub[1] = new Node(new SphTri(new HVector[]{ pts[0], m10,    m02    }));
				node.sub[2] = new Node(new SphTri(new HVector[]{ m02,    m21,    pts[2] }));
				node.sub[3] = new Node(new SphTri(new HVector[]{ m10,    pts[1], m21    }));
				
				List<HVector> contents = node.contents;
				contents.add(pt);
				for(Node n: node.sub){
					for(HVector p: contents){
						if (n.triangle.contains(p))
							recursiveInsert(n, p);
					}
				}
				node.contents = null;
			}
		}
		else {
			for(int i=0; i<node.sub.length; i++){
				if (node.sub[i].triangle.contains(pt)){
					recursiveInsert(node.sub[i], pt);
				}
			}
		}
	}
	
	public void insert(HVector pt){
		for(int i=0; i<top.length; i++){
			if (top[i].triangle.contains(pt)){
				recursiveInsert(top[i], pt);
			}
		}
	}
	
	private List<HVector> recursiveFind(SphRect sphRect, Node node, List<SphTri> overlap){
		List<HVector> ptsList = new ArrayList<HVector>();
		
		if (sphRect.intersects(node.getTriangle())){
			if (node.contents != null){
				for(HVector pt: node.contents)
					if (sphRect.contains(pt))
						ptsList.add(pt);
			}
			if (node.sub != null){
				for(Node n: node.sub)
					ptsList.addAll(recursiveFind(sphRect, n, overlap));
			}
			else {
				if (overlap != null)
					overlap.add(node.getTriangle());
			}
		}
		
		return ptsList;
	}
	
	public List<HVector> find(SphRect sphRect, List<SphTri> overlap){
		List<HVector> ptsList = new ArrayList<HVector>();
		for(int i=0; i<top.length; i++)
			ptsList.addAll(recursiveFind(sphRect, top[i], overlap));
		return ptsList;
	}
	
	static class Node {
		SphTri triangle;
		Node sub[];
		
		List<HVector> contents;
		
		public Node(SphTri triangle){
			this.triangle = triangle;
		}
		
		public SphTri getTriangle(){
			return triangle;
		}
		
		public Node[] getSubNodes(){
			return sub;
		}
		
		private List<Point2D> from(List<HVector> vList){
			List<Point2D> ptsList = new ArrayList<Point2D>();
			if (vList != null){
				for(HVector v: vList)
					ptsList.add(v.toLonLat(null));
			}
			return ptsList;
		}
		
		public String toString(){
			return getClass().getSimpleName()+"["+
				"triangle="+triangle.toString()+","+
				"contents="+from(contents)+
				"]";
		}
	}
	
	public static List<Point2D> toLatLon(List<HVector> vecs){
		List<Point2D> pts = new ArrayList<Point2D>();
		for(HVector v: vecs){
			Point2D pt = new Point2D.Double();
			v.toLonLat(pt);
			pts.add(pt);
		}
		return pts;
	}
	
	public static SphRect[] getRects(){
		SphRect r1 = new SphRect(new HVector[]{
				HVectorUtil.fromSpatialE(0,80),
				HVectorUtil.fromSpatialE(90,80),
				HVectorUtil.fromSpatialE(180,80),
				HVectorUtil.fromSpatialE(270,80),
		});
		
		SphRect r2 = new SphRect(new HVector[]{
				HVectorUtil.fromSpatialE(5,5),
				HVectorUtil.fromSpatialE(25,5),
				HVectorUtil.fromSpatialE(25,25),
				HVectorUtil.fromSpatialE(5,25),
		});
		
		return new SphRect[]{ r1, r2 };
	}
	
	public static void main(String[] args){
		HVector[] p = new HVector[]{
				HVectorUtil.fromSpatialE(0, 0),
				HVectorUtil.fromSpatialE(0, 0),
				HVectorUtil.fromSpatialE(10, 10),
				HVectorUtil.fromSpatialE(20, 20),
				HVectorUtil.fromSpatialE(30, 30)
		};
		SphTriIdx idx = new SphTriIdx();
		for(int i=0; i<p.length; i++)
			idx.insert(p[i]);
		
		System.out.println("input:"+toLatLon(Arrays.asList(p)));
		
		SphRect[] sphRects = getRects();
		for(int i=0; i<sphRects.length; i++)
			System.out.println(sphRects[i]+"=>"+toLatLon(idx.find(sphRects[i], null)));
	}
}
