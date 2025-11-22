package edu.asu.jmars.swing.quick.menu.command;

import java.util.HashMap;
import java.util.Map;


public class CommandExecutor {

	private Map<EnumStubInterface, QuickMenuCommand> commands = new HashMap<>();

	public void addRequest(EnumStubInterface menucommand, QuickMenuCommand command) {
		commands.put(menucommand, command);
	}

	public void processRequest(EnumStubInterface menucommand) {
		commands.get(menucommand).execute();
	}

}
