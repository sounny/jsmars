package edu.asu.jmars.layer.shape2;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.util.Util;

public class ShapeUtil {




	/**
	 * Method that takes in a Shape and a ProjObj, and attempts to make the Shape follow the same spatial path it would have 
	 * taken in the Main.PO projection.
	 * 
	 * @param p
	 * @param coordSystem
	 * @param po
	 * @return
	 */
    public static GeneralPath path2Path(Shape p, int coordSystem, ProjObj po) {
    	
    	if (coordSystem==FPath.WORLD) {
    		p=Util.normalize360(p);	
    	}
    	
    	ArrayList<ArrayList<Point2D>> points = generalPathToPoints(p, false, true, coordSystem, po);
    	
    	GeneralPath newPath = new GeneralPath();
    	
    	for (ArrayList<Point2D> path : points) {
    		newPath.moveTo(path.get(0).getX(), path.get(0).getY());
    		
    		for (int i=1; i<path.size(); i++) {
    			newPath.lineTo(path.get(i).getX(), path.get(i).getY());
    		}
    	}
    
    	double[] coords = new double[6]; 
    	for (PathIterator pi = p.getPathIterator(null); !pi.isDone(); pi.next()){

			if(pi.currentSegment(coords)==PathIterator.SEG_CLOSE){
				newPath.closePath();
				break;
			}
    	}
    	
    	return newPath;
    }
    
    // Stolen from PathUtil -- TODO: Review this to see if it makes sense at all
    public static ArrayList<ArrayList<Point2D>> generalPathToPoints(Shape gp, boolean flipLongitude, boolean normalize, int coordSystem, ProjObj po) 
	{
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
				Point2D newPoint = new Point2D.Double(coords[0], coords[1]);
				vertices.add(newPoint);
				break;
			case PathIterator.SEG_CLOSE:
				if (vertices!=null) {
					vertices.add((Point2D)vertices.get(0));
				}
				break;
			}
		}
		
		vertices = makeConsistentLine(vertices, po, Main.PO);
		
		paths.add(vertices);
		
		return paths;
	}

    private static double distanceBetween(Point2D a, Point2D b) {
		return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
	}
 
    private static Point2D fixEndPoint(Point2D startPoint, Point2D endPoint) {
    	double xdiff = startPoint.getX() - endPoint.getX();
    	
    	if (Math.abs(xdiff)>180) {
    		if (startPoint.getX() < endPoint.getX()) {
    			endPoint = new Point2D.Double(endPoint.getX()-360, endPoint.getY());
    		} else {
    			endPoint = new Point2D.Double(endPoint.getX()+360, endPoint.getY());
    		}
    	}
    	
    	return endPoint;
    }    	
    	
    
    /**
     * Calculate the middle point between two points, all in WORLD coordinates
     * @param startPoint
     * @param endPoint
     * @return
     */
    private static Point2D midPointWorld(Point2D startPoint, Point2D endPoint) {
    	endPoint = fixEndPoint(startPoint, endPoint);
    	
    	Point2D midPointWorld = new Point2D.Double((startPoint.getX() + endPoint.getX()) / 2, (startPoint.getY() + endPoint.getY()) / 2);

    	return midPointWorld;
    }
    
    public static ArrayList<Point2D> makeConsistentLine(ArrayList<Point2D> inPoints, ProjObj thisProjection, ProjObj projToEmulate) {
    	ArrayList<Point2D> outPoints = ((ArrayList<Point2D>)inPoints.clone());
    	
    	ArrayList<Point2D> emulatePoints = new ArrayList<Point2D>();
    	
    	// Build up an array with the same points in the world projection we want to emulate
    	for (Point2D inPoint : inPoints) {
    		emulatePoints.add(projToEmulate.convSpatialToWorld(thisProjection.convWorldToSpatial(inPoint)));
    	}
    	
    	int indexOut=0;
    	int indexEmulate=0;
    	
    	int loopLimit=0;
    	
    	while(loopLimit++<10000 && indexOut<outPoints.size()-1) {
    		Point2D outStart = outPoints.get(indexOut);
    		Point2D outEnd = outPoints.get(indexOut+1);
    		
    		Point2D emulateStart = emulatePoints.get(indexEmulate);
    		Point2D emulateEnd = emulatePoints.get(indexEmulate+1);
    	
    		Point2D outMid = midPointWorld(outStart, outEnd);
    		Point2D emulateMid = midPointWorld(emulateStart, emulateEnd);
    		
    		double distance = distanceBetween(thisProjection.convWorldToSpatial(outMid), projToEmulate.convWorldToSpatial(emulateMid));
    		// TODO: Vary delta based on zoom level?
    		if (distance<0.001) {
    			indexOut++;
    			indexEmulate++;
    			continue;
    		} else {
    			Point2D outEquivPoint = thisProjection.convSpatialToWorld(projToEmulate.convWorldToSpatial(emulateMid));

    			outEquivPoint = fixEndPoint(outStart, outEquivPoint);
    			emulateMid = fixEndPoint(emulateStart, emulateMid);
    			
    			emulatePoints.add(indexEmulate+1, emulateMid);
    			
    			outPoints.add(indexOut+1, outEquivPoint);
    		}
    		
    	}
    	
    	Point2D ptArray[] = new Point2D[outPoints.size()];
    	ptArray = outPoints.toArray(ptArray);
    	
    	ptArray=Util.normalize360(ptArray);
    	
    	outPoints.clear();

    	Point2D lastPoint = null;
    	
    	for (Point2D pt : ptArray) {
    		if (lastPoint!=null) {
    			pt = fixEndPoint(lastPoint, pt);
    		}
    		outPoints.add(pt);
    		lastPoint = pt;
    	}
    	    	
    	return outPoints;
    }
    
    
    public static void main(String args[]) {
    	Point2D aSpatial = new Point2D.Double(360-279.64449248745996, 69.82269084922387);
    	Point2D bSpatial = new Point2D.Double(360-37.71671992367732, -69.84583762091388);
    	
//    	Point2D origLine[] = new Point2D[2];
//    	origLine[0]=a;
//    	origLine[1]=b;
    	
    	
    	ProjObj desiredProjection = new ProjObj.Projection_OC(360-175,45);
    	ProjObj projToEmulate = new ProjObj.Projection_OC(360-185, 45);

    	Point2D aWorld = desiredProjection.convSpatialToWorld(aSpatial);
    	Point2D bWorld = desiredProjection.convSpatialToWorld(bSpatial);
    	
    	ArrayList<Point2D> origPoints = new ArrayList<Point2D>();
    	origPoints.add(aWorld);
    	origPoints.add(bWorld);
    	
    	ArrayList<Point2D> newPoints = makeConsistentLine(origPoints, desiredProjection, projToEmulate);
    	
    	System.out.println("newPoints size = " + newPoints.size());
    	
    	desiredProjection = new ProjObj.Projection_OC(225,75);
    	
    	aWorld = desiredProjection.convSpatialToWorld(aSpatial);
    	bWorld = desiredProjection.convSpatialToWorld(bSpatial);
    	
    	origPoints.clear();
    	origPoints.add(aWorld);
    	origPoints.add(bWorld);
    	
    	newPoints = makeConsistentLine(origPoints, desiredProjection, projToEmulate);

    	System.out.println("newPoints size = " + newPoints.size());

//		for (Point2D point : newPoints) {
//			System.out.println("point = " + point);
//		}

		///
/*		
    	desiredProjection = new ProjObj.Projection_OC(100,-5);
    	
    	aWorld = desiredProjection.convSpatialToWorld(aSpatial);
    	bWorld = desiredProjection.convSpatialToWorld(bSpatial);
    	
    	origPoints.clear();
    	origPoints.add(aWorld);
    	origPoints.add(bWorld);
    	
    	newPoints = makeConsistentLine(origPoints, desiredProjection, projToEmulate);

    	System.out.println("newPoints size = " + newPoints.size());

		for (Point2D point : newPoints) {
			System.out.println("point = " + point + " : spatial = " + desiredProjection.convWorldToSpatial(point));
		}
*/
    	desiredProjection = new ProjObj.Projection_OC(225,55);
    	
    	aWorld = desiredProjection.convSpatialToWorld(aSpatial);
    	bWorld = desiredProjection.convSpatialToWorld(bSpatial);
    	
    	origPoints.clear();
    	origPoints.add(aWorld);
    	origPoints.add(bWorld);
    	
    	newPoints = makeConsistentLine(origPoints, desiredProjection, projToEmulate);

    	System.out.println("newPoints size = " + newPoints.size());

		for (Point2D point : newPoints) {
			System.out.println("point = " + point + " : spatial = " + desiredProjection.convWorldToSpatial(point));
		}

		
    	GeneralPath newPath = new GeneralPath();
    	
		newPath.moveTo(newPoints.get(0).getX()+400, 180-newPoints.get(0).getY());
		
		for (int i=1; i<newPoints.size(); i++) {
			newPath.lineTo(newPoints.get(i).getX()+400, 180-newPoints.get(i).getY());
		}

		BufferedImage bi = new BufferedImage(1000,500,BufferedImage.TYPE_BYTE_GRAY);
		
		Graphics2D g2 = (Graphics2D)bi.getGraphics();

		g2.draw(newPath);
		
		try {
			 File outputfile = new File("/mars/u/sdickens/shapeimages/mainout"+desiredProjection.getCenterLat()+"-"+desiredProjection.getCenterLon()+".png");
			 ImageIO.write(bi, "png", outputfile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
//    	FPath originalLine = new FPath(origLine, FPath.SPATIAL_EAST, false);
//    
//    	ArrayList<ArrayList<Point2D>> outputPoints = generalPathToPoints(originalLine.convertTo(FPath.WORLD).getShape(), false, false, FPath.WORLD, new ProjObj.Projection_OC(360-175,45));
//    	
////    	for (ArrayList<Point2D> shape : outputPoints) {
////    		for (Point2D point : shape) {
////    			System.out.println("point = " + point);
////    		}
////    	}
//    	
//    	System.out.println("NEXT PROJECTION");
//    	outputPoints = generalPathToPoints(originalLine.convertTo(FPath.WORLD).getShape(), false, false, FPath.WORLD, new ProjObj.Projection_OC(225,75));
//    	
////    	for (ArrayList<Point2D> shape : outputPoints) {
////    		for (Point2D point : shape) {
////    			System.out.println("point = " + point);
////    		}
////    	}
    	
    	
    }
    
    
}
