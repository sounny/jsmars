package edu.asu.jmars.samples.layer.test;

import edu.asu.jmars.layer.*;
import edu.asu.jmars.util.*;
public class LatLayer extends Layer
 {
	private static DebugLog log = DebugLog.instance();

	/**
	 ** The only data this layer maintains is the current latitude of
	 ** the line to draw.
	 **/
	public Double lat = new Double(0);

	/**
	 ** Responds to null requests by sending the requestor the current
	 ** latitude, as a {@link Double}.
	 **
	 ** <p>Responds to recieved {@link Double} requests by saving and
	 ** {@link #broadcast}ing it as the new latitude.
	 **/
	public void receiveRequest(Object layerRequest,
							   DataReceiver requestor)
	 {
		// Respond to a null request by sending the current latitude
		if(layerRequest == null)
			requestor.receiveData(lat);

		// Respond to a Double by setting the latitude, broadcast the change
		else if(layerRequest instanceof Double)
		 {
			double newLat = ( (Double) layerRequest ).doubleValue();
			newLat = Math.round(newLat);
			if(newLat != lat.doubleValue())
			 {
				lat = new Double(newLat);
				broadcast(lat);
			 }
		 }

		// Should never occur
		else
			log.aprintln("BAD REQUEST CLASS: " +
						 layerRequest.getClass().getName());
	 }
 }
