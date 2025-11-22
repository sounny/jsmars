package edu.asu.jmars.layer.profile;

import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;


public class ChartDataConverter {
	
	private Layer.LView view = null;
	
	public ChartDataConverter (Layer.LView lview) {
		view = lview;
	}
	
	/**
	 * BaseGlass proxy wraps the screen coordinates, which we do NOT want, so we use
	 * the real point it remembers IF this event is a wrapped one.
	 */
	public Point2D clampedWorldPoint(Point2D anchor, MouseEvent e) {
		Point mousePoint = e instanceof WrappedMouseEvent ? ((WrappedMouseEvent) e).getRealPoint() : e.getPoint();
		Point2D worldPoint = view.getProj().screen.toWorld(mousePoint);
		double x = Util.mod360(worldPoint.getX());
		double a = Util.mod360(anchor.getX());
		if (x - a > 180.0)
			x -= 360.0;
		if (a - x > 180.0)
			x += 360.0;
		double y = worldPoint.getY();
		if (y > 90)
			y = 90;
		if (y < -90)
			y = -90;
		return new Point2D.Double(x, y);
	}

	public Point2D clampedWorldPoint2(Point2D anchor, Point2D inputpoint) {		
		Point2D worldPoint = view.getProj().screen.toWorld(inputpoint);
		double x = Util.mod360(worldPoint.getX());
		double a = Util.mod360(anchor.getX());
		if (x - a > 180.0)
			x -= 360.0;
		if (a - x > 180.0)
			x += 360.0;
		double y = worldPoint.getY();
		if (y > 90)
			y = 90;
		if (y < -90)
			y = -90;
		return new Point2D.Double(x, y);
	}
	
	public static Shape convertToFPath(List<Point2D> pts, Point2D lastPt) {
		GeneralPath gp = new GeneralPath();
		if (pts.isEmpty()) {
			return gp;
		}
		List<Point2D> tmp = new ArrayList<Point2D>(pts.size() + 1);
		tmp.addAll(pts);
		if (lastPt != null)
			tmp.add(lastPt);

		for (Point2D pt : tmp) {
			if (gp.getCurrentPoint() == null)
				gp.moveTo((float) pt.getX(), (float) pt.getY());
			else
				gp.lineTo((float) pt.getX(), (float) pt.getY());
		}

		FPath fp = new FPath(gp, FPath.WORLD);
		return fp.getShape();
	}

	/**
	 * Computes the perimeter length of the given shape (in world coordinates). The
	 * length is computed using the 
	 * {@link Util#angularAndLinearDistanceW(Point2D, Point2D, edu.asu.jmars.layer.MultiProjection)}
	 * method.
	 * @param shape Shape in world coordinates.
	 * @return Length of perimeter in degrees, kilometers and cartesian-distance.
	 */
	public  double[] perimeterLength(Shape shape){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double angularDist = 0;
		double linearDist = 0;
		double cartDist = 0;
		
		while(!pi.isDone()){
			switch(pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double dists[] = Util.angularAndLinearDistanceW(lseg.getP1(), lseg.getP2(), view.getProj());
			angularDist += dists[0];
			linearDist += dists[1];
			cartDist += lseg.getP2().distance(lseg.getP1());
			pi.next();
		}
		
		return new double[]{ angularDist, linearDist, cartDist };
	}
	
	
	/**
	 * Returns the shape formed by sub-selection based on the
	 * parameter range of <code>t</code>, where <code>t</code> ranges
	 * between <code>0</code> and <code>1</code> based on the Cartesian
	 * distance from the first point in the shape.
	 * @param shape Line-string.
	 * @param t0 Starting value of <code>t</code>
	 * @param t1 Ending value of <code>t</code>
	 * @return A sub-selected shape.
	 */
	public  Shape spanSelect(Shape shape, double t0, double t1){
		if (t0 < 0 && t1 > 1){
			GeneralPath p = new GeneralPath();
			
			Point2D p0 = interpolate(shape, t0);
			p.moveTo((float)p0.getX(), (float)p0.getY());
			
			PathIterator pi = shape.getPathIterator(null, 0);
			float[] coords = new float[6];
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
				case PathIterator.SEG_LINETO:
					p.lineTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					throw new RuntimeException("Unhandled situation! Expecting unclosed line-string.");
				}
				pi.next();
			}
			
			Point2D p1 = interpolate(shape, t1);
			p.lineTo((float)p1.getX(), (float)p1.getY());
			
			return p;
		}
		else if (t0 < 0){
			GeneralPath p = new GeneralPath();
			
			Point2D p0 = interpolate(shape, t0);
			p.moveTo((float)p0.getX(), (float)p0.getY());
			
			PathIterator pi = shape.getPathIterator(null, 0);
			float[] coords = new float[6];
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
				case PathIterator.SEG_LINETO:
					p.lineTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					throw new RuntimeException("Unhandled situation! Expecting unclosed line-string.");
				}
				pi.next();
			}
			return p;
		}
		else if (t1 > 1){
			GeneralPath p = new GeneralPath();
			PathIterator pi = shape.getPathIterator(null, 0);
			float[] coords = new float[6];
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
					p.moveTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_LINETO:
					p.lineTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					throw new RuntimeException("Unhandled situation! Expecting unclosed line-string.");
				}
				pi.next();
			}
			
			Point2D p1 = interpolate(shape, t1);
			p.lineTo((float)p1.getX(), (float)p1.getY());
			
			return p;
		}
		else {
			GeneralPath p = new GeneralPath();
			
			PathIterator pi = shape.getPathIterator(null, 0);
			double coords[] = new double[6];
			Point2D.Double first = new Point2D.Double();
			Line2D.Double lseg = new Line2D.Double();
			double linearDist = 0;
			boolean startDone = false, endDone = false;
			double totalLength = perimeterLength(shape)[2];
			
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
					first.x = lseg.x1 = lseg.x2 = coords[0];
					first.y = lseg.y1 = lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_LINETO:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = coords[0]; lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_CLOSE:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = first.x; lseg.y2 = first.y;
					break;
				}
				
				double segLength = lseg.getP2().distance(lseg.getP1());
				if (!startDone && t0 >= (linearDist/totalLength) && t0 <= ((linearDist+segLength)/totalLength)){
					Point2D pt = Util.interpolate(lseg, t0-linearDist/totalLength);
					p.moveTo((float)pt.getX(), (float)pt.getY());
					startDone = true;
				}
				if (!endDone && t1 >= (linearDist/totalLength) && t1 <= ((linearDist+segLength)/totalLength)){
					Point2D pt = Util.interpolate(lseg, t1-linearDist/totalLength);
					p.lineTo((float)pt.getX(), (float)pt.getY());
					endDone = true;
				}
				if (startDone && !endDone){
					p.lineTo((float)coords[0], (float)coords[1]);
				}
				
				linearDist += segLength;
				pi.next();
			}
			return p;
		}
	}
	
	/**
	 * Linearly interpolates a point given the shape (in world coordinates)
	 * and the parameter <code>t</code>.
	 * @param shape Line-string in world coordinates.
	 * @param t Interpolation parameter <code>t</code>.
	 * @return A point obtained by linear-interpolation using the points
	 *     in the shape, given the parameter <code>t</code>.
	 */
	public Point2D interpolate(Shape shape, double t){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double cartDist = 0;
		int currSeg = PathIterator.SEG_MOVETO;
		double totalLength = perimeterLength(shape)[2];
		
		while(!pi.isDone()){
			switch(currSeg = pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double segLength = lseg.getP2().distance(lseg.getP1());
			if (currSeg != PathIterator.SEG_MOVETO && ((cartDist + segLength)/totalLength) >= t){
				return Util.interpolate(lseg, (t*totalLength-cartDist)/segLength);
			}
			
			cartDist += segLength;
			pi.next();
		}
		
		return Util.interpolate(lseg, (t*totalLength-cartDist)/(lseg.getP2().distance(lseg.getP1())));
	}
	
	
	/**
	 * Computes the perimeter length of the given shape (in world coordinates). The
	 * length is computed using the 
	 * {@link Util#angularAndLinearDistanceW(Point2D, Point2D, edu.asu.jmars.layer.MultiProjection)}
	 * method.
	 * @param shape Shape in world coordinates.
	 * @return Length of perimeter in degrees, kilometers and cartesian-distance.
	 */
	public double[] distanceTo(Shape shape, Point2D pt){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double angularDist = 0;
		double linearDist = 0;
		double cartDist = 0;
		double t = uninterpolate(shape, pt, null);
		double lengths[] = perimeterLength(shape);
		double totalLength = lengths[2];
		
		if (t < 0 || t > 1){
			return new double[]{ Double.NaN, Double.NaN, Double.NaN };
		}
		else {
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
					first.x = lseg.x1 = lseg.x2 = coords[0];
					first.y = lseg.y1 = lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_LINETO:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = coords[0]; lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_CLOSE:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = first.x; lseg.y2 = first.y;
					break;
				}

				double lsegLength = lseg.getP2().distance(lseg.getP1());
				if ((cartDist + lsegLength)/totalLength > t){
					double dists[] = Util.angularAndLinearDistanceW(lseg.getP1(), pt, view.getProj());
					angularDist += dists[0]; 
					linearDist += dists[1];
					cartDist += pt.distance(lseg.getP1());
					break;
				}
				double dists[] = Util.angularAndLinearDistanceW(lseg.getP1(), lseg.getP2(), view.getProj());
				angularDist += dists[0];
				linearDist += dists[1];
				cartDist += lsegLength;
				pi.next();
			}
		}
		
		return new double[]{ angularDist, linearDist, cartDist };
	}

	/**
	 * Linearly uninterpolates the parameter <code>t</code> value of the specified
	 * point from its closest approach to the specified shape (in world 
	 * coordinates).
	 * @param shape Line-string in world-coordinates.
	 * @param pt Point for which the parameter <code>t</code> is to be determined.
	 * @param distance If not <code>null</code>, its first element contains
	 *     the minimum distance to one of the segments in the line-string.
	 * @return The parameter <code>t</code> which will give the specified
	 *     point if {@link #interpolate(Shape, double)} is called using it as the
	 *     second parameter. Returns {@link Double#NaN} if the shape contains only
	 *     a single point.
	 * {@see Util#uninterploate(Line2D, Point2D)}
	 */
	public double uninterpolate(Shape shape, Point2D pt, double[] distance){
		double t = Double.NaN;
		
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double cartDist = 0, linearDistToMinSeg = 0;
		double minDistSq = Double.MAX_VALUE;
		Line2D.Double minSeg = null;
		int currSeg = PathIterator.SEG_MOVETO;
		double totalLength = perimeterLength(shape)[2];
		
		while(!pi.isDone()){
			switch(currSeg = pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double lsegDistSq = lseg.ptSegDistSq(pt);
			if (currSeg != PathIterator.SEG_MOVETO && lsegDistSq < minDistSq){
				minSeg = new Line2D.Double(lseg.x1, lseg.y1, lseg.x2, lseg.y2);
				minDistSq = lsegDistSq;
				linearDistToMinSeg = cartDist;
			}
			
			cartDist += lseg.getP2().distance(lseg.getP1());
			pi.next();
		}
		
		if (minSeg != null){
			double tt = Util.uninterploate(minSeg, pt);
			double minSegLength = minSeg.getP2().distance(minSeg.getP1());
			if (tt < 0 && linearDistToMinSeg > 0)
				tt = 0;
			if (tt > 1 && (linearDistToMinSeg + minSegLength) < totalLength)
				tt = 1;
			t = (linearDistToMinSeg + tt * minSegLength) / totalLength;
		
			if (distance != null && distance.length > 0)
				distance[0] = Math.sqrt(minDistSq);
		}

		return t;
	}

	public Point2D getFirstPoint(Shape s){
		PathIterator pi = s.getPathIterator(null, 0);
		if (pi.isDone())
			return null;
		
		double coords[] = new double[6];
		pi.currentSegment(coords);
		return new Point2D.Double(coords[0], coords[1]);
	}
	

	public Point2D getLastPoint(Shape s){
		PathIterator pi = s.getPathIterator(null, 0);
		if (pi.isDone())
			return null;
		
		double coords[] = new double[6];
		while(!pi.isDone()){
			pi.currentSegment(coords);
			pi.next();
		}
		return new Point2D.Double(coords[0], coords[1]);
	}
	
	
	public Rectangle2D expandByXPixelsEachSide(Rectangle2D in, int ppd, double xPixels){
		Rectangle2D.Double out = new Rectangle2D.Double();
		out.setFrame(in);
		
		double hdpp = xPixels * (1.0/ppd);
		out.setFrame(out.getX() - hdpp, out.getY() - hdpp, out.getWidth() + 2*hdpp, out.getHeight() + 2*hdpp);
		
		return out;
	}	
	
	/**
	 * Determines the angle at which the line-segment surrounding
	 * the given parameter <code>t</code> is.
	 * @param shape Line-string in world coordinates.
	 * @param t Linear interpolation parameter.
	 * @return Angle of the line-segment bracketing the parameter
	 * <code>t</code> or <code>null</code> if <code>t</code> is
	 * out of range.
	 */
	public double angle(Shape shape, double t){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double linearDist = 0;
		int currSeg = PathIterator.SEG_MOVETO;
		double totalLength = perimeterLength(shape)[2];
		
		while(!pi.isDone()){
			switch(currSeg = pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double segLength = lseg.getP2().distance(lseg.getP1());//Util.angularAndLinearDistanceW(lseg.getP1(), lseg.getP2(), getProj())[1];
			if (currSeg != PathIterator.SEG_MOVETO && ((linearDist + segLength)/totalLength) >= t){
				HVector p1 = new HVector(lseg.x1, lseg.y1, 0);
				HVector p2 = new HVector(lseg.x2, lseg.y2, 0);
				double angle = HVector.X_AXIS.separationPlanar(p2.sub(p1), HVector.Z_AXIS);
				return angle;
			}
			
			linearDist += segLength;
			pi.next();
		}
		
		return Double.NaN;
	}		
	
}
