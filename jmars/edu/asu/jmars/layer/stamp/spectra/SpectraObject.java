package edu.asu.jmars.layer.stamp.spectra;

import java.awt.Color;
import java.awt.geom.Point2D;

import edu.asu.jmars.layer.stamp.StampShape;

public class SpectraObject {

	private String name;
	private String desc;
	private double[] xValues;
	private double[] yValues;
	/** This variable is used to keep the datasets together,
	 * eg. I_over_F, radiance, etc.
	 */
	private String type;
	
	private boolean isMarkedStamp = false;
	private boolean isComputedSpectra = false;
	
	// Additional values for SpectraPerPixel data (eg. CRISM Spectra)
	public Point2D lineSamplePoint;
	public 	int xpad;
	public 	int ypad;
	public StampShape myStamp;
	// End additional values
	
	public SpectraObject(String desc, double[] x, double[] y, String type, boolean marked, boolean computed){
		this.name = desc;
		this.desc = desc;
		this.type = type;
		xValues = x;
		yValues = y;
		isMarkedStamp = marked;
		isComputedSpectra = computed;
	}

	public SpectraObject(String newName, double[] x, double[] y, String type, Point2D lineSample, int xpad, int ypad, StampShape stamp){
		this.name = newName;
		this.desc = name;
		this.type = type;
		xValues = x;
		yValues = y;
		
		// Hardcoded values for this type of spectra
		isMarkedStamp = true;
		isComputedSpectra = false;
		
		lineSamplePoint = lineSample;
		this.xpad = xpad;
		this.ypad = ypad;
		
		myStamp = stamp;		
	}

	public String getName(){
		return name;
	}
	
	public String getDesc(){
		return desc;
	}
	
	public double[] getXValues(){
		return xValues;
	}
	
	public double[] getYValues(){
		return yValues;
	}
	
	public String getType(){
		return type;
	}
	
	public boolean isMarked(){
		return isMarkedStamp;
	}
	
	public boolean isComputed(){
		return isComputedSpectra;
	}
	
	public void setName(String newName){
		name = newName;
	}
	
	// Below columns are used for Spectra per Pixel layers
	public String getLine() {
		if (lineSamplePoint==null) {
			return "N/A";
		} else {
			return ""+lineSamplePoint.getX();
		}
	}
	
	public String getSample() {
		if (lineSamplePoint==null) {
			return "N/A";
		} else {
			return ""+lineSamplePoint.getY();
		}
	}
	
	public String getXPad() {
		if (xpad==0) {
			return "N/A"; 
		} else {
			return ""+xpad;
		}
	}

	public String getYPad() {
		if (ypad==0) {
			return "N/A"; 
		} else {
			return ""+ypad;
		}
	}
	
	private Color myColor = Color.DARK_GRAY;
	
	public void setColor(Color newColor) {
		myColor = newColor;
	}
	
	public Color getColor() {
		return myColor;
	}

}
