package edu.asu.jmars.layer;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class InvestigateData implements Serializable{
	public String name;
	public ArrayList<String> style;
	
	public ArrayList<String> keys;
	public ArrayList<String> vals;
	public ArrayList<Double> numVals;
	public ArrayList<String> numKeys;
	public ArrayList<String> numUnits;
	public ArrayList<String> units;
	public ArrayList<Boolean> isNumeric;
	
	private DecimalFormat df = new java.text.DecimalFormat("###,###.############");
	//default number of significant digits
	int sigFigs = 3;
	
	public InvestigateData(String N){
		name = N;
		keys = new ArrayList<String>();
		vals = new ArrayList<String>();
		numVals = new ArrayList<Double>();
		numKeys = new ArrayList<String>();
		numUnits = new ArrayList<String>();
		style = new ArrayList<String>();
		units = new ArrayList<String>();
		isNumeric = new ArrayList<Boolean>();
		
		style.add("Bold");
	}
	
	public void add(String key, String value){
		keys.add(key);
		vals.add(value);
		units.add("");
		isNumeric.add(false);
		style.add("ItalicSmall");
		style.add("Small");
	}
	
	public void add(String key, String value, String unit, boolean num){
		keys.add(key);
		if(num){
			numVals.add(Double.parseDouble(value));
			numKeys.add(key);
			numUnits.add(unit);
			//format the value for display
			vals.add(formatNumberString(value));
		}else{
			vals.add(value);
		}
		if(unit == null) unit ="";
		units.add(unit);
		isNumeric.add(num);
		style.add("ItalicSmall");
		style.add("Small");		
	}
	
	public void add(String key, String value, String unit, String kStyle, String vStyle, boolean num){
		keys.add(key);
		if(num)	{
			numVals.add(Double.parseDouble(value));
			numKeys.add(key);
			numUnits.add(unit);
			//format the value for display
			vals.add(formatNumberString(value));
		}else{
			vals.add(value);
		}
		if (unit == null) unit = "";
		units.add(unit);
		isNumeric.add(num);
		style.add(kStyle);
		style.add(vStyle);
	}
	
	private String formatNumberString(String numberStr){
		String result = "";
		
		//check for NaN
		if(numberStr.equalsIgnoreCase("NAN")){
			result = "NaN";
		}
		else{
			//convert the number string to a double value
			double dubVal = Double.parseDouble(numberStr);
			//find the absolute val
			double absVal = Math.abs(dubVal);
			
			//if the absolute value is 1000 or larger, change
			// the number of sigfigs based on the power of 10
			if(absVal > 999){
				sigFigs = (int) Math.log(absVal);
			}
			
			//reduce the number based on significant figures
			result = String.format("%."+sigFigs+"G", dubVal);
			//remove all trailing zeroes
			//this line of code was found online (https://stackoverflow.com/questions/14984664/remove-trailing-zero-in-java)
			result = result.contains(".") ? result.replaceAll("0*$","").replaceAll("\\.$","") : result;
			
			//use the decimal formatter to add commas, to make 
			// large numbers easier to read
			result = df.format(Double.parseDouble(result));
		}
		
		return result;
	}
	
	public ArrayList<String> getKeys(){
		return keys;
	}
	
	public ArrayList<String> getValues(){
		return vals;
	}
	
	public Double getNumValue(int index){
		return numVals.get(index);
	}
	
	public String getNumKey(int index){
		return numKeys.get(index);
	}
	
	public String getNumUnit(int index){
		return numUnits.get(index);
	}
	
	public ArrayList<String> getStyles(){
		return style;
	}
	
	public ArrayList<String> getUnits(){
		return units;
	}
	
	public ArrayList<Boolean> getNumerics(){
		return isNumeric;
	}
	//return true if at least one value is numeric
	public boolean hasNumerics(){
		for(int i=0; i<getValSize(); i++){
			if(isNumeric.get(i)){
				return true;
			}
		}
		return false;
	}
	
	public String getCSVDump() {
		String result = "";
				
		for (int i=0; i<keys.size(); i++){
			String s = "\""+keys.get(i)+"\"";
			result += s+",";
			String s2 = "\""+vals.get(i)+"\"";
			result += s2;
			//don't need a new line at the end of dump
			if(i!=keys.size()-1)	result+="\n";
		}
		
		return result;
	}
	
	public int getValSize(){
		if(vals.size()!=keys.size()){
			return 0;
		}else
			return vals.size();
	}
	
	public int getNumValSize(){
		return numVals.size();
	}
	
}
