package edu.asu.jmars.samples.layer.map2.stages.threshold;

import java.awt.BorderLayout;

import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.samples.layer.threshold.ThresholdLView;
import edu.asu.jmars.samples.layer.threshold.ThresholdLayer;

public class ThresholdFocusPanel extends FocusPanel {
	ThresholdLView lview;
	
	public ThresholdFocusPanel(ThresholdLView parent) {
		super(parent);
		this.lview = parent;

		buildUI();
	}

	private void buildUI(){
		setLayout(new BorderLayout());
		add(((ThresholdLayer)lview.getLayer()).getThresholdSettings().createStageView().getStagePanel(), BorderLayout.NORTH);
	}
}
