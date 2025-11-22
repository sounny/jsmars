package edu.asu.jmars.layer.streets;

import java.awt.Rectangle;
import java.awt.geom.Point2D;

public class BoundingBoxDataObject {
	private double westLon=0.00;
	private double eastLon=0.00;
	private double northLat = 0.00;
	private double southLat = 0.00;
	private Point2D spatialCenter = null;

	private int jmarsPpd=0;
	
	
	public BoundingBoxDataObject(Rectangle viewScreen, Point2D screenCenter, double wLon, double eLon, double nLat, double sLat, int jmarsZoom){
		this.spatialCenter  = screenCenter;
		this.westLon = wLon;
		this.eastLon = eLon;
		this.northLat = nLat;
		this.southLat = sLat;
		this.jmarsPpd = jmarsZoom; 
			
	}

	public int getJmarsPpd() {
		return jmarsPpd;
	}

	public Point2D getSpatialCenter() {
		return spatialCenter;
	}

	public double getWestLon() {
		return westLon;
	}

	public double getEastLon() {
		return eastLon;
	}

	public double getNorthLat() {
		return northLat;
	}

	public double getSouthLat() {
		return southLat;
	}
	
}
 