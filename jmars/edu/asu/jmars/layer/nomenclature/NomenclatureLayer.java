package edu.asu.jmars.layer.nomenclature;

import java.util.ArrayList;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;

public class NomenclatureLayer extends Layer {
	public static final int IMAGES_BUFFER=0;
	public static final int LABELS_BUFFER=1;
	
	public NomenclatureLayer() {
		//initiate the stateId to the proper buffers and start them at 0.
		stateIds = new ArrayList<Integer>();
		stateIds.add(IMAGES_BUFFER, 0);
		stateIds.add(LABELS_BUFFER, 0);
	}
	
	
	@Override
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
	}

}
