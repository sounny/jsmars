package edu.asu.jmars.layer.util.features;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.graphics.FontRenderer;
import edu.asu.jmars.util.LineType;


/**
 * Realizes the FeatureRenderer for the ShapeLayer. The ShapeLayer uses this
 * Renderer to render Features to the screen or the off-line buffer.
 * 
 * Methods in this class which take a Graphics2D as a parameter require a World
 * Graphics2D.
 * 
 * The ShapeRenderer uses an instance of {@link Styles} to get all of the info
 * it requires for rendering.  This distinction between feature attributes and
 * styles allows using a renderer with a fixed set of understood attributes
 * with any kind of Feature by mapping attributes to styles through the Styles
 * class.
 */
public class ShapeRenderer {
	/** BasicStroke's end-cap style. */
	private static final int defaultStrokeCapStyle = BasicStroke.CAP_BUTT;

	/** BasicStroke's end-join style. */
	private static final int defaultStrokeJoinStyle = BasicStroke.JOIN_MITER;

	/** BasicStroke's miter-limit. */
	private static final float defaultMiterLimit = 10.0f;

	/**
	 * A standard arrowhead based on the static width and height parameters. The
	 * arrowhead is aligned with the x-axis with its tip at the origin and the
	 * base of the triangle extending in the negative-X direction.
	 */
	private static final Path2D arrowHead;
	static {
		// Height of the arrowhead from the base (in pixels).
		final int ahHeight = 15;
		// Width of the arrowhead base (in pixels).
		final int ahWidth = 12;
		final double ahHalfWidth = ahWidth / 2f;
		// Populate the default arrowhead as a head aligned with the X-axis
		// pointing at (0,0)
		Path2D ah = new Path2D.Double();
		ah.moveTo(0, 0);
		ah.lineTo(-(double) ahHeight, -ahHalfWidth);
		ah.lineTo(-(double) ahHeight, ahHalfWidth);
		ah.closePath();
		arrowHead = ah;
	}
	
	private static final LineType noLineDash = new LineType();
	
	/** The label's x offset in world coordinates from object center */
	private final double labelOffsetX;
	/** The label's y offset in world coordinates from object center */
	private final double labelOffsetY;
	/** the font renderer */
	private final FontRenderer fRenderer;
	/** the pixels per degree of this renderer */
	private final int ppd;
	/** the styles to use for this renderer */
	private final Styles styles = new Styles();
	/** Cached rectangles */
	private final Map<Integer,Rectangle2D.Double> sharedVertexBoxes = new HashMap<Integer,Rectangle2D.Double>();
	/** Cached strokes */
	private final Map<Integer,Map<Double,Stroke>> strokeCaches = new HashMap<Integer,Map<Double,Stroke>>();
	/** Source of FPath instances */
	private StyleSource<FPath> pathSource;
	
	/**
	 * Create a renderer instance suitable for one rendering pass.
	 * Reusable objects within a single rendering pass are cached
	 * on the renderer, so if style or other changes cause a new
	 * rendering to be done, it should be with a new ShapeRenderer
	 * instance.
	 * @param ppd The pixels per degree of this renderer.
	 */
	public ShapeRenderer(int ppd) {
		super();
		this.ppd = ppd;
		fRenderer = new FontRenderer();
		labelOffsetX = labelOffsetY = 3f/ppd;
		pathSource = styles.geometry.getSource();
	}
	
	public Styles getStyleCopy() {
		return new Styles(styles);
	}
	
	public void setStyles(Styles styles) {
		this.styles.setFrom(styles);
		pathSource = styles.geometry.getSource();
	}
	
	/*
	 * This is used to access the path that would be drawn to the screen for a feature.  This may vary from the path of the feature itself, such as in the
	 * case of Circles.  The feature itself is a single point, but it is rendered to the screen as a set of points at a given radius from that point.
	 * When calculating stamp/shape intersections, the difference between a single point and a circle is very noticeable to the user. 
	 */
	FPath getPath(Feature f) {
		return pathSource.getValue(f).getWorld();	
	}
	
	/**
	 * Draws the given Feature onto the specified World Graphics2D.  Also needs
	 * a screen graphics object to properly draw the label.
	 * While drawing, ignore everything that does not fall within our current display
	 * boundaries. In addition pay attention to the controlling flags, such as
	 * "show-label-off", "show-vertices-off", "minimum-line-width" etc.
	 * 
	 * @param g2w
	 *            World Graphics2D to draw shape into.
	 * @param g2s
	 * 			  Screen Graphics2D to draw label into.
	 * @param f
	 *            Feature object to render.
	 */
	public void draw(Graphics2D g2w, Graphics2D g2s, Feature f, int ppd){
		draw(g2w, g2s, f, null, 1, ppd);
	}
	 

	public void draw(Graphics2D g2w, Graphics2D g2s, Feature f){
		draw(g2w, g2s, f, null, 1, -1);
	}

	public void draw(Graphics2D g2w, Graphics2D g2s, Feature f, ProjObj po, int scale){
		draw(g2w, g2s, f, po, scale, -1);
	}

	/**
	 * 
	 * @param g2w
	 * @param g2s
	 * @param f
	 * @param po
	 * @param scale
	 * @param ppd - used only for Style Fill scaling
	 */
	public void draw(Graphics2D g2w, Graphics2D g2s, Feature f, ProjObj po, int scale, int ppd) {
		try {
			
			if (f.isHidden()) {
				return;
			}
			//Use this boolean to determine if this drawing is done
			// for 3d or not. Special logic applies in a few cases if so
			boolean for3D = false;
			if(po!=null){
				for3D = true;
			}
			
			g2w.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				styles.antialias.getValue(f)
					? RenderingHints.VALUE_ANTIALIAS_ON
					: RenderingHints.VALUE_ANTIALIAS_OFF);
			
			// Get the path and path type
			
			//prevent NPE
			FPath path = pathSource.getValue(f);
			if (path == null) {
				return;
			}
			if(for3D){
				path = path.convertToSpecifiedWorld(po);
			}else{
				path = path.getWorld();
			}
			//prevent NPE
			if (path == null) {
				return;
			}
			Shape p = path.getShape();
			int type = path.getType();
			if (type == FPath.TYPE_NONE || p == null)
				return;
			
			// Install various pieces of style as needed and draw.
			
			// Draw filled polygon.
			if (type == FPath.TYPE_POINT) {
				g2w.setColor(styles.fillColor.getValue(f));
				fillVertices(g2w, p, styles.pointSize.getValue(f).intValue()*scale);
			} else if (type == FPath.TYPE_POLYGON) {
				g2w.setColor(styles.fillColor.getValue(f));
				
				g2w.setPaint(styles.fillStyle.getValue(f).getReversedPaint(ppd));
				
				if (styles.fillPolygons.getValue(f)) {
					g2w.fill(p);
				}
			} else if (type == FPath.TYPE_ELLIPSE) {
				g2w.setColor(styles.fillColor.getValue(f));
				if (styles.fillPolygons.getValue(f)) {
					g2w.fill(p);
				}
			}
			
			double lineWidth = styles.lineWidth.getValue(f).doubleValue()*scale;
			
			g2w.setColor(styles.lineColor.getValue(f));
			
			if (type == FPath.TYPE_POLYLINE || styles.drawOutlines.getValue(f)) {
				g2w.setStroke(getStroke(lineWidth, styles.lineDash.getValue(f)));
				
				if (type == FPath.TYPE_POINT)
					drawVertices(g2w, p, styles.pointSize.getValue(f).intValue()*scale);
				else
					g2w.draw(p);
			}
			
			g2w.setStroke(getStroke(lineWidth, noLineDash));
			
			// Draw vertices.
			if (f.getPath().getType() != FPath.TYPE_POINT && styles.showVertices.getValue(f))
				drawVertices(g2w, p, styles.vertexSize.getValue(f).intValue());
			
			// Draw direction arrows.
			if (type == FPath.TYPE_POLYLINE && styles.showLineDir.getValue(f)) {
				Line2D lastSeg = getLastSegment(p);
				Path2D arrowHead = makeArrowHead(lastSeg);
				g2w.fill(arrowHead);
			}

			
			// Draw optional text. (Don't draw if for3D)
			if (!for3D && styles.showLabels.getValue(f) && g2s!=null) {
				String label = styles.labelText.getValue(f);
				Color labelColor = styles.labelColor.getValue(f);
				Color labelOutlineColor = styles.labelBorderColor.getValue(f);
				int fontSize = styles.labelSize.getValue(f).intValue();
				int fontStyleInt = Font.PLAIN;
				String fontStyle = styles.labelStyle.getValue(f);
				String fontName = styles.labelFont.getValue(f);
				Point2D center = path.getCenter();
				//need to be drawing in screen coordinates for labels
				Point2D scCenter = Main.testDriver.mainWindow.getProj().world.toScreen(center);
				//We need to make sure we get all the screen coordinates this point may
				// show up on (if the world wraps it will show in multiple places)
				//Only care about the x direction, doesn't wrap in the y direction
				Dimension s = Main.testDriver.mainWindow.getSize();
				double maxX = s.getWidth();
				ArrayList<Double> xs = new ArrayList<Double>();
				double x = scCenter.getX();
				//Add the original screen x
				xs.add(x);
				//First go to the left (negative world coords)
				int j = 0;
				double centerX = center.getX();
				while(x>0){
					centerX = centerX - 360;
					Point2D newWorld = new Point2D.Double(centerX, center.getY());
					Point2D newScreen = Main.testDriver.mainWindow.getProj().world.toScreen(newWorld);
					x = newScreen.getX();
					if(x>0){
						xs.add(x);
					}
				}
				//Next start over and go right (positive world coords)
				centerX = center.getX();
				while(x<maxX){
					centerX = centerX + 360;
					Point2D newWorld = new Point2D.Double(centerX, center.getY());
					Point2D newScreen = Main.testDriver.mainWindow.getProj().world.toScreen(newWorld);
					x = newScreen.getX();

					if(x<maxX){
						xs.add(x);
					}
				}
				//The y value doesn't change
				double y = scCenter.getY();
				int xOffset = 8;
				int yOffset = 0;
				
				
				if (fontStyle.equalsIgnoreCase("plain")) {
					fontStyleInt = Font.PLAIN;
				} else if (fontStyle.equalsIgnoreCase("bold")) {
					fontStyleInt = Font.BOLD;
				} else if (fontStyle.equalsIgnoreCase("italic")) {
					fontStyleInt = Font.ITALIC;
				}
			    fRenderer.setFont(new Font(fontName, fontStyleInt, fontSize));
				fRenderer.setForeground(labelColor);
				fRenderer.setOutlineColor(labelOutlineColor);
				
				//draw all the labels that are on the screen
				float yVal = (float)y + yOffset;
				for(int i=0; i<xs.size(); i++){
					float xVal = xs.get(i).floatValue() + xOffset;
					fRenderer.paintLabel(g2s, label, xVal, yVal);
				}
			}
		} catch (ClassCastException ex) {
			ex.printStackTrace();
		} catch (NullPointerException ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Returns the shared instance of vertex box used for various drawing
	 * routines. The vetex box is constructed using the LView's current
	 * projection and the preset defaultVertexBoxSide.
	 * 
	 * @return A shared instance of vertex box.
	 * 
	 * @see #sharedVertexBoxes
	 * @see #defaultVertexBoxSide
	 * @see #drawVertices(Graphics2D, Path2D)
	 * @see #fillVertices(Graphics2D, Path2D)
	 */
	private Rectangle2D.Double getSharedVertexBox(int width) {
		Rectangle2D.Double sharedVertexBox = sharedVertexBoxes.get(width);
		
		if (sharedVertexBox == null) {
			double size = width*1.0/ppd;
			sharedVertexBox = new Rectangle2D.Double(-size, -size, size*2, size*2);
			sharedVertexBoxes.put(width, sharedVertexBox);
		}
		
		return sharedVertexBox;
	}
	
	private Stroke getStroke(double width, LineType type) {
		Map<Double,Stroke> typeCache = strokeCaches.get(type.getType());
		if (typeCache == null) {
			typeCache = new HashMap<Double,Stroke>();
			strokeCaches.put(type.getType(), typeCache);
		}
		Stroke stroke = typeCache.get(width);
		if (stroke == null) {
			if (typeCache.size() > 1000) {
				typeCache.clear();
			}
			float[] pattern = type.getDashPattern();
			if (pattern != null) {
				float[] newPat = new float[pattern.length];
				for (int i = 0; i < newPat.length; i++) {
					newPat[i] = pattern[i] / ppd;
				}
				pattern = newPat;
			}
			stroke = new BasicStroke((float)width/ppd, defaultStrokeCapStyle,
				defaultStrokeJoinStyle, defaultMiterLimit, pattern, 0);
			typeCache.put(width, stroke);
		}
		return stroke;
	}
	
	/**
	 * Draws exagerated vertices for the given Path2D. The vertices are
	 * drawn with the help of shared vertex box created by
	 * {@linkplain #getSharedVertexBox()}.
	 * 
	 * @param g2w
	 *            World graphics context in which the drawing takes place.
	 * @param p
	 *            Path2D for which exagerated vertices are drawn.
	 * @throws IllegalArgumentException
	 *             if the Path2D contains quadratic/cubic segments.
	 */
	private void drawVertices(Graphics2D g2w, Shape p, int width) {
		double[] coords = new double[6];
		Rectangle2D.Double v = getSharedVertexBox(width);

		PathIterator pi = p.getPathIterator(null);
		while (!pi.isDone()) {
			switch (pi.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
			case PathIterator.SEG_LINETO:
				v.x = coords[0] - v.width / 2.0;
				v.y = coords[1] - v.height / 2.0;
				g2w.draw(v);
				break;
			case PathIterator.SEG_CLOSE:
				break;
			default:
				throw new IllegalArgumentException(
						"drawVertices() called with a Path2D with quadratic/cubic segments.");
			}
			pi.next();
		}
	}

	/**
	 * Draws filled exagerated vertices for the given Path2D. The vertices
	 * are drawn with the help of shared vertex box created by
	 * {@linkplain #getSharedVertexBox()}.
	 * 
	 * @param g2w
	 *            World graphics context in which the drawing takes place.
	 * @param p
	 *            Path2D for which exagerated vertices are drawn.
	 * @throws IllegalArgumentException
	 *             if the Path2D contains quadratic/cubic segments.
	 */
	private void fillVertices(Graphics2D g2w, Shape p, int pointWidth) {
		double[] coords = new double[6];
		Rectangle2D.Double v = getSharedVertexBox(pointWidth);

		PathIterator pi = p.getPathIterator(null);
		while (!pi.isDone()) {
			switch (pi.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
			case PathIterator.SEG_LINETO:
				v.x = coords[0] - v.width / 2.0f;
				v.y = coords[1] - v.height / 2.0f;
				g2w.fill(v);
				break;
			case PathIterator.SEG_CLOSE:
				break;
			default:
				throw new IllegalArgumentException(
						"drawVertices() called with a Path2D with quadratic/cubic segments.");
			}
			pi.next();
		}
	}

	/**
	 * Returns a polygon containing the arrowhead for the specified line
	 * segment. The tip of the arrowhead is located at the second of the two
	 * points that make up the line segment.
	 * 
	 * @param lineSeg
	 *            Line segment for which an arrow is desired.
	 * @return Arrow ending at the second point of the line segment.
	 */
	protected Path2D makeArrowHead(Line2D lineSeg) {
		Path2D ah = (Path2D)arrowHead.clone();

		double x = lineSeg.getX2() - lineSeg.getX1();
		double y = lineSeg.getY2() - lineSeg.getY1();
		double norm = Math.sqrt(x * x + y * y);
		x /= norm;
		y /= norm;

		// Get angle and put it in the correct half circle.
		double theta = (y < 0) ? -Math.acos(x) : Math.acos(x);

		AffineTransform at = new AffineTransform();

		// Translate it to the end point of the line-segment.
		at.concatenate(AffineTransform.getTranslateInstance(lineSeg.getX2(), lineSeg.getY2()));

		// Rotate arrow to align with the given line-segment.
		at.concatenate(AffineTransform.getRotateInstance(theta));

		// Scale according to projection
		double scale = 1f/ppd;
		
		at.concatenate(AffineTransform.getScaleInstance(scale, scale));

		// Apply rotation and translation.
		ah.transform(at);

		return ah;
	}

	/**
	 * Returns the last line segment from a given Path2D. The Path2D
	 * must have such a segment, otherwise, an IllegalArgumentException is
	 * thrown.
	 * 
	 * @param p
	 *            Path2D for which the last segment is to be returned.
	 * @return The last line segment.
	 * @throws {@link IllegalArgumentException}
	 *             if cubic or quadratic coordinates are encountered, or the
	 *             polygon is a closed polygon or it does not contain enough
	 *             vertices.
	 */
	protected static Line2D getLastSegment(Shape p) {
		PathIterator pi = p.getPathIterator(null);
		Point2D p1 = new Point2D.Double();
		Point2D p2 = new Point2D.Double();
		double[] coords = new double[6];
		int nSegVertices = 0;

		while (!pi.isDone()) {
			switch (pi.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
				nSegVertices = 0;
			case PathIterator.SEG_LINETO:
				nSegVertices++;
				p1.setLocation(p2);
				p2.setLocation(coords[0], coords[1]);
				break;
			case PathIterator.SEG_CUBICTO:
			case PathIterator.SEG_QUADTO:
				throw new IllegalArgumentException(
						"getLastSegment() called with cubic/quadratic curve.");
			case PathIterator.SEG_CLOSE:
				throw new IllegalArgumentException(
						"getLastSegment() called with closed polygon.");
			}
			pi.next();
		}

		if (nSegVertices < 2) {
			throw new IllegalArgumentException(
					"getLastSegment() called with a path without a usable segment.");
		}

		return new Line2D.Double(p1, p2);
	}
}
