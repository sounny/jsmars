package edu.asu.jmars.layer.stamp.spectra;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

public class SpectraTableModel extends AbstractTableModel {

	private static final String ID_COL = "ID";
	private static final String NAME_COL = "Name";
	private static final String COLOR_COL = "Color";
	private static final String LINE_COL = "Line";
	private static final String SAMPLES_COL = "Sample";
	private static final String XPAD_COL = "XPad";
	private static final String YPAD_COL = "YPad";
	
	private ArrayList<String> cNames;
	private ArrayList<SpectraObject> spectra;
	
	private boolean spectraPerPixel = false;
	
	public SpectraTableModel(ArrayList<SpectraObject> contents){
		this(contents, false);
	}

	public SpectraTableModel(ArrayList<SpectraObject> contents, boolean spectraPerPixel){
		spectra = contents;
		
		this.spectraPerPixel = spectraPerPixel;
		buildColumns();
	}

	private void buildColumns() {
		cNames = new ArrayList<String>();

		if (spectraPerPixel) {
			cNames.add(COLOR_COL);
			cNames.add(NAME_COL);
			cNames.add(LINE_COL);
			cNames.add(SAMPLES_COL);
			cNames.add(XPAD_COL);
			cNames.add(YPAD_COL);
		} else {
			cNames.add(NAME_COL);			
		}
	}
	
	@Override
	public int getRowCount() {
		return spectra.size();
	}

	@Override
	public int getColumnCount() {
		return cNames.size();
	}

	@Override
	public String getColumnName(int col){
		return cNames.get(col);
	}

	@Override
    public Class<?> getColumnClass(int columnIndex) {
        if (getColumnName(columnIndex).equalsIgnoreCase(COLOR_COL)) {
        	return Color.class;
        }
        return String.class;
    }
    
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		
		SpectraObject s = spectra.get(rowIndex);
		
		switch(getColumnName(columnIndex)){
	//		case ID_COL:
	//			return s.getId();
			case NAME_COL:
				return s.getName();
			case COLOR_COL:
				return s.getColor();
			case LINE_COL:
				return s.getLine();
			case SAMPLES_COL:
				return s.getSample();
			case XPAD_COL:
				return s.getXPad();
			case YPAD_COL:
				return s.getYPad();
		}
		
		return null;
	}
	
	
	public SpectraObject getSpectra(int row){
		return spectra.get(row);
	}

	
	public ArrayList<SpectraObject> getSpectra(int[] rows){
		ArrayList<SpectraObject> result = new ArrayList<SpectraObject>();
		for(int row : rows){
			result.add(spectra.get(row));
		}
		return result;
	}
}
