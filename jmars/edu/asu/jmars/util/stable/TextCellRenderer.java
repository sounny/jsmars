package edu.asu.jmars.util.stable;

import java.awt.Component;
import java.awt.FontMetrics;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import edu.asu.jmars.util.Util;

// Renderer for Strings, Integers, and Doubles.
public class TextCellRenderer
	extends JLabel
	implements TableCellRenderer
{
	private static final int MAX_WIDTH = 80;

	public TextCellRenderer(){
		super();
		setOpaque(true);
	}
	
	public Component getTableCellRendererComponent(JTable table, Object val, boolean isSelected, boolean hasFocus, int row, int column) {
		String s = (val instanceof String ? (String)val : "");
		String tip = null;
		
		FontMetrics fontMetrics = table.getFontMetrics(table.getFont());
        int columnWidth = table.getColumnModel().getColumn(column).getWidth();
        
        // Check if the text in the cell contents are wider than the column width
        int stringWidth = fontMetrics.stringWidth(s);

        if (stringWidth > columnWidth) {
        	tip = Util.foldText((String)val, MAX_WIDTH, "\n");
        }
        
		setText(s);
		setToolTipText(tip);

		return this;
	}
}
