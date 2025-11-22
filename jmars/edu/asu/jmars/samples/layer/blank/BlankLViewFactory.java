package edu.asu.jmars.samples.layer.blank;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;

public class BlankLViewFactory extends LViewFactory {

	public LView createLView() {
		return null;
	}

	public void createLView(boolean async) {
		// Create LView with defaults
		BlankLView lview = new BlankLView(new BlankLayer());
		lview.originatingFactory = this;
		LManager.receiveNewLView(lview);
	}

	public LView recreateLView(SerializedParameters parmBlock) {
		BlankLView lview = new BlankLView(new BlankLayer());
		lview.originatingFactory = this;
		return lview;
	}

	public String getDesc() {
		return "Blank LView Description";
	}

	public String getName() {
		return "Blank LView";
	}
}
