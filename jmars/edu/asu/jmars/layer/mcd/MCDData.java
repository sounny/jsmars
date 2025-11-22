package edu.asu.jmars.layer.mcd;

import java.util.ArrayList;

class MCDData {
	String name = "";
	ArrayList<String> columnNames=new ArrayList<String>();
	ArrayList<ArrayList<Double>> columnVals;
	
	public MCDData(String name, ArrayList<String> columnNames, ArrayList<ArrayList<Double>> columnValues) {
		this.name = name;
		this.columnNames = columnNames;
		this.columnVals = columnValues;
	}
}