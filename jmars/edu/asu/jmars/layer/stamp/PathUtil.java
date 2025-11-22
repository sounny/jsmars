package edu.asu.jmars.layer.stamp;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.graphics.TransformingIterator;
import edu.asu.jmars.graphics.TransformingIterator.Transformer;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.util.HVector;


/**
 * Given a GeneralPath, returns an ArrayList of ArayLists of Point2Ds, representing the sequences of points contained
 * in the GeneralPath.
 * 
 * The flipLongitude parameter will cause longitudes to be flipped back and forth from west/east leading.
 * 
 * @author sdickens
 *
 */
public class PathUtil {
	public static ArrayList<ArrayList<Point2D>> generalPathToPoints(GeneralPath gp, boolean flipLongitude, boolean normalize) 
	{
		ProjObj po = null;   // Only necessary if we cross the PM... or maybe if points are very far apart?

		if (po==null) {
			FPath fp = new FPath(gp, FPath.SPATIAL_WEST);
			Point2D center = fp.getCenter();
			po = new ProjObj.Projection_OC(center.getX(), center.getY());
		}

		double[] prevcoords = new double[6];
		double[] coords = new double[6]; 
		ArrayList<ArrayList<Point2D>> paths = new ArrayList<ArrayList<Point2D>>();
		ArrayList<Point2D> vertices = null;

		PathIterator pi;
		for (pi = gp.getPathIterator(null); !pi.isDone(); pi.next()){

			switch(pi.currentSegment(coords)){

			case PathIterator.SEG_MOVETO:
				if (vertices!=null) {
					paths.add(vertices);
				}
				for (int i=0; i<6; i++) prevcoords[i]=Integer.MIN_VALUE;
				vertices=new ArrayList<Point2D>();
			case PathIterator.SEG_LINETO:
				if (prevcoords[0]!=Integer.MIN_VALUE && Math.abs(coords[0]-prevcoords[0])>180) {
					// System.out.println("Crosses PM!");
					if (po==null) {
						FPath fp = new FPath(gp, FPath.SPATIAL_WEST);
						Point2D center = fp.getCenter();
						po = new ProjObj.Projection_OC(center.getX(), center.getY());
					}

					Point2D prev=po.convSpatialToWorld(new Point2D.Double(prevcoords[0], prevcoords[1]));
					Point2D cur=po.convSpatialToWorld(new Point2D.Double(coords[0], coords[1]));

					Point2D newPt = null;
					if (prevcoords[0] > coords[0]) {
						double slope = (prev.getY()-cur.getY()) / (prev.getX() - (cur.getX()+360));
						double newLat = -slope*(prev.getX()- 360) + prev.getY();	
						newPt=po.convWorldToSpatial(new Point2D.Double(360, newLat));
						vertices.add(new Point2D.Double(0, newPt.getY())); // How does flipLon and normalize affect this?
						calcIntermediatePoints(vertices, 0, po, 0);
						paths.add(vertices);
						vertices=new ArrayList<Point2D>();
						vertices.add(new Point2D.Double(360, newPt.getY())); // How does flipLon and normalize affect this?
					} else {
						double slope2 = (prev.getY()-cur.getY()) / (prev.getX() - (cur.getX()-360));
						double newLat2 = -slope2*(prev.getX()) + prev.getY();
						newPt=po.convWorldToSpatial(new Point2D.Double(360, newLat2));
						vertices.add(new Point2D.Double(360, newPt.getY()));   // How does flipLon and normalize affect this?
						calcIntermediatePoints(vertices, 0, po, 0);
						paths.add(vertices);
						vertices=new ArrayList<Point2D>();
						vertices.add(new Point2D.Double(0, newPt.getY()));  // How does flipLon and normalize affect this?
					}
					
					// TODO: If this is a line... which we don't know yet :( ... then we're done.  Otherwise we need to do complicated things
					//       to reconnect this correctly.  Maybe do a first run to get points and determine line/polygon, then a second to 
					//       connect things properly?
				}
				vertices.add(new Point2D.Double(adjustLon(coords[0], flipLongitude, normalize), coords[1]));
				System.arraycopy(coords, 0, prevcoords, 0, 6);
				break;

			case PathIterator.SEG_CLOSE:
				if (vertices!=null) {
					vertices.add((Point2D)vertices.get(0));
					calcIntermediatePoints(vertices, 0, po, 0);
					paths.add(vertices);
					vertices=null;
				}
				break;

			default:
			}
		}

		if (vertices!=null && !paths.contains(vertices)) {
			calcIntermediatePoints(vertices, 0, po, 0);
			paths.add(vertices);
		}
		
		return paths;
	}
	
	private static double adjustLon(double lon, boolean flip, boolean normalize) {
		double returnLon = lon;
		
		if (flip) {
			returnLon = 360-returnLon;
		}
		
		if (normalize) {
			returnLon -= Math.floor(returnLon/360)*360;			
		}
		
		return returnLon;
	}
	
	private static double distanceBetween(Point2D a, Point2D b) {
		return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
	}
	
	public static int calcIntermediatePoints(ArrayList<Point2D> points, int index, ProjObj po, int depth) {
//		if (true) return 0;
		if (po==null) {
			System.out.println("PO is null!");
			return 0;
		} else {
//			if (Main.PO!=null) {
//				System.out.println("MainCenterLon: " + Main.PO.getCenterLon());
//				System.out.println("MainCenterLat: " + Main.PO.getCenterLat());
//			}
//			System.out.println("CenterLon: " + po.getCenterLon());
//			System.out.println("CenterLat: " + po.getCenterLat());
//			
		}
		// TODO: Ensure spatialA and spatialB are a) spatial b) normalized c) in the right system
		
		// WYSIWYG
		if (Main.PO!=null) { 
			po=Main.PO;
		}
		
		while (points.size()>index+1 && depth<1000) {
			Point2D worldA = po.convSpatialToWorld(points.get(index));
			Point2D worldB = po.convSpatialToWorld(points.get(index+1));
	
			double diff2 = Math.abs(worldA.getX()-worldB.getX());
			
			if (diff2>180.0) {
				if (worldA.getX()<worldB.getX()) {
					worldA.setLocation(worldA.getX()+360, worldA.getY());
				} else {
					worldB.setLocation(worldB.getX()+360, worldB.getY());
				}
			}
						
			Point2D worldC = new Point2D.Double((worldA.getX() + worldB.getX()) / 2, (worldA.getY() + worldB.getY()) / 2);
			
			HVector a = new HVector(points.get(index));
			HVector b = new HVector(points.get(index+1));
			
			HVector c = a.interpolate(b, 0.5);
			
			Point2D spatialC = c.toLonLat(null);
			
			Point2D spatialCw = po.convWorldToSpatial(worldC);
			
			double diff = distanceBetween(spatialC, spatialCw);
			
			if (diff<0.001) {
				index++;
				continue;
			}
			
			//System.out.println("##################### Adding Point! ##########################");
			depth++;
			points.add(index+1 , spatialCw);			
		}
		return depth;
	}
	
	public static void main(String args[]) {
		ArrayList<Point2D> points = new ArrayList<Point2D>();
//		Point2D a = new Point2D.Double(10,10);
//		Point2D b = new Point2D.Double(20,10);
//		Point2D c = new Point2D.Double(30,10);
		
//		points.add(new Point2D.Double(304.703,73.634));
//		points.add(new Point2D.Double(234.907,74.003));
//		points.add(new Point2D.Double(124.583,68.896));
//		points.add(new Point2D.Double(54.660,70.220));
//		points.add(new Point2D.Double(0.000,81.789));
		
//		points.add(a);
//		points.add(b);
//		points.add(c);
//		points.add(new Point2D.Double(0,0));
//		points.add(new Point2D.Double(45,45));
	
		points.add(new Point2D.Double(304.70318603515625, 73.63397979736328));
		points.add(new Point2D.Double(234.9072265625, 74.002685546875));
		points.add(new Point2D.Double(124.58308410644531, 68.8958969116211));
		points.add(new Point2D.Double(54.660247802734375, 70.22028350830078));
		points.add(new Point2D.Double(308.67315673828125, 72.95449829101562));
		
		int depth=PathUtil.calcIntermediatePoints(points, 0, new ProjObj.Projection_OC(35,85), 0);
		
		System.out.println("Depth = " + depth);
		
		for (Point2D point : points) {
			System.out.println(point.getX() + " : " + point.getY());
		}
		
		System.out.println("");
		System.out.println("");
		System.out.println("");
		points.clear();
		points.add(new Point2D.Double(54.660247802734375, 70.22028350830078));
		points.add(new Point2D.Double(308.67315673828125, 72.95449829101562));
		
		depth=PathUtil.calcIntermediatePoints(points, 0, new ProjObj.Projection_OC(35,85), 0);
		
		System.out.println("Depth = " + depth);
		
		for (Point2D point : points) {
			System.out.println(point.getX() + " : " + point.getY());
		}

	}	
}
