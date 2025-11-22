package edu.asu.jmars.layer.stamp.radar;

import java.awt.geom.Point2D;

public class PixelPoint {
	private int x;
	private int y;
	private Point2D spatialPt; //longitude in degrees W
	private double value;
	
	public PixelPoint(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
	
	public void setSpatialPoint(Point2D pt){
		spatialPt = pt;
	}
	
	public double getLat(){
		return spatialPt.getY();
	}
	
	public double getLon(){
		return spatialPt.getX();
	}
	
	public void setValue(double val){
		value = val;
	}
	
	public double getValue(){
		return value;
	}
}
