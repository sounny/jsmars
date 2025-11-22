package edu.asu.jmars.layer.mcd;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.jfree.data.xy.XYSeries;

/**
 * This class represents the data needed to run MCD.
 * The inputs are all stored and can be changed.  The outputs
 * can be null, until MCD is run.
 * Also has information about how to style and display in
 * the lview.
 */
public class MCDDataPoint {
	private String name;
	private double lat;
	/** in degrees E **/
	private double lon;
	private double lsubs;
	/** local time **/
	private double hour;
	/** height in meters above surface */
	private double height;
	private int scenario;
	private String elevations="0,1,10,100,1000,5000,10000,20000,40000,60000,80000,100000";
	
	private boolean showPt;
	private boolean showLbl;
	private Color fillColor;
	private Color outlineColor;
	private Color labelColor;
	private int fontSize;
	
	MCDData elevationData;
	MCDData lsData;
	MCDData timeData;
	MCDData pressureData;
	
	private DecimalFormat locFormat = new DecimalFormat("#.#####");
	
	
	/**
	 * A MCD Data Point, truncates the lat and lon inputs to 5 decimal
	 * places and sets all default values if setValues is true: 
	 * lsubs = 100, hour = 12, height = 0
	 * @param name Name of the point
	 * @param lat  Latitude of the point
	 * @param lon  Longitude (degrees E) of the point
	 */
	public MCDDataPoint(String name, double lat, double lon){
		this.name = name;

		//set lat and lon
		String latStr = locFormat.format(lat);
		String lonStr = locFormat.format(lon);
		this.lat = Double.parseDouble(latStr);
		this.lon = Double.parseDouble(lonStr);
		
		//set default inputs
		hour = 12;
		height = 0;
		lsubs = 100;
		scenario = 1;
		
		//set ui defaults
		showPt = true;
		showLbl = true;
		fillColor = Color.WHITE;
		outlineColor = Color.BLACK;
		labelColor = Color.WHITE;
		fontSize = 12;
		
	}
	
	/**
	 * @return The name of this mcd data point
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * @return  The latitude for this mcd data point
	 */
	public double getLat(){
		return lat;
	}

	/**
	 * @return  The longitude for this mcd data point
	 */
	public double getLon(){
		return lon;
	}
	
	/**
	 * @return The l_s for this point
	 */
	public double getLSubS(){
		return lsubs;
	}
	
	/**
	 * @return The hour for this point
	 */
	public double getHour(){
		return hour;
	}
	
	/**
	 * @return The height in meters above the surface
	 */
	public double getHeight(){
		return height;
	}
	
	/**
	 * @return The scenario TODO: need a little more info
	 */
	public int getScenario(){
		return scenario;
	}
	
	/**
	 * @return The elevations for this point
	 */
	public String getElevations() {
		return elevations;
	}
	
	/**
	 * @return Whether or not this data point should be displayed
	 * in the lview.  False if it's hidden.
	 */
	public boolean showPoint(){
		return showPt;
	}
	
	/**
	 * @return Whether or not to show the label for the name of this
	 * data point in the lview.
	 */
	public boolean showLabel(){
		return showLbl;
	}
	
	/**
	 * @return Returns the spatial lat and lon (W) as a point
	 */
	public Point2D getPoint(){
		return new Point2D.Double(360-lon, lat);
	}
	
	/**
	 * @return The color to fill the shape displayed on the LView
	 */
	public Color getFillColor(){
		return fillColor;
	}
	/**
	 * @return The color to outline the shape displayed on the LView
	 */
	public Color getOutlineColor(){
		return outlineColor;
	}
	/**
	 * @return The color to draw the label for the name on the LView
	 */
	public Color getLabelColor(){
		return labelColor;
	}
	
	/**
	 * @return The size of the font to display the name label in the lview
	 */
	public int getFontSize(){
		return fontSize;
	}
		
	/**
	 * Set the l sub s for this point
	 * @param ls
	 */
	public void setLSubS(double ls){
		if(lsubs != ls){
			lsubs = ls;
		}
	}
	
	/**
	 * Set the hour for this point
	 * @param hour
	 */
	public void setHour(double hour){
		if(this.hour != hour){
			this.hour = hour;
		}
	}
	
	/**
	 * Set the height for this point
	 * @param height
	 */
	public void setHeight(double height){
		if(this.height != height){
			this.height = height;
		}
	}
	
	/**
	 * Set the scenario for this point
	 * @param scenario
	 */
	public void setScenario(int scenario){
		if(this.scenario != scenario){
			this.scenario = scenario;
		}
	}
	
	/**
	 * Set the elevations for this point
	 * @param elevations
	 */
	public void setElevations(String elevations) {
		this.elevations = elevations;
	}
	
	/**
	 * Set whether to show or hide the point on the lview
	 * @param show
	 */
	public void setShowPoint(boolean show){
		showPt = show;
	}
	
	/**
	 * Set whether to show or hide the label on the lview
	 * @param show
	 */
	public void setShowLabel(boolean show){
		showLbl = show;
	}
	
	/**
	 * Set the color for the outline of the point
	 * @param color
	 */
	public void setOutlineColor(Color color){
		outlineColor = color;
	}
	
	/**
	 * Set the color for the fill of the point
	 * @param color
	 */
	public void setFillColor(Color color){
		fillColor = color;
	}
	
	/**
	 * Set the size for the font for the label
	 * @param size
	 */
	public void setFontSize(int size){
		fontSize = size;
	}
	
	/**
	 * Set the color for the label on the lview
	 * @param color
	 */
	public void setLabelColor(Color color){
		labelColor = color;
	}	
}
