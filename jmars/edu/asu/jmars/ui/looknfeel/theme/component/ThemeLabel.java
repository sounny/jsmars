package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;
import mdlaf.utils.MaterialColors;


public class ThemeLabel implements ThemeComponent {

    private static String catalogKey = "label";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeLabel());
    }

    public ThemeLabel() {
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public Color getForeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getDisabledforeground() {
        return ThemeProvider.getInstance().getText().getDisabled();
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getBackgroundhilight() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }

    @Override
    public void configureUI() {        
        UIManager.put("Label.font", ThemeFont.getBold());
        UIManager.put("Label.background", this.getBackground());
        UIManager.put("Label.foreground", this.getForeground());
        UIManager.put("Label.disabledForeground", this.getDisabledforeground());
        UIManager.put("LinkLabel.foreground", MaterialColors.COSMO_MEDIUM_GRAY);
        UIManager.put("LinkLabel.mouseHoverColor", MaterialColors.COSMO_MEDIUM_GRAY);
        UIManager.put("LinkLabel.additionalSpaceIcon", 4);
        UIManager.put("LinkLabel.font",  ThemeFont.getBold());        
    }

}
