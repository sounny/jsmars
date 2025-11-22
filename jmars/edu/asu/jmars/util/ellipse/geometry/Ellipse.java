package edu.asu.jmars.util.ellipse.geometry;

import java.awt.geom.Point2D;

public class Ellipse {
	private double centerLon;
	private double centerLat;
	private double aLength;
	private double bLength;
	private double angle;
	
	/**
	 * Specifically used from the {@link FitEllipse} code
	 * @param dim dim array contains cenLon, cenLat, a semi-axis length, b semi-axis length, angle
	 */
	public Ellipse(double[] dim){
		this(dim[0], dim[1], dim[2]*2, dim[3]*2, dim[4]);
	}
	
	public Ellipse(double cenLon, double cenLat, double aLength, double bLength, double rotAngle){
		centerLat = cenLat;
		centerLon = cenLon;
		this.aLength = aLength;
		this.bLength = bLength;
		angle = rotAngle;
	}
	
	/**
	 * @return The center lat (or y component)
	 */
	public double getCenterLat(){
		return centerLat;
	}
	
	/**
	 * @return The center lon (or x component)
	 */
	public double getCenterLon(){
		return centerLon;
	}
	
	/**
	 * @return The center point of the ellipse
	 */
	public Point2D getCenterPt(){
		return new Point2D.Double(centerLon, centerLat);
	}
	
	/**
	 * @return The length of the A axis
	 */
	public double getALength(){
		return aLength;
	}
	
	/**
	 * @return The length of the B axis
	 */
	public double getBLength(){
		return bLength;
	}
	
	/**
	 * @return The angle of rotation
	 */
	public double getRotationAngle(){
		return angle;
	}
	
	public String toString(){
		return "Ellipse - Center:("+centerLon+", "+centerLat+"), aLength: "+aLength+" bLength: "+bLength+" Rotation: "+Math.toDegrees(angle);
	}
}
