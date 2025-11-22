package edu.asu.jmars.layer.krc;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.JOptionPane;

import org.jfree.data.xy.XYSeries;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.FieldMap;
import edu.asu.jmars.layer.util.features.FieldMap.Type;
import edu.asu.jmars.util.Util;

/**
 * This class represents the data needed to run KRC.
 * The inputs are all stored and can be changed.  The outputs
 * can be null, until KRC is run.
 * Also has information about how to style and display in
 * the lview.
 */
public class KRCDataPoint implements Serializable{
	private static final long serialVersionUID = 3339555062338848606L;
	private transient KRCLayer myLayer;
	private String name;
	private double lat;
	/** in degrees E **/
	private double lon;
	private double lsubs;
	private double hour;
	private double elevation;
	private double albedo;
	private double thermalInertia;
	private double temperature = Double.MAX_VALUE; // Alternate to thermalInertia
	private double opacity;
	private double slope;
	private double azimuth;
	private boolean showPt;
	private boolean showLbl;
	private XYSeries dayData;
	private XYSeries yearData;
	private Color fillColor;
	private Color outlineColor;
	private Color labelColor;
	private int fontSize;
	
	//values used for map sampling
	private final int ppd;
	private final FPath point;
	private double tempVal;
    private static ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactory() {
    	int id = 0;
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName("KRCDataPoint-sampling-" + (id++));
			t.setPriority(Thread.MIN_PRIORITY);
			t.setDaemon(true);
			return t;
		}
    });
	
	private String logOutput;
	
	private DecimalFormat locFormat = new DecimalFormat("#.#####");
	
	/**
	 * A KRC Data Point, truncates the lat and lon inputs to 5 decimal
	 * places and sets all default values lsubs = 100, hour = 13.5, 
	 * opacity = 0.3, and map samples for elevation, albedo, slope,
	 * thermal inertia, and azimuth.
	 * @param name Name of the point
	 * @param lat  Latitude of the point
	 * @param lon  Longitude (degrees E) of the point
	 */
	public KRCDataPoint(KRCLayer layer, String name, double lat, double lon){
		this(layer, name, lat, lon, true);
	}
	
	/**
	 * A KRC Data Point, truncates the lat and lon inputs to 5 decimal
	 * places and sets all default values if setValues is true: 
	 * lsubs = 100, hour = 13.5, opacity = 0.3, and map samples for 
	 * elevation, albedo, slope, thermal inertia, and azimuth.
	 * @param name Name of the point
	 * @param lat  Latitude of the point
	 * @param lon  Longitude (degrees E) of the point
	 * @param setValues Whether to set the input values to defaults or not
	 */
	public KRCDataPoint(KRCLayer layer, String name, double lat, double lon, boolean setValues){
		myLayer = layer;
		this.name = name;
		
		String latStr = locFormat.format(lat);
		String lonStr = locFormat.format(lon);
		
		this.lat = Double.parseDouble(latStr);
		this.lon = Double.parseDouble(lonStr);
		Point2D[] vertices = {new Point2D.Double(lon, lat)};
		point = new FPath(vertices, FPath.SPATIAL_EAST, false);

		ppd = layer.getPPD();
		
		//set ui defaults
		showPt = true;
		showLbl = true;
		fillColor = Color.WHITE;
		outlineColor = Color.BLACK;
		labelColor = Color.WHITE;
		fontSize = 12;
		
		if(setValues){
			//known static defaults
			populateLSubSDefault();
			populateHourDefault();
			populateOpacityDefault();
			
			//map sampled defaults
			populateElevationDefault();
			populateAlbedoDefault();
			populateThermalInertiaDefault();
			populateSlopeDefault();
			populateAzimuthDefault();
		}
	}
	
	/**
	 * Samples the passed in map with the passed in point.  If the value
	 * is null (there is no data at that point), then the returned value
	 * is Double.NaN
	 * @param sampleMap The map to sample
	 * @param point The place to sample
	 * @return The value at that point, returns Double.NaN if there is no
	 * data at that point (value is null).
	 */
	private double getSampledValue(FieldMap sampleMap, FPath point) throws Exception{
		double result = Double.MAX_VALUE;
		Object value = sampleMap.getValue(point);
		if(value == null){
			throw new Exception("Map Sampling Failed");
		}
		else{
			result = (Double)value;
			//truncate values to 2 decimal places
			int decimalPlaces = 100;
			result = Math.floor(result*decimalPlaces)/decimalPlaces;
		}
		return result;
	}
	
	/**
	 * The default value is 100
	 */
	public void populateLSubSDefault(){
		lsubs = 100;
	}
	/**
	 * The default value is 13.5
	 */
	public void populateHourDefault(){
		hour = 13.5;
	}
	/**
	 * The default value is 0.3 on Mars, and 0.0 on all other bodies
	 */
	public void populateOpacityDefault(){
		if(Main.getCurrentBody().equals("mars")){
			opacity = 0.3;
		}else{
			opacity = 0;
		}
	}
	/**
	 * The default value is map sampled from the elevation source,
	 * except on Europa where it is 0.
	 */
	public void populateElevationDefault(){
		//if on Europa, use Sylvain's default until better maps are available
		if(Main.getCurrentBody().equalsIgnoreCase("europa")){
			elevation = 0;
		}else{
			//set the values to Double.MAX_VALUE to start, that way if any map sampling fails
			// there is a value to check against
			elevation = Double.MAX_VALUE;
			mapSampleSource(myLayer.getElevationSource(), "Elevation");
		}
	}
	/**
	 * The default value is map sampled from the albedo source,
	 * except on Europa where it is 0.5
	 */
	public void populateAlbedoDefault(){
		//if on Europa, use Sylvain's default until better maps are available
		if(Main.getCurrentBody().equalsIgnoreCase("europa")){
			albedo = 0.5;
		}else{
			//set the values to Double.MAX_VALUE to start, that way if any map sampling fails
			// there is a value to check against
			albedo = Double.MAX_VALUE;
			mapSampleSource(myLayer.getAlbedoSource(), "Albedo");
		}
	}
	/**
	 * The default value is map sampled from the thermal inertia source,
	 * except on Europa where it is 30
	 */
	public void populateThermalInertiaDefault(){
		//if on Europa, use Sylvain's default until better maps are available
		if(Main.getCurrentBody().equalsIgnoreCase("europa")){
			thermalInertia = 30;
		}else{
			//set the values to Double.MAX_VALUE to start, that way if any map sampling fails
			// there is a value to check against
			thermalInertia = Double.MAX_VALUE;
			mapSampleSource(myLayer.getThermalInertiaSource(), "Thermal Inertia");
		}
	}
	/**
	 * The default value is map sampled from the slope source,
	 * except on Europa where it is 0
	 */
	public void populateSlopeDefault(){
		//if on Europa, use Sylvain's default until better maps are available
		if(Main.getCurrentBody().equalsIgnoreCase("europa")){
			slope = 0;
		}else{
			//set the values to Double.MAX_VALUE to start, that way if any map sampling fails
			// there is a value to check against
			slope = Double.MAX_VALUE;
			mapSampleSource(myLayer.getSlopeSource(), "Slope");
		}
	}
	/**
	 * The default value is map sampled from the azimuth source,
	 * except on Europa where it is 0
	 */
	public void populateAzimuthDefault(){
		//if on Europa, use Sylvain's default until better maps are available
		if(Main.getCurrentBody().equalsIgnoreCase("europa")){
			azimuth = 0;
		}else{
			//set the values to Double.MAX_VALUE to start, that way if any map sampling fails
			// there is a value to check against
			azimuth = Double.MAX_VALUE;
			mapSampleSource(myLayer.getAzimuthSource(), "Azimuth");
		}
	}
	
	private void mapSampleSource(MapSource source, String inputName){
		//set the values to Double.MAX_VALUE to start, that way if any map sampling fails
		// there is a value to check against
		tempVal = Double.MAX_VALUE;
		
		//cannot call map sampling on the awt thread, so create a separate one
		pool.execute(new Runnable() {
			public void run() {
				boolean nullSource = false;
				//sample the map, check to make sure they don't return
				// null at the given point, and if so, set to Double.MAX_VALUE,
				// and then the focus panel can check for this before setting the textfield
				try{
					if(source == null){
						nullSource = true;
					}else{
						FieldMap map = new FieldMap("map sampling", Type.AVG, ppd, source, 0);
						tempVal = getSampledValue(map, point);
						
						//for the elevation input specifically, check units
						if(source.equals(myLayer.getElevationSource()) && tempVal != Double.MAX_VALUE){
							//elevation needs to be in km, if the source is in m, divide by 1000
							String elevUnits = myLayer.getElevationSource().getUnits();
							if(elevUnits.equalsIgnoreCase("m") | elevUnits.equalsIgnoreCase("meters")){
								tempVal = tempVal/1000;
							}
						}
						
						//set the value on the proper input
						if(source.equals(myLayer.getElevationSource())){
							elevation = tempVal;
						}
						else if(source.equals(myLayer.getAlbedoSource())){
							albedo = tempVal;
						}
						else if(source.equals(myLayer.getThermalInertiaSource())){
							thermalInertia = tempVal;
						}
						else if(source.equals(myLayer.getSlopeSource())){
							slope = tempVal;
						}
						else if(source.equals(myLayer.getAzimuthSource())){
							azimuth = tempVal;
						}
					}
				}catch (Exception e){
					//if the source was not null, but the sample still returned null, notify the user
					if(!nullSource){
					Util.showMessageDialog(inputName+" default input values is null!\n"
						  + " Please enter a value before running KRC.",
							"Map Sampling Returned Null Value", 
							JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
	}
	
	/**
	 * @return The name of this krc data point
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * @return  The latitude for this krc data point
	 */
	public double getLat(){
		return lat;
	}

	/**
	 * @return  The longitude for this krc data point
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
	 * @return The elevation for this point, returns Double.MAX_VALUE
	 * if the map sample was null
	 */
	public double getElevation(){
		return elevation;
	}
	
	/**
	 * @return The albedo for this point, returns Double.MAX_VALUE
	 * if the map sample was null
	 */
	public double getAlbedo(){
		return albedo;
	}
	
	/**
	 * @return The thermal inertia for this point, returns Double.MAX_VALUE
	 * if the map sample was null
	 */
	public double getThermalInertia(){
		return thermalInertia;
	}
	
	/**
	 * @return The inputted temperature value for this point, returns Double.MAX_VALUE if not specified
	 */
	public double getTemperature() {
		return temperature;
	}
	
	/**
	 * @return The opacity for this point
	 */
	public double getOpacity(){
		return opacity;
	}
	
	/**
	 * @return The slope for this point, returns Double.MAX_VALUE
	 * if the map sample was null
	 */
	public double getSlope(){
		return slope;
	}
	
	/**
	 * @return The azimuth for this point, returns Double.MAX_VALUE
	 * if the map sample was null
	 */
	public double getAzimuth(){
		return azimuth;
	}
	
	/**
	 * @return The data for the day chart, can be null
	 * if krc has not been run with the most current input values.
	 */
	public XYSeries getDayData(){
		return dayData;
	}
	
	/**
	 * @return The data for the year chart, can be null
	 * if krc has not been run with the most current inputs
	 */
	public XYSeries getYearData(){
		return yearData;
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
	 * @return The log output from any KRC run for this datapoint
	 */
	public String getLogOutput() {
		return logOutput;
	}
	
	private void clearChartData(){
		dayData = null;
		yearData = null;
	}
	
	/**
	 * Set the l sub s for this point
	 * and clear the chart data
	 * @param ls
	 */
	public void setLSubS(double ls){
		if(lsubs != ls){
			lsubs = ls;
			clearChartData();
		}
	}
	
	/**
	 * Set the hour for this point
	 * and clear the chart data
	 * @param hour
	 */
	public void setHour(double hour){
		if(this.hour != hour){
			this.hour = hour;
			clearChartData();
		}
	}
	
	/**
	 * Set the elevation for this point
	 * and clear the chart data
	 * @param elevation
	 */
	public void setElevation(double elevation){
		if(this.elevation != elevation){
			this.elevation = elevation;
			clearChartData();
		}
	}
	
	/**
	 * Set the albedo for this point
	 * and clear the chart data
	 * @param albedo
	 */
	public void setAlbedo(double albedo){
		if(this.albedo != albedo){
			this.albedo = albedo;
			clearChartData();
		}
	}
	
	/**
	 * Set the thermal inertia for this point
	 * and clear the chart data
	 * @param thermalInertia
	 */
	public void setThermalInertia(double thermalInertia){
		if(this.thermalInertia != thermalInertia){
			this.thermalInertia = thermalInertia;
			clearChartData();
		}
	}
	
	/**
	 * Set the temperature for this point
	 * and clear the chart data
	 * @param newTemperature
	 */
	public void setTemperature(double newTemperature) {
		if (this.temperature != newTemperature) {
			this.temperature = newTemperature;
			clearChartData();
		}
	}
	
	/**
	 * Set the opacity for this point
	 * and clear the chart data
	 * @param opacity
	 */
	public void setOpacity(double opacity){
		if(this.opacity != opacity){
			this.opacity = opacity;
			clearChartData();
		}
	}
	
	/**
	 * Set the slope for this point
	 * and clear the chart data
	 * @param slope
	 */
	public void setSlope(double slope){
		if(this.slope != slope){
			this.slope = slope;
			clearChartData();
		}
	}
	
	/**
	 * Set the azimuth for this point
	 * and clear the chart data
	 * @param azimuth
	 */
	public void setAzimuth(double azimuth){
		if(this.azimuth != azimuth){
			this.azimuth = azimuth;
			clearChartData();
		}
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
	
	/**
	 * Set the chart data for the day results
	 * @param data
	 */
	public void setDayData(XYSeries data){
		dayData = data;
	}
	
	/**
	 * Set the chart data for the year results
	 * @param data
	 */
	public void setYearData(XYSeries data){
		yearData = data;
	}
	
	/**
	 * Set the log output
	 * @param logOutput
	 */
	public void setLogOutput(String newLogOutput) {
		logOutput = newLogOutput;
	}
}
