package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeSeparator implements ThemeComponent {

    private static String catalogKey = "separator";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeSeparator());
    }

    public ThemeSeparator() {
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public Color getForeground() {
        return ThemeProvider.getInstance().getText().getDisabled();
    }

    public Color getAltbackground() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }

    @Override
    public void configureUI() {
        UIManager.put("Separator.background", this.getBackground());
        UIManager.put("Separator.foreground", this.getForeground());
    }

}
