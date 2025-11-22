package edu.asu.jmars.ui.looknfeel.theme.component;

import java.awt.Color;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;

public class ThemeText implements ThemeComponent {

    private static String catalogKey = "text";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeText());
    }

    public ThemeText() {
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getTextcolor() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getHyperlink() {
        return ThemeProvider.getInstance().getText().getLink();
    }

    public Color getTextDisabled() {
        return ThemeProvider.getInstance().getText().getDisabled();
    }
    
    public Color getTextcontrastcolor() {
        return ThemeProvider.getInstance().getSelection().getForegroundContrast();
    }

    public Color getNullText() {
        return ThemeProvider.getInstance().getText().getNullText();
    }
    
    public Color getDateaslink() {
        return ThemeProvider.getInstance().getText().getDateAsLink();
    }  
    
    @Override
    public void configureUI() {        
    }

}
