package edu.asu.jmars.util.quadtree;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** This class is the key to success with the QuadTree class
 *  where point intersections of shapes, lines, or even other QPoints
 *  is desired.
 */
public class QPoint extends Point2D.Double implements Shape {
	
	private static final long serialVersionUID = -2144839392241582705L;
	
	double epsilon = 0.0;
	Ellipse2D.Double epsilonArea;

	public final double MIN_BOUND_X = 0.0001;
	public final double MIN_BOUND_Y = 0.0001;
	
	public QPoint() {
		x = 0.0;
		y = 0.0;
	}
	
	public QPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public QPoint(double x, double y, double epsilon) {
		this.x = x;
		this.y = y;
		this.epsilon = epsilon;
	}
	
	@Override
	public Rectangle getBounds() {
		Rectangle2D r2 = this.getBounds2D();
		return new Rectangle((int)r2.getX(), (int)r2.getY(), (int)r2.getWidth(), (int)r2.getHeight());
	}
	@Override
	public Rectangle2D getBounds2D() {
		if (epsilonArea != null) {
			return epsilonArea.getFrame();
		} else if (epsilon <= 0.0){
			double x = this.x - MIN_BOUND_X;
			double y = this.y - MIN_BOUND_Y;
			double width = MIN_BOUND_X * 2.0;
			double height = MIN_BOUND_Y * 2.0;
			epsilonArea = new Ellipse2D.Double(x, y, width, height);	
			return epsilonArea.getFrame();	
		} else {
			double x = this.x - epsilon;
			double y = this.y - epsilon;
			double width = epsilon * 2.0;
			double height = epsilon * 2.0;
			epsilonArea = new Ellipse2D.Double(x, y, width, height);	
			return epsilonArea.getFrame();	
		}
	}
	@Override
	public boolean contains(double x, double y) {
		Rectangle2D bounds = getBounds2D();
		return bounds.contains(x, y);
	}
	@Override
	public boolean contains(Point2D p) {
		Rectangle2D bounds = getBounds2D();
		return bounds.contains(p);
	}
	@Override
	public boolean intersects(double x, double y, double w, double h) {
		Rectangle2D bounds = getBounds2D();
		return bounds.intersects(x, y, w, h);
	}
	@Override
	public boolean intersects(Rectangle2D r) {
		Rectangle2D bounds = getBounds2D();
		return bounds.intersects(r);
	}
	@Override
	public boolean contains(double x, double y, double w, double h) {
		Rectangle2D bounds = getBounds2D();
		return bounds.contains(x, y, w, h);
	}
	@Override
	public boolean contains(Rectangle2D r) {
		Rectangle2D bounds = getBounds2D();
		return bounds.contains(r);
	}
	@Override
	public PathIterator getPathIterator(AffineTransform at) {
		Rectangle2D bounds = getBounds2D();
		return bounds.getPathIterator(at);
	}
	@Override
	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		Rectangle2D bounds = getBounds2D();
		return bounds.getPathIterator(at, flatness);
	}
	
	@Override
	public String toString() {
		return "QPoint (x,y) "+x+", "+y+", epsilon "+epsilon;
	}
}
