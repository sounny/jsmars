package edu.asu.jmars.layer.util.features;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.awt.ShapeReader;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.graphics.TransformingIterator;
import edu.asu.jmars.graphics.TransformingIterator.Transformer;
import edu.asu.jmars.layer.shape2.ShapeUtil;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.SPolygon;
import edu.asu.jmars.util.Util;

/**
 * Defines an immutable path of vertices and some convenience methods to convert
 * between the world and spatial east/west coordinate systems. Several common
 * computations are also provided.
 */
public final class FPath {
	/** This Feature represents an undefined shape. */
	public static final int TYPE_NONE = 0;
	/** This Feature represents a single point. */
	public static final int TYPE_POINT = 1;
	/** This Feature represents an unclosed polyline with 2 or more points. */
	public static final int TYPE_POLYLINE = 2;
	/** This Feature represents a closed polygon with 3 or more points. */
	public static final int TYPE_POLYGON = 3;
	/** This Feature represents an ellipse. */
	public static final int TYPE_ELLIPSE = 4;
	
	private boolean isEllipse = false;//use this flag to return a type of ellipse, but everything else in this class will treat it as a polygon
	
	public static final int WORLD = 0;
	public static final int SPATIAL_EAST = 1;
	public static final int SPATIAL_WEST = 2;
	
	private static final NumberFormat nf = new DecimalFormat("0.###");
	
	private static final Transformer toggleLon = new Transformer() {
		public void transform(Point2D.Double p) {
			p.x = FeatureUtil.lonNorm(-p.x);
		}
	};
	
	/** The coordinate system, or null if this is a west-leading spatial path */
	private final int coordSystem;
	/** The shape. Will often be Path2D, but other classes are possible so long as they follow the rules defined by {@link PathIterator}. */
	private final Shape shape;
	/** The offsets to the start of each path, plus one additional entry at the end of the array for where the next entry would start. */
	private final int[] pathOffsets;
	/** The closed flag for each path, which is true for those paths that end with the {@link PathIterator#SEG_CLOSE} code, and false otherwise. */
	private final boolean[] pathClosed;
	
	/**
	 * Creates a new Path from the given point array, where each point
	 * is stored as
	 * lat lon lat lon ... (if latFirst is true, or)
	 * lon lat lon lat ... (if latFirst is false)
	 * @param coordSystem One of WORLD, SPATIAL_EAST, or SPATIAL_WEST
	 * @param closed If true, this represents a closed polygon
	 */
	public FPath (double[] coords, boolean latFirst, int coordSystem, boolean closed) {
		this(verticesToPath(coordsToVertices(coords, latFirst), closed), coordSystem);
	}

	public FPath (float[] coords, boolean latFirst, int coordSystem, boolean closed) {
		this(verticesToPath(coordsToVertices(coords, latFirst), closed), coordSystem);
	}

	/**
	 * Creates a new FPath with the given vertices.
	 * @param vertices These Point2D values are copied into the internal
	 * immutable Point2D class.
	 * @param coordSystem One of WORLD, SPATIAL_EAST, or SPATIAL_WEST
	 * @param closed If true, this represents a closed polygon
	 */
	public FPath (Point2D[] vertices, int coordSystem, boolean closed) {
		this(verticesToPath(vertices, closed), coordSystem);
	}
	
	public void setIsEllipse(boolean flag) {
		isEllipse = flag;
	}
	/**
	 * Creates a new FPath from the given Path2D, which must contain
	 * a single connected polygon, line, or point. If it does not, the
	 * resulting FPath will be empty.
	 */
	public FPath (Shape shape, int coordSystem) {
		// set the coordinate system or explode
		switch (coordSystem) {
		case SPATIAL_EAST:
		case SPATIAL_WEST:
		case WORLD:
			this.coordSystem = coordSystem;
			break;
		default:
			throw new IllegalArgumentException("Unrecognized coordinate system " + coordSystem);
		}
		
		// ensure WORLD coordinate shapes are normalized 
		this.shape = coordSystem == WORLD ? Util.normalize360(shape) : shape;
		
		// compute the offsets and closed flags
		PathIterator it = shape.getPathIterator(null);
		double[] coords = new double[6];
		// parallel arrays for each sub-path indicating the initial offset to the path and whether it is closed
		List<Integer> starts = new ArrayList<Integer>();
		List<Boolean> closed = new ArrayList<Boolean>();
		int offset = 0;
		for (boolean done = it.isDone(); !done; done = done | it.isDone()) {
			switch (it.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
				if (starts.size() > closed.size()) {
					closed.add(false);
				}
				starts.add(offset);
				break;
			case PathIterator.SEG_LINETO:
				break;
			case PathIterator.SEG_CLOSE:
				// allowed segment types, but we don't do anything here but avoid
				// the default case
				closed.add(true);
				break;
			default:
				throw new IllegalArgumentException("Curved edges are not supported");
			}
			it.next();
			offset ++;
		}
		
		if (starts.size() > closed.size()) {
			closed.add(false);
		}
		
		int size = starts.size();
		this.pathOffsets = new int[size+1];
		this.pathClosed = new boolean[size];
		for (int i = 0; i < size; i++) {
			pathOffsets[i] = starts.get(i);
			pathClosed[i] = closed.get(i);
		}
		pathOffsets[size] = offset;
	}
	
	/**
	 * @return WORLD, SPATIAL_WEST, or SPATIAL_EAST.
	 */
	public int getCoordSystem () {
		return coordSystem;
	}
	
	/** @return the number of paths within this FPath, always at least one. */
	public int getPathCount() {
		return pathClosed.length;
	}
	
	/** As {@link #getClosed(int)}, for the first sub-path. */
	public boolean getClosed () {
		if (pathClosed.length > 0) { // check if shape not EMPTY
			return pathClosed[0];
		} 
		return false;
	}
	
	
	/**
	 * @return true if the path at the given path index is a closed figure.
	 * Should only be true for paths with three or more points.
	 */
	public boolean getClosed(int path) {
		if (path >= 0 && path < pathClosed.length) { // check if shape not EMPTY
			return pathClosed[path];
		}
		return false;
	}
	
	/** @return As {@link #getVertices(int)}, for the first sub-path. */
	public Point2D[] getVertices () {
		return getVertices(0);
	}
	
	/**
	 * @return a copy of the MOVE_TO and LINE_TO points within the
	 * given sub-path.
	 */
	public Point2D[] getVertices (int path) {
		PathIterator it = getIterator(path);
		int size = getSize(path);
		double[] coords = new double[6];
		Point2D[] points = new Point2D[size];
		for (int i = 0; i < size; i++) {
			it.currentSegment(coords);
			points[i] = new Point2D.Double(coords[0], coords[1]);
			it.next();
		}
		return points;
	}
	
	/**
	 * @return the Shape; note that since Shape has no mutable methods, we return the live object.
	 * Do <em>not</em> cast to a mutable implementation of Shape and alter this object directly,
	 * since many places assume FPath (and therefore the result of this method) are immutable.
	 */
	public Shape getShape() {
		return shape;
	}
	
	/** @return the result of calling {@link #getCoords(int, boolean)} with path=0. */
	public double[] getCoords(boolean latFirst) {
		return getCoords(0, latFirst);
	}
	
	/**
	 * Returns the lat/lon coordinates as a series of doubles, like
	 * 
	 * <code>lat lon lat lon ...</code> (if latFirst is true, or)
	 * <code>lon lat lon lat ...</code> (if latFirst is false)
	 */
	public double[] getCoords(int path, boolean latFirst) {
		PathIterator it = getIterator(path);
		int size = getSize(path);
		double[] coords = new double[6];
		int x = (latFirst ? 1 : 0);
		int y = (latFirst ? 0 : 1);
		double[] latLons = new double[size * 2];
		for (int i = 0; i < latLons.length; i += 2) {
			it.currentSegment(coords);
			latLons[i + x] = coords[0];
			latLons[i + y] = coords[1];
			it.next();
		}
		
		return latLons;
	}
	
	/** @return the path iterator positioned at the start of the given path */
	private PathIterator getIterator(int path) {
		PathIterator it = shape.getPathIterator(null);
		for (int i = 0; i < pathOffsets[path]; i++) {
			it.next();
		}
		return it;
	}
	
	/** @return the number of vertices (MOVE_TO and LINE_TO segments) in the given path. */
	private int getSize(int path) {
		int size = 0; // if shape is EMPTY
		if (pathOffsets.length > (path + 1)) {
			size = pathOffsets[path + 1] - pathOffsets[path];
			if (path >= 0 && path < pathClosed.length) {
				if (pathClosed[path]) {
					size--;
				}
			}
		}
		return size;
	}
	
	/** @return the type of shape in the first path */
	public int getType() {
		return getType(0);
	}
	
	/**
	 * @return the type of the given path as one of the public constants:
	 * <ul>
	 * <li>TYPE_NONE
	 * <li>TYPE_POINT
	 * <li>TYPE_POLYLINE
	 * <li>TYPE_POLYGON
	 * </ul>
	 */
	public int getType (int path) {
		if (isEllipse) {
			return TYPE_ELLIPSE;
		} else if (pathClosed.length == 0)
			return TYPE_NONE;
		else if (getSize(path) < 2)
			return TYPE_POINT;
		else if (pathClosed[path])
			return TYPE_POLYGON;
		else
			return TYPE_POLYLINE;
	}
	
	/**
	 * Returns the vector average for spatial paths, or the positional average
	 * for world paths. If there are multiple subpaths they are all averaged.
	 */
	public Point2D getCenter () {
		double[] coords = new double[6];
		PathIterator it = shape.getPathIterator(null);
		switch (coordSystem) {
		case SPATIAL_EAST:
		case SPATIAL_WEST:
			// vector average
			double lon, lat;
			HVector sum = new HVector (0,0,0);
			while (!it.isDone()) {
				switch (it.currentSegment(coords)) {
				case PathIterator.SEG_MOVETO:
				case PathIterator.SEG_LINETO:
					lon = coordSystem==SPATIAL_EAST ? -coords[0] : coords[0];
					lat = coords[1];
					sum = sum.add(new HVector (lon, lat));
					break;
				}
				it.next();
			}
			lon = coordSystem==SPATIAL_EAST ? FeatureUtil.lonNorm(-sum.lon()) : sum.lon();
			lat = sum.lat();
			return new Point2D.Double (lon, lat);
		case WORLD:
			// If we're working with world coordinates, we need a normalized shape instead, 
			// otherwise a shape that crosses the world meridian gets an incorrect average around 180
			it = Util.normalize360(shape).getPathIterator(null);

			// positional average
			Point2D.Double c = new Point2D.Double();
			int count = 0;
			while (!it.isDone()) {
				switch (it.currentSegment(coords)) {
				case PathIterator.SEG_MOVETO:
				case PathIterator.SEG_LINETO:
					c.x += coords[0];
					c.y += coords[1];
					count ++;
					break;
				}
				it.next();
			}
			if (count > 1) {
				c.x /= count;
				c.y /= count;
			}
			return c;
		default:
			return null;
		}
	}
	
	/**
	 * Returns the spherical area in square kilometers. Currently returns 0.0
	 * for world-coordinate polygons, and open spatial polygons.
	 */
	public double getArea () {
		double area = 0;
		if (coordSystem == SPATIAL_EAST || coordSystem == SPATIAL_WEST) {
			for (int i = 0; i < pathClosed.length; i++) {
				if (pathClosed[i]) {
					Point2D[] vertices = getVertices(i);
					// Since the sphericalArea() method cannot handle duplicate vertices,
					// but such vertices are both legal and do not affect the area, we
					// just remove the duplicates from the array before calling it.
					int length = vertices.length;
					int last = length-1;
					for (int j = 0; j < vertices.length; j++) {
						if (vertices[last].equals(vertices[j])) {
							vertices[j] = null;
							length --;
						} else {
							last = j;
						}
					}
					if (length < vertices.length) {
						Point2D[] tmp = new Point2D[length];
						int idx = 0;
						for (int j = 0; j < vertices.length; j++) {
							if (vertices[j] != null) {
								tmp[idx++] = vertices[j];
							}
						}
						vertices = tmp;
					}
					area += Util.sphericalArea(vertices) * Util.MEAN_RADIUS * Util.MEAN_RADIUS;
				}
			}
		}
		return area;
	}
	
	/**
	 * Returns true if this FPath encloses the given point. The given point must
	 * be in the same coordinate system as this path.
	 */
	public boolean contains (Point2D p) {
		switch (coordSystem) {
		case WORLD:
			return shape.contains(p);
		case SPATIAL_EAST:
			return new SPolygon(shape).contains(new HVector(-p.getX(),p.getY()));
		case SPATIAL_WEST:
			return new SPolygon(shape).contains(new HVector(p.getX(),p.getY()));
		default:
			return false;
		}
	}
	
	private static ThreadLocal<Rectangle2D.Double> mouseBoxTL = new ThreadLocal<Rectangle2D.Double>() {
		protected Rectangle2D.Double initialValue() {
			return new Rectangle2D.Double();
		}
	};
	
	/**
	 * @return true if the given rect intersects this path. The rectangle is
	 *         assumed to be in the same coordinate system as this FPath. If
	 *         this FPath is in world coordinates, then the test is done modulus
	 *         360 on the shape and rectangle. If this FPath is spatial, then
	 *         the test returns true if and only if there is some area of
	 *         overlap between the resulting {@link SPolygo}ns.
	 */
	public boolean intersects(Rectangle2D rect) {
		if (pathClosed.length < 1)
			return false;
		switch (coordSystem) {
		case WORLD:
			Rectangle2D bound = shape.getBounds2D();
			Rectangle2D.Double mouseBox = mouseBoxTL.get();
			mouseBox.setFrame(rect);
			if (mouseBox.y > bound.getMaxY() || mouseBox.getMaxY() < bound.getMinY()) {
				return false;
			}
			mouseBox.x += Math.floor((bound.getMinX() - mouseBox.x) / 360) * 360;
			while (mouseBox.x <= bound.getMaxX()) {
				if (mouseBox.getMaxX() >= bound.getMinX()) {
					switch (getType()) {
					case TYPE_POINT:
						if (mouseBox.contains(bound.getMinX(), bound.getMinY())) {
							return true;
						}
						break;
					case TYPE_POLYLINE:
						final Line2D.Double line = new Line2D.Double();
						double[] coords = new double[6];
						PathIterator it = shape.getPathIterator(null);
						int place = 0;
						while (!it.isDone()) {
							int code = it.currentSegment(coords);
							switch (code) {
							case PathIterator.SEG_MOVETO:
								place = 0;
								// flow into LINE_TO case to get coordinates
							case PathIterator.SEG_LINETO:
								line.x1 = line.x2;
								line.x2 = coords[0];
								line.y1 = line.y2;
								line.y2 = coords[1];
								break;
							}
							if (place > 0 && line.intersects(mouseBox)) {
								return true;
							}
							place ++;
							it.next();
						}
						break;
					case TYPE_POLYGON:
						if (shape.intersects(mouseBox)) {
							return true;
						}
						break;
					}
				}
				mouseBox.x += 360;
			}
			return false;
		case SPATIAL_WEST:
		case SPATIAL_EAST:
			// tests for area of intersection > 0, assuming rect contains four
			// spatial points in the same east/west sense as this FPath
			return SPolygon.area(new SPolygon(shape), new SPolygon(rect)) > 0.0;
		default:
			return false;
		}
	}
	
	/**
	 * Convenience method that calls convertTo(SPATIAL_WEST)
	 */
	public FPath getSpatialWest () {
		return convertTo (SPATIAL_WEST);
	}
	
	/**
	 * Convenience method that calls convertTo(SPATIAL_EAST)
	 */
	public FPath getSpatialEast () {
		return convertTo (SPATIAL_EAST);
	}
	
	/**
	 * Convenience method that calls convertTo(WORLD)
	 */
	public FPath getWorld () {
		return convertTo (WORLD);
	}
	
	/** Converts the given coordinate array to an array of points. */
	private static final Point2D[] coordsToVertices (double[] coords, boolean latFirst) {
		Point2D[] points = new Point2D[coords.length/2];
		int x = (latFirst ? 1 : 0);
		int y = (latFirst ? 0 : 1);
		for (int i = 0; i < coords.length/2; i++)
			points[i] = new Point2D.Double (coords[i*2 + x], coords[i*2 + y]);
		return points;
	}

	private static final Point2D[] coordsToVertices (float[] coords, boolean latFirst) {
		Point2D[] points = new Point2D[coords.length/2];
		int x = (latFirst ? 1 : 0);
		int y = (latFirst ? 0 : 1);
		for (int i = 0; i < coords.length/2; i++)
			points[i] = new Point2D.Double (coords[i*2 + x], coords[i*2 + y]);
		return points;
	}

	/** Converts the given point array into a Path2D */
	private static final Path2D verticesToPath (Point2D[] points, boolean closed) {
		Path2D path=new Path2D.Double();
				
		if (points.length > 0) {
			path.moveTo(points[0].getX(), points[0].getY());
			for (int i = 1; i < points.length; i++)
				path.lineTo(points[i].getX(), points[i].getY());
			if (closed)
				path.closePath();
		}
		return path;
	}
	
	/**
	 * @return a transformation from one coordinate system to another,
	 * or null if no transform can be found.
	 */
	private static final Transformer getTransform(int from, int to) {
		switch (from) {
		case WORLD:
			switch (to) {
			case SPATIAL_EAST: return Main.PO.worldToSpatialEast;
			case SPATIAL_WEST: return Main.PO.worldToSpatial;
			}
			break;
		case SPATIAL_EAST:
			switch (to) {
			case WORLD: return Main.PO.spatialEastToWorld;
			case SPATIAL_WEST: return toggleLon;
			}
			break;
		case SPATIAL_WEST:
			switch (to) {
			case WORLD: return Main.PO.spatialToWorld;
			case SPATIAL_EAST: return toggleLon;
			}
			break;
		}
		return null;
	}
	
	/**
	 * Returns this Path converted into the given coordinate coordinate system.
	 * If this Path is already in the requested coordinate system, this
	 * operation simply returns 'this'.
	 * 
	 * @param coordSystem One of WORLD, SPATIAL_EAST, or SPATIAL_WEST
	 */
	public final FPath convertTo(int coordSystem) {
		if (this.coordSystem == coordSystem) {
			return this;
		}
		if (coordSystem < WORLD || coordSystem > SPATIAL_WEST) {
			throw new IllegalArgumentException("Unrecognized coordinate system");
		}
		Transformer t = getTransform(this.coordSystem, coordSystem);
		if (t == null) {
			throw new IllegalArgumentException("Cannot transform from " + this.coordSystem + " to " + coordSystem);
		}
		PathIterator transformed = new TransformingIterator(shape.getPathIterator(null), t);
		Path2D.Double out = new Path2D.Double();
		out.append(transformed, false);
		return new FPath(out, coordSystem);
	}
	
	
	/**
	 * Gets the current path in spatial east coords and returns it
	 * in a transform to world coords using the passed in ProjObj.
	 * @param po  ProjObj to convert from spatial to world
	 * @return  A path in world coordinates based on the ProjObj passed in.
	 */
	public FPath convertToSpecifiedWorld(ProjObj po){
		//get the spatial path
		FPath path = getSpatialEast();
		//convert to world coords in the specified ProjObj
		//Most of the following code was copied from FPath.convertTo
		Transformer t = po.spatialEastToWorld;
		PathIterator transformed = new TransformingIterator(path.getShape().getPathIterator(null), t);
		Path2D.Double out = new Path2D.Double();
		out.append(transformed, false);
		
		return new FPath(ShapeUtil.path2Path(out, FPath.WORLD, po), FPath.WORLD);
	}
	
	
	/**
	 * Gets the current path to spatial east and returns the path in world coordinates
	 * in a transform passed off the passed in projections
	 * @param from The projection coming in
	 * @param to The projection going out
	 * @return A path in world coordinates based on the projObj coming in
	 */
	public FPath convertToSpecifiedWorld(ProjObj from, ProjObj to){

		//convert to spatial east from world in the specified ProjObj and do the first transform
		//Most of the following code was copied from FPath.convertTo
		Transformer t = from.worldToSpatialEast;
		PathIterator transformed = new TransformingIterator(getShape().getPathIterator(null), t);
		Path2D.Double out = new Path2D.Double();
		out.append(transformed, false);
		
		FPath spatialFP =  new FPath(new GeneralPath(out), FPath.SPATIAL_EAST);
		
		//convert to world coords 
		t = to.spatialEastToWorld;
		transformed = new TransformingIterator(spatialFP.getShape().getPathIterator(null), t);
		out = new Path2D.Double();
		out.append(transformed, false);
		
		return new FPath(ShapeUtil.path2Path(out, FPath.WORLD, to), FPath.WORLD);
		
	}
	
	public String toString(){
		StringBuffer sbuf = new StringBuffer("FPath[");
		
		String coordSys = "UNKNOWN";
		switch(getCoordSystem()){
		case WORLD: coordSys = "world"; break;
		case SPATIAL_EAST: coordSys = "east"; break;
		case SPATIAL_WEST: coordSys = "west"; break;
		}
		sbuf.append("coordSys="+coordSys+";");
		
		sbuf.append("coords=");
		double[] coords = getCoords(false);
		for(int i=0; i<coords.length; i++){
			if (i > 0)
				sbuf.append(",");
			sbuf.append(nf.format(coords[i]));
		}
		sbuf.append("]");
		return sbuf.toString();
	}
	
	public static final class GeometryAdapter {
		private final GeometryFactory fac = new GeometryFactory();
		private final ShapeReader sr = new ShapeReader(fac);
		private final ShapeWriter sw = new ShapeWriter();
		public Geometry getGeometry(FPath path) {
			PathIterator it = path.getShape().getPathIterator(null);
			switch (path.getType()) {
			case FPath.TYPE_POINT:
				List<Coordinate[]> pointCoords = ShapeReader.toCoordinates(it);
				Point[] points = new Point[pointCoords.size()];
				for (int i = 0; i < points.length; i++) {
					points[i] = fac.createPoint(pointCoords.get(i)[0]);
				}
				if (points.length > 1) {
					return fac.createMultiPoint(points);
				} else {
					return points[0];
				}
			case FPath.TYPE_POLYLINE:
				List<Coordinate[]> lineCoords = ShapeReader.toCoordinates(it);
				LineString[] lines = new LineString[lineCoords.size()];
				for (int i = 0; i < lines.length; i++) {
					lines[i] = fac.createLineString(lineCoords.get(i));
				}
				if (lines.length > 1) {
					return fac.createMultiLineString(lines);
				} else {
					return lines[0];
				}
			case FPath.TYPE_POLYGON:
				return sr.read(it);
			default:
				return null;
			}
		}
		private static final Path2D.Double seq2path(CoordinateSequence seq) {
			Path2D.Double path = new Path2D.Double();
			int size = seq.size();
			int lastIdx = size - 1;
			Coordinate first = seq.getCoordinate(0);
			path.moveTo(first.x, first.y);
			for (int i = 1; i < lastIdx; i++) {
				Coordinate c = seq.getCoordinate(i);
				path.lineTo(c.x, c.y);
			}
			Coordinate last = seq.getCoordinate(lastIdx);
			if (size > 3 && first.equals(last)) {
				path.closePath();
			} else {
				path.lineTo(last.x, last.y);
			}
			return path;
		}
		public Path2D.Double getPath(Geometry geom) {
			if (geom instanceof Point) {
				Coordinate c = ((Point)geom).getCoordinate();
				Path2D.Double path = new Path2D.Double();
				path.moveTo(c.x, c.y);
				return path;
			} else if (geom instanceof LineString) {
				return seq2path(((LineString) geom).getCoordinateSequence());
			} else if (geom instanceof LinearRing) {
				return seq2path(((LinearRing)geom).getCoordinateSequence());
			} else if (geom instanceof Polygon) {
				Polygon poly = (Polygon)geom;
				Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
				path.append(seq2path(poly.getExteriorRing().getCoordinateSequence()), false);
				int holes = poly.getNumInteriorRing();
				for (int i = 0; i < holes; i++) {
					path.append(seq2path(poly.getInteriorRingN(i).getCoordinateSequence()), false);
				}
				return path;
			} else if (geom instanceof GeometryCollection) {
				GeometryCollection gc = (GeometryCollection)geom;
				Path2D.Double path = new Path2D.Double();
				int parts = gc.getNumGeometries();
				for (int i = 0; i < parts; i++) {
					path.append(getPath(gc.getGeometryN(i)), false);
				}
				return path;
			} else if (geom == null) {
				return null;
			} else {
				throw new IllegalArgumentException("Unrecognized geometry type " + geom.getClass().getName());
			}
		}
	}
}
