package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;

public class ThemeTextArea implements ThemeComponent {

    private static String catalogKey = "textarea";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeTextArea());
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getSelectionbackground() {
        return ThemeProvider.getInstance().getBackground().getHighlightContrast();
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

    public Color getCaretforeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getInactiveforeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public ThemeTextArea() {
    }

    @Override
    public void configureUI() {        
        UIManager.put("TextArea.font", ThemeFont.getRegular());
        UIManager.put("TextArea.foreground", this.getForeground());
        UIManager.put("TextArea.border", BorderFactory.createEmptyBorder());
        UIManager.put("TextArea.background", this.getBackground());
        UIManager.put("TextArea.caretForeground", this.getCaretforeground());
        UIManager.put("TextArea.selectionForeground", this.getSelectionforeground());
        UIManager.put("TextArea.inactiveForeground", this.getInactiveforeground());
        UIManager.put("TextArea.selectionBackground", this.getSelectionbackground());
    }

}
