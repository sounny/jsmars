package edu.asu.jmars.layer.investigate;

import java.awt.Color;
import java.text.NumberFormat;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.JXTable;

import edu.asu.jmars.util.stable.ColorCellEditor;
import edu.asu.jmars.util.stable.ColorCellRenderer;

/**
 * A table containing the readout values from the chart.
 * There is one row per data item in the plot. The table allows
 * editing of the plot color.
 */
public class ChartReadOutTable extends JTable {
	
	ChartReadOutTableModel tableModel;
	
	public ChartReadOutTable(ChartReadOutTableModel model){
		super(model);
		tableModel = model;
		boolean isColorCellEditable = true;
		setDefaultRenderer(Color.class, new ColorCellRenderer(isColorCellEditable));
		setDefaultEditor(Color.class, new ColorCellEditor());
		setDefaultRenderer(Number.class, new NumberRenderer());
		setDefaultRenderer(String.class, new DefaultTableCellRenderer());
		
		setAutoResizeMode(this.AUTO_RESIZE_OFF);
		TableColumn colorCol = getColumnModel().getColumn(tableModel.COLOR_COLUMN);
		colorCol.setPreferredWidth(70);
		colorCol.setMinWidth(70);
		TableColumn nameCol = getColumnModel().getColumn(tableModel.NAME_COLUMN);
		nameCol.setPreferredWidth(200);
		TableColumn idCol = getColumnModel().getColumn(tableModel.ID_COLUMN);
		idCol.setPreferredWidth(300);
		idCol.setMinWidth(300);
		TableColumn valueCol = getColumnModel().getColumn(tableModel.VALUE_COLUMN);
		valueCol.setPreferredWidth(100);
		TableColumn unitCol = getColumnModel().getColumn(tableModel.UNIT_COLUMN);
		unitCol.setPreferredWidth(85);
		unitCol.setMinWidth(85);
			
	}
	
	class NumberRenderer extends DefaultTableCellRenderer {
		NumberFormat nf = NumberFormat.getNumberInstance();
		
	    public NumberRenderer() { 
	    	super();
	    	nf.setMaximumFractionDigits(8);
	    }

	    public void setValue(Object value) {
	    	/*
	    	 * The data from ReadoutTableModel for which NumberRenderer is used in conjunction with
	    	 * contains NaNs for the MapSources for which we haven't received any data as yet.
	    	 */
	        setText((value == null) ? "" : (Double.isNaN(((Number)value).doubleValue())? "Value Unavailable": nf.format(value)));
	    }  
	}
	
}
