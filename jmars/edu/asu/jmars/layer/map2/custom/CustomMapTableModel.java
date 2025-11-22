package edu.asu.jmars.layer.map2.custom;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class CustomMapTableModel extends AbstractTableModel{

	private ArrayList<String> columnNames;
	private static final String COL_NAME = "Map Name";
	private static final String COL_UPLOAD_DATE = "Upload Date";
	private static final String COL_LAST_ADD_DATE = "Layer Last Added";
	private static final String COL_OWNER = "Owner";
	private static final String COL_ID = "Id";
    private static final String COL_FILENAME = "Filename";
    private static final String COL_KEYWORDS = "Keywords";
    private static final String COL_UNITS = "Units";
    private static final String COL_MAX_LAT = "Max Lat";
    private static final String COL_MIN_LAT = "Min Lat";
    private static final String COL_MAX_LON = "Max Lon";
    private static final String COL_MIN_LON = "Min Lon";
    private static final String COL_LINKS = "Links";
    private static final String COL_CITATION = "Citation";
    private static final String COL_DESCRIPTION = "Abstract";
    private static final String COL_IGNORE_VAL = "Ignore Value";
    private static final String COL_WKT = "WKT String";
    private static final String COL_MAX_PPD = "Max PPD";
    private static final String COL_SHARED = "Sharing";
    
	//TODO: add more columns
	
	private List<CustomMap> maps;
	private ArrayList<String> defaultVisibleColumns = new ArrayList<String>();
	
	public CustomMapTableModel(List<CustomMap> maps){
		this.maps = maps;
		columnNames = new ArrayList<String>();
		columnNames.add(COL_NAME);
        columnNames.add(COL_UPLOAD_DATE);
        columnNames.add(COL_LAST_ADD_DATE);
        columnNames.add(COL_OWNER);
		columnNames.add(COL_ID);
		columnNames.add(COL_FILENAME);
        columnNames.add(COL_KEYWORDS);
        columnNames.add(COL_UNITS);
		columnNames.add(COL_MAX_LAT);
        columnNames.add(COL_MIN_LAT);
        columnNames.add(COL_MAX_LON);
        columnNames.add(COL_MIN_LON);
        columnNames.add(COL_LINKS);
        columnNames.add(COL_CITATION);
		columnNames.add(COL_DESCRIPTION);
        columnNames.add(COL_IGNORE_VAL);
        columnNames.add(COL_WKT);
        columnNames.add(COL_MAX_PPD);
        columnNames.add(COL_SHARED);
		
        defaultVisibleColumns.add(COL_ID);
		defaultVisibleColumns.add(COL_NAME);
		defaultVisibleColumns.add(COL_FILENAME);
        defaultVisibleColumns.add(COL_OWNER);
        defaultVisibleColumns.add(COL_DESCRIPTION);
        defaultVisibleColumns.add(COL_SHARED);
	}
	
	@Override
	public int getColumnCount() {
		return columnNames.size();
	}
	public ArrayList<String> getDefaultVisibleColumns() {
        return defaultVisibleColumns;
    }
	public String getColumnName(int column){
		return columnNames.get(column);
	}

	@Override
	public int getRowCount() {
		return maps.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		CustomMap map = maps.get(rowIndex);
		switch(getColumnName(columnIndex)){
		case COL_NAME:
			return map.getName();    
        case COL_UPLOAD_DATE:
            return map.getUploadDate();
        case COL_LAST_ADD_DATE:
            return map.getLastUsedDate();
        case COL_OWNER:
            return map.getOwner();
		case COL_ID:
            return map.getCustomMapId();
		case COL_FILENAME:
		    return map.getBasename();
		case COL_KEYWORDS:
		    return map.getKeywordsString();
		case COL_UNITS:
            return map.getUnits();
		case COL_MAX_LAT:
		    return map.getNorthLat();
		case COL_MIN_LAT:
            return map.getSouthLat();
        case COL_MAX_LON:
            return map.getEastLon();
        case COL_MIN_LON:
            return map.getWestLon();
        case COL_LINKS:
            return map.getLinks();
        case COL_CITATION:
            return map.getCitation();
        case COL_DESCRIPTION:
            return map.getDescription();
        case COL_IGNORE_VAL:
            return map.getIgnoreValue();
        case COL_WKT:
            return map.getWktString();
        case COL_MAX_PPD:
            return map.getMaxPPD();
        case COL_SHARED:
            if(map.isSharedWithOthers()) {
                return "Shared";
            } else {
                return "";
            }
		}
		return "";
	}
	public int getWidth(String header){
        switch(header) {
        case COL_NAME:
            return 150;
        default:
            return 100;
        }
    }
	public CustomMap getMap(int row){
		return maps.get(row);
	}
	
	public int getRowForMap(CustomMap map){
		return maps.indexOf(map);
	}
}
