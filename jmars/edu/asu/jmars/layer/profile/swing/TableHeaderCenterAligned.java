package edu.asu.jmars.layer.profile.swing;

import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class TableHeaderCenterAligned extends JLabel implements TableCellRenderer {
	private Color headerbackground = new Color(219,219,219);
	private Color headertextcolor = new Color(57,60,71);

	public TableHeaderCenterAligned() {
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		setText(value.toString());
		JComponent component = (JComponent) table.getTableHeader().getDefaultRenderer()
				.getTableCellRendererComponent(table, value, false, false, -1, -2);
		if (component instanceof JLabel) {
			component.setOpaque(true);
			((JLabel) component).setHorizontalAlignment(JLabel.CENTER);
			component.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 0));
			component.setBackground(headerbackground);
			component.setForeground(headertextcolor);
		}
		return component;
	}
}
