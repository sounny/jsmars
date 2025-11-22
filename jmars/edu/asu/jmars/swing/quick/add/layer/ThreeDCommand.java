package edu.asu.jmars.swing.quick.add.layer;

import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class ThreeDCommand implements QuickMenuCommand {
	
	CommandReceiver cr;

	public ThreeDCommand(CommandReceiver cr) {		
		this.cr = cr;
	}

	@Override
	public void execute() {
		cr.load3DLayer(true);
	}

}
