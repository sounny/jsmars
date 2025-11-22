package edu.asu.jmars.swing.landmark.search.popup;

import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class SearchNomCommand implements QuickMenuCommand {
	CommandReceiver cr;	
	
	public SearchNomCommand(CommandReceiver cr) {		
		this.cr = cr;
	}

	@Override
	public void execute() {
	    cr.searchLandmarks();
	}
}
