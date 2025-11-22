package edu.asu.jmars.layer.map2.msd;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * A simple container class for a single group with shared users
 */
public class SharedGroup {
	private String groupName;
	private String domain;

	TreeMap<String, SharedUser> users;
		
	public SharedGroup(String name, String domain) {
		groupName = name;
		this.domain = domain;
		users = new TreeMap<String, SharedUser>();
	}
	
	public SharedGroup(String name) {
		groupName = name;
		users = new TreeMap<String, SharedUser>();
	}
	/**
	 * This method builds a TreeMap of all the users in the provided groups.
	 * All users that are common to all groups are marked as fully shared.
	 * @param groups
	 * @return a TreeMap of all the users in the groups marked as fully shared or not
	 */
	public static TreeMap<String, SharedUser> buildCommonMemberMap(TreeMap<String, SharedGroup> groups) {
		TreeMap<String, SharedUser> users = new TreeMap<String, SharedUser>();

		// build a distinct list of the users
		for (SharedGroup g : groups.values()) {
			Iterator<SharedUser> iter = g.getUsers().iterator();
			while (iter.hasNext()) {
				SharedUser u = iter.next();
				users.put(u.getUserName(), u);
			}
		}
		
		if (!users.isEmpty()) {
			for (SharedUser u : users.values()) {
				u.setFullyShared(true);
				for (SharedGroup g : groups.values()) {
					// add the group members to the member list
					if (g.hasSharedUsers()) {
						if (g.getUser(u.getUserName()) == null) {
							u.setFullyShared(false);
						}
					} else {
						u.setFullyShared(false);
					}
				}
			}
		}
		return users;
	}
	
	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
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
	
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
	public void removeUser(String user) {
		users.remove(user);	
	}
}
