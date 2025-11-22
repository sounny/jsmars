package edu.asu.jmars.layer.threed;

import java.awt.image.Raster;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.map2.MapChannel;
import edu.asu.jmars.layer.map2.MapChannelReceiver;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.MapRequest;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.util.DebugLog;

/* Layer data model */
public class ThreeDLayer extends Layer {
	private static DebugLog log = DebugLog.instance();
	
	/** The active view that provides the viewing geometry for the 3D panel */
    private ThreeDLView activeView;
    /** Most recent requests from all views */
    private Map<ThreeDLView, Request> requests = new LinkedHashMap<ThreeDLView,Request>();
    /** Map to use for elevation data */
    private MapSource elevationSource;
    /** Map producer provides maps for current request for current view for current map source */
    private MapChannel mapProducer = new MapChannel();
    /** Map data last received from the map producer */
    private MapData lastUpdate;
    
    public ThreeDLayer(StartupParameters parms) {
		initialLayerData = parms;
		elevationSource = parms.getMapSource();
		
    	mapProducer.addReceiver(new MapChannelReceiver() {
			public void mapChanged(MapData mapData) {
				if (mapData.isFinished()) {
					lastUpdate = mapData;
					activeView.getFocusPanel().update();
					activeView.setVisible(false);
				}
			}
    	});
	}
    
    /** Map to use for elevation data */
    public MapSource getElevationSource() {
    	return elevationSource;
    }
    
    /** Change map to use for elevation data */
    public void setElevationSource(MapSource source) {
    	this.elevationSource = source;
    }
    
	/** The active view that provides the viewing geometry for the 3D panel */
    public ThreeDLView getActiveView() {
    	return activeView;
    }
    
    /** Change the active view that provides the viewing geometry for the 3D panel */
    public void setActiveView(ThreeDLView view) {
    	this.activeView = view;
    }
    
    /** Returns the raster for the current elevation data */
    public Raster getElevationData() {
    	return lastUpdate.getImage().getRaster();
    }
    
    /**
	 * Saves all requests in case the user changes which view drives the 3D
	 * layer, and builds a MapChannel for the active view.
	 * 
	 * The resulting MapData is cached and pushed to the focus panel when
	 * finished.
	 */
    public void receiveRequest(Object layerRequest, DataReceiver requester) {
    	Request request = (Request)layerRequest;
    	Request oldRequest = requests.put(request.source, request);
    	log.println("Old request: " + oldRequest);
    	if (request.source == getActiveView() && elevationSource != null) {
        	mapProducer.setRequest(new MapRequest(elevationSource, request.extent, request.ppd, request.projection));
    	}
	}
}


