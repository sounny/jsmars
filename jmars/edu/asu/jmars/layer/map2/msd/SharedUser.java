package edu.asu.jmars.layer.map2.msd;

/** 
 * A container class for a user that a custom map has been shared with.
 */
public class SharedUser {
	String userName;
	boolean group = false;
	boolean fullyShared = false;
	boolean unshared = false;
	String status = "";
	String groupName = "";
	
	public SharedUser (String name) {
		userName = name;
	}
	public SharedUser (String name, String group) {
		userName = name;
		groupName = group;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}	
	public boolean isGroup() {
		return group;
	}
	public void setGroup(boolean group) {
		this.group = group;
	}
	public boolean isFullyShared() {
		return fullyShared;
	}
	public void setFullyShared(boolean fullyShared) {
		this.fullyShared = fullyShared;
	}
	public boolean isUnshared() {
		return unshared;
	}
	public void setUnshared(boolean unshared) {
		this.unshared = unshared;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}	
}
