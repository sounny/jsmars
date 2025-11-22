package edu.asu.jmars.ui.looknfeel.theme.component;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.RADIO_OFF_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.RADIO_ON_IMG;


import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeRadioButtonMenuItem implements ThemeComponent {

    private static String catalogKey = "radiobuttonmenuitem";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeRadioButtonMenuItem());
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

    public Color getRadioiconimgcolor() {
        return ThemeProvider.getInstance().getImage().getFill();
    }

    public Color getSelectedradioiconimgcolor() {
        return ThemeProvider.getInstance().getImage().getFill();
    }

    public Color getAcceleratorforeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getAcceleratorselectionforeground() {
        return ThemeProvider.getInstance().getSelection().getForegroundContrast();
    }

    public ThemeRadioButtonMenuItem() {
    }

    @Override
    public void configureUI() {        
        UIManager.put("RadioButtonMenuItem.font", ThemeFont.getBold());
        UIManager.put("RadioButtonMenuItem.selectionBackground", this.getSelectionbackground());
        UIManager.put("RadioButtonMenuItem.selectionForeground", getSelectionforeground());
        UIManager.put("RadioButtonMenuItem.border", BorderFactory.createEmptyBorder(5, 0, 5, 0));
        UIManager.put("RadioButtonMenuItem.background", this.getBackground());
        UIManager.put("RadioButtonMenuItem.foreground", this.getForeground());
        UIManager.put("RadioButtonMenuItem.checkIcon", new ImageIcon(
                ImageFactory.createImage(RADIO_OFF_IMG.withDisplayColor(this.getRadioiconimgcolor()))));
        UIManager.put("RadioButtonMenuItem.selectedCheckIcon", new ImageIcon(
                ImageFactory.createImage(RADIO_ON_IMG.withDisplayColor(this.getSelectedradioiconimgcolor()))));
        UIManager.put("RadioButtonMenuItem.acceleratorForeground", this.getAcceleratorforeground());
        UIManager.put("RadioButtonMenuItem.acceleratorSelectionForeground",
                this.getAcceleratorselectionforeground());
    }

}
