package edu.asu.jmars.layer.map2;

import javax.swing.JPanel;

public interface StageView {
	/**
	 * Returns the JPanel containing the UI for this stage.
	 * This is never null.
	 */
	public JPanel getStagePanel();
	
	/**
	 * Returns the StageSettings that this StageView is backed by.
	 */
	public StageSettings  getSettings();
}
