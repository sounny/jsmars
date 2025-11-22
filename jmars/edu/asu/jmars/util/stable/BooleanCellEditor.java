package edu.asu.jmars.util.stable;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CHECK_OFF_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CHECK_ON_IMG;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;

// Editor for Boolean-classed table columns.
public class BooleanCellEditor extends DefaultCellEditor {
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static final Icon chkbox = new ImageIcon(ImageFactory.createImage(CHECK_OFF_IMG
            						 .withDisplayColor(imgColor)));
	private static final Icon chkboxselected = new ImageIcon(ImageFactory.createImage(CHECK_ON_IMG
			 .withDisplayColor(imgColor)));			
	
	public BooleanCellEditor() {
		super (getCheckbox ());
		
	}

	public boolean isCellEditable(EventObject e) {
		if (e instanceof MouseEvent) {
			int clickCount = ((MouseEvent) e).getClickCount();
			return (clickCount > 1);
		} else {
			return false;
		}
	}

	private static JCheckBox getCheckbox() {
		JCheckBox checkBox = new JCheckBox();	
		checkBox.setIcon(chkbox); 
	 	checkBox.setSelectedIcon(chkboxselected);		
	 	checkBox.setHorizontalAlignment(JCheckBox.CENTER);
		return checkBox;
	}
}
