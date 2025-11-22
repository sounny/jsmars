package edu.asu.jmars.swing.quick.add.layer;

import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;

public class FavoritesCommand implements QuickMenuCommand {
	CommandReceiver cr;	
	
	public FavoritesCommand(CommandReceiver cr) {		
		this.cr = cr;
	}

	@Override
	public void execute() {
		cr.showFavorites();
	}
}

