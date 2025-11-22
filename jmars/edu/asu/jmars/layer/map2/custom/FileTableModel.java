package edu.asu.jmars.layer.map2.custom;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class FileTableModel extends AbstractTableModel {
	private List<CustomMap> files;
	
	public static final String COL_ID = "ID";
	public static final String COL_NAME = "Map Name";
	public static final String COL_STATUS = "Status";
	public static final String COL_NOTE = "Note";
	public static final String COL_BASENAME = "Filename";
    public static final String COL_STAGE = "Stage";
    public static final String COL_KEYWORDS = "Keywords";
    public static final String COL_UNITS = "Units";
    public static final String COL_MAX_LAT = "Max Lat";
    public static final String COL_MIN_LAT = "Min Lat";
    public static final String COL_MAX_LON = "Max Lon";
    public static final String COL_MIN_LON = "Min Lon";
    public static final String COL_LINKS = "Links";
    public static final String COL_CITATION = "Citation";
    public static final String COL_DESCRIPTION = "Abstract";
    public static final String COL_IGNORE_VAL = "Ignore Value";
    public static final String COL_WKT = "WKT String";
	public static final String COL_UPLOAD_DATE = "Upload Date";
	public static final String COL_NUM_BANDS = "# Bands";
    
	private ArrayList<String> columnNames = new ArrayList<String>();
	private ArrayList<String> defaultVisibleColumns = new ArrayList<String>();
	public FileTableModel(List<CustomMap> newFiles){
		files = newFiles;
		
		columnNames.add(COL_ID);
		columnNames.add(COL_NAME);
		columnNames.add(COL_STATUS);
        columnNames.add(COL_BASENAME);
//        columnNames.add(COL_MAX_LAT);
//        columnNames.add(COL_MIN_LAT);
//        columnNames.add(COL_MAX_LON);
//        columnNames.add(COL_MIN_LON);
//        columnNames.add(COL_IGNORE_VAL);
//        columnNames.add(COL_UNITS);
//        columnNames.add(COL_DESCRIPTION);
		columnNames.add(COL_NOTE);
//        columnNames.add(COL_LINKS);
//        columnNames.add(COL_CITATION);
//        columnNames.add(COL_WKT);
        columnNames.add(COL_STAGE);
        columnNames.add(COL_UPLOAD_DATE);
        columnNames.add(COL_NUM_BANDS);
        
        defaultVisibleColumns.add(COL_ID);
        defaultVisibleColumns.add(COL_NAME);
        defaultVisibleColumns.add(COL_BASENAME);
        defaultVisibleColumns.add(COL_STATUS);
        defaultVisibleColumns.add(COL_NOTE);
        defaultVisibleColumns.add(COL_UPLOAD_DATE);

	}
	
    public String getColumnName(int column) {
    	return columnNames.get(column);
    }
    public ArrayList<String> getDefaultVisibleColumns() {
        return defaultVisibleColumns;
    }
	public int getColumnCount() {
		return columnNames.size();
	}
//	public ArrayList<String> getColumnNames() {
//	    return columnNames;
//	}

	public int getRowCount() {
		return files.size();
	}
	
	public Object getValueAt(int rowIndex, int columnIndex){
		CustomMap uf = files.get(rowIndex);
		switch(getColumnName(columnIndex)) {
		    case COL_ID:
		        return (uf.getCustomMapId() == null ? "" : uf.getCustomMapId());
    		case COL_NAME:
    			return uf.getName();
    		case COL_STATUS:
    		    return uf.getStatus().toUpperCase();
    		case COL_NOTE:
    		    return (uf.getErrorMessage() != null && !uf.getErrorMessage().equals("null") ? uf.getErrorMessage() : "");
            case COL_STAGE:
                return (CM_Manager.stageReferenceTable.get(uf.getStage()) == null ? uf.getStage() :  CM_Manager.stageReferenceTable.get(uf.getStage()));
            case COL_DESCRIPTION:
                return uf.getDescription();
            case COL_BASENAME:
                return uf.getBasename();
            case COL_UNITS:
                return uf.getUnits();
            case COL_MAX_LAT:
                return uf.getNorthLat();
            case COL_MIN_LAT:
                return uf.getSouthLat();
            case COL_MAX_LON:
                return uf.getEastLon();
            case COL_MIN_LON:
                return uf.getWestLon();
            case COL_LINKS:
                return uf.getLinks();
            case COL_CITATION: 
                return uf.getCitation();
            case COL_IGNORE_VAL: 
                return uf.getIgnoreValue();
            case COL_WKT: 
                return uf.getWktString();
            case COL_NUM_BANDS:
                return uf.getNumberOfBands();
            case COL_UPLOAD_DATE: 
                return (uf.getUploadDate() == null ? "" : uf.getUploadDate());    
    		default:
    			return null;
    		}
	}
	
	public int getWidth(String header){
        switch(header) {
            case COL_ID:
                return 60;
            case COL_NAME:
                return 150;
            case COL_STATUS:
                return 220;
            case COL_NOTE:
                return 300;
            case COL_STAGE:
                return 25;
            case COL_DESCRIPTION:
                return 300;
            case COL_BASENAME:
                return 300;
            case COL_UNITS:
                return 40;
            case COL_MAX_LAT:
                return 40;
            case COL_MIN_LAT:
                return 40;
            case COL_MAX_LON:
                return 40;
            case COL_MIN_LON:
                return 40;
            case COL_LINKS:
                return 300;
            case COL_CITATION: 
                return 300;
            case COL_IGNORE_VAL: 
                return 30;
            case COL_WKT: 
                return 300;
            default:
                return 200;
            }
    }
	public CustomMap getFile(int index){
		return files.get(index);
	}
	
	public int getColumnIndex(String name){
		return columnNames.indexOf(name);
	}
}
