package edu.asu.jmars.layer.profile.chart.table;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.util.Config;


public class PlotObject {

	private Multimap<Integer, Pair<String, Double>> sampleData = ArrayListMultimap.create();
	private double yValue = Double.NaN;
	private double xValue = Double.NaN;
	private double latValue = Double.NaN;
	private double lonValue = Double.NaN;
	private DecimalFormat df = new DecimalFormat("#,###,##0.00");
	private String plotname = "";
	private String rename = null;
	private String uniqueID = "";
	private String plotunit = "";
	private boolean isVisible = true;
	private boolean isNumericSource = false;
	private final int plotID;
	private final String sourceTitle;
	private Color plotColor;
	private Color profileColorInMainView;
	private ProjObj originalproj;
	private List<Point2D> origSpatialPoints = new ArrayList<>();
	NumberFormat nf = NumberFormat.getNumberInstance();
	public static final String NO_VALUE = "No data";

	public PlotObject(int ID, String title) {
		plotID = ID;
		sourceTitle = title;
	}

	public Multimap<Integer, Pair<String, Double>> getSampleData() {
		return sampleData;
	}

	public void setSampleData(Multimap<Integer, Pair<String, Double>> sampleData) {
		this.sampleData = sampleData;
	}

	public double getyValue() {
		return yValue;
	}

	public void setyValue(double yValue) {
		this.yValue = yValue;
	}

	public double getxValue() {
		return xValue;
	}

	public void setxValue(double xValue) {
		this.xValue = xValue;
	}

	public DecimalFormat getDf() {
		return df;
	}

	public void setDf(DecimalFormat df) {
		this.df = df;
	}

	public String getPlotname() {		
		return plotname;
	}
	
	public String getUniqueID() {
		return uniqueID;
	}

	public void setUniqueID(String uniqueID) {
		this.uniqueID = uniqueID;
	}

	public String getPlotNameWithCoords() {
		String space = " ";
		String brace1 = "(";
		String brace2 = ")";
		StringBuilder str = new StringBuilder();
		str.append(plotname);
		str.append(space);
		Iterator<Point2D> iterator = this.origSpatialPoints.iterator();
        while(iterator.hasNext()){
          Point2D ptSpatial = ((Point2D) iterator.next());        
          str.append(brace1);
          str.append(getCoordOrdering().format(ptSpatial));
          str.append(brace2);
          str.append(space);
        }		
        return str.toString();
	}

	public void setPlotname(String plotname) {
		this.plotname = plotname;
	}

	public ProjObj getProj() {
		ProjObj varproj = new ProjObj.Projection_OC(originalproj.getCenterLon(), originalproj.getCenterLat());
		return varproj;
	}

	public void setProj(ProjObj proj) {
		this.originalproj = proj;
	}

	public void setOrigSpatialPoints(List<Point2D> spatialPts) {
		Iterator<Point2D> iterator = spatialPts.iterator();
        while(iterator.hasNext()) {
        	this.origSpatialPoints.add((Point2D) iterator.next());
        }		    
	}

	public int getPlotID() {
		return plotID;
	}

	public String getSourceTitle() {
		return sourceTitle;
	}

	public Color getPlotColor() {
		return plotColor;
	}

	public void setPlotColor(Color plotColor) {
		this.plotColor = plotColor;
	}

	public Color getProfileColorInMainView() {
		return profileColorInMainView;
	}

	public void setProfileColorInMainView(Color profileColorInMainView) {
		this.profileColorInMainView = profileColorInMainView;
	}

	public String getDistanceFormatted() {
		if (Double.isNaN(xValue))
			return NO_VALUE;
		String unit = "km";
		if (xValue < 1) {
			xValue = xValue * 1000;
			unit = "m";
		}
		return df.format(xValue) + " " + unit;
	}		
		
	public String getValueFormatted(double value) {		
		String val = Double.isNaN(((Number) value).doubleValue()) ? NO_VALUE : nf.format(value);
		return val;
	}

	public boolean isVisible() {
		return isVisible;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}

	public String getRename() {
		return rename;
	}

	public void setRename(String rename) {
		this.rename = rename;
	}

	public String getPlotunit() {
		return plotunit;
	}

	public void setPlotunit(String plotunit) {
		this.plotunit = plotunit;
	}

	public double getLatValue() {
		if (Double.isNaN(latValue))
			return Double.NaN;
		Point2D point = new Point2D.Double(lonValue, latValue);
		Point2D ptSpatial = this.originalproj.convWorldToSpatial(point);
	    return getCoordOrdering().formatLatitude(ptSpatial.getY());		
	}

	public void setLatValue(double latValue) {
		this.latValue = latValue;
	}

	public double getLonValue() {
		if (Double.isNaN(lonValue))
			return Double.NaN;
		Point2D point = new Point2D.Double(lonValue, latValue);
		Point2D ptSpatial = this.originalproj.convWorldToSpatial(point);
	    return getCoordOrdering().formatLongitude(ptSpatial.getX());
	}

	public void setLonValue(double lonValue) {
		this.lonValue = lonValue;
	}


	private Ordering getCoordOrdering() {
		String coordOrdering = Config.get(Config.CONFIG_LAT_LON,Ordering.LAT_LON.asString());
		Ordering ordering = Ordering.get(coordOrdering);
		return ordering;
	}

	public boolean isNumericSource() {
		return isNumericSource;
	}

	public void setIsNumericSource(boolean isNumericSource) {
		this.isNumericSource = isNumericSource;
	}	

}
