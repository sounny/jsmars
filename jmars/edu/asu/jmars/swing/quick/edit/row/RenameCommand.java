package edu.asu.jmars.swing.quick.edit.row;

import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class RenameCommand implements QuickMenuCommand {

	CommandReceiver cr;

	public RenameCommand(CommandReceiver cr) {
		this.cr = cr;
	}

	@Override
	public void execute() {
		cr.rename();
	}
}


