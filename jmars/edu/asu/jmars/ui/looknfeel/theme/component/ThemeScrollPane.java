package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeScrollPane implements ThemeComponent {

    private static String catalogKey = "scrollpane";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeScrollPane());
    }

    public ThemeScrollPane() {
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getLineborder() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }

    @Override
    public void configureUI() {        
        UIManager.put("ScrollPane.font", ThemeFont.getRegular());
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollPane.background", this.getBackground());
    }

}
