package edu.asu.jmars.swing.quick.edit.row;

import javax.swing.JCheckBoxMenuItem;

import edu.asu.jmars.layer.LManager;

public class CommandReceiver {

	public CommandReceiver() {
	}

	public void open() {
		LManager.getLManager().accessSelectedOptions(false);
	}

	public void openDocked() {
		LManager.getLManager().accessSelectedOptions(true);
	}

	public void rename() {
		LManager.getLManager().renameSelectedLayer();
	}

	public void delete() {
		LManager.getLManager().deleteSelectedLayer();
	}
	
	public void showTooltip(JCheckBoxMenuItem eventsource) {		
		LManager.getLManager().showTooltipForSelectedLayer(eventsource.isSelected());
	}
}
