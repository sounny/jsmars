package edu.asu.jmars.layer.mcd;

import java.util.ArrayList;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;

public class MCDLayer extends Layer{

	private ArrayList<MCDDataPoint> dataPoints;
	
	protected static String SCRIPT_VERSION="2.0";
	protected static String POINT_MODE_PROCESS="point_mode";
	
	public static final int IMAGES_BUFFER=0;
	public static final int LABELS_BUFFER=1;
	
	
	public MCDLayer(){
		//instantiate the data arraylist
		dataPoints = new ArrayList<MCDDataPoint>();
		
		//initiate the stateId to the proper buffers and start them at 0.
		stateIds = new ArrayList<Integer>();
		stateIds.add(IMAGES_BUFFER, 0);
		stateIds.add(LABELS_BUFFER, 0);
	}
	
	@Override
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
	}
	
	/**
	 * @return A list of all the MCD data points that have been
	 * created in this layer
	 */
	public ArrayList<MCDDataPoint> getMCDDataPoints(){
		return dataPoints;
	}
	
	/**
	 * Add a new MCD Data Point to the list of data points
	 * contained by this layer
	 * @param newPoint
	 */
	public void addDataPoint(MCDDataPoint newPoint){
		dataPoints.add(newPoint);
		
		//increase states
		increaseStateId(MCDLayer.IMAGES_BUFFER);
		increaseStateId(MCDLayer.LABELS_BUFFER);
	}
	
	/**
	 * Remove the MCD Data Point from the list of data points
	 * contained by this layer
	 * @param pointToRemove
	 */
	public void removeDataPoint(MCDDataPoint pointToRemove){
		dataPoints.remove(pointToRemove);
		
		//increase state
		increaseStateId(MCDLayer.IMAGES_BUFFER);
		increaseStateId(MCDLayer.LABELS_BUFFER);
	}
	
	/**
	 * Used when editing a MCD data point. Replace the old point
	 * with the new point
	 * @param oldPoint  Point before editing
	 * @param newPoint  Point after editing
	 */
	public void replaceDataPoint(MCDDataPoint oldPoint, MCDDataPoint newPoint){
		int index = dataPoints.indexOf(oldPoint);
		dataPoints.remove(oldPoint);
		dataPoints.add(index, newPoint);
	}
	
	/**
	 * Checks to see if the name is already in use from another
	 * MCD data point.  If the name is not in use, returns true
	 * otherwise, returns false.
	 * @param name Name to compare against existing data points
	 * 
	 * @return True if the name is not being used, false if it's
	 * already being used
	 */
	public boolean isUniqueName(String name){
		boolean result = true;
		
		for(MCDDataPoint dp : dataPoints){
			if(dp.getName().equals(name)){
				result = false;
				break;
			}
		}
		
		return result;
	}

}
