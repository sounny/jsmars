package edu.asu.jmars.ui.looknfeel.theme.component;

import java.awt.Color;
import javax.swing.UIManager;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemePasswordField implements ThemeComponent {

    private static String catalogKey = "passwordfield";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemePasswordField());
    }

    public ThemePasswordField() {
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getForeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getCaretforeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getSelectionforeground() {
        return ThemeProvider.getInstance().getText().getSelectionhighlight();
    }

    public Color getInactiveforeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getInactivebackground() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }

    public Color getSelectionbackground() {
        return ThemeProvider.getInstance().getBackground().getHighlight();
    }

    public Color getLineinactivecolor() {
        return ThemeProvider.getInstance().getAction().getLineInactive();
    }

    public Color getLineactivecolor() {
        return ThemeProvider.getInstance().getAction().getLineActive();
    }
    
    public Color getDisabledtext() {
        return ThemeProvider.getInstance().getAction().getDisabledForeground();
    } 
    
    public Color getDisablebackground() {
        return ThemeProvider.getInstance().getAction().getDisabled();
    }          

    @Override
    public void configureUI() {        
        UIManager.put("PasswordField.background", this.getBackground());
        UIManager.put("PasswordField.foreground", this.getForeground());
        UIManager.put("PasswordField.caretForeground", this.getCaretforeground());
        UIManager.put("PasswordField.selectionForeground", this.getSelectionforeground());
        UIManager.put("PasswordField.inactiveForeground", this.getInactiveforeground());
        UIManager.put("PasswordField.inactiveBackground", this.getInactivebackground());
        UIManager.put("PasswordField.selectionBackground", this.getSelectionbackground());
        UIManager.put("PasswordField[Line].inactiveColor", this.getLineinactivecolor());
        UIManager.put("PasswordField[Line].activeColor", this.getLineactivecolor());
        UIManager.put("PasswordField.disabledForeground", this.getDisabledtext());
        UIManager.put("PasswordField.disabledBackground", this.getDisablebackground());
    }

}
