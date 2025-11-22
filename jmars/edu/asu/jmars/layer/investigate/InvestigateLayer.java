package edu.asu.jmars.layer.investigate;

import java.util.ArrayList;

import edu.asu.jmars.layer.*;

public class InvestigateLayer extends Layer{

	private ArrayList<DataSpike> dataSpikes;
	private ArrayList<DataProfile> dataProfiles;
	
	public static final int IMAGES_BUFFER=0;
	public static final int LABELS_BUFFER=1;
	
	
	public InvestigateLayer(){
		this(new ArrayList<DataSpike>(), new ArrayList<DataProfile>());
	}
	
	//used in reloading sessions and layers
	public InvestigateLayer(ArrayList<DataSpike> ds, ArrayList<DataProfile> dp){
		dataSpikes = ds;
		dataProfiles = dp;
		
		//initiate the stateId to the proper buffers and start them at 0.
		stateIds = new ArrayList<Integer>();
		stateIds.add(IMAGES_BUFFER, 0);
		stateIds.add(LABELS_BUFFER, 0);
	}
	
	
	@Override
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
		// TODO Auto-generated method stub
		
	}
	

	public ArrayList<DataSpike> getDataSpikes(){
		return dataSpikes;
	}
	
	public void addDataSpike(DataSpike newDS){
		dataSpikes.add(newDS);
		
		//increase states
		increaseStateId(IMAGES_BUFFER);
		increaseStateId(LABELS_BUFFER);
	}
	
	public ArrayList<DataProfile> getDataProfiles(){
		return dataProfiles;
	}
}
