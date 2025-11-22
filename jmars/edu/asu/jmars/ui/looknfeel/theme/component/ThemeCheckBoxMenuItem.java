package edu.asu.jmars.ui.looknfeel.theme.component;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CHECK_OFF_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CHECK_ON_IMG;


import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeCheckBoxMenuItem implements ThemeComponent {

	private static String catalogKey = "checkboxmenuitem";

	static {
		GUITheme.getCatalog().put(catalogKey, new ThemeCheckBoxMenuItem());
	}

	public static String getCatalogKey() {
		return catalogKey;
	}

	public Color getSelectionbackground() {
		return ThemeProvider.getInstance().getBackground().getHighlight();
	}
	
	public Color getSelectionforeground() {
		return ThemeProvider.getInstance().getSelection().getForegroundContrast();
	}

	public Color getBackground() {
		return ThemeProvider.getInstance().getRow().getAlternate();
	}

	public Color getForeground() {
		return ThemeProvider.getInstance().getText().getMain();
	}

	public Color getCheckiconimgcolor() {
		return ThemeProvider.getInstance().getAction().getImageOn();
	}

	public Color getSelectedcheckiconimgcolor() {
		return ThemeProvider.getInstance().getAction().getImageOff();
	}

	public Color getAcceleratorforeground() {
		return ThemeProvider.getInstance().getText().getMain();
	}

	public Color getAcceleratorselectionforeground() {
		return ThemeProvider.getInstance().getSelection().getForegroundContrast();
	}

	public ThemeCheckBoxMenuItem() {
	}

	@Override
	public void configureUI() {	    
		UIManager.put("CheckBoxMenuItem.font", ThemeFont.getBold());
		UIManager.put("CheckBoxMenuItem.selectionBackground", this.getSelectionbackground());
		UIManager.put("CheckBoxMenuItem.selectionForeground", this.getSelectionforeground());
		UIManager.put("CheckBoxMenuItem.border", BorderFactory.createEmptyBorder(5, 0, 5, 0));
		UIManager.put("CheckBoxMenuItem.background", this.getBackground());
		UIManager.put("CheckBoxMenuItem.foreground", this.getForeground());
		UIManager.put("CheckBoxMenuItem.checkIcon", new ImageIcon(ImageFactory
				.createImage(CHECK_OFF_IMG.withDisplayColor(this.getCheckiconimgcolor()))));
		UIManager.put("CheckBoxMenuItem.selectedCheckIcon", new ImageIcon(ImageFactory.createImage(
				CHECK_ON_IMG.withDisplayColor(this.getSelectedcheckiconimgcolor()))));
		UIManager.put("CheckBoxMenuItem.acceleratorForeground", this.getAcceleratorforeground());
		UIManager.put("CheckBoxMenuItem.acceleratorSelectionForeground",
				this.getAcceleratorselectionforeground());
	}
}
