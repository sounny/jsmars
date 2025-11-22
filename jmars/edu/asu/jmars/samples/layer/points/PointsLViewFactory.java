package edu.asu.jmars.samples.layer.points;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;

public class PointsLViewFactory extends LViewFactory {

	public LView createLView() {
		return null;
	}

	public void createLView(boolean async) {
		// Create LView with defaults
		PointsLView lview = new PointsLView(new PointsLayer());
		lview.originatingFactory = this;
		LManager.receiveNewLView(lview);
	}

	public LView recreateLView(SerializedParameters parmBlock) {
		PointsLView lview = new PointsLView(new PointsLayer());
		lview.originatingFactory = this;
		return lview;
	}

	public String getDesc() {
		return "Points LView Description";
	}

	public String getName() {
		return "Points LView";
	}

}
