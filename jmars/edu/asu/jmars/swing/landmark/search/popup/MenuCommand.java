package edu.asu.jmars.swing.landmark.search.popup;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import edu.asu.jmars.swing.quick.menu.command.EnumStubInterface;

//following Joshua Bloch's "Effective Java" enum impl
public enum MenuCommand implements EnumStubInterface {

	BOOKMARK_PLACE("Bookmark Current Place"), 
	PLACES("Places"),
	SEARCH_PLACES("Search Bookmarked Places"),
	EXPLORE_NOM("Explore Landmarks"),
	SEARCH_NOM("Search Landmarks"),
	COORD_ORDER("Coordinates Order");	

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

