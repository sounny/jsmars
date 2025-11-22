package edu.asu.jmars.layer.profile.swing;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.COLOR_PICK;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;

public class ProfileColorCellRenderer extends JLabel implements TableCellRenderer {
	private Color color;
	private int sideTotalInset = 8;
	private int topBottomInset = 8;
	private static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getFill();
    private static final Icon palette = new ImageIcon(ImageFactory.createImage(COLOR_PICK.withDisplayColor(imgLayerColor)));		
	private boolean isEditable = false;
	private boolean isShowAsSwatch = false;
	private boolean isZebra = false;
    
	public ProfileColorCellRenderer(){
		super();
		setOpaque(true);		
	}
	
	public ProfileColorCellRenderer(boolean b) {
		super();
		setOpaque(true);
		isEditable = b;		
	}
 	
	
	/**
	 * This constructor exists to allow for updated insets rather than the hard coded values that were used. 
	 * @param sideTotalInset: total because half will be on each side. Default is 8, 4 on left and 4 on right.
	 * @param topBottomInset: total half of this value will be on top and half on bottom. Default is 8, 4 on top, 4 on bottom
	 */
	public ProfileColorCellRenderer(int sideTotalInset, int topBottomInset) {
		this();
		
		//force these values to be even
		if (sideTotalInset %2 != 0) {
			sideTotalInset++;
		}
		if (topBottomInset %2 != 0) {
			topBottomInset++;
		}
		this.sideTotalInset = sideTotalInset;
		this.topBottomInset = topBottomInset;
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {	
		color = (Color)value;
		setText(null);
		if (isZebra) {
			if (row % 2 == 0) {
				setBackground(table.getBackground());  			
			} else {
				setBackground(ThemeProvider.getInstance().getRow().getAlternateback()); 
			}			
		}
		return this;
	}
	
	public boolean isEditable() {
		return this.isEditable;
	}

	public void setEditable(boolean isEditable) {
		this.isEditable = isEditable;
	}

	public boolean isShowAsSwatch() {
		return isShowAsSwatch;
	}

	public void setShowAsSwatch(boolean isShowAsSwatch) {
		this.isShowAsSwatch = isShowAsSwatch;
	}

	public boolean isZebra() {
		return isZebra;
	}

	public void setZebra(boolean isZebra) {
		this.isZebra = isZebra;
	}

	@Override
	public void paintComponent(Graphics g) {		
		super.paintComponent(g);
		Insets insets = getInsets();
		int width = Math.max(2, getWidth()-insets.left-insets.right-this.sideTotalInset);
		int height = Math.max(2, getHeight()-insets.top-insets.bottom-this.topBottomInset);
		if (isShowAsSwatch) {
			width=height;
		}
		g.setColor(color);
		g.fillRect((getWidth()-width)/2, (getHeight()-height)/2, width, height);		
		if (this.isEditable) {			
			int xcoord = width - (palette.getIconWidth());
			int ycoord = ((getHeight() - height) / 2) + height / 4;
			palette.paintIcon(this, g, xcoord, ycoord);
		}
	}	
}

