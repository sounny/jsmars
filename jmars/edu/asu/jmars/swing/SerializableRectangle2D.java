package edu.asu.jmars.swing;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A serializable version of Rectangle2D.
 */
public class SerializableRectangle2D extends Rectangle2D implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;

	private Rectangle2D rect;
	
	public SerializableRectangle2D() {
		rect = new Rectangle2D.Double();
	}

	public SerializableRectangle2D(Rectangle2D rect) {
		this();
		this.rect.setFrame(rect);
	}

	public SerializableRectangle2D createIntersection(Rectangle2D r) {
		return new SerializableRectangle2D(rect.createIntersection(r));
	}

	public SerializableRectangle2D createUnion(Rectangle2D r) {
		return new SerializableRectangle2D(rect.createUnion(r));
	}

	public int outcode(double x, double y) {
		return rect.outcode(x, y);
	}

	public void setRect(double x, double y, double w, double h) {
		rect.setRect(x, y, w, h);
	}

	public double getHeight() {
		return rect.getHeight();
	}

	public double getWidth() {
		return rect.getWidth();
	}

	public double getX() {
		return rect.getX();
	}

	public double getY() {
		return rect.getY();
	}

	public boolean isEmpty() {
		return rect.isEmpty();
	}
	
	public Object clone() {
		SerializableRectangle2D r = (SerializableRectangle2D)super.clone();
		r.rect = (Rectangle2D)r.rect.clone();
		return r;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeDouble(rect.getMinX());
		out.writeDouble(rect.getMinY());
		out.writeDouble(rect.getWidth());
		out.writeDouble(rect.getHeight());
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		rect = new Rectangle2D.Double(in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble());
	}
}
