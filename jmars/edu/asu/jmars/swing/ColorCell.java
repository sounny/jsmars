package edu.asu.jmars.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Ported from edu.asu.jmars.layer.NumBackFocus
 * @author whagee
 *
 */

public class ColorCell extends ColorCombo implements TableCellRenderer{
	public Component getTableCellRendererComponent(
			JTable table, Object color,
			boolean isSelected, boolean hasFocus,
			int row, int column)
	{

		ColorCombo combo = (ColorCombo)color;
		setColor(combo.getColor());
        setPreferredSize(new Dimension(73,15));
        setMaximumSize(new Dimension(73, 15));
        
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.add(this, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		
		if (isSelected) {
	         panel.setForeground(table.getSelectionForeground());
	         panel.setBackground(table.getSelectionBackground());
		} else {
	         panel.setForeground(table.getForeground());
	         panel.setBackground(table.getBackground());
		}
		


		return panel;
	}

}
