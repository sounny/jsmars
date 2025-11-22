package edu.asu.jmars.layer;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

import javax.swing.*;

import edu.asu.jmars.LocationListener;
import edu.asu.jmars.LocationManager;
import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.ZoomListener;
import edu.asu.jmars.ZoomManager;
import edu.asu.jmars.graphics.GraphicsWrapped;
import edu.asu.jmars.ruler.RulerManager;
import edu.asu.jmars.swing.OverlapLayout;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.MovableList;
import edu.asu.jmars.util.Util;

/**
 ** The Swing component that handles visually displaying a stack of
 ** {@link Layer.LView} objects, managing their physical size and
 ** logical coordinates.
 **
 ** <p>An LViewManager may have a child LViewManager, which is
 ** generally a context view in a panner. The addition/removal of LView
 ** objects is proxied by calling {@link Layer.LView#dup} on the
 ** originally-added LView to create a new LView to add to the child
 ** LViewManager. During this duplication process, the original LView
 ** also has its child set to point to its duplicate.
 **
 ** LViewManager objects are given a LocationManager and a ZoomManager
 ** in their constructor.  To cause multiple views to share a viewing
 ** center but allow different zooms (for example for a main and panner
 ** view) use the same {@link LocationManager} instance and different
 ** {@link ZoomManager} instances.
 **
 ** <p>The primary entry point for manipulating what LView objects are
 ** displayed in what order is the {@link #viewList} member. Changes
 ** to this {@link List} are reflected in the visual display of
 ** LViews.
 **
 ** <p>In general, views are only useful when added to the {@link
 ** #viewList} of an LViewManager. The {@link LManager} is the visual
 ** user-interactable component responsible for such actions,
 ** normally.
 **
 ** <p>This object will eventually handle all LView mouse/keyboard
 ** interaction, and proxy actions to the current "active" layer. No
 ** such processing is performed right now, except that a trivial
 ** "glass" layer is added to child LViewManagers as a kludge to
 ** prevent interactivity with the layers and implement
 ** click-to-center panning and zooming.
 **
 ** @see Layer.LView
 ** @see Layer.LView#viewman
 **/
public class LViewManager extends JLayeredPane {
	private static DebugLog log = DebugLog.instance();

	/**
	 ** The list of LViews present in this LViewManager. This may one
	 ** day become non-public, it's currently public to facilitate
	 ** bootstrapping all the plumbing between the objects. You should
	 ** add only LView objects (don't add other types of objects), and
	 ** you should NOT add any null pointers to it.
	 **
	 ** <p>Changes to this list are also proxied to the child
	 ** LViewManager's viewList (if one exists). This means, for
	 ** example, that panners typically reflect the contents of the
	 ** main windows they're attached to.
	 **
	 ** <p>Note that {@link MovableList} is an extension to java's
	 ** {@link List} class that also provides a facility for moving
	 ** elements around.
	 **/
	public MovableList<Layer.LView> viewList = new InternalList();

	private LocationManager locMgr;
	private ZoomManager zoomMgr;
	private double unitWidth = Main.PO.getUnitWidth();
	private double unitHeight = Main.PO.getUnitHeight();
	/** The LViewManager that's listening to our position, and follows us */
	LViewManager childVMan = null;
	private int myId = 0;
	private String myName = "UNKNOWN";
	/** initial input panel */
	protected BaseGlass glassPanel = null;
	LManager lmanager;
    private static final double radsConv = Math.PI / 180.0;

	/**
	 ** Returns the panner view manager associated with this view
	 ** manager, or null if this view manager IS a panner.
	 **/
	public LViewManager getChild()
	 {
		return  childVMan;
	 }
	
	public LocationManager getLocationManager() {
		return locMgr;
	}
	
	public ZoomManager getZoomManager() {
		return zoomMgr;
	}
	
	private static int id = 0;
	private synchronized static int nextId()
	 {
		return  ++id;
	 }

	public String getName()
	 {
		return  myName;
	 }
	
	List<PZMenuItem> navItems = new ArrayList<PZMenuItem>();
	{
		navItems.add(new PZMenuItem("Pan right", "RIGHT",     0.5,    0,  0));
		navItems.add(new PZMenuItem("Pan left",  "LEFT",     -0.5,    0,  0));
		navItems.add(new PZMenuItem("Pan up",    "UP",          0,  0.5,  0));
		navItems.add(new PZMenuItem("Pan down",  "DOWN",        0, -0.5,  0));
		navItems.add(new PZMenuItem("Zoom out",  "PAGE_UP",     0,    0, -1));
		navItems.add(new PZMenuItem("Zoom in",   "PAGE_DOWN",   0,    0,  1));
	}
	
	/**
	 ** Returns the "navigation" menu: basically an excuse for a bunch
	 ** of keyboard shortcuts to pan/zoom around.
	 **/
	public JMenu getNavMenu()
	 {
		JMenu menu = new JMenu("Navigation");
		for (PZMenuItem item: navItems) {
			menu.add(item);
		}
		return  menu;
	 }

	/**
	 ** A menu item that performs a pan/zoom function.
	 **/
	private class PZMenuItem extends JMenuItem {
		private ActionListener listener;
		public PZMenuItem(String lbl, String key, final double xFactor, final double yFactor, final int ppdFactor) {
			listener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (ToolManager.getToolMode() != ToolManager.INVESTIGATE){
						Point2D anchorPoint = locMgr.getLoc();
						Rectangle2D win = getProj().getWorldWindow();
						Point2D newPt = new Point2D.Double(
								anchorPoint.getX() + win.getWidth()  * xFactor,
								anchorPoint.getY() + win.getHeight() * yFactor);
						int magnify = zoomMgr.getZoomPPD();
						int newZoom = ppdFactor < 0 ? magnify >> -ppdFactor : magnify << ppdFactor;
						if(!zoomMgr.getZoomFactors().contains(newZoom)) {
							newZoom = magnify;
						}

						locMgr.setLocation(newPt, true);
						zoomMgr.setZoomPPD(newZoom, true);
					}
				}	
			};
			setAction(new AbstractAction(lbl) {
				public void actionPerformed(ActionEvent e) {
					listener.actionPerformed(e);
				}
			});
			setAccelerator(KeyStroke.getKeyStroke(key));
		}
		public ActionListener getListener() {
			return listener;
		}
	}
	
	public BaseGlass getGlassPanel() {
	   return  glassPanel;
	}

	   
    private final LocationListener locListener = new LocationListener() {
        public void locationChanged(Point2D loc) {
            viewChanged();
        }
    };
    
    private final ZoomListener zoomListener = new ZoomListener() {
        public void zoomChanged(int newPPD) {
            viewChanged();
        }
    };

 	/**
	 * Creates a new LViewManager.
	 * @param locMgr Provides the location of the view center of this LViewManager.
	 * @param zoomMgr Provides the zoom level of this LViewManager.
	 * @param parentVMan Provides a parent view manager. If null, this view manager
	 * will be created as a main view manager, otherwise as a panner view manager.
	 */
	public LViewManager(LocationManager locMgr, ZoomManager zoomMgr, LViewManager parentVMan)
	{
		myId = nextId();
		myName = "Main" + myId;
		
		this.locMgr = locMgr;
		this.zoomMgr = zoomMgr;
		locMgr.addLocationListener(locListener);
		zoomMgr.addListener(zoomListener);
		
		setLayout(new OverlapLayout());
		
		for (PZMenuItem item: navItems) {
			registerKeyboardAction(item.getListener(), item.getAccelerator(), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		}
		
		if (parentVMan == null) {
			glassPanel = new MainGlass(this);
			add(glassPanel);
			setLayer(glassPanel, JLayeredPane.DRAG_LAYER.intValue(), 0);
		} else {
			if(parentVMan.childVMan == null)
				parentVMan.childVMan = this;
			else
				log.aprintln("PROGRAMMER: YOU HAVE AN ORPHANED PANNER OBJECT!");

			setLayout(new OverlapLayout());
			glassPanel = new PannerGlass(this, parentVMan);
			add(glassPanel, new Integer(9999));
		}
	}

	private final MultiProjection projection = new MyProjection();
	
	public MultiProjection getProj()
	 {
		return  projection;
	 }

	public Layer.LView getActiveLView()
	 {
		return  LManager.getLManager().getActiveLView();
	 }

	public void repaintChildVMan()
	 {
		// THIS SEEMS LIKE A DEBUG THAT WAS NEVER REMOVED, IS IT NECESSARY!?!
		childVMan.repaint();
	 }

	/**
	 ** Triggers a {@link Layer.LView#viewChanged} in every {@link
	 ** Layer.LView LView} contained by this LViewManager.
	 **/
	public void viewChanged() {
		List<Layer.LView> views = new ArrayList<Layer.LView>(viewList);
		Collections.reverse(views);
		for (Layer.LView view: views) {
			view.viewChanged();
		}

		// Notify any active rulers that the view has changed - only need to do
		// for one LManager
		if (childVMan == null)
			RulerManager.Instance.notifyRulerOfViewChange();
	}
	
	public final Graphics2D wrapScreenGraphics(Graphics2D g2)
	 {
		int magnify = zoomMgr.getZoomPPD();
		return  new GraphicsWrapped(
			g2,
			360 * magnify,
			magnify,
			getProj().getScreenWindow(),
			"wrapScreenGraphics"
		);
	 }
	
	public final Graphics2D wrapWorldGraphics(Graphics2D g2)
	 {
		return  new GraphicsWrapped(
			g2,
			360,
			zoomMgr.getZoomPPD(),
			getProj().getWorldWindow(),
			"wrapWorldGraphics"
		);
	 }

	/**
	 ** Suspends the current thread until every view's layer is no
	 ** longer idle. Note that the thread will therefore lose all of
	 ** its current monitors in that case, so care must be taken to
	 ** leave multi-thread-safe data in a consistent state before
	 ** calling this method.
	 **/
	public void waitForIdle()
	 {
		log.println("Waiting for idle...");

		// Compile a list of Views to wait for
		Layer.LView[] views =
			(Layer.LView[]) viewList.toArray(new Layer.LView[0]);

	 WAITING:
		while(true)
		 {
			for(int i=0; i<views.length; i++)
				views[i].waitForIdle();

			log.println("Woke up for a little while");

			for(int i=0; i<views.length; i++)
				if(views[i].isBusy())
					continue WAITING;

			log.println("WOKE UP FOR GOOD!");
			return;
		 }
	 }

	public BufferedImage copyMainWindow() {
		Dimension pixSize = projection.getScreenSize();
		BufferedImage image = Util.newBufferedImageOpaque((int) pixSize
				.getWidth(), (int) pixSize.getHeight());
		Graphics2D g2 = image.createGraphics();

		int count = viewList.size();
		int i = 0;
		for (Layer.LView view : viewList) {
			if (view.isVisible()) {
				log.println("Building " + ++i + "/" + count + " views: " + view.getName());
				if (view.isVisible())
					view.realPaintComponent(g2);
			}
		}

		g2.dispose();
		return (image);
	}
	
	public BufferedImage getSnapshot(boolean opaque) {
		BufferedImage image;
		if (opaque) {
			image = Util.newBufferedImageOpaque(getWidth(), getHeight());
		} else {
			image = Util.newBufferedImage(getWidth(), getHeight());
		}
		Graphics2D g2 = image.createGraphics();
		g2.setColor(getBackground());
		g2.fill(new Rectangle2D.Float(0,0,getWidth(),getHeight()));
		int count = viewList.size();
		int i = 0;
		for (Layer.LView view : viewList) {
			log.aprintln("Merging " + ++i + "/" + count + " views: " + view.getName());
			if (view.isVisible())
				view.realPaintComponent(g2);
			view.flushImages();
		}
		g2.dispose();
		return image;
	}
	
	public Point2D[] getWorldBoundingPoints() {
		Rectangle screen = Main.testDriver.mainWindow.getProj().getScreenWindow();

		// swap Y coords since screen coords have origin in upper left corner
		
		Point2D min = Main.testDriver.mainWindow.getProj().createScreen().toWorld(screen.getMinX(), screen.getMaxY());
		Point2D max = Main.testDriver.mainWindow.getProj().createScreen().toWorld(screen.getMaxX(), screen.getMinY());

		// Ignore any portion of the view that is beyond the bottom of the world projection.
		if (max.getY()>=90) {
			max.setLocation(max.getX(), 89.9);  // We use 89.9 instead of 90 to avoid ambiguous conversion results
		}

		// Ignore any portion of the view that is beyond the bottom of the world projection.
		if (min.getY()<=-90) {
			min.setLocation(min.getX(), -89.9);  // We use -89.9 instead of -90 to avoid ambiguous conversion results
		}
		
		Point2D[] ret = new Point2D[2];
		ret[0] = min;
		ret[1] = max;
		return ret;
	}
	
	public Point2D getCenterPoint() {
		Rectangle screen = Main.testDriver.mainWindow.getProj().getScreenWindow();
		
		Point2D spatial = Main.testDriver.mainWindow.getProj().screen.toSpatial(screen.getCenterX(), screen.getCenterY());
		
		return spatial;
		
	}
	/**
	 * Methods to create jpegs
	 */
	public void dumpJpg(String filename) {
		log.aprintln("Outputting image...");
		Util.saveAsJpeg(getSnapshot(true), filename);
		log.aprintln("DONE! Image in " + filename);
	}

	/**
	 * Method to create PNG image files
	 */
	public void dumpPNG(String filename) {
		log.aprintln("Outputting image...");
		Util.saveAsPng(getSnapshot(true), filename);
		log.aprintln("DONE! Image in " + filename);
	}
	
	/**
	 * Method to create TIF image files
	 */
	public void dumpTIF(String filename) {
		log.aprintln("Outputting image...");
		Util.saveAsTif(getSnapshot(false), filename);
		log.aprintln("DONE! Image in " + filename);
	}


	/**
	 ** Internal class that implements MovableList to proxy things to
	 ** our JLayeredPane display.
	 **/
     private class InternalList extends AbstractList<Layer.LView> implements MovableList<Layer.LView>
	 {
		// Our InternalList implementation is really (among other
		// things) just a proxy to some "real" list, which we allocate
		// here.
		private java.util.List<Layer.LView> realList = new ArrayList<Layer.LView>();

		// Required for useful AbstractList implementation
		public Layer.LView get(int index)
		 {
			return  realList.get(index);
		 }

		// Required for useful AbstractList implementation
		public int size()
		 {
			return  realList.size();
		 }

		// Required for useful AbstractList implementation
		public Layer.LView set(int index, Layer.LView element)
		 {
			// We really got an LView, so let's cast it so we can use it
			Layer.LView lv = (Layer.LView) element;

			// Do the same thing to our childVMan, if we have one
			if(childVMan != null)
			 {
				Layer.LView lv2 = lv.dup();
				lv.setChild(lv2);
				childVMan.viewList.set(index, lv2);
			 }

			// Remove the old LView and replace it with the new one
			LViewManager.this.remove((Layer.LView) viewList.get(index));
			LViewManager.this.add(lv, new Integer(index));

			// Finally, manipulate the "real" list properly
			++modCount;
			return  realList.set(index, element);
		 }

		// Required for useful AbstractList implementation
		public void add(int index, Layer.LView element)
		 {
			// We really got an LView, so let's cast it so we can use it
			Layer.LView lv = (Layer.LView) element;

			// Do the same thing to our childVMan, if we have one
			if(childVMan != null)
			 {
				Layer.LView lv2 = lv.dup();
				lv.setChild(lv2);
				childVMan.viewList.add(index, lv2);
			 }

			// Shift the existing LViews up in the JLayeredPane
			// z-depth to make room, then add the new LView.
			for(int i=index; i<viewList.size(); i++)
				LViewManager.this.setLayer((Layer.LView)viewList.get(i), i+1);
			LViewManager.this.add(lv, new Integer(index));

 			// Finally, manipulate the "real" list properly
			++modCount;
			realList.add(index, lv);
			LViewManager.this.validate();

			if(childVMan != null)
				childVMan.validate();

			// These were moved from LView.ancestorAdded()
			// so that when a layer is moved in the layer list,
			// the LView is not deleted and re-created.
			//(JW 2/04)
			lv.viewInit();
     
			// Notify any registered listeners on the view list that a change has
			// occured.  (JW 11/04)
			listener.update();
		 }

		// Required for useful AbstractList implementation
		public Layer.LView remove(int index)
		 {
			// Do the same thing to our childVMan, if we have one
			if(childVMan != null)
				childVMan.viewList.remove(index);

			// Get the item at that index from the "real" list
			Layer.LView lv = (Layer.LView) realList.get(index);

			// Remove the LView from the JLayeredPane
			LViewManager.this.remove(lv);

			// This was moved from LView.ancestorRemoved()
			// so that when a layer is moved in the layer list,
			// the LView is not deleted and re-created.
			// (JW 2/04)
			lv.viewCleanup();


			// Manipulate the "real" list properly
			++modCount;
			Layer.LView removedObj =  realList.remove(index);
			
			for(int i=index; i<realList.size(); i++){
				LViewManager.this.setLayer((Layer.LView)realList.get(i), i);
			}
			
			LViewManager.this.validate();
			if (childVMan != null)
				childVMan.validate();

			// Notify any registers listeners on the view list that a change has
			// occured.  (JW 11/04)
			listener.update();

			return removedObj;
		 }

		// Implements MovableList interface
		public void move(int srcIndex, int dstIndex)
		 {
			if(srcIndex == dstIndex)
				return;

			// Do the same thing to our childVMan, if we have one
			if(childVMan != null)
				childVMan.viewList.move(srcIndex, dstIndex);

			// Manipulate the "real" list properly
			++modCount;
			log.println("dstIndex = " + dstIndex);
			realList.add(dstIndex, realList.remove(srcIndex));

			// Redo the z-depths in the JLayeredPane
			int min = Math.min(srcIndex, dstIndex);
			int max = Math.max(srcIndex, dstIndex);
			for(int i=min; i<=max; i++)
				LViewManager.this.setLayer((Layer.LView) realList.get(i), i);
			
			// Notify any registers listeners on the view list that a change has
			// occurred.
			listener.update();


   		 }
	 }

	private final class MyProjection extends MultiProjection
	 {
		MyProjection()
		 {
			log.println("Projection initialized");
		 }

		/**
		 ** Given an x-coordinate of a "messed-up screen coordinate,"
		 ** returns a localized screen coordinate. THIS SHOULD NEVER BE
		 ** USED IN TIME PROJECTION!!!
		 **/
		private double screenToScreenLocal(double x)
		 {
			final double ppd = zoomMgr.getZoomPPD();
			return  x - Math.floor(x / 360 / ppd) * 360 * ppd;
		 }

		// Constructor-helper functions
		protected SingleProjection createScreen()
		 {
			return  new ScreenProjection();
		 }
		protected SingleProjection createWorld()
		 {
			return  new WorldProjection();
		 }
		protected SingleProjection createSpatial()
		 {
			return  new SpatialProjection();
		 }

		public Dimension getScreenSize()
		 {
			Insets in = getInsets();
			return  new Dimension(getWidth()  - in.left - in.right,
								  getHeight() - in.top  - in.bottom);
		 }

		public float getPixelHeight()
		 {
			return  (float) (unitHeight / zoomMgr.getZoomPPD());
		 }

		public float getPixelWidth()
		 {
			return  (float) (unitWidth / zoomMgr.getZoomPPD());
		 }

		public Rectangle getScreenWindow()
		 {
			Insets in = getInsets();
			return  new Rectangle(
				0, 0,
				(int) Math.round(getWidth()  - in.left - in.right),
				(int) Math.round(getHeight() - in.top  - in.bottom));
		 }

		public Rectangle2D getWorldWindow()
		 {
			Dimension2D pixel = getPixelSize();
			Dimension window = getScreenSize();

			double w = window.width  * pixel.getWidth();
			double h = window.height * pixel.getHeight();
			Point2D anchorPoint = locMgr.getLoc();
			double x = anchorPoint.getX() - w / 2;
			double y = anchorPoint.getY() - h / 2;

			// We must ensure that the world coordinate base of the
			// rectangle in fact occurs on an exact pixel coordinate,
			// in order to keep things kosher with the java graphics
			// drawing system. Specifically, ignoring the restriction
			// leads to the infamous "gray lines" problem.
			x = Util.roundToMultiple(x, pixel.getWidth());
			y = Util.roundToMultiple(y, pixel.getHeight());

			return  new Rectangle2D.Double(x, y, w, h);
		 }

		public Shape getWorldWindowMod()
		 {
			Rectangle2D win = getWorldWindow();

			// If we span more than 360, just return a 0->360 ranged rectangle
			if(win.getWidth() >= 360)
			 {
				win.setRect(0, win.getY(), 360, win.getHeight());
				return  win;
			 }

			// If we don't cross zero at all, return the existing rectangle
			double minX = win.getX() - Math.floor(win.getMaxX()/360)*360;
			if(0 <= minX)
			 {
				win.setRect(minX,
							win.getY(),
							win.getWidth(),
							win.getHeight());
				return  win;
			 }

			// We cross zero but span less than 360, gotta join two rectangles
			win.setRect(0,
						win.getY(),
						win.getWidth() + minX,
						win.getHeight());
			Area both = new Area(win);
			win.setRect(360 + minX,
						win.getY(),
						-minX,
						win.getHeight());
			both.add(new Area(win));
			return  both;
		 }

		public AffineTransform getScreenToWorld()
		 {
			Rectangle2D rect = getWorldWindow();
			Dimension window = getScreenSize();
			AffineTransform image2world = new AffineTransform();

			image2world.translate(rect.getX(),
								  rect.getY());
			image2world.scale(rect.getWidth()  / window.getWidth(),
							  rect.getHeight() / window.getHeight());

			// Correct for upside-down-ness
			image2world.translate(0, window.getHeight());
			image2world.scale(1, -1);

			return  image2world;
		 }

		public AffineTransform getWorldToScreen()
		 {
			Rectangle2D rect = getWorldWindow();
			Dimension window = getScreenSize();
			AffineTransform world2image = new AffineTransform();

			// Correct for upside-down-ness
			world2image.scale(1, -1);
			world2image.translate(0, -window.getHeight());

			world2image.scale(window.getWidth()  / rect.getWidth(),
							  window.getHeight() / rect.getHeight());
			world2image.translate(-rect.getX(),
								  -rect.getY());
			return  world2image;
		 }

		public int getPPD()
		 {
			return  zoomMgr.getZoomPPD();
		 }

		public int getPPDLog2()
		 {
			return  zoomMgr.getZoomFactors().indexOf(zoomMgr.getZoomPPD());
		 }

		private class ScreenProjection extends SingleProjection
		 {
			public Point2D toScreen(double x, double y)
			 {
				return  new Point2D.Double(x, y);
			 }
			public Point2D toScreenLocal(double x, double y)
			 {
				return  new Point2D.Double(screenToScreenLocal(x), y);
			 }
			public Point2D toWorld(double x, double y)
			 {
				return  getScreenToWorld().transform(new Point2D.Double(x,y),
													 null);
			 }
			public Point2D toSpatial(double x, double y)
			 {
				return  Main.PO.convWorldToSpatial(toWorld(x, y));
			 }
			public Point2D fromHVector(double x, double y, double z)
			 {
				HVector pt = new HVector(x, y, z);
				return
					getWorldToScreen().transform(
						Main.PO.convSpatialToWorld(
							new Point2D.Double(
								Math.toDegrees(ProjObj.lon_of(pt)),
								Math.toDegrees(ProjObj.lat_of(pt)))
							 ),
						null
						);
			 }
			public double distance(double ax, double ay, double bx, double by)
			 {
				int mod = 360 * zoomMgr.getZoomPPD();
				int xdiff = (int)Math.abs(ax - bx) % mod;
				if(xdiff > mod / 2)
					xdiff = mod - xdiff;
				return  Math.sqrt((ax-bx)*(ax-bx) +
								  (ay-by)*(ay-by) );
			 }
			public double distance(double a1x, double a1y,
								   double a2x, double a2y,
								   double px, double py)
			 {
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

				double perpX = vPx - prllX;
				double perpY = vPy - prllY;
				// double perpX = aPx * aP_dot_p;
				// double perpY = aPy * aP_dot_p;

				return  Math.sqrt(perpX*perpX + perpY*perpY);
			 }
			public boolean hitTest(double a1x, double a1y,
								   double a2x, double a2y,
								   double b1x, double b1y,
								   double b2x, double b2y)
			 {
                 log.println("[("+a1x+","+a1y+"),("+
                             a2x+","+a2y+"),("+b1x+","+b1y+
                             "),("+b2x+","+b2y+")]");
				return  spatial.hitTest(a1x, a1y,
										a2x, a2y,
										b1x, b1y,
										b2x, b2y);
			 }
			public Point2D nearPt(double a1x, double a1y,
								  double a2x, double a2y,
								  double px, double py,
								  double maxDist)
			 {
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

		private class WorldProjection extends SingleProjection
		 {
			public Point2D toScreen(double x, double y)
			 {
				return  getWorldToScreen().transform(new Point2D.Double(x,y),
													 null);
			 }
			public Point2D toScreenLocal(double x, double y)
			 {
				Point2D s = toScreen(x, y);
				return  new Point2D.Double(screenToScreenLocal(s.getX()),
										   s.getY());
			 }
			public Point2D toWorld(double x, double y)
			 {
				return  new Point2D.Double(x,y);
			 }
			public Point2D toSpatial(double x, double y)
			 {
				return  Main.PO.convWorldToSpatial(new Point2D.Double(x,y));
			 }
			public Point2D fromHVector(double x, double y, double z)
			 {
				HVector pt = new HVector(x, y, z);
				return
					Main.PO.convSpatialToWorld(
						new Point2D.Double(
							Math.toDegrees(ProjObj.lon_of(pt)),
							Math.toDegrees(ProjObj.lat_of(pt)))
						 );
			 }
			public double distance(double ax, double ay, double bx, double by)
			 {
				double xdiff = Math.abs(ax - bx) % 360.0;
				if(xdiff > 180)
					xdiff = 360 - xdiff;
				return  Math.sqrt((ax-bx)*(ax-bx) +
								  (ay-by)*(ay-by) );
			 }
			public double distance(double a1x, double a1y,
								   double a2x, double a2y,
								   double px, double py)
			 {
				// Perpendicular to the segment, normalized
				double aPx = a1y - a2y;
				double aPy = a2x - a1x;
				double aPmag = Math.sqrt(aPx*aPx + aPy*aPy);
				aPx /= aPmag;
				aPy /= aPmag;

				// Determine what portion of p is the perpendicular
				// component, relative to the segment.
				double aP_dot_p = aPx*px + aPy*py;
				double perpX = aPx * aP_dot_p;
				double perpY = aPy * aP_dot_p;

				return  Math.sqrt(perpX*perpX + perpY*perpY);
			 }
			public boolean hitTest(double a1x, double a1y,
								   double a2x, double a2y,
								   double b1x, double b1y,
								   double b2x, double b2y)
			 {
                 log.println("[("+a1x+","+a1y+"),("+
                             a2x+","+a2y+"),("+b1x+","+b1y+
                             "),("+b2x+","+b2y+")]");
				return  spatial.hitTest(a1x, a1y,
										a2x, a2y,
										b1x, b1y,
										b2x, b2y);
			 }
			public Point2D nearPt(double a1x, double a1y,
								  double a2x, double a2y,
								  double px, double py,
								  double maxDist)
			 {
                 log.println("[a1=("+a1x+","+a1y+"),a2=("+
                             a2x+","+a2y+"),p=("+px+
                             ","+py+") maxDist="+maxDist);
				// Perpendicular to the segment, normalized
				double aPx = a1y - a2y;
				double aPy = a2x - a1x;
				double aPmag = Math.sqrt(aPx*aPx + aPy*aPy);
				aPx /= aPmag;
				aPy /= aPmag;

				// Determine what portion of p is the perpendicular
				// component, relative to the segment.
				double aP_dot_p = aPx*px + aPy*py;
				double perpX = aPx * aP_dot_p;
				double perpY = aPy * aP_dot_p;

				// Check if we've failed the maxDist constraint.
				if(perpX*perpX + perpY*perpY <= maxDist*maxDist)
					return  null;

				return  new Point2D.Double(px-perpX, py-perpY);
			 }
		 }

		private class SpatialProjection extends SingleProjection
		 {
			public Point2D toWorld(double x, double y)
			 {
				return  Main.PO.convSpatialToWorld(new Point2D.Double(x,y));
			 }
			public Point2D toScreen(double x, double y)
			 {
				return  getWorldToScreen().transform(toWorld(x,y),null);
			 }
			public Point2D toScreenLocal(double x, double y)
			 {
				Point2D s = toScreen(x, y);
				return  new Point2D.Double(screenToScreenLocal(s.getX()),
										   s.getY());
			 }
			public Point2D toSpatial(double x, double y)
			 {
				return  new Point2D.Double(x, y);
			 }
			public Point2D fromHVector(double x, double y, double z)
			 {
				HVector pt = new HVector(x, y, z);
				return  new Point2D.Double(
					Math.toDegrees(ProjObj.lon_of(pt)),
					Math.toDegrees(ProjObj.lat_of(pt)));
			 }
			public double distance(double ax, double ay, double bx, double by)
			 {
				HVector a = toHVector(ax, ay);
				HVector b = toHVector(bx, by);
				return  Math.toDegrees(a.separation(b));
			 }
			public double distance(double a1x, double a1y,
								   double a2x, double a2y,
								   double px, double py)
			 {
				HVector a1 = toHVector(a1x, a1y);
				HVector a2 = toHVector(a2x, a2y);
				HVector p = toHVector(px, py);

				// Check if the nearest point is an endpoint.
				HVector perp = a1.add(a2);
				if(perp.dot(p) < perp.dot(a1))
					return  Math.min(a1.separation(p),
									 a2.separation(p));

				// Determine what portion of p is the perpendicular
				// difference, relative to the plane of the segment.
				HVector n = a1.cross(a2).unit();
				HVector diff = n.mul(p.dot(n));

				return  Math.asin(diff.norm());
			 }
			public boolean hitTest(double a1x, double a1y,
								   double a2x, double a2y,
								   double b1x, double b1y,
								   double b2x, double b2y)
			 {
                 log.println("[("+a1x+","+a1y+"),("+
                             a2x+","+a2y+"),("+b1x+","+b1y+
                             "),("+b2x+","+b2y+")]");
				// Segment A endpoints and normal
				HVector a1 = toHVector(a1x, a1y);
				HVector a2 = toHVector(a2x, a2y);
				HVector aN = a1.cross(a2);

				// Segment B endpoints and normal
				HVector b1 = toHVector(b1x, b1y);
				HVector b2 = toHVector(b2x, b2y);
				HVector bN = b1.cross(b2);

				// The "perpendiculars"... vectors that are
				// perpendicular bisectors of their respective
				// segments. Because of how they're used, we don't
				// need to normalize them (though it wouldn't hurt).
				HVector aP = a1.add(a2);
				HVector bP = b1.add(b2);

				// Candidate intersection point... if the segments
				// intersect, they MUST intersect at either +pt or
				// -pt.
				HVector pt = aN.cross(bN);

				// The candidate intersection point lies within the
				// plane of both segments. Given that fact, it's easy
				// to check whether or not it actually lies within
				// each segment, based on a dot product with the
				// segments' perpendicular bisectors.
				return  aP.dot(pt) <= aP.dot(a1)
					&&  bP.dot(pt) <= bP.dot(b1);
			 }
			public Point2D nearPt(double a1x, double a1y,
								  double a2x, double a2y,
								  double px, double py,
								  double maxDist)
			 {
                 log.println("[a1=("+a1x+","+a1y+"),a2=("+
                             a2x+","+a2y+"),p=("+px+
                             ","+py+") maxDist="+maxDist);
				HVector a1 = toHVector(a1x, a1y);
				HVector a2 = toHVector(a2x, a2y);
				HVector p = toHVector(px, py);
				HVector n = a1.cross(a2).unit();

				// Determine what portion of p is the perpendicular
				// difference, relative to the plane of the segment.
				HVector diff = n.mul(p.dot(n));

				// Check if we've failed the maxDist constraint.
				if(Math.asin(diff.norm()) > maxDist)
					return  null;

				// Check if the point doesn't lie on the segment.
				HVector perp = a1.add(a2);
				if(perp.dot(p) < perp.dot(a1))
					return  null;

				// Our "nearest point"... the original point minus the
				// perpendicular difference.
				p.subEq(diff);
				return  fromHVector(p);
			 }

			/*
			  SAVED: A rather efficient hitTest, but one that can give
			  false positives for pairs of segments that together span
			  more than a hemisphere. It's a shame that I can't see a
			  way to use it for the general case. It doesn't require
			  perpendiculars, and saves a bunch of computation.

			  return  sign(aN.dot(b1)) != sign(aN.dot(b2))
			      &&  sign(bN.dot(a1)) != sign(bN.dot(a2));
			*/
		 }
	}


     /**
      * A mechanism for letting views listen for changes in the view list.
      * Any view that wishes to do so should implement an Observer class and
      * addObserver() to the public viewman.observedObject
      */
     public class ObservedObject extends Observable {
	 public void update(){
	     setChanged();
	     notifyObservers();
	 }
     }
     public ObservedObject listener = new ObservedObject();



 }
