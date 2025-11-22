package edu.asu.jmars.layer.stamp;

import edu.asu.jmars.layer.SerializedParameters;


public class FilledStamp {

    public StampShape stamp;
    public StampImage pdsi;
  
    
    public FilledStamp(StampShape stamp, StampImage pdsi, State state)
    {
		this.stamp = stamp;
		this.pdsi = pdsi;

		// Make sure we actually do something with state data!
		if (state!=null) {
			pdsi.imageType = state.imageType;
		}
    }
    
    public String toString() {
    	String str = stamp.getTooltipText() + " : " + pdsi.imageType;
    	
    	String separator = stamp.stampLayer.getParam(stamp.stampLayer.RENDER_MENU_SEPARATOR);
    	
    	if (separator!=null && separator.length()>0) {
    		str=str.replace(separator, " - ");
    	}
    	
    	return str; 
    }
    
    public State getState(){
		State state = new State();
		
		if (stamp != null){
		    state.id = stamp.getId();
		}
		
		state.imageType = pdsi.imageType;
		return state;
    }
    
    
    /**
     * Minimal description of state needed to recreate
     * a FilledStamp.
     */
    public static class State implements SerializedParameters {
    	private static final long serialVersionUID = -2396089407110933527L;
    	public String id;
    	//ideally there should only be a colorMap instance in the FilledStampImageType.State class,
    	// but to preserve the restoration of old sessions we need to have an instance here too.
    	byte[] colorMap;
    	protected String imageType;

    	public String getImagetype() {
    		return imageType;
    	}
    }
}
