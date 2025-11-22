package edu.asu.jmars.layer.north;

import java.awt.geom.Point2D;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.MultiProjection;

public class NorthLayer extends Layer{
	private NorthArrow arrow;
	private NorthSettings settings;
	
	public NorthLayer( NorthSettings params){
		settings = params;
		//create north arrow
		arrow = new NorthArrow(settings.arrowSize);
	}
	
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
	}
	

	/**
	 * Get the angle which north arrow needs to point.
	 * This will be non-0 if the projection has changed
	 * and the pole is now not directly "up" anymore.
	 * @return  Angle in radians
	 */
	public double getAngleForArrow(){
		//center point of screen in world coords
		Point2D startPt = Main.testDriver.locMgr.getLoc();
		Point2D spatialCenter = Main.PO.convWorldToSpatial(startPt);
		Point2D spatialEnd = new Point2D.Double(spatialCenter.getX(), spatialCenter.getY()+1);
		Point2D end = Main.PO.convSpatialToWorld(spatialEnd);
		
		//difference between start and finish should never be very large 
		// (couple degrees at most) so if it's larger than that, it's 
		// because of wrapping world points, so add 360 to the smaller 
		// x value to compensate.
		double worldXDiff = Math.abs(startPt.getX() - end.getX());
		if(worldXDiff>180){
			if(startPt.getX()<end.getX()){
				startPt = new Point2D.Double(startPt.getX()+360, startPt.getY());
			}
			else if(end.getX()<startPt.getX()){
				end = new Point2D.Double(end.getX()+360, end.getY());
			}
		}
		
		MultiProjection mp = Main.testDriver.mainWindow.getProj();
		Point2D screenStart = mp.world.toScreen(startPt);
		Point2D screenEnd = mp.world.toScreen(end);
		
		double angle = Math.atan2(screenStart.getX()-screenEnd.getX(), screenStart.getY()-screenEnd.getY());

		
		return angle;
	}
	
	
	/**
	 * @return  The North Arrow for this layer
	 */
	public NorthArrow getArrow(){
		return arrow;
	}
	
	/**
	 * @return  The settings object for this layer
	 */
	public NorthSettings getSettings(){
		return settings;
	}
}
