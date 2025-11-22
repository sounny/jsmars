package edu.asu.jmars.layer.investigate;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;

import org.jfree.chart.JFreeChart;

public class DataProfile implements Serializable{

	private JFreeChart myChart;
	private ArrayList<Point2D> myPoints;
	
	private Color color; 
	private String name;
	
	
	public DataProfile(JFreeChart chart, ArrayList<Point2D> points, String n){
		myChart = chart;
		myPoints = points;
		name = n;
		//Default
		color = Color.GREEN;
	}
	
	
	public void setColor(Color c){
		color = c;
	}
	
	public void setName(String n){
		name = n;
	}
	
	//TODO: don't know which of these will actually get used
	public String getName(){
		return name;
	}
	public ArrayList<Point2D> getPoints(){
		return myPoints;
	}
	public JFreeChart getChart(){
		return myChart;
	}
}
