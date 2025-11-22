package edu.asu.jmars.graphics;

import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

/**
 * Wraps a given {@link PathIterator} and {@link Transformer} to transform points in ways
 * beyond what is possible with an affine transform given to
 * {@link java.awt.Shape#getPathIterator(java.awt.geom.AffineTransform)}.
 */
public final class TransformingIterator implements PathIterator {
	/** Transforms the given point in place. */
	public interface Transformer {
		void transform(Point2D.Double p);
	}
	
	private final PathIterator it;
	private final Transformer t;
	private final Point2D.Double p = new Point2D.Double();
	public TransformingIterator(PathIterator it, Transformer t) {
		if (it == null || t == null) {
			throw new IllegalArgumentException("Iterator and transformer must not be null");
		}
		this.t = t;
		this.it = it;
	}
	public int currentSegment(float[] coords) {
		int code = it.currentSegment(coords);
		if (t != null) {
			p.x = coords[0];
			p.y = coords[1];
			t.transform(p);
			coords[0] = (float)p.x;
			coords[1] = (float)p.y;
		}
		return code;
	}
	public int currentSegment(double[] coords) {
		int code = it.currentSegment(coords);
		if (t != null) {
			p.x = coords[0];
			p.y = coords[1];
			t.transform(p);
			coords[0] = p.x;
			coords[1] = p.y;
		}
		return code;
	}
	public int getWindingRule() {
		return it.getWindingRule();
	}
	public boolean isDone() {
		return it.isDone();
	}
	public void next() {
		it.next();
	}
}

