package edu.asu.jmars.layer.grid;

import java.util.ArrayList;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;

/**
 * Creates a layer object for the Lat/Lon Grid.
 *
 */
public class GridLayer extends Layer {

	public GridLayer(){
		//initiate the stateId to one number, and start it at 0.
		stateIds = new ArrayList<Integer>();
		stateIds.add(0);
	}
	
    @Override
    public void receiveRequest(Object layerRequest, DataReceiver requester) {
    }

}
