package edu.asu.jmars.swing.quick.edit.row;

import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class OpenDockedCommand implements QuickMenuCommand {

	CommandReceiver cr;

	public OpenDockedCommand(CommandReceiver cr) {
		this.cr = cr;
	}

	@Override
	public void execute() {
		cr.openDocked();
	}
}
