package edu.asu.jmars.swing.landmark.search.popup;

import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class SearchPlacesCommand implements QuickMenuCommand {
	CommandReceiver cr;	
	
	public SearchPlacesCommand(CommandReceiver cr) {		
		this.cr = cr;
	}

	@Override
	public void execute() {
	    cr.searchPlaces();
	}
}
