package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeMenuItem implements ThemeComponent {

    private static String catalogKey = "menuitem";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeMenuItem());
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
    
    public Color getBackground() {
        return ThemeProvider.getInstance().getRow().getAlternate();
    }

    public Color getForeground() {
        return ThemeProvider.getInstance().getSelection().getForeground();
    }

    public Color getDisabledforeground() {
        return ThemeProvider.getInstance().getText().getDisabled();
    }

    public Color getAcceleratorforeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getAcceleratorselectionforeground() {
        return ThemeProvider.getInstance().getSelection().getForegroundContrast();
    }    

    public ThemeMenuItem() {
    }

    @Override
    public void configureUI() {       
        UIManager.put("MenuItem.font", ThemeFont.getBold());
        UIManager.put("MenuItem.border", BorderFactory.createEmptyBorder());
        UIManager.put("MenuItem.acceleratorFont", ThemeFont.getRegular());
        UIManager.put("MenuItem.selectionBackground", this.getSelectionbackground());
        UIManager.put("MenuItem.selectionForeground", this.getSelectionforeground());
        UIManager.put("MenuItem.border", BorderFactory.createEmptyBorder(5, 0, 5, 0));
        UIManager.put("MenuItem.background", this.getBackground());
        UIManager.put("MenuItem.foreground", this.getForeground());
        UIManager.put("MenuItem.disabledForeground", this.getDisabledforeground());
        UIManager.put("MenuItem.acceleratorForeground", this.getAcceleratorforeground());
        UIManager.put("MenuItem.acceleratorSelectionForeground", this.getAcceleratorselectionforeground());
    }

}
