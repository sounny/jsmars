package edu.asu.jmars.layer.stamp;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class PlotSourceRenderer
extends JLabel
implements TableCellRenderer
{
private static final Color nullFieldBackground = Color.lightGray;

public PlotSourceRenderer(){
	super();
	setOpaque(true);
}

public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	PlotSource source = (PlotSource)value;

	setText(source.getPlotTitle());
	return this;
}
}
