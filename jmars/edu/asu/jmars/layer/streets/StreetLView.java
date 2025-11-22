package edu.asu.jmars.layer.streets;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.terracotta.statistics.Time;

import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.util.Util;



public class StreetLView extends LView {

	private StreetLayer layer;
	private static final String VIEW_SETTINGS_KEY = "open_street_map";
	private int osmType;
	
	public StreetLView(StreetLayer layer, LayerParameters lp) { 
		super(layer);
		this.layer = layer;
		setLayerParameters(lp);
		osmType = layer.getOsmType();
	}
	
	public SerializedParameters getInitialLayerData() {
		return (StreetLViewSettings) getLayer().streetSettings;
	}

	protected LView _new() {
		// Create a copy of ourself for use in the panner-view.
		return new StreetLView((StreetLayer) getLayer(), getLayerParameters()); 	
		
	}
	
	double lastUpdate = 0.00;
	
	public void viewChanged() {
		//creates a time stamp for re-drawing the screen. So it will not draw right away. This 
		//helps with min/max the screen as well as panning around. 
		try {
			lastUpdate = Time.time();
			Thread.sleep(600);
			double currentUpdate = Time.time();
			double timeLapse = currentUpdate - lastUpdate;
			if (timeLapse <600){			
				return;
			}else { 
				super.viewChanged();
				lastUpdate = currentUpdate;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
		 
	
	protected Object createRequest(Rectangle2D where) {
		
		//System.out.println("StreetLView.createRequest()");
		
		//Get the Bounding box of the mainview/panner  

		int jmarsZoom = viewman.getProj().getPPD();
		Rectangle2D here = viewman.getProj().getWorldWindow();	
		Rectangle screenHere = viewman.getProj().getScreenWindow();		
		
		Point2D spatialNW = viewman.getProj().world.toSpatial(here.getMinX(),
				here.getMaxY());		
		
		Point2D spatialSE = viewman.getProj().world.toSpatial(here.getMaxX(),
				here.getMinY());

		Point2D spatialCenter = viewman.getProj().world.toSpatial(here.getCenterX(), here.getCenterY());

		double westLon = 360 - spatialNW.getX();
		westLon = (westLon > 180) ? westLon - 360.0 : westLon;

		double eastLon = 360 - spatialSE.getX();
		eastLon = (eastLon > 180) ? eastLon - 360.0 : eastLon;
		
		double northLat = spatialNW.getY();
		double southLat = spatialSE.getY();		

		final BoundingBoxDataObject boundingBox = new BoundingBoxDataObject(screenHere, spatialCenter, westLon, eastLon, northLat, southLat, jmarsZoom);
		
		return boundingBox;
		
	}

	public void receiveData(Object layerData) {
	
		
		/* Here the object needs to be pasted to offScreen and repaint 
		 * needs to be called to draw to the screen 
		 */
		
		OpenStreetMapTiles osm = (OpenStreetMapTiles) layerData;

		//System.out.println("StreetLView.receiveData()");
		
		Graphics2D g2 = getOffScreenG2Direct();	
		if (g2==null)
			return;

		double yTileLat = osm.getOdetic2ocenN1(); 
		double xTileLon = osm.getxTileLonW();

		Point2D screenPoint = viewman.getProj().spatial.toScreen(360-xTileLon, yTileLat);
		int rasterX1 = (int) screenPoint.getX();
		int rasterY1 = (int) screenPoint.getY();
		
		g2.drawImage(osm.getScaledImage1(), null, rasterX1 , rasterY1);		
		repaint();
		layer.setStatus(Util.darkGreen);

	}
	
	public void setLayerParameters(LayerParameters lp){
 		layerParams=lp;
 		layer.streetSettings.layerParams = lp;
 	}
	

	public StreetLayer getLayer() {
		return layer;
	}

	public void setLayer(StreetLayer layer) {
		this.layer = layer;
	}

	public String getName() {		
		if (osmType == 0){
			return "MapQuest/OSM Satellite Tiles";
		}else if (osmType ==1){
			return "MapQuest/OSM Map Tiles";
		}	
		return "MapQuest-OpenStreetMap";
	}
	
}
	


