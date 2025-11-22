package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeSlider implements ThemeComponent {

    private static String catalogKey = "slider";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeSlider());
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public Color getForeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getTrackcolor() {
        return ThemeProvider.getInstance().getBackground().getAlternateContrast();
    }

    public Color getHalocolor() {
        return ThemeProvider.getInstance().getBackground().getHighlight();
    }

    public ThemeSlider() {
    }

    @Override
    public void configureUI() {       
        UIManager.put("Slider.background", this.getBackground());
        UIManager.put("Slider.foreground", this.getForeground());
        UIManager.put("Slider.trackColor", this.getTrackcolor());
        UIManager.put("Slider.tickColor",  this.getForeground());
        UIManager.put("Slider.border", BorderFactory.createEmptyBorder());
        UIManager.put("Slider[halo].color", this.getHalocolor());
    }

}
