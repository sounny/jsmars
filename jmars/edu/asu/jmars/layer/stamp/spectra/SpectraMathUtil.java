package edu.asu.jmars.layer.stamp.spectra;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JOptionPane;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.Util;

/**
 * This class was created to support the "Spectral Math Window" idea that
 * has been discussed with Vicky.  It is called from the jtable in the 
 * {@link SpectraView}.  All the functions were discussed with Vicky Hamilton
 * for the J-Asteroid visualization requirements and 
 * the intended functionality was implemented.
 */
public class SpectraMathUtil {

	/**
	 * Calculates the average of every y value of all the spectra passed in.
	 * 
	 * @param spectra List of spectra objects to be summed
	 * @return A spectraObject with y-values that are the average of each 
	 * individual y value of the spectra passed in.
	 */
	public static SpectraObject avgSpectra(ArrayList<SpectraObject> spectra){
		SpectraObject firstSpectra = spectra.get(0);
		int length = firstSpectra.getYValues().length;
		
		String desc = "Avg of (";
		double[] xVals = new double[length];
		double[] yVals = new double[length];
		
		for(int i=0; i<length; i++){
			double ySum = 0;
			for(SpectraObject so : spectra){
				ySum = ySum + so.getYValues()[i];
				//if this is the first time through, create the description
				// based off all the passed in spectra
				if(i==0){
					desc = desc+so.getDesc()+", ";
				}
			}
			yVals[i] = ySum/spectra.size();
			xVals[i] = firstSpectra.getXValues()[i];
		}
		
		//trim off the last comma and space from description
		desc = desc.substring(0, desc.length()-2)+")";
		String type = firstSpectra.getType();
		
		return new SpectraObject(desc, xVals, yVals, type, false, true);
	}
	
	/**
	 * Calculates the sum of every y value of all the spectra passed in.
	 * 
	 * @param spectra List of spectra objects to be summed
	 * @return A spectraObject with y-values that are the sum of each 
	 * individual y value of the spectra passed in.
	 */
	public static SpectraObject sumSpectra(ArrayList<SpectraObject> spectra){
		SpectraObject firstSpectra = spectra.get(0);
		int length = firstSpectra.getYValues().length;
		
		String desc = "Sum of (";
		double[] xVals = new double[length];
		double[] yVals = new double[length];
		
		for(int i=0; i<length; i++){
			double ySum = 0;
			for(SpectraObject so : spectra){
				ySum = ySum + so.getYValues()[i];
				//if this is the first time through, create the description
				// based off all the passed in spectra
				if(i==0){
					desc = desc+so.getDesc()+", ";
				}
			}
			yVals[i] = ySum;
			xVals[i] = firstSpectra.getXValues()[i];
		}
		
		//trim off the last comma and space from description
		desc = desc.substring(0, desc.length()-2)+")";
		String type = firstSpectra.getType();
		
		return new SpectraObject(desc, xVals, yVals, type, false, true);
	}
	
	/**
	 * Calculates the product of every y value of all the spectra passed in.
	 * 
	 * @param spectra List of spectra objects to be summed
	 * @return A spectraObject with y-values that are the product of each 
	 * individual y value of the spectra passed in.
	 */
	public static SpectraObject multiplySpectra(ArrayList<SpectraObject> spectra){
		SpectraObject firstSpectra = spectra.get(0);
		int length = firstSpectra.getYValues().length;
		
		String desc = "Product of (";
		double[] xVals = new double[length];
		double[] yVals = new double[length];
		
		for(int i=0; i<length; i++){
			double yProd = 1;
			for(SpectraObject so : spectra){
				yProd = yProd * so.getYValues()[i];
				//if this is the first time through, create the description
				// based off all the passed in spectra
				if(i==0){
					desc = desc+so.getDesc()+", ";
				}
			}
			yVals[i] = yProd;
			xVals[i] = firstSpectra.getXValues()[i];
		}
		
		//trim off the last comma and space from description
		desc = desc.substring(0, desc.length()-2)+")";
		String type = firstSpectra.getType();
		
		return new SpectraObject(desc, xVals, yVals, type, false, true);
	}
	
	/**
	 * Calculates the difference of every y value of the two spectra passed in.
	 * 
	 * @param first The minuend of the subtraction (the one being subtracted FROM)
	 * @param second The subtrahend of the subtraction (the one BEING subtracted)
	 * @return A spectraObject with y-values that are the difference of each 
	 * individual y value of the first spectra minus the second spectra.
	 */
	public static SpectraObject subtractSpectra(SpectraObject first, SpectraObject second){
		int length = first.getYValues().length;
		String desc = "Difference of ("+first.getDesc()+" - "+second.getDesc()+")";
		double[] xVals = new double[length];
		double[] yVals = new double[length];
		
		double[] y1 = first.getYValues();
		double[] y2 = second.getYValues();
		
		for(int i=0; i<length; i++){
			double diff = y1[i] - y2[i];
			
			yVals[i] = diff;
			xVals[i] = first.getXValues()[i];
		}
		
		String type = first.getType();
		
		return new SpectraObject(desc, xVals, yVals, type, false, true);
	}
	
	/**
	 * Calculates the 'quotient' of every y value of the two spectra passed in.
	 * Not quite the true quotient.  Any time the denominator or numerator is 0,
	 * then the quotient is set to 0.  This is to avoid having Double.INFINITY 
	 * as a value.  The spectra plot does not like plotting Double.INFINITY.
	 * 
	 * This behavior was agreed upon by Vicky Hamilton for J-Asteroid Vis Requirements
	 * 
	 * @param first The numerator of the division
	 * @param second The denominator of the divison
	 * @return A spectraObject with y-values that are the quotient (with the 
	 * exception of INFINITY is replaced with 0) of each individual y value
	 * of the first spectra divided the second spectra.
	 */
	public static SpectraObject divideSpectra(SpectraObject first, SpectraObject second){
		int length = first.getYValues().length;
		String desc = "Quotient of ("+first.getDesc()+" divided by "+second.getDesc()+")";
		double[] xVals = new double[length];
		double[] yVals = new double[length];

		double[] y1 = first.getYValues();
		double[] y2 = second.getYValues();
		
		for(int i=0; i<length; i++){
			double quo;
			if(y1[i] == 0 || y2[i] == 0){
				quo = 0;
			}else{
				quo = y1[i] / y2[i];
			}
			
			yVals[i] = quo;
			xVals[i] = first.getXValues()[i];
		}
		
		String type = first.getType();
		
		return new SpectraObject(desc, xVals, yVals, type, false, true);
	}
	
	/**
	 * Normalizes any passed in spectra based off a specified wavelength and value.
	 * If the wavelength does not match exactly, the closest x-value to that 
	 * wavelength is used for the index.  The corresponding y-value at that index 
	 * will be set to the new specified value, and a scale is calculated as the 
	 * quotient of the (specified value)/(original y value) and is used to 
	 * normalize the rest of the entire spectra.  New Y Value = (old y val)*scale
	 * If the old y-value at the corresponding index is equal to 0, then that record
	 * is NOT normalized (not returned in the array of SpectraObjects), and the user
	 * is notified of the record(s) that failed.
	 * 
	 * This behavior was agreed upon by Vicky Hamilton for J-Asteroid Vis Requirements
	 * 
	 * @param spectra  List of all spectra objects to be normalized
	 * @param wavelength  The wavelength to normalize at (x-value)
	 * @param value  The value to normalize to (y-value)
	 * @return  A list of the normalized SpectraObjects
	 */
	public static ArrayList<SpectraObject> pointNormalizeSpectra(ArrayList<SpectraObject> spectra, double wavelength, double value){
		ArrayList<SpectraObject> newSpectra = new ArrayList<SpectraObject>();
		SpectraObject firstSpectra = spectra.get(0);
		int length = firstSpectra.getYValues().length;
		
		//keep a list of names that failed because their yvalue was 0 to start with
		ArrayList<String> failures = new ArrayList<String>();
		
		int xIndex = -1;
		double xDiff = Double.MAX_VALUE;
		//find the closest index to the specified wavelength
		for(double x : firstSpectra.getXValues()){
			double diff = Math.abs(x-wavelength);
			
			//getting closer to the nearest actual x value
			if(diff<xDiff){
				xDiff = diff;
			}
			//moving away from it so end here
			else{
				break;
			}
			//keep track of the index
			xIndex++;
		}
		
		//normalize each spectra to the specified value for that wavelength
		for(SpectraObject sp : spectra){
			double[] newYVals = new double[length];
			double[] oldYVals = sp.getYValues();
			
			//if the y value is 0, this spectra cannot be normalized at 
			// this wavelength.  Keep track of it to tell the user at the end
			if(oldYVals[xIndex] == 0){
				failures.add(sp.getDesc());
			}
			else{
				double scale = value/oldYVals[xIndex];
				
				//find every new value by yval*scale = normalized value
				for(int i=0; i<length; i++){
					newYVals[i] = oldYVals[i]*scale;
				}
				
				String desc = "Pt Norm ["+wavelength+", "+value+"] of ("+sp.getDesc()+")";
				//create the new spectra object
				SpectraObject so = new SpectraObject(desc, sp.getXValues(), newYVals, sp.getType(), false, true);
				//add it to the list to be returned
				newSpectra.add(so);
			}
		}
		
		//if any spectra failed to normalize, tell the user
		if(failures.size()>0){
			String message = "The following records failed to normalize because the\n"
					+ "value at the wavelength "+wavelength+" was 0.\n"
					+ "Records must have a non-0 number to normalize.\n";
			for(String name : failures){
				message += name+"\n";
			}
			
			Util.showMessageDialog(message, "Point Normalization Failure", JOptionPane.ERROR_MESSAGE);
		}
		
		
		return newSpectra;
	}
	
	/**
	 * Normalizes any passed in spectra based off a specified x and y range.  Looks
	 * at all y values that fall within the x range, and uses those min and max y
	 * values to calculate the equation to stretch between the specified min and max
	 * y values.  All y values of each spectra will be normalized using these ranges
	 * 
	 * @param spectra List of all spectra objects to be normalized
	 * @param xMin The minimum x value of the defined X Range,
	 * which is used to determine the original Y Range
	 * @param xMax The maximum x value of the defined X Range,
	 *  which is used to determine the original Y range
	 * @param yMin The new Y minimum for values falling within the X Range
	 * @param yMax  The new Y maximum for values falling within the X Range
	 * @return A list of the normalized SpectraObjects
	 */
	public static ArrayList<SpectraObject> rangeNormalizeSpectra(ArrayList<SpectraObject> spectra, double xMin, double xMax, double yMin, double yMax){
		ArrayList<SpectraObject> newSpectra = new ArrayList<SpectraObject>();
		SpectraObject firstSpectra = spectra.get(0);
		int length = firstSpectra.getYValues().length;
		
		//find the indices for the x min and max
		int minIndex = -1;
		int maxIndex = -1;
		double minDiff = Double.MAX_VALUE;
		double maxDiff = Double.MAX_VALUE;
		for(double x : firstSpectra.getXValues()){
			//check the min index
			double diff1 = Math.abs(xMin - x);
			if(diff1<minDiff){
				minDiff = diff1;
				minIndex++;
			}
			//check the max index
			double diff2 = Math.abs(xMax - x);
			if(diff2<maxDiff){
				maxDiff = diff2;
				maxIndex++;
			}
			
			//if both new differences are greater than the
			// previous difference, we are past the range minimum 
			// and maximum, so stop this loop.
			if(diff1>minDiff && diff2>maxDiff){
				break;
			}
		}
		
		//normalize each spectra based on the x and y ranges specified
		// use this equation to find the new values:
		// newValue = newMin + slope * (oldValue - oldMin)
		// where slope = (newMax - newMin) / (oldMax - oldMin)
		for(SpectraObject sp : spectra){
			//find the actual min and max y values for the given range
			double[] yRange = Arrays.copyOfRange(sp.getYValues(), minIndex, maxIndex+1);
			Arrays.sort(yRange);
			
			double oldYMin = yRange[0];
			double oldYMax = yRange[yRange.length-1];
			
			//calculate the slope of the equation used for converting
			// from the old range to the new range where 
			double slope = (yMax - yMin)/(oldYMax - oldYMin);
			
			double[] oldYVals = sp.getYValues();
			double[] newYVals = new double[length];
			//calculate new y value for every value in the spectra
			for(int i=0; i<length; i++){
				newYVals[i] = yMin + slope * (oldYVals[i] - oldYMin);
			}
			
			String desc = "Range Norm ("+xMin+" to "+xMax+", "+yMin+" to "+yMax+") of ("+sp.getDesc()+")";
			//create the new spectra object
			SpectraObject so = new SpectraObject(desc, sp.getXValues(), newYVals, sp.getType(), false, true);
			//add it to the list to be returned
			newSpectra.add(so);
		}
		
		return newSpectra;
	}
}
