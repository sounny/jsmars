package edu.asu.jmars.ui.looknfeel.theme.component;

import java.awt.Color;
import javax.swing.UIManager;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;

public class ThemeColorChooser implements ThemeComponent {

    private static String catalogKey = "colorchooser";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeColorChooser());
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }  

    public ThemeColorChooser() {
    }

    @Override
    public void configureUI() {   
    	UIManager.put("ColorChooser.background", this.getBackground());    					
    }
}
