package edu.asu.jmars.layer.profile.swing;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class LabelRenderer extends DefaultTableCellRenderer {
	
	public LabelRenderer() {
		setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
	}

	private int rowH = 50;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (table.getRowHeight(row) < rowH) {
			table.setRowHeight(row, rowH);
		}
		return c;
	}

	@Override
	protected void setValue(Object value) {
		if (value instanceof String) {
			String sVal = (String) value;
			if (sVal.indexOf('\n') >= 0 && // any newline?
					!(sVal.startsWith("<html>") && sVal.endsWith("</html>"))) // already HTML?
				value = "<html>&nbsp;" + sVal + "</html>";
		}
		super.setValue(value);
	}
}
