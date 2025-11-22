package edu.asu.jmars.layer.landing;

import java.awt.geom.Point2D;
import java.text.DecimalFormat;

import edu.asu.jmars.layer.util.features.Field;

public class Stat {
	private String name;
	private String sName; //shorter name for esri exporting
	private double min;
	private double max;
	private double avg;
	private double std;
	
	boolean hasMin;
	boolean hasMax;
	boolean hasAvg;
	boolean hasStd;
	
	//location information
	Point2D upperLeft;
	Point2D lowerRight;
	double angle;

	/**
	 * There is one stat object per landing site for every stat calculator.
	 * This object includes booleans for whether the min, max, avg and stdev
	 * where calculated, and the values for those that exist.  Also has an
	 * abreviated name for ESRI exporting compatablility. 
	 * @param LongName		Name that will display in the LandingSite table
	 * @param ShortName		Shorter name that will export for ESRI column name compatability
	 */
	public Stat(String LongName, String ShortName){
		name = LongName;
		sName = ShortName;
	}
	
	public String getName(){
		return name;
	}
	public String getShortName(){
		return sName;
	}
	public double getAvg(){
		return avg;
	}
	public double getStd(){
		return std;
	}
	public double getMax(){
		return max;
	}
	public double getMin(){
		return min;
	}
	//These fields are how the stat object is exported with 
	// the landing site objects
	public Field getAveField(){
		return new Field("AVG "+sName, Double.class);
	}
	public Field getStdField(){
		return new Field("STD "+sName, Double.class);
	}
	public Field getMaxField(){
		return new Field("MAX "+sName, Double.class);
	}
	public Field getMinField(){
		return new Field("MIN "+sName, Double.class);
	}
	
	public void setName(String n){
		name = n;
	}
	public void setShortName(String n){
		sName = n;
	}
	public void setAvg(double a){
		avg = a;
	}
	public void setStd(double s){
		std = s;
	}
	public void setMax(double m){
		max = m;
	}
	public void setMin(double m){
		min = m;
	}
	/**
	 * A formatted output of the stats that exist (avg, min, max, std).
	 * --Uses html so the corresponding string cannot be easily concatenated
	 * with other strings.--
	 */
	public String toString(){
		DecimalFormat decimalFormat = new DecimalFormat("0.#####");
		String myString = "<html><P style=\"padding: 10 10 10 10\" ALIGN=Center>";
		if(hasAvg){
			if(Double.isNaN(avg)){
				myString += "&nbsp;&nbsp;<i>Avg:</i> No Data<br>";
			}else{
				myString += "&nbsp;&nbsp;<i>Avg:</i> "+decimalFormat.format(avg)+"<br>";
			}
		}
		if(hasMax){
			if(Double.isNaN(avg)){
				myString += "&nbsp;&nbsp;<i>Avg:</i> No Data<br>";
			}else{
				myString += "&nbsp;&nbsp;<i>Max:</i> "+decimalFormat.format(max)+"<br>";
			}
		}
		if(hasMin){
			if(Double.isNaN(avg)){
				myString += "&nbsp;&nbsp;<i>Avg:</i> No Data<br>";
			}else{
				myString += "&nbsp;&nbsp;<i>Min:</i> "+decimalFormat.format(min)+"<br>";
			}
		}
		if(hasStd){
			if(Double.isNaN(avg)){
				myString += "&nbsp;&nbsp;<i>Avg:</i> No Data<br>";
			}else{
				myString +="&nbsp;&nbsp;<i>StDev:</i> "+decimalFormat.format(std)+"<br>";
			}
		}
		myString += "</html>";
		
		return myString;
	}
}
