package edu.asu.jmars.layer.crater;


import java.util.ArrayList;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;

public class CraterLayer extends Layer {

	public void receiveRequest(Object layerRequest, DataReceiver requester) {
		broadcast(layerRequest);
	}
	
	public CraterSettings settings;
	private ArrayList<Crater> selectedCraters = new ArrayList<Crater>();
	private ArrayList<Crater> matchingCraters = new ArrayList<Crater>();
	
	public CraterLayer() {
		this(new CraterSettings());
	}
	
	public CraterLayer(CraterSettings newSettings) {
		super();
		settings=newSettings;
		
		//initiate the stateId to one number, and start it at 0.
		stateIds = new ArrayList<Integer>();
		stateIds.add(0);
	}
	
	public ArrayList<Crater> getSelectedCraters(){
		return selectedCraters;
	}
	
	public ArrayList<Crater> getMatchingCraters(){
		return matchingCraters;
	}
}
