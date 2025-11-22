package edu.asu.jmars.util.stable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.util.LineType;

//  The cell renderer for the line type column.
public class LineTypeTableCellRenderer extends JPanel implements TableCellRenderer
{
	private BasicStroke lineStroke = null;
	private static final Color nullFieldBackground = Color.lightGray;
	
	public LineTypeTableCellRenderer(){
		super();
		setOpaque(true);
	}
	
	public Component getTableCellRendererComponent(JTable table, Object val, boolean isSelected, boolean hasFocus, int row, int column) {
		if (val==null){
			lineStroke = null;
			repaint();
			return this;
		}
		
		LineType lineType = (LineType)val;
		lineStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
				lineType.getDashPattern(), 0.0f);
		
		repaint();
		return this;
	}
	
	public void paintComponent( Graphics g){
		Graphics2D g2 = (Graphics2D)g;
		Dimension d = getSize();
		
		g2.setBackground(getBackground());
		g2.clearRect(0, 0, d.width, d.height);
		if (lineStroke != null){
			g2.setStroke(lineStroke);
			g2.setColor(ThemeProvider.getInstance().getText().getMain());
			g2.draw(new Line2D.Double(0,d.height/2, d.width, d.height/2));
		}
	}
	
}
