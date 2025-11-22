package edu.asu.jmars.layer.profile.swing;

import java.awt.Component;
import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import mdlaf.components.table.MaterialTableCellRenderer;

public class BooleanTableCellRender extends MaterialTableCellRenderer {
	JPanel panel;

	public BooleanTableCellRender() {
	}

	private void updateData(JComponent comp, Object value, int row, JTable table) {	
		panel = new JPanel(new GridBagLayout());
		panel.add(comp);
		if (row % 2 == 0) {
			panel.setBackground(table.getBackground());
		} else {
			panel.setBackground(ThemeProvider.getInstance().getRow().getAlternateback());
		}
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		JComponent comp = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
				column);
		updateData(comp, value, row, table);
		return panel;
	}
}
