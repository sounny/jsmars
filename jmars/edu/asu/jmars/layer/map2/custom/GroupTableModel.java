package edu.asu.jmars.layer.map2.custom;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

public class GroupTableModel extends AbstractTableModel {
	
	private ArrayList<String> columnNames;
	private final String COL_NAME = "Group Name";
	private final String COL_USERS = "Users";
	
	private ArrayList<SharingGroup> groups;

	public GroupTableModel(ArrayList<SharingGroup> groups){
		this.groups = groups;
		
		columnNames = new ArrayList<String>();
		columnNames.add(COL_NAME);
		columnNames.add(COL_USERS);
	}
	
	
	public int getColumnCount() {
		return columnNames.size();
	}

	public String getColumnName(int column){
		return columnNames.get(column);
	}
	
	public int getRowCount() {
		return groups.size();
	}

	public Object getValueAt(int rowIndex, int colIndex) {
		SharingGroup group = groups.get(rowIndex);
		switch(getColumnName(colIndex)){
		case COL_NAME:
			return group.getName();
		case COL_USERS:
			return group.getUsersString();
		}
		return null;
	}

	public SharingGroup getGroup(int row){
		return groups.get(row);
	}
}
