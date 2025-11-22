package edu.asu.jmars.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.AbstractCellEditor;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 * Taken from LandingSiteTable.  Displays a color combo that is centered 
 * in the table cell.  Has the same layout and sizes as the color combo
 * that is displayed in the ColorCombo class (a cell renderer).
 */

public class ColorComboCellEditor extends AbstractCellEditor implements TableCellEditor{

	JPanel panel = new JPanel();
	ColorCombo combo;
	
	public Object getCellEditorValue() {
		return combo.getColor();
	}

	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		
		panel.removeAll();
		
		//set size constraints on combo box
		combo = (ColorCombo)value;
		combo.setMinimumSize(new Dimension(73, 15));
        combo.setPreferredSize(new Dimension(73,15));
        combo.setMaximumSize(new Dimension(73, 15));
        
        panel.setLayout(new GridBagLayout());
        panel.add(combo, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
        
        //cell is always selected when it gets down into the editor, so make background selected color
		panel.setBackground(table.getSelectionBackground());
        
       
		return panel;
	}

}
