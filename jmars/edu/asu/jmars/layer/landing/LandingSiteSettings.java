package edu.asu.jmars.layer.landing;

import java.awt.Color;
import java.util.ArrayList;

import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;

public class LandingSiteSettings implements SerializedParameters {
	private static final long serialVersionUID = -118830083811384603L;
	
	Color nextColor = Color.black;
	int alpha=150;
	int nextHorSize; //in meters
	int nextVerSize; //in meters
	//preset ellipse sizes
	int axisIndex;
	
	String name = "Landing Site";
	double siteLineThickness = 2.0;
	//site line styles
	int styleIndex = 1;
	
	
	public transient ArrayList<LandingSite> sites = new ArrayList<LandingSite>(); //list of sites that have been added to lview
	
	public String configEntry;
	public String layerName;
	

	boolean filterVisibleDiameter = true;
	boolean filterSiteFill = true;
	
	
	boolean inpExpLatitude = true;
	boolean inpExpLongitude = true;
	boolean inpExpHorAxis = true;
	boolean inpExpVerAxis = true;
	boolean inpExpColor = true;
	boolean inpExpNote = true;
	boolean inpExpUser = true;
	boolean inpExpStats = true;
	
	
	LayerParameters myLP = null;
	
	
	//not actually used anymore but is kept to allow backwards compatibility with old saved layers
	public ArrayList<StatCalculator> statCalcs = new ArrayList<StatCalculator>();
}