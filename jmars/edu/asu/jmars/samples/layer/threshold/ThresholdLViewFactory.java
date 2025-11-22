package edu.asu.jmars.samples.layer.threshold;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;

public class ThresholdLViewFactory extends LViewFactory {

	public LView createLView() {
		return null;
	}

	public void createLView(boolean async) {
		// Create LView with defaults
		ThresholdLView lview = new ThresholdLView(new ThresholdLayer());
		lview.originatingFactory = this;
		LManager.receiveNewLView(lview);
	}

	public LView recreateLView(SerializedParameters parmBlock) {
		ThresholdLView lview = new ThresholdLView(new ThresholdLayer());
		lview.originatingFactory = this;
		return lview;
	}

	public String getDesc() {
		return "Threshold LView Description";
	}

	public String getName() {
		return "Threshold LView";
	}

}
