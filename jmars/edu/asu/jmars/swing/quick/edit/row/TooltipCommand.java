package edu.asu.jmars.swing.quick.edit.row;

import javax.swing.JCheckBoxMenuItem;
import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class TooltipCommand implements QuickMenuCommand {

	CommandReceiver cr;
	JCheckBoxMenuItem eventsource;
	
	public TooltipCommand(CommandReceiver cr, JCheckBoxMenuItem source) {		
		this.cr = cr;
		this.eventsource = source;
	}

	@Override
	public void execute() {
		cr.showTooltip(this.eventsource);
	}

}
