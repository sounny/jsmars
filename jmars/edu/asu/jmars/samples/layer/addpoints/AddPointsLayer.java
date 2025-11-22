package edu.asu.jmars.samples.layer.addpoints;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;

public class AddPointsLayer extends Layer {
	List<Point2D> pts = new ArrayList<Point2D>();
	
	public AddPointsLayer(){
	}
	
	public void addPoint(Point2D newPoint){
		pts.add((Point2D)newPoint.clone());
		broadcast(Collections.unmodifiableList(pts));
	}
	
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
		// Layer can filter the data such that it lives within the layerRequest boundary,
		// if it so chooses.
		broadcast(Collections.unmodifiableList(pts));
	}
}
