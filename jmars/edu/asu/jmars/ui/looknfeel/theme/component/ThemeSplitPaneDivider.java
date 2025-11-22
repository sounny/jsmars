package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeSplitPaneDivider implements ThemeComponent {

    private static String catalogKey = "splitpanedivider";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeSplitPaneDivider());
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }

    public Color getDraggingcolor() {
        return ThemeProvider.getInstance().getBackground().getHighlight();
    }

    public ThemeSplitPaneDivider() {
    }

    @Override
    public void configureUI() {       
        UIManager.put("SplitPaneDivider.background", this.getBackground());
        UIManager.put("SplitPaneDivider.draggingColor", this.getDraggingcolor());
    }

}
