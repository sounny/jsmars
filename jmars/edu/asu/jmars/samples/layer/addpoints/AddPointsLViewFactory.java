package edu.asu.jmars.samples.layer.addpoints;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;

public class AddPointsLViewFactory extends LViewFactory {

	public LView createLView() {
		return null;
	}

	public void createLView(boolean async) {
		// Create LView with defaults
		AddPointsLView lview = new AddPointsLView(new AddPointsLayer());
		lview.originatingFactory = this;
		LManager.receiveNewLView(lview);
	}

	public LView recreateLView(SerializedParameters parmBlock) {
		AddPointsLView lview = new AddPointsLView(new AddPointsLayer());
		lview.originatingFactory = this;
		return lview;
	}

	public String getDesc() {
		return "AddPoints LView Description";
	}

	public String getName() {
		return "AddPoints LView";
	}
}
