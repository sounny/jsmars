package edu.asu.jmars.layer;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.TestDriverLayered;
import edu.asu.jmars.graphics.SpatialGraphics2D;
import edu.asu.jmars.graphics.SpatialGraphicsCyl;
import edu.asu.jmars.swing.Dimension2D_Double;

/**
 ** Contains the state and methods for transforming between screen,
 ** world, and spatial coordinates. Methods are provided for
 ** converting to and from each coordinate system, as well as to and
 ** from raw HVectors. <code>MultiProjection</code> objects and all
 ** their members are immutable.
 **
 ** <p>The object itself only supplies methods for retrieving the
 ** screen/world {@link AffineTransform}s and methods for determining
 ** the size, extent, and resolution of the current viewing
 ** window. The bulk of the actual conversion methods lie in child
 ** {@link SingleProjection} objects. These are exposed as member
 ** variables {@link #screen}, {@link #world}, and {@link #spatial}.
 **
 ** <p>Conversion between coordinate systems is accomplished by
 ** calling the appropriate "to" function of the source coordinate
 ** system's corresponding <code>SingleProjection</code> object. For
 ** example, to go from world coordinates to screen coordinates, one
 ** could do:
 **
 ** <p><code>MultiProjection proj;
 ** double worldX, worldY;
 ** Point2D screenPt;
 ** screenPt = proj.world.toScreen(worldX, worldY);
 ** // or, equivalently:
 ** Point2D worldPt;
 ** screenPt = proj.world.toScreen(worldPt);
 ** </code>
 **/
public abstract class MultiProjection
 {
	abstract protected SingleProjection createScreen();
	abstract protected SingleProjection createWorld();
	abstract protected SingleProjection createSpatial();
	public MultiProjection()
	 {
		this.screen  = createScreen();
		this.world   = createWorld();
		this.spatial = createSpatial();
	 }

	public final SingleProjection screen;
	public final SingleProjection world;
	public final SingleProjection spatial;

	public abstract AffineTransform getScreenToWorld();
	public abstract AffineTransform getWorldToScreen();

	public abstract Rectangle2D getWorldWindow();
	public abstract Shape getWorldWindowMod();

	public abstract Dimension getScreenSize();
	public abstract Rectangle getScreenWindow();
	public abstract float getPixelWidth();
	public abstract float getPixelHeight();
	public abstract int getPPD();
	public abstract int getPPDLog2();

	public boolean isVisible(Shape sh)
	 {
		return  getWorldWindowMod().intersects(sh.getBounds2D());
	 }

	public Dimension2D getPixelSize()
	 {
		return  new Dimension2D_Double(getPixelWidth(), getPixelHeight());
	 }

	public Rectangle2D getClickBox(Point2D worldCenter, int pixelRadius)
	 {
		double w = getPixelWidth() * pixelRadius;
		double h = getPixelHeight() * pixelRadius;
		return  new Rectangle2D.Double(worldCenter.getX() - w,
									   worldCenter.getY() - h,
									   w * 2, h * 2);
	 }

	/**
	 ** Given a graphics context that draws in world coordinates,
	 ** returns one that draws using spatial coordinates (lon/lat in
	 ** degs). This WAYYY doesn't belong here in the abstract base (it
	 ** should be an abstract method that's defined in the
	 ** subclasses), but oh well for now. Returns null iff null is
	 ** passed in.
	 **/
	public SpatialGraphics2D createSpatialGraphics(Graphics2D g2)
	 {
		if(g2 == null)
			return  null;
		else
			return  new SpatialGraphicsCyl(g2, this);
	 }

	public Rectangle2D screen2world(Rectangle2D s)
	 {
		Rectangle2D.Double w = new Rectangle2D.Double();
		w.setFrameFromDiagonal(screen.toWorld(s.getMinX(), s.getMinY()),
							   screen.toWorld(s.getMaxX(), s.getMaxY()));
		w.x -= Math.floor(w.x / 360) * 360;
		return  w;
	 }

	/**
	 ** Given a width in pixels, returns a world-coordinate stroke
	 ** that paints at that width.
	 **
	 ** <b>NOT USEFUL IN THE TIME PROJECTION!</b> It can be adapted,
	 ** though... please ask Michael.
	 **
	 ** @throws ArrayIndexOutOfBoundsException if the pixelWidth is
	 ** less than zero or greater than {@link #WORLD_STROKE_MAX}.
	 **/
	public Stroke getWorldStroke(int widthInPixels)
	 {
		return  worldStrokes[widthInPixels][getPPDLog2()];
	 }

	/**
	 ** Maximum pixel size supported by {@link #getWorldStroke},
	 ** currently 10.
	 **/
	public static final int WORLD_STROKE_MAX = 10;

	private static final Stroke[][] worldStrokes = createWorldStrokes();
	private static Stroke[][] createWorldStrokes()
	 {
		int maxPPDLog2 = TestDriverLayered.INITIAL_MAX_ZOOM_LOG2;

		Map allStrokes = new HashMap(); // from Float objects to Stroke objects
		Stroke[][] indexed = new Stroke[WORLD_STROKE_MAX+1][maxPPDLog2];

		for(int i=0; i<indexed.length; i++)
			for(int j=0; j<indexed[i].length; j++)
			 {
				float width = (float) i / (1<<j);
				Object key = new Float(width);
				Stroke s = (Stroke) allStrokes.get(key);
				if(s == null)
					allStrokes.put(key, s = new BasicStroke(width));
				indexed[i][j] = s;
			 }

		return  indexed;
	 }
	
	public static MultiProjection getIdentity(){
		return new IdentityProjection();
	}
	
	/**
	 * Identity projection for testing purposes.
	 * 
	 * @author saadat
	 */
	public static class IdentityProjection extends MultiProjection {
		protected AffineTransform identityTransform = new AffineTransform();

		protected SingleProjection createScreen(){ return  new IdentitySingle(); }
		protected SingleProjection createWorld(){ return  new IdentitySingle(); }
		protected SingleProjection createSpatial(){ return  new IdentitySingle(); }

		public AffineTransform getScreenToWorld(){ return identityTransform; }
		public AffineTransform getWorldToScreen(){ return identityTransform; }

		public Shape getWorldWindowMod(){
			return  null;
		}

		public Rectangle getScreenWindow(){
			return  null;
		}

		public Rectangle2D getWorldWindow(){
			return  null;
		}

		public Dimension getScreenSize(){
			return  null;
		}
		public float getPixelWidth(){ return  1; }
		public float getPixelHeight(){ return  1; }
		public int getPPDLog2(){ return  3; }
		public int getPPD(){ return  8; }

		private class IdentitySingle extends SingleProjection{
			public Point2D toScreen(double x, double y){
				return new Point2D.Double(x,y);
			}
			public Point2D toScreenLocal(double x, double y){
				return new Point2D.Double(x,y);
			}
			public Point2D toWorld(double x, double y){
				return new Point2D.Double(x,y);
			}
			public Point2D toSpatial(double x, double y){
				return new Point2D.Double(x,y);
			}
			public Point2D fromHVector(double x, double y, double z){
				return new Point2D.Double();
			}
			public double distance(double ax, double ay, double bx, double by){
				return Math.sqrt((bx-ax)*(bx-ax)+(by-ay)*(by-ay));
			}
			public double distance(double a1x, double a1y, double a2x, double a2y,
					double px, double py){
				// http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
				return  ((a2x-a1x)*(a1y-py)-(a1x-px)*(a2y-a1y))/distance(a1x,a1y,a2x,a2y);
			}
			public boolean hitTest(double a1x, double a1y, double a2x, double a2y,
					double b1x, double b1y, double b2x, double b2y){
				
				Rectangle2D r1 = new Rectangle2D.Double();
				r1.setFrameFromDiagonal(a1x,a1y,a2x,a2y);
				Rectangle2D r2 = new Rectangle2D.Double();
				r2.setFrameFromDiagonal(b1x,b1y,b2x,b2y);
				
				return r1.intersects(r2);
			}
			public Point2D nearPt(double a1x, double a1y, double a2x, double a2y,
					double px, double py, double maxDist){
				// ClosestPoint = PointOnLine + ((Point - PointOnLine) * LineDir) * LineDir;
				// ClosestPoint = P1+(P2-P1)*(Point-P1))*(P2-P1)/((P2-P1)*(P2-P1))
				
                // Perpendicular to the segment, normalized
                // double aPx = a1y - a2y;
                // double aPy = a2x - a1x;
                double aPx = a2x - a1x;
                double aPy = a2y - a1y;

				double aPmag = Math.sqrt(aPx*aPx + aPy*aPy);
				aPx /= aPmag;
				aPy /= aPmag;

                // Make a vector out of p as well
                double vPy = py - a1y;
                double vPx = px - a1x;

				// Determine what portion of p is the perpendicular
				// component, relative to the segment.
				// double aP_dot_p = aPx*px + aPy*py;

                double aP_dot_p = aPx*vPx + aPy*vPy;

                double prllX = aPx * aP_dot_p;
                double prllY = aPy * aP_dot_p;
                double magPrll = Math.sqrt(prllX*prllX + prllY*prllY);

				double perpX = vPx - prllX;
				double perpY = vPy - prllY;

				// Check if we've met the maxDist constraint.
				// if(perpX*perpX + perpY*perpY <= maxDist*maxDist)
				//	return  new Point2D.Double(px-perpX, py-perpY);

                if (magPrll <= maxDist){
                    aPx *= magPrll;
                    aPy *= magPrll;
                    return new Point2D.Double(a1x+aPx,a1y+aPy);
                }

				// Return failure
				return  null;
			}
		}
	}
	
	// TODO: MultiProjection should have a ProjObj field, not a static reference!
	public ProjObj getProjection() {
		return Main.PO;
	}
}
