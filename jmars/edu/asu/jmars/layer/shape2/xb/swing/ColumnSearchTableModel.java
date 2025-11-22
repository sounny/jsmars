package edu.asu.jmars.layer.shape2.xb.swing;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import edu.asu.jmars.layer.shape2.xb.data.service.Data;
import edu.asu.jmars.layer.shape2.xb.data.service.DataServiceEvent;
import edu.asu.jmars.layer.shape2.xb.data.service.IDataServiceEventListener;
import edu.asu.jmars.layer.util.features.Field;


public class ColumnSearchTableModel extends AbstractTableModel implements IDataServiceEventListener {
	private final String FIELD = "COLUMN NAME";
	private final String DATATYPE = "COLUMN TYPE";
	private List<Field> columnList = new ArrayList<>();	
	private  String[] columnNames = new String[] { FIELD,     DATATYPE };
	private final Class[] columnClass = new Class[] { String.class,  String.class };

			
	public ColumnSearchTableModel() {	
		Data.SERVICE.addDataEventListener(this);
	}
	
	public void addData() {
		columnList.clear();
		columnList.addAll(Data.SERVICE.getData());
		fireTableDataChanged();
	}

	
	public List<Field> getModelData() {
		List<Field> modeldata = new ArrayList<>();
		modeldata.addAll(columnList);
		return modeldata;
	}
	
	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columnClass[columnIndex];
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return columnList.isEmpty() ? 0 : columnList.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnList.isEmpty())
			return null;

		Field row = columnList.get(rowIndex);

		if (getColumnName(columnIndex).equals(FIELD)) {
			return row.name;
		}
		// data type of a Field
		else if (getColumnName(columnIndex).equals(DATATYPE)) {
			String datatype = row.type.getSimpleName();
			return datatype;
		}
		return null;
	}


	@Override
	public boolean isCellEditable(int rowIndex, int colIndex) {	
		return false;
	}

	public Field getColumnDataAt(int row) {
		return columnList.get(row);
	}

	
	public String[] getInitialColumnNames() {
		return columnNames;
	}


	@Override
	public void handleDataServiceEvent(DataServiceEvent ev) {
		addData();
	}

}
