package edu.asu.jmars.layer.map2.msd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONObject;

import edu.asu.jmars.layer.map2.MapSource;

/**
 * A simple container class for a single map with shared users
 */
public class SharedMap {
	private String mapName;
	TreeMap<String, SharedUser> users;
		
	public SharedMap(String name) {
		mapName = name;
		users = new TreeMap<String, SharedUser>();
	}
	
	public String getMapName() {
		return mapName;
	}

	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	public Collection<SharedUser> getUsers() {
		return users.values();
	}

	public void setUsers(TreeMap<String, SharedUser> users) {
		this.users = users;
	}
	
	public SharedUser getUser(String name) {
		return users.get(name);
	}
	
	public SharedUser addUser(SharedUser user) {
		return users.put(user.getUserName(), user);
	}
	
	public boolean hasSharedUsers() {
		return !users.isEmpty();
	}
}
