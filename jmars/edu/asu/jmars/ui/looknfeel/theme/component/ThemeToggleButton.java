package edu.asu.jmars.ui.looknfeel.theme.component;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.TOGGLE_OFF_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.TOGGLE_ON_IMG;


import java.awt.Color;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;

public class ThemeToggleButton implements ThemeComponent {

    private static String catalogKey = "togglebutton";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeToggleButton());
    }

    public ThemeToggleButton() {
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public Color getForeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getDisabledtext() {
        return ThemeProvider.getInstance().getText().getDisabled();
    }

    public Color getSelect() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public Color getOffColor() {
        return ThemeProvider.getInstance().getAction().getToggleOff();
    }

    public Color getOnColor() {
        return ThemeProvider.getInstance().getAction().getToggleOn();
    }

    @Override
    public void configureUI() {        
        UIManager.put("ToggleButton.font", ThemeFont.getRegular());
        UIManager.put("ToggleButton.background", this.getBackground());
        UIManager.put("ToggleButton.foreground", this.getForeground());
        UIManager.put("ToggleButton.disabledText", this.getDisabledtext());
        UIManager.put("ToggleButton.select", this.getSelect());
        UIManager.put("ToggleButton.icon",
                new ImageIcon(ImageFactory.createImage(TOGGLE_OFF_IMG.withDisplayColor(this.getOffColor()))));                       
        UIManager.put("ToggleButton.selectedIcon",
                new ImageIcon(ImageFactory.createImage(TOGGLE_ON_IMG.withDisplayColor(this.getOnColor()))));
    }

}
