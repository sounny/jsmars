package edu.asu.jmars.layer.threed;

import org.json.JSONObject;

public class VREntry {

	private String id = null;
	private String key = null;
	private String name = null;
	
	public VREntry(JSONObject obj) throws Exception {
		id = (obj.isNull("id") ? "" : obj.getString("id"));
		name = (obj.isNull("name") ? "" : obj.getString("name"));
		key = (obj.isNull("key") ? "" : obj.getString("key"));
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
