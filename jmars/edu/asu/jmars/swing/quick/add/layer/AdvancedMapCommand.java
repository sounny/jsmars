package edu.asu.jmars.swing.quick.add.layer;

import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class AdvancedMapCommand implements QuickMenuCommand {
	CommandReceiver cr;	
	
	public AdvancedMapCommand(CommandReceiver cr) {		
		this.cr = cr;
	}

	@Override
	public void execute() {
		cr.loadAdvancedMap();
	}
}

