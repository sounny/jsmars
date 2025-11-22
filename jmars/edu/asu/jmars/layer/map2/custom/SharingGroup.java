package edu.asu.jmars.layer.map2.custom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SharingGroup {
	private ArrayList<String> users;
	private String name;
	private String id;
	private boolean dirtyFlag = false;
	
	public SharingGroup(String groupName, ArrayList<String> groupUsers){
        name = groupName;
        users = groupUsers;
        Collections.sort(users);
    }
	public SharingGroup(String key, String groupName, ArrayList<String> groupUsers){
	    id = key;
		name = groupName;
		users = groupUsers;
		Collections.sort(users);
	}
	/**
	 * This constructor is for sending a group name and having the rest populated for you.
	 * @param groupName
	 */
	public SharingGroup(String groupName) {
	    name = groupName;
	    CustomMapBackendInterface.populateSharingGroup(this);
	    Collections.sort(users);
	}
	public String getName(){
		return name;
	}
	
	public ArrayList<String> getUsers(){
		return users;
	}
	public void clearUsers() {
	    users.clear();
	}
	public void addUser(String user) {
	    users.add(user);
	}
	public String getUsersString(){
		String text = "";
		
		for(int i=0; i<users.size(); i++){
			
			if(i>0){
				text+= ", ";
			}
			text += users.get(i);
		}
		
		return text;
	}
	
	public void setName(String newName){
		name = newName;
	}
	
	public void setUsers(ArrayList<String> newUsers){
		users = newUsers;
	}
	
	public void setId(String myId){
		id = myId;
	}
	public String getId() {
	    return id;
	}
	
	public static Comparator<SharingGroup> NameComparator = new Comparator<SharingGroup>() {
		public int compare(SharingGroup o1, SharingGroup o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};
	
	public String toString(){
		return name;
	}
	
	public boolean equals(Object obj){
		if(obj == null || (getClass() != obj.getClass())){
			return false;
		}
		else{
			SharingGroup sg = (SharingGroup) obj;
			if(sg.getName().equals(name) && getUsersString().equals(sg.getUsersString())){
				return true;
			}
		}
		
		return false;
	}

    public boolean isDirtyFlag() {
        return dirtyFlag;
    }
    public void setDirtyFlag(boolean dirtyFlag) {
        this.dirtyFlag = dirtyFlag;
    }
}
