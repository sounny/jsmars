package edu.asu.jmars.layer.krc;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import edu.asu.jmars.util.stable.ColorCellEditor;
import edu.asu.jmars.util.stable.ColorCellRenderer;

public class ReadoutTable extends JTable {
	
	public ReadoutTable(ReadoutTableModel model){
		super(model);
		boolean isColorCellEditable = true;
		setDefaultRenderer(Color.class, new ColorCellRenderer(isColorCellEditable));
		setDefaultEditor(Color.class, new ColorCellEditor());
	}
	
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component c = super.prepareRenderer(renderer, row, column);	
		return c;
	}

}
