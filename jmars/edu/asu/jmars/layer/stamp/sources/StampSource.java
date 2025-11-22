package edu.asu.jmars.layer.stamp.sources;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.SwingUtilities;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapAttrReceiver;
import edu.asu.jmars.layer.map2.MapRequest;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.MapSourceDefault;
import edu.asu.jmars.layer.map2.MapSourceListener;
import edu.asu.jmars.layer.map2.NonRetryableException;
import edu.asu.jmars.layer.map2.RetryableException;
import edu.asu.jmars.layer.stamp.FilledStamp;
import edu.asu.jmars.layer.stamp.FilledStampImageType;
import edu.asu.jmars.layer.stamp.RenderProgress;
import edu.asu.jmars.layer.stamp.StampComparator;
import edu.asu.jmars.layer.stamp.StampGroupComparator;
import edu.asu.jmars.layer.stamp.StampImage;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayerSettings;
import edu.asu.jmars.layer.stamp.StampServer;
import edu.asu.jmars.layer.stamp.StampShape;
import edu.asu.jmars.layer.stamp.StampLView.DrawFilledRequest;
import edu.asu.jmars.layer.stamp.focus.OutlineFocusPanel;
import edu.asu.jmars.layer.stamp.focus.StampFocusPanel;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.emory.mathcs.backport.java.util.concurrent.Semaphore;

/**
 * A source of map data produced by the stamp layer. The
 * {@link #fetchTile(MapRequest)} method is called by the map processing system,
 * and it creates an image for the requested area, calls the
 * {@link StampLView#doRender(edu.asu.jmars.layer.stamp.StampImage.Request, java.util.List, RenderProgress, Graphics2D)}
 * method to draw rendered stamps into the tile, and then returns it to the
 * caller.
 * 
 * Since use of this MapSource implementation causes a map layer to become
 * dependent on a stamp layer, serialization of the map layer must include
 * enough information to find the stamp layer later after reloading. This is
 * done by including the StampLayerSettings object in the serialized state for
 * this class. When fetchTile() needs to find the StampLView, the existing
 * layers will be searched, and {@link StampLView#doRender()} called on the
 * match.
 * 
 * StampSource instances are created through a static constructor, and
 * deserialized objects are passed through readResolve(), so that there can only
 * be one StampSource instance for each StampLayerSettings instance. This allows
 * adding listeners to StampSource, and ensures that all StampSource instances
 * point at the singleton StampServer and that that server contains all
 * StampSource instances.
 * 
 * Tiles can take awhile to render in the stamp layer, but the clipping system
 * used is pretty efficient, so once we have an answer back from the stamp
 * layer, we keep it and return cached tiles. When the stamp layer detects a
 * settings change, it will call {@link #clearCache()} to invalidate all of the
 * cache for that layer.
 */
public class StampSource implements MapSource, Serializable {
    private static final long serialVersionUID = -4275867253020220221L;
	protected static final Map<CacheKey,BufferedImage> cache = new LinkedHashMap<CacheKey,BufferedImage>();
	
	public final StampLayerSettings settings;
	protected double maxPPD = MapSource.MAXPPD_MAX;
	protected double[] ignore = null;
	/** will lazily create these if this instance was deserialized */
	protected transient List<MapSourceListener> listeners = new ArrayList<MapSourceListener>();
	
	protected String sourceName;
	
	protected String nameRoot = "graphic";
	
//	/**
//	 * Creates a new StampSouce or returns an existing instance, in both cases
//	 * ensuring that the instance has a StampServer defined, and that the server
//	 * contains this source.
//	 */
//	public static synchronized StampSource create(StampLayerSettings settings) {
//		StampSource out = instances.get(settings.id);
//		if (out == null) {
//			// create new instance with these settings
//			instances.put(settings.id, out = new StampSource(settings));
//		}
//		StampServer.getInstance().add(out);
//
//		// really just setting up the server
////		merge(out,out);
////		out.server.add(out);
//		return out;
//	}
	
	/** private to force using {@link #create(StampLayerSettings)}. */
	public StampSource(StampLayerSettings settings) {
		this.settings = settings;
	}
	
	/**
	 * Does the same work as {@link #create(StampLayerSettings)}, but uses a
	 * deserialized instance rather than creating a new one.
	 */
	private Object readResolve() {
		StampSource out = StampServer.getInstance().getSource(this.getClass(), settings.id);
		
//		System.out.println("source = " + source);
//		
//		StampSource out = instances.get(settings.id);
		
		if (out == null) {
			System.out.println("Please don't be null....");
			out = this;
			// add this unserialized object to the instance hash
//			instances.put(settings.id, out);
			StampServer.getInstance().add(out);
		}
		
		return out;
	}
		
	/**
	 * Find a view at run time based on the settings, which is the most
	 * immediately usable serializable identifier between map and stamp layers.
	 */
	protected StampLView getView() {
		for (LView loadedView: (List<LView>)Main.testDriver.mainWindow.viewList) {
			if (loadedView instanceof StampLView) {
				StampLView stamp = (StampLView)loadedView;
				if (stamp.stampLayer.getSettings().id == settings.id) {
					return stamp;
				}
			}
		}
		return null;
	}
	
	/** Clears the cache for this StampSource, and initiates a StampSource change event to notify all listeners */
	public synchronized void clearCache() {
		boolean changed;
		if (cachedTiles==null) {
			cachedTiles = new HashMap<String,BufferedImage>();
		}
		synchronized(cachedTiles) {
			changed = cachedTiles.size() > 0;
			cachedTiles.clear();
		}
		if (changed) {
			changed();
		} 
	}
	
	private static final class CacheKey {
		public Rectangle2D extent;
		public ProjObj po;
		public int ppd;
		public int hashCode() {
			return ppd*317 + extent.hashCode()*31 + po.hashCode();
		}
		public boolean equals(Object o) {
			if (o instanceof CacheKey) {
				CacheKey ck = (CacheKey)o;
				return ppd == ck.ppd && po == ck.po && extent.equals(ck.extent);
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Cache is disabled right now, since it is causing rendering issues.
	 */
	private static final boolean useCache = false;
	
	public BufferedImage fetchTile(final MapRequest req) throws RetryableException, NonRetryableException {
		if (useCache) {
			return fetchCached(req);
		} else {
			return fetchDirect(req);
		}
	}
	
	private BufferedImage fetchCached(MapRequest req) throws RetryableException, NonRetryableException {
		CacheKey ck = new CacheKey();
		ck.extent = req.getExtent();
		ck.po = req.getProjection();
		ck.ppd = req.getPPD();
		
		// if item is in the cache, move it to end and return it
		synchronized(cache) {
			// check the cache
			BufferedImage bi = cache.remove(ck);
			
			// if not found, fetch it immediately
			if (bi == null) {
				bi = fetchDirect(req);
			}
			
			if (!req.isCancelled()) {
				// remove from the head on each request
				int toRemove = cache.size() - 200;
				if (toRemove > 0) {
					Iterator<?> it = cache.keySet().iterator();
					while (toRemove > 0) {
						it.next();
						it.remove();
						toRemove --;
					}
				}
				
				// append image to end of cache and return image
				cache.put(ck, bi);
				return bi;
			} else {
				return null;
			}
		}
	}
	
//	protected BufferedImage fetchDirect(MapRequest req) throws RetryableException, NonRetryableException {
//		// otherwise fill out target image with data from the stamp layer
//		Dimension size = req.getImageSize();
//		BufferedImage bi = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
//		Graphics2D g2 = bi.createGraphics();
//		g2.setTransform(Util.world2image(req.getExtent(), size.width, size.height));
//		
//		// check for a view, returning an empty image if there isn't one
//		StampLView view = getView();
//		if (view == null) {
//			return bi;
//		}
//		
//		// create rendering listener that will unlock the calling thread when the last stamp has
//		// been rendered into this tile
//		final boolean[] done = {false};
//		RenderProgress progress = new RenderProgress() {
//			public void update(int current, int max) {
//				if (current == max) {
//					synchronized(done) {
//						done[0] = true;
//						done.notifyAll();
//					}
//				}
//			}
//		};
//		
//		// do the rendering on this thread
//		List<FilledStamp> stamps = view.getFilteredFilledStamps();
//		for (Iterator<FilledStamp> it = stamps.iterator(); it.hasNext(); ) {
//			FilledStampImageType fs = (FilledStampImageType)it.next();
//			Point2D offset=fs.getOffset();
//			
//			List<Area> stampAreas = fs.stamp.getFillAreas(req.getProjection());
//			boolean hitThisStamp=false;
//			
//			for (Area area : stampAreas) {				
//				Rectangle2D imageBounds=area.getBounds2D();
//				imageBounds.setRect(imageBounds.getMinX()+offset.getX(), imageBounds.getMinY()+offset.getY(), imageBounds.getWidth(), imageBounds.getHeight());
//				if (hit(imageBounds, req.getExtent())) {
//					hitThisStamp=true;
//					break;  // Once we've hit this stamp once, we don't need to search further
//				}
//			}
//			
//			if (!hitThisStamp) {
//				it.remove();
//			}
//		}
//		
//		// if there are no stamps to render, don't call the stamp layer, and
//		// don't cache the image since the lookup is more than fast enough that
//		// we don't want to pollute the cache with empty images
//		if (!stamps.isEmpty()) {
//			view.doRender(new MapStampRequest(req), stamps, progress, g2, null);
//			
//			// wait on completion of rendering thread, as caught by the RenderProgress above
//			// there is no method to hand off exceptions from the rendering thread, but
//			// we don't need to handle those intricacies at the moment
//			synchronized(done) {
//				while (!done[0]) {
//					try {
//						done.wait(30000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//						throw new RetryableException("Timed out", e);
//					} catch (Throwable t) {
//						t.printStackTrace();
//						throw new NonRetryableException("Exception while waiting on stamp rendering", t);
//					}
//				}
//			}
//		}
//		
//		return bi;
//	}
	
	transient int cachedStateId = -1;
	transient HashMap<String,BufferedImage> cachedTiles = new HashMap<String,BufferedImage>();
	
	public transient HashMap<String, Semaphore> inProcessTiles = new HashMap<String, Semaphore>();

	protected BufferedImage fetchDirect(MapRequest req) throws RetryableException, NonRetryableException {
		StampLView view = getView();

		ArrayList<Integer> stateIds=view.getLayer().getStateIds();
		int currentStateId = stateIds.get(0);
		if (currentStateId==0 || cachedStateId != currentStateId) {
			clearCache();
			cachedStateId = currentStateId;
		}
		
		// TODO: Is this a reasonable key?
		String tileKey = req.getProjection().getProjString() + " : " + req.getExtent().toString() + " : " + req.getPPD();

		Semaphore tileSemaphore;
		
		if (inProcessTiles==null) {
			inProcessTiles=new HashMap<String, Semaphore>();
		}
		
		synchronized(inProcessTiles) {
			tileSemaphore = inProcessTiles.get(tileKey);
			
			if (tileSemaphore==null) {
				tileSemaphore=new Semaphore(1);
				inProcessTiles.put(tileKey, tileSemaphore);
			}
		}
		
		try {
			try {
				tileSemaphore.acquire();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
	
			BufferedImage bi = cachedTiles.get(tileKey);
			if (bi!=null) {
				tileSemaphore.release();
				return bi;
			} 
					
			// otherwise fill out target image with data from the stamp layer
			Dimension size = req.getImageSize();
			
			bi = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
								
			Graphics2D g2 = bi.createGraphics();
			g2.setTransform(Util.world2image(req.getExtent(), size.width, size.height));
						
			// create rendering listener that will unlock the calling thread when the last stamp has
			// been rendered into this tile
			final boolean[] done = {false};
			RenderProgress progress = new RenderProgress() {
				public void update(int current, int max) {
					if (current == max) {
						synchronized(done) {
							done[0] = true;
							done.notifyAll();
						}
					}
				}
			};
			
			ProjObj reqPO = req.getProjection();

			if (view.stampLayer.spectraData()) {
				int ppd = req.getPPD();
				Rectangle2D extent = req.getExtent();
				
				List<StampShape> stampsWithinExtent = view.findStampsByWorldRect(extent, reqPO);
	
				OutlineFocusPanel ofp = null;
				
				if (view.getChild()==null) {
					ofp = ((StampFocusPanel)view.getParentLView().focusPanel).outlinePanel;
				} else {
					ofp = ((StampFocusPanel)view.focusPanel).outlinePanel;
				}
					
				StampGroupComparator orderSort = ofp.getOrderSort();
				
				Collections.sort(stampsWithinExtent, orderSort);
				
				ArrayList<Integer> dataGrid[][] = new ArrayList[bi.getWidth()][bi.getHeight()];
				
				for (int i=0; i<bi.getWidth(); i++) {
					for (int j=0; j<bi.getHeight(); j++) {
						dataGrid[i][j]=new ArrayList<Integer>();
					}
				}
	
				double startx = extent.getMinX();
				double starty = extent.getMaxY();
	
				for (StampShape stamp : stampsWithinExtent) {
					List<Area> stampAreas = stamp.getFillAreas(reqPO);
	
					for (Area shapeArea : stampAreas) {
						for (int i=0; i<bi.getWidth(); i++) {
							for (int j=0; j<bi.getHeight(); j++) {
								double x = startx + (i*1.0)/ppd;
								double y = starty - (j*1.0)/ppd;
	
								if (shapeArea.contains(x, y)) {
									Color c = stamp.getCalculatedColor();
									if (c!=null) {
										dataGrid[i][j].add(c.getRGB());
									}
								}
							}
						}
					}
				}
				
				for (int i=0; i<bi.getWidth(); i++) {
					for (int j=0; j<bi.getHeight(); j++) {
						int avgVal = 0;
						
						if (dataGrid[i][j].size()>0) {
							avgVal = getTopColor(dataGrid[i][j]);
						} 
						
						bi.setRGB(i, j, avgVal);
					}
				}
				
				currentStateId = stateIds.get(0);
				if (cachedStateId == currentStateId) {
					cachedTiles.put(tileKey, bi);
				} else {
					// ?
				}
				tileSemaphore.release();
				
				return bi;
			} else {
				
				// do the rendering on this thread
				List<FilledStamp> stamps = view.getFilteredFilledStamps();
				for (Iterator<FilledStamp> it = stamps.iterator(); it.hasNext(); ) {
					FilledStampImageType fs = (FilledStampImageType)it.next();
					Point2D offset=fs.getOffset();
					
					List<Area> stampAreas = fs.stamp.getFillAreas(req.getProjection());
					boolean hitThisStamp=false;
					
					for (Area area : stampAreas) {				
						Rectangle2D imageBounds=area.getBounds2D();
						imageBounds.setRect(imageBounds.getMinX()+offset.getX(), imageBounds.getMinY()+offset.getY(), imageBounds.getWidth(), imageBounds.getHeight());
						if (hit(imageBounds, req.getExtent())) {
							hitThisStamp=true;
							break;  // Once we've hit this stamp once, we don't need to search further
						}
					}
					
					if (!hitThisStamp) {
						it.remove();
					}
				}
				
				// if there are no stamps to render, don't call the stamp layer, and
				// don't cache the image since the lookup is more than fast enough that
				// we don't want to pollute the cache with empty images
				if (!stamps.isEmpty()) {
					view.doRender(new MapStampRequest(req), stamps, progress, g2, null);
					
					// wait on completion of rendering thread, as caught by the RenderProgress above
					// there is no method to hand off exceptions from the rendering thread, but
					// we don't need to handle those intricacies at the moment
					synchronized(done) {
						while (!done[0]) {
							try {
								done.wait(30000);
							} catch (InterruptedException e) {
								e.printStackTrace();
								throw new RetryableException("Timed out", e);
							} catch (Throwable t) {
								t.printStackTrace();
								throw new NonRetryableException("Exception while waiting on stamp rendering", t);
							}
						}
					}
				}
				
				return bi;

//				// Non-spectra layers
//				int ppd = req.getPPD();
//				Rectangle2D extent = req.getExtent();
//				
//				///
//				List<FilledStamp> renderedStamps = view.getFilteredFilledStamps();
//				for (Iterator<FilledStamp> it = renderedStamps.iterator(); it.hasNext(); ) {
//					FilledStampImageType fs = (FilledStampImageType)it.next();
//										
//					Point2D offset=fs.getOffset();
//					
//					List<Area> stampAreas = fs.stamp.getFillAreas(reqPO);
//					boolean hitThisStamp=false;
//					
//					for (Area area : stampAreas) {				
//						Rectangle2D imageBounds=area.getBounds2D();
//						imageBounds.setRect(imageBounds.getMinX()+offset.getX(), imageBounds.getMinY()+offset.getY(), imageBounds.getWidth(), imageBounds.getHeight());
//						if (hit(imageBounds, req.getExtent())) {
//							hitThisStamp=true;
//							break;  // Once we've hit this stamp once, we don't need to search further
//						}
//					}
//					
//					if (!hitThisStamp) {
//						it.remove();
//					}
//				}
//				///
//								
//				ArrayList<Color> dataGrid[][] = new ArrayList[bi.getWidth()][bi.getHeight()];
//				
//				for (int i=0; i<bi.getWidth(); i++) {
//					for (int j=0; j<bi.getHeight(); j++) {
//						dataGrid[i][j]=new ArrayList<Color>();
//					}
//				}
//	
//				double startx = extent.getMinX();
//				double starty = extent.getMaxY();
//	
//				for (FilledStamp fs : renderedStamps) {
//					List<Area> stampAreas = fs.stamp.getFillAreas(reqPO);
//	
//					for (Area shapeArea : stampAreas) {
//						for (int i=0; i<bi.getWidth(); i++) {
//							for (int j=0; j<bi.getHeight(); j++) {
//								double x = startx + (i*1.0)/ppd;
//								double y = starty - (j*1.0)/ppd;
//	
//								if (shapeArea.contains(x, y)) {
//									// TODO: Does this really require the Main.PO projection or can it be other values?
//									Point2D worldCoord = new Point2D.Double(x,y);
//									Point2D spatialCoord = reqPO.convWorldToSpatial(worldCoord);
//									HVector spatialVector = new HVector(spatialCoord);
//									
//									double val = (double)fs.pdsi.getRGB(spatialVector, reqPO, ppd);
//									
//									// TODO: Support multiple ignore values?
//									if (val==ignore[0]) continue; 
//									dataGrid[i][j].add(val);
//								}
//							}
//						}
//					}
//				}
//				
//				for (int i=0; i<bi.getWidth(); i++) {
//					for (int j=0; j<bi.getHeight(); j++) {
//						double avgVal = 0.0;
//						
//						if (dataGrid[i][j].size()>0) {
//							avgVal = processValues(dataGrid[i][j]);
//						} else {
//							avgVal = getIgnoreValue()[0];
//						}
//						
//						bi.getRaster().setSample(i,  j,  0, avgVal);
//					}
//				}
//				
//				currentStateId = stateIds.get(0);
//				if (cachedStateId == currentStateId) {
//					cachedTiles.put(tileKey, bi);
//				} else {
//					// ?
//				}
//				tileSemaphore.release();
//				
//				return bi;
//				
			}
			
		} finally {
			tileSemaphore.release();
		}
	}

	public int getTopColor(ArrayList<Integer> values) {
		return values.get(values.size()-1);
	}
	
	/**
	 * Decides whether two rectangles overlap 'on the ground', by getting all
	 * parts of the [0,360] wrapped world coordinates covered by each rectangle
	 * and seeing if any part of the first rectangle touches any part of the
	 * second rectangle
	 */
	protected static boolean hit(Rectangle2D r1, Rectangle2D r2) {
		for (Rectangle2D a: Util.toWrappedWorld(r1)) {
			for (Rectangle2D b: Util.toWrappedWorld(r2)) {
				if (a.intersects(b)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public String getAbstract() {
		return "Stamp layer exposed as a map layer";
	}

	public String[][] getCategories() {
		StampLView view = getView();

		if (view == null || LManager.getLManager() == null) {
			return new String[][]{{settings.getName(), "graphic data"}};
		} else {
			return new String[][]{{LManager.getLManager().getUniqueName(view), "graphic data"}};
		}
	}

	public double[] getIgnoreValue() {
		return ignore;
	}

	public Rectangle2D getLatLonBoundingBox() {
		return new Rectangle2D.Double(0,-90,360,180);
	}

	public MapAttr getMapAttr() {
		return MapAttr.COLOR;
	}

	public void getMapAttr(MapAttrReceiver receiver) {
		receiver.receive(MapAttr.COLOR);
	}

	public double getMaxPPD() {
		return maxPPD;
	}

	public String getMimeType() {
		return null;
	}

	public String getName() {
		return nameRoot + settings.id;
	}
	
	public String getUnits() {
		StampLView view = getView();
		if (view==null) return "";
		return view.getUnits();
	}

	public Point2D getOffset() {
		return new Point(0,0);
	}

	public MapServer getServer() {
		return StampServer.getInstance();
	}

	public String getTitle() {
		StampLView view = getView();
		if (view == null || LManager.getLManager() == null) {
			return "undefined_stamp_source" + " : " + nameRoot;
		} else {
			return LManager.getLManager().getUniqueName(view) + " ("+nameRoot+")";
		}
	}

	public boolean hasNumericKeyword() {
		return false;
	}

	public boolean hasElevationKeyword() {
		return false;
	}
	
	public boolean hasGeologicKeyword() {
		return false;
	}
	
	public String getOwner() {
		return null;
	}

	public boolean isMovable() {
		return false;
	}

	public void setIgnoreValue(double[] ignoreValue) {
		ignore = ignoreValue;
		changed();
	}

	public void setMaxPPD(double maxPPD) throws IllegalArgumentException {
		this.maxPPD = maxPPD;
		changed();
	}
	
	public void setOffset(Point2D offset) {
		if (offset == null || offset.getX() != 0 || offset.getY() != 0) {
			throw new IllegalArgumentException("StampSource#setOffset must not be called with a null or non-zero point");
		}
	}
	
	protected List<MapSourceListener> getListeners() {
		if (listeners == null) {
			listeners = new ArrayList<MapSourceListener>();
		}
		return listeners;
	}
	
	public void addListener(MapSourceListener l) {
		getListeners().add(l);
	}

	public void removeListener(MapSourceListener l) {
		getListeners().remove(l);
	}
	
	public void changed() {
		final List<MapSourceListener> list = new ArrayList<MapSourceListener>(getListeners());
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (MapSourceListener l: list) {
					l.changed(StampSource.this);
				}
			}
		});
	}
	
	public String toString(){
		return getTitle();
	}
	
	public static class MapStampRequest implements StampLView.DrawFilledRequest {
		private final MapRequest request;
		public MapStampRequest(MapRequest request) {
			this.request = request;
		}
		public boolean changed() {
			return request.isCancelled();
		}
		public Rectangle2D getExtent() {
			return request.getExtent();
		}
		public int getPPD() {
			return request.getPPD();
		}
	}
}

