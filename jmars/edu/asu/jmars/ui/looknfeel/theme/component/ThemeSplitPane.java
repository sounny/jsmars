package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeSplitPane implements ThemeComponent {

    private static String catalogKey = "splitpane";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeSplitPane());
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }

    public ThemeSplitPane() {
    }

    @Override
    public void configureUI() {       
        UIManager.put("SplitPane.background", this.getBackground());
    }

}
