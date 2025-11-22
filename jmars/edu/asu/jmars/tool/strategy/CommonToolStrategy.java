package edu.asu.jmars.tool.strategy;

import edu.asu.jmars.lmanager.AddLayerDialog;
import edu.asu.jmars.swing.landmark.search.LandmarkSearchPanel;

public class CommonToolStrategy implements ToolStrategy {

	@Override
	public void doMode(int newmode, int oldmode) {
	}

	@Override
	public void preMode(int newmode, int oldmode) {
		LandmarkSearchPanel.closeSearchDialog();
		if (AddLayerDialog.getInstance().isShowing()) {
			// Hide the add layer dialog if it is showing and do not propagate the mouse
			// event.
			AddLayerDialog.getInstance().closeAddLayerDialog();
			return;
		}
	}

	@Override
	public void postMode(int newmode, int oldmode) {
	}
}
