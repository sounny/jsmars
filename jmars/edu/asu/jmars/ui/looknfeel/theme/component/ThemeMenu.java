package edu.asu.jmars.ui.looknfeel.theme.component;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeMenu implements ThemeComponent {

    private static String catalogKey = "menu";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeMenu());
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getRow().getAlternate();
    }

    public Color getSelectionbackground() {
        return ThemeProvider.getInstance().getBackground().getHighlight();
    }
    
    public Color getSelectionforeground() {
        return ThemeProvider.getInstance().getSelection().getForegroundContrast();
    }

    public Color getForeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }    
   

    public ThemeMenu() {
    }

    @Override
    public void configureUI() {       
        UIManager.put("Menu.font", ThemeFont.getBold());
        UIManager.put("Menu.selectionBackground", this.getSelectionbackground());
        UIManager.put("Menu.selectionForeground", this.getSelectionforeground());
        UIManager.put("Menu.border", BorderFactory.createEmptyBorder(5, 0, 5, 0));
        UIManager.put("Menu.background", this.getBackground());
        UIManager.put("Menu.foreground", this.getForeground());        
        UIManager.put("Menu[arrowIcon].hoverColor", this.getSelectionforeground());
        UIManager.put("Menu[arrowIcon].color", this.getForeground());       
    }
}
