package edu.asu.jmars.swing.quick.add.layer;

import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class CustomMapsCommand implements QuickMenuCommand {
	CommandReceiver cr;	
	
	public CustomMapsCommand(CommandReceiver cr) {		
		this.cr = cr;
	}

	@Override
	public void execute() {
		cr.loadCustomMaps();
	}

}
