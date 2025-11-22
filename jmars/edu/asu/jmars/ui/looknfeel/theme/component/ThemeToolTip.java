package edu.asu.jmars.ui.looknfeel.theme.component;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;

public class ThemeToolTip implements ThemeComponent {

    private static String catalogKey = "tooltip";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeToolTip());
    }

    public ThemeToolTip() {
    }

    public Color getBackground() {       
       return ThemeProvider.getInstance().getBackground().getContrast();
    } 

    public Color getForeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }
    
    public static String getCatalogKey() {
        return catalogKey;
    }    

    @Override
    public void configureUI() {        
        UIManager.put("ToolTip.font", ThemeFont.getRegular());
        UIManager.put("ToolTip.background", this.getBackground());
        UIManager.put("ToolTip.foreground", this.getForeground());
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

}
