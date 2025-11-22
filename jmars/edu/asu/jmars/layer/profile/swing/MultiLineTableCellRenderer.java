package edu.asu.jmars.layer.profile.swing;

import java.awt.Component;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class MultiLineTableCellRenderer extends JList<String> implements TableCellRenderer {

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		// make multi line where the cell value is String[]
		if (value instanceof String[]) {
			setListData((String[]) value);
		}
		return this;
	}
}
