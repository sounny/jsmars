package edu.asu.jmars.layer.slider;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.map2.CacheManager;
import edu.asu.jmars.layer.map2.MapChannel;
import edu.asu.jmars.layer.map2.MapChannelReceiver;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.MapRequest;
import edu.asu.jmars.layer.map2.MapRetriever;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.MapTile;
import edu.asu.jmars.layer.map2.Pipeline;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.WMSMapServer;
import edu.asu.jmars.layer.map2.stages.GrayscaleStage;
import edu.asu.jmars.layer.map2.stages.GrayscaleStageSettings;
import edu.asu.jmars.util.Util;

public class SliderLView extends LView {

	public ArrayList<String> names;
	public ArrayList<MapSource> sources = new ArrayList<MapSource>();
	public String displayName;
		
	public SliderLView(Layer layerParent, LayerParameters lp) {
		super(layerParent);
		names = lp.options;
		displayName = lp.name;
		layerParams = lp;
		
		List<MapServer> servers = MapServerFactory.getMapServers();
		namesLoop: for(String match: names){
			for (MapServer server : servers){
				List<MapSource> sourcelist = server.getMapSources();
				for(MapSource map: sourcelist){
					if(map.getName().equalsIgnoreCase(match)){
						sources.add(map);
						continue namesLoop;
					}
				}
			}
		}
		
	}

	public SliderLView(Layer layerParent, ArrayList<MapSource> n, boolean dumb, LayerParameters lp) {
		super(layerParent);
		sources = n;	
		layerParams = lp;
	}
	
	protected LView _new() {
		return new SliderLView(getLayer(), sources, true, layerParams);
	}

	SliderFocusPanel focusPanel = null;
	
	public SliderFocusPanel getFocusPanel() {
		if (focusPanel==null) {
			if (getParentLView()==null) {
				focusPanel = new SliderFocusPanel(this);
			} else {
				return (SliderFocusPanel)getParentLView().getFocusPanel();
			}
		}
		return focusPanel;
	}
	
	public void viewChanged() {
		super.viewChanged();
		
		if (getChild()!=null) {
		} else {
		}
    	if (!getFocusPanel().movieIsPlaying()) {
    		getFocusPanel().updateStatus();
    	}

	}
	
	public String getName() {
		return displayName;
	}
	
	public String toString() {
		return getName();
	}
	
	/**
	 * Create request to send to the Layer, which will filter on an LView from
	 * which to create the 3D view. This is only called when the layer is visible,
	 * and so requests are only sent when someone explicitly enables a layer,
	 * we disable visibility immediately.
	 */
	protected synchronized Object createRequest(Rectangle2D where) {
	    final int val = getFocusPanel().slider.getValue()-1;
		return requestMap(sources.get(val), val, true);
	}
	
	private MapRequest lastRequest = null;

	/*
	 * This method should completely replace createRequest() but it doesn't currently to 
	 * prevent Layer/Manager level objects that call createRequest() from breaking
	 */
	protected synchronized Object requestMap(MapSource ms, final int sourceIndex, final boolean displayMap) {
		if (isAlive()) {
			Rectangle2D extent = getProj().getWorldWindow();
			int ppd = 	viewman.getZoomManager().getZoomPPD();
	    	MapRequest mr = new MapRequest(ms, extent, ppd, Main.PO);

	    	if (lastRequest != null) {
	    		// if the main view or the panner view have changed stop the slide 
	    		// show if needed and clear out any cached maps
	    		if (!lastRequest.getExtent().equals(extent) 
	    				|| lastRequest.getPPD() != ppd 
	    				|| lastRequest.getProjection()!=mr.getProjection()) {
	    			/* enable this code to auto stop the slide show when the 
	    			 * projection, zoom level, or window extent changes
	    			if (displayMap) {
		    			if (getFocusPanel().movieIsPlaying()) {
			    				getFocusPanel().pauseMovie();
		    			}	
	    			}
	    			*/
	    			getFocusPanel().updateStatus();
	    			lastRequest = mr;
	    			return null;
	    		} 
	    	}
			lastRequest = mr;
	    	
	    	MapChannel mapProducer = new MapChannel();
	    	mapProducer.setRequest(mr);
	      	if (ms.hasNumericKeyword()) {
		    	Stage[] s = new Stage[1];
		    	s[0] = new GrayscaleStage(new GrayscaleStageSettings());
		    	Pipeline p = new Pipeline(ms,s);
		    	Pipeline[] pipelines = {p};
	      		mapProducer.setPipeline(pipelines);
	      	}
	    	mapProducer.addReceiver(new MapChannelReceiver() {
	    		public void mapChanged(MapData mapData) {
	    			if (mapData.isFinished()) {
	    				if (getChild()==null) {
	    					if (!(getFocusPanel().panbuttons[sourceIndex].getBackground()).equals(Color.GREEN)) {
	    						getFocusPanel().panbuttons[sourceIndex].setBackground(Color.GREEN);
	    					}
	    				} else {
	    					if (!(getFocusPanel().mainbuttons[sourceIndex].getBackground()).equals(Color.GREEN)) {
	    						getFocusPanel().mainbuttons[sourceIndex].setBackground(Color.GREEN);
	    					}
	    				}
	    				
	    				if (displayMap && (getFocusPanel().slider.getValue()-1 == sourceIndex)) {
		    				lastUpdate = mapData;
			    			repaint();
	    				}
	    				
	    			} else {
	    				if (getChild()==null) {
	    					if (!(getFocusPanel().panbuttons[sourceIndex].getBackground()).equals(Color.YELLOW)
	    							&& !(getFocusPanel().panbuttons[sourceIndex].getBackground()).equals(Color.GREEN)) {
	    						getFocusPanel().panbuttons[sourceIndex].setBackground(Color.YELLOW);
	    					}
	    				} else {
	    					if (!(getFocusPanel().mainbuttons[sourceIndex].getBackground()).equals(Color.YELLOW)
	    							&& !(getFocusPanel().mainbuttons[sourceIndex].getBackground()).equals(Color.GREEN)) {
	    						getFocusPanel().mainbuttons[sourceIndex].setBackground(Color.YELLOW);
	    					}
	    				}
	    			}
	    			
	    		}
	    	});
    	}
		
		return null;
	}
	
	public void paintComponent(Graphics g) {
		SliderLayer myLayer = (SliderLayer)getLayer();
		if (lastUpdate==null) {
			return;
		} else {
			updateGraphicData(lastUpdate);
		}
		
		super.paintComponent(g);
	}

//	MapChannel mapProducer = new MapChannel();
	public MapData lastUpdate = null;

	private final Rectangle2D worldClip = new Rectangle2D.Double();

	/** Receives a tile of map data and paints the visible portion of it to the back buffer */
	private void updateGraphicData(MapData newData) {
		// this if block currently locks the EDT when the panner view is added or removed
//		if (!isAlive()) {
			// we have nothing to do when the LView is not selected for viewing
//			return;
//		}
		
		// clear the screen and get out if we don't have good data
		if (newData == null) {
			return;
		}
		
		BufferedImage img = newData.getImage();
		
		Graphics2D g2 = null;
		try {
			// At this point, we have something to draw
			g2 = getOffScreenG2();
			if (newData.isFinished() && newData.getFinishedArea().isEmpty()) {
//				// paint an indication of error, the 'death tile'
//				double length = 50d / newData.getRequest().getPPD();
//				Paint p = new TexturePaint(errorTile, new Rectangle2D.Double(0,0,length,length));
//				g2.setPaint(p);
//				g2.fill(newData.getRequest().getExtent());
			} else if (!newData.getValidArea().isEmpty() && img != null) {
				// paint a good MapData object by passing through ignore filter if defined, and clipping to extent
				BufferedImageOp op = newData.getOperator();
				if (op != null) {
					img = op.filter(img, Util.newBufferedImage(img.getWidth(), img.getHeight()));
				}
				Rectangle2D dataBounds = newData.getRequest().getExtent();
				worldClip.setFrame(-180,-90,720,180);
				Rectangle bounds = MapData.getRasterBoundsForWorld(img.getRaster(), dataBounds, worldClip);
				if (!bounds.isEmpty()) {
					g2.setComposite(AlphaComposite.Src);
					Rectangle2D.intersect(worldClip, dataBounds, worldClip);
					img = img.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
					g2.drawImage(img, Util.image2world(img.getWidth(), img.getHeight(), worldClip), null);
				}
			} else {
			}
		} catch (Exception ex) {
		} finally {
			if (g2 != null) {
				g2.dispose();
			}
		}
		
//		repaintDeferred(graphicRequest == null || graphicRequest.isFinished());
	}

	
	@Override
	public void receiveData(Object layerData) {
		// TODO Auto-generated method stub
		
	}
	
	protected synchronized boolean isMapCached (MapRequest mr) {
		
		Set<MapTile> tileSet=MapRetriever.createTiles(mr);
		
		MapTile[] tilesToCheck=new MapTile[tileSet.size()];
		
		tilesToCheck=tileSet.toArray(tilesToCheck);
		
		MapTile[][] tiles=CacheManager.checkCache(tilesToCheck);
		
	
		if (tiles[1].length==0) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void viewCleanup() {
		// clean up any time slider associated Timer tasks that may be running
    	if (getFocusPanel().movieIsPlaying()) {
    		getFocusPanel().pauseMovie();
    	}
	}
	
//The following two methods are used to query for the
// info panel fields (description, citation, etc)	
 	public String getLayerKey(){
 		if(layerParams != null)
 			return layerParams.name;
 		else
 			return getName();
 	}
 	public String getLayerType(){
 		if(layerParams != null)
 			return layerParams.type;
 		else
 			return "timeslider";
 	}
}
