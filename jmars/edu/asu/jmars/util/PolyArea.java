package edu.asu.jmars.util;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides utilities for working with java.awt.geom.Area.
 * 
 * @author eengle
 */
public final class PolyArea extends Area {
	public PolyArea() {
		super();
	}
	
	public PolyArea(Shape shape) {
		super(shape);
		if (!isPolygonal()) {
			throw new IllegalArgumentException("Can only create PolyArea from polygonal shapes");
		}
	}
	
	/**
	 * Returns an array of double-precision rectangles that cover the given Area.
	 * 
	 * The input Area must be polygonal (e.g. no curves.)
	 * 
	 * The algorithm gets all distinct x and y values for each vertex
	 * in the area, makes a grid of rectangular cells out of those values, and
	 * returns all cells in the grid that are in the original area.
	 * 
	 * This technique
	 * tends to return more than the minimal number of rectangles, and if any
	 * lines in the input area are not at makes no
	 * attempt to the e rectangles are created by discovering the grid of all rectangular cells
	 *  
	 * Find all unique x and y values for the area construct each
	 * non-overlapping rectangle if it touches the area, add it to the return
	 * list only accepts polygonal areas that explicitly close each sub-area
	 * 
	 * Larger numbers of areas have shown the speed decreases faster than
	 * linear, so be careful with extraordinarily complex shapes.
	 */
	public final Rectangle2D[] getRectangles() {
		double[] coords = new double[6];
		double[] xlist = new double[10];
		double[] ylist = new double[10];
		int vertices = 0;
		List<Rectangle2D> rects = new LinkedList<Rectangle2D>();
		boolean closed = false;
		for (PathIterator path = getPathIterator(null); !path.isDone(); path.next()) {
			switch (path.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
			case PathIterator.SEG_LINETO:
				closed = false;
				if (vertices >= xlist.length) {
					xlist = resizeArray(xlist, xlist.length*2);
					ylist = resizeArray(ylist, ylist.length*2);
				}
				xlist[vertices] = coords[0];
				ylist[vertices] = coords[1];
				vertices ++;
				break;
			case PathIterator.SEG_CLOSE:
				closed = true;
				Arrays.sort(xlist = resizeArray(xlist, vertices));
				Arrays.sort(ylist = resizeArray(ylist, vertices));
				xlist = unique(xlist, vertices);
				ylist = unique(ylist, vertices);
				for (int j = 1; j < xlist.length; j++) {
					for (int k = 1; k < ylist.length; k++) {
						Rectangle2D rect = new Rectangle2D.Double(
							xlist[j-1], ylist[k-1],
							xlist[j]-xlist[j-1], ylist[k]-ylist[k-1]);
						if (contains(rect)) {
							rects.add(rect);
						}
					}
				}
				vertices = 0;
				xlist = new double[10];
				ylist = new double[10];
				break;
			default:
				throw new IllegalArgumentException("Input area must consist of straight line geometry only!");
			}
		}
		if (rects.isEmpty()) {
			return new Rectangle2D[0];
		}
		if (!closed) {
			throw new IllegalArgumentException("Area's subareas must be closed!");
		}
		return rects.toArray(new Rectangle2D[0]);
	}
	
	/**
	 * Returns numeric area for this Area. Will always return a positive value.
	 * 
	 * <p>
	 * <code>area = 0.5 * sum for all i (x(i)*y(i+1) - x(i+1)*y(i))</code>
	 * </p>
	 * 
	 * If there are multiple disconnected polygons in this Area, then the area
	 * of each is computed and summed. If there is e.g. a hexagon with a square
	 * cut out of it, the area of the square will be subtracted from the area of
	 * the hexagon and only the internal area of the shape will be shown.
	 * 
	 * @throws UnsupportedOperationException
	 *		If this is not one or more closed polygons composed entirely
	 *		of straight line segments.
	 */
	public double getArea() {
		double partialArea = 0d; // area of the current singular polygon
		double totalArea = 0d;   // area of all singular polygons
		double x0 = 0d, y0 = 0d; // first point in the current singular polygon
		double x1, y1;           // previous point in the current singular polygon
		double x2 = 0d, y2 = 0d; // current point in the current singular polygon
		double[] coords = new double[6];
		for (PathIterator pi = getPathIterator(null); !pi.isDone(); pi.next()) {
			int code = pi.currentSegment(coords);
			// set last point
			x1 = x2;
			y1 = y2;
			// set current point
			x2 = coords[0];
			y2 = coords[1];
			switch (code) {
			case PathIterator.SEG_MOVETO:
				// set first point
				x0 = x2;
				y0 = y2;
				// init polygon area
				partialArea = 0d;
				break;
			case PathIterator.SEG_LINETO:
				// add up area with last and current points
				partialArea += x1*y2 - x2*y1;
				break;
			case PathIterator.SEG_CLOSE:
				// add up area with current and first points
				partialArea += x2*y0 - x0*y2;
				// add polygon area to total of areas for all polygons
				totalArea += partialArea;
				break;
			default:
				throw new UnsupportedOperationException("Unsupported path element " + code);
			}
		}
		return Math.abs(totalArea / 2d);
	}
	
	/** Returns a new area equal to the part of this area not within the argument area */
	public PolyArea newSub(PolyArea a) {
		PolyArea out = new PolyArea(this);
		out.subtract(a);
		return out;
	}
	
	/** Returns a new area equal to the union of this area and the argument area */
	public PolyArea newAdd(Area a) {
		PolyArea out = new PolyArea(this);
		out.add(a);
		return out;
	}
	
	// processes the first 'length' elements of data, returning a possibly
	// shorter array of only the unique items (requires 'data' to be
	// monotonically increasing for the first length elements)
	private static double[] unique(double[] data, int length) {
		data = resizeArray(data, length);
		double last = Double.NaN;
		int pos = 0;
		for (int i = 0; i < data.length; i++) {
			if (!(last == data[i])) { // catch NaNs
				data[pos++] = last = data[i];
			}
		}
		return resizeArray(data, pos);
	}
	
	/**
	 * Creates a copy of the given data array of the given length, copying as
	 * many elements from the input as possible
	 */
	private static double[] resizeArray(double[] data, int length) {
		double[] out = new double[length];
		System.arraycopy(data,0, out,0, Math.min(out.length, data.length));
		return out;
	}
	
	public static void main(String[] args) {
		// number of rectangles to build shape => microseconds per call to getAlignedRects():
		// 3 => 26.5
		// 7 => 28.28
		// 10 => 76.87
		// 13 => 108.59
		Rectangle2D[] rects = {
			new Rectangle2D.Double(0,0,10,10), // simple case
			new Rectangle2D.Double(5,3,9,6), // overlapping case
			new Rectangle2D.Double(20,20,10,10), // disjoint case
			new Rectangle2D.Double(5,5,3,3), // enclosed case
			new Rectangle2D.Double(25,25,10,10),
			new Rectangle2D.Double(35,35,10,10),
			new Rectangle2D.Double(45,45,10,10),
			new Rectangle2D.Double(55,55,10,10),
			new Rectangle2D.Double(65,65,10,10), // more points for better measurements
			new Rectangle2D.Double(75,75,10,10),
			new Rectangle2D.Double(85,85,10,10),
			new Rectangle2D.Double(95,95,10,10),
			new Rectangle2D.Double(105,105,10,10),
		};
		PolyArea test = new PolyArea();
		for (int i = 0; i < rects.length; i++) {
			test.add(new PolyArea(rects[i]));
		}
		long time = System.currentTimeMillis();
		Rectangle2D[] out = null;
		for (int i = 0; i < 10000; i++) {
			out = test.getRectangles();
		}
		System.out.println(System.currentTimeMillis() - time);
		System.out.println(Arrays.asList(out));
	}
}

