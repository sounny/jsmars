package edu.asu.jmars.layer.profile.manager;

import java.awt.Component;
import java.net.URL;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public class HoverWithIconRenderer extends JLabel implements TableCellRenderer {
	
	public HoverWithIconRenderer() {
		setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		setText((String) value);
		if (column == 0) { //Name column
			String profilename = (String) value;
			String htmltext = getHtml(profilename);
			this.setToolTipText(htmltext);
		} else if (column == 3) { //Coordinates column
			String htmltext2 = getHtml2();
			this.setToolTipText(htmltext2);
		} else {
			this.setToolTipText(null);
		}
		return this;
	}

	private String getHtml(String name) {
		URL url = edu.asu.jmars.Main.class.getResource("/resources/edit.png");
		String html = "<html><head></head><body>"
				+ "<div>Double click to rename &nbsp; &nbsp;" + "<img src='" + url + "'></img></div>"
				+ "</body></html>";
		return html;
	}
	
	private String getHtml2() {
		URL url = edu.asu.jmars.Main.class.getResource("/resources/copy.png");
		String html = "<html><head><style>img {float:right;}</style></head><body>"
				+ "<div >Coordinates are read-only but you can select in-cell and copy these coordinates.</div>"
				+ "<div>Double click in cell, then right-click to bring up Copy-Paste menu. &nbsp; &nbsp;" 
				+ "<img src='" + url + "'>"
				+ "</img></div></body></html>";
		return html;
	}

}


