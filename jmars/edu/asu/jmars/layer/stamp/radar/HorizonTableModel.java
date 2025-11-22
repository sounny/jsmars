package edu.asu.jmars.layer.stamp.radar;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import edu.asu.jmars.layer.stamp.StampLView;

public class HorizonTableModel extends AbstractTableModel{

	private StampLView myLView;
	private ArrayList<RadarHorizon> myHorizons;
	final String SHOW_COL = "Show?";
	private final String ID_COL = "ID";
	private final String NAME_COL = "Name";
	private final String LOC_COL = "Location";
	final String COLOR_COL = "Color";
	private final String NOTE_COL = "Notes";
	private String[] columnNames = {
			SHOW_COL,
			ID_COL,
			COLOR_COL,
			NAME_COL,
			NOTE_COL,
			LOC_COL
	};
	
	
	public HorizonTableModel(ArrayList<RadarHorizon> horizons, StampLView view){
		myHorizons = horizons;
		myLView = view;
	}
	
	public String getColumnName(int column){
		return columnNames[column];
	}
	
	public int getRowCount() {
		return myHorizons.size();
	}

	public int getColumnCount() {
		return columnNames.length;
	}
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		RadarHorizon h = myHorizons.get(rowIndex);
		
		switch(getColumnName(columnIndex)){
		case SHOW_COL:
			return new Boolean(h.isVisible());
		case ID_COL:
			return h.getID();
		case NAME_COL:
			return h.getName();
		case LOC_COL:
			return h.getLocationText();
		case COLOR_COL:
			return h.getColor();
		case NOTE_COL:
			return h.getNote();
		default:
			return null;
		}
	}
	
	public void setValueAt(Object value, int row, int col){
		if(getColumnName(col) == SHOW_COL && value instanceof Boolean){
			RadarHorizon h = getSelectedHorizon(row);
			boolean bool = (boolean)value;
			h.setVisible(bool);
			//refresh views
			myLView.getFocusPanel().getRadarView().repaintHorizon();
			myLView.repaint();
		}
		fireTableCellUpdated(row, col);
	}
	
	public Class<?> getColumnClass(int column){
		if(getColumnName(column) == SHOW_COL){
			return Boolean.class;
		}
		return Object.class;
	}
	
	public boolean isCellEditable(int rowIndex, int columnIndex){
		if(getColumnName(columnIndex) == SHOW_COL){
			return true;
		}else{
			return false;
		}
	}
	
	public RadarHorizon getSelectedHorizon(int row){
		return myHorizons.get(row);
	}
	
	public ArrayList<RadarHorizon> getHorizons(){
		return myHorizons;
	}
}
