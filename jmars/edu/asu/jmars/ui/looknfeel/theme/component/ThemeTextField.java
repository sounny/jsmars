package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;
import mdlaf.utils.MaterialColors;


public class ThemeTextField implements ThemeComponent {

    private static String catalogKey = "textfield";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeTextField());
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getSelectionbackground() {
        return ThemeProvider.getInstance().getBackground().getHighlight();
    }

    public Color getSelectionforeground() {
    	 return ThemeProvider.getInstance().getText().getSelectionhighlight();
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

    public Color getInactivebackground() {
        return ThemeProvider.getInstance().getBackground().getContrast();        	
    }

    public Color getLineactive() {
        return ThemeProvider.getInstance().getAction().getLineActive();
    }

    public Color getLineinactive() {
        return ThemeProvider.getInstance().getAction().getLineInactive();
    }
    
    public Color getDisabledtext() {
        return ThemeProvider.getInstance().getAction().getDisabledForeground();
    } 
    
    public Color getDisablebackground() {
        return ThemeProvider.getInstance().getAction().getDisabled();
    }      

    public ThemeTextField() {
    }

    @Override
    public void configureUI() {        
        UIManager.put("TextField.font", ThemeFont.getRegular());
        UIManager.put("TextField.background", this.getBackground());
        UIManager.put("TextField.foreground", this.getForeground());
        UIManager.put("TextField.caretForeground", this.getCaretforeground());
        UIManager.put("TextField.selectionForeground", this.getSelectionforeground());
        UIManager.put("TextField.inactiveForeground", this.getInactiveforeground());
        UIManager.put("TextField.inactiveBackground", this.getInactivebackground());
        UIManager.put("TextField.selectionBackground", this.getSelectionbackground());
        UIManager.put("TextField[Line].inactiveColor", this.getLineinactive());
        UIManager.put("TextField[Line].activeColor", this.getLineactive());
        UIManager.put("TextField.disabledForeground", this.getDisabledtext());
        UIManager.put("TextField.disabledBackground", this.getDisablebackground());  
        
        UIManager.put("TextFieldPlaceholder.background", this.getBackground());
        UIManager.put("TextFieldPlaceholder.foreground", this.getForeground());
        UIManager.put("TextFieldPlaceholder[Line].activeColor", this.getLineactive());
        UIManager.put("TextFieldPlaceholder[Line].inactiveColor", this.getLineinactive());                       
        UIManager.put("TextFieldPlaceholder.caret",  this.getCaretforeground());
        UIManager.put("TextFieldPlaceholder.separatorColor", MaterialColors.COSMO_DARK_GRAY);       
    }

}
