package edu.asu.jmars.layer.stamp.radar;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.stamp.FilledStamp;
import edu.asu.jmars.layer.stamp.StampImage;
import edu.asu.jmars.layer.stamp.StampImageFactory;
import edu.asu.jmars.layer.stamp.StampShape;

public class FilledStampRadarType extends FilledStamp{
	private String fullResURL;
	private BufferedImage fullResImage;
	private ArrayList<RadarHorizon> horizons;
	
	public FilledStampRadarType(StampShape stamp, StampImage pdsi, FilledStamp.State state) {
		super(stamp, pdsi, state);

		horizons = new ArrayList<RadarHorizon>();
		
		if(state instanceof FilledStampRadarType.State){
			FilledStampRadarType.State fstate = (FilledStampRadarType.State)state;
			fullResURL = fstate.getURL();
			fullResImage = StampImageFactory.getImage(fullResURL, false);
			horizons = fstate.getHorizons();
		}
	}
	
	
	
	public State getState(){
		State state = new State();
		
		if(stamp != null){
			state.id = stamp.getId();
		}
		
		state.setImageType("SHARAD");
		state.setURL(fullResURL);
		state.setHorizons(horizons);
		
		return state;
	}
	
    public BufferedImage getFullResImage(){
    	return fullResImage;
    }
    
    public void addHorizon(RadarHorizon newHorizon){
    	horizons.add(newHorizon);
    }
    
    public void removeHorizon(RadarHorizon horizon){
    	horizons.remove(horizon);
    }
    
    public ArrayList<RadarHorizon> getHorizons(){
    	return horizons;
    }
	
	
    /**
     * Minimal description of state needed to recreate
     * a FilledStamp.
     */
    public static class State extends FilledStamp.State implements SerializedParameters {
    	private static final long serialVersionUID = -2396089407110933527L;
    	
    	private String fullResLocation;
    	private ArrayList<RadarHorizon> horizons;
    	
    	public State(){
    		fullResLocation = null;
    		imageType = null;
    		horizons = new ArrayList<RadarHorizon>();
    	}
    	
    	public State(String fullRes, String type){
    		fullResLocation = fullRes;
    		imageType = type;
    		horizons = new ArrayList<RadarHorizon>();
    	}
    	
    	
    	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    		ois.defaultReadObject();
    	}
    	
    	private String getURL(){
    		return fullResLocation;
    	}
    	
    	private ArrayList<RadarHorizon> getHorizons(){
    		return horizons;
    	}
    	
    	private void setURL(String url){
    		fullResLocation = url;
    	}
    	
    	private void setImageType(String type){
    		imageType = type;
    	}
  
    	private void setHorizons(ArrayList<RadarHorizon> h){
    		horizons = h;
    	}
    }
}
