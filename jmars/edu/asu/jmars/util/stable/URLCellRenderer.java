package edu.asu.jmars.util.stable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import org.apache.commons.validator.routines.UrlValidator;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;

public class URLCellRenderer extends DefaultTableCellRenderer {
	
	private static final Color HIGHLIGHT_COLOR = ThemeProvider.getInstance().getBackground().getHighlight();
	private static final Color LINK_COLOR = ThemeProvider.getInstance().getText().getMain();
	private static final String colorhex = edu.asu.jmars.ui.looknfeel.Utilities.getColorAsBrowserHex(HIGHLIGHT_COLOR);
	private final UrlValidator urlvalidator;
	
	public URLCellRenderer() {
		super();
		urlvalidator = new UrlValidator();
		setForeground(LINK_COLOR);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}  

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (value != null) {
			if (urlvalidator.isValid(getText())) {				
				String html = formatAsHtmlLink(getText());
				setText(html);
				setToolTipText(value.toString());
			} else {
				setToolTipText(null);
			}
		}
        return this;
    }

    public static String formatAsHtmlLink(String str) {
    	StringBuilder strbuilder = new StringBuilder();
    	strbuilder.append("<html><a style=\"color:");
    	strbuilder.append(colorhex);
    	strbuilder.append(";\" href=" + "\"");
    	strbuilder.append(str);
    	strbuilder.append("\">");
    	strbuilder.append(str);
    	strbuilder.append("</a></html>");
    	return strbuilder.toString();    	
    }

}

