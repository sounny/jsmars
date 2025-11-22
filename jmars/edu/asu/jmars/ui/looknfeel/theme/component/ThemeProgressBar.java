package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeProgressBar implements ThemeComponent {

    private static String catalogKey = "progressbar";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeProgressBar());
    }

    public ThemeProgressBar() {
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getBordercolor() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public int getBorderthickness() {
        return ThemeProvider.getInstance().getSettings().getProgressbarBorderThickness();
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }

    public Color getForeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getSplash() {
        return ThemeProvider.getInstance().getBackground().getProgressbar();
    }

    @Override
    public void configureUI() {        
        UIManager.put("ProgressBar.border",
                BorderFactory.createLineBorder(this.getBordercolor(), this.getBorderthickness()));
        UIManager.put("ProgressBar.background", this.getBackground());
        UIManager.put("ProgressBar.foreground", this.getForeground());

    }

}
