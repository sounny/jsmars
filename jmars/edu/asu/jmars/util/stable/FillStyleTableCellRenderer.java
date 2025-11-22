package edu.asu.jmars.util.stable;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import edu.asu.jmars.util.FillStyle;

//  The cell renderer for the fill style column.
public class FillStyleTableCellRenderer extends JPanel implements TableCellRenderer
{
	private static final long serialVersionUID = -1130961411983125304L;
	
	FillStyle fillStyle = null;
	
	public FillStyleTableCellRenderer(){
		super();
		setOpaque(true);
	}
	
	public Component getTableCellRendererComponent(JTable table, Object val, boolean isSelected, boolean hasFocus, int row, int column) {
		if (val==null){
			repaint();
			return this;
		}
		
		if (val instanceof FillStyle) {
			fillStyle = (FillStyle)val;
		}
				
		repaint();
		return this;
	}
	
	public void paintComponent( Graphics g){
		Graphics2D g2 = (Graphics2D)g;
		Dimension d = getSize();
		
		g2.setBackground(getBackground());
		g2.clearRect(0, 0, d.width, d.height);
		if (fillStyle != null){
			g2.setPaint(fillStyle.getPaint(1));
			g2.fill(new Rectangle2D.Double(0,0, d.width, d.height));
		}
	}
	
}
