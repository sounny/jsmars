package edu.asu.jmars.layer.map2.custom;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

public class SharingTableModel extends AbstractTableModel{
	private ArrayList<String> columnNames;
	private final String COL_NAME = "Shared Map";
	private final String COL_USER = "Shared Users";
	private final String COL_GROUPS = "Shared Groups";
	
	private ArrayList<CustomMap> maps;
	private ArrayList<String> defaultVisibleColumns = new ArrayList<String>();
	
	public SharingTableModel(ArrayList<CustomMap> maps){
		this.maps = maps;
		columnNames = new ArrayList<String>();
		columnNames.add(COL_NAME);
		columnNames.add(COL_USER);
		columnNames.add(COL_GROUPS);
		defaultVisibleColumns.add(COL_NAME);
        defaultVisibleColumns.add(COL_USER);
        defaultVisibleColumns.add(COL_GROUPS);
	}
	
	public int getColumnCount(){
		return columnNames.size();
	}
	public ArrayList<String> getDefaultVisibleColumns() {
        return defaultVisibleColumns;
    }
	public String getColumnName(int column){
		return columnNames.get(column);
	}
	
	public int getRowCount(){
		return maps.size();
	}
	public int getWidth(String header){
        switch(header) {
        case COL_NAME:
            return 200;
        default:
            return 300;
        }
    }
	public Object getValueAt(int rowIndex, int colIndex){
		CustomMap map = maps.get(rowIndex);
		switch(getColumnName(colIndex)){
		case COL_NAME:
			return map.getName();
		case COL_USER:
				return map.getSharedWithUsersString();
		case COL_GROUPS:
				return map.getSharedWithGroupsString();
		}
		return null;
	}
	
	public CustomMap getMap(int row){
		return maps.get(row);
	}
}
