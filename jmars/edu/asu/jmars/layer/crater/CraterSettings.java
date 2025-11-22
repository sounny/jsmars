/**
 * 
 */
package edu.asu.jmars.layer.crater;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.util.Util;


class CraterSettings implements SerializedParameters {
	private static final long serialVersionUID = -118830083811384603L;
	Color nextColor = Color.black;
	int alpha=150;
	
	//The following variables are transient but should not be removed until we are sure that pre 3.0.4 sessions are gone and no longer contain these variables.
	//The problem was created when these variables were changed from ints to doubles. When sessions were loaded that contained these values as ints, the object
	//could not be de-serialized. Transient allows the object to ignore these values and use the new variables for the crater settings.
	transient int nextSize = -1;//this value may exist in old sessions
	transient int defaultSize = -1;//this value may exist in old sessions
	transient int meterIncrement = -1;//this value may exist in old sessions
	

	/**
	 * Constants to allow the initial crater size and increment to be
	 * somewhat scaled to the size of the body.
	 */
	final double TINY_ASTEROIDS             = 1;    // 1 km radius and below
	final double TINY_ASTEROIDS_CRATER_SIZE = 100;
	final double TINY_ASTEROIDS_INCREMENT   = 1;
	
	final double SMALL_BODIES             = 100;    // 100 km radius and below
	final double SMALL_BODIES_CRATER_SIZE = 1000;
	final double SMALL_BODIES_INCREMENT   = 10;
	
	final double SMALL_MOONS             = 300;     // 300 km radius and below
	final double SMALL_MOONS_CRATER_SIZE = 10000;
	final double SMALL_MOONS_INCREMENT   = 100;
	
	final double PLANETS_CRATER_SIZE = 100000;
	final double PLANETS_INCREMENT   = 1000;
	

	
	//These new variables replace the transient values
    double nextCraterSize = computeCraterSize();
	double defaultCraterSize = -1;            // in meters
	double craterMeterIncrement = computeCraterIncrement(nextCraterSize);
	
	String name = "Crater Counting";
	
	double craterLineThickness = 2.0;
	HashMap<Color, String> colorToNotesMap = new HashMap<Color, String>();
	transient ArrayList<Crater> craters = new ArrayList<Crater>();
		
	String minLat="";
	String maxLat="";
	String minLon="";
	String maxLon="";
	String minDia="";
	String maxDia="";
	
	// These are used to track whether changing the filter updates the various
	// data views or just the count.
	boolean filterMainView=false;
	boolean filterPanView=false;
	boolean filterTableView=false;
	boolean filter3dView=false;
	boolean filterVisibleDiameter = true;
	boolean filterCraterFill = true;
	boolean toggleDefaultCraterSizeReset=false;
	boolean filterTableCenterSelectedCrater = false;
	
	final boolean inpExpLatitude = true;
	final boolean inpExpLongitude = true;
	final boolean inpExpDiameter = true;
	boolean inpExpColor = true;
	boolean inpExpNote = true;
	boolean inpExpUser = true;
	
	//used for repopulating the info panel from a session reload
	LayerParameters myLP = null;
	
	public double getNextSize() {
		return this.nextCraterSize;
	}
	public double getDefaultSize() {
		if (this.defaultCraterSize == 0.0) {
			this.defaultCraterSize = -1;
		}
		return this.defaultCraterSize;
	}
	public double getMeterIncrement() {
		if (this.craterMeterIncrement == 0.0) {
			this.craterMeterIncrement = 1000;
		}
		return this.craterMeterIncrement;
	}
	
	
	public void setNextSize(double size) {
		this.nextCraterSize = size;
	}
	public void setDefaultSize(double defaultSize) {
		this.defaultCraterSize = defaultSize;
	}
	public void setMeterIncrement(double increment) {
		this.craterMeterIncrement = increment;
	}

	
	
	private double computeCraterSize() {
		 
		double size;   // nextCraterSize in meters
		
		if (Util.MEAN_RADIUS < TINY_ASTEROIDS) {
			size = TINY_ASTEROIDS_CRATER_SIZE; 
		} else if (Util.MEAN_RADIUS < SMALL_BODIES) {
			size = SMALL_BODIES_CRATER_SIZE;
		} else if (Util.MEAN_RADIUS < SMALL_MOONS) {
			size = SMALL_MOONS_CRATER_SIZE;
		} else {   // PLANETS
			size = PLANETS_CRATER_SIZE;
		}
	    return size;
	}
	
	private double computeCraterIncrement(double craterSize) {
		
		double increment;   // crater increment in meters
		
		if (Util.MEAN_RADIUS < TINY_ASTEROIDS) {
			increment = TINY_ASTEROIDS_INCREMENT; 
		} else if (Util.MEAN_RADIUS < SMALL_BODIES) {
			increment = SMALL_BODIES_INCREMENT;
		} else if (Util.MEAN_RADIUS < SMALL_MOONS) {
			increment = SMALL_MOONS_INCREMENT;
		} else {   // PLANETS
			increment = PLANETS_INCREMENT;
		}
		return increment;
	}

}