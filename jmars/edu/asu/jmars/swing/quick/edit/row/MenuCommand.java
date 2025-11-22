package edu.asu.jmars.swing.quick.edit.row;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import edu.asu.jmars.swing.quick.menu.command.EnumStubInterface;

//following Joshua Bloch's "Effective Java" enum impl
public enum MenuCommand implements EnumStubInterface {

	OPEN("Open"), 
	OPEN_DOCKED("Open Docked"), 
	RENAME("Rename"), 
	DELETE("Delete"),
	TOOLTIP("Show Tooltip");

	private String menuCommand;
	private static Map<String, MenuCommand> commands = new ConcurrentHashMap<>();

	public String getMenuCommand() {
		return menuCommand;
	}

	MenuCommand(String command) {
		this.menuCommand = command;
	}

	static {
		Map<String,MenuCommand> map = new ConcurrentHashMap<>();
		for (MenuCommand instance : MenuCommand.values()) {
			map.put(instance.getMenuCommand(), instance);
		}
		commands = Collections.unmodifiableMap(map);
	}

	public static MenuCommand get(String name) {
		return commands.get(name);
	}
}
