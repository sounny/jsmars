package edu.asu.jmars.layer.investigate;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;

import org.jfree.chart.JFreeChart;

import edu.asu.jmars.layer.InvestigateData;

public class DataSpike implements Serializable{
	public static final String CIRCLE_STYLE = "Circle";
	public static final String SQUARE_STYLE = "Square";

	private JFreeChart myChart;
	private Point2D myPoint; //in W degree spatial coordinates
	private ArrayList<InvestigateData> myInvData;
	
	private String shapeStyle;
	private Color fillColor;
	private Color outlineColor;
	private Color labelColor;
	private String name;
	private boolean showLabel = true;
	private boolean showMarker = true;
	private int labelSize;
	
	public DataSpike(JFreeChart chart, Point2D point, ArrayList<InvestigateData>id, String n){
		
		myChart = chart;
		myPoint = point;
		myInvData = new ArrayList(id);
		
		name = n;
		
		//Set the colors to default (green fill, black outline, white label)
		fillColor = Color.GREEN;
		outlineColor = Color.BLACK;
		labelColor = Color.WHITE;
//		//Set the default shape to a circle
		shapeStyle = CIRCLE_STYLE;
		labelSize = 15;
	}
	
	
	public void setFillColor(Color c){
		fillColor = c;
	}
	public void setOutlineColor(Color c){
		outlineColor = c;
	}
	public void setLabelColor(Color c){
		labelColor = c;
	}
	public void setLabelSize(int ls){
		labelSize = ls;
	}
	public void setLabel(boolean b){
		showLabel = b;
	}
	public void setName(String n){
		name = n;
	}
	public void setShapeStyle(String s){
		shapeStyle = s;
	}
	public void setMarkerShow(boolean b){
		showMarker = b;
	}
	
	
	public String getName(){
		return name;
	}
	public Point2D getPoint(){
		return myPoint;
	}
	public JFreeChart getChart(){
		return myChart;
	}
	public ArrayList<InvestigateData> getInvData(){
		return myInvData;
	}
	public String getShapeStyle(){
		return shapeStyle;
	}
	public Color getFillColor(){
		return fillColor;
	}
	public Color getOutlineColor(){
		return outlineColor;
	}
	public Color getLabelColor(){
		return labelColor;
	}
	public boolean isLabelOn(){
		return showLabel;
	}
	public int getLabelSize(){
		return labelSize;
	}
	public boolean isMarkerOn(){
		return showMarker;
	}
}
