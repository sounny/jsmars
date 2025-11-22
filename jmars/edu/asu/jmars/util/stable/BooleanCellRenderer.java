package edu.asu.jmars.util.stable;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CHECK_OFF_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CHECK_ON_IMG;
import java.awt.Color;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;

// The cell editor for Boolean columns.
public class BooleanCellRenderer extends JCheckBox 	implements TableCellRenderer
{
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static final Icon chkbox = new ImageIcon(ImageFactory.createImage(CHECK_OFF_IMG
            						 .withDisplayColor(imgColor)));
	private static final Icon chkboxselected = new ImageIcon(ImageFactory.createImage(CHECK_ON_IMG
			 						 .withDisplayColor(imgColor)));			
	
	public BooleanCellRenderer(){
		super("");
		setOpaque(true);
		setHorizontalAlignment(JCheckBox.CENTER);
		setIcon(chkbox); 
	 	setSelectedIcon(chkboxselected); 		
	}

	public Component getTableCellRendererComponent(JTable table, Object val, boolean isSelected, boolean hasFocus, int row, int column) {
		if (val != null) {
			setSelected(((Boolean)val).booleanValue());
		}
		return this;
	}
}

