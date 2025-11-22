package edu.asu.jmars.swing.landmark.search.popup;

import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class BookmarkCommand implements QuickMenuCommand {

	CommandReceiver cr;	
	
	public BookmarkCommand(CommandReceiver cr) {		
		this.cr = cr;
	}

	@Override
	public void execute() {
	    cr.bookmarkCurrentPlace();
	}

}
