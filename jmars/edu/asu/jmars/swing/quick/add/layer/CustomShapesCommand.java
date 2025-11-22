package edu.asu.jmars.swing.quick.add.layer;

import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class CustomShapesCommand implements QuickMenuCommand {

	CommandReceiver cr;	
	
	public CustomShapesCommand(CommandReceiver cr) {		
		this.cr = cr;
	}

	@Override
	public void execute() {
		cr.loadCustomShapesLayer();
	}

}
