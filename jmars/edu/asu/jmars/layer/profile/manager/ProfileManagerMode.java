package edu.asu.jmars.layer.profile.manager;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum ProfileManagerMode {	
	
	MANAGE("manage"),
	
	SELECT_MANY("selectMany"),
	
	SELECT_ONE("selectOne"), 
	
	CREATE_NEW_CHART("createNewChart"),
	
	ADD_TO_CHART("addToChart"),
	
	RENAME_PROFILE("renameProfile");
	
	private String mode = "manage";
	private static Map<String, ProfileManagerMode> modes = new ConcurrentHashMap<>();

	ProfileManagerMode(String newmode) {
		this.mode = newmode;
	}
	
	public String getMode() {
		return this.mode;
	}
	
	static {
		Map<String,ProfileManagerMode> map = new ConcurrentHashMap<>();
		for (ProfileManagerMode instance : ProfileManagerMode.values()) {
			map.put(instance.getMode(), instance);
		}
		modes = Collections.unmodifiableMap(map);
	}

	public static ProfileManagerMode get(String name) {
		return modes.get(name);
	}	

}
