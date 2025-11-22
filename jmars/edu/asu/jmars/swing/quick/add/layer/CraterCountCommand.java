package edu.asu.jmars.swing.quick.add.layer;

import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class CraterCountCommand implements QuickMenuCommand {	

		CommandReceiver cr;	
		
		public CraterCountCommand(CommandReceiver cr) {		
			this.cr = cr;
		}

		@Override
		public void execute() {
			cr.loadCraterCount();
		}
}	
