package edu.asu.jmars.swing.landmark.search.swing;

import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;


public class MyLeftColumnHeaderRenderer extends JLabel implements TableCellRenderer {
	
	public MyLeftColumnHeaderRenderer() {
		setOpaque(true);
		setForeground(ThemeProvider.getInstance().getText().getMain());
		setBackground(ThemeProvider.getInstance().getBackground().getMain());
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		setHorizontalAlignment(JLabel.LEFT);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		setText(value.toString());
		return this;
	}
}
