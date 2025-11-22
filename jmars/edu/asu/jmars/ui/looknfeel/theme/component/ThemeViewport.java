package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;

public class ThemeViewport implements ThemeComponent {

    private static String catalogKey = "viewport";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeViewport());
    }

    public ThemeViewport() {
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    @Override
    public void configureUI() {        
        UIManager.put("Viewport.background", this.getBackground());
    }

}
