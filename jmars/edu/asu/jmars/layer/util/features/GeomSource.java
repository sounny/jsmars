package edu.asu.jmars.layer.util.features;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.layer.MultiProjection;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.layer.util.features.GeomSource.EllipseData;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.ellipse.geometry.Ellipse;

/**
 * Converts the given path into a circular outer path if the path was a point
 * a means of determining the kilometer radius has been set.
 */
public class GeomSource implements StyleSource<FPath> {
	private static final long serialVersionUID = 1L;
	
	//Circle variables
	/** The source of radius info, or null if circles should not be expanded. */
	private Field radiusField;
	/** The interpretation of the radius field value */
	private Units units;
	/** Creates new uninitialized style source */
	
	//Ellipse variables
	/** The source of a (is x-axis when roation is 0) axis. */
	private Field aAxisField;
	/** The source of b (is y-axis when roation is 0) axis. */
	private Field bAxisField;
	/** The source of angle. */
	private Field angleField;
	/** The latitude of the center point in degrees N */
	private Field centerLatField;
	/** The longitude of the center point in degrees E */
	private Field centerLonField;
	
	private Field meanAxisField;
	
	/** The interpretation of the axes field values */
	private LengthUnits lengthUnits;
	/** The interpretation of the angle field value */
	private AngleUnits angleUnits;
	
	
	
	/** Number of vertices used to define an ellipse,
	 * it is important to keep this number a multiple 
	 * of four, so there will always be a point at the
	 * perpendicular intersection of both axes from 
	 * the center point.
	 */
	private final static int vertices = 36;
	
	
	public GeomSource(Field radiusField, Units units, Field aAxisField, Field bAxisField, Field angleField, LengthUnits lUnits, AngleUnits aUnits, 
			Field cenLat, Field cenLon, Field mAxisField){
		this.radiusField = radiusField;
		this.units = units;
		this.aAxisField = aAxisField;
		this.bAxisField = bAxisField;
		this.angleField = angleField;
		this.lengthUnits = lUnits;
		this.angleUnits = aUnits;
		this.meanAxisField = mAxisField;
		centerLatField = cenLat;
		centerLonField = cenLon;
	}
	
	
	public void setRadiusField(Field radiusField) {
		this.radiusField = radiusField;
	}

	public void setUnits(Units units) {
		this.units = units;
	}

	public void setAAxisField(Field aAxisField) {
		this.aAxisField = aAxisField;
	}

	public void setBAxisField(Field bAxisField) {
		this.bAxisField = bAxisField;
	}

	public void setAngleField(Field angleField) {
		this.angleField = angleField;
	}

	public void setCenterLatField(Field centerLatField) {
		this.centerLatField = centerLatField;
	}

	public void setCenterLonField(Field centerLonField) {
		this.centerLonField = centerLonField;
	}

	public void setLengthUnits(LengthUnits lengthUnits) {
		this.lengthUnits = lengthUnits;
	}

	public void setAngleUnits(AngleUnits angleUnits) {
		this.angleUnits = angleUnits;
	}
	
	/** @return the field the user chose to store radius values. */
	public Field getRadiusField() {
		return radiusField;
	}
	
	/** @return the units of the field the user chose to store radius values. */
	public Units getUnits() {
		return units;
	}
	
	/** @return the field the user chose to store a axis values. */
	public Field getAAxisField() {
		return aAxisField;
	}
	
	/** @return the field the user chose to store b axis values. */
	public Field getBAxisField() {
		return bAxisField;
	}
	
	/** @return the field the user chose to store angle values. */
	public Field getAngleField() {
		return angleField;
	}
	
	/** @return the units of the field the user chose to store axes values. */
	public LengthUnits getAxesUnits() {
		return lengthUnits;
	}
	
	/** @return the units of the field the user chose to store angle value. */
	public AngleUnits getAngleUnits() {
		return angleUnits;
	}
	
	/** @return the field the user chose to store the center latitude */
	public Field getLatField(){
		return centerLatField;
	}
	
	/** @return the field the user chose to store the center longitude */
	public Field getLonField(){
		return centerLonField;
	}
	
	/** @return the field the user chose to store the center longitude */
	public Field getMeanAxisField(){
		return meanAxisField;
	}
	public void setMeanAxisField(Field mAxisField) {
		meanAxisField = mAxisField;
	}
	
	/**
	 * @returns either Field.FIELD_PATH if no radius field has been defined, or
	 *          Field.FIELD_PATH, the radius field, A axis field, B axis field,
	 *          angle field, center longitude field, and center latitude field 
	 *          chosen by the user.
	 */
	public Set<Field> getFields() {
		//build the set of fields that have already been set
		HashSet<Field> set = new HashSet<Field>();
		set.add(Field.FIELD_PATH);
		if(radiusField != null){
			set.add(radiusField);
		}
		if(aAxisField != null){
			set.add(aAxisField);
		}
		if(bAxisField != null){
			set.add(bAxisField);
		}
		if(angleField != null){
			set.add(angleField);
		}
		if(centerLonField != null){
			set.add(centerLonField);
		}
		if(centerLatField != null){
			set.add(centerLatField);
		}
		if(meanAxisField != null){
			set.add(meanAxisField);
		}
		
		return set;
	}
	
	/**
	 * @returns either Field.FIELD_PATH if no radius field has been defined, or
	 *          Field.FIELD_PATH and the radius field the user chose.
	 */
	public Set<Field> getCircleFields() {
		return radiusField == null ?
			Collections.singleton(Field.FIELD_PATH) :
			new HashSet<Field>(Arrays.asList(Field.FIELD_PATH, radiusField));
	}
	
	/**
	 * @returns either Field.FIELD_PATH if no radius field has been defined, or
	 *          Field.FIELD_PATH and the a axis, b axis, angle, center lat, center 
	 *          lon fields the user chose.
	 */
	public Set<Field> get5ptEllipseFields() {
		if(aAxisField == null || bAxisField == null || angleField == null){
			return Collections.singleton(Field.FIELD_PATH);
		}else{
			return new HashSet<Field>(Arrays.asList(Field.FIELD_PATH, aAxisField, bAxisField, angleField, centerLonField, centerLatField));
		}
	}
	
	
	public FPath getValue(Feature f) {
		if (f == null) {
			return null;
		}
		FPath path = f.getPath();
		
		//circles
		if (path.getType() == FPath.TYPE_POINT && f.getAttribute(radiusField) != null) {
			Number radius = (Number)f.attributes.get(radiusField);
			if (radius != null) {
				double dblRadius = radius.doubleValue();
				if (dblRadius != 0) {
					path = getCirclePath(path, dblRadius * units.getScale(), 36);
				}
			}
		}
		
		//ellipses
		else if(FeatureUtil.isEllipseSource(this, f)){
			//if the coords length is two that means it's the center point,
			// not the entire spatial path, so calculate the path (like loading
			// from a shape file)

//			Double cenLon;
//			Double cenLat;
//			
//			if(path.getCoords(false).length == 2){
//				System.out.println("calculating ellipse path");
//				//get the center point off the path
//				cenLon = path.getCoords(false)[0];
//				cenLat = path.getCoords(false)[1];
//			}else{
//				cenLon = (Double)f.getAttribute(centerLonField);
//				cenLat = (Double)f.getAttribute(centerLatField);
//			}
//			
//			Double aAxis = (Double)f.attributes.get(aAxisField);
//			Double bAxis = (Double)f.attributes.get(bAxisField);
//			Double angle = (Double)f.attributes.get(angleField);
//			
//			if(cenLat!=null && cenLon!=null && aAxis!=null && bAxis!=null && angle!=null){
//				//pass in degrees W for calculation
//				path = getEllipticalPath(new Point2D.Double(360-cenLon, cenLat), scaleToKm(aAxis), scaleToKm(bAxis), angle*angleUnits.getScale());
//				//set the proper path, and populate the lat and lon fields for the ellipse
//				f.setAttributeQuiet(Field.FIELD_PATH, path);
//				f.setAttributeQuiet(centerLatField, cenLat);
//				//displaying degrees E so leave the same
//				f.setAttributeQuiet(centerLonField, cenLon);
//			}
		
			//Define the ellipse used for this feature
			double cenLon = (double)f.getAttribute(centerLonField);
			double cenLat = (double)f.getAttribute(centerLatField);
			double aLength = (double)f.getAttribute(aAxisField);
			double bLength = (double)f.getAttribute(bAxisField);
			double rotAngle = (double)f.getAttribute(angleField);
			Ellipse e = new Ellipse(cenLon, cenLat, aLength, bLength, rotAngle);
			
			//if nothing has changed just return the path without changing the feature
			//otherwise, recalculate the feature and attributes
			if(hasEllipseChanged(e, path)){
//				path = getEllipticalPath(e, path);
//				f.setAttributeQuiet(Field.FIELD_PATH, path);
				//TODO: implement this when editing ellipses works
//				updateEllipseFeature(f, e);
//				path = f.getPath();
			}
		}
		
		
		return path;
	}
	
	
	/**
	 * Given an FPath, returns a circular polygon with the given number of
	 * vertices the given kilometer distance from the center of the given path.
	 */
	public static FPath getCirclePath(FPath path, double kmRadius, int vertexCount) {
		// get center of the feature in special west coordinates
		HVector center = new HVector(path.getSpatialWest().getCenter()).unit();
		// get the point of intersection between the ellipsoid and a ray from the center of mass toward the center of the feature
		HVector hit = HVector.intersectMars(HVector.ORIGIN, center);
		// convert radius from kilometers to radians by dividing out the magnitude in kilometers of the ellipsoid hit
		// a point 'radius' radians away from 'center'
		HVector point = center.rotate(
			center.cross(HVector.Z_AXIS).unit(),
			kmRadius / hit.norm()).unit();
		// rotate 'point' around 'center' once per vertex
		float[] coords = new float[vertexCount*2];
		double toRad = Math.toRadians(360/(vertexCount*1.0));
		for (int i = 0; i < vertexCount; i++) {
			double omega = i*toRad;
			HVector vertex = point.rotate(center, omega);
			coords[2*i] = (float)vertex.lonW();
			coords[2*i+1] = (float)vertex.latC();
		}
		return new FPath(coords, false, FPath.SPATIAL_WEST, true);
	}

	/**
	 * Given a center point, radius, and number of desired points, returns a number of equally spaced
	 * points the given kilometer distance from the center
	 * 
	 * @param centerPt  Center point of the circle specified in spatial west coordinates
	 * @param kmRadius  The radius in km
	 * @param vertexCount  Number of vertices
	 * @param angularOffset  Angular offset specified in degrees
	 * @return  Equally spaced points around the center point for the given radius
	 */
	public static Point2D[] getCirclePoints(Point2D centerPt, double kmRadius, int vertexCount, double angularOffset) {
		// get center of the feature in special west coordinates
		HVector center = new HVector(centerPt).unit();
		// get the point of intersection between the ellipsoid and a ray from the center of mass toward the center of the feature
		HVector hit = HVector.intersectMars(HVector.ORIGIN, center);
		// convert radius from kilometers to radians by dividing out the magnitude in kilometers of the ellipsoid hit
		// a point 'radius' radians away from 'center'
		HVector point = center.rotate(
			center.cross(HVector.Z_AXIS).unit(),
			kmRadius / hit.norm()).unit();
		
		Point2D returnPts[] = new Point2D.Double[vertexCount];
		// rotate 'point' around 'center' once per vertex
		double toRad = Math.toRadians(360/(vertexCount*1.0));
		for (int i = 0; i < vertexCount; i++) {
			double omega = i*toRad+Math.toRadians(angularOffset);
			//use negative omega to go around the circle clockwise,
			// instead of counter clockwise
			HVector vertex = point.rotate(center, -omega);
			returnPts[i]=new Point2D.Double(vertex.lonW(),vertex.latC());
		}
		return returnPts;
	}

	/**
	 * Take a spatial ellipse and the FPath with spatial points defining
	 * that Ellipse.  Check to see if they match, if they don't return false;
	 * @param e Spatial ellipse
	 * @param path Spatial path for the ellipse
	 * @return True if they are the same
	 */
	private boolean hasEllipseChanged(Ellipse e, FPath path){
		//ellipse center point is in degrees E, convert to deg West
		Point2D ellipseSpCen = new Point2D.Double((360-e.getCenterLon()), e.getCenterLat());
		
		//convert points to world based on a projection at the center and compare centers
		ProjObj po = new Projection_OC(ellipseSpCen.getX(), ellipseSpCen.getY());
		double cenX = 0;
		double cenY = 0;
		//the first and last point are the same, so only include one
		for(int i = 0; i<vertices; i++){
			Point2D wdPt = po.convSpatialToWorld(path.getVertices()[i]);
			cenX += wdPt.getX();
			cenY += wdPt.getY();
		}
		cenX = cenX/(vertices);
		cenY = cenY/(vertices);
		Point2D pathWdCen = new Point2D.Double(cenX, cenY);
		Point2D pathSpCen = po.convWorldToSpatial(pathWdCen);
		
		//compare the centers, if they're not close, the ellipse has changed
		if(Point2D.Double.distance(pathSpCen.getX(), pathSpCen.getY(), ellipseSpCen.getX(), ellipseSpCen.getY()) > 0.001){
//			System.out.println("center has changed");
			return true;
		}
		
		
		//Need to get the A and B points (where the axes intersect perpendicularly from the center on the path)
		Point2D aSpPt = path.getVertices()[0];
		Point2D bSpPt = path.getVertices()[vertices/4]; //the b point is a quarter around the ellipse
		
		//compare the a-axis (if it's changed more than 0.1% the length, it's significant)
		double ellipseA = e.getALength();
		double pathA = Util.angularAndLinearDistanceS(pathSpCen, aSpPt, Main.testDriver.mainWindow.getProj())[1] * 2;
		if(Math.abs(ellipseA-pathA) > ellipseA*0.001){
//			System.out.println("a axis changed");
			return true;
		}
		
		//compare the b-axis (if it's changed more than 0.1% the length, it's significant)
		double ellipseB = e.getBLength();
		double pathB = Util.angularAndLinearDistanceS(pathSpCen, bSpPt, Main.testDriver.mainWindow.getProj())[1] * 2;
		if(Math.abs(ellipseB-pathB) > ellipseB*.001){
//			System.out.println("b axis changed");
			return true;
		}
		
		//compare the angle
		double ellipseAngle = e.getRotationAngle();
		Point2D shiftedCenSpPt = new Point2D.Double(pathSpCen.getX(), 0);
		Point2D shiftedASpPt = new Point2D.Double(aSpPt.getX(), Math.abs(pathSpCen.getY()-aSpPt.getY()));
		Point2D shiftedAXSpPt = new Point2D.Double(aSpPt.getX(), 0);
		double hypDist = Util.angularAndLinearDistanceS(shiftedCenSpPt, shiftedASpPt, Main.testDriver.mainWindow.getProj())[1];
		double adjDist = Util.angularAndLinearDistanceS(shiftedCenSpPt, shiftedAXSpPt, Main.testDriver.mainWindow.getProj())[1];
		double pathAngle = Math.acos(adjDist/hypDist);
		if(Math.abs(ellipseAngle-pathAngle)>0.001){
//			System.out.println("angle changed");
			return true;
		}
		
		
		return false;
	}
	
	private void updateEllipseFeature(Feature f, Ellipse e){
		FPath oldPath = f.getPath();
		//get the center of the ellipse (convert from deg E to west)
		Point2D cenSpPt = new Point2D.Double((360-e.getCenterLon()), e.getCenterLat());
		//get the old A and B points
		Point2D aSpPt = oldPath.getVertices()[0];
		Point2D bSpPt = oldPath.getVertices()[vertices/4]; //the b point is a quarter around the ellipse
		
//		System.out.println("a Org, "+(360-aSpPt.getX())+", "+aSpPt.getY());
//		System.out.println("b Org, "+(360-bSpPt.getX())+", "+bSpPt.getY());
		
	
		//Use vectors to find proper points
		HVector start = new HVector(cenSpPt);
		//A direction
		HVector horDir = new HVector(aSpPt);
		HVector horAxis = start.cross(horDir);
		HVector horEnd = start.rotate(horAxis, (e.getALength()/2)/Util.MEAN_RADIUS);
		
		//B Direction
		HVector verDir = new HVector(bSpPt);
		HVector verAxis = start.cross(verDir);
		HVector verEnd = start.rotate(verAxis, (e.getBLength()/2)/Util.MEAN_RADIUS);
		
//		System.out.println("a new, "+horEnd.lonE()+", "+horEnd.lat());
//		System.out.println("b new, "+verEnd.lonE()+", "+verEnd.lat());
		
		
		//create world ellipse with proj centered on center of ellipse
		ProjObj po = new Projection_OC(cenSpPt.getX(), cenSpPt.getY());
		//get world coords to work with
		Point2D cenWdPt = po.convSpatialToWorld(start.lon(), start.lat());
		Point2D aWdPtEnd = po.convSpatialToWorld(horEnd.lon(), horEnd.lat());
		Point2D bWdPtEnd = po.convSpatialToWorld(verEnd.lon(), verEnd.lat());
					
		//check to make sure the a or b x values are 
		// within 180 degrees of the center point (they should never
		// be greater than 180 because that means the ellipse would
		// be greater than 360 degrees wide)
		double cenWdX = cenWdPt.getX();
		double aWdX = aWdPtEnd.getX();
		double bWdX = bWdPtEnd.getX();
		//Check A axis
		if(Math.abs(cenWdX - aWdX) > 180){
			//if the center point is greater, than add 360 to A
			if(cenWdX > aWdX){
				aWdX = aWdX + 360;
			}
			//otherwise subtract 360 from A
			else{
				aWdX = aWdX - 360;
			}
		}
		//Check B axis
		if(Math.abs(cenWdX - bWdX) > 180){
			//if the center point is greater, than add 360 to B
			if(cenWdX > bWdX){
				bWdX = bWdX + 360;
			}
			//otherwise subtract 360 from B
			else{
				bWdX = bWdX - 360;
			}
		}
					
		//calculate x and y components of distances between a and b to the start
		// and use to find width and height
		double x = cenWdPt.getX();
		double y = cenWdPt.getY();
						
		double aX = (aWdX-x)*2;
		double aY = (aWdPtEnd.getY()-y)*2;
		double width = Math.sqrt(Math.pow(aX, 2)+Math.pow(aY, 2));
		double bX = Math.abs(x-bWdX)*2;
		double bY = Math.abs(y-bWdPtEnd.getY())*2;
		double height = Math.sqrt(Math.pow(bX, 2)+Math.pow(bY, 2));
						
		//convert the angle from spatial to world
		double hyp = Math.sqrt(Math.pow(Math.abs(cenWdX - aWdX), 2) + Math.pow(Math.abs(cenWdPt.getY() - aWdPtEnd.getY()), 2));
		double adj = cenWdX - aWdX;
		double angle = Math.acos(adj/hyp);
		
		Ellipse worldE = new Ellipse(x, y, width, height, angle);

		//get spatial path for world ellipse
		FPath newPath = getSpatialPathFromWorlEllipse(worldE, po);
		
		
		//set new values on feature
		f.setAttributeQuiet(Field.FIELD_PATH, newPath);
		
	}
	
	/**
	 * Given an FPath, returns an elliptical polygon (in SPATIAL COORDS) with the given number of
	 * vertices the given kilometer distance and angle in radians from the center of the given path.
	 *
	 * @param cenPt  Center point in Spatial degrees west
	 * @param kmAAxis  Length of the A axis in km
	 * @param kmBAxis  Length of the B axis in km
	 * @param angle  Angle of rotation in radians
	 * @return
	 */
	public static FPath getEllipticalPath(Point2D cenPt, double kmAAxis, double kmBAxis, double angle) {
		//get the center of the ellipse
		HVector start = new HVector(cenPt);

		//Horizontal component
		//use the spatial angle to find the A direction
		HVector horDir = new HVector(start.lon()-Math.cos(angle), start.lat()+Math.sin(angle));
		HVector horAxis = start.cross(horDir);
		HVector horEnd = start.rotate(horAxis, (kmAAxis/2)/Util.MEAN_RADIUS);
		
		//Vertical component
		//use 90 degrees plus the A-angle to find the B angle and direction
		double theta = Math.PI/2+angle;
		HVector verDir = new HVector(start.lon()-Math.cos(theta), start.lat()+Math.sin(theta));
		HVector verAxis = start.cross(verDir);
		HVector verEnd = start.rotate(verAxis, (kmBAxis/2)/Util.MEAN_RADIUS);
		
		
//		System.out.println("Spatial cen, "+(360-start.lon())+", "+start.lat()+"\na, "+(360-horEnd.lon())+", "+horEnd.lat()+"\nb, "+(360-verEnd.lon()+", "+verEnd.lat()));
		
		//center world proj on the center point
		ProjObj po = new Projection_OC(cenPt.getX(), cenPt.getY());
		//get world coords to work with
		Point2D cenWdPt = po.convSpatialToWorld(start.lon(), start.lat());
		Point2D aWdPtEnd = po.convSpatialToWorld(horEnd.lon(), horEnd.lat());
		Point2D bWdPtEnd = po.convSpatialToWorld(verEnd.lon(), verEnd.lat());
		
		//check to make sure the a or b x values are 
		// within 180 degrees of the center point (they should never
		// be greater than 180 because that means the ellipse would
		// be greater than 360 degrees wide)
		double cenWdX = cenWdPt.getX();
		double aWdX = aWdPtEnd.getX();
		double bWdX = bWdPtEnd.getX();
		//Check A axis
		if(Math.abs(cenWdX - aWdX) > 180){
			//if the center point is greater, than add 360 to A
			if(cenWdX > aWdX){
				aWdX = aWdX + 360;
			}
			//otherwise subtract 360 from A
			else{
				aWdX = aWdX - 360;
			}
		}
		//Check B axis
		if(Math.abs(cenWdX - bWdX) > 180){
			//if the center point is greater, than add 360 to B
			if(cenWdX > bWdX){
				bWdX = bWdX + 360;
			}
			//otherwise subtract 360 from B
			else{
				bWdX = bWdX - 360;
			}
		}
		
		//calculate x and y components of distances between a and b to the start
		// and use to find width and height
		double x = cenWdPt.getX();
		double y = cenWdPt.getY();
		
		double aX = (aWdX-x)*2;
		double aY = (aWdPtEnd.getY()-y)*2;
		double width = Math.sqrt(Math.pow(aX, 2)+Math.pow(aY, 2));
		double bX = Math.abs(x-bWdX)*2;
		double bY = Math.abs(y-bWdPtEnd.getY())*2;
		double height = Math.sqrt(Math.pow(bX, 2)+Math.pow(bY, 2));
		
		//convert the angle from spatial to world
		double hyp = Math.sqrt(Math.pow(Math.abs(cenWdX - aWdX), 2) + Math.pow(Math.abs(cenWdPt.getY() - aWdPtEnd.getY()), 2));
		double adj = cenWdX - aWdX;
		
		
		angle = Math.acos(adj/hyp);


//		System.out.println("Ending World: cen: "+cenWdPt.getX()+", "+cenWdPt.getY()+" a1: "+aWdPtEnd.getX()+", "+aWdPtEnd.getY()+" b1: "+bWdPtEnd.getX()+", "+bWdPtEnd.getY()+" deg:"+Math.toDegrees(angle));

		
		//use all the components to create an ellipse with world values
		Ellipse worldEllipse = new Ellipse(x, y, width, height, angle);
		
//		System.out.println("Ending world ellipse: "+worldEllipse);
		
		//get the spatial path from the world ellipse and return
		return getSpatialPathFromWorlEllipse(worldEllipse, po);
	}
	
	
	
	/**
	 * Takes in an {@link Ellipse} with world values, and the projection those world
	 * values were created in, and returns an ellipse with spatial values.
	 * Where the longitude is in degrees W.
	 * @param e An {@link Ellipse} with world values (center point in world coords, a and
	 * b axis length in world degree distances, angle in world space)
	 * @param po The projection that should be centered around this ellipse 
	 * (either using the center point, or one of the 5 points that defined it)
	 * @return An {@link Ellipse} with spatial values (center lon in degrees East,
	 * and center lat in degrees N).  A and B lengths in km. Angle in spatial space.
	 */
	public static Ellipse convertWorldEllipseToSpatialEllipse(Ellipse e, ProjObj po){
		//get center world point
		Point2D cenWdPt = e.getCenterPt();

		//find a point for the A Axis
		double angle = e.getRotationAngle();
		double aLength = e.getALength();
		//find the x and y components of the length
		double aX = Math.cos(angle)*aLength;
		double aY = Math.sin(angle)*aLength;
		//go halfway down/to the left and halfway up/to the right to get points to measure from
		Point2D aWdPt1 = new Point2D.Double(cenWdPt.getX()-aX/2, cenWdPt.getY()-aY/2);
		Point2D aWdPt2 = new Point2D.Double(cenWdPt.getX()+aX/2, cenWdPt.getY()+aY/2);

		//find a point for the B Axis
		double bLength = e.getBLength();
		//find the x and y components
		double bX = Math.sin(angle)*bLength;
		double bY = Math.cos(angle)*bLength;
		//find two points halfway down/right and up/left from the center
		Point2D bWdPt1 = new Point2D.Double(cenWdPt.getX()-bX/2, cenWdPt.getY()+bY/2);
		Point2D bWdPt2 = new Point2D.Double(cenWdPt.getX()+bX/2, cenWdPt.getY()-bY/2);
		
		//measure A and B lengths in km
		double aAxisKm = Util.angularAndLinearDistanceW(aWdPt1, aWdPt2, Main.testDriver.mainWindow.getProj())[1];
		double bAxisKm = Util.angularAndLinearDistanceW(bWdPt1, bWdPt2, Main.testDriver.mainWindow.getProj())[1];
		
		
		//convert the rotation angle from world coords to spatial
		//use A point and the center point to calculate rotation
		Point2D cenSpPt = po.convWorldToSpatial(cenWdPt);
		Point2D aSpPt = po.convWorldToSpatial(aWdPt2);
		
		//shift the spatial points back to the equator and use lines of 
		// latitude to calculate the angle relative to
		Point2D shiftedCenSpPt = new Point2D.Double(cenSpPt.getX(), 0);
		Point2D shiftedASpPt = new Point2D.Double(aSpPt.getX(), Math.abs(cenSpPt.getY()-aSpPt.getY()));
		Point2D shiftedAXSpPt = new Point2D.Double(aSpPt.getX(), 0);
		
		double hypDist = Util.angularAndLinearDistanceS(shiftedCenSpPt, shiftedASpPt, Main.testDriver.mainWindow.getProj())[1];
		double adjDist = Util.angularAndLinearDistanceS(shiftedCenSpPt, shiftedAXSpPt, Main.testDriver.mainWindow.getProj())[1];
		double theta = Math.acos(adjDist/hypDist) * 100;
		
		
		//create an ellipse
		Ellipse newEllipse = new Ellipse(cenSpPt.getX(), cenSpPt.getY(), aAxisKm, bAxisKm, theta);
		
		return newEllipse;
	}
	
	/**
	 * Converts the value to kilometers based on what the units are set to
	 * @param value
	 * @return That value in km.
	 */
	private double scaleToKm(double value){
		return value * lengthUnits.getScale();
	}
	private static double scaleAxisFieldToKM(double val, LengthUnits existingLu) {
		return val * existingLu.getScale();	
	}
	public double scaleRadiusToKm(double val) {
		return val * units.getScale();
	}
	
	/**
	 * Given an {@link Ellipse} with world values, and a projection centered on
	 * the spatial center of that ellipse, return a spatial path that defines
	 * the edge of that ellipse.
	 * @param e {@link Ellipse} defined in world coordinates
	 * @param po A ProjObj centered at the spatial center of the ellipse
	 * @return A Path in spatial coordinates defining the edge of the ellipse
	 */
	public static FPath getSpatialPathFromWorlEllipse(Ellipse e, ProjObj po){
		//since everything is in world x and y are from the center point, 
		// and height == bLength and width == aLength
		Point2D cenWdPt = e.getCenterPt();
		double aLength = e.getALength();
		double bLength = e.getBLength();
		double angle = e.getRotationAngle();
		//find 36 (vertices value) points around the edge in world coords and create a path
		// set the spatial path on the ellipse by calling that path.getSpatialWest()
		Point2D[] spPts = new Point2D.Double[vertices+1];
		double x = cenWdPt.getX();
		double y = cenWdPt.getY();
		double width = aLength;
		double height = bLength;
		int index = 0;
		//step around the ellipse and calculate points on the edge
		for(double t=0; t<2*Math.PI; t+= Math.PI/(vertices/2)){
			double a = width/2;
			double b = height/2;
			double x_diff = a*Math.cos(t)*Math.cos(angle)-b*Math.sin(t)*Math.sin(angle);
			double y_diff = a*Math.cos(t)*Math.sin(angle)+b*Math.sin(t)*Math.cos(angle);
			
			double x_new = x + x_diff;
			double y_new = y + y_diff;
			Point2D pt_new = new Point2D.Double(x_new, y_new);
			//convert back to spatial
			Point2D spPt = po.convWorldToSpatial(pt_new);
			spPts[index] = spPt;
			index++;
		}
		
		return new FPath(spPts, FPath.SPATIAL_WEST, true);
	}
	
	/**
	 * Edits the given geometry source in place, and returns a new GeomSource if
	 * the values were changed and ok was hit.
	 */
	public static StyleSource<FPath> editCircleSource(Frame parent, Collection<Field> fields,
			final GeomSource geomSource) {
		final List<StyleSource<FPath>> out = new ArrayList<StyleSource<FPath>>(1);
		final Field nullField = new Field("<None>", String.class);
		
		// create units cb and default it to the first unit with equal scale
		final JComboBox unitBox = new JComboBox(Units.values());
		unitBox.setSelectedItem(geomSource == null ? null : geomSource.units);
		
		// create field cb and default it to the current field
		Set<Field> numberFields = new LinkedHashSet<Field>();
		numberFields.add(nullField);
		for (Field f: fields) {
			if (Integer.class.isAssignableFrom(f.type) ||
					Float.class.isAssignableFrom(f.type) ||
					Double.class.isAssignableFrom(f.type)) {
				numberFields.add(f);
			}
		}
		final JComboBox fieldBox = new JComboBox(numberFields.toArray(
			new Field[numberFields.size()]));
		fieldBox.setSelectedItem(geomSource == null ? nullField : geomSource.radiusField);
		
		final JDialog frame = new JDialog(parent, "Geometry Options", true);
		
		// cancel button just hides the dialog
		JButton cancel = new JButton("Cancel".toUpperCase());
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
			}
		});
		
		
		// ok button copies in the results
		JButton ok = new JButton("OK".toUpperCase());
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Field f = (Field) fieldBox.getSelectedItem();
				Units u = (Units)unitBox.getSelectedItem();
				if (f == nullField || u == null) {
					out.add(new StyleFieldSource<FPath>(Field.FIELD_PATH, null));
				}
				//if the fields are not null, use them in creating a new geom source
				else{
					//if the geomSource is null, only populated with settings for circles
					if(geomSource == null){
						out.add(new GeomSource(f,u,null,null,null,null,null,null,null,null));
					}
					//if the geomSource already exists, create a new one with new settings
					// for circles, and the existing settings for ellipses
					
					//TODO: this is wrong. Why create a new GeomSource and wipe out all the user's style settings. Just populate the new fields 
					//and return it.
					else if (geomSource.radiusField != f || geomSource.units != u) {
						out.add(new GeomSource(f, u, geomSource.aAxisField, geomSource.bAxisField,
								geomSource.angleField, geomSource.lengthUnits, geomSource.angleUnits,
								geomSource.centerLatField, geomSource.centerLonField, geomSource.meanAxisField));
					}
				}
				
				frame.setVisible(false);
			}
		});
		
		// create and show frame
		JPanel p = new JPanel(new GridBagLayout());
		int pad = 4;
		p.setBorder(new EmptyBorder(pad,pad,pad,pad));
		frame.setContentPane(p);
		Insets in = new Insets(0, pad, pad, 0);
		Box buttons = Box.createHorizontalBox();
		buttons.add(cancel);
		buttons.add(Box.createHorizontalStrut(pad));
		buttons.add(ok);
		Component[][] parts = {
				{new JLabel("Circle Radius Field"), fieldBox},
				{new JLabel("Circle Radius Scale"), unitBox},
				{new JLabel(), buttons}
		};
		for (int y = 0; y < parts.length; y++) {
			for (int x = 0; x < parts[y].length; x++) {
				int wx = parts[y][x] instanceof JLabel ? 0 : 1;
				p.add(parts[y][x], new GridBagConstraints(
					x,y, 1,1, wx,0, GridBagConstraints.NORTHWEST,
					GridBagConstraints.HORIZONTAL, in, pad, pad));
			}
		}
		
		frame.pack();
		frame.setLocationRelativeTo(parent);
		frame.setVisible(true);
		
		return out.isEmpty() ? null : out.get(0);
	}
	
	class EllipseData {
		int majorIndex1 = -1;
		int majorIndex2 = -1;
		int minorIndex1 = -1;
		int minorIndex2 = -1;
		double majorDistanceW = -1.00;
		double majorDistanceScr = -1.00;
		double minorDistanceW = -1.00;
		double minorDistanceScr = -1.00;
		Point2D majorPoint1W = null;
		Point2D majorPoint2W = null;
		Point2D minorPoint1W = null;
		Point2D minorPoint2W = null;
		Point2D majorPoint1Scr = null;
		Point2D majorPoint2Scr = null;
		Point2D minorPoint1Scr = null;
		Point2D minorPoint2Scr = null;
		Point2D centerPoint = null;
		double calculatedThetaWorld = -1.00;
		double calculatedThetaSpatial = -1.00;
		double calculatedThetaScreen = -1.00;
	}
	public EllipseData calculateEllipseData(FPath fp, MultiProjection projection) {
		Point2D[] worldVertices = fp.getWorld().getVertices();

//		for (int i=0; i<worldVertices.length; i++) {
//			if (i < 18) {
//				Point2D point1 = worldVertices[i];
//				System.out.println("point "+i+": "+point1);
//				Point2D point2 = worldVertices[i+18];
//
//				System.out.println("point "+(i+18)+": "+point2);
//			}
//		}
		
		EllipseData ed = new EllipseData();
		ed.centerPoint = fp.getWorld().getCenter();
		for (int i=0; i<worldVertices.length; i++) {
			if (i < 18) {
				Point2D point1 = worldVertices[i];
//				System.out.println("point1: "+point1);
//				point1 = projection.world.toScreen(point1);
				Point2D point2 = worldVertices[i+18];

//				System.out.println("point2: "+point2);
//				point2 = projection.world.toScreen(point2);
		
				double worldDistance = Math.hypot(point1.getX() - point2.getX(), point1.getY() - point2.getY());
				if (Double.compare(worldDistance, ed.majorDistanceW) > 0) {
//					System.out.println("i: "+i);
//					System.out.println("new point1: "+point1);
//					System.out.println("new point2: "+point2);
//					
					System.out.println("new world distance: "+worldDistance);
					ed.majorDistanceW = worldDistance;
					ed.majorIndex1 = i;
					ed.majorIndex2 = i+18;
					ed.majorPoint1W = point1;
					ed.majorPoint2W = point2;
				}
			}			
		}
		
		if (ed.majorIndex1 < 10) {
			ed.minorIndex1 = ed.majorIndex1 + 9;
		} else {
			ed.minorIndex1 = ed.majorIndex1 - 9;
		}
		ed.minorIndex2 = ed.minorIndex1 + 18;
		if (ed.minorIndex2 > worldVertices.length -1) {
			ed.minorIndex2 = 0;
		}
		ed.minorPoint1W = worldVertices[ed.minorIndex1];
//		ed.minorPoint1 = projection.world.toScreen(ed.minorPoint1);
		ed.minorPoint2W = worldVertices[ed.minorIndex2];
//		ed.minorPoint2 = projection.world.toScreen(ed.minorPoint2);
		double minorDistance = Math.hypot(ed.minorPoint1W.getX() - ed.minorPoint2W.getX(), ed.minorPoint1W.getY() - ed.minorPoint2W.getY());
		ed.minorDistanceW = minorDistance;
		
		if (Double.compare(ed.majorPoint1W.getX(), ed.majorPoint2W.getX()) > 0) {
			Point2D temp = ed.majorPoint1W;
			ed.majorPoint1W = ed.majorPoint2W;
			ed.majorPoint2W = temp;
		}
		
		ed.majorPoint1Scr = projection.world.toScreen(ed.majorPoint1W);
		ed.majorPoint2Scr = projection.world.toScreen(ed.majorPoint2W);
		ed.minorPoint1Scr = projection.world.toScreen(ed.minorPoint1W);
		ed.minorPoint2Scr = projection.world.toScreen(ed.minorPoint2W);
		ed.majorDistanceScr = Math.hypot(ed.majorPoint1Scr.getX() - ed.majorPoint2Scr.getX(), ed.majorPoint1Scr.getY() - ed.majorPoint2Scr.getY());
		ed.minorDistanceScr = Math.hypot(ed.minorPoint1Scr.getX() - ed.minorPoint2Scr.getX(), ed.minorPoint1Scr.getY() - ed.minorPoint2Scr.getY());
		
		Point2D[] pointArr = new Point2D[]{ed.majorPoint1W, ed.majorPoint2W};
        FPath lineFP = new FPath(pointArr, FPath.WORLD, false);
        Object angle = FeatureUtil.calculateLineAngle(lineFP.getWorld());
        if (angle != null && angle instanceof Double) {
			ed.calculatedThetaWorld = (Double) angle;
		}
        
        pointArr = new Point2D[]{projection.world.toSpatial(ed.majorPoint1W), projection.world.toSpatial(ed.majorPoint2W)};
        lineFP = new FPath(pointArr, FPath.SPATIAL_WEST, false);
        angle = FeatureUtil.calculateLineAngle(lineFP.getSpatialEast());//TODO: do we need this in user's selection?
        if (angle != null && angle instanceof Double) {
			ed.calculatedThetaSpatial = (Double) angle;
		}

        ed.calculatedThetaScreen = Math.toDegrees(Math.atan2(ed.majorPoint1Scr.getY() - ed.majorPoint2Scr.getY(), (ed.majorPoint1Scr.getX() - ed.majorPoint2Scr.getX())));  
        return ed;
    }
	
	private FPath drawEllipse(double majorRadius, double minorRadius, boolean yAxis, Point2D startingPoint, MultiProjection projection) {
		boolean negative = false;
        Point2D first = null;
        ArrayList<Point2D> ellipsePoints = new ArrayList<Point2D>();
		
        double downY = startingPoint.getY();
        double downX = startingPoint.getX();
        
//        System.out.println("startingPoint: "+startingPoint);
//        System.out.println("in drawEllipse major screen: "+(majorRadius*2.0));
//        System.out.println("in drawEllipse minor screen: "+(minorRadius*2.0));
//        if (forEdit) {//downX and downY are in world coordinates for edits
//        	
//        }
        
		for (int i = 0; i < 360; i=i+10) {
            double x, y;
            double xPt, yPt = 0;
            
            if (yAxis) {
            	y = majorRadius * Math.sin(Math.toRadians(i));
            	x = minorRadius * Math.cos(Math.toRadians(i));
            	yPt = y + downY + majorRadius;
            	xPt = x + downX;
            } else {
            	x = majorRadius * Math.sin(Math.toRadians(i));
            	y = minorRadius * Math.cos(Math.toRadians(i));
            	yPt = y + downY;
            	xPt = x + downX + majorRadius;
            }
            
        	Point2D p = new Point2D.Double(xPt, yPt);
//        	if (!forEdit) { //on edit, we are sending world points. For initial draw, we are sending screen points
        		p = projection.screen.toWorld(p);
//        	}
//        	System.out.println("point "+i+": "+p);
        	
        	if (p.getX() < 0) {
        		negative = true;
        	}
        	

        	ellipsePoints.add(p);
        	if (first == null) {
        		first = p;//store the first to close the polygon
//        		System.out.println("first: "+p);
        	}

        }
        if (negative) {//if we found a negative x value in world coordinates
	        for (Point2D pt : ellipsePoints) {
	        	if (pt.getX() > 0) {
	        		pt.setLocation(pt.getX()-360, pt.getY());//subtract 360 from x to adjust for negative value, for points where x is positive
	        	}
	        }
        }
        ellipsePoints.add(first);
        
        Point2D[] pointArr = (Point2D[]) ellipsePoints.toArray(new Point2D[]{});
		FPath ellipseFP = new FPath(pointArr, FPath.WORLD, true);
		return ellipseFP;
	}
	private FPath drawEllipse2(Point2D center, double angle, double majorAxis, double minorAxis, MultiProjection projection) {
		System.out.println("Center: "+center);
		
		int numPoints = 36;
		double angleIncrement = angle;
		double centerX = center.getX();
		double centerY = center.getY();
		boolean negative = false;
		angleIncrement = 2 * Math.PI / numPoints;
		ArrayList<Point2D> points = new ArrayList<Point2D>();
		double angleInRadians = Math.toRadians(angle);
		for (int i = 0; i < numPoints; i++) {
            double theta = i * angleIncrement;
            double x = centerX + majorAxis * Math.cos(theta);
            double y = centerY + minorAxis * Math.sin(theta);
            
            double rotatedX = (centerX + (x - centerX) * Math.cos(angleInRadians) - (y - centerY) * Math.sin(angleInRadians));
            double rotatedY = (centerY + (x - centerX) * Math.sin(angleInRadians) + (y - centerY) * Math.cos(angleInRadians));
            
            Point2D p = new Point2D.Double(rotatedX, rotatedY);
            p = projection.screen.toWorld(p);
            if (p.getX() < 0) {
        		negative = true;
        	}
            points.add(p);
        }
		if (negative) {//if we found a negative x value in world coordinates
	        for (Point2D pt : points) {
	        	if (pt.getX() > 0) {
	        		pt.setLocation(pt.getX()-360, pt.getY());//subtract 360 from x to adjust for negative value, for points where x is positive
	        	}
	        }
        }
        points.add(points.get(0));
        
        Point2D[] pointArr = (Point2D[]) points.toArray(new Point2D[]{});
		FPath ellipseFP = new FPath(pointArr, FPath.WORLD, true);
		return ellipseFP;
		
	}
	private FPath updateEllipse(FPath ellipseFP, Point2D centerPoint, double minorAxis) {
		Point2D[] ellipsePoints = ellipseFP.getWorld().getVertices();
//		Point2D centerPoint = ellipseFP.getWorld().getCenter();
		double centerX = centerPoint.getX();
		double centerY = centerPoint.getY();
		double newMinorAxis = minorAxis + 10;
		for (Point2D point : ellipsePoints) {
            double x = point.getX();
            double scaledY = centerY + (point.getY() - centerY) * (newMinorAxis / (double) minorAxis);
            point.setLocation(x, scaledY);
        }
		FPath fp = new FPath(ellipsePoints, FPath.WORLD, true);
		return fp;
	}
	public Shape getEllipseShapeForEdit(EllipseData ed, Feature selectedEllipse, double tempTheta, double minorAxisAdjustment, double majorAxisAdjustment, 
			double locationUpDownAdjustment, double locationLeftRightAdjustment, MultiProjection projection) {
//		Double theta = (Double)selectedEllipseFeature.getAttribute(this.getAngleField());//spatial
		FPath fp = selectedEllipse.getPath().getWorld();

		double minor = (Double)selectedEllipse.getAttribute(bAxisField);
		double centerY = (Double)selectedEllipse.getAttribute(centerLatField);
		double centerX = (Double)selectedEllipse.getAttribute(centerLonField);
		
		Point2D center = new Point2D.Double(centerX, centerY);
		FPath newFP = updateEllipse(fp, center, minor);
		return newFP.getWorld().getShape();
//		Point2D center = projection.world.toScreen(fp.getCenter());
		
//		double centerY = (Double)selectedEllipse.getAttribute(centerLatField);
//		double centerX = (Double)selectedEllipse.getAttribute(centerLonField);
//		
//		Point2D center = new Point2D.Double(centerX, centerY);
//		center = projection.spatial.toWorld(center);
//		center = projection.world.toScreen(center);
//		double theta = (Double)selectedEllipse.getAttribute(angleField);
////		double major = (Double)selectedEllipse.getAttribute(aAxisField);
////		double minor = (Double)selectedEllipse.getAttribute(bAxisField);
//		double major = ed.majorDistanceScr;
//		double minor = ed.minorDistanceScr;
//		double majorRadius = major / 2.0;
//		double minorRadius = minor / 2.0;
//		FPath fp = drawEllipse2(center,theta ,majorRadius,minorRadius,projection);
//		return fp.getWorld().getShape();
		
		/*
		FPath ellipseGhostFP = null;
		Shape returnShape = null;
		
		boolean yLonger = false;
		if (Double.compare(tempTheta, 45.0) < 0 || Double.compare(tempTheta, 135.0) > 0) {
			yLonger = true;
		}
		
//		if (Double.compare(majorAxisAdjustment, 0.00) != 0 || Double.compare(majorAxisAdjustment, 0.00) != 0) {
			//we have a change in the ellipse size (major or minor), we will redraw it
		
			double majorRadius = 0;
			double minorRadius = 0;
	
	//		System.out.println("start edit angle: "+tempTheta);
			
			double majorDiam, minorDiam;
			majorDiam = ed.majorDistanceScr;//note that we have moved these back to screen distances
			minorDiam = ed.minorDistanceScr;
//			System.out.println("major screen in edit: "+majorDiam);
//			System.out.println("minor screen in edit: "+minorDiam);
			
//			double factor = 1.0 / (1.0*Main.testDriver.mainWindow.getZoomManager().getZoomPPD());
//			majorDiam = majorDiam + majorAxisAdjustment * factor; //mouse wheel listener with shift held down
//			minorDiam = minorDiam + minorAxisAdjustment * factor; //mouse wheel listener 
			
			majorDiam = majorDiam + majorAxisAdjustment; //mouse wheel listener with shift held down
			minorDiam = minorDiam + minorAxisAdjustment; //mouse wheel listener 
	    	
	        majorRadius = majorDiam / 2.0;
			minorRadius = minorDiam / 2.0;
			
	//		System.out.println("major: "+majorRadius+" minor: "+minorRadius+" yLonger: "+ yLonger+" mp: "+ed.majorPoint1W);
			Point2D originalCenter = ed.centerPoint;
			double origX = originalCenter.getX();
			double origY = originalCenter.getY();
			
			Point2D startingPoint = ed.majorPoint1Scr;
			
			//move the original shape to the center of the world coordinates (0,0)
//			FPath centerFP = selectedEllipse.getPath().getWorld();
//	    	Shape originalShape = centerFP.getShape();
//	    	AffineTransform move1 = AffineTransform.getTranslateInstance(-origX, -origY);
//	    	Shape movedToCenter = move1.createTransformedShape(originalShape);
	    	
	    	//now get the new starting point when centered at 0,0 (won't be 0,0) so that we can draw in world with as little distortion as possible.
//	    	centerFP = new FPath(movedToCenter, FPath.WORLD);
//	    	EllipseData newEd = calculateEllipseData(centerFP.getWorld(), projection);
	    	
			
//	    	
//			System.out.println("major radius before rotate: "+majorRadius);
//			System.out.println("minor radius before rotate: "+minorRadius);
			
//			FPath p = selectedEllipse.getPath().getWorld();
//			double tx = p.getCenter().getX();
//			double ty = p.getCenter().getY();
//			Shape tempShp = p.getShape();
//			
//			double testTheta = 90;
//			AffineTransform mc = AffineTransform.getTranslateInstance(-tx, -ty);
//			AffineTransform r = AffineTransform.getRotateInstance(Math.toRadians(testTheta));
//	        AffineTransform mb = AffineTransform.getTranslateInstance(ty, tx);
//			Shape cd = mc.createTransformedShape(tempShp);
//	        Shape rd = r.createTransformedShape(cd);
//			returnShape = mb.createTransformedShape(rd);
//			p = new FPath(returnShape, FPath.WORLD);
//			
//			EllipseData tested = calculateEllipseData(p.getWorld(), projection);
			
			
//			Point2D startingPoint = tested.majorPoint1Scr;
//			majorDiam = tested.majorDistanceScr;
//			minorDiam = tested.minorDistanceScr;
//			
//			majorDiam = majorDiam + majorAxisAdjustment; //mouse wheel listener with shift held down
//			minorDiam = minorDiam + minorAxisAdjustment; //mouse wheel listener 
//	    	
//	        majorRadius = majorDiam / 2.0;
//			minorRadius = minorDiam / 2.0;
			
			
//			System.out.println("major diam after rotate: "+tested.majorDistanceScr);
//			System.out.println("minor diam after rotate: "+tested.minorDistanceScr);
			
	    	ellipseGhostFP = drawEllipse(majorRadius, minorRadius, yLonger, startingPoint, projection);
			//we re-created the ellipse, we need to rotate it
	    	Shape ellipse = ellipseGhostFP.getShape();
	    	Point2D tempCenter = ellipseGhostFP.getWorld().getCenter();
	    	//these are in world
	    	double tempX = tempCenter.getX();
	    	double tempY = tempCenter.getY();
	    	
	    	//adjust for the affine transform which is 0 at bottom and 180 at top
	    	tempTheta = 180 - tempTheta;
			//if we are drawing along the x-axis (x is longer), we need to adjust theta by 90 degrees to adjust for the jump from y axis to x axis
			if (!yLonger) {
				tempTheta -= 90;
			}
	    	
	    	AffineTransform moveToCenter = AffineTransform.getTranslateInstance(-tempX, -tempY);
			AffineTransform actualRotate = AffineTransform.getRotateInstance(Math.toRadians(tempTheta));
	        AffineTransform moveBack = AffineTransform.getTranslateInstance(origX+locationLeftRightAdjustment, origY+locationUpDownAdjustment);
			Shape centered = moveToCenter.createTransformedShape(ellipse);
	        Shape shapeRotated = actualRotate.createTransformedShape(centered);
			returnShape = moveBack.createTransformedShape(shapeRotated);
			
			
//			newEd = calculateEllipseData(ellipseGhostFP.getWorld(), projection);
//			Point2D centerOnAxis = newEd.centerPoint;
//			double cX = centerOnAxis.getX();
//			double cY = centerOnAxis.getY();
//			Shape ellipse = ellipseGhostFP.getShape();
//			AffineTransform anotherCenter = AffineTransform.getTranslateInstance(-cX, -cY);
//			Shape newShape = anotherCenter.createTransformedShape(ellipse);
//		} 
		
		//move it back
//		AffineTransform moveToOriginal = AffineTransform.getTranslateInstance(origX, origY);
//		Shape newShape = moveToOriginal.createTransformedShape(ellipse);
//		ellipseGhostFP = new FPath(newShape, FPath.WORLD);
//		Point2D tempCenter = ellipseGhostFP.getWorld().getCenter();
//		double tempX = tempCenter.getX();
//		double tempY = tempCenter.getY();
//		
//		EllipseData testEd = calculateEllipseData(ellipseGhostFP.getWorld(), projection);
//		System.out.println("after move to original:" + testEd.majorPoint1W);
		
		
//		if (tempTheta > 180) {
//			tempTheta = tempTheta - 180.0;
//		}
		//adjust for the affine transform which is 0 at bottom and 180 at top
//		tempTheta = 180 - tempTheta;
//		//if we are drawing along the x-axis (x is longer), we need to adjust theta by 90 degrees to adjust for the jump from y axis to x axis
//		if (!yLonger) {
//			tempTheta -= 90;
//		}
		
//		System.out.println("theta before affine: "+tempTheta);
		//to move the shape correctly and rotate, first, move the shape to negative worldDownPt.
		//next move the shape further so that the center of the shape is on the 0,0 at world. This will be different if yLonger or xLonger and will be y or x radius respectively
		//next rotate
        //next move the shape back so that the negative worldPt is back at 0,0 (opposite of last step)
        //move back to original point
		
		
		
//        AffineTransform moveToCenter = AffineTransform.getTranslateInstance(-tempX, -tempY);
//		AffineTransform actualRotate = AffineTransform.getRotateInstance(Math.toRadians(tempTheta));
//        AffineTransform moveBack = AffineTransform.getTranslateInstance(origX+locationLeftRightAdjustment, origY+locationUpDownAdjustment);
        
//		newShape = ellipseGhostFP.getWorld().getShape();
//		Shape centeredShape = moveToCenter.createTransformedShape(newShape);
//        FPath fp = new FPath(newShape, FPath.WORLD);
//        EllipseData ed1 = calculateEllipseData(fp.getWorld(), projection);
//		System.out.println("edit before rotate majorpoint1: "+ed1.majorPoint1W);
//		System.out.println("edit before rotate majorpoint2: "+ed1.majorPoint2W);
//        
//		Shape shapeRotated = actualRotate.createTransformedShape(newShape);
//		Shape returnedShape = moveBack.createTransformedShape(shapeRotated);
//		
//		FPath f = new FPath(returnedShape, FPath.WORLD);
//		EllipseData n = calculateEllipseData(f.getWorld(), projection);
//		System.out.println("end edit angle: "+n.calculatedThetaWorld);
//		System.out.println("done edit feature idx/startingpt: "+n.majorIndex1+"/"+n.majorPoint1W);
		
//		ellipseGhostFP = null;
//		ellipseGhostFP = selectedEllipse.getPath().getWorld();
//		
//		
//		returnShape = ellipseGhostFP.getShape();
//		Point2D[] points = ellipseGhostFP.getWorld().getVertices();
//		double x,y;
//		double xPt,yPt;
//		Point2D point;
//		double factor = 1.0 / (1.0*Main.testDriver.mainWindow.getZoomManager().getZoomPPD());
//		double minorIncrease = 1 * factor;
//		for(int i=0; i<points.length; i++) {
//			point = points[i];
//			xPt = point.getX();
//			yPt = point.getY();
//        	
//			x = majorRadius * Math.sin(Math.toRadians(i));
//        	y = minorRadius * Math.cos(Math.toRadians(i));
//        	yPt = y + downY;
//        	xPt = x + downX + majorRadius;
//            
//		}
			
		return returnShape;*/
	}
	public Shape drawInitialEllipse(Point2D spatialMouseDown, Point2D spatialCurrentMouse, double minorAxisAdjustment, MultiProjection projection) {
		Point2D worldMouse = projection.spatial.toWorld(spatialMouseDown);
        Point2D currentMouseWorld = projection.spatial.toWorld(spatialCurrentMouse);
		
		double wx = worldMouse.getX();
        double cwx = currentMouseWorld.getX();
        if (Double.compare(wx, cwx) > 0) {
        	if (Double.compare(wx - cwx,180) > 0) {
        		currentMouseWorld.setLocation(currentMouseWorld.getX()+360, currentMouseWorld.getY());
        	}
        } else {
        	if (Double.compare(cwx - wx,180) > 0) {
        		worldMouse.setLocation(worldMouse.getX()+360, worldMouse.getY());
        	}
        }
        Point2D screenMouse = projection.world.toScreen(worldMouse);
        double downX = screenMouse.getX();
        double downY = screenMouse.getY();
        
        
        //get the screen location of the mouse point
        Point2D currentMouse = projection.world.toScreen(currentMouseWorld);
        double currentX = currentMouse.getX();
        double currentY = currentMouse.getY();
        
        double majorDiam = -1;
        double minorDiam = -1;
        
        double distance = Math.hypot(downX - currentX, downY - currentY);

        double majorRadius = -1;
        double minorRadius = -1;
        
        
        majorDiam = distance;
        
//        drawCircle = false;//used in addEllipseFeature, reset here
        boolean defaultCircle = Config.get("shape.ellipse_default_circle", false);
        if (defaultCircle) {
//        	drawCircle = true;
        	minorDiam = majorDiam;
        } else {
        	minorDiam = majorDiam / 2.0;
        }

    	minorDiam = minorDiam + minorAxisAdjustment; //mouse wheel listener with ctrl held down
        
        majorRadius = majorDiam / 2.0;
        minorRadius = minorDiam / 2.0;
        
        
        double tempTheta = 0.00;
        double calcTheta = Math.toDegrees(Math.atan2(currentY - downY, (currentX - downX)));  
//        Point2D[] pointArr = new Point2D[]{worldMouse, currentMouseWorld};
//        FPath lineFP = new FPath(pointArr, FPath.WORLD, false);
//        Object angle = FeatureUtil.calculateLineAngle(lineFP.getWorld());
//        if (angle != null && angle instanceof Double) {
//			thetaValue.set((Double)angle);
//			tempTheta = (Double)angle;
        	
//		}
//        System.out.println("Calculated Theta using atan: "+calcTheta);
//        System.out.println("Calculated Theta using method: "+tempTheta);
        tempTheta = calcTheta;
//        double tempTheta = 0.00;
//        Point2D[] pointArr = new Point2D[]{spatialMouseDown, spatialCurrentMouse};
//		FPath lineFP = new FPath(pointArr, FPath.SPATIAL_WEST, true);
//		lineFP = lineFP.getSpatialEast();
//		Object angle = FeatureUtil.calculateLineAngle(lineFP);
//		if (angle != null && angle instanceof Double) {
//			thetaValue.set((Double)angle);
//			tempTheta = thetaValue.get();
//		}
		
//		//adjust theta based on the difference between the formula and what we want
		if (Double.compare(calcTheta, 90.0) > 0) {
			tempTheta = tempTheta - 180.0;
		} else if (Double.compare(calcTheta, -90.0) < 0) {
			tempTheta = tempTheta + 180;
		}
		//all cases from above add 90
		tempTheta = tempTheta + 90;
		// draw the ellipse
		
		boolean yLonger = false;
		if (defaultCircle) {
//			thetaValue.set(0.00);
			yLonger = true;
		} else {
//			thetaValue.set(tempTheta);//This is the theta value that will go into the shape layer table. Remaining adjustments are for the affine transform
			if (Double.compare(tempTheta, 45.0) < 0 || Double.compare(tempTheta, 135.0) > 0) {
				yLonger = true;
			}
		}
		
//		System.out.println("draw major screen radius being sent: "+majorRadius);
//		System.out.println("draw minor screen radius being sent: "+minorRadius);
//		FPath ellipseGhostFP = drawEllipse(majorRadius, minorRadius, yLonger, screenMouse, projection);		        
//		FPath ellipseGhostFP = drawEllipse2(screenMouse, tempTheta, majorDiam, minorDiam,projection);
//		Shape newTestShape = ellipseGhostFP.getWorld().getShape();
//		EllipseData ed = calculateEllipseData(ellipseGhostFP.getWorld(), projection);
		
//		System.out.println("draw major screen after: "+ed.majorDistanceScr);
//		System.out.println("draw minor screen after: "+ed.minorDistanceScr);
		
		//let's make sure we are using the correct calculated major point as the down so that we get the angle the user actually drew
//		double cx1 = worldMouse.getX();
//        double cy1 = worldMouse.getY();
//        double a = Math.hypot(cx1 - ed.majorPoint1W.getX(), cy1 - ed.majorPoint1W.getY());
//        double b = Math.hypot(cx1 - ed.majorPoint2W.getX(), cy1 - ed.majorPoint2W.getY());
//        Point2D major = null;
//        if (Double.compare(a,b) > 0) {
//        	major = ed.majorPoint2W;
//        } else {
//        	major = ed.majorPoint1W;
//        }
		
//		double cx = major.getX();
//		double cy = major.getY();
//		System.out.println("majorpoint1: "+ed.majorPoint1W);
//		System.out.println("majorpoint2: "+ed.majorPoint2W);

		//get the world location of the mouse point
		        
//        System.out.println("cx/cy: "+cx1+"/"+cy1);
		FPath ellipseGhostFP = drawEllipse(majorRadius, minorRadius, yLonger, screenMouse, projection);
		double cx = worldMouse.getX();
		double cy = worldMouse.getY();
		
        
		//adjust for the affine transform which is 0 at bottom and 180 at top
		tempTheta = 180 - tempTheta;
		//if we are drawing along the x-axis (x is longer), we need to adjust theta by 90 degrees to adjust for the jump from y axis to x axis
		if (!yLonger) {
			tempTheta -= 90;
//			thetaValue.set(tempTheta);
		}
		if (Double.compare(calcTheta, 90.0) > 0) {
		//if theta is positive, and tempTheta is less than 90, we have the issue
			tempTheta = tempTheta - 180;
		} else if (Double.compare(calcTheta, -90.0) < 0) {
			tempTheta = tempTheta + 180;
		}

        AffineTransform moveShapeToCenter = AffineTransform.getTranslateInstance(-cx, -cy);
		AffineTransform actualRotate = AffineTransform.getRotateInstance(Math.toRadians(tempTheta));
        AffineTransform returnShapeToMouseLocation = AffineTransform.getTranslateInstance(cx, cy);
        
		Shape ellipse = ellipseGhostFP.getShape();
		Shape createTransformedShape = moveShapeToCenter.createTransformedShape(ellipse);
		Shape createTransformedShape2 = actualRotate.createTransformedShape(createTransformedShape);
		Shape returnLocationShape = returnShapeToMouseLocation.createTransformedShape(createTransformedShape2);
//		
//		return newTestShape;
		
//		FPath fp = new FPath(createTransformedShape, FPath.WORLD);
//        EllipseData ed1 = calculateEllipseData(fp.getWorld(), projection);
//		System.out.println("draw before rotate majorpoint1: "+ed1.majorPoint1W);
//		System.out.println("draw before rotate majorpoint2: "+ed1.majorPoint2W);
		
		
		
		
		
		
//		FPath fp = new FPath(createTransformedShape2, FPath.WORLD);
//		EllipseDataScreen ed = calculateEllipseData(fp, projection);
//		Point2D majorPt = null;
//		System.out.println("point1 x: "+ed.majorPoint1.getX());
//		System.out.println("down x: "+downX);
//		System.out.println("point2 x: "+ed.majorPoint2.getX());
//		System.out.println("current x: "+currentX);
//		if (Double.compare(downX, currentX) <= 0) {//comparison in screen
//			majorPt = ed.majorPoint1;
//			System.out.println("point1");
//		} else {
//			majorPt = ed.majorPoint2;
//			System.out.println("point2");
//		}
//		majorPt = projection.screen.toWorld(majorPt);
//		majorPt = projection.world.toSpatial(majorPt);
//		Point2D major1 = projection.screen.toWorld(ed.majorPoint1);
//		major1 = projection.world.toSpatial(major1);
//		Point2D major2 = projection.screen.toWorld(ed.majorPoint2);
//		major2 = projection.world.toSpatial(major2);
//		if (Double.compare(major1.getX(), major2.getX()) <= 0) {
//			majorPt = major1;
//		} else {
//			majorPt = major2;
//		}
		
		
//		Point2D adjCen = new Point2D.Double(0, 0);
//		adjCen = projection.world.toSpatial(adjCen);
//		Point2D majorxy = new Point2D.Double(majorPt.getX(), majorPt.getY());
//		Point2D majorx = new Point2D.Double(majorPt.getX(), 0);
//		double oppDist = Util.angularAndLinearDistanceS(majorxy, majorx, projection)[1];
//		double hypDist = Util.angularAndLinearDistanceS(adjCen, majorxy, projection)[1];
//		double spatialTheta = Math.asin(oppDist/hypDist);
////		System.out.println("spatial1: "+spatialTheta);
//		spatialTheta *= 100;
        
//		System.out.println("theta: "+thetaValue.get());
//		System.out.println("Spatial theta: "+spatialTheta);
//		System.out.println("Diff: "+(thetaValue.get() - spatialTheta));
		
		
		

		
		
		
//		Point2D center = projection.world.toSpatial(fp.getCenter());
//		Point2D minor1 = projection.screen.toWorld(ed.minorPoint1);
//		minor1 = projection.world.toSpatial(minor1);
//		Point2D minor2 = projection.screen.toWorld(ed.minorPoint2);
//		minor2 = projection.world.toSpatial(minor2);
//		

		return returnLocationShape;
		
	}
	
	/**
	 * Edits the given geometry source in place, and returns a new GeomSource if
	 * the values were changed and ok was hit.
	 */
	public static StyleSource<FPath> editEllipseSource(Frame parent, ShapeLayer shapeLayer, final GeomSource geomSource, final MultiProjection projection) {
		
		List<Field> fields = shapeLayer.getFeatureCollection().getSchema();
//		AtomicBoolean axesUnitUpdated = new AtomicBoolean(false);
		final List<StyleSource<FPath>> out = new ArrayList<StyleSource<FPath>>(1);
		final Field nullField = new Field("<None>", String.class);
		
		// create units cb and default it to the first unit with equal scale
		final JComboBox<LengthUnits> axesUnitBox = new JComboBox<LengthUnits>(LengthUnits.values());
		
//		final JComboBox<AngleUnits> angleUnitBox = new JComboBox<AngleUnits>(AngleUnits.values());
		
		// create field cb and default it to the current field
		LinkedHashSet<Field> numberFields = new LinkedHashSet<Field>();
		numberFields.add(nullField);
		for (Field f: fields) {
			if (Integer.class.isAssignableFrom(f.type) ||
					Float.class.isAssignableFrom(f.type) ||
					Double.class.isAssignableFrom(f.type)) {
				numberFields.add(f);
			}
		}
		
		Field aField = new Field("A Axis", Double.class);
		Field bField = new Field("B Axis", Double.class);
		Field angField = new Field("Rotation Angle", Double.class);
		Field lonField = new Field("Longitude", Double.class);
		Field latField = new Field("Latitude", Double.class);
		Field mField = new Field("Mean Axis Value", Double.class);
		if (geomSource != null) {
			LengthUnits defaultUnits = geomSource.lengthUnits;
			if (defaultUnits == null) {
				defaultUnits = LengthUnits.AxesKm;
			}
			String units = Config.get("shape.ellipse.axesUnits", defaultUnits.getConfigName());
			defaultUnits = LengthUnits.getEntryByConfigName(units);
			axesUnitBox.setSelectedItem(defaultUnits);
			
//			AngleUnits aUnits = geomSource.angleUnits;
//			String angleUnits = Config.get("shape.ellipse.angleUnits", aUnits.getConfigName());
//			aUnits = AngleUnits.getEntryByConfigName(angleUnits);
//			angleUnitBox.setSelectedItem(aUnits);
			
			if (geomSource.aAxisField != null) {
				numberFields.add(geomSource.aAxisField);
			} else {
				numberFields.add(aField);
			}
			if (geomSource.bAxisField != null) {
				numberFields.add(geomSource.bAxisField);
			} else {
				numberFields.add(bField);
			}
			if (geomSource.angleField != null) {
				numberFields.add(geomSource.angleField);
			} else {
				numberFields.add(angField);
			}
			if (geomSource.centerLatField != null) {
				numberFields.add(geomSource.centerLatField);
			} else {
				numberFields.add(latField);
			}
			if (geomSource.centerLonField != null) {
				numberFields.add(geomSource.centerLonField);
			} else {
				numberFields.add(lonField);
			}
			if (geomSource.meanAxisField != null) {
				numberFields.add(geomSource.meanAxisField);
			} else {
				numberFields.add(mField);
			}
			
		} else {
			numberFields.add(aField);
			numberFields.add(bField);
			numberFields.add(angField);
			numberFields.add(lonField);
			numberFields.add(latField);
			numberFields.add(mField);
		}
		
		Field[] numberFieldsArray = numberFields.toArray(new Field[numberFields.size()]);
		final JComboBox<Field> aAxisBox = new JComboBox<Field>(numberFieldsArray);
		aAxisBox.setSelectedItem((geomSource == null || geomSource.aAxisField == null) ? aField : geomSource.aAxisField);
		
		final JComboBox<Field> bAxisBox = new JComboBox<Field>(numberFieldsArray);
		bAxisBox.setSelectedItem((geomSource == null || geomSource.bAxisField == null) ? bField : geomSource.bAxisField);
			
		final JComboBox<Field> angleBox = new JComboBox<Field>(numberFieldsArray);
		angleBox.setSelectedItem((geomSource == null || geomSource.angleField == null) ? angField : geomSource.angleField);
		
		final JComboBox<Field> cenLatBox = new JComboBox<Field>(numberFieldsArray);
		cenLatBox.setSelectedItem((geomSource == null || geomSource.centerLatField == null) ? latField : geomSource.centerLatField);
		
		final JComboBox<Field> cenLonBox = new JComboBox<Field>(numberFieldsArray);
		cenLonBox.setSelectedItem((geomSource == null || geomSource.centerLonField == null) ? lonField : geomSource.centerLonField);
		
		final JComboBox<Field> meanAxisBox = new JComboBox<Field>(numberFieldsArray);
		meanAxisBox.setSelectedItem((geomSource == null || geomSource.meanAxisField == null) ? mField : geomSource.meanAxisField);
		
		final JDialog dialog = new JDialog(parent, "Geometry Options", true);
		
		// cancel button just hides the dialog
		JButton cancel = new JButton("Cancel".toUpperCase());
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
			}
		});
		
		JButton helpBtn = new JButton("HELP");
		helpBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JPanel panel = new JPanel();
				GroupLayout layout = new GroupLayout(panel);
				panel.setLayout(layout);
				layout.setAutoCreateContainerGaps(true);
				layout.setAutoCreateGaps(true);
				String summaryText = "<html>To draw an ellipse (does not apply to 5 point drawing): "
						+ "<ul><li>click where major axis should start, ghost outline appears</li><li>move the mouse to the end of the major axis</li>"
						+ "<li>use the scroll wheel to increase or decrease the size of the minor axis</li><li>move the mouse to adjust the rotation angle</li>"
						+ "<li>click anywhere in the main view to finish drawing the ellipse</li></ul> "
						+ "<br>When you click to finish drawing, you will see the actual ellipse feature with your style settings.<br><br>"
						+ "To edit an ellipse using the mouse controls: "
						+ "<ul><li>in select mode, click on an ellipse with no other features selected (including the ellipse you want to edit)</li>"
						+ "<li>you will see a \"ghost\" of the ellipse</li>"
						+ "<li>minor axis: scroll/mouse wheel</li>"
						+ "<li>major axis: scroll/mouse wheel with the shift key</li>"
						+ "<li>rotation angle: scroll/mouse wheel with the control key</li>"
						+ "<li>finish editing: click anywhere in the main view</li></ul> "
						+ "To edit an ellipse using the key controls: "
						+ "<ul><li>edit the minor axis: m and n keys</li>"
						+ "<li>major axis: j and k keys</li>"
						+ "<li>rotation angle: z and x keys</li>"
						+ "<li>finish editing: enter key</li>"
						+ "<li>cancel editing: escape key</li>"
						+ "</ul>Note: key and mouse controls can be used interchangeably.<br>"
						+ "Note: ellipses drawn with 5 point drawing are not editable.<br>"
						+ "Note: ellipse axes values are displayed in KM.</html>";
						
				JLabel title = new JLabel("Ellipse Drawing Information");
				JLabel summary = new JLabel(summaryText);
				JButton doneBtn = new JButton(new AbstractAction("Done") {
					
					@Override
					public void actionPerformed(ActionEvent arg0) {
						dialog.setVisible(true);
						dialog.dispose();
					}
				});
				
				layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
					.addComponent(title)	
					.addComponent(summary)
					.addComponent(doneBtn));
				layout.setVerticalGroup(layout.createSequentialGroup()
					.addComponent(title)	
					.addComponent(summary)
					.addComponent(doneBtn));
				
				JDialog helpDialog = new JDialog(dialog);
				helpDialog.setContentPane(panel);
				helpDialog.pack();
				helpDialog.setTitle("Ellipse Help");
				helpDialog.setLocationRelativeTo(dialog);
				helpDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				helpDialog.setVisible(true);
			}
		});
		
		
//		final JCheckBox circleCb = new JCheckBox("Draw Circle");
//		circleCb.setSelected(circleDefault);
		
		// ok button copies in the results
		JButton ok = new JButton("OK".toUpperCase());
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Field aAxisField = (Field) aAxisBox.getSelectedItem();
				Field bAxisField = (Field) bAxisBox.getSelectedItem();
				Field angleField = (Field) angleBox.getSelectedItem();
				LengthUnits lUnits = (LengthUnits)axesUnitBox.getSelectedItem();
				Config.set("shape.ellipse.axesUnits",lUnits.getConfigName());
//				AngleUnits aUnits = (AngleUnits)angleUnitBox.getSelectedItem();
//				Config.set("shape.ellipse.angleUnits",aUnits.getConfigName());
				Field cenLatField = (Field) cenLatBox.getSelectedItem();
				Field cenLonField = (Field) cenLonBox.getSelectedItem();
				Field mAxisField = (Field) meanAxisBox.getSelectedItem();
				
				GeomSource newSource = null;
				if (aAxisField == nullField || bAxisField == nullField || 
					angleField == nullField || lUnits == null ) {
					
					out.add(new StyleFieldSource<FPath>(Field.FIELD_PATH, null));
				} else{
				
					if(geomSource==null){
						newSource = new GeomSource(null, null, aAxisField, bAxisField, angleField, lUnits, AngleUnits.Degrees, cenLatField, cenLonField, mAxisField);
						out.add(newSource);
						
					}else if (geomSource.aAxisField != aAxisField ||geomSource.bAxisField != bAxisField 
							||geomSource.angleField != angleField || geomSource.lengthUnits != lUnits
							||geomSource.meanAxisField != mAxisField) {
						newSource = new GeomSource(geomSource.radiusField, geomSource.units, aAxisField, 
								bAxisField, angleField, lUnits, AngleUnits.Degrees, cenLatField, cenLonField, mAxisField);
						out.add(newSource);
					}
				}
				
				//commenting out the next block to not scale each feature with the new selection
//				if (axesUnitUpdated.get()) {
//					if (newSource != null) {
//						int[] rows = shapeLayer.getFileTable().getSelectedRows();
//						FeatureCollection fc = shapeLayer.getFileTable().getFileTableModel().get(rows[0]);//TODO: is this right? test with multiple files?
//						for (Feature f : fc.getFeatures()) {
//							if (FeatureUtil.isEllipseSource(geomSource, f)) {
//								Shape ellipse = geomSource.getEllipseShapeForEdit(f, 0.0, 0.0, 0.0, 0.0, 0.0, projection);
//								
////								double tempA = GeomSource.scaleAxisFieldToKM((Double) f.getAttribute(geomSource.bAxisField), geomSource.getAxesUnits());
////								double tempB = GeomSource.scaleAxisFieldToKM((Double) f.getAttribute(geomSource.bAxisField), geomSource.getAxesUnits());
////								double tempC = (tempA + tempB) / 2;//these are always in km
////								tempC /= newSource.getAxesUnits().getScale();
////								newSource.setFieldValue(f, geomSource.aAxisField, tempA, false);
////								newSource.setFieldValue(f, geomSource.bAxisField, tempB, false);
////								f.setAttribute(geomSource.getMeanAxisField(), tempC);
//							}
//							
//						}
//					}
//					
//					
//				}
				
				dialog.setVisible(false);
			}
		});
		
		JLabel aAxisLbl = new JLabel("A Axis Field");
		JLabel bAxisLbl = new JLabel("B Axis Field");
		JLabel centerLatLbl = new JLabel("Center Lat Field");
		JLabel centerLonLbl = new JLabel("Center Lon Field");
		JLabel rotationLbl = new JLabel("Rotation Angle Field");
//		JLabel scaleLbl = new JLabel("Axis Scale");
//		JLabel defaultShapeLbl = new JLabel("Starting Shape");
		JLabel mAxisLbl = new JLabel("Mean Axis Field");
		
		
		
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
			.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(aAxisLbl)
					.addComponent(bAxisLbl)
					.addComponent(centerLatLbl)
					.addComponent(centerLonLbl)
					.addComponent(rotationLbl)
//					.addComponent(scaleLbl)
					.addComponent(mAxisLbl))
//					.addComponent(defaultShapeLbl)
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(aAxisBox)
					.addComponent(bAxisBox)
					.addComponent(cenLatBox)
					.addComponent(cenLonBox)
					.addComponent(angleBox)
//					.addComponent(axesUnitBox)
					.addComponent(meanAxisBox)))
//					.addComponent(defaultShapeBox)
				.addGroup(layout.createSequentialGroup()
					.addComponent(cancel)
					.addComponent(ok)
					.addComponent(helpBtn)));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(aAxisLbl)
				.addComponent(aAxisBox))
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(bAxisLbl)
				.addComponent(bAxisBox))
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(centerLatLbl)
				.addComponent(cenLatBox))
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(centerLonLbl)
				.addComponent(cenLonBox))
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(mAxisLbl)
				.addComponent(meanAxisBox))
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(rotationLbl)
				.addComponent(angleBox))
//			.addGroup(layout.createParallelGroup(Alignment.CENTER)
//				.addComponent(scaleLbl)
//				.addComponent(axesUnitBox))
//			.addGroup(layout.createParallelGroup(Alignment.CENTER)
//				.addComponent(defaultShapeLbl)
//				.addComponent(defaultShapeBox))
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(cancel)
				.addComponent(ok)
				.addComponent(helpBtn)));
		
		dialog.setLocationRelativeTo(parent);
		dialog.setContentPane(panel);
		dialog.pack();
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
//		axesUnitBox.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				if (!axesUnitUpdated.get()) {
//					Util.showMessageDialog("Note: All rows will be updated for this change.");
//					axesUnitUpdated.set(true);
//				}
//			}
//		});
		dialog.setVisible(true);
			
		return out.isEmpty() ? null : out.get(0);
	}
	
	/**
	 * Sets the given radius onto this feature. Handles converting from
	 * kilometer radius to the units and type stored on the feature.
	 */
	public void setRadius(Feature f, double km) {
		km /= units.getScale();
		Number radius;
		if (Integer.class.isAssignableFrom(radiusField.type)) {
			radius = (int)(km);
		} else if (Float.class.isAssignableFrom(radiusField.type)) {
			radius = (float)(km);
		} else if (Double.class.isAssignableFrom(radiusField.type)) {
			radius = (double)(km);
		} else {
			throw new IllegalStateException("Radius has unsupported type " + radiusField.type.getName());
		}
		f.setAttribute(radiusField, radius);
	}
	
	/**
	 * Sets the given value onto this feature (either a axis, b axis or angle).
	 * Handles converting from kilometer radius to the units and type stored on the feature.
	 */
	public void setFieldValue(Feature f, Field field, double val, boolean quietFlag) {
		//only scale for the axes, not the angle
		if(aAxisField == field || bAxisField == field){
			val /= lengthUnits.getScale();
		}
//		if (angleField == field) {
//			val /= angleUnits.getScale();
//		}
		Number numberValue;
		
		if (Integer.class.isAssignableFrom(field.type)) {
			numberValue = (int)(val);
		} else if (Float.class.isAssignableFrom(field.type)) {
			numberValue = (float)(val);
		} else if (Double.class.isAssignableFrom(field.type)) {
			numberValue = (double)(val);
		} else {
			throw new IllegalStateException(field.toString()+" has unsupported type " + field.type.getName());
		}
		

		Field tempField = null;
		if (aAxisField == field || bAxisField == field || angleField == field || centerLonField == field || centerLatField == field) {
			tempField = field;
		} else {
			throw new IllegalArgumentException("Invalid field set on geom source feature.");
		}
		
		if (quietFlag) {
			f.setAttributeQuiet(tempField, numberValue);
		} else {
			f.setAttribute(tempField, numberValue);
		}
	}
	
	
	
	/** Provides converters from common methods of describing circle size to the one internal form that we need. */
	public static enum Units implements Serializable {
		RadiusKm(1.0, "Radius (km)"),
		RadiusMeters(.001, "Radius (m)"),
		RadiusMiles(1.609344, "Radius (mi)"),
		RadiusFeet(0.0003048, "Radius (ft)"),
		DiameterKm(1.0/2, "Diameter (km)"),
		DiameterMeters(.001/2, "Diameter (m)"),
		DiameterMiles(1.609344/2, "Diameter (mi)"),
		DiameterFeet(0.0003048/2, "Diameter (ft)");
		private final double scale;
		private final String name;
		private Units(double scale, String name) {
			this.scale = scale;
			this.name = name;
		}
		public double getScale() {
			return scale;
		}
		public String toString() {
			return name;
		}
	}
	
	public static enum LengthUnits implements Serializable {
		AxesKm(1.0, "Axes Length (km)", "km"),
		AxesMeters(.001, "Axes Length (m)", "meters"),
		AxesMiles(1.609344, "Axes Length (mi)", "miles"),
		AxesFeet(0.0003048, "Axes Length (ft)", "feet");
		private final double scale;
		private final String name;
		private final String configName;
		private LengthUnits(double scale, String name, String cName) {
			this.scale = scale;
			this.name = name;
			this.configName = cName;
		}
		public double getScale() {
			return scale;
		}
		public String toString() {
			return name;
		}
		public String getConfigName() {
			return configName;
		}
		public static LengthUnits getEntryByConfigName(String val) {
			LengthUnits returnEntry = null;
			if ("km".equalsIgnoreCase(val)) {
				returnEntry = AxesKm;
			} else if ("meters".equalsIgnoreCase(val)) {
				returnEntry = AxesMeters;
			} else if ("miles".equalsIgnoreCase(val)) {
				returnEntry = AxesMiles;
			} else if ("feet".equalsIgnoreCase(val)) {
				returnEntry = AxesFeet;
			}
			return returnEntry;
		}
	}
	
	public static enum AngleUnits implements Serializable {
		Radians(1.0, "Rotation Angle (rad)", "radians"),
		Degrees(180/Math.PI, "Rotation Angle (deg)", "degrees");
		private final double scale;
		private final String name;
		private final String configName;
		private AngleUnits(double scale, String name, String cName) {
			this.scale = scale;
			this.name = name;
			this.configName = cName;
		}
		public double getScale() {
			return scale;
		}
		public String toString() {
			return name;
		}
		public String getConfigName() {
			return configName;
		}
		public static AngleUnits getEntryByConfigName(String val) {
			AngleUnits returnVal = null;
			if ("radians".equalsIgnoreCase(val)) {
				returnVal = Radians;
			} else if ("degrees".equals(val)) {
				returnVal = Degrees;
			}
			return returnVal;
		}
	}
}

