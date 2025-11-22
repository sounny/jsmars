package edu.asu.jmars.layer.landing;

import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Set;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.MultiProjection;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.Field;

public class LandingSite {

	final static Field UPPER_LEFT_LON = new Field("UL Lon", Double.class);
	final static Field UPPER_LEFT_LAT = new Field("UL Lat", Double.class);
	final static Field LOWER_RIGHT_LON = new Field("LR Lon", Double.class);
	final static Field LOWER_RIGHT_LAT = new Field("LR Lat", Double.class);
	final static Field LONGITUDE = new Field("Longitude", Double.class);
	final static Field LATITUDE = new Field("Latitude", Double.class);
	final static Field ANGLE = new Field("ANGLE", Double.class);
	final static Field HORIZ_AXIS = new Field("Hor. Axis", Double.class);
	final static Field VERT_AXIS = new Field("Vert. Axis", Double.class);
	final static Field USER = new Field("User", String.class);	
	final static Field COLOR = new Field("Color", Integer.class);
	final static Field COMMENT = new Field("Note", String.class);

	private String user = "";
	
	private Point2D upperLeft; //in spatial coords
	private Point2D lowerRight; //in spatial coords
	private double cenLon; //in spatial coords
	private double cenLat; //in spatial coords
	private double horAxis; //distance in meters
	private double verAxis; //distance in meters
	private ArrayList<Point2D> points; //defines outline of the ellipse in spatial coords
	private GeneralPath screenPath; //in screen coords
	private GeneralPath worldPath; //in world coords (used for box intersection because of meridian bug)
	private double theta = 0; //in radians

	private String comment = "";
	private int color = Color.BLACK.getRGB();
	
	private ArrayList<Stat> stats = new ArrayList<Stat>();
	public boolean dirty = false;

	public LandingSite(){
		super();
	}
	
	// Used when reloading from files
	public LandingSite(Feature feature) {
		super();
		Set<Field> fields = feature.getKeys();
		
		for (Field f : fields) {
			Object o = feature.getAttribute(f);			
			setAttribute(f, o);			
		}		
		
	}
	
	public LandingSite(Point2D ul, Point2D lr, Color myColor, ProjObj proj) {
		color = myColor.getRGB();
		upperLeft = ul; //in spatial coords
		lowerRight = lr; //in spatial coords
		calcCenter();
		calcPoints(theta);
	}
	
	//recalculates points array (boundary points defining ellipse) based off the inclination angle theta
	private void calcPoints(double angle){
		ProjObj proj = Main.PO;
		//set angle
		theta = angle;
		//work in world coords
		Point2D ulworld = proj.convSpatialToWorld(upperLeft);
		Point2D lrworld = proj.convSpatialToWorld(lowerRight);
		
		//Difference should always be negative.
		if(ulworld.getX()-lrworld.getX()>0){
			lrworld = new Point2D.Double(lrworld.getX()+360, lrworld.getY());
		}
		
		double x = ulworld.getX();
		double y = ulworld.getY();
		double width = Math.abs(ulworld.getX() - lrworld.getX());
		double height = Math.abs(ulworld.getY() - lrworld.getY());
		//compute boundary points for path outline
		points = new ArrayList<Point2D>();
		for(double t=0; t<2*Math.PI; t+= Math.PI/50){
			double a = width/2;
			double b = height/2;
			double x_diff = a*Math.cos(t)*Math.cos(theta)-b*Math.sin(t)*Math.sin(theta);
			double y_diff = a*Math.cos(t)*Math.sin(theta)+b*Math.sin(t)*Math.cos(theta);
			
			double x_new = x + width/2 + x_diff;
			double y_new = y - height/2 + y_diff;
			Point2D pt_new = new Point2D.Double(x_new, y_new);
			//store in spatial
			pt_new = proj.convWorldToSpatial(pt_new);
			points.add(pt_new);
		}
	}
	//recalculates the path shape to be displayed in the lview, based off current projection
	public GeneralPath calcScreenPath(MultiProjection mp){
		ArrayList<Point2D> pts = new ArrayList<Point2D>(points.size());
		for(Point2D pt: points){
//			System.out.println(pt.getX()+", "+pt.getY());
			pt = mp.spatial.toScreen(pt);
			pts.add(pt);
		}
		screenPath = new GeneralPath();
		for(Point2D pt : pts){
			if(screenPath.getCurrentPoint() == null){
				screenPath.moveTo(pt.getX(), pt.getY());
			}else{
				screenPath.lineTo(pt.getX(), pt.getY());
			}
		}
		return screenPath;
	}
	
	//recalculates the path shape to be used for calculating intersection with the selection box, based off the passed in projection
	public GeneralPath calcWorldPath(ProjObj po){
		ArrayList<Point2D> pts = new ArrayList<Point2D>(points.size());
		for(Point2D pt: points){
			pt = po.convSpatialToWorld(pt);
			if(pt.getX()>180){
				pt = new Point2D.Double(pt.getX()-360, pt.getY());
			}
			pts.add(pt);
		}
		worldPath = new GeneralPath();
		for(Point2D pt : pts){
			if(worldPath.getCurrentPoint() == null){
				worldPath.moveTo(pt.getX(), pt.getY());
			}else{
				worldPath.lineTo(pt.getX(), pt.getY());
			}
		}
		return worldPath;
	}
	
	private void calcCenter(){
		//center point values
		double cenX = (upperLeft.getX()+lowerRight.getX())/2;
		double cenY = (upperLeft.getY()+lowerRight.getY())/2;
		cenLon = 360-cenX; //change to degrees E
		cenLat = cenY;
		//TODO: Maybe also calculate the axis?
//		//vertical axis
//		Point2D upCen = new Point2D.Double(cenX, upperLeft.getY());
//		Point2D downCen = new Point2D.Double(cenX, lowerRight.getY());
//		verAxis = spatialProj.distance(upCen, downCen);
//		//horizontal axis
//		Point2D leftCen = new Point2D.Double(upperLeft.getX(), cenY);
//		Point2D rightCen = new Point2D.Double(lowerRight.getX(), cenX);
//		horAxis = spatialProj.distance(leftCen, rightCen);
	}
	
	public double getLon() {
		return cenLon;
	}
	public double getLat() {
		return cenLat;
	}
	/**
	 * Axis in METERS
	 */
	public double getHorizontalAxis() {
		return horAxis;
	}
	/**
	 * Axis in METERS
	 */
	public double getVerticalAxis(){
		return verAxis;
	}
	public String getUser() {
		return user;
	}
	public Color getColor() {
		return new Color(color);
	}
	public String getComment() {
		return comment;
	}
	public ArrayList<Stat> getStats(){
		return stats;
	}
	public GeneralPath getScreenPath(){
		return screenPath;
	}
	public GeneralPath getWorldPath(){
		return worldPath;
	}
	public Point2D getUpperLeft(){
		return upperLeft;
	}
	public Point2D getLowerRight(){
		return lowerRight;
	}
	/**
	 * @return  Inclination angle in radians
	 */
	public double getAngle(){
		return theta;
	}
	public ArrayList<Point2D> getPoints(){
		return points;
	}
	public Point2D[] getPointsArray(){
		Point2D[] pointArray = new Point2D[points.size()];
		for(int i=0; i<points.size(); i++){
			pointArray[i] = points.get(i);
		}
		return pointArray;
	}
	public Feature getFeature(LandingSiteSettings cs) {
		Feature f = new Feature();
		
		setPath(f);
		
		f.setAttribute(UPPER_LEFT_LON, upperLeft.getX());
		f.setAttribute(UPPER_LEFT_LAT, upperLeft.getY());
		f.setAttribute(LOWER_RIGHT_LON, lowerRight.getX());
		f.setAttribute(LOWER_RIGHT_LAT, lowerRight.getY());
		f.setAttribute(LONGITUDE, cenLon);
		f.setAttribute(LATITUDE, cenLat);
		f.setAttribute(ANGLE, theta);
		
		if (cs.inpExpHorAxis) {
			f.setAttribute(HORIZ_AXIS, horAxis);
		}
		if (cs.inpExpHorAxis) {
			f.setAttribute(VERT_AXIS, verAxis);
		}
		if (cs.inpExpColor) {		
			f.setAttribute(COLOR, color);
		}
		if (cs.inpExpNote) {		
			f.setAttribute(COMMENT, comment);
		}
		if (cs.inpExpUser) {		
			f.setAttribute(USER, user);
		}
		if (cs.inpExpStats){
			for(Stat s : stats){
				if(s.hasAvg){
					f.setAttribute(s.getAveField(), s.getAvg());
				}
				if(s.hasStd){
					f.setAttribute(s.getStdField(), s.getStd());
				}
				if(s.hasMax){
					f.setAttribute(s.getMaxField(), s.getMax());
				}
				if(s.hasMin){
					f.setAttribute(s.getMinField(), s.getMin());
				}
			}
		}
		return f;
	}
	
	
	
	public void setLon(double lon) {
		double horDiff = cenLon - lon;
		
		upperLeft = new Point2D.Double(upperLeft.getX()+horDiff, upperLeft.getY());
		lowerRight = new Point2D.Double(lowerRight.getX()+horDiff, lowerRight.getY());
		
		cenLon = lon;
		calcPoints(theta);
	}

	
	public void setLat(double lat) {
		double verDiff = cenLat - lat;
		
		upperLeft = new Point2D.Double(upperLeft.getX(), upperLeft.getY()-verDiff);
		lowerRight = new Point2D.Double(lowerRight.getX(), lowerRight.getY()-verDiff);
		
		cenLat = lat;
		calcPoints(theta);
	}
	public void setUpperLeft(Point2D pt){
		upperLeft = pt;
		calcCenter();
		calcPoints(theta);
	}
	public void setLowerRight(Point2D pt){
		lowerRight = pt;
		calcCenter();
		calcPoints(theta);
	}
	//TODO: create set axis methods that adjust the ul and lr points as well
	public void setHorizontalAxis(double ha){
		horAxis = ha;
	}
	public void setVerticalAxis(double va){
		verAxis = va;
	}
//	public void setLocation(Point2D pt) {
//		double horDiff = cenLon - pt.getX();
//		double verDiff = cenLat - pt.getY();
//		
//		upperLeft = new Point2D.Double(upperLeft.getX()+horDiff, upperLeft.getY()+verDiff);
//		lowerRight = new Point2D.Double(lowerRight.getX()+horDiff, lowerRight.getY()+verDiff);
//		
//		cenLon = pt.getX();
//		cenLat = pt.getY();
//		
//		calcPoints(theta);
//	}
	public void setUser(String newUser) {
		user = newUser;
	}
	public void setComment(String newComment) {
		comment = newComment;		
	}
	public void setColor(Color newColor) {
		color = newColor.getRGB();
	}
	public void setAngleInDeg(double deg){
		theta = deg*Math.PI/180;
		calcPoints(theta);
	}
	private void setPath(Feature f) {
		FPath path = new FPath(getPointsArray(), FPath.SPATIAL_WEST, true);
		f.setPath(path);
	}
	
	Double ul_lat = null, ul_lon = null, lr_lat = null, lr_lon = null;
	public void setAttribute(Field var, Object value) {

		if (var.name != null && var.name.equals(UPPER_LEFT_LON.name)){
			ul_lon = ((Double)value).doubleValue();
		} else if (var.name != null && var.name.equals(UPPER_LEFT_LAT.name)){
			ul_lat = ((Double)value).doubleValue();
		} else if (var.name != null && var.name.equals(LOWER_RIGHT_LON.name)){
			lr_lon = ((Double)value).doubleValue();
		} else if (var.name != null && var.name.equals(LOWER_RIGHT_LAT.name)){
			lr_lat = ((Double)value).doubleValue();
		} else if (var.name != null && var.name.equals(ANGLE.name)){
			theta = ((Double)value).doubleValue();
		} else if (var.name != null && var.name.equals(LONGITUDE.name)) {
			cenLon = ((Double)value).doubleValue();
		} else if (var.name != null && var.name.equals(LATITUDE.name)) {
			cenLat = ((Double)value).doubleValue();
		} else if (var.name != null && var.name.equals(HORIZ_AXIS.name)) {
			horAxis = ((Double)value).doubleValue();
		} else if (var.name !=null && var.name.equals(VERT_AXIS.name)) {
			verAxis = ((Double)value).doubleValue();
		} else if (var.name != null && var.name.equals(USER.name)) {
			user = (String)value;
		} else if (var.name != null && var.name.equals(COLOR.name)) {
			if (value instanceof Integer) {
				color = ((Integer)value).intValue();
			} else if (value instanceof Color) {
				color = ((Color) value).getRGB();
			}
		} else if (var.name != null && var.name.equals(COMMENT.name)) {
			comment = (String)value;
		}
		//Recalculate the points based off the upper left and lower right points and theta
		if(ul_lat!=null && ul_lon!=null && lr_lat!=null && lr_lon!=null){
			upperLeft = new Point2D.Double(ul_lon, ul_lat);
			lowerRight = new Point2D.Double(lr_lon, lr_lat);
			calcPoints(theta);
		}
	}
	 
}