package edu.asu.jmars.samples.layer.blank;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;

public class BlankLayer extends Layer {

	public void receiveRequest(Object layerRequest, DataReceiver requester) {
		broadcast(layerRequest);
	}

}
