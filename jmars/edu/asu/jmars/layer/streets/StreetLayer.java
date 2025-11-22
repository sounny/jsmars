package edu.asu.jmars.layer.streets;

import java.awt.Color;
import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;

public class StreetLayer extends Layer  {

	public StreetLViewSettings streetSettings = new StreetLViewSettings();
	int osmType;
	static final int osmZoom = 19;

	
	public StreetLayer(int osmIntValue) {
		this.osmType = osmIntValue;

	}

	public void receiveRequest(Object layerRequest, DataReceiver requester) {
		
		/*Using the object created in the createRequest (Lview) that specifies the 
			bbox in Lat/lon, calculate the zoom from OSM in meters per pixel and kick 
			off the rest of the process in OpenStreetMapTiles. 
		*/

		BoundingBoxDataObject boundingBox = (BoundingBoxDataObject) layerRequest;
		double equaRadius = 6378137.00;
		double equaCircumference = equaRadius *2*(Math.PI);
		double metersInOneDegree = equaCircumference/360;
		double latRadiansValueCenter = (Math.PI/180)* boundingBox.getSpatialCenter().getY();		
		
		double metersPerPixel = 0.00;
		int i = 0;
		int zoom = 0;
		
		//Get the zoom level for OSM at at a particular ppd
		double jmarsPPD = boundingBox.getJmarsPpd();
		double jmarsMeters = metersInOneDegree/jmarsPPD;

		for (i = 1; i <osmZoom; i++){		
			metersPerPixel = equaCircumference * Math.cos(latRadiansValueCenter)/ (Math.pow(2, (i+8)));
				if ((metersPerPixel) > (jmarsMeters)){
					zoom = i;

				}else {
					/*If for some reason the loop does not find a matching JMARS(in Meters)
						to the OSM MPP then it will exit. This shouldn't happen. It goes through 
						every possible zoom level so there should always be a matched number.
					*/
						break; 
				}
				
		}
		
	
		OpenStreetMapTiles osmTiles = new OpenStreetMapTiles(osmType);
		StreetLayer.this.setStatus(Color.yellow);		
		osmTiles.callStatus(this);
		osmTiles.getTileImages(boundingBox, zoom, jmarsMeters, equaCircumference);		
		requester.receiveData(osmTiles);

		
	}

	public final int getOsmType() {
		return osmType;
	}

	public final void setOsmType(int osmType) {
		this.osmType = osmType;
		this.streetSettings.osmType = this.osmType;
	}
	
}
