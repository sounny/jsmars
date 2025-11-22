package edu.asu.jmars.swing.quick.edit.row;

import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class DeleteCommand implements QuickMenuCommand {

	CommandReceiver cr;

	public DeleteCommand(CommandReceiver cr) {
		this.cr = cr;
	}

	@Override
	public void execute() {
		cr.delete();
	}
}
