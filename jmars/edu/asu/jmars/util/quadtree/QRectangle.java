package edu.asu.jmars.util.quadtree;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Exaqmple of overloaded Rectangle2D to demonstrate implementing user defined Shape class 
 * which can easily encapsulate any desired class
 *
 */
public class QRectangle implements Shape {
	
	private static final long serialVersionUID = -2144839392241586455L;
	Rectangle2D rec;

	public QRectangle() {
		rec = new Rectangle2D.Double();
	}
	
	public QRectangle(double x, double y, double width, double height) {
		rec = new Rectangle2D.Double(x, y, width, height);		
	}
	
	@Override
	public Rectangle getBounds() {
		return rec.getBounds();
	}

	@Override
	public Rectangle2D getBounds2D() {
		return rec.getBounds2D();
	}

	@Override
	public boolean contains(double x, double y) {
		return rec.contains(x, y);
	}

	@Override
	public boolean contains(Point2D p) {
		return rec.contains(p);
	}

	@Override
	public boolean intersects(double x, double y, double w, double h) {
		return rec.intersects(x, y, w, h);
	}

	@Override
	public boolean intersects(Rectangle2D r) {
		return rec.intersects(r);
	}

	@Override
	public boolean contains(double x, double y, double w, double h) {
		return rec.contains(x, y, w, h);
	}

	@Override
	public boolean contains(Rectangle2D r) {
		return rec.contains(r);
	}

	@Override
	public PathIterator getPathIterator(AffineTransform at) {
		return rec.getPathIterator(at);
	}

	@Override
	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return rec.getPathIterator(at, flatness);
	}
	
	@Override
	public String toString() {
		return "QRectangle (x,y,w,h) "+rec.getX()+", "+rec.getY()+", "+rec.getWidth()+", "+rec.getHeight();
	}

}
