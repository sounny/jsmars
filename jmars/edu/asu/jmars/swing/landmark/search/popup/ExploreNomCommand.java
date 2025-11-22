package edu.asu.jmars.swing.landmark.search.popup;

import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class ExploreNomCommand implements QuickMenuCommand {

	CommandReceiver cr;	
	
	public ExploreNomCommand(CommandReceiver cr) {		
		this.cr = cr;
	}

	@Override
	public void execute() {
	    cr.exploreNomenclature();
	}

}
