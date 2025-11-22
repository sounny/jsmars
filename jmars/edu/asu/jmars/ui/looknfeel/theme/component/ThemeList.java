package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;

public class ThemeList implements ThemeComponent {

    private static String catalogKey = "list";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeList());
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
    
    public Color getForeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }


    public ThemeList() {
    }

    @Override
    public void configureUI() {        
        UIManager.put("List.font", ThemeFont.getRegular());
        UIManager.put("List.foreground", this.getForeground());
        UIManager.put("List.background", this.getBackground());
        UIManager.put("List.selectionBackground", this.getSelectionbackground());
        UIManager.put("List.selectionForeround", this.getSelectionforeground());
    }

}
