package edu.asu.jmars.layer.krc;

import java.util.ArrayList;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.map2.MapSource;

public class KRCLayer extends Layer{

	private ArrayList<KRCDataPoint> dataPoints;
	private MapSource elevSource;
	private MapSource albedoSource;
	private MapSource tiSource;
	private MapSource slopeSource;
	private MapSource azimuthSource;
	private int ppd;
	
	protected static String SCRIPT_VERSION="1.0";
	protected static String POINT_MODE_PROCESS="point_mode";
	
	public static final int IMAGES_BUFFER=0;
	public static final int LABELS_BUFFER=1;
	
	public KRCLayer(ArrayList<MapSource> mapSources){
		//instantiate the data arraylist
		dataPoints = new ArrayList<KRCDataPoint>();
		//set the sources for map sampling
		for(MapSource source : mapSources){
//			System.out.println("source: "+source);
			String name = source.getName().toLowerCase();
			//set the sources based off their names
			if(name.contains("elev") || name.contains("topo")){
				elevSource = source;
			}
			else if(name.contains("albedo")){
				albedoSource = source;
			}
			else if(name.contains("inertia")){
				tiSource = source;
			}
			else if(name.contains("slope")){
				slopeSource = source;
			}
			else if(name.contains("aspect") || name.contains("azimuth")){
				azimuthSource = source;
			}
			else{
				System.err.println("Unable to set source for KRC, did not match input types: "+name);
			}
		}
		
		//set the ppd level to something reasonable
		ppd = 1024;
		
		//initiate the stateId to the proper buffers and start them at 0.
		stateIds = new ArrayList<Integer>();
		stateIds.add(IMAGES_BUFFER, 0);
		stateIds.add(LABELS_BUFFER, 0);
	}
	
	
	public MapSource getElevationSource(){
		return elevSource;
	}
	
	public MapSource getAlbedoSource(){
		return albedoSource;
	}
	
	public MapSource getThermalInertiaSource(){
		return tiSource;
	}
	
	public MapSource getSlopeSource(){
		return slopeSource;
	}
	
	public MapSource getAzimuthSource(){
		return azimuthSource;
	}
	
	public int getPPD(){
		return ppd;
	}
	
	@Override
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
	}
	
	/**
	 * @return A list of all the KRC data points that have been
	 * created in this layer
	 */
	public ArrayList<KRCDataPoint> getKRCDataPoints(){
		return dataPoints;
	}
	
	/**
	 * Add a new KRC Data Point to the list of data points
	 * contained by this layer
	 * @param newPoint
	 */
	public void addDataPoint(KRCDataPoint newPoint){
		dataPoints.add(newPoint);
		
		//increase states
		increaseStateId(KRCLayer.IMAGES_BUFFER);
		increaseStateId(KRCLayer.LABELS_BUFFER);
	}
	
	/**
	 * Remove the KRC Data Point from the list of data points
	 * contained by this layer
	 * @param pointToRemove
	 */
	public void removeDataPoint(KRCDataPoint pointToRemove){
		dataPoints.remove(pointToRemove);
		
		//increase state
		increaseStateId(KRCLayer.IMAGES_BUFFER);
		increaseStateId(KRCLayer.LABELS_BUFFER);
	}
	
	/**
	 * Used when editing a krc data point. Replace the old point
	 * with the new point
	 * @param oldPoint  Point before editing
	 * @param newPoint  Point after editing
	 */
	public void replaceDataPoint(KRCDataPoint oldPoint, KRCDataPoint newPoint){
		int index = dataPoints.indexOf(oldPoint);
		dataPoints.remove(oldPoint);
		dataPoints.add(index, newPoint);
	}
	
	/**
	 * Checks to see if the name is already in use from another
	 * krc data point.  If the name is not in use, returns true
	 * otherwise, returns false.
	 * @param name Name to compare against existing data points
	 * 
	 * @return True if the name is not being used, false if it's
	 * already being used
	 */
	public boolean isUniqueName(String name){
		boolean result = true;
		
		for(KRCDataPoint dp : dataPoints){
			if(dp.getName().equals(name)){
				result = false;
				break;
			}
		}
		
		return result;
	}

}
