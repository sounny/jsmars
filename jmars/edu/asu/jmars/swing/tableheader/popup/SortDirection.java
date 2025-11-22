package edu.asu.jmars.swing.tableheader.popup;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import edu.asu.jmars.swing.quick.menu.command.EnumStubInterface;

public enum SortDirection implements EnumStubInterface {
	
		ASCENDING(1), 
		DESCENDING(-1),
		SECONDARY_ASCENDING(1),
		SECONDARY_DESCENDING(-1),
		NO_SORT(0);

		private int direction;
		private static Map<Integer, SortDirection> commands = new ConcurrentHashMap<>();

		public int getDirection() {
			return direction;
		}

		SortDirection(int command) {
			this.direction = command;
		}

		static {
			Map<Integer,SortDirection> map = new ConcurrentHashMap<>();
			for (SortDirection instance : SortDirection.values()) {
				map.put(instance.getDirection(), instance);
			}
			commands = Collections.unmodifiableMap(map);
		}

		public static SortDirection get(int direction) {
			return commands.get(direction);
		}	

}
