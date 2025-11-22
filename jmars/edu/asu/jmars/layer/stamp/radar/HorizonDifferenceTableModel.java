package edu.asu.jmars.layer.stamp.radar;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

public class HorizonDifferenceTableModel extends AbstractTableModel{

	private RadarHorizon myHorizon;
	private ArrayList<HorizonDifference> diffs;
	private final String NAME_COL = "Subtracted Horizon";
	private final String CONSTANT_COL = "Dielectric Constant";
	private final String DIFF_COL = "Mean Difference (Pixels)";
	private String[] columnNames = {NAME_COL, CONSTANT_COL, DIFF_COL};
	
	public HorizonDifferenceTableModel(RadarHorizon horizon) {
		myHorizon = horizon;
		diffs = myHorizon.getHorizonDifferences();
	}
	
	public String getColumnName(int column){
		return columnNames[column];
	}
	
	public int getRowCount() {
		if(diffs!=null){
			return diffs.size();
		}else{
			return 0;
		}
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		HorizonDifference diff = diffs.get(rowIndex);
		
		switch(getColumnName(columnIndex)){
		
		case NAME_COL:
			return diff.getHorizonDifferenceName();
		case CONSTANT_COL:
			return diff.getDielectricConstant();
		case DIFF_COL:
			if(diff.getMeanDifference()==null) return "null";
			else return String.format("%.2f", diff.getMeanDifference());
		default:
			return null;
		}
	}
	
	public HorizonDifference getDifferenceAtRow(int row){
		return diffs.get(row);
	}

}
