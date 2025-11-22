package edu.asu.jmars.layer;

import edu.asu.jmars.*;
import edu.asu.jmars.util.*;
import java.awt.geom.*;

public abstract class SingleProjection
 {
	// Fundamental conversion routines
	public abstract Point2D toScreen(double x, double y);
	public abstract Point2D toScreenLocal(double x, double y);
	public abstract Point2D toWorld(double x, double y);
	public abstract Point2D toSpatial(double x, double y);
	public abstract Point2D fromHVector(double x, double y, double z);

	// Non-optimized version that should always work
	public HVector toHVector(double x, double y)
	 {
		Point2D s = toSpatial(x, y);
		return new HVector(s);
	 }

	// Fundamental geometry routines
	public abstract double distance(double ax, double ay,
									double bx, double by);
	public abstract double distance(double a1x, double a1y,
									double a2x, double a2y,
									double px, double py);
	public abstract boolean hitTest(double a1x, double a1y,
									double a2x, double a2y,
									double b1x, double b1y,
									double b2x, double b2y);
	public abstract Point2D nearPt(double a1x, double a1y,
								   double a2x, double a2y,
								   double px, double py,
								   double maxDist);

	// Shortcut geometry routines
	public double distance(Point2D a, Point2D b)
	 {
		return  distance(a.getX(), a.getY(),
						 b.getX(), b.getY());
	 }
	public double distance(Line2D a, Point2D p)
	 {
		return  distance(a.getX1(), a.getY1(),
						 a.getX2(), a.getY2(),
						 p.getX(), p.getY());
	 }
	public double distance(Point2D a1, Point2D a2, Point2D p)
	 {
		return  distance(a1.getX(), a1.getY(),
						 a2.getX(), a2.getY(),
						 p.getX(), p.getY());
	 }
	public boolean hitTest(Line2D a, Line2D b)
	 {
		return  hitTest(a.getX1(), a.getY1(),
						a.getX2(), a.getY2(),
						b.getX1(), b.getY1(),
						b.getX2(), b.getY2());
	 }
	public boolean hitTest(Point2D a1, Point2D a2,
						   Point2D b1, Point2D b2)
	 {
		return  hitTest(a1.getX(), a1.getY(),
						a2.getX(), a2.getY(),
						b1.getX(), b1.getY(),
						b2.getX(), b2.getY());
	 }
	public Point2D nearPt(Line2D a, Point2D p, double maxDist)
	 {
		return  nearPt(a.getX1(), a.getY1(),
					   a.getX2(), a.getY2(),
					   p.getX(), p.getY(),
					   maxDist);
	 }
	public Point2D nearPt(Point2D a1, Point2D a2, Point2D p, double maxDist)
	 {
		return  nearPt(a1.getX(), a1.getY(),
					   a2.getX(), a2.getY(),
					   p.getX(), p.getY(),
					   maxDist);
	 }

	// Shortcut conversion routines
	public Point2D toScreen(Point2D pt)
	 {
		return  toScreen(pt.getX(), pt.getY());
	 }
	public Point2D toScreenLocal(Point2D pt)
	 {
		return  toScreenLocal(pt.getX(), pt.getY());
	 }
	public Point2D toWorld(Point2D pt)
	 {
		return  toWorld(pt.getX(), pt.getY());
	 }
	public Point2D toSpatial(Point2D pt)
	 {
		return  toSpatial(pt.getX(), pt.getY());
	 }
	public HVector toHVector(Point2D pt)
	 {
		return  toHVector(pt.getX(), pt.getY());
	 }
	public Point2D fromHVector(HVector pt)
	 {
		return  fromHVector(pt.x, pt.y, pt.z);
	 }






/****************************************************************************
 ** NOT YET IMPLEMENTED

	// Shape conversion routines
	public Shape toScreen(Shape sh)
	 {
		return  toScreen(sh.getPathIterator());
	 }
	public Shape toWorld(Shape sh)
	 {
		return  toWorld(sh.getPathIterator());
	 }
	public Shape toSpatial(Shape sh)
	 {
		return  toSpatial(sh.getPathIterator());
	 }
	public HVector[] toHVector(Shape sh, boolean close)
	 {
		return  toHVector(sh.getPathIterator());
	 }
	public GeneralPath fromHVector(HVector[] vv, boolean closed)
	 {
		// do stuff
	 }

*****************************************************************************/

 }
