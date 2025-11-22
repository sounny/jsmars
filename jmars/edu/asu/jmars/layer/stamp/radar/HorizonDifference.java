package edu.asu.jmars.layer.stamp.radar;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class HorizonDifference {
	/**
	 * This horizon is used to calculate the difference from
	 * the horizon which owns this object.
	 */
	private RadarHorizon diffHorizon;
	
	/**
	 * This horizon is the owner of this object.  A difference
	 * is calculated between this horizon and the diffHorizon.
	 */
	private RadarHorizon ownerHorizon;
	
	/**
	 * The first x pixel value where both horizons overlap.
	 */
	private int xStart;
	/**
	 * The last x pixel value where both horizons overlap.
	 */
	private int xEnd;
	
	/**
	 * The difference between the y values of the overlapping x 
	 * region of the two horizons.
	 */
	private LinkedHashMap<Integer, Double> xToYDiff;
	
	/**
	 * Constant used to calculate depth between two surfaces based off
	 * an assumption of the composition between those surfaces.
	 */
	private double dielectricConstant;
	
	
	public HorizonDifference(RadarHorizon owner, RadarHorizon second, double dConstant){
		diffHorizon = second;
		ownerHorizon = owner;
		dielectricConstant = dConstant;
		
		calculateDifference();
	}
	
	private void calculateDifference(){
		try{
			ArrayList<PixelPoint> ownerPts = ownerHorizon.getPixelPoints();
			LinkedHashMap<Integer, Double> ownerXToYValue = new LinkedHashMap<Integer,Double>();
			xStart = 999999;
			xEnd = 0;
			//find the starting and ending x from the ownerHorizon
			//loop through to build a hashmap of y values and x valuse as the keys
			// for the owner horizon
			//if there are multiple x values in the difference then the value is NaN.
			for(int i=0; i<ownerPts.size(); i++){
				PixelPoint pt = ownerPts.get(i);
				int x = pt.getX();
				if(x<xStart){
					xStart = x;
				}
				if(x>xEnd){
					xEnd = x;
				}
				//if this x already appears in the map, then we need to reset
				// it to NaN
				if(ownerXToYValue.keySet().contains(x)){
					ownerXToYValue.put(x, Double.NaN);
				}else{
					ownerXToYValue.put(x, (double)pt.getY());
				}
			}
			
			ArrayList<PixelPoint> diffPts = diffHorizon.getPixelPoints();
			LinkedHashMap<Integer, Double> diffXToYValue = new LinkedHashMap<Integer, Double>();
			//create a hashmap of y values with x values as the keys for the difference
			// horizon.
			//if there are multiple x values in the difference then the value is NaN.
			for(int i=0; i<diffPts.size(); i++){
				PixelPoint pt = diffPts.get(i);
				int x = pt.getX();
				//if this x already appears in the map, then we need to reset
				// it to NaN
				if(diffXToYValue.keySet().contains(x)){
					diffXToYValue.put(x, Double.NaN);
				}else{
					diffXToYValue.put(x, (double)pt.getY());
				}
			}
			
			
			xToYDiff = new LinkedHashMap<Integer, Double>();
			//loop from the start x to end x, and find the difference of y values
			for(int i=xStart; i<=xEnd; i++){
				double yVal = ownerXToYValue.get(i);
				
				//if the second horizon has that same x, then find the difference
				if(diffXToYValue.keySet().contains(i)){
					double diffYVal = diffXToYValue.get(i);
					
					//if either of the values is NaN, make the difference NaN
					if(Double.isNaN(yVal) || Double.isNaN(diffYVal)){
						xToYDiff.put(i, Double.NaN);
					}
					else{
						double diff = yVal - diffYVal;
						//use negative diff because the user would expect the lower
						// the difference from top to bottom to be negative (which
						// is not the case with pixels in an image).
						xToYDiff.put(i, -diff);
					}
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * @return The difference horizon's id "-" the difference horizon's name.
	 */
	public String getHorizonDifferenceName(){
		return diffHorizon.getID()+" : "+diffHorizon.getName();
	}
	
	/**
	 * @return The {@link RadarHorizon} that is being subtracted from the owner.
	 */
	public RadarHorizon getSubtractedHorizon(){
		return diffHorizon;
	}
	
	/**
	 * @return A map of x pixel value to y pixel difference.  The y pixel 
	 * difference will be NaN, if that x is found at multiple locations (if
	 * the radar horizon crosses back over on itself).
	 */
	public LinkedHashMap<Integer, Double> getDifferenceMap(){
		return xToYDiff;
	}
	
	
	/**
	 * @return The mean value of all the y pixel differences.  Does not include
	 * any NaN values in this calculation.
	 * Returns null if none of the points are at the same x location.
	 */
	public Double getMeanDifference(){
		Double mean = null;
		
		double sum = 0;
		double total = xToYDiff.size();
		
		if(total>0){
			for(int key : xToYDiff.keySet()){
				double val = xToYDiff.get(key);
				if(!Double.isNaN(val)){
					sum += val;
				}
			}
						
			mean = sum/total;
		}
		return mean;
	}
	
	/**
	 * @return The dielectric constant specified by the user for this
	 *         horizon difference.
	 */
	public double getDielectricConstant(){
		return dielectricConstant;
	}
}
