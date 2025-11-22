package edu.asu.jmars.ui.looknfeel.theme.component;


import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemePanel implements ThemeComponent {

    private static String catalogKey = "panel";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemePanel());
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public Color getForeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }
    
    public Color getMidContrast() {
    	return ThemeProvider.getInstance().getBackground().getMidContrast();
    }

    public Color getBackgroundhi() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }
    
    public Color getBackgroundaltcontrastbright() {
        return ThemeProvider.getInstance().getBackground().getAlternateContrastBright();
    }

    public Color getLinecolor() {
        return ThemeProvider.getInstance().getBackground().getAlternateContrast();
    }

    public Color getTextcolor() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getSelectionhi() {
        return ThemeProvider.getInstance().getBackground().getHighlight();
    }
    
    public Color getBordercolor() {
        return ThemeProvider.getInstance().getBackground().getBorder();
    }    

    public ThemePanel() {
    }

    @Override
    public void configureUI() {        
        UIManager.put("Panel.font", ThemeFont.getRegular());
        UIManager.put("Panel.background", this.getBackground());
        UIManager.put("Panel.border", BorderFactory.createEmptyBorder());
    }

}
