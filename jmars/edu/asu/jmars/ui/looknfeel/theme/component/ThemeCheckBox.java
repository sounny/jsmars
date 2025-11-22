package edu.asu.jmars.ui.looknfeel.theme.component;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CHECK_OFF_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CHECK_ON_IMG;


import java.awt.Color;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeCheckBox implements ThemeComponent {

	private static String catalogKey = "checkbox";

	static {
		GUITheme.getCatalog().put(catalogKey, new ThemeCheckBox());
	}

	public ThemeCheckBox() {
	    
	}

	public Font getFont() {
	    return ThemeFont.getRegular();
	}
	public Color getBackground() {
		return ThemeProvider.getInstance().getBackground().getMain();
	}

	public Color getForeground() {
		return ThemeProvider.getInstance().getAction().getForeground();
	}

	public Color getDisabledtext() {
		return ThemeProvider.getInstance().getAction().getDisabledForeground();
	}

	public Color getOffColor() {
		return ThemeProvider.getInstance().getAction().getImageOff();
	}

	public Color getOnColor() {
		return ThemeProvider.getInstance().getAction().getImageOn();
	}

	public static String getCatalogKey() {
		return catalogKey;
	}

	public Color getForegroundhilight() {
		return ThemeProvider.getInstance().getBackground().getHighlight();
	}

	@Override
	public void configureUI() {	    
		UIManager.put("CheckBox.font", this.getFont());
		UIManager.put("CheckBox.background", this.getBackground());
		UIManager.put("CheckBox.foreground", this.getForeground());
		UIManager.put("CheckBox.disabledText", this.getDisabledtext());
	    UIManager.put("CheckBox.icon", new ImageIcon(
				ImageFactory.createImage(CHECK_OFF_IMG.withDisplayColor(this.getOffColor()))));
		UIManager.put("CheckBox.selectedIcon", new ImageIcon(
				ImageFactory.createImage(CHECK_ON_IMG.withDisplayColor(this.getOnColor()))));
		
	}
}
