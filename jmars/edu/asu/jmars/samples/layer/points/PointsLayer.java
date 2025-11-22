package edu.asu.jmars.samples.layer.points;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;

public class PointsLayer extends Layer {
	List<Point2D> pts = new ArrayList<Point2D>();
	
	public PointsLayer(){
		generateData();
	}
	
	private void generateData(){
		pts.add(new Point2D.Double(-1, -1));
		pts.add(new Point2D.Double(1, -1));
		pts.add(new Point2D.Double(1, 1));
		pts.add(new Point2D.Double(-1, 1));
	}
	
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
		// Layer can filter the data such that it lives within the layerRequest boundary,
		// if it so chooses.
		broadcast(Collections.unmodifiableList(pts));
	}

}
