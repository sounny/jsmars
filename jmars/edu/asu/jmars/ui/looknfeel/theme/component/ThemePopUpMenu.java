package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemePopUpMenu implements ThemeComponent {

    private static String catalogKey = "popupmenu";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemePopUpMenu());
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getBorder() {
        return ThemeProvider.getInstance().getBackground().getBorder();
    }

    public ThemePopUpMenu() {
    }

    @Override
    public void configureUI() {        
        UIManager.put("PopupMenu.border", new LineBorder(this.getBorder()));
    }

}
