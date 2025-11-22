package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeMenuBar implements ThemeComponent {

    private static String catalogKey = "menubar";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeMenuBar());
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getRow().getAlternate();
    }

    public Color getBorder() {
        return ThemeProvider.getInstance().getBackground().getHighlight();
    }

    public int getItemSpacing() {
        return ThemeProvider.getInstance().getSettings().getMenubarItemSpacing();
    }
    
    public int getItemPadding() {
        return ThemeProvider.getInstance().getSettings().getMenubarItemPadding();
    }

    public ThemeMenuBar() {
    }

    @Override
    public void configureUI() {        
        UIManager.put("MenuBar.font", ThemeFont.getBold());
        UIManager.put("MenuBar.background", this.getBackground());
        UIManager.put("MenuBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, this.getBorder()));
    }

}
