package edu.asu.jmars.layer.mcdslider;

import java.awt.Color;
import java.util.ArrayList;

import edu.asu.jmars.layer.SerializedParameters;

public class MCDLayerSettings implements SerializedParameters {

	public String name; // Layer name
	
	public String instrument;
	public String mapPath;
	public String versionString;
	
	public String displayType;
	public boolean blendMola=false;
	public ArrayList<Integer> sliderPositions = new ArrayList<Integer>();
	
	public Double stretchMin;
	public Double stretchMax;
	public int[] mapperVals;
	public Color[] mapperColors;
	
	public String animateType;
	public int timeDelay=1000;

	//used for debugging purposes
	public String toString(){
		return "name: "+name+", instrument: "+instrument+", display type: "+displayType+", animate type: "+animateType+", time delay: "+timeDelay+", slider positions: "+sliderPositions+", blended mola: "+blendMola;
	}
	
}
