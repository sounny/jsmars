package edu.asu.jmars.layer.crater.profiler;

public class ProfileData {
	
	private String name;
	private double[] xValues;
	private double[] yValues;
	
	
	public ProfileData(String desc, double[] x, double[] y){
		this.name = desc;
		xValues = x;
		yValues = y;
	}
	

	public String getName(){
		return name;
	}
	
	public double[] getXValues(){
		return xValues;
	}
	
	public double[] getYValues(){
		return yValues;
	}
}
