package edu.asu.jmars.layer.stamp.sources;

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
import java.util.Arrays;
import java.util.Comparator;
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
import edu.asu.jmars.layer.stamp.sources.StampSource.MapStampRequest;
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
public class NumericStampSource extends StampSource implements MapSource, Serializable {
    private static final long serialVersionUID = -1840073561231935949L;
			
	public NumericStampSource(StampLayerSettings settings) {
		super(settings);
		nameRoot = "top_value";
		double ignoreVal[] = new double[1];
		ignoreVal[0]=StampImage.IGNORE_VALUE;
		 
		setIgnoreValue(ignoreVal);
	}
						
	protected BufferedImage fetchDirect(MapRequest req) throws RetryableException, NonRetryableException {
		StampLView view = getView();

		ArrayList<Integer> stateIds=view.getLayer().getStateIds();
		int currentStateId = stateIds.get(0);
		if (currentStateId==0 || cachedStateId != currentStateId) {
			clearCache();			
			cachedStateId = currentStateId;
		}

		// TODO: Is this a reasonable key?
		String tileKey = req.getProjection().getProjString() + " : " + req.getExtent().toString() + " : " + req.getPPD()+ " : " + view.getLayer().getStateId(1);

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
			
			SampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_DOUBLE, size.width, size.height, 1, size.width, new int[] {0});
			
			DataBuffer db = new DataBufferFloat(size.width * size.height);
			WritableRaster wr = Raster.createWritableRaster(sm, db, null);
			ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
			ColorModel cm = new ComponentColorModel(cs, false, true, Transparency.OPAQUE, DataBuffer.TYPE_DOUBLE);
			
			bi = new BufferedImage(cm, wr, true, null);		
					
			double[] fdata = new double[size.width*size.height];
			
			for (int i=0;i<fdata.length;i++) {
				fdata[i]=StampImage.IGNORE_VALUE;  // Initialize with ignore value.  Anything not explicitly filled should be rendered transparent
			}
			
			bi.getRaster().setDataElements(0, 0, size.width, size.height, fdata);
			
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
				
				// TODO: Do we need to worry about projection variations?  Hmm...  This needs to take a ProjObj parameter
				List<StampShape> stampsWithinExtent = view.findStampsByWorldRect(extent, reqPO);
	
				OutlineFocusPanel ofp = null;
				
				if (view.getChild()==null) {
					ofp = ((StampFocusPanel)view.getParentLView().focusPanel).outlinePanel;
				} else {
					ofp = ((StampFocusPanel)view.focusPanel).outlinePanel;
				}
					
				StampGroupComparator orderSort = ofp.getOrderSort();
				
				Collections.sort(stampsWithinExtent, orderSort);
								
				ArrayList dataGrid[][] = new ArrayList[bi.getWidth()][bi.getHeight()];
				
				for (int i=0; i<bi.getWidth(); i++) {
					for (int j=0; j<bi.getHeight(); j++) {
						dataGrid[i][j]=new ArrayList<Double>();
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
									dataGrid[i][j].add(stamp.getCalculatedValue());
								}
							}
						}
					}
				}
				
				for (int i=0; i<bi.getWidth(); i++) {
					for (int j=0; j<bi.getHeight(); j++) {
						double avgVal = 0.0;
						
						if (dataGrid[i][j].size()>0) {
							avgVal = processValues(dataGrid[i][j]);
						} else {
							avgVal = getIgnoreValue()[0];
						}
						
						bi.getRaster().setSample(i,  j,  0, avgVal);
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
				// Non-spectra layers
				int ppd = req.getPPD();
				Rectangle2D extent = req.getExtent();
				
				///
				List<FilledStamp> renderedStamps = view.getFilteredFilledStamps();
				for (Iterator<FilledStamp> it = renderedStamps.iterator(); it.hasNext(); ) {
					FilledStampImageType fs = (FilledStampImageType)it.next();
					
					if (!fs.pdsi.isNumeric()) {
						it.remove();
						continue;
					}
					
					Point2D offset=fs.getOffset();
					
					List<Area> stampAreas = fs.stamp.getFillAreas(reqPO);
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
				///
								
				ArrayList<Double> dataGrid[][] = new ArrayList[bi.getWidth()][bi.getHeight()];
				
				for (int i=0; i<bi.getWidth(); i++) {
					for (int j=0; j<bi.getHeight(); j++) {
						dataGrid[i][j]=new ArrayList<Double>();
					}
				}
	
				double startx = extent.getMinX();
				double starty = extent.getMaxY();
	
				for (FilledStamp fs : renderedStamps) {
					List<Area> stampAreas = fs.stamp.getFillAreas(reqPO);
	
					// Factor user nudge into the results
					Point2D offset = ((FilledStampImageType)fs).getOffset();
					
					for (Area shapeArea : stampAreas) {
						for (int i=0; i<bi.getWidth(); i++) {
							for (int j=0; j<bi.getHeight(); j++) {
								double x = startx + (i*1.0)/ppd;
								double y = starty - (j*1.0)/ppd;
	
								x -= offset.getX();
								y -= offset.getY();
								
								if (shapeArea.contains(x, y)) {
									Point2D worldCoord = new Point2D.Double(x,y);
									Point2D spatialCoord = reqPO.convWorldToSpatial(worldCoord);
									HVector spatialVector = new HVector(spatialCoord);
									
									double val = (double)fs.pdsi.getFloatVal(spatialVector, reqPO, ppd);
									
									// TODO: Support multiple ignore values?
									if (val==ignore[0]) continue; 
									dataGrid[i][j].add(val);
								}
							}
						}
					}
				}
				
				for (int i=0; i<bi.getWidth(); i++) {
					for (int j=0; j<bi.getHeight(); j++) {
						double avgVal = 0.0;
						
						if (dataGrid[i][j].size()>0) {
							avgVal = processValues(dataGrid[i][j]);
						} else {
							avgVal = getIgnoreValue()[0];
						}
						
						bi.getRaster().setSample(i,  j,  0, avgVal);
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
				
			}
			
//			// do the rendering on this thread
//			List<FilledStamp> stamps = view.getFilteredFilledStamps();
//			for (Iterator<FilledStamp> it = stamps.iterator(); it.hasNext(); ) {
//				FilledStampImageType fs = (FilledStampImageType)it.next();
//				
//				if (!fs.pdsi.isNumeric()) {
//					it.remove();
//					continue;
//				}
//				
//		        // TODO: Change this back		
//				Point2D offset=fs.getOffset();
//				
//				List<Area> stampAreas = fs.stamp.getFillAreas();
//				boolean hitThisStamp=false;
//				
//				for (Area area : stampAreas) {				
//					Rectangle2D imageBounds=area.getBounds2D();
//					imageBounds.setRect(imageBounds.getMinX()+offset.getX(), imageBounds.getMinY()+offset.getY(), imageBounds.getWidth(), imageBounds.getHeight());
//					if (hit(imageBounds, req.getExtent())) {
//						hitThisStamp=true;
//						break;  // Once we've hit this stamp once, we don't need to search further
//					}
//				}
//				
//				if (!hitThisStamp) {
//					it.remove();
//				}
//			}
//			
//			// if there are no stamps to render, don't call the stamp layer, and
//			// don't cache the image since the lookup is more than fast enough that
//			// we don't want to pollute the cache with empty images
//			if (!stamps.isEmpty()) {
//				
//				view.doRender(new MapStampRequest(req), stamps, progress, g2, bi);
//				
//				// wait on completion of rendering thread, as caught by the RenderProgress above
//				// there is no method to hand off exceptions from the rendering thread, but
//				// we don't need to handle those intricacies at the moment
//				synchronized(done) {
//					while (!done[0]) {
//						try {
//							done.wait(30000);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//							throw new RetryableException("Timed out", e);
//						} catch (Throwable t) {
//							t.printStackTrace();
//							throw new NonRetryableException("Exception while waiting on numeric stamp rendering", t);
//						}
//					}
//				}
//			}
//			
//			return bi;
		} finally {
			tileSemaphore.release();
		}
	}
	
	public double processValues(ArrayList<Double> values) {
		return values.get(values.size()-1);
	}
		
	public String getAbstract() {
		return "Numeric Stamp layer exposed as a map layer";
	}

	public String[][] getCategories() {
		StampLView view = getView();

		if (view == null || LManager.getLManager() == null) {
			return new String[][]{{settings.getName(), "numeric data"}};
		} else {
			return new String[][]{{LManager.getLManager().getUniqueName(view), "numeric data"}};
		}
	}

	public MapAttr getMapAttr() {
		return MapAttr.SINGLE_BAND;
	}

	public void getMapAttr(MapAttrReceiver receiver) {
		receiver.receive(MapAttr.SINGLE_BAND);
	}
	
	public boolean hasNumericKeyword() {
		return true;
	}

	public boolean hasElevationKeyword() {
		return true;
	}
		
	public void setOffset(Point2D offset) {
		if (offset == null || offset.getX() != 0 || offset.getY() != 0) {
			throw new IllegalArgumentException("NumericStampSource#setOffset must not be called with a null or non-zero point");
		}
	}	
}

