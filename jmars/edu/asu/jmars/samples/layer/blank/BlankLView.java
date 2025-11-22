package edu.asu.jmars.samples.layer.blank;

import java.awt.geom.Rectangle2D;

import edu.asu.jmars.layer.Layer.LView;

public class BlankLView extends LView {
	public BlankLView(BlankLayer layer){
		super(layer);
	}
	
	protected LView _new() {
		// Create a copy of ourself for use in the panner-view.
		return new BlankLView((BlankLayer)getLayer());
	}

	protected Object createRequest(Rectangle2D where) {
		// Build a request object for the layer.
		// The layer will respond back with the data.
		return where;
	}

	public void receiveData(Object layerData) {
		// Process the data returned by the layer.
		// Including displaying the data to the screen.
		Rectangle2D r = (Rectangle2D)layerData;
		System.out.println("Layer returned: [x="+r.getMinX()+",y="+r.getMinY()+",w="+r.getWidth()+",h="+r.getHeight()+"]");
	}
	
	public String getName() {
		return "Blank";
	}

}
